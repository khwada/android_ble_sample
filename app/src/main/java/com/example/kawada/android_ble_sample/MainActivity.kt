package com.example.kawada.android_ble_sample

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private val context: Context
        get() {
            return this
        }
    private val bluetoothAdapter: BluetoothAdapter
        get() {
            val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            return bluetoothManager.adapter
        }
    private val bluetoothScanner: BluetoothLeScanner
        get() {
            return bluetoothAdapter.bluetoothLeScanner
        }

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            Log.i("TAG", "name=${device?.name} address=${device?.address} rssi=${result?.rssi}")
            if (device?.name != null && device.name.contains("SensorTag")) {
                device.connectGatt(context, false, SensorTagBluetoothGatCallback())
            }
        }
    }

    private val scanCallBackForCreateBond = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result?.device?.name != null && result.device.name.contains("SensorTag")) {
                Log.i("TAG", result.toString())
                Log.i("TAG", result.scanRecord.bytes.joinToString (transform= {String.format("%02X", it)}))
                bluetoothScanner.stopScan(this)
                Log.i("TAG", "scan stopped")
                if (result.device.bondState == BluetoothDevice.BOND_NONE) {
                    result.device.createBond()
                    val myToast = Toast.makeText(context, "create bond with SensorTag", Toast.LENGTH_LONG)
                    myToast.show()
                } else {
                    val myToast = Toast.makeText(context, "create bond skipped: ${result.device.bondState}", Toast.LENGTH_LONG)
                    myToast.show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }

    fun bondedList(view: View) {
        // ペアリング済みのデバイスを表示
        val bondedList = bluetoothAdapter.bondedDevices
        val myToast = Toast.makeText(context, bondedList.joinToString(transform = {it.name}), Toast.LENGTH_LONG)
        myToast.show()
    }

    fun createBondSensorTag(views: View) {
        bluetoothScanner.startScan(scanCallBackForCreateBond)
    }

    fun readBondedSensorTag(views: View) {
        val bondedList = bluetoothAdapter.bondedDevices
        bondedList.find { it.name == "CC2650 SensorTag" }?.connectGatt(context, false, SensorTagBluetoothGatCallback())
    }

    fun removeSensorTag(views: View) {
        val bondedList = bluetoothAdapter.bondedDevices
        bondedList.filter { it.name == "CC2650 SensorTag" }.forEach{removeBond(it)}
        val myToast = Toast.makeText(context, "removed", Toast.LENGTH_LONG)
        myToast.show()
    }

    private fun removeBond(device: BluetoothDevice) {
        val clazz = BluetoothDevice::class.java
        val method = clazz.getDeclaredMethod("removeBond")
        method.isAccessible = true
        method.invoke(device)
    }

    fun startScan(views: View) {
        bluetoothScanner.startScan(scanCallback)
        Log.i("TAG", "scan started")
        val myToast = Toast.makeText(context, "scan started", Toast.LENGTH_LONG)
        myToast.show()
    }

    fun stopScan(views: View) {
        bluetoothScanner.stopScan(scanCallback)
        bluetoothScanner.stopScan(scanCallBackForCreateBond)
        Log.i("TAG", "scan stopped")
        val myToast = Toast.makeText(context, "scan stopped", Toast.LENGTH_LONG)
        myToast.show()
    }

}
