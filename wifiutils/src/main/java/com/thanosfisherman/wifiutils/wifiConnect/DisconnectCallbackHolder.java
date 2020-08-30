package com.thanosfisherman.wifiutils.wifiConnect;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

/**
 * Singleton Class to keep references of {@link ConnectivityManager} and {@link ConnectivityManager.NetworkCallback}
 * so that we can easily bind/unbiding process from Network and disconnect on Android 10+.
 */
@RequiresApi(Build.VERSION_CODES.Q)
public class DisconnectCallbackHolder {
    @Nullable
    private static volatile DisconnectCallbackHolder sInstance;
    @Nullable
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    @Nullable
    private ConnectivityManager mConnectivityManager;

    private boolean isNetworkcallbackAdded;

    private boolean isProcessBoundToNetwork;

    private DisconnectCallbackHolder() {
    }

    /**
     * Gets a Singleton instance of DisconnectCallbackHolder.
     * This is a Lazy and Thread safe Singleton with Double-check locking
     *
     * @return DisconnectCallbackHolder Singleton instance
     */
    public static DisconnectCallbackHolder getInstance() {
        if (sInstance == null) {
            synchronized (DisconnectCallbackHolder.class) {
                if (sInstance == null) {
                    sInstance = new DisconnectCallbackHolder();
                }
            }
        }
        return sInstance;
    }

    /**
     * Keeps a reference of {@link ConnectivityManager} and {@link ConnectivityManager.NetworkCallback}
     * This method must be called before anything else.
     *
     * @param networkCallback     the networkcallback class to keep a reference of
     * @param connectivityManager the ConnectivityManager
     */
    public void addNetworkCallback(@NonNull ConnectivityManager.NetworkCallback networkCallback, @NonNull ConnectivityManager connectivityManager) {
        mNetworkCallback = networkCallback;
        mConnectivityManager = connectivityManager;
        isNetworkcallbackAdded = true;
    }

    /**
     * Disconnects from Network and nullifies networkcallback meaning you will have to
     * call {@link DisconnectCallbackHolder#addNetworkCallback(ConnectivityManager.NetworkCallback, ConnectivityManager)} again
     * next time you want to connect again.
     */
    public void disconnect() {
        if (mNetworkCallback != null && mConnectivityManager != null) {
            wifiLog("Disconnecting on Android 10+");
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
            isNetworkcallbackAdded = false;
        }
    }

    /**
     * See {@link ConnectivityManager#requestNetwork(NetworkRequest, ConnectivityManager.NetworkCallback) }
     *
     * @param networkRequest {@link NetworkRequest}
     */
    public void requestNetwork(NetworkRequest networkRequest) {
        if (mNetworkCallback != null && mConnectivityManager != null) {
            mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback);
        } else {
            wifiLog("NetworkCallback has not been added yet. Please call addNetworkCallback method first");
        }
    }

    /**
     * Unbinds the previously bound Network from the process.
     */
    public void unbindProcessFromNetwork() {
        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(null);
            isProcessBoundToNetwork = false;
        } else {
            wifiLog("ConnectivityManager is null. Did you call addNetworkCallback method first?");
        }
    }


    /**
     * binds so all api calls performed over this new network
     * if we don't bind, connection with the wifi network is immediately dropped
     */
    public void bindProcessToNetwork(@NonNull Network network) {

        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(network);
            isProcessBoundToNetwork = true;
        } else {
            wifiLog("ConnectivityManager is null. Did you call addNetworkCallback method first?");
        }
    }

    /**
     * Checks whether {@link DisconnectCallbackHolder#addNetworkCallback(ConnectivityManager.NetworkCallback, ConnectivityManager)}
     * is called
     *
     * @return true if networkcallback is initialized false otherwise.
     */
    public boolean isNetworkcallbackAdded() {
        return isNetworkcallbackAdded;
    }

    /**
     * Checks whether {@link DisconnectCallbackHolder#bindProcessToNetwork(Network)}
     * is called
     *
     * @return true if bound false otherwise.
     */
    public boolean isProcessBoundToNetwork() {
        return isProcessBoundToNetwork;
    }
}
