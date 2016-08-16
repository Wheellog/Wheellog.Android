package com.cooper.wheellog.utils;

import android.content.Context;
import android.util.TypedValue;

public class MathsUtil {
    public static double kmToMiles(double km) {
        return km * 0.62137119;
    }


    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

}
