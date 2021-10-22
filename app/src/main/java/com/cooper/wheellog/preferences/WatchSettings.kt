package com.cooper.wheellog.preferences

import android.content.Context
import androidx.preference.*
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.wheellog.shared.*

class WatchSettings(context: Context, ps: PreferenceScreen) : BaseSettingsClass(context, ps) {

    override fun fill(mac: String) {
        ps.removeAll()
        ListPreference(context).apply {
            key = getString(R.string.horn_mode)
            title = getString(R.string.horn_mode_title)
            summary = getString(R.string.horn_mode_description)
            entries = arrayOf(getString(R.string.disabled), getString(R.string.on_board_horn_ks), getString(R.string.bluetooth_audio_horn))
            entryValues = arrayOf("0", "1", "2")
            setDefaultValue(WheelLog.AppConfig.hornMode.toString())
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = getString(R.string.garmin_connectiq_enable)
            title = getString(R.string.garmin_connectiq_enable_title)
            summary = getString(R.string.garmin_connectiq_enable_description)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = getString(R.string.garmin_connectiq_use_beta)
            title = getString(R.string.garmin_connectiq_use_beta_title)
            summary = getString(R.string.garmin_connectiq_use_beta_description)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = getString(R.string.miband_on_mainscreen_enable)
            title = getString(R.string.miband_on_mainscreen_title)
            summary = getString(R.string.miband_on_mainscreen_description)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = getString(R.string.miband_fixrs_enable)
            title = getString(R.string.miband_fixrs_title)
            summary = getString(R.string.miband_fixrs_description)
            ps.addPreference(this)
        }
        MultiSelectPreference(context).apply {
            key = Constants.wearPages
            title = getString(R.string.wearos_pages_title)
            summary = getString(R.string.wearos_pages_description)
            dialogTitle = title
            setValues(WheelLog.AppConfig.wearOsPages.map { it.name })
            entries = WearPage.values().map { it.name }.toTypedArray()
            useSort = false
            ps.addPreference(this)
        }
    }
}