package com.example.kawada.android_ble_sample

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import java.util.*

class SensorTagBluetoothGatCallback : BluetoothGattCallback() {
    companion object {
        private val UUID_HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000")
        private val UUID_HUMIDITY_CHARACTERISTIC_DATA = UUID.fromString("f000aa21-0451-4000-b000-000000000000")
        private val UUID_HUMIDITY_CHARACTERISTIC_CONFIGURATION = UUID.fromString("f000aa22-0451-4000-b000-000000000000")
        private val UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.i("TAG", "stateChange status=$status state=$newState")
        gatt?.discoverServices()
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.i("TAG", "servicesDiscovered [${gatt?.services?.joinToString(transform = {it.uuid.toString()})}]")
        val batteryCharacteristic = gatt?.getService(UUID_BATTERY_SERVICE)?.getCharacteristic(UUID_BATTERY_CHARACTERISTIC_BATTERY_LEVEL)
        // properties=18 -> PROPERTY_READ:2 + PROPERTY_NOTIFY:16
        // https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic#PROPERTY_NOTIFY
        Log.i("TAG", "batteryCharacteristic properties=${batteryCharacteristic?.properties} descriptors=${batteryCharacteristic?.descriptors?.joinToString { it.uuid.toString() }}")
        gatt?.readCharacteristic(batteryCharacteristic)
        // この時点で読んでもnull
        Log.i("TAG", "battery level=${batteryCharacteristic?.value?.joinToString(transform = {String.format("%02X", it)})}")
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        when (characteristic?.uuid) {
            UUID_BATTERY_CHARACTERISTIC_BATTERY_LEVEL -> {
                if (characteristic?.value != null) {
                    val batteryLevel = characteristic.value[0].toInt()
                    Log.i("TAG", "onCharacteristicRead battery level=$batteryLevel %")
                }
                enableHumidityCollection(gatt)
            }
        }
    }

    // 以下、湿度測定値をサーバ主導更新で受け取る（先にセンサーをONにしている）
    private fun enableHumidityCollection(gatt: BluetoothGatt?) {
        val characteristic = gatt?.getService(UUID_HUMIDITY_SERVICE)?.getCharacteristic(UUID_HUMIDITY_CHARACTERISTIC_CONFIGURATION)
        // properties=10 -> PROPERTY_READ:2 + PROPERTY_WRITE:8
        Log.i("TAG", "humidityConfigufationCharacteristic properties=${characteristic?.properties} descriptors=${characteristic?.descriptors?.joinToString { it.uuid.toString() }}")
        characteristic?.value = byteArrayOf(0x01)
        gatt?.writeCharacteristic(characteristic)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        Log.i("TAG", "onCharacteristicWrite")
        enableHumidityNotification(gatt)
    }

    private fun enableHumidityNotification(gatt: BluetoothGatt?) {
        val characteristic = gatt?.getService(UUID_HUMIDITY_SERVICE)?.getCharacteristic(UUID_HUMIDITY_CHARACTERISTIC_DATA)
        gatt?.setCharacteristicNotification(characteristic, true)

        // Client Characteristic Configuration Description
        val descriptor = characteristic?.getDescriptor(UUID_CCCD)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt?.writeDescriptor(descriptor)
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.i("TAG", "onDescriptorWrite")
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        if (characteristic?.value != null) {
            // http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User%27s_Guide#Humidity_Sensor
            val temperature = ((characteristic.value[1].toInt().shl(8) + characteristic.value[0].toInt()).toDouble() / 65536) * 165 - 40
            val humidity = ((characteristic.value[3].toInt().shl(8) + characteristic.value[2].toInt()).toDouble() / 65536) * 100
            Log.i("TAG", "onCharacteristicChanged temperature=$temperature °C humidity=$humidity %RH" )
        }
    }
}