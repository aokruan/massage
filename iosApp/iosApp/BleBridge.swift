import Foundation
import CoreBluetooth
import ComposeApp

@objcMembers
final class BleBridge: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    static let shared = BleBridge()

    private var central: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var interop: IosBleInterop?

    private var targetServiceUuid: CBUUID?
    private var targetDeviceName: String?

    private var currentStatusChar: CBCharacteristic?
    private var alarmEventChar: CBCharacteristic?
    private var alarmHistoryChar: CBCharacteristic?
    private var controlChar: CBCharacteristic?

    private override init() {
        super.init()
        central = CBCentralManager(delegate: self, queue: nil)
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
    }

    private func startMonitoring(serviceUuid: String, deviceName: String) {
        print("BleBridge.startMonitoring serviceUuid=\(serviceUuid) deviceName=\(deviceName)")
        targetServiceUuid = CBUUID(string: serviceUuid)
        targetDeviceName = deviceName

        guard central.state == .poweredOn else {
            print("BleBridge: central not poweredOn, state=\(central.state.rawValue)")
            return
        }

        startScan()
    }

    private func stopMonitoring() {
        print("BleBridge.stopMonitoring")
        central.stopScan()
        if let peripheral {
            central.cancelPeripheralConnection(peripheral)
        }
        peripheral = nil
        currentStatusChar = nil
        alarmEventChar = nil
        alarmHistoryChar = nil
        controlChar = nil
    }

    private func startScan() {
        guard let targetServiceUuid else {
            print("BleBridge: targetServiceUuid is nil")
            return
        }
        print("BleBridge.startScan uuid=\(targetServiceUuid.uuidString)")
        central.stopScan()
        central.scanForPeripherals(withServices: [targetServiceUuid], options: nil)
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

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("BleBridge.centralManagerDidUpdateState state=\(central.state.rawValue)")
        if central.state == .poweredOn, targetServiceUuid != nil {
            startScan()
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover discoveredPeripheral: CBPeripheral,
        advertisementData: [String : Any],
        rssi RSSI: NSNumber
    ) {
        let peripheralName = discoveredPeripheral.name ?? "(null)"
        let advertisedName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let expectedName = targetDeviceName

        print(
            """
            BleBridge.didDiscover \
            peripheralName=\(peripheralName) \
            advertisedName=\(advertisedName ?? "(null)") \
            expectedName=\(expectedName ?? "(null)") \
            rssi=\(RSSI)
            """
        )

        if let expectedName {
            let matches = peripheralName == expectedName || advertisedName == expectedName
            if !matches {
                print("BleBridge.didDiscover skipped by name filter")
                return
            }
        }

        print("BleBridge.didDiscover matched target, connecting")

        peripheral = discoveredPeripheral
        discoveredPeripheral.delegate = self
        central.stopScan()
        central.connect(discoveredPeripheral, options: nil)
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("BleBridge.didConnect name=\(peripheral.name ?? "(null)")")
        self.peripheral = peripheral
        peripheral.delegate = self

        guard let targetServiceUuid else { return }
        peripheral.discoverServices([targetServiceUuid])
    }

    func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        print("BleBridge.didFailToConnect error=\(String(describing: error))")
    }

    func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        print("BleBridge.didDisconnect error=\(String(describing: error))")
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        print("BleBridge.didDiscoverServices error=\(String(describing: error))")

        guard let services = peripheral.services else { return }
        guard let service = services.first(where: { $0.uuid == targetServiceUuid }) else {
            print("BleBridge: target service not found")
            return
        }

        peripheral.discoverCharacteristics([
                                               CBUUID(string: "12345678-1234-5678-1234-56789abc0001"),
                                               CBUUID(string: "12345678-1234-5678-1234-56789abc0002"),
                                               CBUUID(string: "12345678-1234-5678-1234-56789abc0003"),
                                               CBUUID(string: "12345678-1234-5678-1234-56789abc0004"),
                                           ], for: service)
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        print("BleBridge.didDiscoverCharacteristics error=\(String(describing: error))")

        service.characteristics?.forEach { ch in
            switch ch.uuid.uuidString.lowercased() {
            case "12345678-1234-5678-1234-56789abc0001":
                currentStatusChar = ch
            case "12345678-1234-5678-1234-56789abc0002":
                alarmEventChar = ch
            case "12345678-1234-5678-1234-56789abc0003":
                alarmHistoryChar = ch
            case "12345678-1234-5678-1234-56789abc0004":
                controlChar = ch
            default:
                break
            }
        }

        if let currentStatusChar {
            print("BleBridge.subscribe current_status")
            peripheral.setNotifyValue(true, for: currentStatusChar)
        }

        if let alarmEventChar {
            print("BleBridge.subscribe alarm_event")
            peripheral.setNotifyValue(true, for: alarmEventChar)
        }

        if let alarmHistoryChar {
            print("BleBridge.read alarm_history")
            peripheral.readValue(for: alarmHistoryChar)
        }
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        print("BleBridge.didUpdateNotificationState uuid=\(characteristic.uuid.uuidString) notifying=\(characteristic.isNotifying) error=\(String(describing: error))")
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        let text = characteristic.value.flatMap { String(data: $0, encoding: .utf8) }
        print("BleBridge.didUpdateValue uuid=\(characteristic.uuid.uuidString) text=\(String(describing: text)) error=\(String(describing: error))")

        guard let text else { return }

        switch characteristic.uuid.uuidString.lowercased() {
        case "12345678-1234-5678-1234-56789abc0001":
            interop?.emitCurrentStatusJson(payload: text)
        case "12345678-1234-5678-1234-56789abc0002":
            interop?.emitAlarmEventJson(payload: text)
        default:
            break
        }
    }
}