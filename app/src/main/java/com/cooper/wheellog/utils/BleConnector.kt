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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic

class BleConnector(val context: Context) {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    internal var mBluetoothGatt: BluetoothGatt? = null

    private var mDisconnectTime: Date? = null
    private var reconnectTimer: Timer? = null

    private var disconnectRequested = false
    internal var autoConnect = false

    private var beepTimer: Timer? = null
    private var timerTicks = 0
    var mgr: PowerManager? = null
    var wl: WakeLock? = null
    private var fileUtilRawData: FileUtil? = null
    private val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    private val sdf2 = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val wakeLogTag = "WhellLog:WakeLockTag"
    private var mConnectionState = BleStateEnum.Disconnected
    private var mLastData = 0L

    var connectionState
        get() = mConnectionState
        private set(value) {
            if (mConnectionState == value) {
                return
            }
            Timber.i("[ble] connectionState changed. New state = $connectionState")
            mConnectionState = value
            val intent = Intent(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
            intent.putExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, value.ordinal)
            context.sendBroadcast(intent)
        }

    private val bleIsEnabled
        get() = mBluetoothAdapter?.isEnabled == true

    var deviceAddress: String?
        get() = mBluetoothDeviceAddress
        set(value) {
            if (!value.isNullOrEmpty()) {
                mBluetoothDeviceAddress = value
            }
        }

    fun toggleConnectToWheel() {
        Timber.i("[ble] toggleConnectToWheel called. state = $connectionState")
        if (connectionState == BleStateEnum.Disconnected) {
            connect()
        } else {
            disconnect()
            close()
        }
    }

    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic?) {
        Timber.i("[ble] Set characteristic start")
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.i("[ble] BluetoothAdapter not initialized")
            return
        }
        val success = mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)
        Timber.i("[ble] Set characteristic %b", success)
    }

    private fun toggleReconnectToWheel() {
        Timber.i("[ble] toggleReconnectToWheel called")
        if (connectionState == BleStateEnum.Connected) {
            Timber.wtf("Trying to reconnect")
            // After disconnect, the method onConnectionStateChange will automatically reconnect
            // because disconnectRequested is false
            disconnectRequested = false
            mBluetoothGatt!!.disconnect()
            connectionState = BleStateEnum.Disconnected
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
        Timber.i("[ble] connect called")
        if (connectionState == BleStateEnum.Connected) {
            return true
        }
        disconnectRequested = false
        autoConnect = false
        mDisconnectTime = null
        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null || mBluetoothDeviceAddress!!.isEmpty()) {
            Timber.i("[ble] BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        if (mBluetoothGatt != null && mBluetoothGatt!!.device.address == mBluetoothDeviceAddress) {
            Timber.i("[ble] Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                WheelData.getInstance().btName = mBluetoothGatt!!.device.name
                connectionState = BleStateEnum.Connecting
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(mBluetoothDeviceAddress)
        if (device == null) {
            Timber.i("[ble] Device not found.  Unable to connect.")
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

                    Timber.i("[ble] onConnectionStateChange. oldState = $connectionState / newState = $state")

                    when (state) {
                        BleStateEnum.Connected -> {
                            Timber.i("[ble] Connected to GATT server.")
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
                            val discover = mBluetoothGatt?.discoverServices()
                            Timber.i("[ble] Attempting to start service discovery:%b", discover)
                            connectionState = BleStateEnum.Connected
                        }
                        BleStateEnum.Disconnected -> {
                            Timber.i("[ble] Disconnected from GATT server.")
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
                                Timber.i("[ble] Trying to reconnect")
                                when (WheelData.getInstance().wheelType) {
                                    WHEEL_TYPE.INMOTION -> {
                                        InMotionAdapter.stopTimer()
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.INMOTION_V2 -> {
                                        InmotionAdapterV2.stopTimer()
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.NINEBOT_Z -> {
                                        NinebotZAdapter.stopTimer()
                                        NinebotZAdapter.getInstance().resetConnection()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
                                    WHEEL_TYPE.KINGSONG -> {
                                        KingsongAdapter.stopTimer()
                                    }
                                    WHEEL_TYPE.NINEBOT -> {
                                        NinebotAdapter.stopTimer()
                                        NinebotAdapter.getInstance().resetConnection()
                                    }
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
                                connectionState = BleStateEnum.Connecting
                            } else {
                                Timber.i("[ble] Disconnected")
                                connectionState = BleStateEnum.Disconnected
                            }
                        }
                        else -> {
                            val message = "[ble] Unknown connection state: $newState"
                            Timber.i(message)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    Timber.i("[ble] onServicesDiscovered called, status = %s", status.toString())
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val recognisedWheel = detectWheel(mBluetoothDeviceAddress)
                        if (recognisedWheel) {
                            connectionState = BleStateEnum.Connected
                        } else {
                            disconnect()
                        }
                        return
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    Timber.i("[ble] onCharacteristicRead called %s", characteristic.uuid.toString())
                    readData(characteristic, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mLastData = System.currentTimeMillis()
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    super.onCharacteristicChanged(gatt, characteristic)
                    Timber.i("[ble] onCharacteristicChanged called %s", characteristic.uuid.toString())
                    readData(characteristic, BluetoothGatt.GATT_SUCCESS)
                    mLastData = System.currentTimeMillis()
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int
                ) {
                    super.onDescriptorWrite(gatt, descriptor, status)
                    Timber.i("[ble] onDescriptorWrite %d", status)
                }
            })
        Timber.i("[ble] Trying to create a new connection.")
        connectionState = BleStateEnum.Connecting
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
        Timber.i("[ble] disconnect called")
        if (connectionState == BleStateEnum.Disconnected) {
            Timber.i("[ble] already disconnected")
            return
        }
        disconnectRequested = true
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.i("[ble] BluetoothAdapter not initialized")
            connectionState = BleStateEnum.Disconnected
            return
        }
        mBluetoothGatt!!.disconnect()
        connectionState = BleStateEnum.Disconnected
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @Synchronized
    fun close() {
        Timber.i("[ble] close called")
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    private fun getCharacteristic(serviceUUID: String, characteristicUUID: String, cmd: ByteArray? = null): BluetoothGattCharacteristic? {
        val service =
            mBluetoothGatt!!.getService(UUID.fromString(serviceUUID))
        if (service == null) {
            Timber.i("[ble] getCharacteristic / service == null")
            return null
        }
        val characteristic =
            service.getCharacteristic(UUID.fromString(characteristicUUID))
        if (characteristic == null) {
            Timber.i("[ble] getCharacteristic / characteristic == null")
            return null
        }
        if (cmd != null) {
            characteristic.value = cmd
        }
        Timber.i("[ble] getCharacteristic / writeType = %d", characteristic.writeType)
        return characteristic
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
        Timber.i("[ble] Transmitted: %s", stringBuilder.toString())
        try {
            when (WheelData.getInstance().wheelType) {
                WHEEL_TYPE.KINGSONG -> {
                    val ksCharacteristic = getCharacteristic(
                        Constants.KINGSONG_SERVICE_UUID,
                        Constants.KINGSONG_READ_CHARACTER_UUID,
                        cmd
                    ) ?: return false
                    ksCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    return mBluetoothGatt!!.writeCharacteristic(ksCharacteristic)
                }
                WHEEL_TYPE.GOTWAY,
                WHEEL_TYPE.GOTWAY_VIRTUAL,
                WHEEL_TYPE.VETERAN -> {
                    val gwCharacteristic = getCharacteristic(
                        Constants.GOTWAY_SERVICE_UUID,
                        Constants.GOTWAY_READ_CHARACTER_UUID,
                        cmd
                    ) ?: return false
                    return mBluetoothGatt!!.writeCharacteristic(gwCharacteristic)
                }
                WHEEL_TYPE.NINEBOT_Z -> {
                    val nzCharacteristic = getCharacteristic(
                        Constants.NINEBOT_Z_SERVICE_UUID,
                        Constants.NINEBOT_Z_WRITE_CHARACTER_UUID,
                        cmd
                    ) ?: return false
                    return mBluetoothGatt!!.writeCharacteristic(nzCharacteristic)
                }
                WHEEL_TYPE.NINEBOT -> {
                    val nbCharacteristic = getCharacteristic(
                        Constants.NINEBOT_SERVICE_UUID,
                        Constants.NINEBOT_WRITE_CHARACTER_UUID,
                        cmd
                    ) ?: return false
                    return mBluetoothGatt!!.writeCharacteristic(nbCharacteristic)
                }
                WHEEL_TYPE.INMOTION -> {
                    val imCharacteristic = getCharacteristic(
                        Constants.INMOTION_WRITE_SERVICE_UUID,
                        Constants.INMOTION_WRITE_CHARACTER_UUID
                    ) ?: return false

                    // splitting the cmd into parts equal to 20 bytes
                    // all commands are 22+ bytes each =)
                    val buf = ByteArray(20)
                    val fullPartCount = cmd.size / 20
                    val remainPart = cmd.size - fullPartCount * 20
                    var fullPartIndex = 0
                    Timber.i("[ble] writeBluetoothGattCharacteristic cmdSize = ${cmd.size} | fullParts = $fullPartCount")
                    while (fullPartIndex < fullPartCount) {
                        System.arraycopy(cmd, fullPartIndex * 20, buf, 0, 20)
                        imCharacteristic.value = buf
                        if (!mBluetoothGatt!!.writeCharacteristic(imCharacteristic)) return false
                        try {
                            Thread.sleep(20)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        fullPartIndex++
                    }
                    if (remainPart > 0) {
                        imCharacteristic.value = cmd.copyOfRange(fullPartCount * 20, cmd.size)
                        if (!mBluetoothGatt!!.writeCharacteristic(imCharacteristic)) return false
                    }
                    return true
                }
                WHEEL_TYPE.INMOTION_V2 -> {
                    val inv2Characteristic = getCharacteristic(
                        Constants.INMOTION_V2_SERVICE_UUID,
                        Constants.INMOTION_V2_WRITE_CHARACTER_UUID
                    ) ?: return false
                    return mBluetoothGatt!!.writeCharacteristic(inv2Characteristic)
                }
                else -> {
                    Timber.i("[ble] writeBluetoothGattCharacteristic !!! unknown wheelType = ${WheelData.getInstance().wheelType}")
                }
            }
        } catch (e: NullPointerException) {
            // sometimes mBluetoothGatt is null... If the user starts to connect and disconnect quickly
            Timber.i("[ble] writeBluetoothGattCharacteristic throws NullPointerException: %s", e.message)
        }
        return false
    }

    private fun writeBluetoothGattDescriptor(descriptor: BluetoothGattDescriptor?) {
        val success = mBluetoothGatt?.writeDescriptor(descriptor) == true
        Timber.i("[ble] Write descriptor %b", success)
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    private fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return mBluetoothGatt?.services
    }

    private fun getGattService(service_id: UUID?): BluetoothGattService? {
        return mBluetoothGatt?.getService(service_id)
    }

    fun startReconnectTimer() {
        Timber.i("[ble] startReconnectTimer called")
        if (reconnectTimer != null) {
            stopReconnectTimer()
        }
        reconnectTimer = Timer()
        val wd = WheelData.getInstance()
        val magicPeriod = 15_000L
        reconnectTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (connectionState == BleStateEnum.Connected && wd.lastLifeData > 0 && (System.currentTimeMillis() - wd.lastLifeData) / 1000 > magicPeriod) {
                    toggleReconnectToWheel()
                }
            }
        }, magicPeriod, magicPeriod)
    }

    fun stopReconnectTimer() {
        Timber.i("[ble] stopReconnectTimer called")
        reconnectTimer?.cancel()
        reconnectTimer = null
    }

    init {
        mBluetoothAdapter = getAdapter(context)
        if (!bleIsEnabled) {
            Timber.e(context.resources.getString(R.string.error_bluetooth_not_initialised))
            Toast.makeText(context, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show()
        }
        Timber.i("[ble] init")
    }

    private fun writeRAWData(value: ByteArray) {
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
                    toHexStringRaw(value)
                )
            )
        } else if (fileUtilRawData != null && !fileUtilRawData!!.isNull) {
            fileUtilRawData!!.close()
        }
    }

    private fun readData(characteristic: BluetoothGattCharacteristic, status: Int) {
        Timber.i("[ble] readData called.")
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        val value = characteristic.value
        writeRAWData(value)
        val wd = WheelData.getInstance()
        Timber.i("[ble] readData. wheel type = ${wd.wheelType}")
        when (wd.wheelType) {
            WHEEL_TYPE.KINGSONG -> if (characteristic.uuid.toString() == Constants.KINGSONG_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, context)
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

    fun getDisconnectTime(): Date? {
        return mDisconnectTime
    }

    private fun setWheelServices(serviceUUID: String, charUUID: String, descriptorUUID: String? = null) {
        val targetService = getGattService(UUID.fromString(serviceUUID))
        Timber.i("[ble] service UUID")
        val notifyCharacteristic = targetService?.getCharacteristic(UUID.fromString(charUUID))
        Timber.i("[ble] read UUID")
        if (notifyCharacteristic == null) {
            Timber.i("[ble] it seems that RX UUID doesn't exist")
        }
        setCharacteristicNotification(notifyCharacteristic)
        Timber.i("[ble] notify UUID")
        if (descriptorUUID != null) {
            val descriptor =
                notifyCharacteristic!!.getDescriptor(UUID.fromString(descriptorUUID))
            Timber.i("[ble] descr UUID")
            if (descriptor == null) {
                Timber.i("[ble] it seems that descr UUID doesn't exist")
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                Timber.i("[ble] enable notify UUID")
                writeBluetoothGattDescriptor(descriptor)
                Timber.i("[ble] write notify")
            }
        }
    }

    private fun detectWheel(deviceAddress: String?): Boolean {
        Timber.i("[ble] detectWheel called")
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
            Timber.i("[ble] Protocol recognized as %s", adapterName)
            if (WHEEL_TYPE.GOTWAY.toString().equals(adapterName, ignoreCase = true) &&
                (mBluetoothGatt!!.device.name == "RW" || WheelData.getInstance().name.startsWith("ROCKW"))
            ) {
                Timber.i("[ble] It seems to be RochWheel, force to Kingsong proto")
                adapterName = WHEEL_TYPE.KINGSONG.toString()
            }
            when (adapterName.uppercase(Locale.ROOT)) {
                WHEEL_TYPE.KINGSONG.toString() -> {
                    wd.wheelType = WHEEL_TYPE.KINGSONG
                    setWheelServices(
                        Constants.KINGSONG_SERVICE_UUID,
                        Constants.KINGSONG_READ_CHARACTER_UUID,
                        Constants.KINGSONG_DESCRIPTER_UUID)
                    Timber.i("[ble] starting Kingsong adapter")
                    KingsongAdapter.getInstance().startStartingTimer();
                }
                WHEEL_TYPE.GOTWAY.toString() -> {
                    wd.wheelType = WHEEL_TYPE.GOTWAY_VIRTUAL
                    setWheelServices(
                        Constants.GOTWAY_SERVICE_UUID,
                        Constants.GOTWAY_READ_CHARACTER_UUID)
                    // Let the user know it's working by making the wheel beep
                    if (WheelLog.AppConfig.connectBeep) {
                        writeBluetoothGattCharacteristic("b".toByteArray())
                    }
                    Timber.i("[ble] starting Gotway adapter")
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
                        Timber.i("[ble] starting Inmotion adapter")
                    } else {
                        detectedWheel = false
                        Timber.i("[ble] error: password in Inmotion adapter.")
                    }
                }
                WHEEL_TYPE.INMOTION_V2.toString() -> {
                    wd.wheelType = WHEEL_TYPE.INMOTION_V2
                    setWheelServices(
                        Constants.INMOTION_V2_SERVICE_UUID,
                        Constants.INMOTION_V2_READ_CHARACTER_UUID,
                        Constants.INMOTION_V2_DESCRIPTER_UUID)
                    InmotionAdapterV2.getInstance().startKeepAliveTimer()
                    Timber.i("[ble] starting Inmotion V2 adapter")
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
                    Timber.i("[ble] starting ninebot adapter")
                }
                WHEEL_TYPE.NINEBOT.toString() -> {
                    Timber.i("[ble] Trying to start Ninebot")
                    wd.wheelType = WHEEL_TYPE.NINEBOT
                    setWheelServices(
                        Constants.NINEBOT_SERVICE_UUID,
                        Constants.NINEBOT_READ_CHARACTER_UUID,
                        Constants.NINEBOT_DESCRIPTER_UUID)
                    NinebotAdapter.getInstance().startKeepAliveTimer(protoVer)
                    Timber.i("[ble] starting ninebot adapter")
                }
                else -> {
                    WheelLog.AppConfig.lastMac = ""
                    Timber.i("[ble] Protocol recognized as Unknown")
                    val services = getSupportedGattServices()
                    if (services != null) {
                        for (service in services) {
                            if (service != null) {
                                Timber.i("[ble] Service: %s", service.uuid.toString())
                                for (characteristics in service.characteristics) {
                                    Timber.i("[ble] Characteristics: %s", characteristics.uuid.toString())
                                }
                            }
                        }
                    }
                    detectedWheel = false
                }
            }
        }
        return detectedWheel
    }

    private fun startBeepTimer() {
        Timber.i("[ble] startBeepTimer called")
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
        Timber.i("[ble] stopBeepTimer called")
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