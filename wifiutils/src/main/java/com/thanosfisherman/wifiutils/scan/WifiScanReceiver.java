package com.thanosfisherman.wifiutils.scan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;


public class WifiScanReceiver extends BroadcastReceiver {

    private final WifiScanCallback callback;

    public WifiScanReceiver(@NonNull WifiScanCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        callback.onScanResultsReady();
    }

}
