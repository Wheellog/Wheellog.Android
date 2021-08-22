package com.cooper.wheellog.services

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.cooper.wheellog.*
import com.cooper.wheellog.companion.WearOs
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.SomeUtil.Companion.playBeep
import timber.log.Timber

class CoreService: Service() {
    lateinit var bleConnector: BleConnector
    var wearOs: WearOs? = null
    private val binder = LocalBinder()

    fun toggleWatch() {
        togglePebbleService()
        if (WheelLog.AppConfig.garminConnectIqEnable) toggleGarminConnectIQ() else stopGarminConnectIQ()
        toggleWearOs()
    }

    private fun toggleWearOs() {
        wearOs = if (wearOs == null) {
            WearOs(this)
        } else {
            wearOs?.stop()
            null
        }
    }

    private fun stopPebbleService() {
        if (PebbleService.isInstanceCreated()) {
            togglePebbleService()
        }
    }

    private fun togglePebbleService() {
        val pebbleServiceIntent = Intent(applicationContext, PebbleService::class.java)
        if (PebbleService.isInstanceCreated()) stopService(pebbleServiceIntent) else ContextCompat.startForegroundService(
            this,
            pebbleServiceIntent
        )
    }

    private fun stopGarminConnectIQ() {
        if (GarminConnectIQ.isInstanceCreated) {
            toggleGarminConnectIQ()
        }
    }

    private fun toggleGarminConnectIQ() {
        val garminConnectIQIntent = Intent(applicationContext, GarminConnectIQ::class.java)
        if (GarminConnectIQ.isInstanceCreated) stopService(garminConnectIQIntent) else ContextCompat.startForegroundService(
            this,
            garminConnectIQIntent
        )
    }

    fun toggleSwitchMiBand(): MiBandEnum {
        Timber.i("[core] toggleSwitchMiBand called")
        val buttonMiBand = WheelLog.AppConfig.mibandMode.next()
        WheelLog.AppConfig.mibandMode = buttonMiBand
        WheelLog.Notifications.update()
        return buttonMiBand
    }

    fun toggleLogger(): Boolean {
        Timber.i("[core] toggleLogger called")
        val dataLoggerServiceIntent = Intent(applicationContext, LoggingService::class.java)
        if (LoggingService.isStarted) {
            stopService(dataLoggerServiceIntent)
            return false
        } else if (bleConnector.connectionState == BleStateEnum.Connected) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                startService(dataLoggerServiceIntent)
            } else {
                startForegroundService(dataLoggerServiceIntent)
            }
            return true
        }
        return false
    }

    private val mCoreBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.i("[core] BroadcastReceiver onReceive, action = ${intent.action}")
            when (intent.action) {
                Constants.ACTION_BLUETOOTH_CONNECTION_STATE -> {
                    val connectionState = BleStateEnum.values()[intent.getIntExtra(
                        Constants.INTENT_EXTRA_CONNECTION_STATE,
                        BleStateEnum.Disconnected.ordinal
                    )]
                    Timber.i("[core] Bluetooth state = %s", connectionState)
                    when (connectionState) {
                        BleStateEnum.Connected -> {
                            if (bleConnector.deviceAddress?.isNotEmpty() == true) {
                                WheelLog.AppConfig.lastMac = bleConnector.deviceAddress!!
                                ElectroClub.instance.getAndSelectGarageByMac {  }
                                if (WheelLog.AppConfig.useBeepOnVolumeUp) {
                                    WheelLog.VolumeKeyController.setActive(true)
                                }
                            }
                            if (!LoggingService.isStarted &&
                                WheelLog.AppConfig.autoLog &&
                                !WheelLog.AppConfig.startAutoLoggingWhenIsMoving) {
                                toggleLogger()
                            }
                            if (WheelData.getInstance().wheelType == WHEEL_TYPE.KINGSONG) {
                                KingsongAdapter.getInstance().requestNameData()
                            }
                            if (WheelLog.AppConfig.autoWatch && wearOs == null) {
                                toggleWatch()
                            }
                            WheelLog.Notifications.notificationMessageId = R.string.connected
                        }
                        BleStateEnum.Disconnected -> {
                            if (WheelLog.AppConfig.useBeepOnVolumeUp) {
                                WheelLog.VolumeKeyController.setActive(false)
                            }
                            when (WheelData.getInstance().wheelType) {
                                WHEEL_TYPE.INMOTION -> {
                                    InMotionAdapter.newInstance()
                                    InmotionAdapterV2.newInstance()
                                    NinebotZAdapter.newInstance()
                                    NinebotAdapter.newInstance()
                                }
                                WHEEL_TYPE.INMOTION_V2 -> {
                                    InmotionAdapterV2.newInstance()
                                    NinebotZAdapter.newInstance()
                                    NinebotAdapter.newInstance()
                                }
                                WHEEL_TYPE.NINEBOT_Z -> {
                                    NinebotZAdapter.newInstance()
                                    NinebotAdapter.newInstance()
                                }
                                WHEEL_TYPE.NINEBOT -> NinebotAdapter.newInstance()
                                else -> {
                                }
                            }
                            WheelLog.Notifications.notificationMessageId = R.string.disconnected
                        }
                        BleStateEnum.Connecting ->
                            WheelLog.Notifications.notificationMessageId =
                                if (bleConnector.autoConnect) {
                                    R.string.searching
                                } else {
                                    R.string.connecting
                                }
                        else -> {
                        }
                    }
                    WheelLog.Notifications.update()
                }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> {
                    wearOs?.updateData()
                    if (WheelLog.AppConfig.mibandMode !== MiBandEnum.Alarm) {
                        WheelLog.Notifications.update()
                    }
                    if (!LoggingService.isStarted &&
                        WheelLog.AppConfig.startAutoLoggingWhenIsMoving &&
                        WheelLog.AppConfig.autoLog && WheelData.getInstance().speedDouble > 3.5
                    ) {
                        toggleLogger()
                    }
                }
                Constants.ACTION_PEBBLE_SERVICE_TOGGLED -> {
                    WheelLog.Notifications.update()
                }
                Constants.ACTION_LOGGING_SERVICE_TOGGLED -> {
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_CONNECTION -> {
                    bleConnector.toggleConnectToWheel()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_LOGGING -> {
                    toggleLogger()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_WATCH -> {
                    toggleWatch()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_BEEP -> playBeep(
                    applicationContext
                )
                Constants.NOTIFICATION_BUTTON_LIGHT -> if (WheelData.getInstance().adapter != null) {
                    WheelData.getInstance().adapter.switchFlashlight()
                }
                Constants.NOTIFICATION_BUTTON_MIBAND -> toggleSwitchMiBand()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        startForeground(Constants.MAIN_NOTIFICATION_ID, WheelLog.Notifications.notification)
        bleConnector = BleConnector(this).apply {
            deviceAddress = WheelLog.AppConfig.lastMac
            toggleConnectToWheel()
        }
        registerReceiver(mCoreBroadcastReceiver, makeCoreIntentFilter())
        Timber.i("[core] service started!")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun unbindService(conn: ServiceConnection) {
        Timber.i("[core] service unbind!")
        bleConnector.disconnect()
        bleConnector.close()
        stopGarminConnectIQ()
        stopPebbleService()
        try {
            super.unbindService(conn)
        } catch (e: IllegalArgumentException) {
            Timber.i("[core] unbind exception: ${e.message}")
        }
    }

    private fun makeCoreIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
            addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
            addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
            addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED)
            addAction(Constants.ACTION_PREFERENCE_RESET)
            addAction(Constants.NOTIFICATION_BUTTON_CONNECTION)
            addAction(Constants.NOTIFICATION_BUTTON_WATCH)
            addAction(Constants.NOTIFICATION_BUTTON_LOGGING)
            addAction(Constants.NOTIFICATION_BUTTON_BEEP)
            addAction(Constants.NOTIFICATION_BUTTON_LIGHT)
            addAction(Constants.NOTIFICATION_BUTTON_MIBAND)
        }
    }

    inner class LocalBinder : Binder() {
        val service
            get() = this@CoreService
    }
}