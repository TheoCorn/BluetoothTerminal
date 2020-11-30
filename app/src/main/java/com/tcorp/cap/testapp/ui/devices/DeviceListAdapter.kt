package com.tcorp.cap.testapp.ui.devices

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.tcorp.cap.testapp.R
import com.tcorp.cap.testapp.bluetooth.Bluetooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch


class DeviceListAdapter(private val deviceList: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
		val itemView = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
		return DeviceViewHolder(itemView)
	}
	
	@ExperimentalCoroutinesApi
	@InternalCoroutinesApi
	override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
		holder.switch.isChecked = false
		val device = deviceList[position]
		
		holder.txtName.text = device.name
		holder.txtAddress.text = device.address
		
		
		if (Bluetooth.connected(device) == true) {
			holder.switch.isChecked = true
		}
		holder.switch.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				Bluetooth.connect(device)
			} else {
				CoroutineScope(IO).launch { Bluetooth.disconnect(device) }
				holder.switch.isChecked = false
			}
		}
		
		
		Bluetooth.mapManagerAndDevice.observeForever {
			if (holder.switch.isChecked && it?.containsKey(device) == false) {
				holder.switch.isChecked = false
			}
		}
	}
	
	override fun getItemCount(): Int = deviceList.size
	
	class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val txtName: TextView
		val txtAddress: TextView
		val switch: SwitchCompat
		
		init {
			txtName = itemView.findViewById(R.id.txtName)
			txtAddress = itemView.findViewById(R.id.txtAddress)
			switch = itemView.findViewById(R.id.con)
		}
	}
}