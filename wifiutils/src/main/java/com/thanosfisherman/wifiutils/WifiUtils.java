package com.thanosfisherman.wifiutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionScanResultsListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiConnect.DisconnectCallbackHolder;
import com.thanosfisherman.wifiutils.wifiConnect.TimeoutHandler;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionReceiver;
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiRemove.RemoveErrorCode;
import com.thanosfisherman.wifiutils.wifiRemove.RemoveSuccessListener;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanCallback;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanReceiver;
import com.thanosfisherman.wifiutils.wifiState.WifiStateCallback;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateReceiver;
import com.thanosfisherman.wifiutils.wifiWps.ConnectionWpsListener;

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.wifiutils.utils.Elvis.of;
import static com.thanosfisherman.wifiutils.ConnectorUtils.cleanPreviousConfiguration;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifiHidden;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectWps;
import static com.thanosfisherman.wifiutils.ConnectorUtils.disconnectFromWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResult;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultBssid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultSsid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reenableAllHotspots;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.removeWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isAndroidQOrLater;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isLollipopOrLater;

@SuppressLint("MissingPermission")
public final class WifiUtils implements WifiConnectorBuilder,
        WifiConnectorBuilder.WifiUtilsBuilder,
        WifiConnectorBuilder.WifiSuccessListener,
        WifiConnectorBuilder.WifiWpsSuccessListener {
    private static final String TAG = WifiUtils.class.getSimpleName();

    @Nullable
    private final WifiManager mWifiManager;
    @Nullable
    private final ConnectivityManager mConnectivityManager;
    @NonNull
    private final Context mContext;
    private static boolean mEnableLog=true;
    @Nullable
    private static Logger customLogger;
    private long mWpsTimeoutMillis = 30000;
    private long mTimeoutMillis = 30000;
    @NonNull
    private WeakHandler mHandler;
    @NonNull
    private final WifiStateReceiver mWifiStateReceiver;
    @NonNull
    private final WifiConnectionReceiver mWifiConnectionReceiver;
    @NonNull
    private final TimeoutHandler mTimeoutHandler;
    @NonNull
    private final WifiScanReceiver mWifiScanReceiver;
    @Nullable
    private String mSsid;
    @Nullable
    private String type;
    @Nullable
    private String mBssid;
    @Nullable
    private String mPassword;
    @Nullable
    private ScanResult mSingleScanResult;
    @Nullable
    private ScanResultsListener mScanResultsListener;
    @Nullable
    private ConnectionScanResultsListener mConnectionScanResultsListener;
    @Nullable
    private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable
    private WifiStateListener mWifiStateListener;
    @Nullable
    private ConnectionWpsListener mConnectionWpsListener;
    @Nullable
    private boolean mPatternMatch;

    @NonNull
    private final WifiStateCallback mWifiStateCallback = new WifiStateCallback() {
        @Override
        public void onWifiEnabled() {
            wifiLog("WIFI ENABLED...");
            unregisterReceiver(mContext, mWifiStateReceiver);
            of(mWifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(true));

            if (mScanResultsListener != null || mPassword != null) {
                wifiLog("START SCANNING....");
                if (mWifiManager.startScan()) {
                    registerReceiver(mContext, mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                } else {
                    of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                    of(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.COULD_NOT_SCAN);
                    wifiLog("ERROR COULDN'T SCAN");
                }
            }
        }
    };

    @NonNull
    private final WifiScanCallback mWifiScanResultsCallback = new WifiScanCallback() {
        @Override
        public void onScanResultsReady() {
            wifiLog("GOT SCAN RESULTS");
            unregisterReceiver(mContext, mWifiScanReceiver);

            final List<ScanResult> scanResultList = mWifiManager.getScanResults();
            of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(scanResultList));
            of(mConnectionScanResultsListener).ifPresent(connectionResultsListener -> mSingleScanResult = connectionResultsListener.onConnectWithScanResult(scanResultList));

            if (mConnectionWpsListener != null && mBssid != null && mPassword != null) {
                mSingleScanResult = matchScanResultBssid(mBssid, scanResultList);
                if (mSingleScanResult != null && isLollipopOrLater()) {
                    connectWps(mWifiManager, mHandler, mSingleScanResult, mPassword, mWpsTimeoutMillis, mConnectionWpsListener);
                } else {
                    if (mSingleScanResult == null) {
                        wifiLog("Couldn't find network. Possibly out of range");
                    }
                    mConnectionWpsListener.isSuccessful(false);
                }
                return;
            }

            if (mSsid != null) {
                if (mBssid != null) {
                    mSingleScanResult = matchScanResult(mSsid, mBssid, scanResultList);
                } else {
                    mSingleScanResult = matchScanResultSsid(mSsid, scanResultList, mPatternMatch);
                }
            }
            if (mSingleScanResult != null && mPassword != null) {
                if (connectToWifi(mContext, mWifiManager, mConnectivityManager, mHandler, mSingleScanResult, mPassword, mWifiConnectionCallback, mPatternMatch, mSsid)) {
                    registerReceiver(mContext, (mWifiConnectionReceiver).connectWith(mSingleScanResult, mPassword, mConnectivityManager),
                            new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                    registerReceiver(mContext, mWifiConnectionReceiver,
                            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                    mTimeoutHandler.startTimeout(mSingleScanResult, mTimeoutMillis);
                } else {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.COULD_NOT_CONNECT);
                }
            } else {
                if (connectToWifiHidden(mContext, mWifiManager, mConnectivityManager, mHandler, mSsid, type, mPassword, mWifiConnectionCallback)) {
                    registerReceiver(mContext, (mWifiConnectionReceiver).connectWith(mSsid, mPassword, mConnectivityManager),
                            new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                    registerReceiver(mContext, mWifiConnectionReceiver,
                            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                    mTimeoutHandler.startTimeout(mSingleScanResult, mTimeoutMillis);
                } else {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.COULD_NOT_CONNECT);
                }
            }
        }
    };

    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback = new WifiConnectionCallback() {
        @Override
        public void successfulConnect() {
            wifiLog("CONNECTED SUCCESSFULLY");
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            mTimeoutHandler.stopTimeout();

            //reenableAllHotspots(mWifiManager);
            of(mConnectionSuccessListener).ifPresent(ConnectionSuccessListener::success);
        }

        @Override
        public void errorConnect(@NonNull ConnectionErrorCode connectionErrorCode) {
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            mTimeoutHandler.stopTimeout();
            if (isAndroidQOrLater()) {
                DisconnectCallbackHolder.getInstance().disconnect();
            }
            reenableAllHotspots(mWifiManager);
            //if (mSingleScanResult != null)
            //cleanPreviousConfiguration(mWifiManager, mSingleScanResult);
            of(mConnectionSuccessListener).ifPresent(successListener -> {
                successListener.failed(connectionErrorCode);
                wifiLog("DIDN'T CONNECT TO WIFI " + connectionErrorCode);
            });
        }
    };

    private WifiUtils(@NonNull Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            throw new RuntimeException("WifiManager is not supposed to be null");
        }
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiStateReceiver = new WifiStateReceiver(mWifiStateCallback);
        mWifiScanReceiver = new WifiScanReceiver(mWifiScanResultsCallback);
        mHandler = new WeakHandler();
        mWifiConnectionReceiver = new WifiConnectionReceiver(mWifiConnectionCallback, mWifiManager);
        mTimeoutHandler = new TimeoutHandler(mWifiManager, mHandler, mWifiConnectionCallback);
    }

    public static WifiUtilsBuilder withContext(@NonNull final Context context) {
        return new WifiUtils(context);
    }

    public static void wifiLog(final String text) {
        if (mEnableLog) {
            Logger logger = of(customLogger).orElse((priority, tag, message) -> {
                Log.println(priority, TAG, message);
            });
            logger.log(Log.VERBOSE, TAG, text);
        }
    }

    public static void enableLog(final boolean enabled) {
        mEnableLog = enabled;
    }

    /**
     * Send logs to a custom logging implementation. If none specified, defaults to logcat.
     *
     * @param logger custom logger
     */
    public static void forwardLog(Logger logger) {
        WifiUtils.customLogger = logger;
    }

    @Override
    public void enableWifi(@Nullable final WifiStateListener wifiStateListener) {
        mWifiStateListener = wifiStateListener;
        if (mWifiManager.isWifiEnabled()) {
            mWifiStateCallback.onWifiEnabled();
        } else {
            if (mWifiManager.setWifiEnabled(true)) {
                registerReceiver(mContext, mWifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            } else {
                of(wifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(false));
                of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                of(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.COULD_NOT_ENABLE_WIFI);
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
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @Deprecated
    @Override
    public void disconnectFrom(@NonNull final String ssid, @NonNull final DisconnectionSuccessListener disconnectionSuccessListener) {
        this.disconnect(disconnectionSuccessListener);
    }

    @Override
    public void disconnect(@NonNull DisconnectionSuccessListener disconnectionSuccessListener) {
        if (mConnectivityManager == null) {
            disconnectionSuccessListener.failed(DisconnectionErrorCode.COULD_NOT_GET_CONNECTIVITY_MANAGER);
            return;
        }

        if (mWifiManager == null) {
            disconnectionSuccessListener.failed(DisconnectionErrorCode.COULD_NOT_GET_WIFI_MANAGER);
            return;
        }

        if (isAndroidQOrLater()) {
            DisconnectCallbackHolder.getInstance().unbindProcessFromNetwork();
            DisconnectCallbackHolder.getInstance().disconnect();
            disconnectionSuccessListener.success();
        } else {
            if (disconnectFromWifi(mWifiManager)) {
                disconnectionSuccessListener.success();
            } else {
                disconnectionSuccessListener.failed(DisconnectionErrorCode.COULD_NOT_DISCONNECT);
            }
        }
    }


    @Override
    public void remove(@NonNull String ssid, @NonNull RemoveSuccessListener removeSuccessListener) {
        if (mConnectivityManager == null) {
            removeSuccessListener.failed(RemoveErrorCode.COULD_NOT_GET_CONNECTIVITY_MANAGER);
            return;
        }

        if (mWifiManager == null) {
            removeSuccessListener.failed(RemoveErrorCode.COULD_NOT_GET_WIFI_MANAGER);
            return;
        }

        if (isAndroidQOrLater()) {
            DisconnectCallbackHolder.getInstance().disconnect();
            removeSuccessListener.success();
        } else {
            if (removeWifi(mWifiManager, ssid)) {
                removeSuccessListener.success();
            } else {
                removeSuccessListener.failed(RemoveErrorCode.COULD_NOT_REMOVE);
            }
        }
    }

    @NonNull
    @Override
    public WifiUtilsBuilder patternMatch() {
        mPatternMatch = true;

        return this;
    }


    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid) {
        mSsid = ssid;
        mPassword = ""; // FIXME: Cover no password case

        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password) {
        mSsid = ssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password, @NonNull final TypeEnum type) {
        mSsid = ssid;
        mPassword = password;
        this.type = type.name();
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String bssid, @NonNull final String password) {
        mSsid = ssid;
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWithScanResult(@NonNull final String password,
                                                     @Nullable final ConnectionScanResultsListener connectionScanResultsListener) {
        mConnectionScanResultsListener = connectionScanResultsListener;
        mPassword = password;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiWpsSuccessListener connectWithWps(@NonNull final String bssid, @NonNull final String password) {
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @Override
    public void cancelAutoConnect() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        of(mSingleScanResult).ifPresent(scanResult -> cleanPreviousConfiguration(mWifiManager, scanResult));
        reenableAllHotspots(mWifiManager);
    }

    @Override
    public boolean isWifiConnected(@NonNull String ssid) {
        return ConnectorUtils.isAlreadyConnected(mWifiManager, mConnectivityManager, ssid);
    }

    @Override
    public boolean isWifiConnected() {
        return ConnectorUtils.isAlreadyConnected(mConnectivityManager);
    }

    @NonNull
    @Override
    public WifiSuccessListener setTimeout(final long timeOutMillis) {
        mTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @Override
    public WifiWpsSuccessListener setWpsTimeout(final long timeOutMillis) {
        mWpsTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiConnectorBuilder onConnectionWpsResult(@Nullable final ConnectionWpsListener successListener) {
        mConnectionWpsListener = successListener;
        return this;
    }


    @NonNull
    @Override
    public WifiConnectorBuilder onConnectionResult(@Nullable final ConnectionSuccessListener successListener) {
        mConnectionSuccessListener = successListener;
        return this;
    }

    @Override
    public void start() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        enableWifi(null);
    }

    @Override
    public void disableWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            unregisterReceiver(mContext, mWifiStateReceiver);
            unregisterReceiver(mContext, mWifiScanReceiver);
            unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
        wifiLog("WiFi Disabled");
    }
}
