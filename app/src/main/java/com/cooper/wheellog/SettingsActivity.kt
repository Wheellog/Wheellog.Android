package com.cooper.wheellog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cooper.wheellog.databinding.SettingsActivityBinding
import com.cooper.wheellog.preferences.PreferencesFragment
import com.cooper.wheellog.preferences.SettingsMainScreen

class SettingsActivity : AppCompatActivity() {

//    private lateinit var fragment: PreferencesFragment
    private lateinit var binding: SettingsActivityBinding

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater, null, false)

        binding.prefSettings.setContent {
            SettingsMainScreen()
        }
        binding.prefSettings.visibility = View.VISIBLE
        setContentView(binding.root)
        setTheme(R.style.SettingStyle)
//        setContentView(R.layout.settings_activity)
//        setTheme(R.style.SettingStyle)
//        if (savedInstanceState == null) {
//            fragment = PreferencesFragment()
//            supportFragmentManager
//                    .beginTransaction()
//                    .replace(R.id.settings_frame, fragment)
//                    .commit()
//        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}