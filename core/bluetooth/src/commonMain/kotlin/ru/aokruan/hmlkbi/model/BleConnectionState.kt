package ru.aokruan.hmlkbi.model

sealed interface BleConnectionState {
    data object Idle : BleConnectionState
    data object CheckingPermissions : BleConnectionState
    data object Scanning : BleConnectionState
    data class DeviceFound(val name: String) : BleConnectionState
    data object Connecting : BleConnectionState
    data object DiscoveringServices : BleConnectionState
    data object Subscribing : BleConnectionState
    data object Connected : BleConnectionState
    data class Failed(val reason: BleConnectError) : BleConnectionState
}

sealed interface BleConnectError {
    data object InvalidQr : BleConnectError
    data object PermissionDenied : BleConnectError
    data object BluetoothDisabled : BleConnectError
    data object DeviceNotFound : BleConnectError
    data object ServiceNotFound : BleConnectError
    data object CharacteristicNotFound : BleConnectError
    data class Platform(val message: String) : BleConnectError
}