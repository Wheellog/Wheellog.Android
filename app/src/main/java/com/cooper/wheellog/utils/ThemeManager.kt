package com.cooper.wheellog.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog

class ThemeManager {
    var theme = WheelLog.AppConfig.appTheme

    fun getTypeface(context: Context): Typeface {
        val fontResId = when(theme) {
            R.style.AJDMTheme -> R.font.ajdm
            else -> R.font.prime
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.resources.getFont(fontResId)
        else
            ResourcesCompat.getFont(context, fontResId)!!
    }

    private val ajdmDrawables = HashMap<Int, Int>()
    
    init {
        // notification
        ajdmDrawables[R.drawable.ic_action_wheel_light_orange] = R.drawable.ajdm_wheel_new_g
        ajdmDrawables[R.drawable.ic_action_wheel_orange] = R.drawable.connect_ajdm_o
        ajdmDrawables[R.drawable.ic_action_wheel_grey] = R.drawable.ajdm_wheel_new_b
        ajdmDrawables[R.drawable.ic_action_logging_orange] = R.drawable.ajdm_log_new_o
        ajdmDrawables[R.drawable.ic_action_logging_grey] = R.drawable.ajdm_log_new_b
        ajdmDrawables[R.drawable.ic_action_watch_orange] = R.drawable.ajdm_watch_new_o
        ajdmDrawables[R.drawable.ic_action_watch_grey] = R.drawable.ajdm_watch_new_b
        ajdmDrawables[R.drawable.ic_horn_32_gray] = R.drawable.horn_ajdm_o
        ajdmDrawables[R.drawable.ic_sun_32_gray] = R.drawable.light_ajdm_o
        ajdmDrawables[R.drawable.ic_stat_wheel] = R.drawable.wheel_ajdm_w
        // menu
        ajdmDrawables[R.drawable.ic_action_watch_orange] = R.drawable.ajdm_watch_g
        ajdmDrawables[R.drawable.ic_action_watch_white] = R.drawable.ajdm_watch_new
        ajdmDrawables[R.drawable.ic_mi_alarm] = R.drawable.ajdm_ic_mi_alarm
        ajdmDrawables[R.drawable.ic_mi_min] = R.drawable.ajdm_ic_mi_min
        ajdmDrawables[R.drawable.ic_mi_med] = R.drawable.ajdm_ic_mi_med
        ajdmDrawables[R.drawable.ic_mi_max] = R.drawable.ajdm_ic_mi_max
        ajdmDrawables[R.drawable.ic_action_logging_orange] = R.drawable.ajdm_log_new_g
        ajdmDrawables[R.drawable.ic_action_logging_white] = R.drawable.ajdm_log_new
        ajdmDrawables[R.drawable.ic_action_wheel_orange] = R.drawable.ajdm_wheel_new_g
        ajdmDrawables[R.drawable.anim_wheel_icon] = R.drawable.ajdm_anim_wheel_icon
        ajdmDrawables[R.drawable.ic_action_wheel_white] = R.drawable.ajdm_wheel_new
        ajdmDrawables[R.drawable.ic_baseline_settings_24] = R.drawable.ajdm_sett3_new
        ajdmDrawables[R.drawable.ic_action_bluetooth_searching_white] = R.drawable.ajdm_bt_new
        // prefs
        ajdmDrawables[R.drawable.ic_speedometer_white_24dp] = R.drawable.ajdm_sett5_new
        ajdmDrawables[R.drawable.ic_show_chart_white_24dp] = R.drawable.ajdm_log_new_set
        ajdmDrawables[R.drawable.ic_baseline_watch_24] = R.drawable.ajdm_watch_new
        ajdmDrawables[R.drawable.ic_baseline_bug_report_24] = R.drawable.ajdm_flow
        ajdmDrawables[R.drawable.ic_baseline_info_24] = R.drawable.ajdm_info
        ajdmDrawables[R.drawable.ic_baseline_vibration_24] = R.drawable.ajdm_alarm
        ajdmDrawables[R.drawable.ic_wheel_white_24] = R.drawable.ajdm_wheel_set
        ajdmDrawables[R.drawable.ic_baseline_explore_24] = R.drawable.ajdm_trip
        //trip page
        ajdmDrawables[R.drawable.ic_baseline_cloud_upload_24] = R.drawable.ajdm_cloud
        ajdmDrawables[R.drawable.ic_baseline_share_24] = R.drawable.ajdm_share
    }

    @DrawableRes
    fun getDrawableId(@DrawableRes originalId: Int): Int {
        return when(theme) {
            R.style.AJDMTheme -> ajdmDrawables[originalId] ?: R.drawable.transparent
            else -> originalId
        }
    }

    fun changeAppIcon(pkg: Context) {
        // change main launcher icon
        val pm = pkg.packageManager
        val original = ComponentName(pkg, "com.cooper.wheellog.OriginalIconAlias")
        val ajdm = ComponentName(pkg, "com.cooper.wheellog.AjdmIconAlias")
        if (WheelLog.AppConfig.appTheme == R.style.AJDMTheme) {
            pm.setComponentEnabledSetting(
                    ajdm,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(
                    original,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        } else {
            pm.setComponentEnabledSetting(
                    ajdm,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(
                    original,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
        }
    }
}