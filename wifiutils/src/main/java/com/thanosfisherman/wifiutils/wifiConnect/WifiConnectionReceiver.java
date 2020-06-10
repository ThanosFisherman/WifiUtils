package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

import com.thanosfisherman.elvis.Objects;
import com.thanosfisherman.wifiutils.WeakHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.thanosfisherman.elvis.Elvis.of;
import static com.thanosfisherman.wifiutils.ConnectorUtils.isAlreadyConnected;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reEnableNetworkIfPossible;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver implements ConnectionHandler {
    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback;
    @Nullable
    private ScanResult mScanResult;
    @NonNull
    private final WifiManager mWifiManager;
    private long mDelay;
    @NonNull
    private final WeakHandler handler;
    @NonNull
    private final Runnable timeoutCallback = new Runnable() {
        @Override
        public void run() {
            wifiLog("Connection Timed out...");
            reEnableNetworkIfPossible(mWifiManager, mScanResult);
            if (isAlreadyConnected(mWifiManager, of(mScanResult).next(scanResult -> scanResult.BSSID).get())) {
                mWifiConnectionCallback.successfulConnect();
            } else {
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.TIMEOUT_OCCURRED);
            }
            handler.removeCallbacks(this);
        }
    };

    public WifiConnectionReceiver(@NonNull final WifiConnectionCallback callback, @NonNull final WifiManager wifiManager, final long delayMillis) {
        this.mWifiConnectionCallback = callback;
        this.mWifiManager = wifiManager;
        this.mDelay = delayMillis;
        this.handler = new WeakHandler();
    }

    @Override
    public void onReceive(final Context context, @NonNull final Intent intent) {
        final String action = intent.getAction();
        wifiLog("Connection Broadcast action: " + action);
        if (Objects.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION, action)) {
            /*
                Note here we don't check if has internet connectivity, because we only validate
                if the connection to the hotspot is active, and not if the hotspot has internet.
             */
            if (isAlreadyConnected(mWifiManager, of(mScanResult).next(scanResult -> scanResult.BSSID).get())) {
                handler.removeCallbacks(timeoutCallback);
                mWifiConnectionCallback.successfulConnect();
            }
        } else if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (state == null) {
                handler.removeCallbacks(timeoutCallback);
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.COULD_NOT_CONNECT);
                return;
            }

            wifiLog("Connection Broadcast state: " + state);

            switch (state) {
                case COMPLETED:
                case FOUR_WAY_HANDSHAKE:
                    if (isAlreadyConnected(mWifiManager, of(mScanResult).next(scanResult -> scanResult.BSSID).get())) {
                        handler.removeCallbacks(timeoutCallback);
                        mWifiConnectionCallback.successfulConnect();
                    }
                    break;
                case DISCONNECTED:
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                        wifiLog("Authentication error...");
                        handler.removeCallbacks(timeoutCallback);
                        mWifiConnectionCallback.errorConnect(ConnectionErrorCode.AUTHENTICATION_ERROR_OCCURRED);
                    } else {
                        wifiLog("Disconnected. Re-attempting to connect...");
                        reEnableNetworkIfPossible(mWifiManager, mScanResult);
                    }
            }
        }
    }

    public void setTimeout(long millis) {
        this.mDelay = millis;
    }

    @NonNull
    @Override
    public WifiConnectionReceiver connectWith(@NonNull ScanResult result, @NonNull String password, @NonNull ConnectivityManager connectivityManager) {
        mScanResult = result;
        handler.postDelayed(timeoutCallback, mDelay);
        return this;
    }
}
