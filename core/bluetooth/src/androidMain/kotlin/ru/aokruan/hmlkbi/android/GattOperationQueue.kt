package ru.aokruan.hmlkbi.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import kotlinx.coroutines.CompletableDeferred
import java.util.ArrayDeque

internal sealed class GattOperation(
    open val deferred: CompletableDeferred<Boolean>,
) {
    data class RequestMtu(
        val mtu: Int,
        override val deferred: CompletableDeferred<Boolean>,
    ) : GattOperation(deferred)

    data class WriteDescriptor(
        val descriptor: BluetoothGattDescriptor,
        val value: ByteArray,
        override val deferred: CompletableDeferred<Boolean>,
    ) : GattOperation(deferred)

    data class ReadCharacteristic(
        val characteristic: BluetoothGattCharacteristic,
        override val deferred: CompletableDeferred<Boolean>,
    ) : GattOperation(deferred)

    data class WriteCharacteristic(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray,
        override val deferred: CompletableDeferred<Boolean>,
    ) : GattOperation(deferred)
}

internal class GattOperationQueue {
    private val queue = ArrayDeque<GattOperation>()
    private var inFlight: GattOperation? = null

    fun clear() {
        inFlight?.deferred?.complete(false)
        inFlight = null

        while (queue.isNotEmpty()) {
            queue.removeFirst().deferred.complete(false)
        }
    }

    fun enqueue(operation: GattOperation) {
        queue.addLast(operation)
    }

    @SuppressLint("MissingPermission")
    fun drain(gatt: BluetoothGatt): Boolean {
        if (inFlight != null) return true
        if (queue.isEmpty()) return true

        val op = queue.removeFirst()
        inFlight = op

        val started = when (op) {
            is GattOperation.RequestMtu -> {
                gatt.requestMtu(op.mtu)
            }

            is GattOperation.WriteDescriptor -> {
                op.descriptor.value = op.value
                gatt.writeDescriptor(op.descriptor)
            }

            is GattOperation.ReadCharacteristic -> {
                gatt.readCharacteristic(op.characteristic)
            }

            is GattOperation.WriteCharacteristic -> {
                op.characteristic.value = op.value
                gatt.writeCharacteristic(op.characteristic)
            }
        }

        if (!started) {
            op.deferred.complete(false)
            inFlight = null
            return false
        }

        return true
    }

    fun completeCurrent(success: Boolean) {
        val current = inFlight ?: return
        current.deferred.complete(success)
        inFlight = null
    }
}