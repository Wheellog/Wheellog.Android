package com.cooper.wheellog

//import com.yandex.metrica.YandexMetrica
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.cooper.wheellog.databinding.EdittextLayoutBinding
import com.cooper.wheellog.databinding.PrivacyPolicyBinding
import com.cooper.wheellog.databinding.UpdatePwmSettingsBinding
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.PermissionsUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object DialogHelper : KoinComponent {
    private val appConfig: AppConfig by inject()
    /**
     * return false if in App's Battery settings "Not optimized" and true if "Optimizing battery use"
     */
    private fun isBatteryOptimizations(context: Context): Boolean {
        val powerManager =
            context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.applicationContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !powerManager.isIgnoringBatteryOptimizations(name)
        }
        return false
    }

    private fun syncSeekBar(editText: EditText?, seekBar: SeekBar) {
        val s = (editText?.text ?: "").toString().replace(',', '.')
        if (s.isNotEmpty()) {
            val value = (s.toDouble() * 10).toInt()
            if (value > seekBar.max) {
                editText?.setText(String.format("%.1f", seekBar.max / 10f))
            } else {
                seekBar.progress = value
            }
        }
    }

    @SuppressLint("BatteryLife")
    fun checkBatteryOptimizationsAndShowAlert(context: Context) {
        if (!appConfig.detectBatteryOptimization ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            !isBatteryOptimizations(context)
        ) {
            return
        }
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:" + context.packageName)
        }
        context.startActivity(intent)
    }

    fun checkPWMIsSetAndShowAlert(context: Context) {
        val wd = WheelData.getInstance()
        if (!wd.isWheelIsReady || wd.isHardwarePWM || appConfig.hwPwm || appConfig.rotationIsSet) {
            return
        }
        if (appConfig.rotationSpeed != 500 && appConfig.rotationVoltage != 840) {
            appConfig.rotationIsSet = true
            return
        }
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val binding = UpdatePwmSettingsBinding.inflate(inflater, null, false)
        binding.modelName.text =
            if (WheelData.getInstance().model.isNullOrEmpty())
                "Unknown model"
            else WheelData.getInstance().model
        val svLayout: LinearLayout = binding.setSpeedVoltageLayout
        val templatesBox: Spinner = binding.spinnerTemplates
        val templates = when (WheelData.getInstance().wheelType) {
            Constants.WHEEL_TYPE.GOTWAY ->
                mutableMapOf(
                        "Begode MTen 67v" to Pair(440, 672), // first - speed, second - voltage
                        "Begode MTen 84v" to Pair(560, 840),
                        "Begode MCM5 67v" to Pair(440, 672),
                        "Begode MCM5v2 67v" to Pair(512, 672),
                        "Begode MCM5 84v" to Pair(560, 840),
                        "Begode MCM5v2 84v" to Pair(640, 840),
                        "Begode Tesla/T3 84v" to Pair(665, 840),
                        "Begode Nikola 84v" to Pair(706, 840),
                        "Begode Nikola 100v" to Pair(855, 1008),
                        "Begode MSX 84v" to Pair(790, 840),
                        "Begode MSX 100v" to Pair(950, 1008),
                        "Begode MSP HS (C30)" to Pair(1005, 1008),
                        "Begode MSP HT (C38)" to Pair(790, 1008),
                        "Begode EX (C40)" to Pair(790, 1008),
                        "Begode EX.N (C30)" to Pair(1071, 1008),
                        "Begode RS HS (C30)" to Pair(1050, 1008),
                        "Begode RS HT (C38)" to Pair(790, 1008),
                        "Begode Hero HS (C30)" to Pair(1050, 1008),
                        "Begode Hero HT (C38)" to Pair(790, 1008),
                        "Begode Master (C38)" to Pair(1130, 1344),
                        "Begode Monster 84v" to Pair(744, 1008),
                        "Begode Monster 100v" to Pair(930, 1008)
                )
            Constants.WHEEL_TYPE.VETERAN ->
                mutableMapOf(
                        "Veteran Sherman" to Pair(1020, 1008),
                        "Veteran Abrams" to Pair(980, 1000)
                )
            Constants.WHEEL_TYPE.NINEBOT_Z ->
                mutableMapOf(
                        "Ninebot Z6" to Pair(615, 577),
                        "Ninebot Z8/Z10" to Pair(815, 577)
                )
            Constants.WHEEL_TYPE.INMOTION ->
                mutableMapOf(
                        "Inmotion V5F" to Pair(370, 840),
                        "Inmotion V8" to Pair(450, 840),
                        "Inmotion V8F/V8S" to Pair(580, 840),
                        "Inmotion V10/V10F" to Pair(550, 840)
                )
            Constants.WHEEL_TYPE.INMOTION_V2 ->
                mutableMapOf(
                        "Inmotion V11" to Pair(800, 840)
                )
            else -> {
                binding.radioButton3.isEnabled = false
                mutableMapOf()
            }
        }
        templatesBox.visibility = View.GONE
        templatesBox.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1,
            templates.toList().map { it.first })



        var selectedOption = 1
        binding.selectedPwmVariant
            .setOnCheckedChangeListener { _, checkedId ->
                svLayout.visibility =
                    if (checkedId == binding.radioButton1.id) View.VISIBLE else View.GONE
                templatesBox.visibility =
                    if (checkedId == binding.radioButton3.id) View.VISIBLE else View.GONE
                when (checkedId) {
                    R.id.radioButton1 -> selectedOption = 1
                    R.id.radioButton2 -> selectedOption = 2
                    R.id.radioButton3 -> selectedOption = 3
                }
            }
        binding.seekBarSpeed.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.speedValue.editText?.setText(String.format("%.1f", progress / 10f))
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        binding.speedValue.editText?.doAfterTextChanged {
            syncSeekBar(binding.speedValue.editText, binding.seekBarSpeed)
        }

        binding.seekBarVoltage.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.voltageValue.editText?.setText(String.format("%.1f", progress / 10f))
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        binding.voltageValue.editText?.doAfterTextChanged {
            syncSeekBar(binding.voltageValue.editText, binding.seekBarVoltage)
        }

        val model = binding.modelName.text.split(" ").last()
        val defaultFromTemplate =
            templates.filter { it.key.split(" ", "/").contains(model) }.values.firstOrNull()
                ?: Pair(500, 1000)
        binding.speedValue.editText?.setText(String.format("%.1f", defaultFromTemplate.first / 10f))
        binding.voltageValue.editText?.setText(String.format("%.1f", defaultFromTemplate.second / 10f))

        AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(R.string.setup_pwm_dialog_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                when (selectedOption) {
                    1 -> {
                        appConfig.apply {
                            rotationSpeed = binding.seekBarSpeed.progress
                            rotationVoltage = binding.seekBarVoltage.progress
                        }
                        appConfig.rotationIsSet = true
                    }
                    2 -> TODO("доделать как-то Авто")
                    3 -> {
                        val temp = templates[templatesBox.selectedItem]
                        if (temp != null) {
                            appConfig.apply {
                                rotationSpeed = temp.first
                                rotationVoltage = temp.second
                            }
                            appConfig.rotationIsSet = true
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    fun checkAndShowPrivatePolicyDialog(mainActivity: MainActivity) {
        if (appConfig.privatePolicyAccepted) {
            return
        }

        val inflater: LayoutInflater = LayoutInflater.from(mainActivity)
        val binding = PrivacyPolicyBinding.inflate(inflater, null, false)
        val agree = binding.agreeWithPolicy
        val policyText = binding.policyLinks
        policyText.movementMethod = LinkMovementMethod.getInstance()
        policyText.text = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Html.fromHtml(mainActivity.resources.getString(R.string.private_policy), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(mainActivity.resources.getString(R.string.private_policy))
        }
        val dialog = AlertDialog.Builder(mainActivity)
            .setView(binding.root)
            .setCancelable(false)
            .show()

        agree.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked) {
                binding.okButton.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.GONE
            } else {
                binding.okButton.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE
            }
        }
        binding.okButton.setOnClickListener {
            appConfig.privatePolicyAccepted = true
            appConfig.yandexMetricaAccepted = binding.agreeWithMetrica.isChecked
//            YandexMetrica.setStatisticsSending(
//                mainActivity.applicationContext,
//                binding.agreeWithMetrica.isChecked
//            )
            dialog.dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            mainActivity.finish()
        }
    }

    fun showEditProfileName(context: Context) {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val binding = EdittextLayoutBinding.inflate(inflater, null, false)
        binding.edit.setText(appConfig.profileName)
        AlertDialog.Builder(context)
            .setTitle(context.getText(R.string.profile_name_title))
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                appConfig.profileName = binding.edit.text.toString()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int -> }
            .show()
    }

    fun checkAndShowLocationDialog(context: Context) {
        if (appConfig.useGps) {
            val mLocationManager = ContextCompat.getSystemService(
                context,
                LocationManager::class.java
            ) as LocationManager
            val mGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!mGPS) {
                AlertDialog.Builder(context, R.style.OriginalTheme_Dialog_Alert)
                    .setMessage(R.string.gpsdisabled_alert)
                    .setPositiveButton(R.string.gotosettings) { _: DialogInterface?, _: Int ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        ContextCompat.startActivity(context, intent, null)
                    }
                    .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
                    .create()
                    .show()
            }

            if (!PermissionsUtil.checkLocationPermission(context)) {
                AlertDialog.Builder(context, R.style.OriginalTheme_Dialog_Alert)
                    .setMessage(R.string.logging_error_no_location_permission)
                    .setPositiveButton(R.string.gotosettings) { _: DialogInterface?, _: Int ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        ContextCompat.startActivity(context, intent, null)
                    }
                    .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
                    .create()
                    .show()
            }
        }
    }

    fun AlertDialog.setBlackIcon(): AlertDialog {
        this.findViewById<ImageView>(android.R.id.icon)
            ?.setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
        return this
    }
}