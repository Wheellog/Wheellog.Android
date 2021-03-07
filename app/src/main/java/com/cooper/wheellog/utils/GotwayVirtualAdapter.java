package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;
import timber.log.Timber;

public class GotwayVirtualAdapter extends BaseAdapter {
    private static GotwayVirtualAdapter INSTANCE;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Begode_Gotway_detect");
        WheelData wd = WheelData.getInstance();
        boolean result = false;
        if ((data[0] == (byte) 0xDC) && (data[1] == (byte) 0x5A) && (data[2] == (byte) 0x5C) && (data[3] == (byte)0x20)) {
            wd.setWheelType(Constants.WHEEL_TYPE.VETERAN);
            wd.setModel("Veteran");
            result = VeteranAdapter.getInstance().setContext(mContext).decode(data);
        } else if ((data[0] == (byte) 0x55) && (data[1] == (byte) 0xAA)) {
            wd.setWheelType(Constants.WHEEL_TYPE.GOTWAY);
            wd.setModel("Begode");
            result = GotwayAdapter.getInstance().setContext(mContext).decode(data);
        } else return false;

        return result;
    }

    public static GotwayVirtualAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GotwayVirtualAdapter();
        }
        return INSTANCE;
    }
}