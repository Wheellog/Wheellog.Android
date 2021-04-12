package com.cooper.wheellog.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator


class BaseCarAppService : CarAppService() {
    private val session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen {
            CarToast.makeText(
                    carContext,
                    "Wheellog forever!",
                    CarToast.LENGTH_LONG
            ).show()
            return CarScreen(carContext)
        }
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return session
    }
}