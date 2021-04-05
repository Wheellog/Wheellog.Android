package com.cooper.wheellog

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout


class LoginActivity : AppCompatActivity() {
    lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED, Intent())
        val nullParent: ViewGroup? = null
        val convertView = layoutInflater.inflate(R.layout.activity_login, nullParent)
        val email = convertView.findViewById<TextInputLayout>(R.id.editTextTextEmailAddress)
        val password = convertView.findViewById<TextInputLayout>(R.id.editTextTextPassword)
        val ok = convertView.findViewById<Button>(R.id.ok_button)
        val cancel = convertView.findViewById<Button>(R.id.btn_cancel)
        val title = convertView.findViewById<TextView>(R.id.alertTitle)
        title.text = "electro.club"

        dialog = AlertDialog.Builder(this, R.style.OriginalTheme_Dialog_Alert)
                .setView(convertView)
                .setCancelable(false)
                .show()

        ok.setOnClickListener {
            ElectroClub.instance.login(
                    email.editText?.text.toString(),
                    password.editText?.text.toString()
            ) {
                this.runOnUiThread {
                    if (it) {
                        setResult(RESULT_OK, Intent())
                        finish()
                    } else {
                        password.startAnimation(shakeError())
                    }
                }
            }
        }

        cancel.setOnClickListener {
            finish()
        }
    }

    private fun shakeError(): TranslateAnimation? {
        val shake = TranslateAnimation(0F, 15F, 0F, 10F)
        shake.duration = 500
        shake.interpolator = CycleInterpolator(7F)
        return shake
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}