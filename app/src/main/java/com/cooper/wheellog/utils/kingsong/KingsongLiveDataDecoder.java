package com.cooper.wheellog.utils.kingsong;

import static com.cooper.wheellog.utils.kingsong.KingsongAdapter.KS18L_SCALER;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is100vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is126vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is84vWheel;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.MathsUtil;
import com.cooper.wheellog.utils.StringUtil;

import java.util.Locale;

public class KingsongLiveDataDecoder {

    private final WheelData wd;
    private final AppConfig appConfig;

    public KingsongLiveDataDecoder(
            final WheelData wd,
            final AppConfig appConfig
    ) {
        this.wd = wd;
        this.appConfig = appConfig;
    }

    public KingsongLiveDataDecoderResult decode(byte[] data, boolean m18Lkm, int mode) {
        int mMode = mode;
        // Live data
        int voltage = MathsUtil.getInt2R(data, 2);
        wd.setVoltage(voltage);
        wd.setSpeed(MathsUtil.getInt2R(data, 4));
        wd.setTotalDistance(MathsUtil.getInt4R(data, 6));
        if ((wd.getModel().compareTo("KS-18L") == 0) && !m18Lkm) {
            wd.setTotalDistance(Math.round(wd.getTotalDistance() * KS18L_SCALER));
        }
        wd.setCurrent((data[10] & 0xFF) + (data[11] << 8));

        wd.setTemperature(MathsUtil.getInt2R(data, 12));
        wd.setVoltageSag(voltage);
        if ((data[15] & 255) == 224) {
            mMode = data[14];
            wd.setModeStr(String.format(Locale.US, "%d", mMode));
        }

        int battery;
        boolean useBetterPercents = appConfig.getUseBetterPercents();
        if (is84vWheel(wd)) {
            if (useBetterPercents) {
                if (voltage > 8350) {
                    battery = 100;
                } else if (voltage > 6800) {
                    battery = (voltage - 6650) / 17;
                } else if (voltage > 6400) {
                    battery = (voltage - 6400) / 45;
                } else {
                    battery = 0;
                }
            } else {
                if (voltage < 6250) {
                    battery = 0;
                } else if (voltage >= 8250) {
                    battery = 100;
                } else {
                    battery = (voltage - 6250) / 20;
                }
            }
        } else if (is126vWheel(wd)) {
            if (useBetterPercents) {
                if (voltage > 12525) {
                    battery = 100;
                } else if (voltage > 10200) {
                    battery = (int) Math.round((voltage - 9975) / 25.5);
                } else if (voltage > 9600) {
                    battery = (int) Math.round((voltage - 9600) / 67.5);
                } else {
                    battery = 0;
                }
            } else {
                if (voltage < 9375) {
                    battery = 0;
                } else if (voltage >= 12375) {
                    battery = 100;
                } else {
                    battery = (voltage - 9375) / 30;
                }
            }
        } else if (is100vWheel(wd)) {
            if (useBetterPercents) {
                if (voltage > 10020) {
                    battery = 100;
                } else if (voltage > 8160) {
                    battery = (int) Math.round((voltage - 7980) / 20.4);
                } else if (voltage > 7680) {
                    battery = (int) Math.round((voltage - 7680) / 54.0);
                } else {
                    battery = 0;
                }
            } else {
                if (voltage < 7500) {
                    battery = 0;
                } else if (voltage >= 9900) {
                    battery = 100;
                } else {
                    battery = (voltage - 7500) / 24;
                }
            }

        } else {
            if (useBetterPercents) {
                if (voltage > 6680) {
                    battery = 100;
                } else if (voltage > 5440) {
                    battery = (int) Math.round((voltage - 5320) / 13.6);
                } else if (voltage > 5120) {
                    battery = (voltage - 5120) / 36;
                } else {
                    battery = 0;
                }
            } else {
                if (voltage < 5000) {
                    battery = 0;
                } else if (voltage >= 6600) {
                    battery = 100;
                } else {
                    battery = (voltage - 5000) / 16;
                }
            }
        }
        wd.setBatteryLevel(battery);
        KingsongLiveDataDecoderResult result = new KingsongLiveDataDecoderResult(mMode);
        return result;
    }

    record KingsongLiveDataDecoderResult(int mode) {
    }
}
