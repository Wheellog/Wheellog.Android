package com.cooper.wheellog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.cooper.wheellog.presentation.preferences.SeekBarPreference;
import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.KingsongAdapter;

import java.util.Map;

import timber.log.Timber;

public class MainPreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, IDataListener {
    private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private boolean mDataWarningDisplayed = false;
    private SettingsScreen currentScreen = SettingsScreen.Main;

    @Override
    public void changeWheelType() {
        mWheelType = WheelData.getInstance().getWheelType();
        switchSpecificSettings(WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        WheelData.getInstance().addListener(this);

        // Reset ElectroClub settings
        ElectroClub.getInstance().setUserToken(WheelLog.AppConfig.getEcToken());
        ElectroClub.getInstance().setUserId(WheelLog.AppConfig.getEcUserId());
        if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() == null) {
            WheelLog.AppConfig.setAutoUploadEc(false, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setupScreen();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        WheelLog.AppConfig.switchSettingSpecific(key, true);
        final Context context = getContext();
        if (context == null)
            return;

        // Add a check for control migration when setting a value on wheels
        // Through WheelLog.AppConfig.getIsInProgressControlsMigration() method
        switch (WheelLog.AppConfig.getResId(key)) {
            case R.string.ec_token:
                ElectroClub.getInstance().setUserToken(WheelLog.AppConfig.getEcToken());
                break;
            case R.string.ec_user_id:
                ElectroClub.getInstance().setUserId(WheelLog.AppConfig.getEcUserId());
                break;
            case R.string.connection_sound:
                hideShowSeekBarsApp();
                break;
            case R.string.alarms_enabled:
            case R.string.altered_alarms:
                hideShowSeekBarsAlarms();
                break;
            case R.string.auto_upload_ec:
                if (WheelLog.AppConfig.getAutoUploadEc() && !mDataWarningDisplayed) {
                    WheelLog.AppConfig.setAutoUploadEc(false, true);
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.enable_auto_upload_title))
                            .setMessage(getString(R.string.enable_auto_upload_descriprion))
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                mDataWarningDisplayed = true;
                                WheelLog.AppConfig.setAutoUploadEc(true, true);
                                refreshVolatileSettings();
                            })
                            .setNegativeButton(android.R.string.no, (dialog, which) -> {
                                mDataWarningDisplayed = false;
                                refreshVolatileSettings();
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                } else
                    mDataWarningDisplayed = false;
                break;
            case R.string.max_speed:
            case R.string.use_mph:
                context.sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
            case R.string.use_eng:
                new AlertDialog.Builder(context)
                        .setTitle(R.string.use_eng_alert_title)
                        .setMessage(R.string.use_eng_alert_description)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> { })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
                break;
            case R.string.light_enabled:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateLight(WheelLog.AppConfig.getLightEnabled());
                break;
            case R.string.led_enabled:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateLed(WheelLog.AppConfig.getLedEnabled());
                break;
            case R.string.handle_button_disabled:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateHandleButton(WheelLog.AppConfig.getHandleButtonDisabled());
                break;
            case R.string.wheel_max_speed:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateMaxSpeed(WheelLog.AppConfig.getWheelMaxSpeed());
                break;
            case R.string.speaker_volume:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateSpeakerVolume(WheelLog.AppConfig.getSpeakerVolume());
                break;
            case R.string.pedals_adjustment:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updatePedals(WheelLog.AppConfig.getPedalsAdjustment());
                break;
            case R.string.pedals_mode:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updatePedalsMode(WheelLog.AppConfig.getPedalsMode());
                break;
            case R.string.light_mode:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateLightMode(WheelLog.AppConfig.getLightMode());
                break;
            case R.string.alarm_mode:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateAlarmMode(WheelLog.AppConfig.getAlarmMode());
                break;
            case R.string.strobe_mode:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateStrobe(WheelLog.AppConfig.getStrobeMode());
                break;
            case R.string.led_mode:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    WheelData.getInstance().updateLedMode(WheelLog.AppConfig.getLedMode());
                break;
            case R.string.wheel_ks_alarm3:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    KingsongAdapter.getInstance().updateKSAlarm3(WheelLog.AppConfig.getWheelKsAlarm3());
                break;
            case R.string.wheel_ks_alarm2:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    KingsongAdapter.getInstance().updateKSAlarm2(WheelLog.AppConfig.getWheelKsAlarm2());
                break;
            case R.string.wheel_ks_alarm1:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    KingsongAdapter.getInstance().updateKSAlarm1(WheelLog.AppConfig.getWheelKsAlarm1());
                break;
            case R.string.ks18l_scaler:
                if (!WheelLog.AppConfig.getIsInProgressControlsMigration())
                    KingsongAdapter.getInstance().set18Lkm(WheelLog.AppConfig.getKs18LScaler());
                break;
            case R.string.current_on_dial:
                Timber.i("Change dial type to %b", WheelLog.AppConfig.getCurrentOnDial());
                break;
        }

        if (WheelLog.AppConfig.getControlSettings().containsKey(key) && !WheelLog.AppConfig.getIsInProgressControlsMigration()) {
            Intent intent = new Intent(Constants.ACTION_PREFERENCE_CHANGED);
            intent.putExtra(Constants.INTENT_EXTRA_SETTINGS_KEY, key);
            context.sendBroadcast(intent);
        }
    }

    private void setupScreen() {
        final FragmentActivity activity = getActivity();
        if (activity == null)
            return;

        Toolbar tb = activity.findViewById(R.id.preference_toolbar);
        if (currentScreen == SettingsScreen.Main) {
            tb.setNavigationIcon(null);
        } else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            tb.setNavigationOnClickListener(view -> showMainMenu());
        }

        switch (currentScreen) {
            case Main:
                tb.setTitle(getText(R.string.settings_title));
                Preference speedButton = findPreference(getString(R.string.speed_preferences));
                Preference logsButton = findPreference(getString(R.string.log_preferences));
                Preference alarmButton = findPreference(getString(R.string.alarm_preferences));
                Preference watchButton = findPreference(getString(R.string.watch_preferences));
				Preference wheelButton = findPreference(getString(R.string.wheel_settings));
				Preference resetTopButton = findPreference(getString(R.string.reset_top_speed));
                Preference resetLowestBatteryButton = findPreference(getString(R.string.reset_lowest_battery));
				Preference resetUserDistanceButton = findPreference(getString(R.string.reset_user_distance));
                Preference lastMacButton = findPreference(getString(R.string.last_mac));
                Preference profileNameButton = findPreference(getString(R.string.profile_name));
                Preference aboutButton = findPreference(getString(R.string.about));

                if (speedButton != null) {
                    speedButton.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Speed;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_speed);
                        setupScreen();
                        return true;
                    });
                }
                if (logsButton != null) {
                    logsButton.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Logs;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_logs);
                        setupScreen();
                        return true;
                    });
                }
                if (alarmButton != null) {
                    alarmButton.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Alarms;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_alarms);
                        setupScreen();
                        return true;
                    });
                }
                if (watchButton != null) {
                    watchButton.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Watch;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_watch);
                        setupScreen();
                        return true;
                    });
                }

                if (wheelButton != null) {
                    wheelButton.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Wheel;
                        getPreferenceScreen().removeAll();
                        switch (mWheelType) {
                            case NINEBOT_Z:
                                addPreferencesFromResource(R.xml.preferences_ninebot_z);
                                break;
                            case INMOTION:
                                addPreferencesFromResource(R.xml.preferences_inmotion);
                                break;
                            case KINGSONG:
                                addPreferencesFromResource(R.xml.preferences_kingsong);
                                KingsongAdapter adapter = KingsongAdapter.getInstance();
                                if (adapter.getKSAlertsAndSpeedupdated()) {
                                    correctWheelBarState(getString(R.string.wheel_max_speed), WheelData.getInstance().getWheelMaxSpeed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm1), WheelLog.AppConfig.getWheelKsAlarm1());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm2), WheelLog.AppConfig.getWheelKsAlarm2());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm3), WheelLog.AppConfig.getWheelKsAlarm3());
                                }
                                break;
                            case GOTWAY:
                                addPreferencesFromResource(R.xml.preferences_gotway);
                                Preference startCalibrationButton = findPreference(getString(R.string.start_calibration));
                                if (startCalibrationButton != null) {
                                    startCalibrationButton.setOnPreferenceClickListener(preference1 -> {
                                        WheelData.getInstance().updateCalibration();
                                        return true;
                                    });
                                }
                                break;
                            case VETERAN:
                                addPreferencesFromResource(R.xml.preferences_veteran);
                                break;
                        }
                        setupScreen();
                        return true;
                    });
                }

				if (resetTopButton != null) {
                    resetTopButton.setOnPreferenceClickListener(preference -> {
                        WheelData.getInstance().resetTopSpeed();
                        return true;
                    });
                }

                if (resetLowestBatteryButton != null) {
                    resetLowestBatteryButton.setOnPreferenceClickListener(preference -> {
                        WheelData.getInstance().resetVoltageSag();
                        activity.sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_RESET));
                        return true;
                    });
                }
				if (resetUserDistanceButton != null) {
                    resetUserDistanceButton.setOnPreferenceClickListener(preference -> {
                        WheelData.getInstance().resetUserDistance();
                        return true;
                    });
                }
                if (aboutButton != null) {
                    aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            String versionName = BuildConfig.VERSION_NAME;
                            String buildTime = BuildConfig.BUILD_TIME;
                            new AlertDialog.Builder(activity)
                                    .setTitle(R.string.about_app_title)
                                    .setMessage(Html.fromHtml(String.format("Version %s <br>build at %s <br>by <i>Palachzzz</i> <br><a href=\"palachzzz.wl@gmail.com\">palachzzz.wl@gmail.com</a> <br> Thanks to:<br>JumpMaster - project initiator<br>cedbossneo - Inmotion support<br>juliomap - Tizen support<br>MacPara - some improvements<br>datarsoja - KS alerts<br>and others!", versionName, buildTime)))
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> { })
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show();
                            return true;
                        }
                    });
                }

                if (lastMacButton != null) {
                    lastMacButton.setOnPreferenceClickListener(preference -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(getText(R.string.edit_mac_addr_title));

                        final EditText input = new EditText(activity);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(WheelLog.AppConfig.getLastMac());
                        builder.setView(input);
                        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            final String deviceAddress = input.getText().toString();
                            WheelLog.AppConfig.setLastMac(deviceAddress, true);
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                            builder1.setTitle(getText(R.string.wheel_pass_imotion));

                            final EditText input1 = new EditText(activity);
                            input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            builder1.setView(input1);
                            builder1.setPositiveButton(android.R.string.ok, (dialog12, which12) -> {
                                String password = input1.getText().toString();
                                WheelLog.AppConfig.setPasswordForWheel(deviceAddress, password);
                            });
                            builder1.setNegativeButton(android.R.string.cancel, (dialog1, which1) -> dialog1.cancel());
                            builder1.show();
                        });
                        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                        builder.show();
                        return true;
                    });
                }

                if (profileNameButton != null) {
                    profileNameButton.setOnPreferenceClickListener(preference -> {
                        if (WheelLog.AppConfig.isGeneral())
                            return false;

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(getText(R.string.profile_name_title));
                        String profileName = WheelLog.AppConfig.getProfileName();
                        final EditText input = new EditText(activity);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(profileName == null ? "" : profileName);
                        builder.setView(input);
                        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            final String newProfileName = input.getText().toString();
                            WheelLog.AppConfig.setProfileName(newProfileName, true);
                        });
                        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
                        builder.show();
                        return true;
                    });
                }
                break;
            case Speed:
                tb.setTitle(getText(R.string.speed_settings_title));
                hideShowSeekBarsApp();
                break;
            case Logs:
                tb.setTitle(getText(R.string.logs_settings_title));
                break;
            case Alarms:
                tb.setTitle(getText(R.string.alarm_settings_title));
                hideShowSeekBarsAlarms();
                break;
            case Watch:
                tb.setTitle(getText(R.string.watch_settings_title));
                break;
			case Wheel:
                tb.setTitle(getText(R.string.wheel_settings_title));
                break;
        }

        switchSpecificSettings(WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown);
    }

    void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log));
            correctCheckState(getString(R.string.log_location_data));
            correctCheckState(getString(R.string.auto_upload));
            correctCheckState(getString(R.string.auto_upload_ec));
        }
    }
	
    void refreshWheelSettings(boolean isLight, boolean isLed, boolean isButton, int maxSpeed, int speakerVolume, int pedals) {
        correctWheelCheckState(getString(R.string.light_enabled), isLight);
        correctWheelCheckState(getString(R.string.led_enabled), isLed);
        correctWheelCheckState(getString(R.string.handle_button_disabled), isButton);
        correctWheelBarState(getString(R.string.wheel_max_speed), maxSpeed);
		correctWheelBarState(getString(R.string.speaker_volume), speakerVolume);
		correctWheelBarState(getString(R.string.pedals_adjustment), pedals);
    }

    boolean isMainMenu() {
        return currentScreen == SettingsScreen.Main;
    }

    void showMainMenu() {
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);

        Preference wheelButton = findPreference(getString(R.string.wheel_settings));
        if (wheelButton == null) {
            return;
        }

        if ((mWheelType == WHEEL_TYPE.INMOTION || mWheelType == WHEEL_TYPE.KINGSONG || mWheelType == WHEEL_TYPE.GOTWAY || mWheelType == WHEEL_TYPE.NINEBOT_Z || mWheelType == WHEEL_TYPE.VETERAN))
            wheelButton.setEnabled(true);

        currentScreen = SettingsScreen.Main;
        setupScreen();
    }

	private void correctWheelCheckState(String preference, boolean state) {
        CheckBoxPreference cbPreference = findPreference(preference);
        if (cbPreference == null)
            return;

        boolean checkState = cbPreference.isChecked();
        if (state != checkState)
            cbPreference.setChecked(state);
    }
	
	private void correctWheelBarState(String preference, int stateInt) {
        SeekBarPreference sbPreference = findPreference(preference);
		if (sbPreference == null)
			return;

		int sbValue = sbPreference.getValue();
		if (stateInt != sbValue) {
			sbPreference.setValue(stateInt);

			// Workaround, seekbar doesn't want to update view
            getPreferenceScreen().removeAll();

            if (mWheelType == WHEEL_TYPE.NINEBOT_Z) addPreferencesFromResource(R.xml.preferences_ninebot_z);
			if (mWheelType == WHEEL_TYPE.INMOTION) addPreferencesFromResource(R.xml.preferences_inmotion);
			if (mWheelType == WHEEL_TYPE.KINGSONG) addPreferencesFromResource(R.xml.preferences_kingsong);
			if (mWheelType == WHEEL_TYPE.GOTWAY) addPreferencesFromResource(R.xml.preferences_gotway);
			if (mWheelType == WHEEL_TYPE.VETERAN) addPreferencesFromResource(R.xml.preferences_veteran);
            setupScreen();
		}

		WheelLog.AppConfig.setIntByGeneralKey(preference, stateInt);
    }

    private void correctCheckState(String preference) {
        boolean settingState = WheelLog.AppConfig.isTrueSettings(preference);
        CheckBoxPreference checkBoxPreference = findPreference(preference);
        if (checkBoxPreference == null)
            return;

        boolean checkState = checkBoxPreference.isChecked();
        if (settingState != checkState)
            checkBoxPreference.setChecked(settingState);
    }

    private void hideShowSeekBarsAlarms() {
        boolean alarmsEnabled = WheelLog.AppConfig.getAlarmsEnabled();
        boolean alteredAlarms = WheelLog.AppConfig.getAlteredAlarms();
        String[] seekbarPreferencesNormal = {
                getString(R.string.alarm_1_speed),
                getString(R.string.alarm_2_speed),
                getString(R.string.alarm_3_speed),
                getString(R.string.alarm_1_battery),
                getString(R.string.alarm_2_battery),
                getString(R.string.alarm_3_battery),
                getString(R.string.alarm_current),
				getString(R.string.alarm_temperature)
        };

        String[] seekbarPreferencesAltered = {
                getString(R.string.rotation_speed),
                getString(R.string.rotation_voltage),
                getString(R.string.power_factor),
                getString(R.string.alarm_factor1),
                getString(R.string.alarm_factor2),
                getString(R.string.alarm_factor3),
                getString(R.string.warning_speed),
                getString(R.string.warning_pwm),
                getString(R.string.warning_speed_period),
                getString(R.string.use_short_pwm),
        };

        String[] seekbarPreferencesCommon = {
                getString(R.string.alarm_current),
                getString(R.string.alarm_temperature)
        };

        for (String preference : seekbarPreferencesNormal) {
            Preference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled && !alteredAlarms);
        }

        for (String preference : seekbarPreferencesAltered) {
            Preference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled && alteredAlarms);
        }

        for (String preference : seekbarPreferencesCommon) {
            Preference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled);
        }
    }

    private void hideShowSeekBarsApp() {
        boolean connectSound = WheelLog.AppConfig.getConnectionSound();
        String[] seekbarPreferences = {
                getString(R.string.no_connection_sound)
        };

        for (String preference : seekbarPreferences) {
            SeekBarPreference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(connectSound);
        }
    }

    private void switchSpecificSettings(Boolean isOn) {
        String[] specificPreferences = {
            getString(R.string.alarm_preferences),
            getString(R.string.wheel_settings),
            getString(R.string.reset_top_speed),
            getString(R.string.reset_lowest_battery),
            getString(R.string.reset_user_distance),
            getString(R.string.last_mac),
            getString(R.string.profile_name)
        };

        for (String preference : specificPreferences) {
            Preference pref = findPreference(preference);
            if (pref != null)
                pref.setVisible(isOn);
        }

        Map<String, AppConfigBase.SettingsType> controlSettings = WheelLog.AppConfig.getControlSettings();
        for (String key : controlSettings.keySet()) {
            if (controlSettings.get(key) == AppConfigBase.SettingsType.Specific) {
                Preference pref = findPreference(key);
                if (pref != null)
                    pref.setVisible(isOn);
            }
        }

        // Hide inaccessible settings for VoltageTiltbackUnsupported wheels
        if (WheelData.getInstance().isVoltageTiltbackUnsupported()) {
            String[] preferences = {
                getString(R.string.fixed_percents),
                getString(R.string.cell_voltage_tiltback),
                getString(R.string.battery_capacity),
                getString(R.string.charging_power),
            };

            for (String preference : preferences) {
                Preference pref = findPreference(preference);
                if (pref != null)
                    pref.setVisible(false);
            }
        }
    }

    private enum SettingsScreen {
        Main,
        Speed,
        Logs,
        Alarms,
        Watch,
        Wheel
    }
}