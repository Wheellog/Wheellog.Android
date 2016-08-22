package com.cooper.wheellog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

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
        if ("alarms_enabled".equals(key))
            hideShowSeekBars();
        getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }

    public void refreshLogSettings() {
        if (!SettingsUtil.getAutoLog(getActivity()))
            ((CheckBoxPreference) findPreference("auto_log")).setChecked(false);

        if (!SettingsUtil.getLogLocation(getActivity()))
            ((CheckBoxPreference) findPreference("log_location_data")).setChecked(false);
    }

    private void hideShowSeekBars() {
        boolean alarms_enabled = getPreferenceManager().getSharedPreferences().getBoolean("alarms_enabled", false);
        findPreference("alarm_speed").setEnabled(alarms_enabled);
    }
}
