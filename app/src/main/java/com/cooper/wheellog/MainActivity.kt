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
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Rational
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cooper.wheellog.data.CsvImportViewModel
import com.cooper.wheellog.data.CsvImportState
import com.cooper.wheellog.views.showTripBrowserDialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.cooper.wheellog.BluetoothService.LocalBinder
import com.cooper.wheellog.DialogHelper.checkAndShowPrivatePolicyDialog
import com.cooper.wheellog.DialogHelper.checkBatteryOptimizationsAndShowAlert
import com.cooper.wheellog.DialogHelper.checkPWMIsSetAndShowAlert
import com.cooper.wheellog.companion.WearOs
import com.cooper.wheellog.compose.MainScreen
import com.cooper.wheellog.data.TripDatabase.Companion.getDataBase
import com.cooper.wheellog.data.TripParser
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
// import com.yandex.metrica.YandexMetrica
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val appConfig: AppConfig by inject()
    private val notifications: NotificationUtil by inject()
    private val volumeKeyController: VolumeKeyController by inject()
    private var eventsLoggingTree: EventsLoggingTree? = null
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    private val csvImports by lazy { ViewModelProvider(this)[CsvImportViewModel::class.java] }
    private var csvImportBar: Snackbar? = null
    private var csvImportDialog: androidx.appcompat.app.AlertDialog? = null

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
        if (appConfig.usePipMode
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !this.isInPictureInPictureMode) {
            when (appConfig.pipBlock) {
                getString(R.string.consumption) -> speedModel.title = getString(R.string.consumption)
                else -> speedModel.title = getString(R.string.speed)
            }
            try {
                this.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            } catch (e: RuntimeException) {
                Toast.makeText(this, R.string.pip_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setConnectionState(connectionState: ConnectionState, wheelSearch: Boolean) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                pagerAdapter.configureSecondDisplay()
                if (mDeviceAddress.isNotEmpty()) {
                    appConfig.lastMac = mDeviceAddress
                    if (appConfig.autoUploadEc && appConfig.ecToken != null) {
                        ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                            appConfig.lastMac,
                            this
                        ) { }
                    }
                    if (appConfig.useBeepOnVolumeUp) {
                        volumeKeyController.setActive(true)
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
                if (appConfig.useBeepOnVolumeUp) {
                    volumeKeyController.setActive(false)
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
                    when (appConfig.pipBlock) {
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
                    LoggingService.noteConnection(connectionState == ConnectionState.CONNECTED)
                    when (connectionState) {
                        ConnectionState.CONNECTED -> {
                            if (WheelData.getInstance().wheelType == WHEEL_TYPE.KINGSONG) {
                                KingsongAdapter.getInstance().requestNameData()
                            }
                            if (appConfig.autoWatch && wearOs == null) {
                                toggleWatch()
                            }
                            notifications.notificationMessageId = R.string.connected
                        }
                        ConnectionState.DISCONNECTING, ConnectionState.DISCONNECTED -> if (isWheelSearch) {
                            if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT)) {
                                notifications.notificationMessageId = R.string.searching
                            } else {
                                notifications.notificationMessageId = R.string.connecting
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
                            notifications.notificationMessageId = R.string.disconnected
                        }
                        else -> {}
                    }
                    notifications.update()
                }
                Constants.ACTION_PREFERENCE_RESET -> {
                    Timber.i("Reset battery lowest")
                    pagerAdapter.wheelView?.resetBatteryLowest()
                }
                Constants.ACTION_TRACK_CHANGED -> if (!onDestroyProcess) {
                    pagerAdapter.updatePageOfTrips()
                    updateTrackRecordIcon()
                    notifications.update()
                }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> {
                    if (wearOs != null) {
                        wearOs!!.sendUpdateData()
                    }
                    if (appConfig.mibandMode !== MiBandEnum.Alarm) {
                        notifications.update()
                    }
                    LoggingService.maybeAutoStart(this@MainActivity, appConfig)
                    if (appConfig.alarmsEnabled) {
                        checkAlarm(
                            WheelData.getInstance().calculatedPwm / 100,
                            applicationContext
                        )
                    }
                }
                Constants.ACTION_PEBBLE_SERVICE_TOGGLED -> {
                    setMenuIconStates()
                    notifications.update()
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
                    notifications.update()
                }
                Constants.ACTION_RAW_LOGGING_TOGGLED -> {
                    val running = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_RUNNING, false)
                    val paused = intent.getBooleanExtra("raw_logging_paused", false)
                    val resumed = intent.getBooleanExtra("raw_logging_resumed", false)
                    if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
                        val filepath =
                            intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)
                        val fileName = filepath!!.substring(filepath.lastIndexOf("\\") + 1)
                        when {
                            paused -> {
                                showSnackBar(
                                    resources.getString(R.string.paused_raw_logging),
                                    5000
                                )
                            }
                            resumed -> {
                                showSnackBar(
                                    resources.getString(R.string.resumed_raw_logging) + " " + fileName,
                                    5000
                                )
                            }
                            running -> {
                                showSnackBar(
                                    resources.getString(R.string.started_raw_logging) + " " + fileName,
                                    5000
                                )
                            }
                            else -> {
                                showSnackBar(
                                    resources.getString(R.string.stopped_raw_logging) + " " + fileName,
                                    5000
                                )
                            }
                        }
                    }
                }
                Constants.NOTIFICATION_BUTTON_CONNECTION -> {
                    toggleConnectToWheel()
                    notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_LOGGING -> {
                    toggleLogging()
                    notifications.update()
                }
                Constants.NOTIFICATION_BUTTON_WATCH -> {
                    toggleWatch()
                    notifications.update()
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
        // if (appConfig.garminConnectIqEnable) toggleGarminConnectIQ() else stopGarminConnectIQ()
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
        updateTrackRecordIcon()
        if (mDeviceAddress.isEmpty()) {
            miWheel!!.isEnabled = false
            miWheel!!.icon!!.alpha = 64
        } else {
            miWheel!!.isEnabled = true
            miWheel!!.icon!!.alpha = 255
        }
        when (appConfig.mibandMode) {
            MiBandEnum.Alarm -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandAlarm))
            MiBandEnum.Min -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMin))
            MiBandEnum.Medium -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMed))
            MiBandEnum.Max -> miBand!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuMiBandMax))
        }

        miBand?.isVisible = appConfig.mainMenuButtons.contains("miband")
        miWatch?.isVisible = appConfig.mainMenuButtons.contains("watch")
        mMenu?.findItem(R.id.miReset)?.isVisible = appConfig.mainMenuButtons.contains("reset")

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
                if (LoggingService.isInstanceCreated()) {
                    miLogging!!.isEnabled = true
                    miLogging!!.icon!!.alpha = 255
                } else {
                    miLogging!!.isEnabled = false
                    miLogging!!.icon!!.alpha = 64
                }
            }
            else -> {}
        }
        if (ThemeManager.palette != null) {
            val menu = mMenu ?: return
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                if (item == miWheel) continue
                val active = (item == miWatch && PebbleService.isInstanceCreated()) ||
                    (item == miLogging && LoggingService.isInstanceCreated())
                item.icon?.mutate()?.setTint(getColorEx(if (active) R.color.accent else R.color.icons))
            }
            val wheelIcon = com.cooper.wheellog.utils.PaletteWheelIcon.create(
                this, ThemeManager.palette!!,
                connected = mConnectionState == ConnectionState.CONNECTED,
                searching = isWheelSearch,
            )
            wheelIcon.alpha = if (mDeviceAddress.isEmpty()) 64 else 255
            miWheel!!.icon = wheelIcon
            (wheelIcon as? AnimationDrawable)?.start()
        }
        updateTrackRecordIcon()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pagerAdapter.updateScreen(true)
    }

    private fun createPager() {
        // add pages into main view
        pager = binding.pager
        pager.offscreenPageLimit = 10
        val pages = DashboardPages.visible(appConfig.dashboardPageOrder, appConfig.dashboardVisiblePages, false).toMutableList()
        pagerAdapter = MainPageAdapter(pages, this)
        pager.adapter = pagerAdapter
        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pagerAdapter.position = position
                pagerAdapter.updateTrackPages()
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
        AppCompatDelegate.setDefaultNightMode(appConfig.dayNightThemeMode)
        appConfig.migrateRecordButtonVisibility()
        setTheme(appConfig.appTheme)
        ThemeManager.theme = appConfig.appTheme
        ThemeManager.changeAppIcon(this)
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycle.coroutineScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                csvImports.state.collect { renderCsvImport(it) }
            }
        }
        lifecycle.coroutineScope.launch {
            LoggingService.status.collect { updateTrackRecordIcon() }
        }
        LoggingService.ensureArmed(this, appConfig)

        if (appConfig.useComposeUI) {
            // на Compose
            setContent {
                AppTheme {
                    MainScreen()
                }
            }
        } else {
            // на xml
            setContentView(binding.root)
        }

        ElectroClub.instance.apply {
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
        mDeviceAddress = appConfig.lastMac
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
        notifications.update()

        binding.settingsView.apply {
            setContent {
                AppTheme(useDarkTheme = true) {
                    settingsNavHostController = rememberNavController()
                    mainScreen(navController = settingsNavHostController!!)
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        savedInstanceState?.getString("settingsRoute")?.takeIf { it != "Start" && it != settingsNavHostController?.currentDestination?.route }?.let {
                            settingsNavHostController!!.navigate(it)
                        }
                    }
                }
            }
        }

        if (savedInstanceState?.getBoolean("settingsVisible") == true) binding.settingsView.visibility = View.VISIBLE
        checkBatteryOptimizationsAndShowAlert(this)
        // for test without wheel go to isHardwarePWM and comment Unknown case
        // DialogHelper.INSTANCE.checkPWMIsSetAndShowAlert(this);
    }

    private fun checkClockVisible() {
        if (appConfig.showClock) {
            binding.textClock.visibility = View.VISIBLE
        } else {
            binding.textClock.visibility = View.GONE
        }
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    public override fun onResume() {
        super.onResume()
        handleTrackIntent()
        LoggingService.ensureArmed(this, appConfig)
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
            notifications.update()
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
        outState.putBoolean("settingsVisible", binding.settingsView.visibility == View.VISIBLE)
        outState.putString("settingsRoute", settingsNavHostController?.currentDestination?.route)
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
        csvImportBar?.dismiss()
        csvImportDialog?.dismiss()
        super.onDestroy()
        eventsLoggingTree?.let { Timber.uproot(it); it.close() }
        eventsLoggingTree = null
        runCatching { unregisterReceiver(mCoreBroadcastReceiver) }
        if (loggingService != null) runCatching { unbindService(mLoggingServiceConnection) }
        if (!isFinishing) return // Theme/configuration recreation must not stop the recorder or Bluetooth.
        LoggingService.command(this, LoggingService.EXIT)
        wearOs?.stop()
        stopPebbleService()
        stopGarminConnectIQ()
        DashboardBluetoothBinding.release(this)
        WheelData.getInstance().full_reset()
        object : CountDownTimer(120_000L, 1_000L) {
            override fun onTick(remaining: Long) {
                if (!LoggingService.isAlive) { cancel(); notifications.close() }
            }
            override fun onFinish() { if (!LoggingService.isAlive) notifications.close() }
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

        setMenuIconStates()
        // Themes
        if (ThemeManager.theme == R.style.AJDMTheme) {
            val miSettings = mMenu!!.findItem(R.id.miSettings)
            miSettings.setIcon(ThemeManager.getId(ThemeIconEnum.MenuSettings))
            miSearch!!.setIcon(ThemeManager.getId(ThemeIconEnum.MenuBluetooth))
        }
        if (ThemeManager.palette != null) {
            binding.toolbar.overflowIcon?.mutate()?.setTint(getColorEx(R.color.icons))
        }
        updateTrackRecordIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Alarms.vibrate(this, longArrayOf(0, 30, 40))
        return when (item.itemId) {
            R.id.miTrackRecord -> {
                showTrackControlMenu()
                true
            }
            R.id.miSearch -> {
                startScanActivity()
                true
            }
            R.id.miWheel -> {
                toggleConnectToWheel()
                true
            }
            R.id.miLogging -> {
                toggleLogging()
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

    private val trackPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            LoggingService.command(this, LoggingService.START)
            showTrackMap()
        } else showSnackBar(R.string.track_permission_error)
    }

    fun requestTrackRecording() {
        if (!appConfig.logLocationData) { showSnackBar(R.string.track_location_disabled); return }
        if (LoggingService.hasPermission(this)) {
            LoggingService.command(this, LoggingService.START)
            showTrackMap()
        } else trackPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun showTrackMap() {
        val index = pagerAdapter.pageIndex(R.layout.main_view_trips)
        if (index >= 0) pager.setCurrentItem(index, true)
    }

    private var trackMenuRequested = false

    private fun showTrackControlMenu() {
        if (mMenu?.findItem(R.id.miTrackRecord) == null) { trackMenuRequested = true; return }
        trackMenuRequested = false
        val anchor = binding.toolbar.findViewById<View>(R.id.miTrackRecord) ?: binding.toolbar
        com.cooper.wheellog.track.TrackRecordingControl.showMenu(this, anchor, LoggingService.status.value, appConfig.logLocationData) { action ->
            if (action == LoggingService.START) requestTrackRecording() else LoggingService.command(this, action)
        }
    }

    private fun updateTrackRecordIcon() {
        val item = mMenu?.findItem(R.id.miTrackRecord) ?: return
        item.isVisible = appConfig.mainMenuButtons.contains("record")
        val state = LoggingService.status.value
        item.setIcon(com.cooper.wheellog.track.TrackRecordingControl.icon(state))
        item.title = getString(R.string.track_title) + " · " + getString(com.cooper.wheellog.track.TrackRecordingControl.title(state))
        item.icon?.mutate()?.setTint(com.cooper.wheellog.track.TrackRecordingControl.color(state))
        if (trackMenuRequested) binding.toolbar.post { if (trackMenuRequested) showTrackControlMenu() }
    }

    private fun handleTrackIntent() {
        if (!intent.getBooleanExtra(LoggingService.CONTROL_EXTRA, false)) return
        intent.removeExtra(LoggingService.CONTROL_EXTRA)
        trackMenuRequested = true
        binding.toolbar.post { if (trackMenuRequested) showTrackControlMenu() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTrackIntent()
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
                        setMenuIconStates()
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
                        setAction(android.R.string.ok) { }
                    }
            }
            snackbar?.apply {
                duration = timeout
                com.cooper.wheellog.map.styleTripMessage(this@MainActivity, this)
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
        LoggingService.command(this, if (LoggingService.isInstanceCreated()) LoggingService.STOP else LoggingService.START)
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
        val buttonMiBand = appConfig.mibandMode.next()
        appConfig.mibandMode = buttonMiBand
        notifications.update()
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

        ) {
            DashboardBluetoothBinding.ensure(this, mDeviceAddress)
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
            if (appConfig.autoUploadEc && appConfig.ecToken != null) {
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

    // Copy selected logs into app-owned Downloads only after duplicate checks.
    val getCsvResults = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty() && !csvImports.start(uris)) showSnackBar(R.string.csv_import_busy)
    }

    private fun renderCsvImport(state: CsvImportState) {
        if (state.running) {
            val progress = state.progress
            val message = if (progress == null || progress.indexing) getString(R.string.csv_import_indexing)
                else getString(R.string.csv_import_progress, progress.done, progress.total)
            val bar = csvImportBar ?: Snackbar.make(binding.mainView,message,Snackbar.LENGTH_INDEFINITE).also { csvImportBar = it }
            com.cooper.wheellog.map.styleTripMessage(this,bar)
            bar.setText(message)
            if (!bar.isShown) bar.show()
            return
        }
        csvImportBar?.dismiss(); csvImportBar = null
        val result = state.result ?: return
        if (csvImportDialog?.isShowing == true || isFinishing) return
        if (::pagerAdapter.isInitialized) pagerAdapter.updatePageOfTrips()
        val message = buildString {
            append(getString(R.string.csv_import_summary,result.added,result.existing,result.errors.size))
            if (result.uncheckedExisting > 0) append("\n\n" + getString(R.string.csv_import_unchecked,result.uncheckedExisting))
            if (result.errors.isNotEmpty()) append("\n\n" + result.errors.take(8).joinToString("\n") { it.take(120) })
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.csv_import_title)
            .setMessage(message).setPositiveButton(android.R.string.ok) { _,_ -> csvImports.acknowledge() }
            .setOnCancelListener { csvImports.acknowledge() }.create()
        csvImportDialog = dialog
        showTripBrowserDialog(this,dialog)
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
        intentFilter.addAction(Constants.ACTION_RAW_LOGGING_TOGGLED)
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
        intentFilter.addAction(Constants.ACTION_TRACK_CHANGED)
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        intentFilter.addAction(Constants.ACTION_RAW_LOGGING_TOGGLED)
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
