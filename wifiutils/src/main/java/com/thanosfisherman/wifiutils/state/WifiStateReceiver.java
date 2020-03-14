package com.thanosfisherman.wifiutils.state;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

public final class WifiStateReceiver extends BroadcastReceiver {

    private final WifiStateCallback wifiStateCallback;

    public WifiStateReceiver(@NonNull WifiStateCallback callbacks) {
        wifiStateCallback = callbacks;
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            wifiStateCallback.onWifiEnabled();
        }
    }
}
