package com.tcorp.cap.testapp.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.nio.charset.Charset
import java.time.LocalTime
import java.util.*


private const val BLUETOOTH_SPP = "00001101-0000-1000-8000-00805F9B34FB"
private const val TAG = "BluetoothManager"

/**
 * An easy way to create and manage bluetooth SPP sockets
 * @param btDevice a BluetoothDevice you want to connect
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class BluetoothManager(private val btDevice: BluetoothDevice, startInput: Boolean = true) {
    
    /**
     * listener for BluetoothManager
     */
    interface BTListener {
        fun onConnect(btDevice: BluetoothDevice)
        suspend fun onDisconnect(btDevice: BluetoothDevice)
        fun onDisconnectedByUser(btDevice: BluetoothDevice)
        suspend fun onConnectionError(btDevice: BluetoothDevice)
        fun onNoBluetooth()
        
    }
    
    private val listener = Bluetooth as BTListener
    private var readerPipeline: Job? = null

    private var btsocket: BluetoothSocket? = null
    
    
    init {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            CoroutineScope(IO).launch { connectToDevice(btDevice, startInput) }
        } else {
            listener.onNoBluetooth()

        }
    }
    
    
    @InternalCoroutinesApi
    private suspend fun connectToDevice(device: BluetoothDevice, startInput: Boolean) {
        
        try {
            withTimeout(15000) {

                val socket =
                    device.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_SPP))
                socket.connect()

                btsocket = socket

                CoroutineScope(Main).launch { listener.onConnect(btDevice) }
                if (startInput) {
                    readerPipeline = CoroutineScope(IO).launch { startInputStreamPipeline(btsocket!!) }
                }
            }
            
            
        } catch (e: Exception) {
            listener.onConnectionError(btDevice)
        }

    }


    @ExperimentalCoroutinesApi
    private suspend fun CoroutineScope.startInputStreamPipeline(socket: BluetoothSocket) {
        try {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var len: Int?
            while (isActive) {
                len = socket.inputStream.read(buffer)
                if (len != 0) {
                    val data = String(buffer.copyOf(len))
                    val time = LocalTime.now()
                    Bluetooth.inputChannel.send(Pair(data, time.toString()))
                }
            }
        } catch (ex: Exception) {
            try {
                btsocket?.close()
            } catch (ex: Exception) {
            } finally {
                btsocket = null
    
                listener.onDisconnect(btDevice)
            }
        }
    }

    fun startInput(){
        if (readerPipeline?.isActive != true) {
            readerPipeline = CoroutineScope(IO).launch { startInputStreamPipeline(btsocket!!) }
        }
    }

    fun stopInput(){
        if (readerPipeline?.isActive == true){
            readerPipeline?.cancel()
        }
    }
    
    
    //write is an kotlin coroutine thus can not be called from java use runWrite()
    /**
     * Dispatcher IO best practice
     * @param data is a String that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: String) = Dispatchers.Default {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data.toByteArray(Charset.defaultCharset()))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a ByteArray that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: ByteArray) = Dispatchers.Default {
        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data) ?: Log.d(TAG, "socket not connected in write")
        } else {
            Log.d(TAG, "socket not connected in write")
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a Int that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Int) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data)
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a char that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Char) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(byteArrayOf(data.toByte()))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a mutableList<*> that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Byte) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(byteArrayOf(data))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a mutableList<*> that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: MutableList<*>) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            for (d in data) {
                btsocket?.outputStream?.write(d.toString().toByteArray())
            }
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a List<*> that will be send via BT
     */
    @JvmName("write1")
    @ExperimentalCoroutinesApi
    suspend fun write(data: List<*>) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            for (d in data) {
                btsocket?.outputStream?.write(d.toString().toByteArray())
            }
        }
    }
    
    
    suspend fun disconnect() = Dispatchers.IO {
        try {
            readerPipeline?.cancel()
            btsocket?.close()
        } finally {
            btsocket = null
            CoroutineScope(Main).launch { listener.onDisconnectedByUser(btDevice) }
        }
    }
    
    
}