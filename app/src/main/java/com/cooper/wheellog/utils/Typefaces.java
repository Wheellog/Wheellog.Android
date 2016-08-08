package com.cooper.wheellog.utils;

import android.content.Context;
import android.graphics.Typeface;
import java.util.Hashtable;

import timber.log.Timber;

public class Typefaces {

    private static final Hashtable<String, Typeface> cache = new Hashtable<>();

    public static Typeface get(Context c, String assetPath) {
        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(c.getAssets(),
                            assetPath);
                    cache.put(assetPath, t);
                    Timber.i("Loaded '%s'", assetPath);
                } catch (Exception e) {
                    Timber.e("Could not get typeface '%s' because %s", assetPath, e.getMessage());
                    return null;
                }
            }
            return cache.get(assetPath);
        }
    }
}