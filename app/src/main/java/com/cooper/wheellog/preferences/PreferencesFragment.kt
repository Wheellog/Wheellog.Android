package com.cooper.wheellog.preferences

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.preference.*
import com.cooper.wheellog.*
import com.cooper.wheellog.R
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.cooper.wheellog.presentation.preferences.MultiSelectPreferenceDialogFragment.Companion.newInstance
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.KingsongAdapter
import com.cooper.wheellog.utils.SomeUtil.Companion.getDrawableEx
import timber.log.Timber

class PreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var mDataWarningDisplayed = false
    private var currentScreen = SettingsScreen.Main
    private val dialogTag = "wheellog.MainPreferenceFragment.DIALOG"
    private val authRequestCode = 50
    private val mediaRequestCode = 60
    private lateinit var speedSettings: SpeedSettings
    private lateinit var wheelSettings: WheelSettings
    private lateinit var alarmSettings: AlarmSettings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        speedSettings = SpeedSettings(requireContext(), preferenceScreen)
        wheelSettings = WheelSettings(requireContext(), preferenceScreen)
        alarmSettings = AlarmSettings(requireContext(), preferenceScreen)
        changeWheelType()
        checkAndRequestPermissions()
    }

    private fun changeWheelType() {
        switchSpecificSettingsIsVisible()
        alarmSettings.switchAlarmsIsVisible(this)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("Settings onActivityResult $resultCode")
        when (requestCode) {
            authRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    WheelLog.AppConfig.autoUploadEc = true
                    refreshVolatileSettings()
                    ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(WheelData.getInstance().mac, activity as Activity) { }
                    WheelLog.AppConfig.autoUploadEc = true
                } else {
                    ElectroClub.instance.logout()
                }
                refreshVolatileSettings()
            }
            mediaRequestCode -> {
                val pref = findPreference<SwitchPreference>(getString(R.string.custom_beep))
                var isDefault = true
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    isDefault = data.data == null
                    if (!isDefault) {
                        pref?.summary = data.data?.path
                        WheelLog.AppConfig.beepFile = data.data!!
                    }
                }
                if (isDefault) {
                    pref?.summary = "default"
                    WheelLog.AppConfig.beepFile = Uri.EMPTY
                    WheelLog.AppConfig.useCustomBeep = false
                    pref?.isChecked = false
                }
            }
        }
    }

    private fun requestPermissionsEx(permissions: Array<String>, code: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.requestPermissions(permissions, code)
        } else {
            ActivityCompat.requestPermissions(activity as Activity, permissions, code)
        }
    }

    private fun checkAndRequestPermissions() {
        if (WheelLog.AppConfig.autoLog || WheelLog.AppConfig.enableRawData) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    && (WheelLog.AppConfig.autoLog || WheelLog.AppConfig.enableRawData)) {
                requestPermissionsEx(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        SettingsActivity.permissionWriteCode)
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && WheelLog.AppConfig.useCustomBeep) {
            requestPermissionsEx(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    SettingsActivity.permissionReadCode)
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
                            requestPermissionsEx(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    SettingsActivity.permissionLocationCode)
                        } else {
                            requestPermissionsEx(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    SettingsActivity.permissionLocationCode)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                        WheelLog.AppConfig.useGps = false
                        refreshVolatileSettings()
                    }
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_baseline_gps_24)
                    .show()
        }
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
            R.string.alarms_enabled, R.string.altered_alarms -> alarmSettings.switchAlarmsIsVisible(this)
            R.string.auto_upload_ec -> {
                // TODO check user token
                if (WheelLog.AppConfig.autoUploadEc) {
                    WheelLog.AppConfig.autoUploadEc = false
                    AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.enable_auto_upload_title))
                            .setMessage(getString(R.string.enable_auto_upload_descriprion))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                mDataWarningDisplayed = true
                                if (WheelLog.AppConfig.ecToken == null) {
                                    startActivityForResult(Intent(activity, LoginActivity::class.java), authRequestCode)
                                } else {
                                    ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(WheelData.getInstance().mac, activity as Activity) { }
                                }
                            }
                            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                                ElectroClub.instance.logout()
                                refreshVolatileSettings()
                            }
                            .setCancelable(false)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show()
                } else {
                    // logout after uncheck
                    if (!WheelLog.AppConfig.autoUploadEc) {
                        ElectroClub.instance.logout()
                    }
                }
            }
            R.string.max_speed, R.string.use_mph -> context?.sendBroadcast(Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED))
            R.string.use_eng, R.string.app_theme -> {
                WheelLog.ThemeManager.theme = WheelLog.AppConfig.appTheme
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.use_eng_alert_title)
                        .setMessage(R.string.use_eng_alert_description)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show()
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
            }
            R.string.light_enabled -> wd.updateLight(WheelLog.AppConfig.lightEnabled)
            R.string.led_enabled -> wd.updateLed(WheelLog.AppConfig.ledEnabled)
            R.string.handle_button_disabled -> wd.updateHandleButton(WheelLog.AppConfig.handleButtonDisabled)
            R.string.wheel_max_speed -> wd.updateMaxSpeed(WheelLog.AppConfig.wheelMaxSpeed)
            R.string.speaker_volume -> wd.updateSpeakerVolume(WheelLog.AppConfig.speakerVolume)
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
            R.string.alarm_mode -> wd.updateAlarmMode(Integer.parseInt(WheelLog.AppConfig.alarmMode))
            R.string.strobe_mode -> wd.updateStrobe(Integer.parseInt(WheelLog.AppConfig.strobeMode))
            R.string.led_mode -> wd.updateLedMode(Integer.parseInt(WheelLog.AppConfig.ledMode))
            R.string.wheel_ks_alarm3 -> KingsongAdapter.getInstance().updateKSAlarm3(WheelLog.AppConfig.wheelKsAlarm3)
            R.string.wheel_ks_alarm2 -> KingsongAdapter.getInstance().updateKSAlarm2(WheelLog.AppConfig.wheelKsAlarm2)
            R.string.wheel_ks_alarm1 -> KingsongAdapter.getInstance().updateKSAlarm1(WheelLog.AppConfig.wheelKsAlarm1)
            R.string.ks18l_scaler -> KingsongAdapter.getInstance().set18Lkm(WheelLog.AppConfig.ks18LScaler)
            R.string.current_on_dial -> Timber.i("Change dial type to %b", WheelLog.AppConfig.currentOnDial)
            R.string.custom_beep -> {
                checkAndRequestPermissions()
                speedSettings.selectCustomBeep(this, mediaRequestCode)
            }
            R.string.notification_buttons -> WheelLog.Notifications.update()
            R.string.beep_on_volume_up -> WheelLog.VolumeKeyController.setActive(WheelLog.AppConfig.useBeepOnVolumeUp)
            R.string.use_reconnect -> {
                if (WheelLog.AppConfig.useReconnect)
                    wd.bluetoothLeService?.startReconnectTimer()
                else
                    wd.bluetoothLeService?.stopReconnectTimer()
            }
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
                    alarmSettings.fill(WheelData.getInstance().mac + "_")
                    setupScreen()
                    true
                }
                watchButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Watch
                    preferenceScreen.removeAll()
                    addPreferencesFromResource(R.xml.preferences_watch)
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
                    true
                }
                // Themes
                if (WheelLog.AppConfig.appTheme == R.style.AJDMTheme) {
                    speedButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_speedometer_white_24dp))
                    logsButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_show_chart_white_24dp))
                    alarmButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_vibration_24))
                    watchButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_watch_24))
                    wheelButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_wheel_white_24))
                    tripButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_explore_24))
                    findPreference<Preference>(getString(R.string.bug_report))?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_bug_report_24))
                    aboutButton?.icon = getDrawableEx(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_info_24))
                }
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
                alarmSettings.switchAlarmsIsVisible(this)
            }
            SettingsScreen.Watch -> {
                tb.title = getText(R.string.watch_settings_title)
            }
            SettingsScreen.Wheel -> {
                tb.title = getText(R.string.wheel_settings_title)
                val profileNameButton: Preference? = findPreference(getString(R.string.profile_name))
                profileNameButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(getText(R.string.profile_name_title))
                    val profileName = WheelLog.AppConfig.profileName
                    val input = EditText(activity)
                    input.inputType = InputType.TYPE_CLASS_TEXT
                    input.setText(profileName)
                    builder.setView(input)
                    builder.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        val newProfileName = input.text.toString()
                        WheelLog.AppConfig.profileName = newProfileName
                    }
                    builder.setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
                    builder.show()
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
        alarmSettings.switchAlarmsIsVisible(this)
    }

    fun refreshVolatileSettings() {
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
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        setupScreen()
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
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
        if (WheelData.getInstance().isVoltageTiltbackUnsupported) {
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