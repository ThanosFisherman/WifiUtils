package com.thanosfisherman.wifiutils;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionFailListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;

public interface WifiConnectorBuilder
{
    WifiConnectorBuilder onErrorListener(ConnectionFailListener connectionFailListener);
    WifiConnectorBuilder onSuccess(ConnectionSuccessListener connectionSuccessListener);

    interface WifiState
    {
        ScanBuilder enableWifi();
        void disableWifi();
    }

    interface ScanBuilder
    {
        WifiConnectionBuilder startScan(ScanResultsListener scanResultsListener);
        WifiConnectorBuilder connectWith(String ssid);
        WifiConnectorBuilder connectWith(String ssid, String bssid);
    }

    interface WifiConnectionBuilder
    {
        WifiConnectorBuilder connectWithScanResult(String password);
    }
}
