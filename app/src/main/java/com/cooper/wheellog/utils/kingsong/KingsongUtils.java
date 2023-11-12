package com.cooper.wheellog.utils.kingsong;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.StringUtil;

public class KingsongUtils {

    static boolean is84vWheel(WheelData wd) {
        return StringUtil.inArray(wd.getModel(), new String[]{"KS-18L", "KS-16X", "KS-16XF", "RW", "KS-18LH", "KS-18LY", "KS-S18"})
                || wd.getName().startsWith("ROCKW") // support rockwheel models
                || wd.getBtName().compareTo("RW") == 0;
    }

    static boolean is126vWheel(WheelData wd) {
        return StringUtil.inArray(wd.getModel(), new String[]{"KS-S20", "KS-S22"});
    }

    static boolean is100vWheel(WheelData wd) {
        return StringUtil.inArray(wd.getModel(), new String[]{"KS-S19"});
    }

    static byte[] getEmptyRequest() {
        return new byte[]{(byte) 0xAA, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x14, 0x5A, 0x5A};
    }
}
