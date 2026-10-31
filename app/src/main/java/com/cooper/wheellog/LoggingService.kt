package com.cooper.wheellog

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.cooper.wheellog.data.NativeCsv
import com.cooper.wheellog.data.NativeWheelSample
import com.cooper.wheellog.track.*
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.FileUtil
import com.welie.blessed.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The original WheelLog CSV logger is the only recorder, including during Bluetooth loss. */
class LoggingService : Service(), LocationListener {
    private val config: AppConfig by inject()
    private val preferences by lazy { getSharedPreferences(SESSION, MODE_PRIVATE) }
    private val dayPreferences by lazy { getSharedPreferences("native_logging_day", MODE_PRIVATE) }
    private val manager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private lateinit var thread: HandlerThread
    private lateinit var worker: Handler
    private var fileUtil: FileUtil? = null
    private var pauseState = TrackPauseState()
    private var filter = TrackPointFilter()
    private val gpsMotion = TrackMotionDetector()
    private var gps: TrackPoint? = null
    private var wheel: NativeWheelSample? = null
    private var wheelAt = 0L
    private var connected = false
    private var disconnectedAt = 0L
    private var elapsedBase = 0L
    private var elapsedSince: Long? = null
    private var lastRowElapsed: Long? = null
    private var csvColumns = NativeCsv.HEADER.split(',')
    private var subscribed = false
    private var locationEnabled = false
    private var segment = true
    private var csvTime = 0L
    private var lastChanged = 0L
    @Volatile private var destroying = false
    private val binder = LocalBinder()
    private val settings by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val changed = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == getString(R.string.log_location_data) || key == getString(R.string.use_gps)) enqueue {
            if (locationEnabled && !config.logLocationData) { finishRecording(); stopSelf() }
            else { unsubscribe(); gps = null; gpsMotion.reset(); segment = true; filter.startSegment(); subscribe() }
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.ACTION_BLUETOOTH_CONNECTION_STATE) {
                val next = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, 0) == ConnectionState.CONNECTED.value
                val receivedAt = System.currentTimeMillis()
                enqueue { connected = next; if (!next) { wheel = null; wheelAt = 0; disconnectedAt = receivedAt } }
            }
        }
    }

    private fun captureWheelData(data: WheelData) {
        val packetAt = data.lastLifeData
        val receivedAt = SystemClock.elapsedRealtime()
        val sample = NativeWheelSample.capture(data, data.distance, data.alert ?: "")
        enqueue {
            if (fileUtil == null) return@enqueue
            // Only the actual decoder invokes this path. Do not gate real packets on
            // a cached BluetoothService state or delivery of an implicit UI broadcast.
            if (packetAt > 0 && (packetAt <= disconnectedAt || System.currentTimeMillis() - packetAt !in -1_000..5_000)) return@enqueue
            val odometer = sample.columns[9].toLongOrNull()
            val loggedSample = if (odometer == null) sample else sample.copy(columns = sample.columns.toMutableList().apply {
                this[8] = data.getDistanceAtOdometer(odometer).toString()
            })
            if (odometer != null && odometer > 0 && preferences.getLong("distance_start", -1) < 0) {
                check(preferences.edit().putLong("distance_start", odometer - data.getDistanceAtOdometer(odometer)).commit())
            }
            connected = true
            wheel = loggedSample; wheelAt = receivedAt
            motion(loggedSample.telemetry.speedKmh > .1)
            if (pauseState.pause == TrackPause.NONE) writeRow(if (loggedSample.telemetry.speedKmh > .1) freshGps() else null, loggedSample)
        }
    }

    inner class LocalBinder : Binder() { fun getService() = this@LoggingService }
    override fun onBind(intent: Intent): IBinder = binder
    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread("WheelLogCsv").apply { start() }
        worker = Handler(thread.looper)
        instance = this
        ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
            addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        settings.registerOnSharedPreferenceChangeListener(changed)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == STOP || action == EXIT) {
            enqueue { automaticSuppressed = true; finishRecording(continueDay = action == EXIT); stopSelf() }
            return START_NOT_STICKY
        }
        if (config.logLocationData && !hasPermission(this)) {
            mutableStatus.value = TrackStatus(error = getString(R.string.track_permission_error))
            stopSelf(); return START_NOT_STICKY
        }
        try { foreground() } catch (error: Exception) {
            Timber.e(error, "Cannot start native CSV logger")
            mutableStatus.value = TrackStatus(error = getString(R.string.track_permission_error))
            stopSelf(); return START_NOT_STICKY
        }
        enqueue {
            if (fileUtil == null) startRecording()
            when (action) { PAUSE -> setManualPause(); RESUME -> resumeRecording() }
        }
        return START_STICKY
    }
    private fun foreground() {
        val channel = Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel, Constants.notificationChannelName, NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, channel).setSmallIcon(R.drawable.ic_stat_wheel)
            .setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.track_recording)).setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 7110, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()
        val type = if (config.logLocationData) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        ServiceCompat.startForeground(this, Constants.MAIN_NOTIFICATION_ID, notification, if (Build.VERSION.SDK_INT >= 29) type else 0)
    }
    private fun enqueue(action: () -> Unit) { if (!destroying) worker.post { try { action() } catch (error: Exception) { fail(error) } } }
    private fun fail(error: Exception) {
        Timber.e(error, "Cannot persist native CSV")
        unsubscribe(); fileUtil?.close(); fileUtil = null
        mutableStatus.value = TrackStatus(error = getString(R.string.track_save_error))
        broadcast(); stopSelf()
    }
    private fun startRecording() {
        if (fileUtil != null) return
        val file = FileUtil(this)
        val pendingName = preferences.getString("file", null)
        val forceNewDistance = preferences.getBoolean("new_distance", false)
        val data = WheelData.getInstance()
        fun knownMac(value: String?) = value?.takeIf { it.isNotBlank() && it != "default" && it != "GPS" }
        val mac = knownMac(data?.mac) ?: knownMac(config.lastMac) ?: "GPS"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val continuing = pendingName == null && config.continueThisDayLog &&
            dayPreferences.getString("day", null) == today && dayPreferences.getString("mac", null) == mac
        val sourcePreferences = if (continuing) dayPreferences else preferences
        val recordedMac = sourcePreferences.getString("mac", null)
        val savedDistanceStart = sourcePreferences.getLong("distance_start", -1).takeIf { it >= 0 }
        val recoveredName = sourcePreferences.getString("file", null)
        val savedElapsed = sourcePreferences.getLong("elapsed", 0L)
        val recovered = recoveredName != null && sourcePreferences.getString("uri", null)?.let { source ->
            // Never recreate a deleted file while attempting to continue a day.
            val uri = android.net.Uri.parse(source)
            val exists = runCatching { contentResolver.openInputStream(uri)?.use { it.read() >= 0 } == true }.getOrDefault(false)
            exists && file.openExisting(uri, sourcePreferences.getString("path", null), recoveredName)
        } == true
        if (pendingName != null && !recovered) error("Cannot reopen the unfinished CSV; its identity is preserved")
        locationEnabled = config.logLocationData
        filter = TrackPointFilter(); gpsMotion.reset(); gps = null; wheel = null; segment = true
        val started = if (recovered) sourcePreferences.getLong("start", System.currentTimeMillis()) else System.currentTimeMillis()
        if (recovered) {
            file.repairCsvTail()
            pauseState = TrackPauseState(if (continuing) TrackPause.NONE else runCatching {
                TrackPause.valueOf(preferences.getString("pause", "NONE")!!)
            }.getOrDefault(TrackPause.NONE))
        } else {
            val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date(started)) + ".csv"
            check(file.prepareFile(name, WheelData.getInstance()?.mac?.takeIf { it.isNotBlank() } ?: "GPS"))
            file.appendLineChecked(NativeCsv.HEADER)
            pauseState = TrackPauseState()
        }
        check(preferences.edit().remove("new_distance").putString("file", file.fileName).putString("uri", file.contentUri.toString())
            .putString("path", file.absolutePath).putLong("start", started).putString("pause", pauseState.pause.name)
            .putLong("elapsed", if (recovered) savedElapsed else 0L).putString("mac", mac).commit())
        fileUtil = file
        if (recovered) runCatching {
            org.koin.core.context.GlobalContext.get().getOrNull<com.cooper.wheellog.data.TripDao>()?.let { dao ->
                dao.getTripByFileName(file.fileName)?.let { trip -> trip.duration = 0; dao.update(trip) }
            }
        }.onFailure { Timber.w(it, "Trip preview cache will be refreshed later") }
        val ticks = if (recovered) file.inputStream?.let(NativeCsv::read) ?: emptyList() else emptyList()
        val odometerStart = if (recovered && recordedMac == mac) savedDistanceStart
            ?: ticks.firstOrNull { it.wheelDataAvailable && it.totalDistance > 0 }?.totalDistance?.toLong() else null
        val currentOdometer = data?.totalDistance?.takeIf { it > 0 }
        val sessionStart = currentOdometer?.let { current ->
            (current - data.distanceFromStart.toLong()).takeIf { it > 0 } ?: current
        }
        val distanceStart = odometerStart ?: if (recovered || forceNewDistance) currentOdometer else sessionStart
        data?.setRecordingDistanceStart(distanceStart)
        check(preferences.edit().putLong("distance_start", distanceStart ?: -1L).commit())
        csvColumns = if (recovered) file.inputStream?.bufferedReader()?.use { it.readLine()?.trimStart('\uFEFF')?.split(',') }
            ?: NativeCsv.HEADER.split(',') else NativeCsv.HEADER.split(',')
        val points = NativeCsv.points(ticks)
        csvTime = ticks.lastOrNull()?.timestampMillis ?: 0
        elapsedBase = maxOf(if (recovered) savedElapsed else 0L, ticks.lastOrNull()?.recordingElapsedMillis
            ?: ticks.takeIf { it.isNotEmpty() }?.let { (it.last().timestampMillis - it.first().timestampMillis).coerceAtLeast(0) } ?: 0L)
        elapsedSince = null
        updateRecordingClock()
        lastRowElapsed = ticks.lastOrNull()?.recordingElapsedMillis ?: elapsedBase.takeIf { ticks.isNotEmpty() }
        mutableStatus.value = TrackStatus(recording = true, id = file.fileName, points = points.size, distance = summarizeTrack(points).distanceMeters,
            startedAt = started, paused = pauseState.pause != TrackPause.NONE, pauseReason = pauseState.pause)
        subscribe(); broadcast(logging = true)
    }
    private fun freshWheel(): NativeWheelSample? = wheel.takeIf { connected && SystemClock.elapsedRealtime() - wheelAt <= 5_000 }
    private fun freshGps(): TrackPoint? = gps?.takeIf { locationEnabled && System.currentTimeMillis() - it.time in -5_000..5_000 }
    private fun recordingElapsed(now: Long = SystemClock.elapsedRealtime()): Long =
        elapsedBase + (elapsedSince?.let { (now - it).coerceAtLeast(0) } ?: 0L)
    private fun updateRecordingClock() {
        val now = SystemClock.elapsedRealtime()
        if (pauseState.pause == TrackPause.NONE) {
            if (elapsedSince == null) elapsedSince = now
        } else {
            elapsedBase = recordingElapsed(now)
            elapsedSince = null
        }
    }
    private fun motion(moving: Boolean, wheelEvidence: Boolean = true) {
        // After a long stop, carrying the phone on foot/in a car must not restart the wheel's route.
        if (pauseState.pause == TrackPause.AUTOMATIC && moving && !wheelEvidence) return
        if (!moving) { gps = null; segment = true; filter.startSegment() }
        if (pauseState.motion(moving, SystemClock.elapsedRealtime())) {
            if (pauseState.pause == TrackPause.NONE) { segment = true; filter.startSegment(); gps = null }
            savePause(); broadcast()
        }
    }
    private fun savePause() {
        updateRecordingClock()
        check(preferences.edit().putString("pause", pauseState.pause.name).putLong("elapsed", recordingElapsed()).commit())
        mutableStatus.value = mutableStatus.value.copy(paused = pauseState.pause != TrackPause.NONE, pauseReason = pauseState.pause)
    }
    private fun setManualPause() { if (fileUtil != null) { pauseState.manualPause(); savePause(); unsubscribe(); gps = null; broadcast() } }
    private fun resumeRecording() {
        if (fileUtil == null) return
        pauseState.resume(); savePause(); segment = true; filter.startSegment(); gpsMotion.reset(); gps = null; subscribe(); broadcast()
    }
    private fun subscribe() {
        if (subscribed || !locationEnabled || fileUtil == null || pauseState.pause == TrackPause.MANUAL || !hasPermission(this)) return
        val gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val provider = if (gpsEnabled && (config.useGps || !networkEnabled)) LocationManager.GPS_PROVIDER
            else if (networkEnabled) LocationManager.NETWORK_PROVIDER else return
        manager.requestLocationUpdates(provider, 1_000L, 0f, this, thread.looper)
        subscribed = true
    }
    private fun unsubscribe() { if (subscribed) manager.removeUpdates(this); subscribed = false }
    override fun onLocationChanged(location: Location) { enqueue { acceptFix(Location(location)) } }
    private fun acceptFix(location: Location) {
        if (fileUtil == null || !locationEnabled || pauseState.pause == TrackPause.MANUAL || !location.hasAccuracy()) return
        if (!config.logLocationData) { finishRecording(); stopSelf(); return }
        val sample = freshWheel()
        val speed = if (location.hasSpeed()) location.speed else gps?.let { previous ->
            val seconds = (location.time - previous.time) / 1_000.0
            val meters = FloatArray(1)
            Location.distanceBetween(previous.latitude, previous.longitude, location.latitude, location.longitude, meters)
            if (seconds in 5.0..30.0 && meters[0] > location.accuracy + previous.accuracy) (meters[0] / seconds).toFloat() else 0f
        } ?: 0f
        var candidate = TrackPoint(location.time, location.latitude, location.longitude,
            if (location.hasAltitude()) location.altitude else null, location.accuracy, speed, telemetry = sample?.telemetry)
        val now = System.currentTimeMillis()
        val valid = location.accuracy.isFinite() && location.accuracy in 0f..25f && speed.isFinite() && speed in 0f..70f &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 && location.time in (now - 15_000)..(now + 5_000)
        if (!valid) return
        val evidence = gpsMotion.observe(candidate, location.hasSpeed(), SystemClock.elapsedRealtime())
        if (!location.hasSpeed() && evidence == TrackMotion.MOVING) candidate = candidate.copy(speedMps = gpsMotion.estimatedSpeedMps)
        val moving = if (sample != null) sample.telemetry.speedKmh > .1 else when (evidence) {
            TrackMotion.MOVING -> true
            TrackMotion.STATIONARY -> false
            TrackMotion.UNKNOWN -> return
        }
        motion(moving, wheelEvidence = sample != null)
        if (pauseState.pause != TrackPause.NONE) return
        // Quiet/unconfirmed GPS fixes never enter the CSV, including the five-minute grace period.
        if (!moving) return
        val previous = gps
        val accepted = filter.accept(candidate, now) ?: return
        gps = accepted
        if (accepted.segmentStart) segment = true
        val added = if (accepted.segmentStart || previous == null) 0.0 else previous.distanceTo(accepted)
        val state = mutableStatus.value
        mutableStatus.value = state.copy(points = state.points + 1, distance = state.distance + added, lastFixAt = now)
        writeRow(accepted, sample)
    }
    private fun writeRow(point: TrackPoint?, sample: NativeWheelSample?) {
        val file = fileUtil ?: return
        if (pauseState.pause != TrackPause.NONE) return
        csvTime = maxOf(System.currentTimeMillis(), csvTime + 1)
        val elapsed = recordingElapsed()
        file.appendLineChecked(NativeCsv.alignRow(NativeCsv.row(csvTime, point, sample, mutableStatus.value.distance,
            segment && point != null, elapsed), csvColumns))
        lastRowElapsed = elapsed
        if (point != null) segment = false
        if (SystemClock.elapsedRealtime() - lastChanged > 15_000) broadcast()
    }
    private fun finishRecording(continueDay: Boolean = false) {
        unsubscribe()
        val file = fileUtil
        // Timing only: no invented coordinates, speed or wheel readings. This retains
        // short final stops, while manual/automatic pauses remain excluded by the clock.
        val elapsed = recordingElapsed()
        if (file != null && lastRowElapsed?.let { elapsed - it >= 1_000 } == true)
            file.appendLineChecked(NativeCsv.alignRow(NativeCsv.row(maxOf(System.currentTimeMillis(), csvTime + 1),
                null, null, mutableStatus.value.distance, false, elapsed), csvColumns))
        fileUtil = null
        file?.close()
        if (continueDay && file != null) {
            check(dayPreferences.edit().clear().putString("file", file.fileName).putString("uri", file.contentUri.toString())
                .putString("path", file.absolutePath).putLong("start", mutableStatus.value.startedAt)
                .putLong("elapsed", elapsed)
                .putLong("distance_start", preferences.getLong("distance_start", -1))
                .putString("mac", preferences.getString("mac", "GPS"))
                .putString("day", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())).commit())
        } else if (!continueDay) check(dayPreferences.edit().clear().commit())
        check(preferences.edit().clear().apply { if (!continueDay) putBoolean("new_distance", true) }.commit())
        mutableStatus.value = TrackStatus()
        broadcast(logging = false, path = file?.absolutePath)
        if (file != null && config.autoUploadEc && config.ecToken != null) {
            Thread {
                runCatching { file.readBytes()?.let { ElectroClub.instance.uploadTrack(it, file.fileName, true) { _ -> } } }
                    .onFailure { Timber.e(it, "CSV upload failed") }
            }.apply { name = "WheelLogCsvUpload"; start() }
        }
    }
    private fun broadcast(logging: Boolean? = null, path: String? = fileUtil?.absolutePath) {
        lastChanged = SystemClock.elapsedRealtime()
        sendBroadcast(Intent(Constants.ACTION_TRACK_CHANGED).setPackage(packageName))
        if (logging != null) sendBroadcast(Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED).setPackage(packageName)
            .putExtra(Constants.INTENT_EXTRA_IS_RUNNING, logging).putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, path))
    }
    override fun onProviderDisabled(provider: String) { enqueue { unsubscribe(); filter.startSegment(); gpsMotion.reset(); segment = true; gps = null } }
    override fun onProviderEnabled(provider: String) { enqueue { subscribe() } }
    @Deprecated("Legacy location callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onDestroy() {
        destroying = true
        settings.unregisterOnSharedPreferenceChangeListener(changed)
        runCatching { unregisterReceiver(receiver) }
        val closed = CountDownLatch(1)
        worker.post { try { unsubscribe(); fileUtil?.close(); fileUtil = null } finally { closed.countDown() } }
        closed.await(3, TimeUnit.SECONDS); thread.quitSafely()
        instance = null; mutableStatus.value = TrackStatus()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

    companion object {
        const val START = "csv.start"
        const val PAUSE = "csv.pause"
        const val RESUME = "csv.resume"
        const val STOP = "csv.stop"
        const val EXIT = "csv.exit"
        const val SESSION = "native_logging_session"
        const val CONTROL_EXTRA = "csv.show_controls"
        @Volatile private var instance: LoggingService? = null
        @Volatile private var automaticSuppressed = false
        private var wasConnected = false
        private val mutableStatus = MutableStateFlow(TrackStatus())
        val status = mutableStatus.asStateFlow()
        val isAlive get() = instance != null
        val currentLogFileName get() = status.value.id
        fun isInstanceCreated() = status.value.recording
        @JvmStatic fun onWheelDataDecoded(data: WheelData) { instance?.captureWheelData(data) }
        fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        fun disableLocation() { instance?.let { if (it.locationEnabled) command(it, EXIT) } }
        fun maybeAutoStart(context: Context, config: AppConfig) {
            val data = WheelData.getInstance() ?: return
            val connected = data.bluetoothService?.connectionState?.let { it == ConnectionState.CONNECTED } ?: data.isConnected
            noteConnection(connected)
            if (TrackStartPolicy.shouldStart(!config.logLocationData || hasPermission(context), config.autoLog,
                connected, data.speedDouble, config.startAutoLoggingWhenIsMovingMore, isAlive, automaticSuppressed)) command(context, START)
        }
        fun noteConnection(connected: Boolean) {
            if (connected && !wasConnected) automaticSuppressed = false
            wasConnected = connected
        }
        fun ensureArmed(context: Context, config: AppConfig) {
            if (!isAlive && context.getSharedPreferences(SESSION, MODE_PRIVATE).contains("file") &&
                (!config.logLocationData || hasPermission(context))) command(context, RESUME_PENDING)
        }
        private const val RESUME_PENDING = "csv.restore"
        fun command(context: Context, action: String) {
            instance?.let { it.onStartCommand(Intent(context, LoggingService::class.java).setAction(action), 0, 0); return }
            if (action == STOP || action == EXIT) {
                automaticSuppressed = true
                // No live writer: only an explicit Stop invalidates the last completed day's file.
                if (action == STOP) context.getSharedPreferences("native_logging_day", MODE_PRIVATE).edit().clear().commit()
                context.getSharedPreferences(SESSION, MODE_PRIVATE).edit().clear().apply {
                    if (action == STOP) putBoolean("new_distance", true)
                }.commit()
                mutableStatus.value = TrackStatus()
                return
            }
            if (action == PAUSE || action == RESUME) return
            runCatching { ContextCompat.startForegroundService(context, Intent(context, LoggingService::class.java).setAction(action)) }
                .onFailure { mutableStatus.value = TrackStatus(error = context.getString(R.string.track_permission_error)); Timber.e(it) }
        }
    }
}
