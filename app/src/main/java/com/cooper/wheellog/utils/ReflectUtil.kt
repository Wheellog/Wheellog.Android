package com.cooper.wheellog.utils;

import java.lang.reflect.Field;

public class ReflectUtil {

    public static boolean SetPrivateField(Object object, String propertyName, Object value) {
        try {
            Field wdField = object.getClass().getDeclaredField(propertyName);
            wdField.setAccessible(true);
            wdField.set(object, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
