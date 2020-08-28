package com.thanosfisherman.wifiutils.utils;

import android.os.Build;

public class VersionUtils {
    private VersionUtils() {

    }

    public static boolean isJellyBeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    public static boolean isLollipopOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isMarshmallowOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isAndroidQOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
