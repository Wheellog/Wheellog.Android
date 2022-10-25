package com.cooper.wheellog

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.Constants.*
import com.cooper.wheellog.utils.StringUtil.Companion.getRawTextResource
import com.cooper.wheellog.utils.StringUtil.Companion.inArray
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object WheelManager {
    private val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    private val sdf2 = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private const val GRAPH_UPDATE_INTERVAL = 1000 // milliseconds

    var lastLifeData: Long = -1
        private set

    private var timestampRaw: Long = 0
    private var timestampLast: Long = 0

    fun detectWheel(
        deviceAddress: String,
        bluetoothService: BluetoothService,
        servicesResId: Int
    ): Boolean {
        val wd = WheelData.getInstance() ?: return false
        val mContext = bluetoothService.applicationContext
        WheelLog.AppConfig.lastMac = deviceAddress
        val advData = WheelLog.AppConfig.advDataForWheel
        var adapterName = ""
        wd.protoVer = ""
        if (inArray(advData, arrayOf("4e421300000000ec", "4e421302000000ea"))) {
            wd.protoVer = "S2"
        } else if (inArray(
                advData,
                arrayOf("4e421400000000eb", "4e422000000000df", "4e422200000000dd", "4e4230cf")
            ) || advData.startsWith("5600")
        ) {
            wd.protoVer = "Mini"
        }
        Timber.i("ProtoVer %s, adv: %s", wd.protoVer, advData)
        var detectedWheel = false
        val text = getRawTextResource(mContext!!, servicesResId)
        val wheelServices: List<BluetoothGattService> = bluetoothService.getWheelServices()
            ?: return false
        try {
            val arr = JSONArray(text)
            var i = 0
            while (i < arr.length() && !detectedWheel) {
                val services = arr.getJSONObject(i)
                if (services.length() - 1 != wheelServices.size) {
                    Timber.i("Services len not corresponds, go to the next")
                    i++
                    continue
                }
                adapterName = services.getString("adapter")
                Timber.i("Searching for %s", adapterName)
                val iterator = services.keys()
                // skip adapter key
                iterator.next()
                var goNextAdapter = false
                while (iterator.hasNext()) {
                    val keyName = iterator.next()
                    Timber.i("Key name %s", keyName)
                    val sUuid = UUID.fromString(keyName)
                    val service = bluetoothService.getWheelService(sUuid)
                    if (service == null) {
                        Timber.i("No such service")
                        goNextAdapter = true
                        break
                    }
                    val serviceUuid = services.getJSONArray(keyName)
                    if (serviceUuid.length() != service.characteristics.size) {
                        Timber.i("Characteristics len not corresponds, go to the next")
                        goNextAdapter = true
                        break
                    }
                    for (j in 0 until serviceUuid.length()) {
                        val cUuid = UUID.fromString(serviceUuid.getString(j))
                        Timber.i("UUid %s", serviceUuid.getString(j))
                        val characteristic = service.getCharacteristic(cUuid)
                        if (characteristic == null) {
                            Timber.i("UUid not found")
                            goNextAdapter = true
                            break
                        } else {
                            Timber.i("UUid found")
                        }
                    }
                    if (goNextAdapter) {
                        break
                    }
                }
                if (!goNextAdapter) {
                    Timber.i("Wheel Detected as %s", adapterName)
                    detectedWheel = true
                }
                i++
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (detectedWheel) {
            Timber.i("Protocol recognized as %s", adapterName)
            if (WHEEL_TYPE.GOTWAY.toString()
                    .equals(
                        adapterName,
                        ignoreCase = true
                    ) && (wd.btName == "RW" || wd.name.startsWith(
                    "ROCKW"
                ))
            ) {
                Timber.i("It seems to be RochWheel, force to Kingsong proto")
                adapterName = WHEEL_TYPE.KINGSONG.toString()
            }
            if (WHEEL_TYPE.KINGSONG.toString().equals(adapterName, ignoreCase = true)) {
                wd.wheelType = WHEEL_TYPE.KINGSONG
                return notifyWheel(
                    bluetoothService, KINGSONG_SERVICE_UUID,
                    KINGSONG_READ_CHARACTER_UUID, KINGSONG_DESCRIPTER_UUID
                )
            } else if (WHEEL_TYPE.GOTWAY.toString().equals(adapterName, ignoreCase = true)) {
                wd.wheelType = WHEEL_TYPE.GOTWAY_VIRTUAL
                if (!notifyWheel(
                        bluetoothService, GOTWAY_SERVICE_UUID,
                        GOTWAY_READ_CHARACTER_UUID, null
                    )
                ) {
                    return false
                }
                // Let the user know it's working by making the wheel beep
                if (WheelLog.AppConfig.connectBeep) {
                    bluetoothService.writeWheelCharacteristic("b".toByteArray())
                }
                return true
            } else if (WHEEL_TYPE.INMOTION.toString().equals(adapterName, ignoreCase = true)) {
                wd.wheelType = WHEEL_TYPE.INMOTION
                if (!notifyWheel(
                        bluetoothService, INMOTION_SERVICE_UUID,
                        INMOTION_READ_CHARACTER_UUID, INMOTION_DESCRIPTER_UUID
                    )
                ) {
                    return false
                }
                val inmotionPassword = WheelLog.AppConfig.passwordForWheel
                if (inmotionPassword.isNotEmpty()) {
                    InMotionAdapter.getInstance().startKeepAliveTimer(inmotionPassword)
                    return true
                }
                return false
            } else if (WHEEL_TYPE.INMOTION_V2.toString().equals(adapterName, ignoreCase = true)) {
                Timber.i("Trying to start Inmotion V2")
                wd.wheelType = WHEEL_TYPE.INMOTION_V2
                if (notifyWheel(
                        bluetoothService, INMOTION_V2_SERVICE_UUID,
                        INMOTION_V2_READ_CHARACTER_UUID, INMOTION_V2_DESCRIPTER_UUID
                    )
                ) {
                    return false
                }
                InmotionAdapterV2.getInstance().startKeepAliveTimer()
                Timber.i("starting Inmotion V2 adapter")
                return true
            } else if (WHEEL_TYPE.NINEBOT_Z.toString().equals(adapterName, ignoreCase = true)) {
                Timber.i("Trying to start Ninebot Z")
                if (wd.protoVer.compareTo("") == 0) {
                    Timber.i("really Z")
                    wd.wheelType = WHEEL_TYPE.NINEBOT_Z
                } else {
                    Timber.i("no, switch to NB")
                    wd.wheelType = WHEEL_TYPE.NINEBOT
                }
                if (!notifyWheel(
                        bluetoothService, NINEBOT_Z_SERVICE_UUID,
                        NINEBOT_Z_READ_CHARACTER_UUID, NINEBOT_Z_DESCRIPTER_UUID
                    )
                ) {
                    return false
                }

                if (wd.protoVer.compareTo("S2") == 0 || wd.protoVer.compareTo("Mini") == 0) {
                    NinebotAdapter.getInstance().startKeepAliveTimer(wd.protoVer)
                    Timber.i("starting ninebot adapter, proto: %s", wd.protoVer)
                } else {
                    NinebotZAdapter.getInstance().startKeepAliveTimer()
                    Timber.i("starting ninebot Z adapter")
                }
                return true
            } else if (WHEEL_TYPE.NINEBOT.toString().equals(adapterName, ignoreCase = true)) {
                Timber.i("Trying to start Ninebot")
                wd.wheelType = WHEEL_TYPE.NINEBOT
                if (!notifyWheel(
                        bluetoothService, NINEBOT_Z_SERVICE_UUID,
                        NINEBOT_Z_READ_CHARACTER_UUID, NINEBOT_Z_DESCRIPTER_UUID
                    )
                ) {
                    return false
                }

                NinebotAdapter.getInstance().startKeepAliveTimer(wd.protoVer)
                Timber.i("starting ninebot adapter")
                return true
            }
        } else {
            WheelLog.AppConfig.lastMac = ""
            Timber.i("Protocol recognized as Unknown")
            for (service in wheelServices) {
                Timber.i("Service: %s", service.uuid.toString())
                for (characteristics in service.characteristics) {
                    Timber.i("Characteristics: %s", characteristics.uuid.toString())
                }
            }
        }
        return false
    }

    private fun notifyWheel(
        bleService: BluetoothService,
        serviceId: UUID,
        characteristicId: UUID,
        descriptorId: UUID?
    ): Boolean {
        val targetService = bleService.getWheelService(serviceId)
        Timber.i("service UUID")
        val notifyCharacteristic =
            targetService?.getCharacteristic(characteristicId) ?: return false
        Timber.i("read UUID")
        bleService.setCharacteristicNotification(notifyCharacteristic, true)
        if (descriptorId != null) {
            Timber.i("notify UUID")
            val descriptor = notifyCharacteristic.getDescriptor(descriptorId)
            Timber.i("descr UUID")
            if (descriptor == null) {
                Timber.i("it seems that descr UUID doesn't exist")
            } else {
                Timber.i("enable notify UUID")
                bleService.writeWheelDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
                Timber.i("write notify")
            }
        }
        return true
    }

    fun readData(characteristic: BluetoothGattCharacteristic, bleService: BluetoothService, value: ByteArray) {
        // RAW data
        bleService.apply {
            if (WheelLog.AppConfig.enableRawData) {

                if (fileUtilRawData == null) {
                    fileUtilRawData = FileUtil(applicationContext)
                }
                if (fileUtilRawData!!.isNull) {
                    val fileNameForRawData = "RAW_" + sdf.format(Date()) + ".csv"
                    fileUtilRawData!!.prepareFile(fileNameForRawData, WheelData.getInstance().mac)
                }
                fileUtilRawData!!.writeLine(
                    String.format(
                        Locale.US, "%s,%s",
                        sdf2.format(System.currentTimeMillis()),
                        StringUtil.toHexStringRaw(value)
                    )
                )
            } else if (fileUtilRawData != null && !fileUtilRawData!!.isNull) {
                fileUtilRawData!!.close()
            }
        }
        val applicationContext = bleService.applicationContext
        val wd = WheelData.getInstance() ?: return
        when (wd.wheelType) {
            WHEEL_TYPE.KINGSONG -> if (characteristic.uuid == KINGSONG_READ_CHARACTER_UUID) {
                wd.decodeResponse(value, applicationContext)
                if (WheelData.getInstance().name.isEmpty()) {
                    KingsongAdapter.getInstance().requestNameData()
                } else if (WheelData.getInstance().serial.isEmpty()) {
                    KingsongAdapter.getInstance().requestSerialData()
                }
            }
            WHEEL_TYPE.GOTWAY, WHEEL_TYPE.GOTWAY_VIRTUAL, WHEEL_TYPE.VETERAN ->
                wd.decodeResponse(value, applicationContext)
            WHEEL_TYPE.INMOTION -> if (characteristic.uuid == INMOTION_READ_CHARACTER_UUID)
                wd.decodeResponse(value, applicationContext)
            WHEEL_TYPE.INMOTION_V2 -> if (characteristic.uuid == INMOTION_V2_READ_CHARACTER_UUID)
                wd.decodeResponse(value, applicationContext)
            WHEEL_TYPE.NINEBOT_Z -> {
                Timber.i("Ninebot Z reading")
                if (characteristic.uuid == NINEBOT_Z_READ_CHARACTER_UUID) {
                    wd.decodeResponse(value, applicationContext)
                }
            }
            WHEEL_TYPE.NINEBOT -> {
                Timber.i("Ninebot reading")
                if (characteristic.uuid == NINEBOT_READ_CHARACTER_UUID
                    || characteristic.uuid == NINEBOT_Z_READ_CHARACTER_UUID) {
                    // in case of S2 or Mini
                    Timber.i("Ninebot read cont")
                    wd.decodeResponse(value, applicationContext)
                }
            }
            else -> {}
        }
    }

    fun decodeResponse(data: ByteArray, mContext: Context) {
        val wd = WheelData.getInstance() ?: return
        timestampRaw = System.currentTimeMillis()
        val stringBuilder = StringBuilder(data.size)
        for (aData in data) {
            stringBuilder.append(String.format(Locale.US, "%02X", aData))
        }
        Timber.i("Received: %s", stringBuilder)
        if (wd.protoVer !== "") {
            Timber.i("Decode, proto: %s", wd.protoVer)
        }
        if (!wd.adapter.setContext(mContext).decode(data)) {
            return // no new data
        }
        lastLifeData = System.currentTimeMillis()
        wd.resetRideTime()
        wd.updateRideTime()
        wd.topSpeed = wd.speedReal
        wd.voltageSag = wd.voltage
        wd.maxTemp = wd.temperature
        var calculatedPwm = 0.0
        if (wd.wheelType == WHEEL_TYPE.KINGSONG || wd.wheelType == WHEEL_TYPE.INMOTION_V2 || WheelLog.AppConfig.hwPwm) {
            calculatedPwm = wd.output.toDouble() / 10000.0
        } else {
            val rotationSpeed = WheelLog.AppConfig.rotationSpeed / 10.0
            val rotationVoltage = WheelLog.AppConfig.rotationVoltage / 10.0
            val powerFactor = WheelLog.AppConfig.powerFactor / 100.0
            calculatedPwm = wd.speedReal / (rotationSpeed / rotationVoltage * wd.voltage * powerFactor)
        }

        wd.calculatedPwm = calculatedPwm * 100
        wd.maxPwm = calculatedPwm
        if (wd.wheelType == WHEEL_TYPE.GOTWAY || wd.wheelType == WHEEL_TYPE.VETERAN) {
            wd.current = (wd.calculatedPwm * wd.phaseCurrent).roundToInt()
        }
        else if (wd.wheelType != WHEEL_TYPE.INMOTION_V2) {
            wd.setPower((wd.currentDouble * wd.voltage).roundToInt())
        }
        val intent = Intent(ACTION_WHEEL_DATA_AVAILABLE)
        if (graph_last_update_time + GRAPH_UPDATE_INTERVAL < Calendar.getInstance().timeInMillis) {
            graph_last_update_time = Calendar.getInstance().timeInMillis
            intent.putExtra(INTENT_EXTRA_GRAPH_UPDATE_AVILABLE, true)
            currentAxis.add(getCurrentDouble().toFloat())
            speedAxis.add(getSpeedDouble().toFloat())
            xAxis.add(SimpleDateFormat("HH:mm:ss", Locale.US).format(Calendar.getInstance().time))
            if (speedAxis.size > 3600000 / WheelData.GRAPH_UPDATE_INTERVAL) {
                speedAxis.removeAt(0)
                currentAxis.removeAt(0)
                xAxis.removeAt(0)
            }
        }
        if (WheelLog.AppConfig.alarmsEnabled) checkAlarmStatus(mContext)
        timestamp_last = timestamp_raw
        intent.putExtra("Speed", mSpeed)
        mContext.sendBroadcast(intent)
        if (!mWheelIsReady && getAdapter().isReady()) {
            mWheelIsReady = true
            val isReadyIntent = Intent(ACTION_WHEEL_IS_READY)
            mContext.sendBroadcast(isReadyIntent)
        }

        wd.CheckMuteMusic()
    }
}