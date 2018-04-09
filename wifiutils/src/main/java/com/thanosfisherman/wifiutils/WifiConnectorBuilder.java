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

    interface WifiUtilsBuilder
    {
        void enableWifi(WifiStateListener wifiStateListener);
        void enableWifi();
        void disableWifi();

        @NonNull
        WifiConnectorBuilder scanWifi(@Nullable ScanResultsListener scanResultsListener);
        @NonNull
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String password);
        @NonNull
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String bssid, @NonNull String password);
        @NonNull
        WifiSuccessListener connectWithScanResult(@NonNull String password, @Nullable ConnectionScanResultsListener connectionScanResultsListener);
        @NonNull
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiWpsSuccessListener connectWithWps(@NonNull String bssid, @NonNull String password);
        void cancelAutoConnect();
    }

    interface WifiSuccessListener
    {
        @NonNull
        WifiSuccessListener setTimeout(long timeOutMillis);
        @NonNull
        WifiConnectorBuilder onConnectionResult(@Nullable ConnectionSuccessListener successListener);
    }

    interface WifiWpsSuccessListener
    {
        @NonNull
        WifiWpsSuccessListener setWpsTimeout(long timeOutMillis);
        @NonNull
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiConnectorBuilder onConnectionWpsResult(@Nullable ConnectionWpsListener successListener);
    }
}
