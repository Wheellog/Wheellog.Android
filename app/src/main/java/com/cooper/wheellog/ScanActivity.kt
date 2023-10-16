package com.cooper.wheellog

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cooper.wheellog.databinding.ActivityScanBinding
import com.cooper.wheellog.utils.PermissionsUtil
import com.cooper.wheellog.utils.StringUtil
import com.cooper.wheellog.utils.StringUtil.toHexStringRaw
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.Transport
import timber.log.Timber


class ScanActivity: AppCompatActivity() {
    private var mDeviceListAdapter: DeviceListAdapter? = null
    private val central: BluetoothCentralManager by lazy {
        BluetoothCentralManager(
            this,
            bluetoothCentralManagerCallback,
            Handler(Looper.getMainLooper())
        ).apply { transport = Transport.LE }
    }
    private var pb: ProgressBar? = null
    private var scanTitle: TextView? = null
    // Stops scanning after 10 seconds.
    private val scanPeriodHandler = Handler(Looper.getMainLooper())
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
                binding.lastMacText.error = "incorrect MAC"
                binding.lastMacText.errorIconDrawable = null
                return@setEndIconOnClickListener
            }
            if (central.isScanning) {
                scanLeDevice(false)
            }
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
                        if (central.isScanning) {
                            scanLeDevice(false)
                        }
                        dialogInterface.cancel()
                        close()
                    }
                    false
                }
                .create()
        window.attributes = alertDialog.window?.attributes?.apply {
            gravity = Gravity.TOP
            flags = flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        }
        alertDialog.show()
        if (!isLocationEnabled(this)) {
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }
    }

    private val bluetoothCentralManagerCallback: BluetoothCentralManagerCallback =
        object : BluetoothCentralManagerCallback() {
            override fun onDiscoveredPeripheral(
                peripheral: BluetoothPeripheral,
                scanResult: ScanResult
            ) {
                super.onDiscoveredPeripheral(peripheral, scanResult)
                val scanRecord = scanResult.scanRecord?.bytes ?: byteArrayOf()
                if (scanResult.scanRecord != null) {
                    val manufacturerData = findManufacturerData(scanRecord) // 4e421300000000ec
                    runOnUiThread {
                        mDeviceListAdapter!!.addDevice(scanResult.device, manufacturerData)
                        mDeviceListAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }

    private fun close () {
        central.stopScan()
        alertDialog.dismiss()
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (central.isBluetoothEnabled) {
            if (!PermissionsUtil.checkBlePermissions(this)) {
                if (PermissionsUtil.isMaxBleReq) {
                    killMe()
                }
                return
            }
            scanLeDevice(true)
        } else {
            if (PermissionsUtil.checkBlePermissions(this)) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                ActivityCompat.startActivityForResult(this, enableBtIntent, 2, null)
            } else {
                killMe()
            }
        }
    }

    private fun killMe() {
        central.close()
        alertDialog.dismiss()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all {r -> r == PackageManager.PERMISSION_GRANTED}) {
            scanLeDevice(true)
        }
    }

    @SuppressLint("MissingPermission")
    private val onItemClickListener = OnItemClickListener { _, _, i, _ ->
        if (central.isScanning) {
            central.stopScan()
        }
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

    private infix fun Byte.eq(i: Int): Boolean = this == i.toByte()

    private fun findManufacturerData(scanRecord: ByteArray): String {
        var index = 0
        var result = ""
        while (index < scanRecord.size) {
            val length = scanRecord[index++]
            // Done once we run out of records
            val toIndex = index + length
            if (length eq 0 || toIndex > scanRecord.size) {
                break
            }
            val type = scanRecord[index]
            // Done if our record isn't a valid type
            if (type eq 0) {
                break
            }
            val data = scanRecord.copyOfRange(index + 1, toIndex)

            // Advance
            index = toIndex
            if (type eq -1) {
                result = toHexStringRaw(data)
            }
        }
        Timber.i("Found data: %s", result)
        return result
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanPeriodHandler.postDelayed({ scanLeDevice(false) }, scanPeriod)
            central.stopScan()
            central.scanForPeripherals()
            pb!!.visibility = View.VISIBLE
            scanTitle!!.setText(R.string.scanning)
            macLayout.visibility = View.GONE
        } else {
            scanPeriodHandler.removeCallbacksAndMessages(null)
            central.stopScan()
            pb!!.visibility = View.GONE
            scanTitle!!.setText(R.string.devices)
            macLayout.visibility = View.VISIBLE
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            val lm = context.getSystemService(LOCATION_SERVICE) as LocationManager
            lm.isLocationEnabled
        } else {
            // This is Deprecated in API 28
            val locationMode = try {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}