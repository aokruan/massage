package ru.aokruan.hmlkbi

sealed interface BleSessionState {
    data object Idle : BleSessionState
    data object Scanning : BleSessionState
    data object Connecting : BleSessionState
    data object DiscoveringServices : BleSessionState
    data object NegotiatingMtu : BleSessionState
    data object SubscribingCurrentStatus : BleSessionState
    data object SubscribingAlarmEvent : BleSessionState
    data object ReadingAlarmHistory : BleSessionState
    data object Ready : BleSessionState
    data object Reconnecting : BleSessionState
    data class Failed(val message: String) : BleSessionState
}