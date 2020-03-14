package com.thanosfisherman.wifiutils.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import com.thanosfisherman.elvis.Objects;
import com.thanosfisherman.wifiutils.WeakHandler;

import static com.thanosfisherman.elvis.Elvis.of;
import static com.thanosfisherman.wifiutils.ConnectorUtils.isAlreadyConnected;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reEnableNetworkIfPossible;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver {

    private final WifiConnectionCallback wifiConnectionCallback;
    private final WifiManager wifiManager;
    private final WeakHandler handler;

    private ScanResult scanResult;

    @NonNull
    private final Runnable handlerCallback = new Runnable() {
        @Override
        public void run() {
            wifiLog("Connection Timed out...");
            reEnableNetworkIfPossible(wifiManager, scanResult);
            if (isAlreadyConnected(wifiManager, of(scanResult).next(scanResult -> scanResult.BSSID).get())) {
                wifiConnectionCallback.successfulConnect();
            } else {
                wifiConnectionCallback.errorConnect();
            }
            handler.removeCallbacks(this);
        }
    };

    private long delay;

    public WifiConnectionReceiver(@NonNull WifiConnectionCallback callback,
                                  @NonNull WifiManager wifiManager,
                                  long delayMillis) {
        this.wifiConnectionCallback = callback;
        this.wifiManager = wifiManager;
        this.delay = delayMillis;
        this.handler = new WeakHandler();
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        wifiLog("Connection Broadcast action: " + action);
        if (Objects.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION, action)) {
            /*
                Note here we dont check if has internet connectivity, because we only validate
                if the connection to the hotspot is active, and not if the hotspot has internet.
             */
            if (isAlreadyConnected(wifiManager, of(scanResult).next(scanResult -> scanResult.BSSID).get())) {
                handler.removeCallbacks(handlerCallback);
                wifiConnectionCallback.successfulConnect();
            }
        } else if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (state == null) {
                handler.removeCallbacks(handlerCallback);
                wifiConnectionCallback.errorConnect();
                return;
            }

            wifiLog("Connection Broadcast action: " + state);

            switch (state) {
                case COMPLETED:
                case FOUR_WAY_HANDSHAKE:
                    if (isAlreadyConnected(wifiManager, of(scanResult).next(scanResult -> scanResult.BSSID).get())) {
                        handler.removeCallbacks(handlerCallback);
                        wifiConnectionCallback.successfulConnect();
                    }
                    break;
                case DISCONNECTED:
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                        wifiLog("Authentication error...");
                        handler.removeCallbacks(handlerCallback);
                        wifiConnectionCallback.errorConnect();
                    } else {
                        wifiLog("Disconnected. Re-attempting to connect...");
                        reEnableNetworkIfPossible(wifiManager, scanResult);
                    }
            }
        }
    }

    public void setTimeout(long millis) {
        this.delay = millis;
    }

    @NonNull
    public WifiConnectionReceiver activateTimeoutHandler(@NonNull ScanResult result) {
        scanResult = result;
        handler.postDelayed(handlerCallback, delay);
        return this;
    }

}
