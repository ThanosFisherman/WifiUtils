package com.thanosfisherman.wifiutils.wifiScan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thanosfisherman.wifiutils.ReceiverCallbacks;


public class WifiScanReceiver extends BroadcastReceiver
{
    private WifiScanCallback callback;

    public WifiScanReceiver(ReceiverCallbacks callback)
    {
        this.callback = (WifiScanCallback) callback;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {

    }
}
