package com.cooper.wheellog

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.cooper.wheellog.preferences.PreferencesFragment

class SettingsActivity : AppCompatActivity() {
    companion object {
        const val permissionWriteCode = 1
        const val permissionReadCode = 2
        const val permissionLocationCode = 3
    }

    private lateinit var fragment: PreferencesFragment

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            fragment = PreferencesFragment()
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_frame, fragment)
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }) {
            return super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        // Denied
        when (requestCode) {
            permissionWriteCode -> {
                WheelLog.AppConfig.autoLog = false
                WheelLog.AppConfig.enableRawData = false
            }
            permissionReadCode -> WheelLog.AppConfig.useCustomBeep = false
            permissionLocationCode -> WheelLog.AppConfig.useGps = false
        }
        fragment.refreshVolatileSettings()
    }
}