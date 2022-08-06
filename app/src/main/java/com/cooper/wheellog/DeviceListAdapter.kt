package com.cooper.wheellog

import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.BaseAdapter

// Adapter for holding devices found through scanning.
class DeviceListAdapter(appCompatActivity: AppCompatActivity) : BaseAdapter() {
    private val mLeDevices = mutableListOf<BluetoothDevice>()
    private val mLeAdvDatas = mutableListOf<String>()
    private val mInflator: LayoutInflater

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    init {
        mInflator = appCompatActivity.layoutInflater
    }

    fun addDevice(device: BluetoothDevice, advData: String) {
        if (!WheelLog.AppConfig.showUnknownDevices) {
            if (device.name.isNullOrEmpty()) {
                return
            }
        }
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device)
            mLeAdvDatas.add(advData)
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return mLeDevices[position]
    }

    fun getAdvData(position: Int): String {
        return mLeAdvDatas[position]
    }

    // public void clear() {
    //    mLeDevices.clear();
    // }
    override fun getCount(): Int {
        return mLeDevices.size
    }

    override fun getItem(i: Int): Any {
        return mLeDevices[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        var view = view
        val viewHolder: ViewHolder
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.scan_list_item, null)

            viewHolder = ViewHolder().apply {
                deviceAddress = view.findViewById(R.id.device_address)
                deviceName = view.findViewById(R.id.device_name)
            }

            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        val device = mLeDevices[i]
        val deviceName = device.name

        if (!deviceName.isNullOrEmpty()) {
            viewHolder.deviceName!!.text = deviceName
        } else {
            viewHolder.deviceName!!.setText(R.string.unknown_device)
        }

        viewHolder.deviceAddress!!.text = device.address
        return requireNotNull(view)
    }
}