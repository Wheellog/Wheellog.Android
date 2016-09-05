package com.cooper.wheellog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.SettingsUtil;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    enum SettingsScreen {
        Main,
        Speed,
        Logs,
        Alarms
    }

    private boolean mDataWarningDisplayed = false;
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
                            .setTitle("Enable Auto Upload?")
                            .setMessage("Automatic uploading while not connected to WiFi will use your mobile data.  This may result in charges from your network provider if you do not have a data plan.")
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
                tb.setTitle("Settings");
                Preference speed_button = findPreference("speed_preferences");
                Preference logs_button = findPreference("log_preferences");
                Preference alarm_button = findPreference("alarm_preferences");

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
                break;
            case Speed:
                tb.setTitle("Speed Settings");
                break;
            case Logs:
                tb.setTitle("Log Settings");
                break;
            case Alarms:
                tb.setTitle("Alarm Settings");
                hideShowSeekBars();
                break;
        }
    }

    public void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState("auto_log");
            correctCheckState("log_location_data");
            correctCheckState("auto_upload");
        }
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
        boolean alarms_enabled = getPreferenceManager().getSharedPreferences().getBoolean("alarms_enabled", false);
        findPreference("alarm_1_speed").setEnabled(alarms_enabled);
        findPreference("alarm_2_speed").setEnabled(alarms_enabled);
        findPreference("alarm_3_speed").setEnabled(alarms_enabled);
        findPreference("alarm_1_battery").setEnabled(alarms_enabled);
        findPreference("alarm_2_battery").setEnabled(alarms_enabled);
        findPreference("alarm_3_battery").setEnabled(alarms_enabled);
    }

    public boolean show_main_menu() {
        if (currentScreen == SettingsScreen.Main)
            return false;
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
        currentScreen = SettingsScreen.Main;
        setup_screen();
        return true;
    }
}
