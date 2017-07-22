package com.thanosfisherman.wifiutils.wifiScan;


import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

public interface ScanResultsListener
{
    @Nullable
    ScanResult onScanResults(@NonNull List<ScanResult> scanResults);
}
