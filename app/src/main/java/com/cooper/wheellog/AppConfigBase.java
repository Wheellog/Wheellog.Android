package com.cooper.wheellog;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AppConfigBase {
    protected AppConfigBase(Context context) {
        this.context = context;
        this.specificID = generalSettingsPrefix;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private final String generalSettingsPrefix = "_";
    private final String settingsKeyDelimiter = "_";
    private Context context;
    private SharedPreferences sharedPreferences;
    private String specificID;

    public abstract void switchSettingSpecific(String keyString, Boolean fromControl);
    public abstract void setAllSettingsToControls();
    public abstract void setAllControlsToSettings();

    // Common settings methods
    public Boolean isGeneral() {
        return generalSettingsPrefix == this.specificID;
    }

    public void initGeneralSettingsSpecific() {
        if (!isGeneral())
            return;

        if (getIsGeneralSettingsMigrated()) {
            setAllSettingsToControls();
        } else {
            setAllControlsToSettings();
            setIsGeneralSettingsMigrated(true);
        }
    }

    public void changeSettingsSpecific(String specificID) {
        if (specificID == this.specificID)
            return;

        this.specificID = specificID;
        if (getIsSpecificSettingsMigrated()) {
            setAllSettingsToControls();
        } else {
            setAllControlsToSettings();
            setIsSpecificSettingsMigrated(true);
        }
    }

    protected String getControlKey(int resId) {
        return context.getString(resId);
    }

    protected String getGeneralKey(int resId) {
        return getGeneralKey(getControlKey(resId));
    }

    protected String getGeneralKey(String keyString) {
        return generalSettingsPrefix + settingsKeyDelimiter + keyString;
    }

    protected String getSpecificKey(int resId) {
        return getSpecificKey(getControlKey(resId));
    }

    public String getSpecificKey(String keyString) {
        return specificID + settingsKeyDelimiter + keyString;
    }

    private String getRequiredKey(int resId, SettingsType settingsType, Boolean... isControl) {
        if (isFromControl(isControl))
            return getControlKey(resId);

        return settingsType == SettingsType.General
                ? getGeneralKey(resId)
                : getSpecificKey(resId);
    }

    private Boolean isFromControl(Boolean... fromControl) {
        return fromControl != null && fromControl.length != 0 && fromControl[0];
    }

    public Boolean isTrueSettings(String key) {
        if (cachedBooleanSettings.containsKey(key))
            return cachedBooleanSettings.get(key);

        Boolean value = sharedPreferences.getBoolean(key, false);
        cachedBooleanSettings.put(key, value);
        return value;
    }

    protected Boolean contains(String key) {
        return cachedBooleanSettings.containsKey(key)
                || cachedStringSettings.containsKey(key)
                || cachedIntegerSettings.containsKey(key)
                || cachedLongSettings.containsKey(key)
                || sharedPreferences.contains(key);
    }

    // Boolean settings
    private Map<String, Boolean> cachedBooleanSettings = new HashMap<String, Boolean>();

    protected Boolean getBoolean(int resId, Boolean defaultValue, SettingsType settingsType, Boolean... fromControl) {
        return isFromControl(fromControl)
                ? sharedPreferences.getBoolean(getControlKey(resId), defaultValue)
                : getBoolean(getRequiredKey(resId, settingsType, fromControl), defaultValue);
    }

    protected Boolean getBoolean(String key, Boolean defaultValue) {
        if (cachedBooleanSettings.containsKey(key))
            return cachedBooleanSettings.get(key);

        Boolean value = sharedPreferences.getBoolean(key, defaultValue);
        cachedBooleanSettings.put(key, value);
        return value;
    }

    protected void setBoolean(int resId, Boolean newValue, SettingsType settingsType, Boolean... toControl) {
        setBoolean(getRequiredKey(resId, settingsType, toControl), newValue);
    }

    protected void setBoolean(String key, Boolean newValue) {
        sharedPreferences.edit().putBoolean(key, newValue).commit();
        cachedBooleanSettings.remove(key);
    }

    // String settings
    private Map<String, String> cachedStringSettings = new HashMap<String, String>();

    protected String getString(int resId, String defaultValue, SettingsType settingsType, Boolean... fromControl) {
        return isFromControl(fromControl)
                ? sharedPreferences.getString(getControlKey(resId), defaultValue)
                : getString(getRequiredKey(resId, settingsType, fromControl), defaultValue);
    }

    protected String getString(String key, String defaultValue) {
        if (cachedStringSettings.containsKey(key))
            return cachedStringSettings.get(key);

        String value = sharedPreferences.getString(key, defaultValue);
        cachedStringSettings.put(key, value);
        return value;
    }

    protected void setString(int resId, String newValue,SettingsType settingsType, Boolean... toControl) {
        setString(getRequiredKey(resId, settingsType, toControl), newValue);
    }

    protected void setString(String key, String newValue) {
        sharedPreferences.edit().putString(key, newValue).commit();
        cachedStringSettings.remove(key);
    }

    // Integer settings
    private Map<String, Integer> cachedIntegerSettings = new HashMap<String, Integer>();

    protected Integer getInt(int resId, Integer defaultValue, SettingsType settingsType, Boolean... fromControl) {
        return isFromControl(fromControl)
                ? sharedPreferences.getInt(getControlKey(resId), defaultValue)
                : getInt(getRequiredKey(resId, settingsType, fromControl), defaultValue);
    }

    protected Integer getInt(String key, Integer defaultValue) {
        if (cachedIntegerSettings.containsKey(key))
            return cachedIntegerSettings.get(key);

        Integer value = sharedPreferences.getInt(key, defaultValue);
        cachedIntegerSettings.put(key, value);
        return value;
    }

    protected void setInt(int resId, Integer newValue, SettingsType settingsType, Boolean... toControl) {
        setInt(getRequiredKey(resId, settingsType, toControl), newValue);
    }

    private void setInt(String key, Integer newValue) {
        sharedPreferences.edit().putInt(key, newValue).commit();
        cachedIntegerSettings.remove(key);
    }

    public void setIntByGeneralKey(String key, Integer newValue) {
        setInt(key, newValue);
    }

    // Long settings
    private Map<String, Long> cachedLongSettings = new HashMap<String, Long>();

    protected Long getLong(int resId, Long defaultValue, SettingsType settingsType, Boolean... fromControl) {
        return isFromControl(fromControl)
                ? sharedPreferences.getLong(getControlKey(resId), defaultValue)
                : getLong(getRequiredKey(resId, settingsType, fromControl), defaultValue);
    }

    protected Long getLong(String key, Long defaultValue) {
        if (cachedLongSettings.containsKey(key))
            return cachedLongSettings.get(key);

        Long value = sharedPreferences.getLong(key, defaultValue);
        cachedLongSettings.put(key, value);
        return value;
    }

    protected void setLong(int resId, Long newValue, SettingsType settingsType, Boolean... toControl) {
        setLong(getRequiredKey(resId, settingsType, toControl), newValue);
    }

    protected void setLong(String key, Long newValue) {
        sharedPreferences.edit().putLong(key, newValue).commit();
        cachedLongSettings.remove(key);
    }

    // Set<String> settings
    private Map<String, Set<String>> cachedStringSetSettings = new HashMap<String, Set<String>>();

    protected Set<String> getStringSet(int resId, Set<String> defaultValue, SettingsType settingsType, Boolean... fromControl) {
        return isFromControl(fromControl)
                ? sharedPreferences.getStringSet(getControlKey(resId), defaultValue)
                : getStringSet(getRequiredKey(resId, settingsType, fromControl), defaultValue);
    }

    protected Set<String> getStringSet(String key, Set<String> defaultValue) {
        if (cachedStringSetSettings.containsKey(key))
            return cachedStringSetSettings.get(key);

        Set<String> value = sharedPreferences.getStringSet(key, defaultValue);
        cachedStringSetSettings.put(key, value);
        return value;
    }

    protected void setStringSet(int resId, Set<String> newValue, SettingsType settingsType, Boolean... toControl) {
        setStringSet(getRequiredKey(resId, settingsType, toControl), newValue);
    }

    private void setStringSet(String key, Set<String> newValue) {
        sharedPreferences.edit().putStringSet(key, newValue).commit();
        cachedStringSetSettings.remove(key);
    }

    // Migrated settings
    public Boolean getIsGeneralSettingsMigrated() {
        return getBoolean("isGeneralSettingsMigrated", false);
    }

    public void setIsGeneralSettingsMigrated(Boolean newValue) {
        setBoolean("isGeneralSettingsMigrated", newValue);
    }

    public Boolean getIsSpecificSettingsMigrated() {
        return getBoolean("isSpecificSettingsMigrated", false);
    }

    public void setIsSpecificSettingsMigrated(Boolean newValue) {
        setBoolean("isSpecificSettingsMigrated", newValue);
    }

    public enum SettingsType
    {
        General,
        Specific,
    }
}