package com.thanosfisherman.wifiutils;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionScanResultsListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;
import com.thanosfisherman.wifiutils.wifiWps.ConnectionWpsListener;

public interface WifiConnectorBuilder
{
    void start();

    interface WifiUtilsListener
    {
        void enableWifi(WifiStateListener wifiStateListener);
        void disableWifi();

        WifiConnectorBuilder scanWifi(@Nullable ScanResultsListener scanResultsListener);
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String password);
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String bssid, @NonNull String password);
        WifiSuccessListener connectWithScanResult(@NonNull String password, @Nullable ConnectionScanResultsListener connectionScanResultsListener);
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiWpsSuccessListener connectWithWps(@NonNull String bssid, @NonNull String password);
    }

    interface WifiSuccessListener
    {
        WifiSuccessListener setTimeout(long timeOutMillis);
        WifiConnectorBuilder onConnectionResult(@Nullable ConnectionSuccessListener successListener);
    }

    interface WifiWpsSuccessListener
    {
        WifiWpsSuccessListener setWpsTimeout(long timeOutMillis);
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiConnectorBuilder onConnectionWpsResult(@Nullable ConnectionWpsListener successListener);
    }
}
