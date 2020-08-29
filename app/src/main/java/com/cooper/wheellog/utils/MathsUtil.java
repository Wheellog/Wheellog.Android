package com.cooper.wheellog.utils;

import android.content.Context;
import android.util.TypedValue;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MathsUtil {
    public static double kmToMiles(double km) {
        return km * 0.62137119;
    }

    public static float kmToMiles(float km) {
        return km * 0.62137119F;
    }

    public static int dpToPx(@NotNull Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static int getInt2(byte[] arr, int offset) {
        return ByteBuffer.wrap(arr, offset, 2).getShort();
    }

    public static long getInt4(byte[] arr, int offset) {
        return ByteBuffer.wrap(arr, offset, 4).getInt();
    }

    @NotNull
    public static byte[] getBytes(short input) {
        return ByteBuffer.allocate(2).putShort(input).array();
    }

    @NotNull
    public static byte[] getBytes(int input) {
        return ByteBuffer.allocate(4).putInt(input).array();
    }

    @NotNull
    public static byte[] reverseEvery2(@NotNull byte[] input) {
        byte[] result = Arrays.copyOf(input, input.length);
        for (int i = 0; i < result.length - 1; i += 2) {
            byte temp = result[i];
            result[i] = result[i + 1];
            result[i + 1] = temp;
        }
        return result;
    }
}
