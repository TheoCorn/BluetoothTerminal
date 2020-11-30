package com.tcorp.cap.testapp.ui.devices

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tcorp.cap.testapp.R
import com.tcorp.cap.testapp.bluetooth.Bluetooth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class DevicesFragment : Fragment() {

    private lateinit var devicesList: RecyclerView

    private val itemList = mutableListOf<BluetoothDevice>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val conFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        requireActivity().registerReceiver(btConReceiver, conFilter)

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(btDeviceFoundRec, foundFilter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        checkPermission()


        val root = inflater.inflate(R.layout.fragment_devices, container, false)


        devicesList = root.findViewById(R.id.DevicesList)
        setUp()

        val refreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.refreshLayout)

        refreshLayout.setOnRefreshListener {
            setUp()
            refreshLayout.isRefreshing = false
        }

        BluetoothAdapter.getDefaultAdapter().startDiscovery()

        return root
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onDestroy() {
        super.onDestroy()
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        requireActivity().unregisterReceiver(btConReceiver)
        requireActivity().unregisterReceiver(btDeviceFoundRec)
    }


    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private fun setUp() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Bluetooth.requestBluetoothON()
        }
        itemList.addAll(BluetoothAdapter.getDefaultAdapter().bondedDevices.toList())
        devicesList.adapter = DeviceListAdapter(itemList)
        devicesList.layoutManager = LinearLayoutManager(context)
        devicesList.setHasFixedSize(false)
    }



    private fun checkPermission() {
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            BluetoothAdapter.getDefaultAdapter().startDiscovery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        try {
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) BluetoothAdapter.getDefaultAdapter()
                    .startDiscovery()
            }
        }catch (ex: Exception){checkPermission()}
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private val btConReceiver = object : BroadcastReceiver() {
        @ExperimentalCoroutinesApi
        @InternalCoroutinesApi
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_OFF -> Bluetooth.requestBluetoothON()
                BluetoothAdapter.STATE_ON -> {
                    BluetoothAdapter.getDefaultAdapter().startDiscovery(); setUp()
                }
            }
        }
    }

    private val btDeviceFoundRec = object : BroadcastReceiver() {
        @ExperimentalCoroutinesApi
        @InternalCoroutinesApi
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (!itemList.contains(device) && device != null) {
                        itemList.add(0, device)
                        devicesList.adapter?.notifyItemInserted(itemList.lastIndex)
                    }
                }
            }
        }
    }
}