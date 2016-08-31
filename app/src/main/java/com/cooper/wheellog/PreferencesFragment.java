package com.cooper.wheellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.SettingsUtil;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
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
        hideShowSeekBars();
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
        }
        getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }

    public void refreshVolatileSettings() {
        ((CheckBoxPreference) findPreference("auto_log")).setChecked(SettingsUtil.isAutoLogEnabled(getActivity()));
        ((CheckBoxPreference) findPreference("log_location_data")).setChecked(SettingsUtil.isLogLocationEnabled(getActivity()));
        ((CheckBoxPreference) findPreference("auto_upload")).setChecked(SettingsUtil.isAutoUploadEnabled(getActivity()));
    }

    private void hideShowSeekBars() {
        boolean alarms_enabled = getPreferenceManager().getSharedPreferences().getBoolean("alarms_enabled", false);
        findPreference("alarm_speed").setEnabled(alarms_enabled);
    }
}
