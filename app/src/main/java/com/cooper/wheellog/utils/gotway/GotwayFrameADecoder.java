package com.cooper.wheellog.utils.gotway;

import static com.cooper.wheellog.utils.gotway.GotwayAdapter.RATIO_GW;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.MathsUtil;

public class GotwayFrameADecoder {

    private WheelData wd;
    private GotwayScaledVoltageCalculator gotwayScaledVoltageCalculator;

    public GotwayFrameADecoder(final WheelData wd, final GotwayScaledVoltageCalculator gotwayScaledVoltageCalculator) {
        this.wd = wd;
        this.gotwayScaledVoltageCalculator = gotwayScaledVoltageCalculator;
    }

    public void decode(byte[] buff, Boolean useRatio, Boolean useBetterPercents, int gotwayNegative) {
        int voltage = MathsUtil.shortFromBytesBE(buff, 2);
        int speed = (int) Math.round(MathsUtil.signedShortFromBytesBE(buff, 4) * 3.6);
        int distance = MathsUtil.shortFromBytesBE(buff, 8);
        int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 10);
        int temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 340.0) + 36.53) * 100);  // mpu6050
        //int temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 333.87) + 21.00) * 100); // mpu6500
        int hwPwm = MathsUtil.signedShortFromBytesBE(buff, 14) * 10;
        if (gotwayNegative == 0) {
            speed = Math.abs(speed);
            phaseCurrent = Math.abs(phaseCurrent);
            hwPwm = Math.abs(hwPwm);
        } else {
            speed = speed * gotwayNegative;
            phaseCurrent = phaseCurrent * gotwayNegative;
            hwPwm = hwPwm * gotwayNegative;
        }

        int battery;
        if (useBetterPercents) {
            if (voltage > 6680) {
                battery = 100;
            } else if (voltage > 5440) {
                battery = (voltage - 5380) / 13;
            } else if (voltage > 5290) {
                battery = (int) Math.round((voltage - 5290) / 32.5);
            } else {
                battery = 0;
            }
        } else {
            if (voltage <= 5290) {
                battery = 0;
            } else if (voltage >= 6580) {
                battery = 100;
            } else {
                battery = (voltage - 5290) / 13;
            }
        }

        if (useRatio) {
            distance = (int) Math.round(distance * RATIO_GW);
            speed = (int) Math.round(speed * RATIO_GW);
        }
        voltage = (int) Math.round(gotwayScaledVoltageCalculator.getScaledVoltage(voltage));

        wd.setSpeed(speed);
        wd.setTopSpeed(speed);
        wd.setWheelDistance(distance);
        wd.setTemperature(temperature);
        wd.setPhaseCurrent(phaseCurrent);
        wd.setVoltage(voltage);
        wd.setVoltageSag(voltage);
        wd.setBatteryLevel(battery);
        wd.updateRideTime();
        wd.setOutput(hwPwm);
    }
}
