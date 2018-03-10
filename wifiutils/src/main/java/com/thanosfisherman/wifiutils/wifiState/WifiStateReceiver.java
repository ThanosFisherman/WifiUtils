package com.thanosfisherman.wifiutils.wifiState;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

public final class WifiStateReceiver extends BroadcastReceiver
{

    @NonNull private final WifiStateCallback wifiStateCallback;

    public WifiStateReceiver(@NonNull WifiStateCallback callbacks)
    {
        wifiStateCallback = callbacks;
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent)
    {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

        switch (wifiState)
        {
            case WifiManager.WIFI_STATE_ENABLED:
                wifiStateCallback.onWifiEnabled();
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                break;
        }
    }
}
