package com.cooper.wheellog

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cooper.wheellog.preferences.PreferencesFragment

class SettingsActivity : AppCompatActivity() {

    private lateinit var fragment: PreferencesFragment

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setTheme(R.style.SettingStyle)
        if (savedInstanceState == null) {
            fragment = PreferencesFragment()
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_frame, fragment)
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}