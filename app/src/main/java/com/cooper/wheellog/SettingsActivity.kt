package com.cooper.wheellog

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.core.content.res.ResourcesCompat

class SettingsActivity : AppCompatActivity() {
    companion object {
        const val permissionWriteCode = 1
        const val permissionLocationCode = 2
    }

    private lateinit var fragment: PreferencesFragment

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val typefacePrime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) resources.getFont(R.font.prime) else ResourcesCompat.getFont(this, R.font.prime)!!

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
            permissionLocationCode -> WheelLog.AppConfig.useGps = false
        }
        fragment.refreshVolatileSettings()
    }
}