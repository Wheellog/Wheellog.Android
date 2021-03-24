package com.cooper.wheellog

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PrivacyPolicyActivity : AppCompatActivity() {
    lateinit var dialog: AlertDialog

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED, Intent())
        val convertView = layoutInflater.inflate(R.layout.privacy_policy, null)
        val agree = convertView.findViewById<CheckBox>(R.id.agree_with_policy)
        val policyText = convertView.findViewById<TextView>(R.id.policy_links)
        policyText.movementMethod = LinkMovementMethod.getInstance()
        policyText.text = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Html.fromHtml(resources.getString(R.string.private_policy), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(resources.getString(R.string.private_policy))
        }
        val ok = convertView.findViewById<Button>(R.id.ok_button)
        val cancel = convertView.findViewById<Button>(R.id.btn_cancel)

        dialog = AlertDialog.Builder(this, R.style.OriginalTheme_Dialog_Alert)
                .setView(convertView)
                .setCancelable(false)
                .show()

        agree.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked) {
                ok.visibility = View.VISIBLE
                cancel.visibility = View.GONE
            } else {
                ok.visibility = View.GONE
                cancel.visibility = View.VISIBLE
            }
        }
        ok.setOnClickListener {
            setResult(RESULT_OK, Intent())
            finish()
        }
        cancel.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}