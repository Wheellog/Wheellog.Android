package com.cooper.wheellog;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Add methods get and set for new settings at the end of the class
// If the setting is common to all wheels, the 3rd parameter in get<TYPE> and set<TYPE> specify "SettingsType.General" or correct map from "controlSettings"
// If the setting is unique for each wheel, the 3rd parameter in get<TYPE> and set<TYPE> specify "SettingsType.Specific" or correct map from "controlSettings"
//
// Do not forget to add "switchSettingSpecific" method and "controlSettings" fields as needed
// If you need a reaction to a change in the setting, you can describe it in "MainPreferencesFragment.onSharedPreferenceChanged" method
public class AppConfig extends AppConfigBase {
    private AppConfig(Context context) {
        super(context);
    }

    private static AppConfig INSTANCE;
    public static AppConfig getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new AppConfig(context);

        return INSTANCE;
    }

    private Boolean isInProgressControlsMigration = false;

    // Add here all settings that are changed through controls
    private Map<String, SettingsType> controlSettings  = new HashMap<String, SettingsType>() {{
        // Add here all settings that are changed through controls that are unique to each wheel
        // Remember to add them to the "switchSettingSpecific" method
        put("alarms_enabled", SettingsType.Specific);
        put("disable_phone_vibrate", SettingsType.Specific);
        put("disable_phone_beep", SettingsType.Specific);
        put("alarm_1_speed", SettingsType.Specific);
        put("alarm_1_battery", SettingsType.Specific);
        put("alarm_2_speed", SettingsType.Specific);
        put("alarm_2_battery", SettingsType.Specific);
        put("alarm_3_speed", SettingsType.Specific);
        put("alarm_3_battery", SettingsType.Specific);
        put("alarm_current", SettingsType.Specific);
        put("alarm_temperature", SettingsType.Specific);
        put("altered_alarms", SettingsType.Specific);
        put("rotation_speed", SettingsType.Specific);
        put("rotation_voltage", SettingsType.Specific);
        put("power_factor", SettingsType.Specific);
        put("alarm_factor1", SettingsType.Specific);
        put("alarm_factor2", SettingsType.Specific);
        put("alarm_factor3", SettingsType.Specific);
        put("warning_speed", SettingsType.Specific);
        put("warning_pwm", SettingsType.Specific);
        put("warning_speed_period", SettingsType.Specific);
        put("use_short_pwm", SettingsType.Specific);
        put("light_enabled", SettingsType.Specific);
        put("led_enabled", SettingsType.Specific);
        put("handle_button_disabled", SettingsType.Specific);
        put("wheel_max_speed", SettingsType.Specific);
        put("speaker_volume", SettingsType.Specific);
        put("pedals_adjustment", SettingsType.Specific);
        put("pedals_mode", SettingsType.Specific);
        put("light_mode", SettingsType.Specific);
        put("strobe_mode", SettingsType.Specific);
        put("led_mode", SettingsType.Specific);
        put("alarm_mode", SettingsType.Specific);
        put("gotway_voltage", SettingsType.Specific);
        put("gotway_negative", SettingsType.Specific);
        put("wheel_ks_alarm3", SettingsType.Specific);
        put("wheel_ks_alarm2", SettingsType.Specific);
        put("wheel_ks_alarm1", SettingsType.Specific);
        put("use_better_percents", SettingsType.Specific);
        put("fixed_percents", SettingsType.Specific);
        put("tiltback_voltage", SettingsType.Specific);
        put("current_on_dial", SettingsType.Specific);
        put("battery_capacity", SettingsType.Specific);
        put("charging_power", SettingsType.Specific);
        put("connect_beep", SettingsType.Specific);
        put("max_speed", SettingsType.Specific);
        put("use_ratio", SettingsType.Specific);
        put("ks18l_scaler", SettingsType.Specific);
        put("profile_name", SettingsType.Specific);

        // Add here all settings, which are changed through controls, which are common for all wheels
        // Remember to add them to the "switchSettingSpecific" method
        put("show_unknown_devices", SettingsType.General);
        put("auto_log", SettingsType.General);
        put("auto_upload_ec", SettingsType.General);
        put("ec_token", SettingsType.General);
        put("ec_user_id", SettingsType.General);
        put("auto_upload", SettingsType.General);
        put("log_location_data", SettingsType.General);
        put("use_gps", SettingsType.General);
        put("use_mph", SettingsType.General);
        put("use_eng", SettingsType.General);
        put("use_pip_mode", SettingsType.General);
        put("connection_sound", SettingsType.General);
        put("no_connection_sound", SettingsType.General);
        put("use_stop_music", SettingsType.General);
        put("garmin_connectiq_enable", SettingsType.General);
        put("horn_mode", SettingsType.General);
        put("view_blocks", SettingsType.General);
        put("last_mac", SettingsType.General);
    }};

    public Map<String, SettingsType> getControlSettings() {
        return controlSettings;
    }

    @Override
    public void switchSettingSpecific(String keyString, Boolean fromControl) {
        Boolean[] fromControlOptional = new Boolean[] { fromControl };
        Boolean[] fromControlReverseOptional = new Boolean[] { !fromControl };
        switch (keyString) {
            // Add here all settings that are changed through controls that are unique to each wheel
            // Remember to add them to the "controlSettings" array
            case "alarms_enabled": setAlarmsEnabled(getAlarmsEnabled(fromControlOptional), fromControlReverseOptional); break;
            case "disable_phone_vibrate": setDisablePhoneVibrate(getDisablePhoneVibrate(fromControlOptional), fromControlReverseOptional); break;
            case "disable_phone_beep": setDisablePhoneBeep(getDisablePhoneBeep(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_1_speed": setAlarm1Speed(getAlarm1Speed(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_1_battery": setAlarm1Battery(getAlarm1Battery(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_2_speed": setAlarm2Speed(getAlarm2Speed(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_2_battery": setAlarm2Battery(getAlarm2Battery(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_3_speed": setAlarm3Speed(getAlarm3Speed(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_3_battery": setAlarm3Battery(getAlarm3Battery(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_current": setAlarmCurrent(getAlarmCurrent(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_temperature": setAlarmTemperature(getAlarmTemperature(fromControlOptional), fromControlReverseOptional); break;
            case "altered_alarms": setAlteredAlarms(getAlteredAlarms(fromControlOptional), fromControlReverseOptional); break;
            case "rotation_speed": setRotationSpeed(getRotationSpeed(fromControlOptional), fromControlReverseOptional); break;
            case "rotation_voltage": setRotationVoltage(getRotationVoltage(fromControlOptional), fromControlReverseOptional); break;
            case "power_factor": setPowerFactor(getPowerFactor(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_factor1": setAlarmFactor1(getAlarmFactor1(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_factor2": setAlarmFactor2(getAlarmFactor2(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_factor3": setAlarmFactor3(getAlarmFactor3(fromControlOptional), fromControlReverseOptional); break;
            case "warning_speed": setWarningSpeed(getWarningSpeed(fromControlOptional), fromControlReverseOptional); break;
            case "warning_pwm": setWarningPwm(getWarningPwm(fromControlOptional), fromControlReverseOptional); break;
            case "warning_speed_period": setWarningSpeedPeriod(getWarningSpeedPeriod(fromControlOptional), fromControlReverseOptional); break;
            case "use_short_pwm": setUseShortPwm(getUseShortPwm(fromControlOptional), fromControlReverseOptional); break;
            case "light_enabled": setLightEnabled(getLightEnabled(fromControlOptional), fromControlReverseOptional); break;
            case "led_enabled": setLedEnabled(getLedEnabled(fromControlOptional), fromControlReverseOptional); break;
            case "handle_button_disabled": setHandleButtonDisabled(getHandleButtonDisabled(fromControlOptional), fromControlReverseOptional); break;
            case "wheel_max_speed": setWheelMaxSpeed(getWheelMaxSpeed(fromControlOptional), fromControlReverseOptional); break;
            case "speaker_volume": setSpeakerVolume(getSpeakerVolume(fromControlOptional), fromControlReverseOptional); break;
            case "pedals_adjustment": setPedalsAdjustment(getPedalsAdjustment(fromControlOptional), fromControlReverseOptional); break;
            case "pedals_mode": setPedalsMode(getPedalsMode(fromControlOptional), fromControlReverseOptional); break;
            case "light_mode": setLightMode(getLightMode(fromControlOptional), fromControlReverseOptional); break;
            case "strobe_mode": setStrobeMode(getStrobeMode(fromControlOptional), fromControlReverseOptional); break;
            case "led_mode": setLedMode(getLedMode(fromControlOptional), fromControlReverseOptional); break;
            case "alarm_mode": setAlarmMode(getAlarmMode(fromControlOptional), fromControlReverseOptional); break;
            case "gotway_voltage": setGotwayVoltage(getGotwayVoltage(fromControlOptional), fromControlReverseOptional); break;
            case "gotway_negative": setGotwayNegative(getGotwayNegative(fromControlOptional), fromControlReverseOptional); break;
            case "wheel_ks_alarm3": setWheelKsAlarm1(getWheelKsAlarm1(fromControlOptional), fromControlReverseOptional); break;
            case "wheel_ks_alarm2": setWheelKsAlarm2(getWheelKsAlarm2(fromControlOptional), fromControlReverseOptional); break;
            case "wheel_ks_alarm1": setWheelKsAlarm3(getWheelKsAlarm3(fromControlOptional), fromControlReverseOptional); break;
            case "use_better_percents": setUseBetterPercents(getUseBetterPercents(fromControlOptional), fromControlReverseOptional); break;
            case "fixed_percents": setFixedPercents(getFixedPercents(fromControlOptional), fromControlReverseOptional); break;
            case "tiltback_voltage": setTiltbackVoltage(getTiltbackVoltage(fromControlOptional), fromControlReverseOptional); break;
            case "current_on_dial": setCurrentOnDial(getCurrentOnDial(fromControlOptional), fromControlReverseOptional); break;
            case "battery_capacity": setBatteryCapacity(getBatteryCapacity(fromControlOptional), fromControlReverseOptional); break;
            case "charging_power": setChargingPower(getChargingPower(fromControlOptional), fromControlReverseOptional); break;
            case "connect_beep": setConnectBeep(getConnectBeep(fromControlOptional), fromControlReverseOptional); break;
            case "max_speed": setMaxSpeed(getMaxSpeed(fromControlOptional), fromControlReverseOptional); break;
            case "use_ratio": setUseRatio(getUseRatio(fromControlOptional), fromControlReverseOptional); break;
            case "ks18l_scaler": setKs18LScaler(getKs18LScaler(fromControlOptional), fromControlReverseOptional); break;
            case "profile_name": setProfileName(getProfileName(fromControlOptional), fromControlReverseOptional); break;

            // Add here all settings, which are changed through controls, which are common for all wheels
            // Remember to add them to the "controlSettings" array.
            case "show_unknown_devices": setShowUnknownDevices(getShowUnknownDevices(fromControlOptional), fromControlReverseOptional); break;
            case "auto_log": setAutoLog(getAutoLog(fromControlOptional), fromControlReverseOptional); break;
            case "auto_upload_ec": setAutoUploadEc(getAutoUploadEc(fromControlOptional), fromControlReverseOptional); break;
            case "ec_token": setEcToken(getEcToken(fromControlOptional), fromControlReverseOptional); break;
            case "ec_user_id": setEcUserId(getEcUserId(fromControlOptional), fromControlReverseOptional); break;
            case "auto_upload": setAutoUpload(getAutoUpload(fromControlOptional), fromControlReverseOptional); break;
            case "log_location_data": setLogLocationData(getLogLocationData(fromControlOptional), fromControlReverseOptional); break;
            case "use_gps": setUseGps(getUseGps(fromControlOptional), fromControlReverseOptional); break;
            case "use_mph": setUseMph(getUseMph(fromControlOptional), fromControlReverseOptional); break;
            case "use_eng": setUseEng(getUseEng(fromControlOptional), fromControlReverseOptional); break;
            case "use_pip_mode": setUsePipMode(getUsePipMode(fromControlOptional), fromControlReverseOptional); break;
            case "connection_sound": setConnectionSound(getConnectionSound(fromControlOptional), fromControlReverseOptional); break;
            case "no_connection_sound": setNoConnectionSound(getNoConnectionSound(fromControlOptional), fromControlReverseOptional); break;
            case "use_stop_music": setUseStopMusic(getUseStopMusic(fromControlOptional), fromControlReverseOptional); break;
            case "garmin_connectiq_enable": setGarminConnectiqEnable(getGarminConnectIqEnable(fromControlOptional), fromControlReverseOptional); break;
            case "horn_mode": setHornMode(getHornMode(fromControlOptional), fromControlReverseOptional); break;
            case "view_blocks": setViewBlocks(getViewBlocks(fromControlOptional), fromControlReverseOptional); break;
            case "last_mac": setLastMac(getLastMac(fromControlOptional), fromControlReverseOptional); break;
        }
    }

    public Boolean getIsInProgressControlsMigration() {
        return isInProgressControlsMigration;
    }

    @Override
    // Get all settings from application and move to controls settings
    public void setAllSettingsToControls() {
        isInProgressControlsMigration = true;
        for (String key : controlSettings.keySet()) {
            switchSettingSpecific(key, false);
        }
        isInProgressControlsMigration = false;
    }

    @Override
    // Get all settings from controls and move to application settings
    public void setAllControlsToSettings() {
        for (String key : controlSettings.keySet()) {
            switchSettingSpecific(key, true);
        }
    }

    // Custom settings
    public String getAdvDataForWheel(String id) {
        return getString("wheel_adv_data_" + id, "");
    }

    public void setAdvDataForWheel(String id, String advData) {
        setString("wheel_adv_data_" + id, advData);
    }

    public Long getUserDistance(String id) {
        return getLong("user_distance_" + id, 0L);
    }

    public void setUserDistance(String id, long distance) {
        setLong("user_distance_" + id, distance);
    }

    public String getPasswordForWheel(String id) {
        return getString("wheel_password_" + id, "000000");
    }

    public void setPasswordForWheel(String id, String password) {
        while (password.length() < 6) {
            password = "0" + password;
        }

        setString("wheel_password_" + id, password);
    }

    public Boolean hasPasswordForWheel(String id) {
        return contains("wheel_password_" + id);
    }

    public Boolean isFirstRun() {
        if (contains("first_run"))
            return false;

        setBoolean("first_run", false);
        return true;
    }

    // Standard settings
    public Boolean getSpeedPreferences(Boolean... fromControl) {
        return getBoolean(R.string.speed_preferences, false, settingsType(R.string.speed_preferences), fromControl);
    }

    public void setSpeedPreferences(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.speed_preferences, newValue, settingsType(R.string.speed_preferences), fromControl);
    }

    public Boolean getLogPreferences(Boolean... fromControl) {
        return getBoolean(R.string.log_preferences, false, settingsType(R.string.log_preferences), fromControl);
    }

    public void setLogPreferences(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.log_preferences, newValue, settingsType(R.string.log_preferences), fromControl);
    }

    public Boolean getAlarmPreferences(Boolean... fromControl) {
        return getBoolean(R.string.alarm_preferences, false, settingsType(R.string.alarm_preferences), fromControl);
    }

    public void setAlarmPreferences(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.alarm_preferences, newValue, settingsType(R.string.alarm_preferences), fromControl);
    }

    public Boolean getWatchPreferences(Boolean... fromControl) {
        return getBoolean(R.string.watch_preferences, false, settingsType(R.string.watch_preferences), fromControl);
    }

    public void setWatchPreferences(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.watch_preferences, newValue, settingsType(R.string.watch_preferences), fromControl);
    }

    public Boolean getWheelSettings(Boolean... fromControl) {
        return getBoolean(R.string.wheel_settings, false, settingsType(R.string.wheel_settings), fromControl);
    }

    public void setWheelSettings(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.wheel_settings, newValue, settingsType(R.string.wheel_settings), fromControl);
    }

    public Boolean getTripSettings(Boolean... fromControl) {
        return getBoolean(R.string.trip_settings, false, settingsType(R.string.trip_settings), fromControl);
    }

    public void setTripSettings(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.trip_settings, newValue, settingsType(R.string.trip_settings), fromControl);
    }

    public Boolean getAlarmsEnabled(Boolean... fromControl) {
        return getBoolean(R.string.alarms_enabled, false, settingsType(R.string.alarms_enabled), fromControl);
    }

    public void setAlarmsEnabled(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.alarms_enabled, newValue, settingsType(R.string.alarms_enabled), fromControl);
    }

    public Boolean getDisablePhoneVibrate(Boolean... fromControl) {
        return getBoolean(R.string.disable_phone_vibrate, false, settingsType(R.string.disable_phone_vibrate), fromControl);
    }

    public void setDisablePhoneVibrate(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.disable_phone_vibrate, newValue, settingsType(R.string.disable_phone_vibrate), fromControl);
    }

    public Boolean getDisablePhoneBeep(Boolean... fromControl) {
        return getBoolean(R.string.disable_phone_beep, false, settingsType(R.string.disable_phone_beep), fromControl);
    }

    public void setDisablePhoneBeep(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.disable_phone_beep, newValue, settingsType(R.string.disable_phone_beep), fromControl);
    }

    public int getAlarm1Speed(Boolean... fromControl) {
        return getInt(R.string.alarm_1_speed, 29, settingsType(R.string.alarm_1_speed), fromControl) * 100;
    }

    public void setAlarm1Speed(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_1_speed, newValue / 100, settingsType(R.string.alarm_1_speed), fromControl);
    }

    public int getAlarm1Battery(Boolean... fromControl) {
        return getInt(R.string.alarm_1_battery, 100, settingsType(R.string.alarm_1_battery), fromControl);
    }

    public void setAlarm1Battery(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_1_battery, newValue, settingsType(R.string.alarm_1_battery), fromControl);
    }

    public int getAlarm2Speed(Boolean... fromControl) {
        return getInt(R.string.alarm_2_speed, 0, settingsType(R.string.alarm_2_speed), fromControl) * 100;
    }

    public void setAlarm2Speed(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_2_speed, newValue / 100, settingsType(R.string.alarm_2_speed), fromControl);
    }

    public int getAlarm2Battery(Boolean... fromControl) {
        return getInt(R.string.alarm_2_battery, 0, settingsType(R.string.alarm_2_battery), fromControl);
    }

    public void setAlarm2Battery(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_2_battery, newValue, settingsType(R.string.alarm_2_battery), fromControl);
    }

    public int getAlarm3Speed(Boolean... fromControl) {
        return getInt(R.string.alarm_3_speed, 0, settingsType(R.string.alarm_3_speed), fromControl) * 100;
    }

    public void setAlarm3Speed(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_3_speed, newValue / 100, settingsType(R.string.alarm_3_speed), fromControl);
    }

    public int getAlarm3Battery(Boolean... fromControl) {
        return getInt(R.string.alarm_3_battery, 0, settingsType(R.string.alarm_3_battery), fromControl);
    }

    public void setAlarm3Battery(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_3_battery, newValue, settingsType(R.string.alarm_3_battery), fromControl);
    }

    public int getAlarmCurrent(Boolean... fromControl) {
        return getInt(R.string.alarm_current, 0, settingsType(R.string.alarm_current), fromControl) * 100;
    }

    public void setAlarmCurrent(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_current, newValue / 100, settingsType(R.string.alarm_current), fromControl);
    }

    public int getAlarmTemperature(Boolean... fromControl) {
        return getInt(R.string.alarm_temperature, 0, settingsType(R.string.alarm_temperature), fromControl) * 100;
    }

    public void setAlarmTemperature(int newValue, Boolean... fromControl) {
        setInt(R.string.alarm_temperature, newValue / 100, settingsType(R.string.alarm_temperature), fromControl);
    }

    public Boolean getAlteredAlarms(Boolean... fromControl) {
        return getBoolean(R.string.altered_alarms, false, settingsType(R.string.altered_alarms), fromControl);
    }

    public void setAlteredAlarms(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.altered_alarms, newValue, settingsType(R.string.altered_alarms), fromControl);
    }

    public double getRotationSpeed(Boolean... fromControl) {
        return getInt(R.string.rotation_speed, 500, settingsType(R.string.rotation_speed), fromControl) / 10.0;
    }

    public void setRotationSpeed(double newValue, Boolean... fromControl) {
        setInt(R.string.rotation_speed, (int)(newValue * 10), settingsType(R.string.rotation_speed), fromControl);
    }

    public double getRotationVoltage(Boolean... fromControl) {
        return getInt(R.string.rotation_voltage, 840, settingsType(R.string.rotation_voltage), fromControl) / 10.0;
    }

    public void setRotationVoltage(double newValue, Boolean... fromControl) {
        setInt(R.string.rotation_voltage, (int)(newValue * 10), settingsType(R.string.rotation_voltage), fromControl);
    }

    public double getPowerFactor(Boolean... fromControl) {
        return getInt(R.string.power_factor, 90, settingsType(R.string.power_factor), fromControl) / 100.0;
    }

    public void setPowerFactor(double newValue, Boolean... fromControl) {
        setInt(R.string.power_factor, (int)(newValue * 100), settingsType(R.string.power_factor), fromControl);
    }

    public double getAlarmFactor1(Boolean... fromControl) {
        return getInt(R.string.alarm_factor1, 80, settingsType(R.string.alarm_factor1), fromControl) / 100.0;
    }

    public void setAlarmFactor1(double newValue, Boolean... fromControl) {
        setInt(R.string.alarm_factor1, (int)(newValue * 100), settingsType(R.string.alarm_factor1), fromControl);
    }

    public double getAlarmFactor2(Boolean... fromControl) {
        return getInt(R.string.alarm_factor2, 90, settingsType(R.string.alarm_factor2), fromControl) / 100.0;
    }

    public void setAlarmFactor2(double newValue, Boolean... fromControl) {
        setInt(R.string.alarm_factor2, (int)(newValue * 100), settingsType(R.string.alarm_factor2), fromControl);
    }

    public double getAlarmFactor3(Boolean... fromControl) {
        return getInt(R.string.alarm_factor3, 95, settingsType(R.string.alarm_factor3), fromControl) / 100.0;
    }

    public void setAlarmFactor3(double newValue, Boolean... fromControl) {
        setInt(R.string.alarm_factor3, (int)(newValue * 100), settingsType(R.string.alarm_factor3), fromControl);
    }

    public int getWarningSpeed(Boolean... fromControl) {
        return getInt(R.string.warning_speed, 0, settingsType(R.string.warning_speed), fromControl);
    }

    public void setWarningSpeed(int newValue, Boolean... fromControl) {
        setInt(R.string.warning_speed, newValue, settingsType(R.string.warning_speed), fromControl);
    }

    public double getWarningPwm(Boolean... fromControl) {
        return getInt(R.string.warning_pwm, 0, settingsType(R.string.warning_pwm), fromControl) / 100.0;
    }

    public void setWarningPwm(double newValue, Boolean... fromControl) {
        setInt(R.string.warning_pwm, (int)(newValue * 100), settingsType(R.string.warning_pwm), fromControl);
    }

    public int getWarningSpeedPeriod(Boolean... fromControl) {
        return getInt(R.string.warning_speed_period, 0, settingsType(R.string.warning_speed_period), fromControl) * 1000;
    }

    public void setWarningSpeedPeriod(int newValue, Boolean... fromControl) {
        setInt(R.string.warning_speed_period, newValue / 1000, settingsType(R.string.warning_speed_period), fromControl);
    }

    public Boolean getUseShortPwm(Boolean... fromControl) {
        return getBoolean(R.string.use_short_pwm, false, settingsType(R.string.use_short_pwm), fromControl);
    }

    public void setUseShortPwm(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_short_pwm, newValue, settingsType(R.string.use_short_pwm), fromControl);
    }

    public Boolean getLightEnabled(Boolean... fromControl) {
        return getBoolean(R.string.light_enabled, false, settingsType(R.string.light_enabled), fromControl);
    }

    public void setLightEnabled(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.light_enabled, newValue, settingsType(R.string.light_enabled), fromControl);
    }

    public Boolean getLedEnabled(Boolean... fromControl) {
        return getBoolean(R.string.led_enabled, false, settingsType(R.string.led_enabled), fromControl);
    }

    public void setLedEnabled(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.led_enabled, newValue, settingsType(R.string.led_enabled), fromControl);
    }

    public Boolean getHandleButtonDisabled(Boolean... fromControl) {
        return getBoolean(R.string.handle_button_disabled, false, settingsType(R.string.handle_button_disabled), fromControl);
    }

    public void setHandleButtonDisabled(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.handle_button_disabled, newValue, settingsType(R.string.handle_button_disabled), fromControl);
    }

    public int getWheelMaxSpeed(Boolean... fromControl) {
        return getInt(R.string.wheel_max_speed, 0, settingsType(R.string.wheel_max_speed), fromControl);
    }

    public void setWheelMaxSpeed(int newValue, Boolean... fromControl) {
        setInt(R.string.wheel_max_speed, newValue, settingsType(R.string.wheel_max_speed), fromControl);
    }

    public int getSpeakerVolume(Boolean... fromControl) {
        return getInt(R.string.speaker_volume, 0, settingsType(R.string.speaker_volume), fromControl);
    }

    public void setSpeakerVolume(int newValue, Boolean... fromControl) {
        setInt(R.string.speaker_volume, newValue, settingsType(R.string.speaker_volume), fromControl);
    }

    public int getPedalsAdjustment(Boolean... fromControl) {
        return getInt(R.string.pedals_adjustment, 0, settingsType(R.string.pedals_adjustment), fromControl);
    }

    public void setPedalsAdjustment(int newValue, Boolean... fromControl) {
        setInt(R.string.pedals_adjustment, newValue, settingsType(R.string.pedals_adjustment), fromControl);
    }

    public int getPedalsMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.pedals_mode, "0", settingsType(R.string.pedals_mode), fromControl));
    }

    public void setPedalsMode(int newValue, Boolean... fromControl) {
        setString(R.string.pedals_mode, Integer.toString(newValue), settingsType(R.string.pedals_mode), fromControl);
    }

    public int getLightMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.light_mode, "0", settingsType(R.string.light_mode), fromControl));
    }

    public void setLightMode(int newValue, Boolean... fromControl) {
        setString(R.string.light_mode, Integer.toString(newValue), settingsType(R.string.light_mode), fromControl);
    }

    public int getStrobeMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.strobe_mode, "0", settingsType(R.string.strobe_mode), fromControl));
    }

    public void setStrobeMode(int newValue, Boolean... fromControl) {
        setString(R.string.strobe_mode, Integer.toString(newValue), settingsType(R.string.strobe_mode), fromControl);
    }

    public int getLedMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.led_mode, "0", settingsType(R.string.led_mode), fromControl));
    }

    public void setLedMode(int newValue, Boolean... fromControl) {
        setString(R.string.led_mode, Integer.toString(newValue), settingsType(R.string.led_mode), fromControl);
    }

    public int getAlarmMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.alarm_mode, "0", settingsType(R.string.alarm_mode), fromControl));
    }

    public void setAlarmMode(int newValue, Boolean... fromControl) {
        setString(R.string.alarm_mode, Integer.toString(newValue), settingsType(R.string.alarm_mode), fromControl);
    }

    public int getGotwayVoltage(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.gotway_voltage, "1", settingsType(R.string.gotway_voltage), fromControl));
    }

    public void setGotwayVoltage(int newValue, Boolean... fromControl) {
        setString(R.string.gotway_voltage, Integer.toString(newValue), settingsType(R.string.gotway_voltage), fromControl);
    }

    public int getGotwayNegative(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.gotway_negative, "0", settingsType(R.string.gotway_negative), fromControl));
    }

    public void setGotwayNegative(int newValue, Boolean... fromControl) {
        setString(R.string.gotway_negative, Integer.toString(newValue), settingsType(R.string.gotway_negative), fromControl);
    }

    public int getWheelKsAlarm3(Boolean... fromControl) {
        return getInt(R.string.wheel_ks_alarm3, 0, settingsType(R.string.wheel_ks_alarm3), fromControl);
    }

    public void setWheelKsAlarm3(int newValue, Boolean... fromControl) {
        setInt(R.string.wheel_ks_alarm3, newValue, settingsType(R.string.wheel_ks_alarm3), fromControl);
    }

    public int getWheelKsAlarm2(Boolean... fromControl) {
        return getInt(R.string.wheel_ks_alarm2, 0, settingsType(R.string.wheel_ks_alarm2), fromControl);
    }

    public void setWheelKsAlarm2(int newValue, Boolean... fromControl) {
        setInt(R.string.wheel_ks_alarm2, newValue, settingsType(R.string.wheel_ks_alarm2), fromControl);
    }

    public int getWheelKsAlarm1(Boolean... fromControl) {
        return getInt(R.string.wheel_ks_alarm1, 0, settingsType(R.string.wheel_ks_alarm1), fromControl);
    }

    public void setWheelKsAlarm1(int newValue, Boolean... fromControl) {
        setInt(R.string.wheel_ks_alarm1, newValue, settingsType(R.string.wheel_ks_alarm1), fromControl);
    }

    public Boolean getAutoLog(Boolean... fromControl) {
        return getBoolean(R.string.auto_log, false, settingsType(R.string.auto_log), fromControl);
    }

    public void setAutoLog(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.auto_log, newValue, settingsType(R.string.auto_log), fromControl);
    }

    public Boolean getAutoUploadEc(Boolean... fromControl) {
        return getBoolean(R.string.auto_upload_ec, false, settingsType(R.string.auto_upload_ec), fromControl);
    }

    public void setAutoUploadEc(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.auto_upload_ec, newValue, settingsType(R.string.auto_upload_ec), fromControl);
    }

    public String getEcToken(Boolean... fromControl) {
        return getString(R.string.ec_token, null, settingsType(R.string.ec_token), fromControl);
    }

    public void setEcToken(String newValue, Boolean... fromControl) {
        setString(R.string.ec_token, newValue, settingsType(R.string.ec_token), fromControl);
    }

    public String getEcUserId(Boolean... fromControl) {
        return getString(R.string.ec_user_id, null, settingsType(R.string.ec_user_id), fromControl);
    }

    public void setEcUserId(String newValue, Boolean... fromControl) {
        setString(R.string.ec_user_id, newValue, settingsType(R.string.ec_user_id), fromControl);
    }

    public Boolean getAutoUpload(Boolean... fromControl) {
        return getBoolean(R.string.auto_upload, false, settingsType(R.string.auto_upload), fromControl);
    }

    public void setAutoUpload(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.auto_upload, newValue, settingsType(R.string.auto_upload), fromControl);
    }

    public Boolean getLogLocationData(Boolean... fromControl) {
        return getBoolean(R.string.log_location_data, false, settingsType(R.string.log_location_data), fromControl);
    }

    public void setLogLocationData(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.log_location_data, newValue, settingsType(R.string.log_location_data), fromControl);
    }

    public Boolean getUseGps(Boolean... fromControl) {
        return getBoolean(R.string.use_gps, false, settingsType(R.string.use_gps), fromControl);
    }

    public void setUseGps(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_gps, newValue, settingsType(R.string.use_gps), fromControl);
    }

    public Boolean getUseMph(Boolean... fromControl) {
        return getBoolean(R.string.use_mph, false, settingsType(R.string.use_mph), fromControl);
    }

    public void setUseMph(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_mph, newValue, settingsType(R.string.use_mph), fromControl);
    }

    public Boolean getUseEng(Boolean... fromControl) {
        return getBoolean(R.string.use_eng, false, settingsType(R.string.use_eng), fromControl);
    }

    public void setUseEng(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_eng, newValue, settingsType(R.string.use_eng), fromControl);
    }

    public Boolean getUseBetterPercents(Boolean... fromControl) {
        return getBoolean(R.string.use_better_percents, false, settingsType(R.string.use_better_percents), fromControl);
    }

    public void setUseBetterPercents(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_better_percents, newValue, settingsType(R.string.use_better_percents), fromControl);
    }

    public Boolean getFixedPercents(Boolean... fromControl) {
        return getBoolean(R.string.fixed_percents, false, settingsType(R.string.fixed_percents), fromControl);
    }

    public void setFixedPercents(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.fixed_percents, newValue, settingsType(R.string.fixed_percents), fromControl);
    }

    public double getTiltbackVoltage(Boolean... fromControl) {
        return getInt(R.string.tiltback_voltage, 660, settingsType(R.string.tiltback_voltage), fromControl) / 10.0;
    }

    public void setTiltbackVoltage(double newValue, Boolean... fromControl) {
        setInt(R.string.tiltback_voltage, (int)(newValue * 10), settingsType(R.string.tiltback_voltage), fromControl);
    }

    public Boolean getUsePipMode(Boolean... fromControl) {
        return getBoolean(R.string.use_pip_mode, false, settingsType(R.string.use_pip_mode), fromControl);
    }

    public void setUsePipMode(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_pip_mode, newValue, settingsType(R.string.use_pip_mode), fromControl);
    }

    public Boolean getCurrentOnDial(Boolean... fromControl) {
        return getBoolean(R.string.current_on_dial, false, settingsType(R.string.current_on_dial), fromControl);
    }

    public void setCurrentOnDial(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.current_on_dial, newValue, settingsType(R.string.current_on_dial), fromControl);
    }

    public Boolean getConnectionSound(Boolean... fromControl) {
        return getBoolean(R.string.connection_sound, false, settingsType(R.string.connection_sound), fromControl);
    }

    public void setConnectionSound(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.connection_sound, newValue, settingsType(R.string.connection_sound), fromControl);
    }

    public int getNoConnectionSound(Boolean... fromControl) {
        return getInt(R.string.no_connection_sound, 0, settingsType(R.string.no_connection_sound), fromControl) * 1000;
    }

    public void setNoConnectionSound(int newValue, Boolean... fromControl) {
        setInt(R.string.no_connection_sound, newValue / 1000, settingsType(R.string.no_connection_sound), fromControl);
    }

    public Boolean getUseStopMusic(Boolean... fromControl) {
        return getBoolean(R.string.use_stop_music, false, settingsType(R.string.use_stop_music), fromControl);
    }

    public void setUseStopMusic(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_stop_music, newValue, settingsType(R.string.use_stop_music), fromControl);
    }

    public int getBatteryCapacity(Boolean... fromControl) {
        return getInt(R.string.battery_capacity, 0, settingsType(R.string.battery_capacity), fromControl);
    }

    public void setBatteryCapacity(int newValue, Boolean... fromControl) {
        setInt(R.string.battery_capacity, newValue, settingsType(R.string.battery_capacity), fromControl);
    }

    public double getChargingPower(Boolean... fromControl) {
        return getInt(R.string.charging_power, 0, settingsType(R.string.charging_power), fromControl) / 10.0;
    }

    public void setChargingPower(double newValue, Boolean... fromControl) {
        setInt(R.string.charging_power, (int)(newValue * 10), settingsType(R.string.charging_power), fromControl);
    }

    public Boolean getConnectBeep(Boolean... fromControl) {
        return getBoolean(R.string.connect_beep, true, settingsType(R.string.connect_beep), fromControl);
    }

    public void setConnectBeep(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.connect_beep, newValue, settingsType(R.string.connect_beep), fromControl);
    }

    public int getMaxSpeed(Boolean... fromControl) {
        return getInt(R.string.max_speed, 30, settingsType(R.string.max_speed), fromControl) * 10;
    }

    public void setMaxSpeed(int newValue, Boolean... fromControl) {
        setInt(R.string.max_speed, newValue / 10, settingsType(R.string.max_speed), fromControl);
    }

    public Boolean getUseRatio(Boolean... fromControl) {
        return getBoolean(R.string.use_ratio, false, settingsType(R.string.use_ratio), fromControl);
    }

    public void setUseRatio(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.use_ratio, newValue, settingsType(R.string.use_ratio), fromControl);
    }

    public Boolean getKs18LScaler(Boolean... fromControl) {
        return getBoolean(R.string.ks18l_scaler, false, settingsType(R.string.ks18l_scaler), fromControl);
    }

    public void setKs18LScaler(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.ks18l_scaler, newValue, settingsType(R.string.ks18l_scaler), fromControl);
    }

    public int getHornMode(Boolean... fromControl) {
        return Integer.parseInt(getString(R.string.horn_mode, "0", settingsType(R.string.horn_mode), fromControl));
    }

    public void setHornMode(int newValue, Boolean... fromControl) {
        setString(R.string.horn_mode, Integer.toString(newValue), settingsType(R.string.horn_mode), fromControl);
    }

    public Boolean getGarminConnectIqEnable(Boolean... fromControl) {
        return getBoolean(R.string.garmin_connectiq_enable, false, settingsType(R.string.garmin_connectiq_enable), fromControl);
    }

    public void setGarminConnectiqEnable(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.garmin_connectiq_enable, newValue, settingsType(R.string.garmin_connectiq_enable), fromControl);
    }

    public Set<String> getViewBlocks(Boolean... fromControl) {
        return getStringSet(R.string.view_blocks, null, settingsType(R.string.view_blocks), fromControl);
    }

    public void setViewBlocks(Set<String> newValue, Boolean... fromControl) {
        setStringSet(R.string.view_blocks, newValue, settingsType(R.string.view_blocks), fromControl);
    }

    public String getLastMac(Boolean... fromControl) {
        return getString(R.string.last_mac, "", settingsType(R.string.last_mac), fromControl);
    }

    public void setLastMac(String newValue, Boolean... fromControl) {
        setString(R.string.last_mac, newValue, settingsType(R.string.last_mac), fromControl);
    }

    public String getProfileName(Boolean... fromControl) {
        return getString(R.string.profile_name, null, settingsType(R.string.profile_name), fromControl);
    }

    public void setProfileName(String newValue, Boolean... fromControl) {
        setString(R.string.profile_name, newValue, settingsType(R.string.profile_name), fromControl);
    }

    public Boolean getShowUnknownDevices(Boolean... fromControl) {
        return getBoolean(R.string.show_unknown_devices, false, settingsType(R.string.show_unknown_devices), fromControl);
    }

    public void setShowUnknownDevices(Boolean newValue, Boolean... fromControl) {
        setBoolean(R.string.show_unknown_devices, newValue, settingsType(R.string.show_unknown_devices), fromControl);
    }

    private SettingsType settingsType(int resId) {
        return controlSettings.get(getControlKey(resId));
    }
}