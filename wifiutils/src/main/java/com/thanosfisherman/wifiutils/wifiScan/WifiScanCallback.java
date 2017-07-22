package com.thanosfisherman.wifiutils.wifiScan;

import android.net.wifi.ScanResult;

import com.thanosfisherman.wifiutils.ReceiverCallbacks;

import java.util.List;

public interface WifiScanCallback extends ReceiverCallbacks
{
    void onScanResultsReady(List<ScanResult> scanResults);
}
