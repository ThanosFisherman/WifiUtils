package com.thanosfisherman.wifiutils.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;

public class SSIDUtils {
    @NonNull
    public static String convertToQuotedString(@NonNull String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return "";
        }

        final int lastPos = ssid.length() - 1;
        if (lastPos < 0 || (ssid.charAt(0) == '"' && ssid.charAt(lastPos) == '"')) {
            return ssid;
        }

        return "\"" + ssid + "\"";
    }
}
