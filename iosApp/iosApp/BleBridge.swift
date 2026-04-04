import Foundation
import AccessorySetupKit
import CoreBluetooth
import ComposeApp
import UIKit
import UserNotifications

@objcMembers
final class BleBridge: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    static let shared = BleBridge()

    private enum SessionStateName: String {
        case idle
        case scanning
        case connecting
        case discoveringServices = "discovering_services"
        case subscribingCurrentStatus = "subscribing_current_status"
        case subscribingAlarmEvent = "subscribing_alarm_event"
        case readingAlarmHistory = "reading_alarm_history"
        case ready
        case reconnecting
        case failed
    }

    private enum StoreKey {
        static let monitoringEnabled = "ble.monitoring.enabled"
        static let serviceUuid = "ble.target.service_uuid"
        static let deviceName = "ble.target.device_name"
        static let peripheralIdentifier = "ble.target.peripheral_id"
        static let currentStatusPayload = "ble.snapshot.current_status"
        static let alarmHistoryPayload = "ble.snapshot.alarm_history"
    }

    private static let restoreIdentifier = "ru.aokruan.hmlkbi.ble.central"
    private static let currentStatusUuid = CBUUID(string: "12345678-1234-5678-1234-56789abc0001")
    private static let alarmEventUuid = CBUUID(string: "12345678-1234-5678-1234-56789abc0002")
    private static let alarmHistoryUuid = CBUUID(string: "12345678-1234-5678-1234-56789abc0003")
    private static let controlUuid = CBUUID(string: "12345678-1234-5678-1234-56789abc0004")

    private let defaults = UserDefaults.standard
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private let notificationCenter = UNUserNotificationCenter.current()
    private let reconnectDelays: [TimeInterval] = [2, 5, 10, 15]

    private var accessorySession: ASAccessorySession?
    private var accessorySessionActivated = false
    private var accessoryPickerInFlight = false
    private var pendingAccessoryPickerReason: String?

    private var central: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var interop: IosBleInterop?

    private var targetServiceUuid: CBUUID?
    private var targetDeviceName: String?
    private var monitoringEnabled = false

    private var currentStatusChar: CBCharacteristic?
    private var alarmEventChar: CBCharacteristic?
    private var alarmHistoryChar: CBCharacteristic?
    private var controlChar: CBCharacteristic?

    private var currentSessionState: SessionStateName = .idle
    private var currentSessionMessage: String?
    private var reconnectAttempt = 0
    private var reconnectWorkItem: DispatchWorkItem?

    private var lastCurrentStatusPayload: String?
    private var lastAlarmHistoryPayload: String?
    private var cachedAlarmHistory: [StoredAlarmEvent] = []
    private var notifiedCriticalAlarmIds = Set<Int64>()

    private override init() {
        super.init()
        loadPersistedState()

        central = CBCentralManager(
            delegate: self,
            queue: nil,
            options: [
                CBCentralManagerOptionShowPowerAlertKey: true,
                CBCentralManagerOptionRestoreIdentifierKey: Self.restoreIdentifier,
            ]
        )

        activateAccessorySessionIfNeeded()
    }

    func bootstrap(launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) {
        if let restoredIds = launchOptions?[.bluetoothCentrals] as? [String], !restoredIds.isEmpty {
            print("BleBridge.bootstrap restored identifiers=\(restoredIds)")
        }

        if monitoringEnabled && currentSessionState == .idle {
            updateSessionState(.reconnecting)
        }
    }

    func registerIntoKotlin(interop: IosBleInterop) {
        self.interop = interop

        interop.registerStartMonitoring { [weak self] serviceUuid, deviceName in
            self?.startMonitoring(serviceUuid: serviceUuid, deviceName: deviceName)
        }

        interop.registerStopMonitoring { [weak self] in
            self?.stopMonitoring()
        }

        interop.registerAcknowledgeAlarm { [weak self] alarmId in
            self?.acknowledgeAlarm(alarmId: Int64(truncating: alarmId as NSNumber))
        }

        syncRuntimeSnapshot()

        if monitoringEnabled {
            restoreMonitoringIfNeeded(reason: "registerIntoKotlin")
        }
    }

    private func startMonitoring(serviceUuid: String, deviceName: String) {
        print("BleBridge.startMonitoring serviceUuid=\(serviceUuid) deviceName=\(deviceName)")

        cancelReconnect()
        reconnectAttempt = 0

        let targetChanged =
            targetServiceUuid?.uuidString != serviceUuid ||
            targetDeviceName != deviceName

        if targetChanged {
            if let peripheral, peripheral.state == .connected || peripheral.state == .connecting {
                central.cancelPeripheralConnection(peripheral)
            }
            self.peripheral = nil
            clearCharacteristicRefs()
            defaults.removeObject(forKey: StoreKey.peripheralIdentifier)
        }

        monitoringEnabled = true
        targetServiceUuid = CBUUID(string: serviceUuid)
        targetDeviceName = deviceName

        persistTarget()
        updateSessionState(.reconnecting)

        if connectAuthorizedAccessoryIfAvailable(reason: "startMonitoring:authorizedAccessory") {
            return
        }

        if showAccessoryPickerIfNeeded(reason: "startMonitoring") {
            return
        }

        if let peripheral, peripheral.state == .connected, matchesTarget(peripheral: peripheral, advertisedName: nil) {
            resumeMonitoring(on: peripheral)
            return
        }

        restoreMonitoringIfNeeded(reason: "startMonitoring")
    }

    private func stopMonitoring() {
        print("BleBridge.stopMonitoring")

        monitoringEnabled = false
        cancelReconnect()
        reconnectAttempt = 0
        notifiedCriticalAlarmIds.removeAll()
        accessoryPickerInFlight = false
        pendingAccessoryPickerReason = nil

        central.stopScan()

        if let peripheral {
            central.cancelPeripheralConnection(peripheral)
        }

        clearCharacteristicRefs()
        self.peripheral = nil
        targetServiceUuid = nil
        targetDeviceName = nil
        lastCurrentStatusPayload = nil
        lastAlarmHistoryPayload = nil
        cachedAlarmHistory = []

        clearPersistedState()
        updateSessionState(.idle)
        interop?.clearMonitoringSnapshot()
    }

    private func acknowledgeAlarm(alarmId: Int64) {
        guard let peripheral, let controlChar else {
            print("BleBridge.acknowledgeAlarm: missing peripheral/controlChar")
            return
        }

        let payload = #"{"type":"ack","alarmId":\#(alarmId)}"#
        guard let data = payload.data(using: .utf8) else { return }
        peripheral.writeValue(data, for: controlChar, type: .withResponse)
    }

    private func restoreMonitoringIfNeeded(reason: String) {
        guard monitoringEnabled, let targetServiceUuid else {
            return
        }

        guard central.state == .poweredOn else {
            updateSessionState(.reconnecting, message: "Bluetooth пока недоступен")
            return
        }

        if connectAuthorizedAccessoryIfAvailable(reason: "\(reason):authorizedAccessory") {
            return
        }

        if let peripheral, matchesTarget(peripheral: peripheral, advertisedName: nil) {
            connectOrResume(peripheral, reason: reason)
            return
        } else {
            self.peripheral = nil
            clearCharacteristicRefs()
        }

        if let persistedIdentifier = persistedPeripheralIdentifier(),
           let candidate = central.retrievePeripherals(withIdentifiers: [persistedIdentifier]).first {
            connectOrResume(candidate, reason: "\(reason):persistedPeripheral")
            return
        }

        if let candidate = central.retrieveConnectedPeripherals(withServices: [targetServiceUuid])
            .first(where: { matchesTarget(peripheral: $0, advertisedName: nil) }) {
            connectOrResume(candidate, reason: "\(reason):connectedPeripheral")
            return
        }

        startScan(reason: reason)
    }

    private func activateAccessorySessionIfNeeded() {
        guard accessorySession == nil else { return }

        let session = ASAccessorySession()
        accessorySession = session
        session.activate(on: .main) { [weak self] event in
            self?.handleAccessoryEvent(event)
        }
    }

    private func handleAccessoryEvent(_ event: ASAccessoryEvent) {
        let accessoryName = event.accessory?.displayName ?? "(null)"
        print(
            "BleBridge.handleAccessoryEvent type=\(event.eventType.rawValue) " +
            "accessory=\(accessoryName) error=\(String(describing: event.error))"
        )

        switch event.eventType {
        case .activated:
            accessorySessionActivated = true

            if let pendingAccessoryPickerReason, monitoringEnabled {
                presentAccessoryPicker(reason: pendingAccessoryPickerReason)
            } else if monitoringEnabled {
                restoreMonitoringIfNeeded(reason: "accessorySessionActivated")
            }

        case .invalidated:
            accessorySessionActivated = false
            accessoryPickerInFlight = false

        case .accessoryAdded, .accessoryChanged:
            guard let accessory = event.accessory, matchesTarget(accessory: accessory) else {
                return
            }

            accessoryPickerInFlight = false
            pendingAccessoryPickerReason = nil

            if let identifier = accessory.bluetoothIdentifier {
                persistPeripheralIdentifier(identifier)
            }

            restoreMonitoringIfNeeded(reason: "accessoryAuthorized")

        case .accessoryRemoved:
            accessoryPickerInFlight = false

            guard let removedIdentifier = event.accessory?.bluetoothIdentifier else {
                return
            }

            if persistedPeripheralIdentifier() == removedIdentifier {
                defaults.removeObject(forKey: StoreKey.peripheralIdentifier)
            }

        case .pickerDidDismiss:
            accessoryPickerInFlight = false
            pendingAccessoryPickerReason = nil

            if monitoringEnabled {
                restoreMonitoringIfNeeded(reason: "accessoryPickerDismissed")
            }

        case .pickerSetupFailed:
            accessoryPickerInFlight = false
            pendingAccessoryPickerReason = nil

            if monitoringEnabled {
                restoreMonitoringIfNeeded(reason: "accessoryPickerSetupFailed")
            }

        default:
            break
        }
    }

    private func connectAuthorizedAccessoryIfAvailable(reason: String) -> Bool {
        guard let identifier = matchingAuthorizedAccessory()?.bluetoothIdentifier else {
            return false
        }

        persistPeripheralIdentifier(identifier)

        guard let candidate = central.retrievePeripherals(withIdentifiers: [identifier]).first else {
            return false
        }

        connectOrResume(candidate, reason: reason)
        return true
    }

    private func showAccessoryPickerIfNeeded(reason: String) -> Bool {
        guard monitoringEnabled else { return false }
        guard targetServiceUuid != nil else { return false }
        guard central.state == .poweredOn else { return false }
        guard UIApplication.shared.applicationState == .active else { return false }
        guard matchingAuthorizedAccessory() == nil else { return false }

        activateAccessorySessionIfNeeded()

        if accessoryPickerInFlight {
            return true
        }

        if !accessorySessionActivated {
            pendingAccessoryPickerReason = reason
            updateSessionState(.scanning)
            return true
        }

        presentAccessoryPicker(reason: reason)
        return true
    }

    private func presentAccessoryPicker(reason: String) {
        guard let session = accessorySession,
              let displayItem = makeAccessoryPickerDisplayItem() else {
            return
        }

        accessoryPickerInFlight = true
        pendingAccessoryPickerReason = nil
        updateSessionState(.scanning)

        session.showPicker(for: [displayItem]) { [weak self] error in
            guard let self else { return }

            if let error {
                print("BleBridge.showPicker error=\(error.localizedDescription)")
                self.accessoryPickerInFlight = false
                self.restoreMonitoringIfNeeded(reason: "\(reason):pickerError")
            }
        }
    }

    private func makeAccessoryPickerDisplayItem() -> ASPickerDisplayItem? {
        guard let targetServiceUuid else { return nil }

        let descriptor = ASDiscoveryDescriptor()
        descriptor.bluetoothServiceUUID = targetServiceUuid
        descriptor.bluetoothRange = .default

        if let targetDeviceName, !targetDeviceName.isEmpty {
            descriptor.bluetoothNameSubstring = targetDeviceName
            descriptor.bluetoothNameSubstringCompareOptions = [.caseInsensitive]
        }

        let productImage = UIImage(systemName: "heart.text.square.fill") ?? UIImage()
        let title = targetDeviceName ?? "Медицинское устройство"
        return ASPickerDisplayItem(
            name: title,
            productImage: productImage,
            descriptor: descriptor
        )
    }

    private func matchingAuthorizedAccessory() -> ASAccessory? {
        guard let session = accessorySession else {
            return nil
        }

        return session.accessories.first(where: matchesTarget(accessory:))
    }

    private func startScan(reason: String) {
        guard monitoringEnabled, let targetServiceUuid else { return }

        print("BleBridge.startScan reason=\(reason) uuid=\(targetServiceUuid.uuidString)")
        clearCharacteristicRefs()
        updateSessionState(.scanning)

        central.stopScan()
        central.scanForPeripherals(
            withServices: [targetServiceUuid],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    private func connectOrResume(_ candidate: CBPeripheral, reason: String) {
        print("BleBridge.connectOrResume reason=\(reason) peripheral=\(candidate.identifier)")

        peripheral = candidate
        candidate.delegate = self
        persistPeripheralIdentifier(candidate.identifier)
        central.stopScan()

        switch candidate.state {
        case .connected:
            resumeMonitoring(on: candidate)

        case .connecting:
            updateSessionState(.connecting)

        case .disconnected:
            updateSessionState(.connecting)
            central.connect(
                candidate,
                options: [
                    CBConnectPeripheralOptionNotifyOnConnectionKey: true,
                    CBConnectPeripheralOptionNotifyOnDisconnectionKey: true,
                    CBConnectPeripheralOptionNotifyOnNotificationKey: true,
                ]
            )

        case .disconnecting:
            updateSessionState(.reconnecting)
            scheduleReconnect()

        @unknown default:
            updateSessionState(.reconnecting)
            scheduleReconnect()
        }
    }

    private func resumeMonitoring(on peripheral: CBPeripheral) {
        self.peripheral = peripheral
        peripheral.delegate = self

        if let targetServiceUuid,
           let service = peripheral.services?.first(where: { $0.uuid == targetServiceUuid }) {
            hydrateCharacteristics(from: service)

            if hasRequiredCharacteristics() {
                subscribeAndRead(on: peripheral)
            } else {
                updateSessionState(.discoveringServices)
                peripheral.discoverCharacteristics(
                    [
                        Self.currentStatusUuid,
                        Self.alarmEventUuid,
                        Self.alarmHistoryUuid,
                        Self.controlUuid,
                    ],
                    for: service
                )
            }

            return
        }

        if let targetServiceUuid {
            updateSessionState(.discoveringServices)
            peripheral.discoverServices([targetServiceUuid])
        }
    }

    private func subscribeAndRead(on peripheral: CBPeripheral) {
        guard currentStatusChar != nil,
              alarmEventChar != nil,
              alarmHistoryChar != nil,
              controlChar != nil else {
            updateSessionState(.failed, message: "Не найдены обязательные BLE-характеристики")
            scheduleReconnect()
            return
        }

        if let currentStatusChar, !currentStatusChar.isNotifying {
            updateSessionState(.subscribingCurrentStatus)
            peripheral.setNotifyValue(true, for: currentStatusChar)
        }

        if let alarmEventChar, !alarmEventChar.isNotifying {
            updateSessionState(.subscribingAlarmEvent)
            peripheral.setNotifyValue(true, for: alarmEventChar)
        }

        if let alarmHistoryChar {
            updateSessionState(.readingAlarmHistory)
            peripheral.readValue(for: alarmHistoryChar)
        }

        updateReadyIfPossible()
    }

    private func clearCharacteristicRefs() {
        currentStatusChar = nil
        alarmEventChar = nil
        alarmHistoryChar = nil
        controlChar = nil
    }

    private func hasRequiredCharacteristics() -> Bool {
        currentStatusChar != nil &&
        alarmEventChar != nil &&
        alarmHistoryChar != nil &&
        controlChar != nil
    }

    private func hydrateCharacteristics(from service: CBService) {
        service.characteristics?.forEach { characteristic in
            switch characteristic.uuid {
            case Self.currentStatusUuid:
                currentStatusChar = characteristic
            case Self.alarmEventUuid:
                alarmEventChar = characteristic
            case Self.alarmHistoryUuid:
                alarmHistoryChar = characteristic
            case Self.controlUuid:
                controlChar = characteristic
            default:
                break
            }
        }
    }

    private func scheduleReconnect() {
        guard monitoringEnabled else { return }

        cancelReconnect()
        updateSessionState(.reconnecting)

        let delay = reconnectDelays[min(reconnectAttempt, reconnectDelays.count - 1)]
        reconnectAttempt += 1

        let workItem = DispatchWorkItem { [weak self] in
            self?.restoreMonitoringIfNeeded(reason: "scheduledReconnect")
        }
        reconnectWorkItem = workItem

        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: workItem)
    }

    private func cancelReconnect() {
        reconnectWorkItem?.cancel()
        reconnectWorkItem = nil
    }

    private func updateSessionState(_ state: SessionStateName, message: String? = nil) {
        currentSessionState = state
        currentSessionMessage = message
        interop?.emitSessionState(name: state.rawValue, message: message)
    }

    private func syncRuntimeSnapshot() {
        if monitoringEnabled {
            interop?.emitSessionState(name: currentSessionState.rawValue, message: currentSessionMessage)
        } else {
            interop?.clearMonitoringSnapshot()
        }

        if let lastAlarmHistoryPayload {
            interop?.emitAlarmHistoryJson(payload: lastAlarmHistoryPayload)
        }

        if let lastCurrentStatusPayload {
            interop?.emitCurrentStatusJson(payload: lastCurrentStatusPayload)
        }
    }

    private func updateReadyIfPossible() {
        guard monitoringEnabled else { return }
        guard currentStatusChar != nil, alarmEventChar != nil else { return }

        let currentReady = currentStatusChar?.isNotifying ?? false
        let alarmReady = alarmEventChar?.isNotifying ?? false

        if currentReady && alarmReady {
            updateSessionState(.ready)
        }
    }

    private func matchesTarget(peripheral: CBPeripheral, advertisedName: String?) -> Bool {
        guard let expectedName = targetDeviceName else { return true }
        let peripheralName = peripheral.name
        return peripheralName == expectedName || advertisedName == expectedName
    }

    private func matchesTarget(accessory: ASAccessory) -> Bool {
        if let persistedIdentifier = persistedPeripheralIdentifier(),
           accessory.bluetoothIdentifier == persistedIdentifier {
            return true
        }

        if let targetServiceUuid, accessory.descriptor.bluetoothServiceUUID == targetServiceUuid {
            return true
        }

        if let expectedName = targetDeviceName {
            return accessory.displayName == expectedName
        }

        return false
    }

    private func persistTarget() {
        defaults.set(monitoringEnabled, forKey: StoreKey.monitoringEnabled)
        defaults.set(targetServiceUuid?.uuidString, forKey: StoreKey.serviceUuid)
        defaults.set(targetDeviceName, forKey: StoreKey.deviceName)
    }

    private func persistPeripheralIdentifier(_ identifier: UUID) {
        defaults.set(identifier.uuidString, forKey: StoreKey.peripheralIdentifier)
    }

    private func persistedPeripheralIdentifier() -> UUID? {
        guard let uuidString = defaults.string(forKey: StoreKey.peripheralIdentifier) else {
            return nil
        }
        return UUID(uuidString: uuidString)
    }

    private func persistCurrentStatusPayload(_ payload: String) {
        lastCurrentStatusPayload = payload
        defaults.set(payload, forKey: StoreKey.currentStatusPayload)
    }

    private func persistAlarmHistoryPayload(_ payload: String) {
        lastAlarmHistoryPayload = payload
        defaults.set(payload, forKey: StoreKey.alarmHistoryPayload)
    }

    private func clearPersistedState() {
        defaults.removeObject(forKey: StoreKey.monitoringEnabled)
        defaults.removeObject(forKey: StoreKey.serviceUuid)
        defaults.removeObject(forKey: StoreKey.deviceName)
        defaults.removeObject(forKey: StoreKey.peripheralIdentifier)
        defaults.removeObject(forKey: StoreKey.currentStatusPayload)
        defaults.removeObject(forKey: StoreKey.alarmHistoryPayload)
    }

    private func loadPersistedState() {
        monitoringEnabled = defaults.bool(forKey: StoreKey.monitoringEnabled)

        if let serviceUuid = defaults.string(forKey: StoreKey.serviceUuid) {
            targetServiceUuid = CBUUID(string: serviceUuid)
        }

        targetDeviceName = defaults.string(forKey: StoreKey.deviceName)
        lastCurrentStatusPayload = defaults.string(forKey: StoreKey.currentStatusPayload)
        lastAlarmHistoryPayload = defaults.string(forKey: StoreKey.alarmHistoryPayload)

        if let lastAlarmHistoryPayload {
            cachedAlarmHistory = decodeAlarmHistory(from: lastAlarmHistoryPayload)
            notifiedCriticalAlarmIds = Set(
                cachedAlarmHistory
                    .filter { $0.active && $0.severity == .critical }
                    .map(\.id)
            )
        }

        currentSessionState = monitoringEnabled ? .reconnecting : .idle
    }

    private func decodeAlarmHistory(from payload: String) -> [StoredAlarmEvent] {
        guard let data = payload.data(using: .utf8) else { return [] }
        return (try? decoder.decode([StoredAlarmEvent].self, from: data)) ?? []
    }

    private func storeAlarmEvent(_ alarm: StoredAlarmEvent) {
        cachedAlarmHistory = mergeAlarm(alarm, into: cachedAlarmHistory)

        guard let data = try? encoder.encode(cachedAlarmHistory),
              let payload = String(data: data, encoding: .utf8) else {
            return
        }

        persistAlarmHistoryPayload(payload)
    }

    private func mergeAlarm(_ alarm: StoredAlarmEvent, into history: [StoredAlarmEvent]) -> [StoredAlarmEvent] {
        let updated = history.filter { $0.id != alarm.id }
        return ([alarm] + updated).sorted { lhs, rhs in
            lhs.timestampEpochSec > rhs.timestampEpochSec
        }
    }

    private func maybeNotifyAboutCriticalAlarm(_ alarm: StoredAlarmEvent) {
        if !alarm.active || alarm.severity != .critical {
            notifiedCriticalAlarmIds.remove(alarm.id)
            return
        }

        guard UIApplication.shared.applicationState != .active else {
            return
        }

        guard !notifiedCriticalAlarmIds.contains(alarm.id) else {
            return
        }

        notifiedCriticalAlarmIds.insert(alarm.id)

        let content = UNMutableNotificationContent()
        content.title = "Критическое состояние"
        content.body = alarm.message
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "ble-critical-\(alarm.id)",
            content: content,
            trigger: nil
        )

        notificationCenter.add(request) { error in
            if let error {
                print("BleBridge notification scheduling error=\(error.localizedDescription)")
            }
        }
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("BleBridge.centralManagerDidUpdateState state=\(central.state.rawValue)")

        switch central.state {
        case .poweredOn:
            if monitoringEnabled {
                restoreMonitoringIfNeeded(reason: "centralPoweredOn")
            }

        case .poweredOff:
            updateSessionState(.failed, message: "Bluetooth выключен")

        case .unauthorized:
            updateSessionState(.failed, message: "Нет доступа к Bluetooth")

        case .unsupported:
            updateSessionState(.failed, message: "Bluetooth не поддерживается")

        case .resetting:
            updateSessionState(.reconnecting, message: "Bluetooth перезапускается")

        case .unknown:
            updateSessionState(.reconnecting, message: "Ожидание Bluetooth")

        @unknown default:
            updateSessionState(.reconnecting, message: "Неизвестное состояние Bluetooth")
        }
    }

    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
        print("BleBridge.willRestoreState dictKeys=\(Array(dict.keys))")

        guard monitoringEnabled else { return }

        if let restoredPeripherals = dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral],
           let restored = restoredPeripherals.first {
            peripheral = restored
            restored.delegate = self
            persistPeripheralIdentifier(restored.identifier)
            connectOrResume(restored, reason: "willRestoreState")
            return
        }

        if let services = dict[CBCentralManagerRestoredStateScanServicesKey] as? [CBUUID], !services.isEmpty {
            updateSessionState(.scanning)
        }

        restoreMonitoringIfNeeded(reason: "willRestoreStateFallback")
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover discoveredPeripheral: CBPeripheral,
        advertisementData: [String : Any],
        rssi RSSI: NSNumber
    ) {
        let peripheralName = discoveredPeripheral.name ?? "(null)"
        let advertisedName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let expectedName = targetDeviceName ?? "(null)"

        print(
            """
            BleBridge.didDiscover \
            peripheralName=\(peripheralName) \
            advertisedName=\(advertisedName ?? "(null)") \
            expectedName=\(expectedName) \
            rssi=\(RSSI)
            """
        )

        guard matchesTarget(peripheral: discoveredPeripheral, advertisedName: advertisedName) else {
            print("BleBridge.didDiscover skipped by name filter")
            return
        }

        connectOrResume(discoveredPeripheral, reason: "didDiscover")
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("BleBridge.didConnect name=\(peripheral.name ?? "(null)")")
        self.peripheral = peripheral
        peripheral.delegate = self
        reconnectAttempt = 0
        updateSessionState(.discoveringServices)

        if let targetServiceUuid {
            peripheral.discoverServices([targetServiceUuid])
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        print("BleBridge.didFailToConnect error=\(String(describing: error))")
        clearCharacteristicRefs()
        updateSessionState(.failed, message: error?.localizedDescription ?? "Не удалось подключиться")
        scheduleReconnect()
    }

    func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        print("BleBridge.didDisconnect error=\(String(describing: error))")
        clearCharacteristicRefs()

        guard monitoringEnabled else {
            updateSessionState(.idle)
            return
        }

        updateSessionState(.reconnecting, message: error?.localizedDescription)
        scheduleReconnect()
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        print("BleBridge.didDiscoverServices error=\(String(describing: error))")

        if let error {
            updateSessionState(.failed, message: error.localizedDescription)
            scheduleReconnect()
            return
        }

        guard let services = peripheral.services,
              let service = services.first(where: { $0.uuid == targetServiceUuid }) else {
            updateSessionState(.failed, message: "Целевой сервис не найден")
            scheduleReconnect()
            return
        }

        clearCharacteristicRefs()
        updateSessionState(.discoveringServices)
        peripheral.discoverCharacteristics(
            [
                Self.currentStatusUuid,
                Self.alarmEventUuid,
                Self.alarmHistoryUuid,
                Self.controlUuid,
            ],
            for: service
        )
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        print("BleBridge.didDiscoverCharacteristics error=\(String(describing: error))")

        if let error {
            updateSessionState(.failed, message: error.localizedDescription)
            scheduleReconnect()
            return
        }

        hydrateCharacteristics(from: service)
        subscribeAndRead(on: peripheral)
    }

    func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateNotificationStateFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        print(
            "BleBridge.didUpdateNotificationState uuid=\(characteristic.uuid.uuidString) " +
            "notifying=\(characteristic.isNotifying) error=\(String(describing: error))"
        )

        if let error {
            updateSessionState(.failed, message: error.localizedDescription)
            scheduleReconnect()
            return
        }

        updateReadyIfPossible()
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        let text = characteristic.value.flatMap { String(data: $0, encoding: .utf8) }
        print(
            "BleBridge.didUpdateValue uuid=\(characteristic.uuid.uuidString) " +
            "text=\(String(describing: text)) error=\(String(describing: error))"
        )

        if let error {
            updateSessionState(.failed, message: error.localizedDescription)
            scheduleReconnect()
            return
        }

        guard let text else { return }

        switch characteristic.uuid {
        case Self.currentStatusUuid:
            persistCurrentStatusPayload(text)
            interop?.emitCurrentStatusJson(payload: text)
            updateReadyIfPossible()

        case Self.alarmEventUuid:
            interop?.emitAlarmEventJson(payload: text)

            if let data = text.data(using: .utf8),
               let alarm = try? decoder.decode(StoredAlarmEvent.self, from: data) {
                storeAlarmEvent(alarm)
                maybeNotifyAboutCriticalAlarm(alarm)
            }

            updateReadyIfPossible()

        case Self.alarmHistoryUuid:
            persistAlarmHistoryPayload(text)
            cachedAlarmHistory = decodeAlarmHistory(from: text)
            interop?.emitAlarmHistoryJson(payload: text)
            updateReadyIfPossible()

        default:
            break
        }
    }
}

private struct StoredAlarmEvent: Codable {
    enum Severity: String, Codable {
        case info = "INFO"
        case warning = "WARNING"
        case critical = "CRITICAL"
    }

    let id: Int64
    let severity: Severity
    let code: String
    let message: String
    let active: Bool
    let timestampEpochSec: Int64

    enum CodingKeys: String, CodingKey {
        case id
        case severity
        case code
        case message
        case active
        case timestampEpochSec = "ts"
    }
}
