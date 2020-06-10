package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;

public interface ConnectionHandler {
    @NonNull
    BroadcastReceiver connectWith(@NonNull ScanResult result, @NonNull String password, @NonNull ConnectivityManager connectivityManager);

    void setTimeout(long millis);
}
