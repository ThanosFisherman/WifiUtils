package com.thanosfisherman.wifiutils.android10;

import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

@RequiresApi(Build.VERSION_CODES.Q)
public class DisconnectCallback {
    @Nullable
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    void addNetworkCallback(@Nullable ConnectivityManager.NetworkCallback networkCallback) {
        mNetworkCallback = networkCallback;
    }

    public void disconnect(@NonNull ConnectivityManager connectivityManager) {
        if (mNetworkCallback != null) {
            wifiLog("Disconnecting on Android 10+");
            connectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }
}
