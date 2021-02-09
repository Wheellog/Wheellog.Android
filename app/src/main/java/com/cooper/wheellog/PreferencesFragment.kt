package com.cooper.wheellog

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.*
import androidx.preference.*
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.cooper.wheellog.presentation.preferences.MultiSelectPreferenceDialogFragment.Companion.newInstance
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.KingsongAdapter
import kotlinx.coroutines.*
import timber.log.Timber

class PreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var mDataWarningDisplayed = false
    private var currentScreen = SettingsScreen.Main
    private val dialogTag = "wheellog.MainPreferenceFragment.DIALOG"
    private val authRequestCode = 50

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        changeWheelType()
        checkAndRequestPermissions()
    }

    private fun changeWheelType() {
        switchSpecificSettingsIsVisible()
        switchAlarmsIsVisible()
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
        Timber.i("onActivityResult")
        when (requestCode) {
            authRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    WheelLog.AppConfig.ecToken = ElectroClub.instance.userToken
                    WheelLog.AppConfig.ecUserId = ElectroClub.instance.userId
                    ElectroClub.instance.getAndSelectGarageByMacOrPrimary(WheelData.getInstance().mac) { }
                } else {
                    WheelLog.AppConfig.autoUploadEc = false
                    WheelLog.AppConfig.ecToken = null
                    WheelLog.AppConfig.ecUserId = null
                    refreshVolatileSettings()
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && (WheelLog.AppConfig.autoLog || WheelLog.AppConfig.enableRawData)) {
            requestPermissionsEx(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    SettingsActivity.permissionWriteCode)
        }
        if (WheelLog.AppConfig.logLocationData) {
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
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (context == null) {
            return
        }

        val resName = key?.replace(WheelData.getInstance().mac + "_", "")
        when (WheelLog.AppConfig.getResId(resName)) {
            R.string.auto_log, R.string.use_raw_data, R.string.log_location_data -> checkAndRequestPermissions()
            R.string.ec_token -> ElectroClub.instance.userToken = WheelLog.AppConfig.ecToken
            R.string.ec_user_id -> ElectroClub.instance.userId = WheelLog.AppConfig.ecUserId
            R.string.connection_sound -> switchConnectionSoundIsVisible()
            R.string.alarms_enabled, R.string.altered_alarms -> switchAlarmsIsVisible()
            R.string.auto_upload_ec -> {
                if (WheelLog.AppConfig.autoUploadEc && !mDataWarningDisplayed) {
                    WheelLog.AppConfig.autoUploadEc = false
                    AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.enable_auto_upload_title))
                            .setMessage(getString(R.string.enable_auto_upload_descriprion))
                            .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                mDataWarningDisplayed = true
                                WheelLog.AppConfig.autoUploadEc = true
                                refreshVolatileSettings()
                                if (ElectroClub.instance.userToken == null) {
                                    startActivityForResult(Intent(activity, LoginActivity::class.java), authRequestCode)
                                } else {
                                    ElectroClub.instance.getAndSelectGarageByMacOrPrimary(WheelData.getInstance().mac) { }
                                }
                            }
                            .setNegativeButton(android.R.string.no) { _: DialogInterface?, _: Int ->
                                mDataWarningDisplayed = false
                                // TODO check user token
                                // TODO: need to implement a logout
                                // logout after uncheck
                                ElectroClub.instance.userToken = null
                                ElectroClub.instance.userId = null
                                WheelLog.AppConfig.ecToken = null
                                refreshVolatileSettings()
                            }
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show()
                } else {
                    mDataWarningDisplayed = false
                }
            }
            R.string.max_speed, R.string.use_mph -> context?.sendBroadcast(Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED))
            R.string.use_eng -> AlertDialog.Builder(requireContext())
                    .setTitle(R.string.use_eng_alert_title)
                    .setMessage(R.string.use_eng_alert_description)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()
            R.string.light_enabled -> WheelData.getInstance().updateLight(WheelLog.AppConfig.lightEnabled)
            R.string.led_enabled -> WheelData.getInstance().updateLed(WheelLog.AppConfig.ledEnabled)
            R.string.handle_button_disabled -> WheelData.getInstance().updateHandleButton(WheelLog.AppConfig.handleButtonDisabled)
            R.string.wheel_max_speed -> WheelData.getInstance().updateMaxSpeed(WheelLog.AppConfig.wheelMaxSpeed)
            R.string.speaker_volume -> WheelData.getInstance().updateSpeakerVolume(WheelLog.AppConfig.speakerVolume)
            R.string.pedals_adjustment -> WheelData.getInstance().updatePedals(WheelLog.AppConfig.pedalsAdjustment)
            R.string.pedals_mode -> WheelData.getInstance().updatePedalsMode(Integer.parseInt(WheelLog.AppConfig.pedalsMode))
            R.string.light_mode -> WheelData.getInstance().adapter?.setLightMode(Integer.parseInt(WheelLog.AppConfig.lightMode))
            R.string.alarm_mode -> WheelData.getInstance().updateAlarmMode(Integer.parseInt(WheelLog.AppConfig.alarmMode))
            R.string.strobe_mode -> WheelData.getInstance().updateStrobe(Integer.parseInt(WheelLog.AppConfig.strobeMode))
            R.string.led_mode -> WheelData.getInstance().updateLedMode(Integer.parseInt(WheelLog.AppConfig.ledMode))
            R.string.wheel_ks_alarm3 -> KingsongAdapter.getInstance().updateKSAlarm3(WheelLog.AppConfig.wheelKsAlarm3)
            R.string.wheel_ks_alarm2 -> KingsongAdapter.getInstance().updateKSAlarm2(WheelLog.AppConfig.wheelKsAlarm2)
            R.string.wheel_ks_alarm1 -> KingsongAdapter.getInstance().updateKSAlarm1(WheelLog.AppConfig.wheelKsAlarm1)
            R.string.ks18l_scaler -> KingsongAdapter.getInstance().set18Lkm(WheelLog.AppConfig.ks18LScaler)
            R.string.current_on_dial -> Timber.i("Change dial type to %b", WheelLog.AppConfig.currentOnDial)
        }
    }

    private fun setupScreen() {
        if (activity == null) {
            return
        }

        val tb: Toolbar = requireActivity().findViewById(R.id.preference_toolbar)
        if (currentScreen == SettingsScreen.Main) {
            tb.navigationIcon = null
        } else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            tb.setNavigationOnClickListener { showMainMenu() }
        }

        when (currentScreen) {
            SettingsScreen.Main -> {
                tb.title = getText(R.string.settings_title)
                val speedButton: Preference? = findPreference(getString(R.string.speed_preferences))
                val logsButton: Preference? = findPreference(getString(R.string.log_preferences))
                val alarmButton: Preference? = findPreference(getString(R.string.alarm_preferences))
                val watchButton: Preference? = findPreference(getString(R.string.watch_preferences))
                val wheelButton: Preference? = findPreference(getString(R.string.wheel_settings))
                val resetTopButton: Preference? = findPreference(getString(R.string.reset_top_speed))
                val resetLowestBatteryButton: Preference? = findPreference(getString(R.string.reset_lowest_battery))
                val resetUserDistanceButton: Preference? = findPreference(getString(R.string.reset_user_distance))
                val lastMacButton: Preference? = findPreference(getString(R.string.last_mac))
                val profileNameButton: Preference? = findPreference(getString(R.string.profile_name))
                val aboutButton: Preference? = findPreference(getString(R.string.about))
                speedButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Speed
                    preferenceScreen.removeAll()
                    addPreferencesFromResource(R.xml.preferences_speed)
                    setupScreen()
                    true
                }
                logsButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Logs
                    preferenceScreen.removeAll()
                    addPreferencesFromResource(R.xml.preferences_logs)
                    setupScreen()
                    true
                }
                alarmButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    currentScreen = SettingsScreen.Alarms
                    preferenceScreen.removeAll()
                    preferenceAlarms()
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
                    preferenceScreen.removeAll()
                    val mac = WheelData.getInstance().mac + "_"
                    when (WheelData.getInstance().wheelType) {
                        WHEEL_TYPE.NINEBOT_Z -> {
                            preferenceNinebotZ(mac)
                        }
                        WHEEL_TYPE.INMOTION -> {
                            preferenceInmotion(mac)
                        }
                        WHEEL_TYPE.KINGSONG -> {
                            preferenceKingsong(mac)
                        }
                        WHEEL_TYPE.GOTWAY -> {
                            preferenceBegode(mac)
                        }
                        WHEEL_TYPE.VETERAN -> {
                            preferenceVeteran(mac)
                        }
                        else -> {
                            preferenceScreen.addPreference(
                                    Preference(context).apply {
                                        key = getString(R.string.no_settings)
                                        isEnabled = false
                                        summary = getString(R.string.unknown_device)
                                    }
                            )
                        }
                    }
                    setupScreen()
                    true
                }
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
                lastMacButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(getText(R.string.edit_mac_addr_title))
                    val input = EditText(activity)
                    input.inputType = InputType.TYPE_CLASS_TEXT
                    input.setText(WheelLog.AppConfig.lastMac)
                    builder.setView(input)
                    builder.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        val deviceAddress = input.text.toString()
                        WheelLog.AppConfig.lastMac = deviceAddress
                        // the wait is needed so that the lastmac is written exactly before it.
                        // because all properties are written asynchronously
                        Thread.sleep(1)
                        WheelLog.AppConfig.passwordForWheel = ""
                    }
                    builder.setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
                    builder.show()
                    true
                }
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
            SettingsScreen.Speed -> {
                tb.title = getText(R.string.speed_settings_title)
                switchConnectionSoundIsVisible()
            }
            SettingsScreen.Logs -> {
                tb.title = getText(R.string.logs_settings_title)
            }
            SettingsScreen.Alarms -> {
                tb.title = getText(R.string.alarm_settings_title)
                switchAlarmsIsVisible()
            }
            SettingsScreen.Watch -> {
                tb.title = getText(R.string.watch_settings_title)
            }
            SettingsScreen.Wheel -> {
                tb.title = getText(R.string.wheel_settings_title)
            }
        }

        switchSpecificSettingsIsVisible()
        switchAlarmsIsVisible()
    }

    private fun preferenceNinebotZ(mac: String) {
        arrayOf(
                Preference(context).apply {
                    key = mac + getString(R.string.no_settings)
                    isEnabled = false
                    summary = getString(R.string.ninebotz_settings_title)
                }
        ).forEach {
            preferenceScreen.addPreference(it)
        }
    }

    private fun preferenceInmotion(mac: String) {
        arrayOf(
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.light_enabled)
                    title = getString(R.string.on_headlight_title)
                    summary = getString(R.string.on_headlight_description)
                    isChecked = WheelLog.AppConfig.lightEnabled
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.led_enabled)
                    title = getString(R.string.leds_settings_title)
                    summary = getString(R.string.leds_settings_description)
                    isVisible = !WheelData.getInstance().model.startsWith("V5")
                    isChecked = WheelLog.AppConfig.ledEnabled
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.handle_button_disabled)
                    title = getString(R.string.disable_handle_button_title)
                    summary = getString(R.string.disable_handle_button_description)
                    isChecked = WheelLog.AppConfig.handleButtonDisabled
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.tilt_back_description)
                    summary = getString(R.string.max_speed_title)
                    min = 3
                    max = 45
                    unit = getString(R.string.kmh)
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.speaker_volume)
                    title = getString(R.string.speaker_volume_title)
                    summary = getString(R.string.speaker_volume_description)
                    min = 0
                    max = 100
                    unit = "%"
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.speakerVolume)
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.pedals_adjustment)
                    title = getString(R.string.pedal_horizont_title)
                    summary = getString(R.string.pedal_horizont_description)
                    min = -80
                    max = 80
                    unit = "°"
                    increment = 1
                    decimalPlaces = 1
                    setDefaultValue(WheelLog.AppConfig.pedalsAdjustment)
                }
        ).forEach {
            preferenceScreen.addPreference(it)
        }
    }

    private fun preferenceKingsong(mac: String) {
        val alertsUpdated = KingsongAdapter.getInstance().ksAlertsAndSpeedupdated
        arrayOf(
                ListPreference(context).apply {
                    key = mac + getString(R.string.light_mode)
                    title = getString(R.string.light_mode_title)
                    summary = getString(R.string.on_off_auto)
                    setEntries(R.array.light_mode_ks)
                    setEntryValues(R.array.light_mode_values)
                    setDefaultValue(WheelLog.AppConfig.lightMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.strobe_mode)
                    title = getString(R.string.strobe_mode_title)
                    summary = getString(R.string.on_off)
                    setEntries(R.array.strobe_mode_ks)
                    setEntryValues(R.array.strobe_mode_values)
                    setDefaultValue(WheelLog.AppConfig.strobeMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.led_mode)
                    title = getString(R.string.led_mode_title)
                    summary = getString(R.string.on_off)
                    setEntries(R.array.led_mode)
                    setEntryValues(R.array.led_mode_values)
                    setDefaultValue(WheelLog.AppConfig.ledMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.pedals_mode)
                    title = getString(R.string.pedals_mode_title)
                    summary = getString(R.string.soft_medium_hard)
                    setEntries(R.array.pedals_mode)
                    setEntryValues(R.array.pedals_mode_values)
                    setDefaultValue(WheelLog.AppConfig.pedalsMode)
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.maxSpeed)
                    if (alertsUpdated) {
                        value = WheelData.getInstance().wheelMaxSpeed
                    }
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm3)
                    title = getString(R.string.alert3_title)
                    summary = getString(R.string.alarm3_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm3)
                    if (alertsUpdated) {
                        value = WheelLog.AppConfig.wheelKsAlarm3
                    }
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm2)
                    title = getString(R.string.alert2_title)
                    summary = getString(R.string.alarm2_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm2)
                    if (alertsUpdated) {
                        value = WheelLog.AppConfig.wheelKsAlarm2
                    }
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm1)
                    title = getString(R.string.alert1_title)
                    summary = getString(R.string.alarm1_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm1)
                    if (alertsUpdated) {
                        value = WheelLog.AppConfig.wheelKsAlarm1
                    }
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.ks18l_scaler)
                    title = getString(R.string.ks18l_scaler_title)
                    summary = getString(R.string.ks18l_scaler_description)
                }
        ).forEach {
            preferenceScreen.addPreference(it)
        }
    }

    private fun preferenceBegode(mac: String) {
        arrayOf(
                ListPreference(context).apply {
                    key = mac + getString(R.string.light_mode)
                    title = getString(R.string.light_mode_title)
                    summary = getString(R.string.on_off_strobe)
                    setEntries(R.array.light_mode_gw)
                    setEntryValues(R.array.light_mode_values)
                    setDefaultValue(WheelLog.AppConfig.lightMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.alarm_mode)
                    title = getString(R.string.alarm_mode_title)
                    summary = getString(R.string.alarm_settings_title)
                    setEntries(R.array.alarm_mode_gw)
                    setEntryValues(R.array.alarm_mode_values)
                    setDefaultValue(WheelLog.AppConfig.alarmMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.pedals_mode)
                    title = getString(R.string.pedals_mode_title)
                    summary = getString(R.string.soft_medium_hard)
                    setEntries(R.array.pedals_mode)
                    setEntryValues(R.array.pedals_mode_values)
                    setDefaultValue(WheelLog.AppConfig.pedalsMode)
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    max = 99
                    min = 0
                    increment = 1
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.maxSpeed)
                },
                Preference(context).apply {
                    key = mac + getString(R.string.start_calibration)
                    title = getString(R.string.calibration_title)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        WheelData.getInstance().updateCalibration()
                        true
                    }
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.use_ratio)
                    title = getString(R.string.is_gotway_mcm_title)
                    summary = getString(R.string.is_gotway_mcm_description)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_voltage)
                    title = getString(R.string.battery_voltage_title)
                    summary = getString(R.string.battary_voltage_description)
                    setEntries(R.array.gotway_voltage)
                    setEntryValues(R.array.gotway_voltage_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayVoltage)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_negative)
                    title = getString(R.string.gotway_negative_title)
                    summary = getString(R.string.gotway_negative_description)
                    setEntries(R.array.gotway_negative)
                    setEntryValues(R.array.gotway_negative_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayNegative)
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.connect_beep)
                    title = getString(R.string.connect_beep_title)
                    summary = getString(R.string.connect_beep_description)
                    setDefaultValue(true)
                }
        ).forEach {
            preferenceScreen.addPreference(it)
        }
    }

    private fun preferenceVeteran(mac: String) {
        arrayOf(
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.connect_beep)
                    title = getString(R.string.connect_beep_title)
                    summary = getString(R.string.connect_beep_description)
                    setDefaultValue(true)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_negative)
                    title = getString(R.string.gotway_negative_title)
                    summary = getString(R.string.gotway_negative_description)
                    setEntries(R.array.gotway_negative)
                    setEntryValues(R.array.gotway_negative_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayNegative)
                }
        ).forEach {
            preferenceScreen.addPreference(it)
        }
    }

    private fun preferenceAlarms() {
        val mac = WheelData.getInstance().mac + "_"
        arrayOf(
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.alarms_enabled)
                    title = getString(R.string.enable_alarms_title)
                    summary = getString(R.string.enable_alarms_description)
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.disable_phone_vibrate)
                    title = getString(R.string.disable_phone_vibrate_title)
                    summary = getString(R.string.disable_phone_vibration_description)
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.disable_phone_beep)
                    title = getString(R.string.disable_phone_beep_title)
                    summary = getString(R.string.disable_phone_beep_description)
                },
                CheckBoxPreference(context).apply {
                    key = mac + getString(R.string.altered_alarms)
                    title = getString(R.string.altered_alarms_title)
                    summary = getString(R.string.altered_alarms_description)
                },
                addPreferenceCategory(getString(R.string.speed_alarm1_phone_title),
                        mac + getString(R.string.speed_alarm1),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_1_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(29)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_1_battery)
                            title = getString(R.string.alarm_1_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(100)
                        }),
                addPreferenceCategory(getString(R.string.speed_alarm2_phone_title),
                        mac + getString(R.string.speed_alarm2),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_2_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_2_battery)
                            title = getString(R.string.alarm_2_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.speed_alarm3_phone_title),
                        mac + getString(R.string.speed_alarm3),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_3_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_3_battery)
                            title = getString(R.string.alarm_3_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.altered_alarms_pref_title),
                        mac + getString(R.string.altered_alarms_section),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.rotation_speed)
                            title = getString(R.string.rotation_speed_title)
                            summary = getString(R.string.rotation_speed_description)
                            min = 0
                            max = 2000
                            decimalPlaces = 1
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(500)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.rotation_voltage)
                            title = getString(R.string.rotation_voltage_title)
                            summary = getString(R.string.rotation_voltage_description)
                            min = 0
                            max = 1200
                            decimalPlaces = 1
                            unit = getString(R.string.volt)
                            increment = 1
                            setDefaultValue(840)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.power_factor)
                            title = getString(R.string.power_factor_title)
                            summary = getString(R.string.power_factor_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(90)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor1)
                            title = getString(R.string.alarm_factor1_title)
                            summary = getString(R.string.alarm_factor1_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(80)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor2)
                            title = getString(R.string.alarm_factor2_title)
                            summary = getString(R.string.alarm_factor2_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(90)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor3)
                            title = getString(R.string.alarm_factor3_title)
                            summary = getString(R.string.alarm_factor3_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(95)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_speed)
                            title = getString(R.string.warning_speed_title)
                            summary = getString(R.string.warning_speed_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_pwm)
                            title = getString(R.string.warning_pwm_title)
                            summary = getString(R.string.warning_pwm_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_speed_period)
                            title = getString(R.string.warning_speed_period_title)
                            summary = getString(R.string.warning_speed_period_description)
                            min = 0
                            max = 60
                            unit = getString(R.string.sec)
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.current_alarm_title), null,
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_current)
                            title = getString(R.string.current_title)
                            summary = getString(R.string.alarm_current_description)
                            min = 0
                            max = 300
                            unit = getString(R.string.amp)
                            increment = 1
                            setDefaultValue(35)
                        }),
                addPreferenceCategory(getString(R.string.temperature_alarm_title), null,
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_temperature)
                            title = getString(R.string.temperature_title)
                            summary = getString(R.string.alarm_temperature_description)
                            min = 0
                            max = 120
                            unit = "°"
                            increment = 1
                            setDefaultValue(60)
                        })
        ).forEach {
            preferenceScreen.addPreference(it)
            if (preferenceScreen.preferenceCount > 1) {
                it.dependency = mac + getString(R.string.alarms_enabled)
            }
        }
    }

    private fun addPreferenceCategory(title: String, key: String?, vararg insidePrefs: Preference): PreferenceCategory {
        return PreferenceCategory(context).apply {
            this.title = title
            this.key = key
            isVisible = true
            GlobalScope.launch {
                // waiting attaching to preferenceScreen
                for (i in 1..100) {
                    if (parent == null) {
                        delay(5)
                    } else {
                        insidePrefs.forEach {
                            addPreference(it)
                        }
                        return@launch
                    }
                }
            }
        }
    }

    fun refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log))
            correctCheckState(getString(R.string.use_raw_data))
            correctCheckState(getString(R.string.log_location_data))
            correctCheckState(getString(R.string.auto_upload))
            correctCheckState(getString(R.string.auto_upload_ec))
        }
    }

    private fun correctCheckState(preference: String) {
        val settingState = WheelLog.AppConfig.getValue(preference, false)
        val checkBoxPreference = findPreference<CheckBoxPreference>(preference)
                ?: return
        val checkState = checkBoxPreference.isChecked
        if (settingState != checkState) checkBoxPreference.isChecked = settingState
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
                getString(R.string.alarm_preferences),
                getString(R.string.wheel_settings),
                getString(R.string.reset_top_speed),
                getString(R.string.reset_lowest_battery),
                getString(R.string.reset_user_distance),
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

    private fun switchAlarmsIsVisible() {
        val alarmsEnabled = WheelLog.AppConfig.alarmsEnabled
        val alteredAlarms = WheelLog.AppConfig.alteredAlarms
        val ksAlteredAlarms = WheelData.getInstance().wheelType == WHEEL_TYPE.KINGSONG
        val categoryPreferencesNormal = arrayOf(
                getString(R.string.speed_alarm1),
                getString(R.string.speed_alarm2),
                getString(R.string.speed_alarm3)
        )
        val preferencesCommon = arrayOf(
                getString(R.string.alarm_current),
                getString(R.string.alarm_temperature)
        )
        val preferencesKs = arrayOf(
                getString(R.string.rotation_voltage),
                getString(R.string.rotation_speed),
                getString(R.string.power_factor)
        )
        val mac = WheelData.getInstance().mac + "_"
        for (preference in categoryPreferencesNormal) {
            findPreference<PreferenceCategory>(mac + preference)?.isVisible = alarmsEnabled && !alteredAlarms
        }
        findPreference<PreferenceCategory>(mac + getString(R.string.altered_alarms_section))?.isVisible = alarmsEnabled && alteredAlarms
        for (preference in preferencesCommon) {
            findPreference<Preference>(mac + preference)?.isVisible = alarmsEnabled
        }
        for (preference in preferencesKs) {
            findPreference<Preference>(mac + preference)?.isVisible = alarmsEnabled && !ksAlteredAlarms && alteredAlarms
        }
    }

    private fun switchConnectionSoundIsVisible() {
        findPreference<SeekBarPreference>(getString(R.string.no_connection_sound))?.isEnabled =
                WheelLog.AppConfig.connectionSound
    }

    private enum class SettingsScreen {
        Main, Speed, Logs, Alarms, Watch, Wheel
    }
}