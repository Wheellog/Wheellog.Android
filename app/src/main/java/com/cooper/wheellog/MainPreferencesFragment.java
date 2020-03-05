package com.cooper.wheellog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.SettingsUtil;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

public class MainPreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

	private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private boolean mDataWarningDisplayed = false;
    private SettingsScreen currentScreen = SettingsScreen.Main;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
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
        final Context context = getContext();
        if (context == null) {
            return;
        }

        switch (key) {
            case "connection_sound":
                hideShowSeekBarsApp();
                break;
            case "alarms_enabled":
            case "altered_alarms":
                hideShowSeekBarsAlarms();
                break;
            case "auto_upload":
                if (SettingsUtil.isAutoUploadEnabled(getActivity()) && !mDataWarningDisplayed) {
                    SettingsUtil.setAutoUploadEnabled(getActivity(), false);
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.enable_auto_upload_title))  // ("Enable Auto Upload?")
                            .setMessage(getString(R.string.enable_auto_upload_descriprion))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = true;
                                    SettingsUtil.setAutoUploadEnabled(getActivity(), true);
                                    refreshVolatileSettings();
                                    context.sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = false;
                                    refreshVolatileSettings();
                                    context.sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                } else
                    mDataWarningDisplayed = false;
                break;
            case "max_speed":
            case "use_mph":
                context.sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
            case "use_eng":
                new AlertDialog.Builder(context)
                        .setTitle(R.string.use_eng_alert_title)
                        .setMessage(R.string.use_eng_alert_description)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
                break;
            case "light_enabled":
				boolean ligthEnabled = sharedPreferences.getBoolean(getString(R.string.light_enabled), false);
				WheelData.getInstance().updateLight(ligthEnabled);
				break;
            case "use_better_percents":
                boolean betterPercents = sharedPreferences.getBoolean(getString(R.string.use_better_percents), false);
                WheelData.getInstance().setBetterPercents(betterPercents);
                break;
			case "led_enabled":
				boolean ledEnabled = sharedPreferences.getBoolean(getString(R.string.led_enabled), false);
				WheelData.getInstance().updateLed(ledEnabled);
				break;
			case "handle_button_disabled":
				boolean handleButtonDisabled = sharedPreferences.getBoolean(getString(R.string.handle_button_disabled), false);
				WheelData.getInstance().updateHandleButton(handleButtonDisabled);
				break;
			case "wheel_max_speed":
				final int maxSpeed = sharedPreferences.getInt(getString(R.string.wheel_max_speed), 0);
				WheelData.getInstance().updateMaxSpeed(maxSpeed);
				break;
			case "speaker_volume":
				int speakerVolume = sharedPreferences.getInt(getString(R.string.speaker_volume), 0);
				WheelData.getInstance().updateSpeakerVolume(speakerVolume);
				break;
			case "pedals_adjustment":
				int pedalsAdjustment = sharedPreferences.getInt(getString(R.string.pedals_adjustment), 0);
				WheelData.getInstance().updatePedals(pedalsAdjustment);
				break;
			case "pedals_mode":
				int pedalsMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.pedals_mode), "0"));
				WheelData.getInstance().updatePedalsMode(pedalsMode);
				break;
			case "light_mode":
				int lightMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.light_mode), "0"));
				WheelData.getInstance().updateLightMode(lightMode);
				break;
			case "alarm_mode":
				int alarmMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.alarm_mode), "0"));
				WheelData.getInstance().updateAlarmMode(alarmMode);
				break;
			case "strobe_mode":
				int strobeMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.strobe_mode), "0"));
				WheelData.getInstance().updateStrobe(strobeMode);
				break;
			case "led_mode":
				int led_mode = Integer.parseInt(sharedPreferences.getString(getString(R.string.led_mode), "0"));
				WheelData.getInstance().updateLedMode(led_mode);
				break;
            case "wheel_ks_alarm3":
                final int alert3 = sharedPreferences.getInt("wheel_ks_alarm3", 0);
                WheelData.getInstance().updateKSAlarm3(alert3);
                break;
            case "wheel_ks_alarm2":
                final int alert2 = sharedPreferences.getInt("wheel_ks_alarm2", 0);
                WheelData.getInstance().updateKSAlarm2(alert2);
                break;
            case "wheel_ks_alarm1":
                final int alert1 = sharedPreferences.getInt("wheel_ks_alarm1", 0);
                WheelData.getInstance().updateKSAlarm1(alert1);
                break;
        }
        context.sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }

    private void setupScreen() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        Toolbar tb = activity.findViewById(R.id.preference_toolbar);
        if (currentScreen == SettingsScreen.Main)
            tb.setNavigationIcon(null);
        else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            tb.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMainMenu();
                }
            });
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
                Preference aboutButton = findPreference(getString(R.string.about));

                if (speedButton != null) {
                    speedButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Speed;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_speed);
                            setupScreen();
                            return true;
                        }
                    });
                }
                if (logsButton != null) {
                    logsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Logs;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_logs);
                            setupScreen();
                            return true;
                        }
                    });
                }
                if (alarmButton != null) {
                    alarmButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Alarms;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_alarms);
                            setupScreen();
                            return true;
                        }
                    });
                }
                if (watchButton != null) {
                    watchButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Watch;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_watch);
                            setupScreen();
                            return true;
                        }
                    });
                }
		        if (wheelButton != null) {
                    wheelButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Wheel;
                            getPreferenceScreen().removeAll();
                            if (mWheelType == WHEEL_TYPE.NINEBOT_Z) addPreferencesFromResource(R.xml.preferences_ninebot_z);
							if (mWheelType == WHEEL_TYPE.INMOTION) addPreferencesFromResource(R.xml.preferences_inmotion);
							if (mWheelType == WHEEL_TYPE.KINGSONG) {
							    addPreferencesFromResource(R.xml.preferences_kingsong);
							    if(WheelData.getInstance().isPrefReceived()) {

                                    correctWheelBarState(getString(R.string.wheel_max_speed), WheelData.getInstance().getWheelMaxSpeed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm1), WheelData.getInstance().getKSAlarm1Speed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm2), WheelData.getInstance().getKSAlarm2Speed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm3), WheelData.getInstance().getKSAlarm3Speed());

                                }
                            }
							if (mWheelType == WHEEL_TYPE.GOTWAY) {
								addPreferencesFromResource(R.xml.preferences_gotway);
								Preference startCalibrationButton = findPreference(getString(R.string.start_calibration));
								if (startCalibrationButton != null) {
									startCalibrationButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
										@Override
										public boolean onPreferenceClick(Preference preference) {                            
											WheelData.getInstance().updateCalibration();
											return true;
										}
									});
								}
							}

                            setupScreen();
                            return true;
                        }
                    });
                }
				if (resetTopButton != null) {
                    resetTopButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
							WheelData.getInstance().resetTopSpeed();
                            return true;
                        }
                    });
                }

                if (resetLowestBatteryButton != null) {
                    resetLowestBatteryButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            WheelData.getInstance().resetVoltageSag();
                            activity.sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_RESET));
                            return true;
                        }
                    });
                }
				if (resetUserDistanceButton != null) {
                    resetUserDistanceButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {                            
							WheelData.getInstance().resetUserDistance();
                            return true;
                        }
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
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show();
                            return true;
                        }
                    });
                }

                if (lastMacButton != null) {
                    lastMacButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle(getText(R.string.edit_mac_addr_title));

                            final EditText input = new EditText(activity);
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setText(SettingsUtil.getLastAddress(activity));
                            builder.setView(input);
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String deviceAddress = input.getText().toString();
                                    SettingsUtil.setLastAddress(activity, deviceAddress);
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                                    builder1.setTitle(getText(R.string.wheel_pass_imotion));

                                    final EditText input1 = new EditText(activity);
                                    input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                    builder1.setView(input1);
                                    builder1.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String password = input1.getText().toString();
                                            SettingsUtil.setPasswordForWheel(activity, deviceAddress, password);
                                        }
                                    });
                                    builder1.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });
                                    builder1.show();
                                }
                            });
                            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            builder.show();
                            return true;
                        }

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
    }

    void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log));
            correctCheckState(getString(R.string.log_location_data));
            correctCheckState(getString(R.string.auto_upload));
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

        mWheelType = WheelData.getInstance().getWheelType();
        if (mWheelType == WHEEL_TYPE.INMOTION || mWheelType == WHEEL_TYPE.KINGSONG || mWheelType == WHEEL_TYPE.GOTWAY || mWheelType == WHEEL_TYPE.NINEBOT_Z) {
            wheelButton.setEnabled(true);
        }

        currentScreen = SettingsScreen.Main;
        setupScreen();
    }

	private void correctWheelCheckState(String preference, boolean state) {
        CheckBoxPreference cbPreference = findPreference(preference);
        if (cbPreference == null) {
            return;
        }

        boolean checkState = cbPreference.isChecked();
        if (state != checkState) {
            cbPreference.setChecked(state);
        }
    }
	
	private void correctWheelBarState(String preference, int stateInt) {
        SeekBarPreference sbPreference = findPreference(preference);
		if (sbPreference == null)
			return;
		int sbValue = sbPreference.getCurrentValue();
		if (stateInt != sbValue) {
			sbPreference.setCurrentValue(stateInt);
			/// Workaround, seekbar doesn't want to update view
            getPreferenceScreen().removeAll();

            if (mWheelType == WHEEL_TYPE.NINEBOT_Z) addPreferencesFromResource(R.xml.preferences_ninebot_z);
			if (mWheelType == WHEEL_TYPE.INMOTION) addPreferencesFromResource(R.xml.preferences_inmotion);
			if (mWheelType == WHEEL_TYPE.KINGSONG) addPreferencesFromResource(R.xml.preferences_kingsong);
			if (mWheelType == WHEEL_TYPE.GOTWAY) addPreferencesFromResource(R.xml.preferences_gotway);
            setupScreen();
		}
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preference, stateInt);
        editor.apply();
    }

    private void correctCheckState(String preference) {
        boolean settingState = SettingsUtil.getBoolean(getActivity(), preference);
        CheckBoxPreference checkBoxPreference = findPreference(preference);
        if (checkBoxPreference == null) {
            return;
        }

        boolean checkState = checkBoxPreference.isChecked();
        if (settingState != checkState) {
            checkBoxPreference.setChecked(settingState);
        }
    }

    private void hideShowSeekBarsAlarms() {
        boolean alarmsEnabled = getPreferenceManager().getSharedPreferences()
                .getBoolean(getString(R.string.alarms_enabled), false);
        boolean alteredAlarms = getPreferenceManager().getSharedPreferences()
                .getBoolean(getString(R.string.altered_alarms), false);
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
        };

        String[] seekbarPreferencesCommon = {
                getString(R.string.alarm_current),
                getString(R.string.alarm_temperature)
        };
        for (String preference : seekbarPreferencesNormal) {
            SeekBarPreference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled && !alteredAlarms);
        }
        for (String preference : seekbarPreferencesAltered) {
            SeekBarPreference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled && alteredAlarms);
        }

        for (String preference : seekbarPreferencesCommon) {
            SeekBarPreference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarmsEnabled);
        }
    }

    private void hideShowSeekBarsApp() {
        boolean connectSound = getPreferenceManager().getSharedPreferences()
                .getBoolean(getString(R.string.connection_sound), false);

        String[] seekbarPreferences = {
                getString(R.string.no_connection_sound)
        };

        for (String preference : seekbarPreferences) {
            SeekBarPreference seekbar = findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(connectSound);
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
