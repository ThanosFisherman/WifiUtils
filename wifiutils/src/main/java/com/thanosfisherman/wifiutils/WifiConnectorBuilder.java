package com.thanosfisherman.wifiutils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionScanResultsListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;

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
    }

    interface WifiSuccessListener
    {
        WifiSuccessListener setTimeout(long delayMillis);
        WifiConnectorBuilder onConnectionResult(ConnectionSuccessListener successListener);
    }
}
