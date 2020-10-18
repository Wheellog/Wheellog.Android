package com.cooper.wheellog.utils;

import android.content.Context;
import android.util.TypedValue;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

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

    public static int getInt2R(byte[] arr, int offset) {
        return ByteBuffer.wrap(reverseEvery2(arr, offset, 2), 0, 2).getShort();
    }

    public static long getInt4(byte[] arr, int offset) {
        return ByteBuffer.wrap(arr, offset, 4).getInt();
    }

    public static int getInt4R(byte[] arr, int offset) {
        return ByteBuffer.wrap(reverseEvery2(arr, offset, 4), 0, 4).getInt();
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
        return reverseEvery2(input, 0, input.length);
    }

    @NotNull
    public static byte[] reverseEvery2(@NotNull byte[] input, int offset, int len) {
        byte[] result = new byte[len];
        System.arraycopy(input, offset, result, 0, len);
        for (int i = 0; i < len - 1; i += 2) {
            byte temp = result[i];
            result[i] = result[i + 1];
            result[i + 1] = temp;
        }
        return result;
    }

    @NotNull
    public static long signedIntFromBytesLE(byte[] bytes, int starting) {
        if (bytes.length >= starting + 4) {
            return (((bytes[starting + 3] & 0xFF) << 24) | ((bytes[starting + 2] & 0xFF) << 16) | ((bytes[starting + 1] & 0xFF) << 8) | (bytes[starting] & 0xFF));
        }
        return 0;
    }

    @NotNull
    public static int intFromBytesLE(byte[] bytes, int starting) {
        if (bytes.length >= starting + 4) {
            return (((bytes[starting + 3] & 0xFF) << 24) | ((bytes[starting + 2] & 0xFF) << 16) | ((bytes[starting + 1] & 0xFF) << 8) | (bytes[starting] & 0xFF));
        }
        return 0;
    }

    @NotNull
    public static int shortFromBytesLE(byte[] bytes, int starting) {
        if (bytes.length >= starting + 2) {
            return ((bytes[starting+1] & 0xFF) << 8) | (bytes[starting] & 0xFF);
        }
        return 0;
    }

    @NotNull
    public static int signedShortFromBytesLE(byte[] bytes, int starting) {
        if (bytes.length >= starting + 2) {
            return ((bytes[starting+1] << 8) | (bytes[starting] & 0xFF));
        }
        return 0;
    }

}
