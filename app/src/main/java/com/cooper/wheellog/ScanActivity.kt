package com.cooper.wheellog

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cooper.wheellog.databinding.ActivityScanBinding
import com.cooper.wheellog.utils.StringUtil
import com.cooper.wheellog.utils.StringUtil.Companion.toHexStringRaw
import timber.log.Timber

class ScanActivity: AppCompatActivity() {
    private var mDeviceListAdapter: DeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private val mHandler = Handler(Looper.getMainLooper())
    private var pb: ProgressBar? = null
    private var scanTitle: TextView? = null
    // Stops scanning after 10 seconds.
    private val scanPeriod: Long = 10_000
    private lateinit var alertDialog: AlertDialog
    private lateinit var macLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityScanBinding.inflate(layoutInflater, null, false)
        pb = binding.scanProgress
        scanTitle = binding.scanTitle
        mDeviceListAdapter = DeviceListAdapter(this)
        binding.list.onItemClickListener = onItemClickListener
        binding.list.adapter = mDeviceListAdapter
        macLayout = binding.lastMacText
        binding.lastMacText.editText!!.setText(WheelLog.AppConfig.lastMac)
        binding.lastMacText.setEndIconOnClickListener {
            val deviceAddress = binding.lastMacText.editText?.text.toString()
            if (!StringUtil.isCorrectMac(deviceAddress)) {
                return@setEndIconOnClickListener
            }
            if (mScanning) {
                scanLeDevice(false)
            }
            mHandler.removeCallbacksAndMessages(null)
            val intent = Intent()
            intent.putExtra("MAC", deviceAddress)
            WheelLog.AppConfig.lastMac = deviceAddress
            setResult(RESULT_OK, intent)
            WheelLog.AppConfig.passwordForWheel = ""
            close()
        }
        alertDialog = AlertDialog.Builder(this, R.style.OriginalTheme_Dialog_Alert)
                .setView(binding.root)
                .setCancelable(false)
                .setOnKeyListener { dialogInterface: DialogInterface, keycode: Int, keyEvent: KeyEvent ->
                    if (keycode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP &&
                            !keyEvent.isCanceled) {
                        if (mScanning) scanLeDevice(false)
                        mHandler.removeCallbacksAndMessages(null)
                        dialogInterface.cancel()
                        close()
                    }
                    false
                }
                .show()
        if (!isLocationEnabled(this)) {
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }

    private fun close () {
        alertDialog.dismiss()
        finish()
    }

    override fun onResume() {
        super.onResume()
        scanLeDevice(true)
    }

    private val onItemClickListener = OnItemClickListener { _, _, i, _ ->
        if (mScanning) scanLeDevice(false)
        mHandler.removeCallbacksAndMessages(null)
        val device = mDeviceListAdapter!!.getDevice(i)
        val deviceAddress = device.address
        val deviceName = device.name
        val advData = mDeviceListAdapter!!.getAdvData(i)
        Timber.i("Device selected MAC = %s", deviceAddress)
        Timber.i("Device selected Name = %s", deviceName)
        Timber.i("Device selected Data = %s", advData)
        val intent = Intent()
        intent.putExtra("MAC", deviceAddress)
        intent.putExtra("NAME", deviceName)
        WheelLog.AppConfig.lastMac = deviceAddress
        WheelLog.AppConfig.advDataForWheel = advData
        setResult(RESULT_OK, intent)
        // Set password for inmotion
        WheelLog.AppConfig.passwordForWheel = ""
        close()
    }

    // Device scan callback.
    private val mLeScanCallback = LeScanCallback { device, _, scanRecord ->
        val manufacturerData = findManufacturerData(scanRecord) // 4e421300000000ec
        runOnUiThread {
            mDeviceListAdapter!!.addDevice(device, manufacturerData)
            mDeviceListAdapter!!.notifyDataSetChanged()
        }
    }

    private fun findManufacturerData(scanRecord: ByteArray): String {
        var index = 0
        var result = ""
        while (index < scanRecord.size) {
            val length = scanRecord[index++].toInt()
            // Done once we run out of records
            if (length == 0) break
            val type = scanRecord[index].toInt()
            // Done if our record isn't a valid type
            if (type == 0) break
            val data = scanRecord.copyOfRange(index + 1, index + length)

            // Advance
            index += length
            if (type == -1) {
                result = toHexStringRaw(data)
            }
        }
        Timber.i("Found data: %s", result)
        return result
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed({ scanLeDevice(false) }, scanPeriod)
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
            pb!!.visibility = View.VISIBLE
            scanTitle!!.setText(R.string.scanning)
            macLayout.visibility = View.GONE
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            pb!!.visibility = View.GONE
            scanTitle!!.setText(R.string.devices)
            macLayout.visibility = View.VISIBLE
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        } else {
            locationProviders = Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
            !TextUtils.isEmpty(locationProviders)
        }
    }
}