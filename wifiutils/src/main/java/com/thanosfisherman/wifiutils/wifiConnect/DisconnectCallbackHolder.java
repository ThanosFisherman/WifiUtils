package com.thanosfisherman.wifiutils.wifiConnect;

import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

/**
 * Class to keep an instance to the network callback so we can easily disconnect on Android 10+.
 */
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

    public void addNetworkCallback(@Nullable ConnectivityManager.NetworkCallback networkCallback) {
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
