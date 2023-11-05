package com.cooper.wheellog.utils.gotway;

import com.cooper.wheellog.WheelLog;

public class GotwayScaledVoltageCalculator {

    public double getScaledVoltage(double value) {
        int voltage = 0;
        double scaler = 1.0;
        if (!WheelLog.AppConfig.getGotwayVoltage().equals("")) {
            voltage = Integer.parseInt(WheelLog.AppConfig.getGotwayVoltage());
        }
        switch (voltage) {
            case 0 -> scaler = 1.0;
            case 1 -> scaler = 1.25;
            case 2 -> scaler = 1.5;
            case 3 -> scaler = 1.7380952380952380952380952380952;
            case 4 -> scaler = 2.0;
        }
        return value * scaler;
    }
}
