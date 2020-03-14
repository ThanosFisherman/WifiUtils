package com.thanosfisherman.wifiutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thanosfisherman.wifiutils.connect.ConnectionScanResultsListener;
import com.thanosfisherman.wifiutils.connect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.connect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.connect.WifiConnectionReceiver;
import com.thanosfisherman.wifiutils.scan.ScanResultsListener;
import com.thanosfisherman.wifiutils.scan.WifiScanCallback;
import com.thanosfisherman.wifiutils.scan.WifiScanReceiver;
import com.thanosfisherman.wifiutils.state.WifiStateCallback;
import com.thanosfisherman.wifiutils.state.WifiStateListener;
import com.thanosfisherman.wifiutils.state.WifiStateReceiver;
import com.thanosfisherman.wifiutils.wps.ConnectionWpsListener;

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.elvis.Elvis.of;
import static com.thanosfisherman.wifiutils.ConnectorUtils.cleanPreviousConfiguration;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectWps;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResult;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultBssid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultSsid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reenableAllHotspots;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;

@SuppressLint("MissingPermission")
public final class WifiUtils implements WifiConnectorBuilder,
        WifiConnectorBuilder.WifiUtilsBuilder,
        WifiConnectorBuilder.WifiSuccessListener,
        WifiConnectorBuilder.WifiWpsSuccessListener {

    private static final String TAG = WifiUtils.class.getSimpleName();

    private final WifiManager wifiManager;

    private final Context context;

    private static boolean enableLog;

    private long wpsTimeoutMillis = 30000;
    private long timeoutMillis = 30000;

    private final WifiStateReceiver wifiStateReceiver;
    private final WifiConnectionReceiver wifiConnectionReceiver;
    private final WifiScanReceiver wifiScanReceiver;

    private String ssid;
    private String bssid;
    private String password;

    private ScanResult singleScanResult;
    private ScanResultsListener scanResultsListener;

    private ConnectionScanResultsListener connectionScanResultsListener;
    private ConnectionSuccessListener connectionSuccessListener;
    private ConnectionWpsListener connectionWpsListener;

    private WifiStateListener wifiStateListener;

    private final WifiStateCallback wifiStateCallback = new WifiStateCallback() {
        @Override
        public void onWifiEnabled() {
            wifiLog("WIFI ENABLED...");
            unregisterReceiver(context, wifiStateReceiver);
            of(wifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(true));

            if (scanResultsListener != null || password != null) {
                wifiLog("START SCANNING....");
                if (wifiManager.startScan()) {
                    registerReceiver(
                            context,
                            wifiScanReceiver,
                            new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                    );
                } else {
                    of(scanResultsListener)
                            .ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                    of(connectionWpsListener)
                            .ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                    wifiConnectionCallback.errorConnect();
                    wifiLog("ERROR COULDN'T SCAN");
                }
            }
        }
    };

    private final WifiScanCallback wifiScanResultsCallback = new WifiScanCallback() {
        @Override
        public void onScanResultsReady() {
            wifiLog("GOT SCAN RESULTS");
            unregisterReceiver(context, wifiScanReceiver);

            final List<ScanResult> scanResultList = wifiManager.getScanResults();
            of(scanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(scanResultList));
            of(connectionScanResultsListener).ifPresent(connectionResultsListener
                    -> singleScanResult = connectionResultsListener.onConnectWithScanResult(scanResultList));

            if (connectionWpsListener != null && bssid != null && password != null) {
                singleScanResult = matchScanResultBssid(bssid, scanResultList);
                if (singleScanResult != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    connectWps(wifiManager, singleScanResult, password, wpsTimeoutMillis, connectionWpsListener);
                } else {
                    if (singleScanResult == null) {
                        wifiLog("Couldn't find network. Possibly out of range");
                    }
                    connectionWpsListener.isSuccessful(false);
                }
                return;
            }

            if (ssid != null) {
                if (bssid != null) {
                    singleScanResult = matchScanResult(ssid, bssid, scanResultList);
                } else {
                    singleScanResult = matchScanResultSsid(ssid, scanResultList);
                }
            }
            if (singleScanResult != null && password != null) {
                if (connectToWifi(context, wifiManager, singleScanResult, password)) {
                    registerReceiver(
                            context,
                            wifiConnectionReceiver.activateTimeoutHandler(singleScanResult),
                            new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
                    );
                    registerReceiver(
                            context,
                            wifiConnectionReceiver,
                            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    );
                } else {
                    wifiConnectionCallback.errorConnect();
                }
            } else {
                wifiConnectionCallback.errorConnect();
            }
        }
    };

    private final WifiConnectionCallback wifiConnectionCallback = new WifiConnectionCallback() {
        @Override
        public void successfulConnect() {
            wifiLog("CONNECTED SUCCESSFULLY");
            unregisterReceiver(context, wifiConnectionReceiver);
            //reenableAllHotspots(mWifiManager);
            of(connectionSuccessListener).ifPresent(successListener -> successListener.isSuccessful(true));
        }

        @Override
        public void errorConnect() {
            unregisterReceiver(context, wifiConnectionReceiver);
            reenableAllHotspots(wifiManager);
            //if (mSingleScanResult != null)
            //cleanPreviousConfiguration(mWifiManager, mSingleScanResult);
            of(connectionSuccessListener).ifPresent(successListener -> {
                successListener.isSuccessful(false);
                wifiLog("DIDN'T CONNECT TO WIFI");
            });
        }
    };

    private WifiUtils(@NonNull Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new RuntimeException("WifiManager is not supposed to be null");
        }
        wifiStateReceiver = new WifiStateReceiver(wifiStateCallback);
        wifiScanReceiver = new WifiScanReceiver(wifiScanResultsCallback);
        wifiConnectionReceiver = new WifiConnectionReceiver(wifiConnectionCallback, wifiManager, timeoutMillis);
    }

    public static WifiUtilsBuilder withContext(@NonNull final Context context) {
        return new WifiUtils(context);
    }

    public static void wifiLog(final String text) {
        if (enableLog) {
            Log.d(TAG, "WifiUtils: " + text);
        }
    }

    public static void enableLog(final boolean enabled) {
        enableLog = enabled;
    }

    @Override
    public void enableWifi(@Nullable final WifiStateListener wifiStateListener) {
        this.wifiStateListener = wifiStateListener;
        if (wifiManager.isWifiEnabled()) {
            wifiStateCallback.onWifiEnabled();
        } else {
            if (wifiManager.setWifiEnabled(true)) {
                registerReceiver(
                        context,
                        wifiStateReceiver,
                        new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
                );
            } else {
                of(wifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(false));
                of(scanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                of(connectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                wifiConnectionCallback.errorConnect();
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @Override
    public void enableWifi() {
        enableWifi(null);
    }

    @NonNull
    @Override
    public WifiConnectorBuilder scanWifi(final ScanResultsListener scanResultsListener) {
        this.scanResultsListener = scanResultsListener;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid,
                                           @NonNull final String password) {
        this.ssid = ssid;
        this.password = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid,
                                           @NonNull final String bssid,
                                           @NonNull final String password) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.password = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWithScanResult(
            @NonNull final String password,
            @Nullable final ConnectionScanResultsListener connectionScanResultsListener
    ) {
        this.connectionScanResultsListener = connectionScanResultsListener;
        this.password = password;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiWpsSuccessListener connectWithWps(@NonNull final String bssid,
                                                 @NonNull final String password) {
        this.bssid = bssid;
        this.password = password;
        return this;
    }

    @Override
    public void cancelAutoConnect() {
        unregisterReceiver(context, wifiStateReceiver);
        unregisterReceiver(context, wifiScanReceiver);
        unregisterReceiver(context, wifiConnectionReceiver);
        of(singleScanResult).ifPresent(scanResult -> cleanPreviousConfiguration(wifiManager, scanResult));
        reenableAllHotspots(wifiManager);
    }

    @NonNull
    @Override
    public WifiSuccessListener setTimeout(final long timeOutMillis) {
        timeoutMillis = timeOutMillis;
        wifiConnectionReceiver.setTimeout(timeOutMillis);
        return this;
    }

    @NonNull
    @Override
    public WifiWpsSuccessListener setWpsTimeout(final long timeOutMillis) {
        wpsTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiConnectorBuilder onConnectionWpsResult(@Nullable final ConnectionWpsListener successListener) {
        connectionWpsListener = successListener;
        return this;
    }


    @NonNull
    @Override
    public WifiConnectorBuilder onConnectionResult(@Nullable final ConnectionSuccessListener successListener) {
        connectionSuccessListener = successListener;
        return this;
    }

    @Override
    public void start() {
        unregisterReceiver(context, wifiStateReceiver);
        unregisterReceiver(context, wifiScanReceiver);
        unregisterReceiver(context, wifiConnectionReceiver);
        enableWifi(null);
    }

    @Override
    public void disableWifi() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            unregisterReceiver(context, wifiStateReceiver);
            unregisterReceiver(context, wifiScanReceiver);
            unregisterReceiver(context, wifiConnectionReceiver);
        }
        wifiLog("WiFi Disabled");
    }
}
