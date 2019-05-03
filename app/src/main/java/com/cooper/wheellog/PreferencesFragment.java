package com.cooper.wheellog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.*;
import android.text.InputType;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.SettingsUtil;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    enum SettingsScreen {
        Main,
        Speed,
        Logs,
        Alarms,
        Watch,
		Wheel
    }
	
	WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;

    private boolean mDataWarningDisplayed = false;
	private boolean mSpeedWarningDisplayed = false;
    private SettingsScreen currentScreen = SettingsScreen.Main;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setup_screen();

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "alarms_enabled":
                hideShowSeekBars();
                break;
            case "auto_upload":
                if (SettingsUtil.isAutoUploadEnabled(getActivity()) && !mDataWarningDisplayed) {
                    SettingsUtil.setAutoUploadEnabled(getActivity(), false);
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.enable_auto_upload_title))  // ("Enable Auto Upload?")
                            .setMessage(getString(R.string.enable_auto_upload_descriprion))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = true;
                                    SettingsUtil.setAutoUploadEnabled(getActivity(), true);
                                    refreshVolatileSettings();
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = false;
                                    refreshVolatileSettings();
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                } else
                    mDataWarningDisplayed = false;
                break;
            case "use_mph":
                getActivity().sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;			
            case "max_speed":
                getActivity().sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
			case "light_enabled":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_LIGHT, true));
				boolean ligth_enabled = sharedPreferences.getBoolean(getString(R.string.light_enabled), false);
				WheelData.getInstance().updateLight(ligth_enabled);
				break;
			case "led_enabled":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_LED, true));
				boolean led_enabled = sharedPreferences.getBoolean(getString(R.string.led_enabled), false);
				WheelData.getInstance().updateLed(led_enabled);
				break;
			case "handle_button_disabled":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_BUTTON, true));
				boolean handle_button_disabled = sharedPreferences.getBoolean(getString(R.string.handle_button_disabled), false);
				WheelData.getInstance().updateHandleButton(handle_button_disabled);
				break;
			case "wheel_max_speed":
				//if (!mSpeedWarningDisplayed) {
				final int max_speed = sharedPreferences.getInt(getString(R.string.wheel_max_speed), 0);
				WheelData.getInstance().updateMaxSpeed(max_speed);
				//	if (max_speed > 30)  {
				//		new AlertDialog.Builder(getActivity())
				//			.setTitle("Are you sure?")
				//			.setMessage("Setting a speed limit higher than 30 km/h is unsafe, this is an undocumented feature. \n\nUSE IT ON YOUR OWN RISK! \n\nNeither the Inmotion Company nor the developers of this application are liable for any damages to your health, EUC or third party resulting by using of this feature.")
				//			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				//				public void onClick(DialogInterface dialog, int which) {									
				//					WheelData.getInstance().updateMaxSpeed(max_speed);
				//				}
				//			})
				//			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				//				public void onClick(DialogInterface dialog, int which) {									
				//					mSpeedWarningDisplayed = true;
				//					correctWheelBarState(getString(R.string.wheel_max_speed), WheelData.getInstance().getWheelMaxSpeed());
				//					
				//				}
				//			})
				//			.setIcon(android.R.drawable.ic_dialog_alert)
				//			.show();
	
				//	} else {
				//		//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_MAX_SPEED, true));
				//		WheelData.getInstance().updateMaxSpeed(max_speed);
				//	}
				//} else mSpeedWarningDisplayed = false;
				
				break;
			case "speaker_volume":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_SPEAKER_VOLUME, true));
				int speaker_volume = sharedPreferences.getInt(getString(R.string.speaker_volume), 0);
				WheelData.getInstance().updateSpeakerVolume(speaker_volume);
				break;
			case "pedals_adjustment":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_PEDALS_ADJUSTMENT, true));
				int pedals_adjustment = sharedPreferences.getInt(getString(R.string.pedals_adjustment), 0);
				WheelData.getInstance().updatePedals(pedals_adjustment);
				break;
			case "pedals_mode":
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_PEDALS_ADJUSTMENT, true));
				int pedals_mode = Integer.parseInt(sharedPreferences.getString(getString(R.string.pedals_mode), "0"));
				WheelData.getInstance().updatePedalsMode(pedals_mode);
				break;
			case "light_mode":
				int light_mode = Integer.parseInt(sharedPreferences.getString(getString(R.string.light_mode), "0"));
				WheelData.getInstance().updateLightMode(light_mode);
				break;
			case "alarm_mode":
				int alarm_mode = Integer.parseInt(sharedPreferences.getString(getString(R.string.alarm_mode), "0"));
				WheelData.getInstance().updateAlarmMode(alarm_mode);
				break;
			case "strobe_mode":
				int strobe_mode = Integer.parseInt(sharedPreferences.getString(getString(R.string.strobe_mode), "0"));
				WheelData.getInstance().updateStrobe(strobe_mode);
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
//			case "reset_user_trip":				
//				WheelData.getInstance().resetUserDistance();
//				break;
//			case "reset_max_speed":				
//				WheelData.getInstance().resetTopSpeed();
//				break;
        }
        getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }
	


    private void setup_screen() {
        Toolbar tb = (Toolbar) getActivity().findViewById(R.id.preference_toolbar);
        if (currentScreen == SettingsScreen.Main)
            tb.setNavigationIcon(null);
        else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            tb.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    show_main_menu();
                }
            });
        }
		

        switch (currentScreen) {
            case Main:
                tb.setTitle(getText(R.string.settings_title));
                Preference speed_button = findPreference(getString(R.string.speed_preferences));
                Preference logs_button = findPreference(getString(R.string.log_preferences));
                Preference alarm_button = findPreference(getString(R.string.alarm_preferences));
                Preference watch_button = findPreference(getString(R.string.watch_preferences));
				Preference wheel_button = findPreference(getString(R.string.wheel_settings));
				Preference reset_top_button = findPreference(getString(R.string.reset_top_speed));
				Preference reset_user_distance_button = findPreference(getString(R.string.reset_user_distance));
                Preference last_mac_button = findPreference(getString(R.string.last_mac));
                Preference about_button = findPreference(getString(R.string.about));
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING).putExtra(Constants.INTENT_EXTRA_WHEEL_UPDATE_SCALE, 1));

				
                if (speed_button != null) {
                    speed_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Speed;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_speed);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (logs_button != null) {
                    logs_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Logs;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_logs);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (alarm_button != null) {
                    alarm_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Alarms;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_alarms);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (watch_button != null) {
                    watch_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Watch;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_watch);
                            setup_screen();
                            return true;
                        }
                    });
                }
		        if (wheel_button != null) {
                    wheel_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Wheel;
                            getPreferenceScreen().removeAll();
                            if (mWheelType == WHEEL_TYPE.NINEBOT_Z) addPreferencesFromResource(R.xml.preferences_ninebot_z);
							if (mWheelType == WHEEL_TYPE.INMOTION) addPreferencesFromResource(R.xml.preferences_inmotion);
							if (mWheelType == WHEEL_TYPE.KINGSONG) {
							    addPreferencesFromResource(R.xml.preferences_kingsong);
							    if(WheelData.getInstance().is_pref_received()) {

                                    correctWheelBarState(getString(R.string.wheel_max_speed), WheelData.getInstance().getWheelMaxSpeed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm1), WheelData.getInstance().getKSAlarm1Speed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm2), WheelData.getInstance().getKSAlarm2Speed());
                                    correctWheelBarState(getString(R.string.wheel_ks_alarm3), WheelData.getInstance().getKSAlarm3Speed());

                                }
                            }
							if (mWheelType == WHEEL_TYPE.GOTWAY) {
							//if (mWheelType == WHEEL_TYPE.INMOTION) {
								addPreferencesFromResource(R.xml.preferences_gotway);
								Preference start_calibration_button = findPreference(getString(R.string.start_calibration));
								if (start_calibration_button != null) {
									start_calibration_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
										@Override
										public boolean onPreferenceClick(Preference preference) {                            
											WheelData.getInstance().updateCalibration();
											return true;
										}
									});
								}
							}
							//addPreferencesFromResource(R.xml.preferences_gotway);
							
                            setup_screen();
                            return true;
                        }
                    });
                }
				if (reset_top_button != null) {
                    reset_top_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
							WheelData.getInstance().resetTopSpeed();
                            return true;
                        }
                    });
                }
				if (reset_user_distance_button != null) {
                    reset_user_distance_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {                            
							WheelData.getInstance().resetUserDistance();
                            return true;
                        }
                    });
                }
                if (about_button != null) {
                    about_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            String versionName = BuildConfig.VERSION_NAME;
                            String buildTime = BuildConfig.BUILD_TIME;
                            new AlertDialog.Builder(getActivity())
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



                if (last_mac_button != null) {
                    last_mac_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(getText(R.string.edit_mac_addr_title));

                            final EditText input = new EditText(getActivity());
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setText(SettingsUtil.getLastAddress(getActivity()));
                            builder.setView(input);
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String deviceAddress = input.getText().toString();
                                    SettingsUtil.setLastAddress(getActivity(), deviceAddress);
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                                    builder1.setTitle(getText(R.string.wheel_pass_imotion));

                                    final EditText input1 = new EditText(getActivity());
                                    input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                    builder1.setView(input1);
                                    builder1.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String password = input1.getText().toString();
                                            //System.out.println("Set password ");
                                            //System.out.println(password);
                                            SettingsUtil.setPasswordForWheel(getActivity(), deviceAddress, password);
                                            password = SettingsUtil.getPasswordForWheel(getActivity(),deviceAddress);
                                            //System.out.println("Set password ");
                                            //System.out.println(password);
                                            //finish();
                                        }
                                    });
                                    builder1.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                            //finish();
                                        }
                                    });
                                    builder1.show();



                                    //SettingsUtil.setPasswordForWheel(getActivity(), deviceAddress, "000000");
                                    //finish();
                                }
                            });
                            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    //finish();
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
                break;
            case Logs:
                tb.setTitle(getText(R.string.logs_settings_title));
                break;
            case Alarms:
                tb.setTitle(getText(R.string.alarm_settings_title));
                hideShowSeekBars();
                break;
            case Watch:
                tb.setTitle(getText(R.string.watch_settings_title));
                break;
			case Wheel:
                tb.setTitle(getText(R.string.wheel_settings_title));
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_REFRESH, true));
                break;
        }
    }

    public void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log));
            correctCheckState(getString(R.string.log_location_data));
            correctCheckState(getString(R.string.auto_upload));
        }
    }
	
    public void refreshWheelSettings(boolean isLight, boolean isLed, boolean isButton, int maxSpeed, int speakerVolume, int pedals) {
        correctWheelCheckState(getString(R.string.light_enabled), isLight);
        correctWheelCheckState(getString(R.string.led_enabled), isLed);
        correctWheelCheckState(getString(R.string.handle_button_disabled), isButton);

        correctWheelBarState(getString(R.string.wheel_max_speed), maxSpeed);
		correctWheelBarState(getString(R.string.speaker_volume), speakerVolume);
		correctWheelBarState(getString(R.string.pedals_adjustment), pedals);

		

    }	
	
	private void correctWheelCheckState(String preference, boolean state) {
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (state != check_state)
            cb_preference.setChecked(state);

    }
	
	private void correctWheelBarState(String preference, int stateInt) {

        SeekBarPreference sb_preference = (SeekBarPreference) findPreference(preference);
		if (sb_preference == null)
			return;
		int sb_value = sb_preference.getCurrentValue();
		if (stateInt != sb_value) {
			sb_preference.setCurrentValue(stateInt);
			/// Workaround, seekbar doesn't want to update view
            getPreferenceScreen().removeAll();

            if (mWheelType == WHEEL_TYPE.NINEBOT_Z) addPreferencesFromResource(R.xml.preferences_ninebot_z);
			if (mWheelType == WHEEL_TYPE.INMOTION) addPreferencesFromResource(R.xml.preferences_inmotion);
			if (mWheelType == WHEEL_TYPE.KINGSONG) addPreferencesFromResource(R.xml.preferences_kingsong);
			if (mWheelType == WHEEL_TYPE.GOTWAY) addPreferencesFromResource(R.xml.preferences_gotway);
            
			//addPreferencesFromResource(R.xml.preferences_gotway);
			
			//addPreferencesFromResource(R.xml.preferences_inmotion);
            setup_screen();

		}
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preference, stateInt);
        //editor.commit();
        editor.apply();
			
    }

    private void correctCheckState(String preference) {
        boolean setting_state = SettingsUtil.getBoolean(getActivity(), preference);
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (setting_state != check_state)
            cb_preference.setChecked(setting_state);
    }

    private void hideShowSeekBars() {
        boolean alarms_enabled = getPreferenceManager().getSharedPreferences()
                .getBoolean(getString(R.string.alarms_enabled), false);
        String[] seekbar_preferences = {
                getString(R.string.alarm_1_speed),
                getString(R.string.alarm_2_speed),
                getString(R.string.alarm_3_speed),
                getString(R.string.alarm_1_battery),
                getString(R.string.alarm_2_battery),
                getString(R.string.alarm_3_battery),
                getString(R.string.alarm_current),
				getString(R.string.alarm_temperature)};

        for (String preference : seekbar_preferences) {
            SeekBarPreference seekbar = (SeekBarPreference) findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarms_enabled);
        }
    }
	
	public boolean is_main_menu() {
        if (currentScreen == SettingsScreen.Main)
            return true;
		else return false;		
	}

    public boolean show_main_menu() {
        //if (currentScreen == SettingsScreen.Main)
        //    return false;
        getPreferenceScreen().removeAll();

        addPreferencesFromResource(R.xml.preferences);
		//System.out.println("ShowMainMenuRecognized");
		Preference wheel_button = findPreference(getString(R.string.wheel_settings));
		mWheelType = WheelData.getInstance().getWheelType();
		if ((mWheelType == WHEEL_TYPE.INMOTION) | (mWheelType == WHEEL_TYPE.KINGSONG) | (mWheelType == WHEEL_TYPE.GOTWAY)  | (mWheelType == WHEEL_TYPE.NINEBOT_Z)) {
			wheel_button.setEnabled(true);
		}
		
        currentScreen = SettingsScreen.Main;
        setup_screen();
        return true;
    }
}
