package com.cooper.wheellog.utils

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.widget.Toast
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.SomeUtil.Companion.playSound
import com.cooper.wheellog.utils.StringUtil.Companion.getRawTextResource
import com.cooper.wheellog.utils.StringUtil.Companion.toHexStringRaw
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class BleConnector(val context: Context) {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    private var mDisconnectTime: Date? = null
    private var reconnectTimer: Timer? = null

    private var disconnectRequested = false
    private var autoConnect = false

    private var beepTimer: Timer? = null
    private var timerTicks = 0
    var mgr: PowerManager? = null
    var wl: WakeLock? = null
    private var fileUtilRawData: FileUtil? = null
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    private val sdf2 = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val wakeLogTag = "WhellLog:WakeLockTag"

    var connectionState = BleStateEnum.Disconnected

    fun setDeviceAddress(address: String?) {
        if (!address.isNullOrEmpty()) {
            mBluetoothDeviceAddress = address
        }
    }

    fun toggleConnectToWheel() {
        if (connectionState == BleStateEnum.Disconnected) {
            connect()
        } else {
            disconnect()
            close()
        }
    }

    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic?) {
        Timber.i("Set characteristic start")
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.i("BluetoothAdapter not initialized")
            return
        }
        val success = mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)
        Timber.i("Set characteristic %b", success)
    }

    private fun toggleReconnectToWheel() {
        if (connectionState == BleStateEnum.Connected) {
            Timber.wtf("Trying to reconnect")
            // After disconnect, the method onConnectionStateChange will automatically reconnect
            // because disconnectRequested is false
            disconnectRequested = false
            mBluetoothGatt!!.disconnect()
            broadcastConnectionUpdate(BleStateEnum.Disconnected)
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @Synchronized
    fun connect(): Boolean {
        disconnectRequested = false
        autoConnect = false
        mDisconnectTime = null
        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null || mBluetoothDeviceAddress!!.isEmpty()) {
            Timber.i("BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        if (mBluetoothGatt != null && mBluetoothGatt!!.device.address == mBluetoothDeviceAddress) {
            Timber.i("Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                WheelData.getInstance().btName = mBluetoothGatt!!.device.name
                broadcastConnectionUpdate(BleStateEnum.Connecting)
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(mBluetoothDeviceAddress)
        if (device == null) {
            Timber.i("Device not found.  Unable to connect.")
            return false
        }
        mBluetoothGatt = device.connectGatt(
            context,
            autoConnect,
            // Implements callback methods for GATT events that the app cares about.  For example,
            // connection change and services discovered.
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    super.onConnectionStateChange(gatt, status, newState)
                    val connectionSound = WheelLog.AppConfig.connectionSound
                    val noConnectionSound = WheelLog.AppConfig.noConnectionSound * 1000
                    val state =  try {
                        BleStateEnum.values()[newState]
                    } catch (ex: Exception) {
                        BleStateEnum.Unknown
                    }

                    when (state) {
                        BleStateEnum.Connected -> {
                            Timber.i("Connected to GATT server.")
                            if (connectionSound) {
                                if (noConnectionSound > 0) {
                                    stopBeepTimer()
                                }
                                wl?.release()
                                wl = mgr?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLogTag)?.apply {
                                    acquire(5 * 60 * 1000L /*5 minutes*/)
                                }
                                playSound(context, R.raw.sound_connect)
                            }
                            mDisconnectTime = null
                            // Attempts to discover services after successful connection.
                            Timber.i(
                                "Attempting to start service discovery:%b",
                                mBluetoothGatt?.discoverServices()
                            )
                            broadcastConnectionUpdate(state)
                        }
                        BleStateEnum.Disconnected -> {
                            Timber.i("Disconnected from GATT server.")
                            if (connectionState == BleStateEnum.Connected) {
                                mDisconnectTime = Date()
                                if (connectionSound) {
                                    playSound(context, R.raw.sound_disconnect)
                                    wl?.release()
                                    wl = null
                                    if (noConnectionSound > 0) {
                                        startBeepTimer()
                                    }
                                }
                            }
                            if (!disconnectRequested && mBluetoothGatt?.device != null) {
                                Timber.i("Trying to reconnect")
                                when (WheelData.getInstance().wheelType) {
                                    WHEEL_TYPE.INMOTION -> {
                                        InMotionAdapter.stopTimer()
                                        InmotionAdapterV2.stopTimer()
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.INMOTION_V2 -> {
                                        InmotionAdapterV2.stopTimer()
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.NINEBOT_Z -> {
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.NINEBOT -> NinebotAdapter.getInstance().resetConnection()
                                    else -> {}
                                }
                                if (!autoConnect) {
                                    autoConnect = true
                                    mBluetoothGatt?.close()
                                    mBluetoothGatt = mBluetoothGatt!!.device.connectGatt(
                                        context, autoConnect,
                                        this
                                    )
                                }
                                broadcastConnectionUpdate(BleStateEnum.Connecting, true)
                            } else {
                                Timber.i("Disconnected")
                                connectionState = BleStateEnum.Disconnected
                                broadcastConnectionUpdate(connectionState)
                            }
                        }
                        else -> {
                            Toast.makeText(
                                context,
                                "Unknown Connection State\rState = $newState", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    Timber.i("onServicesDiscovered called")
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_SUCCESS")
                        val recognisedWheel = detectWheel(mBluetoothDeviceAddress)
                        if (recognisedWheel) {
                            broadcastConnectionUpdate(BleStateEnum.Connected)
                        } else {
                            disconnect()
                        }
                        return
                    }
                    Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_FAILURE")
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    Timber.i("onCharacteristicRead called %s", characteristic.uuid.toString())
                    readData(characteristic, status)
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    super.onCharacteristicChanged(gatt, characteristic)
                    Timber.i("onCharacteristicChanged called %s", characteristic.uuid.toString())
                    readData(characteristic, BluetoothGatt.GATT_SUCCESS)
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int
                ) {
                    super.onDescriptorWrite(gatt, descriptor, status)
                    Timber.i("onDescriptorWrite %d", status)
                }
            })
        Timber.i("Trying to create a new connection.")
        broadcastConnectionUpdate(BleStateEnum.Connecting)
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @Synchronized
    fun disconnect() {
        disconnectRequested = true
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.i("BluetoothAdapter not initialized")
            broadcastConnectionUpdate(BleStateEnum.Disconnected)
            return
        }
        mBluetoothGatt!!.disconnect()
        broadcastConnectionUpdate(BleStateEnum.Disconnected)
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @Synchronized
    fun close() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    @Synchronized
    fun writeBluetoothGattCharacteristic(cmd: ByteArray?): Boolean {
        if (mBluetoothGatt == null || cmd == null) {
            return false
        }
        val stringBuilder = StringBuilder(cmd.size)
        for (aData in cmd) {
            stringBuilder.append(String.format(Locale.US, "%02X", aData))
        }
        Timber.i("Transmitted: %s", stringBuilder.toString())
        try {
            when (WheelData.getInstance().wheelType) {
                WHEEL_TYPE.KINGSONG -> {
                    val ksService =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID))
                    if (ksService == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val ksCharacteristic =
                        ksService.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID))
                    if (ksCharacteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    ksCharacteristic.value = cmd
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        ksCharacteristic.writeType
                    )
                    ksCharacteristic.writeType = 1
                    return mBluetoothGatt!!.writeCharacteristic(ksCharacteristic)
                }
                WHEEL_TYPE.GOTWAY,
                WHEEL_TYPE.GOTWAY_VIRTUAL,
                WHEEL_TYPE.VETERAN -> {
                    val gwService =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID))
                    if (gwService == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val characteristic =
                        gwService.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID))
                    if (characteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    characteristic.value = cmd
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        characteristic.writeType
                    )
                    return mBluetoothGatt!!.writeCharacteristic(characteristic)
                }
                WHEEL_TYPE.NINEBOT_Z -> {
                    val nzService =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.NINEBOT_Z_SERVICE_UUID))
                    if (nzService == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val nzCharacteristic =
                        nzService.getCharacteristic(UUID.fromString(Constants.NINEBOT_Z_WRITE_CHARACTER_UUID))
                    if (nzCharacteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    nzCharacteristic.value = cmd
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        nzCharacteristic.writeType
                    )
                    return mBluetoothGatt!!.writeCharacteristic(nzCharacteristic)
                }
                WHEEL_TYPE.NINEBOT -> {
                    val nbService =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.NINEBOT_SERVICE_UUID))
                    if (nbService == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val nbCharacteristic =
                        nbService.getCharacteristic(UUID.fromString(Constants.NINEBOT_WRITE_CHARACTER_UUID))
                    if (nbCharacteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    nbCharacteristic.value = cmd
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        nbCharacteristic.writeType
                    )
                    return mBluetoothGatt!!.writeCharacteristic(nbCharacteristic)
                }
                WHEEL_TYPE.INMOTION -> {
                    val imService =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.INMOTION_WRITE_SERVICE_UUID))
                    if (imService == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val imCharacteristic =
                        imService.getCharacteristic(UUID.fromString(Constants.INMOTION_WRITE_CHARACTER_UUID))
                    if (imCharacteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    val buf = ByteArray(20)
                    val i2 = cmd.size / 20
                    val i3 = cmd.size - i2 * 20
                    var i4 = 0
                    while (i4 < i2) {
                        System.arraycopy(cmd, i4 * 20, buf, 0, 20)
                        imCharacteristic.value = buf
                        if (!mBluetoothGatt!!.writeCharacteristic(imCharacteristic)) return false
                        try {
                            Thread.sleep(20)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        i4++
                    }
                    if (i3 > 0) {
                        System.arraycopy(cmd, i2 * 20, buf, 0, i3)
                        imCharacteristic.value = buf
                        if (!mBluetoothGatt!!.writeCharacteristic(imCharacteristic)) return false
                    }
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        imCharacteristic.writeType
                    )
                    return true
                }
                WHEEL_TYPE.INMOTION_V2 -> {
                    val inv2Service =
                        mBluetoothGatt!!.getService(UUID.fromString(Constants.INMOTION_V2_SERVICE_UUID))
                    if (inv2Service == null) {
                        Timber.i("writeBluetoothGattCharacteristic service == null")
                        return false
                    }
                    val inv2Characteristic =
                        inv2Service.getCharacteristic(UUID.fromString(Constants.INMOTION_V2_WRITE_CHARACTER_UUID))
                    if (inv2Characteristic == null) {
                        Timber.i("writeBluetoothGattCharacteristic characteristic == null")
                        return false
                    }
                    inv2Characteristic.value = cmd
                    Timber.i(
                        "writeBluetoothGattCharacteristic writeType = %d",
                        inv2Characteristic.writeType
                    )
                    return mBluetoothGatt!!.writeCharacteristic(inv2Characteristic)
                }
                else -> {}
            }
        } catch (e: NullPointerException) {
            // sometimes mBluetoothGatt is null... If the user starts to connect and disconnect quickly
            Timber.i("writeBluetoothGattCharacteristic throws NullPointerException: %s", e.message)
        }
        return false
    }

    private fun writeBluetoothGattDescriptor(descriptor: BluetoothGattDescriptor?) {
        val success = mBluetoothGatt?.writeDescriptor(descriptor) == true
        Timber.i("Write descriptor %b", success)
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return if (mBluetoothGatt == null) null else mBluetoothGatt!!.services
    }

    fun getGattService(service_id: UUID?): BluetoothGattService? {
        return mBluetoothGatt!!.getService(service_id)
    }

    fun getBluetoothDeviceAddress(): String? {
        return mBluetoothDeviceAddress
    }

    fun startReconnectTimer() {
        if (reconnectTimer != null) {
            stopReconnectTimer()
        }
        reconnectTimer = Timer()
        val wd = WheelData.getInstance()
        val magicPeriod = 15000
        reconnectTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (connectionState == BleStateEnum.Connected && wd.lastLifeData > 0 && (System.currentTimeMillis() - wd.lastLifeData) / 1000 > magicPeriod) {
                    toggleReconnectToWheel()
                }
            }
        }, magicPeriod.toLong(), magicPeriod.toLong())
    }

    fun stopReconnectTimer() {
        reconnectTimer?.cancel()
        reconnectTimer = null
    }

    init {
        mBluetoothAdapter = getAdapter()
        if (mBluetoothAdapter == null) {
            Timber.e(context.resources.getString(R.string.error_bluetooth_not_initialised))
            Toast.makeText(context, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show()
        } else {
            setDeviceAddress(WheelLog.AppConfig.lastMac)
            toggleConnectToWheel()
        }
    }

    private fun readData(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }

        // RAW data
        if (WheelLog.AppConfig.enableRawData) {
            if (fileUtilRawData == null) {
                fileUtilRawData = FileUtil(context)
            }
            if (fileUtilRawData!!.isNull) {
                val fileNameForRawData = "RAW_" + sdf.format(Date()) + ".csv"
                fileUtilRawData!!.prepareFile(fileNameForRawData, WheelData.getInstance().mac)
            }
            fileUtilRawData!!.writeLine(
                String.format(
                    Locale.US, "%s,%s",
                    sdf2.format(System.currentTimeMillis()),
                    toHexStringRaw(characteristic.value)
                )
            )
        } else if (fileUtilRawData != null && !fileUtilRawData!!.isNull) {
            fileUtilRawData!!.close()
        }
        val wd = WheelData.getInstance()
        val value = characteristic.value
        when (wd.wheelType) {
            WHEEL_TYPE.KINGSONG -> if (characteristic.uuid.toString() == Constants.KINGSONG_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
                if (wd.name.isEmpty()) {
                    KingsongAdapter.getInstance().requestNameData()
                } else if (wd.serial.isEmpty()) {
                    KingsongAdapter.getInstance().requestSerialData()
                }
            }
            WHEEL_TYPE.GOTWAY,
            WHEEL_TYPE.GOTWAY_VIRTUAL,
            WHEEL_TYPE.VETERAN -> wd.decodeResponse(value, context)
            WHEEL_TYPE.INMOTION -> if (characteristic.uuid.toString() == Constants.INMOTION_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
            }
            WHEEL_TYPE.INMOTION_V2 -> if (characteristic.uuid.toString() == Constants.INMOTION_V2_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
            }
            WHEEL_TYPE.NINEBOT_Z -> if (characteristic.uuid.toString() == Constants.NINEBOT_Z_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
            }
            WHEEL_TYPE.NINEBOT -> if (characteristic.uuid.toString() == Constants.NINEBOT_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
            }
            else -> {}
        }
    }

    private fun broadcastConnectionUpdate(newState: BleStateEnum, auto_connect: Boolean = false) {
        connectionState = newState
        val intent = Intent(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
        intent.putExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, connectionState.ordinal)
        if (auto_connect) {
            intent.putExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT, true)
        }
        context.sendBroadcast(intent)
    }

    private fun getAdapter(): BluetoothAdapter? {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    fun getDisconnectTime(): Date? {
        return mDisconnectTime
    }

    private fun setWheelServices(serviceUUID: String, charUUID: String, decriptorUUID: String?) {
        val targetService = getGattService(UUID.fromString(serviceUUID))
        Timber.i("service UUID")
        val notifyCharacteristic = targetService?.getCharacteristic(UUID.fromString(charUUID))
        Timber.i("read UUID")
        if (notifyCharacteristic == null) {
            Timber.i("it seems that RX UUID doesn't exist")
        }
        setCharacteristicNotification(notifyCharacteristic)
        Timber.i("notify UUID")
        if (decriptorUUID != null) {
            val descriptor =
                notifyCharacteristic!!.getDescriptor(UUID.fromString(decriptorUUID))
            Timber.i("descr UUID")
            if (descriptor == null) {
                Timber.i("it seems that descr UUID doesn't exist")
            }
            descriptor!!.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            Timber.i("enable notify UUID")
            writeBluetoothGattDescriptor(descriptor)
            Timber.i("write notify")
        }
    }

    private fun detectWheel(deviceAddress: String?): Boolean {
        WheelLog.AppConfig.lastMac = deviceAddress!!
        val advData = WheelLog.AppConfig.advDataForWheel
        var adapterName = ""
        val protoVer =
            when (advData) {
                "4e421300000000ec",
                "4e421302000000ea" -> "S2"
                "4e421400000000eb",
                "4e422000000000df",
                "4e422200000000dd",
                "4e4230cf",
                "5600" -> "Mini"
                else -> ""
            }
        var detectedWheel = false
        val text = getRawTextResource(context, R.raw.bluetooth_services)
        try {
            val arr = JSONArray(text)
            adaptersLoop@ for (i in 0 until arr.length()) {
                val services = arr.getJSONObject(i)
                if (services.length() - 1 != getSupportedGattServices()?.size) {
                    continue
                }
                adapterName = services.getString("adapter")
                val iterator = services.keys()
                // skip adapter key
                iterator.next()
                adapterServicesLoop@ while (iterator.hasNext()) {
                    val keyName = iterator.next()
                    val sUuid = UUID.fromString(keyName)
                    val service: BluetoothGattService = getGattService(sUuid)
                        ?: break
                    val serviceUuid = services.getJSONArray(keyName)
                    for (j in 0 until serviceUuid.length()) {
                        val cUuid = UUID.fromString(serviceUuid.getString(j))
                        service.getCharacteristic(cUuid) ?: break@adapterServicesLoop
                    }
                    detectedWheel = true
                    break@adaptersLoop
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val wd = WheelData.getInstance()
        if (detectedWheel) {
            Timber.i("Protocol recognized as %s", adapterName)
            if (WHEEL_TYPE.GOTWAY.toString().equals(adapterName, ignoreCase = true) &&
                (mBluetoothGatt!!.device.name == "RW" || WheelData.getInstance().name.startsWith("ROCKW"))
            ) {
                Timber.i("It seems to be RochWheel, force to Kingsong proto")
                adapterName = WHEEL_TYPE.KINGSONG.toString()
            }
            when (adapterName.toUpperCase(Locale.ROOT)) {
                WHEEL_TYPE.KINGSONG.toString() -> {
                    wd.wheelType = WHEEL_TYPE.KINGSONG
                    val targetService =
                        getGattService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID))
                    val notifyCharacteristic =
                        targetService?.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID))
                    setCharacteristicNotification(notifyCharacteristic)
                    val descriptor =
                        notifyCharacteristic?.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID))
                            ?.apply {
                                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            }
                    writeBluetoothGattDescriptor(descriptor)
                }
                WHEEL_TYPE.GOTWAY.toString() -> {
                    wd.wheelType = WHEEL_TYPE.GOTWAY
                    setWheelServices(
                        Constants.GOTWAY_SERVICE_UUID,
                        Constants.GOTWAY_READ_CHARACTER_UUID,
                        null)
                    // Let the user know it's working by making the wheel beep
                    if (WheelLog.AppConfig.connectBeep) {
                        writeBluetoothGattCharacteristic("b".toByteArray())
                    }
                    Timber.i("starting Gotway adapter")
                }
                WHEEL_TYPE.INMOTION.toString() -> {
                    wd.wheelType = WHEEL_TYPE.INMOTION
                    setWheelServices(
                        Constants.INMOTION_SERVICE_UUID,
                        Constants.INMOTION_READ_CHARACTER_UUID,
                        Constants.INMOTION_DESCRIPTER_UUID)
                    val password = WheelLog.AppConfig.passwordForWheel
                    if (password.isNotEmpty()) {
                        InMotionAdapter.getInstance().startKeepAliveTimer(password)
                        Timber.i("starting Inmotion adapter")
                    } else {
                        detectedWheel = false
                        Timber.i("error: password in Inmotion adapter.")
                    }
                }
                WHEEL_TYPE.INMOTION_V2.toString() -> {
                    wd.wheelType = WHEEL_TYPE.INMOTION_V2
                    setWheelServices(
                        Constants.INMOTION_V2_SERVICE_UUID,
                        Constants.INMOTION_V2_READ_CHARACTER_UUID,
                        Constants.INMOTION_V2_DESCRIPTER_UUID)
                    InmotionAdapterV2.getInstance().startKeepAliveTimer()
                    Timber.i("starting Inmotion V2 adapter")
                }
                WHEEL_TYPE.NINEBOT_Z.toString() -> {
                    wd.wheelType = WHEEL_TYPE.NINEBOT_Z
                    setWheelServices(
                        Constants.NINEBOT_Z_SERVICE_UUID,
                        Constants.NINEBOT_Z_READ_CHARACTER_UUID,
                        Constants.NINEBOT_Z_DESCRIPTER_UUID)
                    if (protoVer.compareTo("S2") == 0 || protoVer.compareTo("Mini") == 0) {
                        NinebotAdapter.getInstance().startKeepAliveTimer(protoVer)
                    } else {
                        NinebotZAdapter.getInstance().startKeepAliveTimer()
                    }
                    Timber.i("starting ninebot adapter")
                }
                WHEEL_TYPE.NINEBOT.toString() -> {
                    Timber.i("Trying to start Ninebot")
                    wd.wheelType = WHEEL_TYPE.NINEBOT
                    setWheelServices(
                        Constants.NINEBOT_SERVICE_UUID,
                        Constants.NINEBOT_READ_CHARACTER_UUID,
                        Constants.NINEBOT_DESCRIPTER_UUID)
                    NinebotAdapter.getInstance().startKeepAliveTimer(protoVer)
                    Timber.i("starting ninebot adapter")
                }
                else -> {
                    WheelLog.AppConfig.lastMac = ""
                    Timber.i("Protocol recognized as Unknown")
                    val services = getSupportedGattServices()
                    if (services != null) {
                        for (service in services) {
                            if (service != null) {
                                Timber.i("Service: %s", service.uuid.toString())
                                for (characteristics in service.characteristics) {
                                    Timber.i("Characteristics: %s", characteristics.uuid.toString())
                                }
                            }
                        }
                    }
                    detectedWheel = false
                }
            }
        }
        wd.isConnected = detectedWheel
        return detectedWheel
    }

    private fun startBeepTimer() {
        wl = mgr!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLogTag)?.apply {
            acquire(5 * 60 * 1000L /*5 minutes*/)
        }
        timerTicks = 0
        val noConnectionSound = WheelLog.AppConfig.noConnectionSound * 1000L
        val beepTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                timerTicks++
                if (timerTicks * noConnectionSound > 300000) {
                    stopBeepTimer()
                }
                playSound(context, R.raw.sound_no_connection)
            }
        }
        beepTimer = Timer()
        beepTimer?.scheduleAtFixedRate(beepTimerTask, noConnectionSound, noConnectionSound)
    }

    private fun stopBeepTimer() {
        wl?.release()
        wl = null
        beepTimer?.cancel()
        beepTimer = null
    }

    fun finalize() {
        fileUtilRawData?.close()
        stopBeepTimer()
        if (mBluetoothGatt != null && connectionState != BleStateEnum.Disconnected) {
            mBluetoothGatt?.disconnect()
        }
        stopReconnectTimer()
        close()
    }

    companion object {
        @JvmStatic
        fun getAdapter(context: Context): BluetoothAdapter? {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            return bluetoothManager.adapter
        }
    }
}