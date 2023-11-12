package com.cooper.wheellog.utils.kingsong;

import static com.cooper.wheellog.utils.kingsong.KingsongAdapter.KS18L_SCALER;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is100vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is126vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is84vWheel;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.MathsUtil;

import java.util.Locale;

public class KingSongLiveDataDecoder {

    private final WheelData wd;
    private final KingSongBatteryCalculator kingSongBatteryCalculator;

    public KingSongLiveDataDecoder(
            final WheelData wd,
            final KingSongBatteryCalculator kingSongBatteryCalculator
    ) {
        this.wd = wd;
        this.kingSongBatteryCalculator = kingSongBatteryCalculator;
    }

    public int decode(byte[] data, boolean m18Lkm, int mode) {
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
        kingSongBatteryCalculator.calculateAndStoreBatteryLevel(voltage);
        return mMode;
    }
}
