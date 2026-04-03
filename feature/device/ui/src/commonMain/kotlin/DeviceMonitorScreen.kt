package ru.aokruan.hmlkbi.feature.device.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceMonitorScreen(
    viewModel: DeviceMonitorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Медицинское BLE-устройство",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        Button(
            onClick = viewModel::onScanQrClicked,
        ) {
            Text("Сканировать QR и подключить")
        }

        state.currentStatus?.let { status ->
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Текущий статус", style = MaterialTheme.typography.titleMedium)
                    Text("Heart rate: ${status.heartRate}")
                    Text("SpO2: ${status.spo2}")
                    Text("Pressure: ${status.systolic ?: "-"} / ${status.diastolic ?: "-"}")
                    Text("Battery: ${status.batteryPercent ?: "-"}")
                    Text("Critical: ${status.critical}")
                    Text("State: ${status.state}")
                }
            }
        }

        state.lastAlarm?.let { alarm ->
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Последняя тревога", style = MaterialTheme.typography.titleMedium)
                    Text("Severity: ${alarm.severity}")
                    Text("Code: ${alarm.code}")
                    Text("Message: ${alarm.message}")
                    Text("Active: ${alarm.active}")
                    Button(
                        onClick = viewModel::onAcknowledgeAlarmClicked,
                    ) {
                        Text("ACK тревоги")
                    }
                }
            }
        }
    }
}