package com.thanosfisherman.wifiutils.android10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thanosfisherman.elvis.Objects;
import com.thanosfisherman.wifiutils.ConfigSecurities;
import com.thanosfisherman.wifiutils.WeakHandler;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionHandler;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

@RequiresApi(Build.VERSION_CODES.Q)
public class Android10WifiConnectionReceiver extends BroadcastReceiver implements ConnectionHandler {
    @NonNull
    private final DisconnectCallbackHolder mDisconnectCallbackHolder;
    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback;
    @Nullable
    private ConnectivityManager mConnectivityManager;
    @Nullable
    ConnectivityManager.NetworkCallback mNetworkCallback;
    private long mDelay;
    @NonNull
    private final WeakHandler handler;
    @NonNull
    private final Runnable timeoutCallback = new Runnable() {
        @Override
        public void run() {
            wifiLog("Connection Timed out...");
            mWifiConnectionCallback.errorConnect(ConnectionErrorCode.TIMEOUT_OCCURRED);
            handler.removeCallbacks(this);
            cancelConnectingIfNeeded();
        }
    };

    public Android10WifiConnectionReceiver(@NonNull final DisconnectCallbackHolder disconnectCallbackHolder, @NonNull final WifiConnectionCallback callback, final long delayMillis) {
        this.mDisconnectCallbackHolder = disconnectCallbackHolder;
        this.mWifiConnectionCallback = callback;
        this.mDelay = delayMillis;
        wifiLog(String.valueOf(delayMillis));
        this.handler = new WeakHandler();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        wifiLog("Connection Broadcast action: " + action);

        if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int suppl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            wifiLog("Connection Broadcast state: " + state);

            if (state == null) {
                return;
            }

            if (state == SupplicantState.DISCONNECTED && suppl_error == WifiManager.ERROR_AUTHENTICATING) {
                wifiLog("Authentication error...");
                handler.removeCallbacks(timeoutCallback);
                cancelConnectingIfNeeded();
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.AUTHENTICATION_ERROR_OCCURRED);
            }
        }
    }

    //region ConnectionHandler

    @NonNull
    @Override
    public BroadcastReceiver connectWith(@NonNull ScanResult result, @NonNull final String password, @NonNull ConnectivityManager connectivityManager) {
        mConnectivityManager = connectivityManager;

        // schedule timeout behavior
        handler.postDelayed(timeoutCallback, mDelay);

        // start connection logic
        connectWith(connectivityManager, result, password);

        return this;
    }

    @Override
    public void setTimeout(long millis) {
        mDelay = millis;
    }

    //endregion

    private void connectWith(@NonNull ConnectivityManager connectivityManager, @NonNull ScanResult scanResult, @NonNull final String password) {
        WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder = new WifiNetworkSpecifier.Builder()
                .setSsid(scanResult.SSID)
                .setBssid(MacAddress.fromString(scanResult.BSSID));

        final String security = ConfigSecurities.getSecurity(scanResult);

        ConfigSecurities.setupWifiNetworkSpecifierSecurities(wifiNetworkSpecifierBuilder, security, password);

        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifierBuilder.build())
                .build();

        // cleanup previous connections just in case
        mDisconnectCallbackHolder.disconnect(connectivityManager);

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);

                wifiLog("AndroidQ+ connected to wifi ");

                // TODO: should this actually be in the success listener on WifiUtils?
                // We could pass the networkrequest maybe?

                // bind so all api calls are performed over this new network
                // if we don't bind, connection with the wifi network is immediately dropped
                connectivityManager.bindProcessToNetwork(network);

                handler.removeCallbacks(timeoutCallback);
                mWifiConnectionCallback.successfulConnect();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();

                wifiLog("AndroidQ+ could not connect to wifi");

                handler.removeCallbacks(timeoutCallback);
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.USER_CANCELLED);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                wifiLog("onLost");

                connectivityManager.bindProcessToNetwork(null);
                mNetworkCallback = null;
            }
        };

        mDisconnectCallbackHolder.addNetworkCallback(mNetworkCallback);
        connectivityManager.requestNetwork(networkRequest, mNetworkCallback);
    }

    private void cancelConnectingIfNeeded() {
        if (mConnectivityManager != null && mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }

        mDisconnectCallbackHolder.addNetworkCallback(null);
    }
}
