package de.piobyte.barnearby.util;

import android.os.Build;

public class SystemUtils {

    public static boolean isAtLeastLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}