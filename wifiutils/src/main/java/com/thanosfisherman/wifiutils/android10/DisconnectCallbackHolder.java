package com.thanosfisherman.wifiutils.android10;

import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

@RequiresApi(Build.VERSION_CODES.Q)
public class DisconnectCallbackHolder {
    @Nullable private static DisconnectCallbackHolder instance;
    @Nullable
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    private DisconnectCallbackHolder() {}

    public static synchronized DisconnectCallbackHolder getInstance() {
        if (instance == null) {
            instance = new DisconnectCallbackHolder();
        }

        return instance;
    }

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
