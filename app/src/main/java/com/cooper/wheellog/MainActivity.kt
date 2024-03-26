package com.cooper.wheellog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.DialogInterface.OnShowListener
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.media.AudioManager
import android.os.*
import android.util.Rational
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.cooper.wheellog.BluetoothService.LocalBinder
import com.cooper.wheellog.DialogHelper.checkAndShowPrivatePolicyDialog
import com.cooper.wheellog.DialogHelper.checkBatteryOptimizationsAndShowAlert
import com.cooper.wheellog.DialogHelper.checkPWMIsSetAndShowAlert
import com.cooper.wheellog.companion.WearOs
import com.cooper.wheellog.data.TripDatabase.Companion.getDataBase
import com.cooper.wheellog.databinding.ActivityMainBinding
import com.cooper.wheellog.settings.mainScreen
import com.cooper.wheellog.ui.theme.AppTheme
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.Alarms.checkAlarm
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.PermissionsUtil.checkBlePermissions
import com.cooper.wheellog.utils.PermissionsUtil.checkExternalFilePermission
import com.cooper.wheellog.utils.PermissionsUtil.checkNotificationsPermissions
import com.cooper.wheellog.utils.PermissionsUtil.isMaxBleReq
import com.cooper.wheellog.utils.SomeUtil.getSerializable
import com.cooper.wheellog.utils.SomeUtil.playBeep
import com.cooper.wheellog.views.PiPView
import com.google.android.material.snackbar.Snackbar
import com.welie.blessed.ConnectionState
// import com.yandex.metrica.YandexMetrica
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var eventsLoggingTree: EventsLoggingTree? = null
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    //region private variables
    private lateinit var binding: ActivityMainBinding
    lateinit var pager: ViewPager2
    lateinit var pagerAdapter: MainPageAdapter
    lateinit var pipView: ComposeView
    var mMenu: Menu? = null
    private var miSearch: MenuItem? = null
    private var miWheel: MenuItem? = null
    private var miWatch: MenuItem? = null
    private var miBand: MenuItem? = null
    private var miLogging: MenuItem? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mDeviceAddress: String = ""
    private var mConnectionState = ConnectionState.DISCONNECTED
    private var isWheelSearch = false
    private var doubleBackToExitPressedOnce = false
    private var snackbar: Snackbar? = null
    private val timeFormatter = SimpleDateFormat("HH:mm:ss ", Locale.US)
    private var wearOs: WearOs? = null
    private val speedModel: PiPView.SpeedModel by lazy { PiPView.SpeedModel() }
    private var settingsNavHostController: NavHostController? = null
    private val bluetoothService: BluetoothService?
        get() = WheelData.getInstance().bluetoothService
    private var loggingService: LoggingService? = null
    inner class ServicesConnection: ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            when (componentName.className) {
                BluetoothService::class.java.name -> {
                    val bluetoothService = (service as LocalBinder).getService()
                    WheelData.getInstance().bluetoothService = bluetoothService
                    if (bluetoothService.connectionState == ConnectionState.DISCONNECTED && mDeviceAddress.isNotEmpty()) {
                        bluetoothService.wheelAddress = mDeviceAddress
                        toggleConnectToWheel()
                    }
                }
                LoggingService::class.java.name -> {
                    loggingService = (service as LoggingService.LocalBinder).getService()
                }
            }
        }

        fun disconnect(componentName: ComponentName?) {
            when (componentName?.className) {
                BluetoothService::class.java.name -> {
                    WheelData.getInstance().bluetoothService = null
                    WheelData.getInstance().isConnected = false
                    Timber.e("BluetoothService disconnected")
                }
                LoggingService::class.java.name -> {
                    loggingService = null
                    Timber.e("LoggingService disconnected")
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            disconnect(componentName)
        }

        override fun onBindingDied(name: ComponentName?) {
            disconnect(name)
        }
    }
    private val mBLEServiceConnection: ServiceConnection = ServicesConnection()
    private val mLoggingServiceConnection: ServiceConnection = ServicesConnection()

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
            togglePipView(show = isInPictureInPictureMode)
        }
    }

    private fun togglePipView(show: Boolean) {
        if (show) {
            try {
                ContextCompat.registerReceiver(
                    this,
                    mPiPBroadcastReceiver,
                    makeIntentPipFilter(),
                    ContextCompat.RECEIVER_EXPORTED
                )
            } catch (_: Exception) {
                // ignore
            } finally {
//                binding.toolbar.visibility = View.GONE
//                pager.visibility = View.GONE
//                binding.indicator.visibility = View.GONE
                pipView.setContent {
                    PiPView().SpeedWidget(modifier = Modifier.fillMaxSize(), model = speedModel)
                }
                pipView.visibility = View.VISIBLE
            }
        } else {
            try {
                unregisterReceiver(mPiPBroadcastReceiver)
            } catch (_: Exception) {
                // ignore
            }
//            binding.toolbar.visibility = View.VISIBLE
//            pager.visibility = View.VISIBLE
//            binding.indicator.visibility = View.VISIBLE
            pipView.visibility = View.GONE
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (WheelLog.AppConfig.usePipMode
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !this.isInPictureInPictureMode) {
            when (WheelLog.AppConfig.pipBlock) {
                getString(R.string.consumption) -> speedModel.title = getString(R.string.consumption)
                else -> speedModel.title = getString(R.string.speed)
            }
            this.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    private fun setConnectionState(connectionState: ConnectionState, wheelSearch: Boolean) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                pagerAdapter.configureSecondDisplay()
                if (mDeviceAddress.isNotEmpty()) {
                    WheelLog.AppConfig.lastMac = mDeviceAddress
                    if (WheelLog.AppConfig.autoUploadEc && WheelLog.AppConfig.ecToken != null) {
                        ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                            WheelLog.AppConfig.lastMac,
                            this
                        ) { }
                    }
                    if (WheelLog.AppConfig.useBeepOnVolumeUp) {
                        WheelLog.VolumeKeyController.setActive(true)
                    }
                }
                hideSnackBar()
            }
            ConnectionState.DISCONNECTED -> if (wheelSearch) {
                if (mConnectionState != ConnectionState.DISCONNECTED && bluetoothService?.getDisconnectTime() != null) {
                    val text = bluetoothService?.getDisconnectTime()
                        ?.let { timeFormatter.format(it) } +
                            getString(R.string.connection_lost_at)
                    showSnackBar(text, Snackbar.LENGTH_INDEFINITE)
                }
            } else {
                if (WheelLog.AppConfig.useBeepOnVolumeUp) {
                    WheelLog.VolumeKeyController.setActive(false)
                }
            }
            else -> {}
        }
        mConnectionState = connectionState
        isWheelSearch = wheelSearch
        setMenuIconStates()
    }

    /**
     * Broadcast receiver for MainView UI. It should only work with UI elements.
     * Intents are accepted only if MainView is active.
     */
    private val mMainViewBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isPaused) {
                return
            }
            when (intent.action) {
                Constants.ACTION_WHEEL_TYPE_CHANGED -> {
                    Timber.i("Wheel type switched")
                    pagerAdapter.configureSecondDisplay()
                    pagerAdapter.updateScreen(true)
                }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> {
                    pagerAdapter.updateScreen(
                        intent.hasExtra(
                            Constants.INTENT_EXTRA_GRAPH_UPDATE_AVAILABLE
                        )
                    )
                }
                Constants.ACTION_WHEEL_NEWS_AVAILABLE -> {
                    Timber.i("Received news")
                    showSnackBar(intent.getStringExtra(Constants.INTENT_EXTRA_NEWS), 1500)
                }
                Constants.ACTION_WHEEL_TYPE_RECOGNIZED -> {}
                Constants.ACTION_WHEEL_MODEL_CHANGED -> pagerAdapter.configureSmartBmsDisplay()
                Constants.ACTION_ALARM_TRIGGERED -> {
                    val alarmType = intent.getSerializable(Constants.INTENT_EXTRA_ALARM_TYPE, ALARM_TYPE::class.java)?.value ?: 0
                    val alarmValue = intent.getDoubleExtra(Constants.INTENT_EXTRA_ALARM_VALUE, 0.0)
                    if (alarmType < 4) {
                        showSnackBar(
                            resources.getString(R.string.alarm_text_speed) + String.format(
                                ": %.1f",
                                alarmValue
                            ), 3000
                        )
                    }
                    if (alarmType == ALARM_TYPE.CURRENT.value) {
                        showSnackBar(
                            resources.getString(R.string.alarm_text_current) + String.format(
                                ": %.1f",
                                alarmValue
                            ), 3000
                        )
                    }
                    if (alarmType == ALARM_TYPE.TEMPERATURE.value) {
                        showSnackBar(
                            resources.getString(R.string.alarm_text_temperature) + String.format(
                                ": %.1f",
                                alarmValue
                            ), 3000
                        )
                    }
                    if (alarmType == ALARM_TYPE.PWM.value) {
                        showSnackBar(
                            resources.getString(R.string.alarm_text_pwm) + String.format(
                                ": %.1f",
                                alarmValue * 100
                            ), 3000
                        )
                    }
                    if (alarmType == ALARM_TYPE.BATTERY.value) {
                        showSnackBar(
                            resources.getString(R.string.alarm_text_battery) + String.format(
                                ": %.0f",
                                alarmValue
                            ), 3000
                        )
                    }
                }
                Constants.ACTION_WHEEL_IS_READY -> checkPWMIsSetAndShowAlert(this@MainActivity)
            }
        }
    }

    /**
     * Broadcast receiver for MainView UI for Picture in Picture mode.
     * It should only work with UI elements.
     */
    private val mPiPBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_WHEEL_TYPE_CHANGED -> {
                    Timber.i("Wheel type switched")
                }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> {
                    when (WheelLog.AppConfig.pipBlock) {
                        getString(R.string.consumption) -> speedModel.value.floatValue = Calculator.whByKm.toFloat()
                        else -> speedModel.value.floatValue = WheelData.getInstance().speed / 10f
                    }
                    pipView.invalidate()
                }
            }
        }
    }

    /**
     * A broadcast receiver that always works. It shouldn't have any UI work.
     */
    private val mCoreBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_BLUETOOTH_CONNECTION_STATE -> {
                    val connectionState = ConnectionState.fromValue(
                        intent.getIntExtra(
                            Constants.INTENT_EXTRA_CONNECTION_STATE,
                            ConnectionState.DISCONNECTED.value
                        )
                    )
                    loggingService?.updateConnectionState(connectionState)
                    val isWheelSearch =
                        intent.getBooleanExtra(Constants.INTENT_EXTRA_WHEEL_SEARCH, false)
                    val isDirectConnectionFailed =
                        intent.getBooleanExtra(Constants.INTENT_EXTRA_DIRECT_SEARCH_FAILED, false)
                    Timber.i(
                        "Bluetooth state = %s, wheel search is %s",
                        connectionState,
                        isWheelSearch
                    )
                    if (connectionState == ConnectionState.DISCONNECTED && isWheelSearch && isDirectConnectionFailed) {
                        showSnackBar(R.string.bluetooth_direct_connect_failed)
                    }
                    setConnectionState(connectionState, isWheelSearch)
                    WheelData.getInstance().isConnected =
                        connectionState == ConnectionState.CONNECTED
                    when (connectionState) {
                        ConnectionState.CONNECTED -> {
                            if (!LoggingService.isInstanceCreated() &&
                                WheelLog.AppConfig.autoLog &&
                                WheelLog.AppConfig.startAutoLoggingWhenIsMovingMore == 0f
                            ) {
                                toggleLoggingService()
                            }
                            if (WheelData.getInstance().wheelType == WHEEL_TYPE.KINGSONG) {
                                KingsongAdapter.getInstance().requestNameData()
                            }
                            if (WheelLog.AppConfig.autoWatch && wearOs == null) {
                                toggleWatch()
                            }
                            WheelLog.Notifications.notificationMessageId = R.string.connected
                        }
                        ConnectionState.DISCONNECTING, ConnectionState.DISCONNECTED -> if (isWheelSearch) {
                            if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT)) {
                                WheelLog.Notifications.notificationMessageId = R.string.searching
                            } else {
                                WheelLog.Notifications.notificationMessageId = R.string.connecting
                            }
                        } else {
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
                                else -> {}
                            }
                            WheelLog.Notifications.notificationMessageId = R.string.disconnected
                        }
                        else -> {}
                    }
                    WheelLog.Notifications.update()
                }
                Constants.ACTION_PREFERENCE_RESET -> {
                    Timber.i("Reset battery lowest")
                    pagerAdapter.wheelView?.resetBatteryLowest()
                }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> {
                    loggingService?.updateFile()
                    if (wearOs != null) {
                        wearOs!!.sendUpdateData()
                    }
                    if (WheelLog.AppConfig.mibandMode !== MiBandEnum.Alarm) {
                        WheelLog.Notifications.update()
                    }
                    if (!LoggingService.isInstanceCreated() &&
                        WheelLog.AppConfig.startAutoLoggingWhenIsMovingMore != 0f &&
                        WheelLog.AppConfig.autoLog &&
                        WheelData.getInstance().speedDouble > WheelLog.AppConfig.startAutoLoggingWhenIsMovingMore
                    ) {
                        toggleLoggingService()
                    }
                    if (WheelLog.AppConfig.alarmsEnabled) {
                        checkAlarm(
                            WheelData.getInstance().calculatedPwm / 100,
                            applicationContext
                        )
                    }
                }
                Constants.ACTION_PEBBLE_SERVICE_TOGGLED -> {
                    setMenuIconStates()
                    WheelLog.Notifications.update()
                }
                Constants.ACTION_LOGGING_SERVICE_TOGGLED -> {
                    val running = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_RUNNING, false)
                    if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
                        val filepath =
                            intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)
                        val fileName = filepath!!.substring(filepath.lastIndexOf("\\") + 1)
                        if (running) {
                            showSnackBar(
                                resources.getString(R.string.started_logging) + " " + fileName,
                                5000
                            )
                        }
                    }
                    setMenuIconStates()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_CONNECTION -> {
                    toggleConnectToWheel()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_LOGGING -> {
                    toggleLogging()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_WATCH -> {
                    toggleWatch()
                    WheelLog.Notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_BEEP -> playBeep()
                Constants.NOTIFICATION_BUTTON_LIGHT -> if (WheelData.getInstance().adapter != null) {
                    WheelData.getInstance().adapter.switchFlashlight()
                }
                Constants.NOTIFICATION_BUTTON_MIBAND -> toggleSwitchMiBand()
            }
        }
    }

    private fun toggleWatch() {
        togglePebbleService()
        // TODO: Fix garmin for API 34
        // if (WheelLog.AppConfig.garminConnectIqEnable) toggleGarminConnectIQ() else stopGarminConnectIQ()
        toggleWearOs()
    }

    private fun toggleLogging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toggleLoggingService()
        } else {
            checkExternalFilePermission(this, RESULT_REQUEST_PERMISSIONS_IO)
        }
    }

    private fun setMenuIconStates() {
        if (mMenu == null) return
        if (mDeviceAddress.isEmpty()) {
            miWheel!!.isEnabled = false
            miWheel!!.icon!!.alpha = 64
        } else {
            miWheel!!.isEnabled = true
            miWheel!!.icon!!.alpha = 255
        }
        when (WheelLog.AppConfig.mibandMode) {
            MiBandEnum.Alarm -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandAlarm))
            MiBandEnum.Min -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMin))
            MiBandEnum.Medium -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMed))
            MiBandEnum.Max -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMax))
        }

        miBand?.isVisible = WheelLog.AppConfig.mainMenuButtons.contains("miband")
        miWatch?.isVisible = WheelLog.AppConfig.mainMenuButtons.contains("watch")
        mMenu?.findItem(R.id.miReset)?.isVisible = WheelLog.AppConfig.mainMenuButtons.contains("reset")

        if (PebbleService.isInstanceCreated()) {
            miWatch!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuWatchOn))
        } else {
            miWatch!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuWatchOff))
        }
        if (LoggingService.isInstanceCreated()) {
            miLogging!!.setTitle(R.string.stop_data_service)
            miLogging!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuLogOn))
        } else {
            miLogging!!.setTitle(R.string.start_data_service)
            miLogging!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuLogOff))
        }
        when (mConnectionState) {
            ConnectionState.CONNECTED -> {
                miWheel!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuWheelOn))
                miWheel!!.setTitle(R.string.disconnect_from_wheel)
                miSearch!!.isEnabled = false
                miSearch!!.icon!!.alpha = 64
                miLogging!!.isEnabled = true
                miLogging!!.icon!!.alpha = 255
            }
            ConnectionState.DISCONNECTED -> {
                if (isWheelSearch) {
                    miWheel!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuWheelSearch))
                    miWheel!!.setTitle(R.string.disconnect_from_wheel)
                    (miWheel!!.icon as AnimationDrawable?)!!.start()
                    miSearch!!.isEnabled = false
                    miSearch!!.icon!!.alpha = 64
                } else {
                    miWheel!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuWheelOff))
                    miWheel!!.setTitle(R.string.connect_to_wheel)
                    miSearch!!.isEnabled = true
                    miSearch!!.icon!!.alpha = 255
                }
                miLogging!!.isEnabled = false
                miLogging!!.icon!!.alpha = 64
            }
            else -> {}
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pagerAdapter.updateScreen(true)
    }

    private fun createPager() {
        // add pages into main view
        pager = binding.pager
        pager.offscreenPageLimit = 10
        val pages = ArrayList<Int>()
        pages.add(R.layout.main_view_main)
        pages.add(R.layout.main_view_params_list)
        if (WheelLog.AppConfig.pageGraph) {
            pages.add(R.layout.main_view_graph)
        }
        if (WheelLog.AppConfig.pageTrips) {
            pages.add(R.layout.main_view_trips)
        }
        if (WheelLog.AppConfig.pageEvents) {
            pages.add(R.layout.main_view_events)
        }
        pagerAdapter = MainPageAdapter(pages, this)
        pager.adapter = pagerAdapter
        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pagerAdapter.position = position
                pagerAdapter.updateScreen(true)
            }
        })
        eventsLoggingTree = EventsLoggingTree(applicationContext, pagerAdapter)
        Timber.plant(eventsLoggingTree!!)
        val indicator = binding.indicator
        indicator.setViewPager(pager)
        pagerAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (onDestroyProcess) {
            Process.killProcess(Process.myPid())
            return
        }
        AppCompatDelegate.setDefaultNightMode(WheelLog.AppConfig.dayNightThemeMode)
        setTheme(WheelLog.AppConfig.appTheme)
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ElectroClub.instance.apply {
            lifecycle.coroutineScope.launch {
                dao = getDataBase(this@MainActivity).tripDao()
            }
            errorListener = { method: String?, error: String? ->
                val message = "[ec] $method error: $error"
                Timber.i(message)
                runOnUiThread { showSnackBar(message, 4000) }
            }
            successListener = label@{ method: String?, success: Any? ->
                if (method == ElectroClub.GET_GARAGE_METHOD) {
                    return@label
                }
                val message = "[ec] $method ok: $success"
                Timber.i(message)
                runOnUiThread { showSnackBar(message, 4000) }
            }
        }
        createPager()
        pipView = binding.pipView

        // clock font
        binding.textClock.typeface = ThemeManager.getTypeface(applicationContext)
        mDeviceAddress = WheelLog.AppConfig.lastMac
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        checkAndShowPrivatePolicyDialog(this)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
        }

        // Checks if Bluetooth is supported on the device.
        val bluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
        } else if (!mBluetoothAdapter!!.isEnabled) {
            if (checkBlePermissions(this, RESULT_REQUEST_PERMISSIONS_BT)) {
                // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
                // fire an intent to display a dialog asking the user to grant permission to enable it.
                enableBleLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            startBluetoothService()
        }

        try {
            unregisterReceiver(mCoreBroadcastReceiver)
        } catch (_: Exception) {
            // ignore
        }
        ContextCompat.registerReceiver(
            this,
            mCoreBroadcastReceiver,
            makeCoreIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED
        )
        WheelLog.Notifications.update()

        binding.settingsView.apply {
            setContent {
                AppTheme(useDarkTheme = true) {
                    settingsNavHostController = rememberNavController()
                    mainScreen(navController = settingsNavHostController!!)
                }
            }
        }

        checkBatteryOptimizationsAndShowAlert(this)
        // for test without wheel go to isHardwarePWM and comment Unknown case
        // DialogHelper.INSTANCE.checkPWMIsSetAndShowAlert(this);
    }

    private fun checkClockVisible() {
        if (WheelLog.AppConfig.showClock) {
            binding.textClock.visibility = View.VISIBLE
        } else {
            binding.textClock.visibility = View.GONE
        }
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    public override fun onResume() {
        super.onResume()
        isPaused = false
        if (bluetoothService != null && mConnectionState != bluetoothService!!.connectionState && isWheelSearch != bluetoothService!!.isWheelSearch) {
            setConnectionState(
                bluetoothService!!.connectionState,
                bluetoothService!!.isWheelSearch
            )
        }
        if (WheelData.getInstance().wheelType != WHEEL_TYPE.Unknown) {
            pagerAdapter.configureSecondDisplay()
        }
        if (checkNotificationsPermissions(this)) {
            WheelLog.Notifications.update()
        }
        try {
            ContextCompat.registerReceiver(
                this,
                mMainViewBroadcastReceiver,
                makeIntentFilter(),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
        pagerAdapter.updateScreen(true)
        pagerAdapter.updatePageOfTrips()

        checkClockVisible()

        // Checking GPS is enabled
        DialogHelper.checkAndShowLocationDialog(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setMenuIconStates()
    }

    public override fun onPause() {
        super.onPause()
        isPaused = true
        try {
            unregisterReceiver(mMainViewBroadcastReceiver)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            togglePipView(show = true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            togglePipView(show = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ConnectionState::class.simpleName, mConnectionState.value)
        outState.putBoolean(isWheelSearch::class.simpleName, isWheelSearch)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mConnectionState = ConnectionState.fromValue(savedInstanceState.getInt(ConnectionState::class.simpleName, ConnectionState.DISCONNECTED.value))
        isWheelSearch = savedInstanceState.getBoolean(isWheelSearch::class.simpleName, false)
        setConnectionState(mConnectionState, isWheelSearch)
        setMenuIconStates()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!this.isFinishing) {
            Timber.wtf("Recreate main activity")
            return
        }
        // Real finish.
        Timber.wtf("-=[ finish ]=-")
        onDestroyProcess = true
        if (wearOs != null) {
            wearOs!!.stop()
        }
        stopPebbleService()
        stopGarminConnectIQ()
        stopLoggingService()
        WheelData.getInstance().full_reset()
        if (bluetoothService != null) {
            try {
                unbindService(mBLEServiceConnection)
                WheelData.getInstance().bluetoothService = null
            } catch (_: Exception) {
                // ignored
            }
        }
        if (loggingService != null) {
            try {
                unbindService(mLoggingServiceConnection)
            } catch (_: Exception) {
                // ignored
            }
        }
        ThemeManager.changeAppIcon(this@MainActivity)
        object : CountDownTimer((2 * 60 * 1000 /* 2 min */).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!LoggingService.isInstanceCreated()) {
                    onFinish()
                }
            }

            override fun onFinish() {
                WheelLog.Notifications.close()
                Timber.uproot(eventsLoggingTree!!)
                eventsLoggingTree!!.close()
                eventsLoggingTree = null
                try {
                    unregisterReceiver(mCoreBroadcastReceiver)
                } catch (_: Exception) {
                    // ignore
                }
                // Kill YandexMetrika process.
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val runningProcesses = am.runningAppProcesses
                for (process in runningProcesses) {
                    if (Process.myPid() != process.pid) {
                        Process.killProcess(process.pid)
                    }
                }
                // Kill self process.
                Process.killProcess(Process.myPid())
            }
        }.start()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.wtf("[Warning] Low memory")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        mMenu = menu.apply {
            miSearch = findItem(R.id.miSearch)
            miWheel = findItem(R.id.miWheel)
            miWatch = findItem(R.id.miWatch)
            miBand = findItem(R.id.miBand)
            miLogging = findItem(R.id.miLogging)
        }

        // Themes
        if (WheelLog.AppConfig.appTheme == R.style.AJDMTheme) {
            val miSettings = mMenu!!.findItem(R.id.miSettings)
            miSettings.setIcon(ThemeManager.getId(ThemeIconEnum.MenuSettings))
            miSearch!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuBluetooth))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Alarms.vibrate(this, longArrayOf(0, 30, 40))
        return when (item.itemId) {
            R.id.miSearch -> {
                startScanActivity()
                true
            }
            R.id.miWheel -> {
                toggleConnectToWheel()
                true
            }
            R.id.miLogging -> {
                if (LoggingService.isInstanceCreated() && WheelLog.AppConfig.continueThisDayLog) {
                    val dialog = AlertDialog.Builder(this)
                        .setTitle(R.string.continue_this_day_log_alert_title)
                        .setMessage(R.string.continue_this_day_log_alert_description)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            WheelLog.AppConfig.continueThisDayLogMacException =
                                WheelLog.AppConfig.lastMac
                            toggleLogging()
                        }
                        .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> toggleLogging() }
                        .create()
                    dialog.setOnShowListener(object : OnShowListener {
                        val AUTO_DISMISS_MILLIS = 5000
                        override fun onShow(dialog: DialogInterface) {
                            val defaultButton =
                                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                            val negativeButtonText = defaultButton.text
                            object : CountDownTimer(AUTO_DISMISS_MILLIS.toLong(), 100) {
                                override fun onTick(millisUntilFinished: Long) {
                                    defaultButton.text = String.format(
                                        Locale.getDefault(), "%s (%d)",
                                        negativeButtonText,
                                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                                    )
                                }

                                override fun onFinish() {
                                    if (dialog.isShowing) {
                                        WheelLog.AppConfig.continueThisDayLogMacException =
                                            WheelLog.AppConfig.lastMac
                                        toggleLogging()
                                        dialog.dismiss()
                                    }
                                }
                            }.start()
                        }
                    })
                    dialog.show()
                } else {
                    toggleLogging()
                }
                true
            }
            R.id.miWatch -> {
                toggleWatch()
                true
            }
            R.id.miBand -> {
                toggleSwitchMiBand()
                true
            }
            R.id.miReset -> {
                WheelData.getInstance().resetExtremumValues()
                showSnackBar(getString(R.string.reset_extremum_values_title))
                true
            }
            R.id.miSettings -> {
                toggleSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun toggleSettings() {
        if (binding.settingsView.visibility != View.VISIBLE) {
            binding.settingsView.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setListener(null)
            }
        } else {
            binding.settingsView
                .animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.settingsView.visibility = View.GONE
                    }
                })

            checkClockVisible()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // If settings is visible, hide it.
                if (binding.settingsView.visibility == View.VISIBLE) {
                    if (settingsNavHostController != null) {
                        if (settingsNavHostController?.previousBackStackEntry == null) {
                            toggleSettings()
                        } else {
                            settingsNavHostController?.navigateUp()
                        }
                    } else {
                        toggleSettings()
                    }
                    return true
                }
                if (doubleBackToExitPressedOnce) {
                    finish()
                    return true
                }
                doubleBackToExitPressedOnce = true
                showSnackBar(R.string.back_to_exit)
                Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showSnackBar(msg: Int) {
        showSnackBar(getString(msg))
    }

    private fun showSnackBar(msg: String?, timeout: Int = 2000) {
        if (!isPaused) {
            if (snackbar == null) {
                snackbar = Snackbar
                    .make(binding.mainView, "", Snackbar.LENGTH_LONG).apply {
                        view.setBackgroundResource(R.color.primary_dark)
                        setAction(android.R.string.ok) { }
                    }
            }
            snackbar?.apply {
                duration = timeout
                setText(msg!!)
                show()
            }
        }
        Timber.wtf(msg)
    }

    private fun hideSnackBar() {
        snackbar?.dismiss()
    }

    // region services
    private fun stopLoggingService() {
        if (LoggingService.isInstanceCreated()) {
            toggleLoggingService()
        }
    }

    fun toggleLoggingService() {
        val dataLoggerServiceIntent = Intent(applicationContext, LoggingService::class.java)
        if (LoggingService.isInstanceCreated()) {
            unbindService(mLoggingServiceConnection)
            if (!onDestroyProcess) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { pagerAdapter.updatePageOfTrips() }, 200)
            }
        } else if (mConnectionState == ConnectionState.CONNECTED) {
            bindService(dataLoggerServiceIntent, mLoggingServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun stopPebbleService() {
        if (PebbleService.isInstanceCreated()) togglePebbleService()
    }

    private fun togglePebbleService() {
        val pebbleServiceIntent = Intent(applicationContext, PebbleService::class.java)
        if (PebbleService.isInstanceCreated()) stopService(pebbleServiceIntent) else ContextCompat.startForegroundService(
            this,
            pebbleServiceIntent
        )
    }

    private fun toggleWearOs() {
        wearOs = if (wearOs == null) {
            WearOs(this)
        } else {
            wearOs!!.stop()
            null
        }
    }

    private fun stopGarminConnectIQ() {
        if (GarminConnectIQ.isInstanceCreated) toggleGarminConnectIQ()
    }

    private fun toggleGarminConnectIQ() {
        val garminConnectIQIntent = Intent(applicationContext, GarminConnectIQ::class.java)
        if (GarminConnectIQ.isInstanceCreated) stopService(garminConnectIQIntent) else ContextCompat.startForegroundService(
            this,
            garminConnectIQIntent
        )
    }

    private fun toggleSwitchMiBand() {
        val buttonMiBand = WheelLog.AppConfig.mibandMode.next()
        WheelLog.AppConfig.mibandMode = buttonMiBand
        WheelLog.Notifications.update()
        when (buttonMiBand) {
            MiBandEnum.Alarm -> showSnackBar(R.string.alarmmiband)
            MiBandEnum.Min -> showSnackBar(R.string.minmiband)
            MiBandEnum.Medium -> showSnackBar(R.string.medmiband)
            MiBandEnum.Max -> showSnackBar(R.string.maxmiband)
        }
        setMenuIconStates()
    }

    private fun startBluetoothService() {
        if (checkBlePermissions(this, RESULT_REQUEST_PERMISSIONS_BT)
            && bluetoothService == null
        ) {
            val bluetoothServiceIntent = Intent(applicationContext, BluetoothService::class.java)
            bindService(bluetoothServiceIntent, mBLEServiceConnection, BIND_AUTO_CREATE)
//            YandexMetrica.reportEvent("BluetoothService is starting.")
        } else if (isMaxBleReq) {
            showSnackBar(R.string.bluetooth_required)
        }
    }

    private fun toggleConnectToWheel() {
        if (bluetoothService != null) {
            bluetoothService!!.toggleConnectToWheel()
        } else {
            startBluetoothService()
        }
    }

    private fun startScanActivity() {
        if (checkBlePermissions(this, RESULT_REQUEST_PERMISSIONS_BT)) {
            scanLauncher.launch(Intent(this@MainActivity, ScanActivity::class.java))
        } else if (isMaxBleReq) {
            showSnackBar(R.string.bluetooth_required)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RESULT_REQUEST_PERMISSIONS_BT) {
            startBluetoothService()
        } else if (requestCode == RESULT_REQUEST_PERMISSIONS_IO) {
            toggleLoggingService()
        }
    }
    // endregion

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && bluetoothService != null) {
            mDeviceAddress = result.data?.getStringExtra("MAC") ?: ""
            Timber.i("Device selected = %s", mDeviceAddress)
            val mDeviceName = result.data?.getStringExtra("NAME")
            Timber.i("Device selected = %s", mDeviceName)
            bluetoothService!!.wheelAddress = mDeviceAddress
            WheelData.getInstance().full_reset()
            WheelData.getInstance().btName = mDeviceName
            pagerAdapter.updateScreen(true)
            setMenuIconStates()
            toggleConnectToWheel()
            if (WheelLog.AppConfig.autoUploadEc && WheelLog.AppConfig.ecToken != null) {
                ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                    mDeviceAddress,
                    this
                ) { }
            }
        } else {
            Timber.i("Scan device is failed.")
        }
    }

    private val enableBleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && mBluetoothAdapter!!.isEnabled) {
            startBluetoothService()
        } else {
            Toast.makeText(this, R.string.bluetooth_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun makeIntentPipFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_WHEEL_MODEL_CHANGED)
        return intentFilter
    }

    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED)
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_RECOGNIZED)
        intentFilter.addAction(Constants.ACTION_WHEEL_MODEL_CHANGED)
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED)
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_CHANGED)
        intentFilter.addAction(Constants.ACTION_WHEEL_NEWS_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_WHEEL_IS_READY)
        return intentFilter
    }

    private fun makeCoreIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED)
        intentFilter.addAction(Constants.ACTION_PREFERENCE_RESET)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_CONNECTION)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_WATCH)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_LOGGING)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_BEEP)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_LIGHT)
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_MIBAND)
        return intentFilter
    }

    companion object {
        lateinit var audioManager: AudioManager

        const val RESULT_REQUEST_PERMISSIONS_BT = 40
        const val RESULT_REQUEST_PERMISSIONS_IO = 50
        private var onDestroyProcess = false

        var isPaused = true
    }
}