package com.cooper.wheellog.utils.gotway;

import static com.cooper.wheellog.utils.gotway.GotwayAdapter.RATIO_GW;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;
import com.cooper.wheellog.utils.MathsUtil;

public class GotwayFrameBDecoder {

    private WheelData wd;
    public GotwayFrameBDecoder(
        final WheelData wd
    ) {
        this.wd = wd;
    }

    public AlertResult decode(byte[] buff, Boolean useRatio, int lock, String fw) {
        var _lock = lock;
        int totalDistance = (int) MathsUtil.getInt4(buff, 2);
        if (useRatio) {
            wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
        } else {
            wd.setTotalDistance(totalDistance);
        }
        int settings = MathsUtil.shortFromBytesBE(buff, 6);
        int pedalsMode = (settings >> 13) & 0x03;
        int speedAlarms = (settings >> 10) & 0x03;
        int rollAngle = (settings >> 7) & 0x03;
        int inMiles = settings & 0x01;
        int powerOffTime = MathsUtil.shortFromBytesBE(buff, 8);
        int tiltBackSpeed = MathsUtil.shortFromBytesBE(buff, 10);
        if (tiltBackSpeed >= 100) tiltBackSpeed = 0;
        int alert = buff[12] & 0xFF;
        int ledMode = buff[13] & 0xFF;
        int lightMode = buff[15] & 0x03;
        if (_lock == 0) {
            WheelLog.AppConfig.setPedalsMode(String.valueOf(2 - pedalsMode));
            WheelLog.AppConfig.setAlarmMode(String.valueOf(speedAlarms)); //CheckMe
            WheelLog.AppConfig.setLightMode(String.valueOf(lightMode));
            WheelLog.AppConfig.setLedMode(String.valueOf(ledMode));
            if (!fw.equals("-")) {
                WheelLog.AppConfig.setGwInMiles(inMiles == 1);
                WheelLog.AppConfig.setWheelMaxSpeed(tiltBackSpeed);
                WheelLog.AppConfig.setRollAngle(String.valueOf(rollAngle));
            }
        } else {
            _lock -= 1;
        }
        return new AlertResult(alert, _lock);
    }
    class AlertResult {
        public int alert;
        public int lock;
        public AlertResult(int alert, int lock) {
            this.alert = alert;
            this.lock = lock;
        }
    }
}
