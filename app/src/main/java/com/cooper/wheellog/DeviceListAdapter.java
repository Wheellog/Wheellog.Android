package com.cooper.wheellog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

// Adapter for holding devices found through scanning.
public class DeviceListAdapter extends BaseAdapter {
    private final ArrayList<BluetoothDevice> mLeDevices;
    private final ArrayList<String> mLeAdvDatas;
    private final LayoutInflater mInflator;

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public DeviceListAdapter(AppCompatActivity appCompatActivity) {
        super();
        mLeDevices = new ArrayList<>();
        mLeAdvDatas = new ArrayList<>();
        mInflator = appCompatActivity.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device, String advData) {
        if (!WheelLog.AppConfig.getShowUnknownDevices()) {
            @SuppressLint("MissingPermission")
            String deviceName = device.getName();
            if (deviceName == null || deviceName.length() == 0)
                return;
        }

        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
            mLeAdvDatas.add(advData);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public String getAdvData(int position) {
        return mLeAdvDatas.get(position);
    }
//    public void clear() {
//        mLeDevices.clear();
//    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.scan_list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = view.findViewById(R.id.device_address);
            viewHolder.deviceName = view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        @SuppressLint("MissingPermission")
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }
}