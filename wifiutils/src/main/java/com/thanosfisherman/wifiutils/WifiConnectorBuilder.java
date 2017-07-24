package com.thanosfisherman.wifiutils;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionStateListener;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;

public interface WifiConnectorBuilder
{
    void start();

    interface WifiState
    {
        void enableWifi(WifiStateListener wifiStateListener);
        void disableWifi();

        WifiConnectionBuilder wifiScan(ScanResultsListener scanResultsListener);
        WifiConnectorBuilder connectWith(String ssid, ConnectionStateListener connectionStateListener);
        WifiConnectorBuilder connectWith(String ssid, String bssid, ConnectionStateListener connectionStateListener);
    }

    interface WifiConnectionBuilder
    {
        WifiConnectorBuilder connectWithScanResult(String password, ConnectionStateListener connectionStateListener);
        void start();
    }
}
