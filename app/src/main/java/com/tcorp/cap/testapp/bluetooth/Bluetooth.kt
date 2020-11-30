package com.tcorp.cap.testapp.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel

private const val REQUEST_BLUETOOTH_ON = 1

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
object Bluetooth : BluetoothManager.BTListener {
	
	private val TAG = "Bluetooth obj"
	
	lateinit var appContext: Context
	lateinit var activity: Activity
	
	
	val inputChannel = Channel<Pair<String, String>>(Channel.UNLIMITED)
	
	//managing bluetooth
	private val _mapManagerAndDevice = MutableLiveData<HashMap<BluetoothDevice, BluetoothManager>>().apply { value = hashMapOf<BluetoothDevice, BluetoothManager>() }
	val mapManagerAndDevice: LiveData<HashMap<BluetoothDevice, BluetoothManager>> = _mapManagerAndDevice
	private val disconnectedDevicesToReconnect = mutableListOf<BluetoothDevice>()
	
	/**
	 * @param btDevice device to connect
	 */
	fun connect(btDevice: BluetoothDevice) {
		val localMap = _mapManagerAndDevice.value
		localMap?.set(btDevice, BluetoothManager(btDevice))
		_mapManagerAndDevice.value = localMap
	}
	
	@JvmName("connect1")
	fun BluetoothDevice.connect() {
		val localMap = _mapManagerAndDevice.value
		localMap?.set(this, BluetoothManager(this))
		_mapManagerAndDevice.value = localMap
	}
	
	suspend fun disconnect(btDevice: BluetoothDevice) {
		val localMap = _mapManagerAndDevice.value
		localMap?.get(btDevice)?.disconnect()
		localMap?.remove(btDevice)
		_mapManagerAndDevice.postValue(localMap)
	}
	
	
	fun reconnectAll() {
		disconnectedDevicesToReconnect.forEach { it.connect() }
	}
	
	fun BluetoothDevice.bluetoothManager() = _mapManagerAndDevice.value?.get(this)
	
	fun requestBluetoothON() {
		Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).also {
			startActivityForResult(activity, it, REQUEST_BLUETOOTH_ON, null)
		}
	}

	private fun remove(btDevice: BluetoothDevice){
		val localMap = _mapManagerAndDevice.value
		localMap?.remove(btDevice)
		_mapManagerAndDevice.postValue(localMap)
		disconnectedDevicesToReconnect.add(btDevice)
	}
	
	
	/**
	 * returns true if the device is connected
	 * @return Boolean
	 */
	fun connected(btDevice: BluetoothDevice) = _mapManagerAndDevice.value?.containsKey(btDevice)
	
	
	override fun onConnect(btDevice: BluetoothDevice) {
		Toast.makeText(appContext, "Connected to ${btDevice.name}", Toast.LENGTH_SHORT).show()
	}
	
	override suspend fun onDisconnect(btDevice: BluetoothDevice) {
		remove(btDevice)
		CoroutineScope(Main).launch { Toast.makeText(appContext, "Disconnected from ${btDevice.name}", Toast.LENGTH_SHORT).show() }
	}
	
	override fun onDisconnectedByUser(btDevice: BluetoothDevice) {
		Toast.makeText(appContext, "Disconnected from ${btDevice.name}", Toast.LENGTH_SHORT).show()
	}
	
	override suspend fun onConnectionError(btDevice: BluetoothDevice) {
		remove(btDevice)
		CoroutineScope(Main).launch { Toast.makeText(appContext, "unable to connect to ${btDevice.name}", Toast.LENGTH_SHORT).show() }
	}
	
	override fun onNoBluetooth() {
		_mapManagerAndDevice.apply { value = hashMapOf<BluetoothDevice, BluetoothManager>() }
		Toast.makeText(appContext, "No bluetooth on device", Toast.LENGTH_SHORT).show()
	}
}