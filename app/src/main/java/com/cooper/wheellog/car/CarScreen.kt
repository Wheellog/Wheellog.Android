package com.cooper.wheellog.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*

class CarScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val row = Row.Builder().setTitle("Wheellog empty screen").build()
        return PaneTemplate.Builder(Pane.Builder().addRow(row).build())
                .setHeaderAction(Action.APP_ICON)
                .build()
    }
}