package com.cooper.wheellog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker

class SettingsActivity : AppCompatActivity() {
    companion object {
        const val permissionWriteCode = 1
        const val permissionLocationCode = 2
    }

    private lateinit var fragment: PreferencesFragment

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
            permissionLocationCode -> WheelLog.AppConfig.logLocationData = false
        }
        fragment.refreshVolatileSettings()
    }
}