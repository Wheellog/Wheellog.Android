package com.cooper.wheellog.preferences

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.preference.*
import com.cooper.wheellog.*
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.R
import com.cooper.wheellog.databinding.ActivityLoginBinding
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.cooper.wheellog.presentation.preferences.MultiSelectPreferenceDialogFragment.Companion.newInstance
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.SomeUtil.Companion.getDrawableEx
import com.cooper.wheellog.utils.ThemeIconEnum
import com.yandex.metrica.YandexMetrica
import timber.log.Timber

class PreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var mDataWarningDisplayed = false
    private var currentScreen = SettingsScreen.Main
    private val dialogTag = "wheellog.MainPreferenceFragment.DIALOG"
    private lateinit var speedSettings: SpeedSettings
    private lateinit var wheelSettings: WheelSettings
    private lateinit var generalSettings: GeneralSettings
    private lateinit var watchSettings: WatchSettings

    private val writeStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            // Access is denied
            WheelLog.AppConfig.autoLog = false
            WheelLog.AppConfig.enableRawData = false
            refreshVolatileSettings()
        }
    }

    private val readStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            // Access is denied
            WheelLog.AppConfig.useCustomBeep = false
            refreshVolatileSettings()
        }
    }

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        if (!map.all { it.value == true }) {
            // Any access is denied
            WheelLog.AppConfig.useGps = false
            refreshVolatileSettings()
        }
    }

    val mediaRequestResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pref = findPreference<SwitchPreference>(getString(R.string.custom_beep))
        var isDefault = true
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            isDefault = false
            pref?.summary = uri.path
            WheelLog.AppConfig.beepFile = uri
        }
        if (isDefault) {
            pref?.summary = "default"
            WheelLog.AppConfig.beepFile = Uri.EMPTY
            WheelLog.AppConfig.useCustomBeep = false
            pref?.isChecked = false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        speedSettings = SpeedSettings(requireContext(), preferenceScreen)
        wheelSettings = WheelSettings(requireContext(), preferenceScreen)
        generalSettings = GeneralSettings(requireContext(), preferenceScreen)
        watchSettings = WatchSettings(requireContext(), preferenceScreen)
        changeWheelType()
        checkAndRequestPermissions()
    }

    private fun changeWheelType() {
        switchSpecificSettingsIsVisible()
        generalSettings.switchAlarmsIsVisible(this)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is MultiSelectPreference) {
            if (parentFragmentManager.findFragmentByTag(dialogTag) != null) {
                return
            }
            val dialogFragment = newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, dialogTag)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            && (WheelLog.AppConfig.autoLog || WheelLog.AppConfig.enableRawData)) {
            writeStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && WheelLog.AppConfig.useCustomBeep) {
            readStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (WheelLog.AppConfig.useGps) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                    return
                }
            } else {
                if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
                        && checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
                    return
                }
            }
            AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.log_location_title))
                    .setMessage(getString(R.string.log_location_pop_up))
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        } else {
                            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                        WheelLog.AppConfig.useGps = false
                        refreshVolatileSettings()
                    }
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_baseline_gps_24)
                    .show()
                    .setBlackIcon()
        }
    }

    private fun electroClubDialog() {
        // TODO check user token
        if (!WheelLog.AppConfig.autoUploadEc) {
            // logout after uncheck
            ElectroClub.instance.logout()
            return
        }

        if (WheelLog.AppConfig.ecUserId != null && WheelLog.AppConfig.ecToken != null) {
            // Success login - No need to show dialog
            return
        }

        fun shakeError(): TranslateAnimation {
            val shake = TranslateAnimation(0F, 15F, 0F, 10F)
            shake.duration = 500
            shake.interpolator = CycleInterpolator(7F)
            return shake
        }

        fun showLoginDialog() {
            val binding = ActivityLoginBinding.inflate(LayoutInflater.from(context), null, false)
            val loginDialog = AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setTitle("electro.club")
                    .setView(binding.root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
            loginDialog.setOnShowListener {
                loginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val email = binding.editTextTextEmailAddress.editText?.text.toString()
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    {
                        binding.editTextTextEmailAddress.error = "Invalid email.";
                        return@setOnClickListener
                    }
                    binding.editTextTextEmailAddress.error = null
                    // click OK -> send request
                    ElectroClub.instance.login(
                        email,
                        binding.editTextTextPassword.editText?.text.toString()
                    ) {
                        activity?.runOnUiThread {
                            if (it) { // response - success login
                                loginDialog.dismiss()
                                ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                                    WheelData.getInstance().mac,
                                    activity as Activity
                                ) { }
                                WheelLog.AppConfig.autoUploadEc = true
                                refreshVolatileSettings()
                            } else { // response - failed
                                ElectroClub.instance.logout()
                                binding.editTextTextPassword.apply {
                                    startAnimation(shakeError())
                                    error = ElectroClub.instance.lastError
                                    errorIconDrawable = null
                                }

                            }
                        }
                    }
                }
                loginDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    loginDialog.dismiss()
                    ElectroClub.instance.logout()
                    refreshVolatileSettings()
                }
            }
            loginDialog.show()
        }

        WheelLog.AppConfig.autoUploadEc = false
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.enable_auto_upload_title))
            .setMessage(getString(R.string.enable_auto_upload_descriprion))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                mDataWarningDisplayed = true

                if (WheelLog.AppConfig.ecToken != null) {
                    ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                        WheelData.getInstance().mac,
                        activity as Activity
                    ) { }
                } else {
                    showLoginDialog()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                ElectroClub.instance.logout()
                refreshVolatileSettings()
            }
            .setCancelable(false)
            .show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (context == null || key == null) {
            return
        }

        val wd = WheelData.getInstance()
        val resName = key.replace(WheelData.getInstance().mac + "_", "")
        when (WheelLog.AppConfig.getResId(resName)) {
            R.string.auto_log, R.string.use_raw_data, R.string.log_location_data, R.string.use_gps -> checkAndRequestPermissions()
            R.string.connection_sound -> switchConnectionSoundIsVisible()
            R.string.alarms_enabled -> {
                generalSettings.switchAlarmsIsVisible(this)
            }
            R.string.altered_alarms -> generalSettings.switchAlarmsIsVisible(this)
            R.string.auto_upload_ec -> electroClubDialog()
            R.string.max_speed, R.string.use_mph -> context?.sendBroadcast(Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED))
            R.string.use_eng, R.string.app_theme -> {
                WheelLog.ThemeManager.theme = WheelLog.AppConfig.appTheme
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.use_eng_alert_title)
                    .setMessage(R.string.use_eng_alert_description)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()
                    .setBlackIcon()
            }
            R.string.day_night_theme -> {
                findPreference<ListPreference>(getString(R.string.day_night_theme))?.summary =
                        when (WheelLog.AppConfig.dayNightThemeMode) {
                            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.day_night_theme_day)
                            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.day_night_theme_night)
                            else -> getString(R.string.day_night_theme_as_system)
                        }
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.use_eng_alert_title)
                    .setMessage(R.string.use_eng_alert_description)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()
                    .setBlackIcon()
            }
            R.string.light_enabled -> wd.updateLight(WheelLog.AppConfig.lightEnabled)
            R.string.led_enabled -> wd.updateLed(WheelLog.AppConfig.ledEnabled)
            R.string.taillight_enabled -> wd.updateTailLight(WheelLog.AppConfig.taillightEnabled)
            R.string.handle_button_disabled -> wd.updateHandleButton(WheelLog.AppConfig.handleButtonDisabled)
            R.string.brake_assistant_enabled -> wd.updateBrakeAssistant(WheelLog.AppConfig.brakeAssistantEnabled)
            R.string.wheel_max_speed -> wd.updateMaxSpeed(WheelLog.AppConfig.wheelMaxSpeed)
            R.string.speaker_volume -> wd.updateSpeakerVolume(WheelLog.AppConfig.speakerVolume)
            R.string.beeper_volume -> wd.adapter?.updateBeeperVolume(WheelLog.AppConfig.beeperVolume)
            R.string.pedals_adjustment -> wd.updatePedals(WheelLog.AppConfig.pedalsAdjustment)
            R.string.pedal_sensivity -> wd.updatePedalSensivity(WheelLog.AppConfig.pedalSensivity)
            R.string.ride_mode -> wd.updateRideMode(WheelLog.AppConfig.rideMode)
            R.string.lock_mode -> wd.updateLockMode(WheelLog.AppConfig.lockMode)
            R.string.transport_mode -> wd.updateTransportMode(WheelLog.AppConfig.transportMode)
            R.string.drl_enabled -> wd.updateDrl(WheelLog.AppConfig.drlEnabled)
            R.string.go_home_mode -> wd.updateGoHome(WheelLog.AppConfig.goHomeMode)
            R.string.fancier_mode -> wd.updateFancierMode(WheelLog.AppConfig.fancierMode)
            R.string.speaker_mute -> wd.updateMute(WheelLog.AppConfig.speakerMute)
            R.string.fan_quiet_enable -> wd.updateFanQuiet(WheelLog.AppConfig.fanQuietEnabled)
            R.string.fan_enabled -> wd.updateFanState(WheelLog.AppConfig.fanEnabled)
            R.string.light_brightness -> wd.updateLightBrightness(WheelLog.AppConfig.lightBrightness)
            R.string.pedals_mode -> wd.updatePedalsMode(Integer.parseInt(WheelLog.AppConfig.pedalsMode))
            R.string.light_mode -> wd.adapter?.setLightMode(Integer.parseInt(WheelLog.AppConfig.lightMode))
            R.string.roll_angle -> wd.adapter?.setRollAngleMode(Integer.parseInt(WheelLog.AppConfig.rollAngle))
            R.string.gw_in_miles -> wd.adapter?.setMilesMode(WheelLog.AppConfig.gwInMiles)
            R.string.alarm_mode -> wd.updateAlarmMode(Integer.parseInt(WheelLog.AppConfig.alarmMode))
            R.string.strobe_mode -> wd.updateStrobe(Integer.parseInt(WheelLog.AppConfig.strobeMode))
            R.string.led_mode -> {
                wd.updateLedMode(Integer.parseInt(WheelLog.AppConfig.ledMode))
                if (wd.wheelType == WHEEL_TYPE.NINEBOT_Z) { // to avoid issues with KS
                    findPreference<Preference>(wd.mac + "_" + getString(R.string.led_mode))?.summary = wd.adapter?.ledModeString
                    findPreference<Preference>(wd.mac + "_" + getString(R.string.nb_led_color1))?.isVisible = wd.adapter.getLedIsAvailable(1)
                    findPreference<Preference>(wd.mac + "_" + getString(R.string.nb_led_color2))?.isVisible = wd.adapter.getLedIsAvailable(2)
                    findPreference<Preference>(wd.mac + "_" + getString(R.string.nb_led_color3))?.isVisible = wd.adapter.getLedIsAvailable(3)
                    findPreference<Preference>(wd.mac + "_" + getString(R.string.nb_led_color4))?.isVisible = wd.adapter.getLedIsAvailable(4)
                }
            }
            R.string.nb_led_color1 -> wd.setLedColor(WheelLog.AppConfig.ledColor1, 1)
            R.string.nb_led_color2 -> wd.setLedColor(WheelLog.AppConfig.ledColor2, 2)
            R.string.nb_led_color3 -> wd.setLedColor(WheelLog.AppConfig.ledColor3, 3)
            R.string.nb_led_color4 -> wd.setLedColor(WheelLog.AppConfig.ledColor4, 4)
            R.string.wheel_ks_alarm3 -> KingsongAdapter.getInstance().updateKSAlarm3(WheelLog.AppConfig.wheelKsAlarm3)
            R.string.wheel_ks_alarm2 -> KingsongAdapter.getInstance().updateKSAlarm2(WheelLog.AppConfig.wheelKsAlarm2)
            R.string.wheel_ks_alarm1 -> KingsongAdapter.getInstance().updateKSAlarm1(WheelLog.AppConfig.wheelKsAlarm1)
            R.string.wheel_alarm1_enabled -> {
                wd.updateAlarmEnabled(WheelLog.AppConfig.wheelAlarm1Enabled, 1)
                findPreference<Preference>(wd.mac + "_" + getString(R.string.wheel_alarm1))?.isVisible = WheelLog.AppConfig.wheelAlarm1Enabled
            }
            R.string.wheel_alarm2_enabled -> {
                wd.updateAlarmEnabled(WheelLog.AppConfig.wheelAlarm2Enabled, 2)
                findPreference<Preference>(wd.mac + "_" + getString(R.string.wheel_alarm2))?.isVisible = WheelLog.AppConfig.wheelAlarm2Enabled
            }
            R.string.wheel_alarm3_enabled -> {
                wd.updateAlarmEnabled(WheelLog.AppConfig.wheelAlarm3Enabled, 3)
                findPreference<Preference>(wd.mac + "_" + getString(R.string.wheel_alarm3))?.isVisible = WheelLog.AppConfig.wheelAlarm3Enabled
            }
            R.string.wheel_alarm1 -> wd.updateAlarmSpeed(WheelLog.AppConfig.wheelAlarm1Speed, 1)
            R.string.wheel_alarm2 -> wd.updateAlarmSpeed(WheelLog.AppConfig.wheelAlarm2Speed, 2)
            R.string.wheel_alarm3 -> wd.updateAlarmSpeed(WheelLog.AppConfig.wheelAlarm3Speed, 3)
            R.string.wheel_limited_mode_enabled -> {
                wd.updateLimitedModeEnabled(WheelLog.AppConfig.wheelLimitedModeEnabled)
                findPreference<Preference>(wd.mac + "_" + getString(R.string.wheel_limited_speed))?.isVisible = WheelLog.AppConfig.wheelLimitedModeEnabled
            }
            R.string.wheel_limited_speed -> wd.updateLimitedSpeed(WheelLog.AppConfig.wheelLimitedModeSpeed)
            R.string.ks18l_scaler -> KingsongAdapter.getInstance().set18Lkm(WheelLog.AppConfig.ks18LScaler)
            R.string.current_on_dial -> Timber.i("Change dial type to %b", WheelLog.AppConfig.currentOnDial)
            R.string.custom_beep -> {
                checkAndRequestPermissions()
                speedSettings.selectCustomBeep(this)
            }
            R.string.notification_buttons -> WheelLog.Notifications.update()
            R.string.beep_on_volume_up -> WheelLog.VolumeKeyController.setActive(wd.isConnected && WheelLog.AppConfig.useBeepOnVolumeUp)
            R.string.use_reconnect -> {
                if (WheelLog.AppConfig.useReconnect)
                    wd.bluetoothService?.startReconnectTimer()
                else
                    wd.bluetoothService?.stopReconnectTimer()
            }
            R.string.alarm_factor2 -> {
                if (WheelLog.AppConfig.alarmFactor2 <= WheelLog.AppConfig.alarmFactor1) {
                    WheelLog.AppConfig.alarmFactor2 = MathsUtil.clamp(WheelLog.AppConfig.alarmFactor1 + 10, 1,100)
                }
            }
            R.string.miband_fixrs_enable -> WheelLog.Notifications.updateKostilTimer()
            R.string.yandex_metriсa_accepted -> YandexMetrica.setStatisticsSending(
                requireContext().applicationContext,
                WheelLog.AppConfig.yandexMetricaAccepted
            )
        }
        correctState(key)
    }

    private fun setupScreen() {
        if (activity == null) {
            return
        }

        val tb: Toolbar = requireActivity().findViewById(R.id.preference_toolbar)

        tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        tb.setNavigationOnClickListener {
            if (currentScreen != SettingsScreen.Main) {
                showMainMenu()
            } else {
                activity?.finish()
            }
        }
        when (currentScreen) {
            SettingsScreen.Main -> {
                tb.title = getText(R.string.settings_title)
                val speedButton: Preference? = findPreference(getString(R.string.speed_preferences))
                val logsButton: Preference? = findPreference(getString(R.string.log_preferences))
                val alarmButton: Preference? = findPreference(getString(R.string.alarm_preferences))
                val watchButton: Preference? = findPreference(getString(R.string.watch_preferences))
                val wheelButton: Preference? = findPreference(getString(R.string.wheel_settings))
                val tripButton: Preference? = findPreference(getString(R.string.trip_settings))
                val aboutButton: Preference? = findPreference(getString(R.string.about))
                val donateButton: Preference? = findPreference(getString(R.string.donate))
                speedButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Speed
                    speedSettings.fill(WheelData.getInstance().mac + "_")
                    setupScreen()
                    true
                }
                logsButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Logs
                    preferenceScreen.removeAll()
                    addPreferencesFromResource(R.xml.preferences_logs)
                    setupScreen()
                    // clear icons in not original theme
                    if (WheelLog.AppConfig.appTheme != R.style.OriginalTheme) {
                        findPreference<SwitchPreference>(getString(R.string.auto_log))?.setIcon(R.drawable.transparent)
                        findPreference<SwitchPreference>(getString(R.string.auto_upload_ec))?.setIcon(R.drawable.transparent)
                        findPreference<SwitchPreference>(getString(R.string.log_location_data))?.setIcon(R.drawable.transparent)
                        findPreference<SwitchPreference>(getString(R.string.use_gps))?.setIcon(R.drawable.transparent)
                        findPreference<SwitchPreference>(getString(R.string.use_raw_data))?.setIcon(R.drawable.transparent)
                    }
                    true
                }
                alarmButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Alarms
                    generalSettings.fill(WheelData.getInstance().mac + "_")
                    setupScreen()
                    true
                }
                watchButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Watch
                    watchSettings.fill(WheelData.getInstance().mac + "_")
                    setupScreen()
                    true
                }
                wheelButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Wheel
                    wheelSettings.fill(WheelData.getInstance().mac + "_")
                    setupScreen()
                    true
                }
                tripButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Trip
                    preferenceScreen.removeAll()
                    addPreferencesFromResource(R.xml.preferences_trip)
                    setupScreen()
                    // clear icons in not original theme
                    if (WheelLog.AppConfig.appTheme != R.style.OriginalTheme) {
                        findPreference<Preference>(getString(R.string.reset_user_distance))?.setIcon(R.drawable.transparent)
                        findPreference<Preference>(getString(R.string.reset_lowest_battery))?.setIcon(R.drawable.transparent)
                    }
                    true
                }
                aboutButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val versionName = BuildConfig.VERSION_NAME
                    val buildTime = BuildConfig.BUILD_TIME
                    AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.about_app_title)
                        .setMessage(String.format("Version %s \n build at %s github.com/Wheellog/Wheellog.Android \n Thanks to all contributors", versionName, buildTime))
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
                        .setBlackIcon()
                    true
                }
                donateButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val kvm = mapOf(
                        "Crypto" to "https://paymicro.github.io/donate",
                        "Credit card (only from russian bank)" to "https://tinkoff.ru/sl/6iw4b0ugfpC")
                    AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.donate_title)
                        .setItems(kvm.keys.toTypedArray()) { _, which ->
                            val uri = Uri.parse(kvm[kvm.keys.elementAt(which)])
                            startActivity(Intent(ACTION_VIEW, uri))
                        }
                        .setIcon(R.drawable.ic_donate_24)
                        .show()
                        .setBlackIcon()
                    true
                }
                // Themes
                speedButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsSpeedometer))
                logsButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsLog))
                alarmButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsVibration))
                watchButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsWatch))
                wheelButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsWheel))
                tripButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsTrips))
                findPreference<Preference>(getString(R.string.bug_report))?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsBug))
                donateButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsDonate))
                aboutButton?.icon = getDrawableEx(WheelLog.ThemeManager.getId(ThemeIconEnum.SettingsAbout))
            }
            SettingsScreen.Speed -> {
                tb.title = getText(R.string.speed_settings_title)
                findPreference<SwitchPreference>(getString(R.string.custom_beep))?.summary =
                        WheelLog.AppConfig.beepFile.lastPathSegment
                switchConnectionSoundIsVisible()
            }
            SettingsScreen.Logs -> {
                tb.title = getText(R.string.logs_settings_title)
            }
            SettingsScreen.Alarms -> {
                tb.title = getText(R.string.alarm_settings_title)
                generalSettings.switchAlarmsIsVisible(this)
            }
            SettingsScreen.Watch -> {
                tb.title = getText(R.string.watch_settings_title)
            }
            SettingsScreen.Wheel -> {
                tb.title = getText(R.string.wheel_settings_title)
                val profileNameButton: Preference? = findPreference(getString(R.string.profile_name))
                profileNameButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    DialogHelper.showEditProfileName(requireActivity())
                    true
                }
            }
            SettingsScreen.Trip -> {
                tb.title = getText(R.string.trip_settings_title)
                val resetTopButton: Preference? = findPreference(getString(R.string.reset_top_speed))
                val resetLowestBatteryButton: Preference? = findPreference(getString(R.string.reset_lowest_battery))
                val resetUserDistanceButton: Preference? = findPreference(getString(R.string.reset_user_distance))
                resetTopButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    WheelData.getInstance().resetTopSpeed()
                    true
                }
                resetLowestBatteryButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    WheelData.getInstance().resetVoltageSag()
                    activity?.sendBroadcast(Intent(Constants.ACTION_PREFERENCE_RESET))
                    true
                }
                resetUserDistanceButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    WheelData.getInstance().resetUserDistance()
                    true
                }
            }
        }

        switchSpecificSettingsIsVisible()
        generalSettings.switchAlarmsIsVisible(this)
    }

    private fun refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctState(getString(R.string.auto_log))
            correctState(getString(R.string.use_raw_data))
            correctState(getString(R.string.log_location_data))
            correctState(getString(R.string.use_gps))
            correctState(getString(R.string.auto_upload))
            correctState(getString(R.string.auto_upload_ec))
        }
    }

    private fun correctState(preference: String) {
        val pref = findPreference<Preference>(preference) ?: return
        when (pref) {
            is TwoStatePreference -> {
                val settingState = WheelLog.AppConfig.getValue(preference, false)
                val checkState = pref.isChecked
                if (settingState != checkState) pref.isChecked = settingState
            }
            is SeekBarPreference -> {
                val settingState = WheelLog.AppConfig.getValue(preference, 0)
                val sbState = pref.value
                if (settingState != sbState) pref.value = settingState
            }
        }
    }

    private fun showMainMenu() {
        preferenceScreen.removeAll()
        addPreferencesFromResource(R.xml.preferences)
        val wheelButton: Preference = findPreference(getString(R.string.wheel_settings))
                ?: return

        if (WheelData.getInstance().wheelType != WHEEL_TYPE.Unknown) {
            wheelButton.isEnabled = true
        }
        currentScreen = SettingsScreen.Main
        setupScreen()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        setupScreen()

        // override Back key
        view?.apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (currentScreen != SettingsScreen.Main) {
                        showMainMenu()
                        return@OnKeyListener true
                    } else {
                        return@OnKeyListener false
                    }
                }
                false
            })
        }
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    private fun switchSpecificSettingsIsVisible() {
        val isVisible = WheelData.getInstance().wheelType != WHEEL_TYPE.Unknown
        val specificPreferences = arrayOf(
                getString(R.string.trip_settings),
                getString(R.string.alarm_preferences),
                getString(R.string.wheel_settings),
                getString(R.string.last_mac),
                getString(R.string.profile_name)
        )

        for (preference in specificPreferences) {
            findPreference<Preference>(preference)?.isVisible = isVisible
        }

        // Hide inaccessible settings for VoltageTiltbackUnsupported wheels
        if (WheelData.getInstance()?.isVoltageTiltbackUnsupported == true) {
            val preferences = arrayOf(
                    getString(R.string.fixed_percents),
                    getString(R.string.cell_voltage_tiltback),
                    getString(R.string.battery_capacity),
                    getString(R.string.charging_power))
            for (preference in preferences) {
                findPreference<Preference>(preference)?.isVisible = false
            }
        }
    }

    private fun switchConnectionSoundIsVisible() {
        findPreference<SeekBarPreference>(getString(R.string.no_connection_sound))?.isEnabled =
                WheelLog.AppConfig.connectionSound
    }

    private enum class SettingsScreen {
        Main, Speed, Logs, Alarms, Watch, Wheel, Trip
    }
}