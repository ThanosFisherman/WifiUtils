package com.thanosfisherman.wifiutils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionScanResultsListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionReceiver;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanCallback;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanReceiver;
import com.thanosfisherman.wifiutils.wifiState.WifiStateCallback;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateReceiver;
import com.thanosfisherman.wifiutils.wifiWps.ConnectionWpsListener;

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.wifiutils.ConnectorUtils.cleanPreviousConfiguration;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectWps;
import static com.thanosfisherman.wifiutils.ConnectorUtils.isConnectedToBSSID;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResult;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultBssid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResultSsid;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reenableAllHotspots;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;

public final class WifiUtils implements WifiConnectorBuilder,
                                        WifiConnectorBuilder.WifiUtilsListener,
                                        WifiConnectorBuilder.WifiSuccessListener,
                                        WifiConnectorBuilder.WifiWpsSuccessListener
{
    private WifiManager mWifiManager;
    private Context mContext;
    private static boolean mEnableLog;
    private long mTimeoutMillis;
    @NonNull private static final String TAG = WifiUtils.class.getSimpleName();
    //@NonNull private static final WifiUtils INSTANCE = new WifiUtils();
    @NonNull private final WifiStateReceiver mWifiStateReceiver;
    @NonNull private final WifiConnectionReceiver mWifiConnectionReceiver;
    @NonNull private final WifiScanReceiver mWifiScanReceiver;
    @Nullable private String mSsid;
    @Nullable private String mBssid;
    @Nullable private String mPassword;
    @Nullable private ScanResult mSingleScanResult;
    @Nullable private ScanResultsListener mScanResultsListener;
    @Nullable private ConnectionScanResultsListener mConnectionScanResultsListener;
    @Nullable private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable private WifiStateListener mWifiStateListener;
    @Nullable private ConnectionWpsListener mConnectionWpsListener;

    @NonNull private final WifiStateCallback mWifiStateCallback = new WifiStateCallback()
    {
        @Override
        public void onWifiEnabled()
        {
            wifiLog("WIFI ENABLED...");
            unregisterReceiver(mContext, mWifiStateReceiver);
            if (mWifiStateListener != null)
                mWifiStateListener.isSuccess(true);

            if (mScanResultsListener != null || mPassword != null)
            {
                wifiLog("START SCANNING....");
                if (mWifiManager.startScan())
                    registerReceiver(mContext, mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                else
                {
                    if (mScanResultsListener != null)
                        mScanResultsListener.onScanResults(new ArrayList<ScanResult>());
                    if (mConnectionWpsListener != null)
                        mConnectionWpsListener.isSuccessful(false);
                    mWifiConnectionCallback.errorConnect();
                    wifiLog("COULDN'T SCAN ERROR");
                }
            }
        }
    };

    @NonNull private final WifiScanCallback mWifiScanResultsCallback = new WifiScanCallback()
    {
        @Override
        public void onScanResultsReady()
        {
            wifiLog("GOT SCAN RESULTS");
            unregisterReceiver(mContext, mWifiScanReceiver);

            final List<ScanResult> scanResultList = mWifiManager.getScanResults();

            if (mScanResultsListener != null)
                mScanResultsListener.onScanResults(scanResultList);

            if (mConnectionScanResultsListener != null)
                mSingleScanResult = mConnectionScanResultsListener.onConnectWithScanResult(scanResultList);

            if (mConnectionWpsListener != null && mBssid != null && mPassword != null)
            {
                mSingleScanResult = matchScanResultBssid(mBssid, scanResultList);
                if (mSingleScanResult != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    connectWps(mWifiManager, mSingleScanResult, mPassword, mTimeoutMillis, mConnectionWpsListener);
                else
                {
                    if (mSingleScanResult == null)
                        wifiLog("Couldn't find network. Possibly out of range");
                    mConnectionWpsListener.isSuccessful(false);
                }
                return;
            }

            if (mSsid != null)
            {
                if (mBssid != null)
                    mSingleScanResult = matchScanResult(mSsid, mBssid, scanResultList);
                else
                    mSingleScanResult = matchScanResultSsid(mSsid, scanResultList);
            }
            if (mSingleScanResult != null && mPassword != null)
            {
                //Do I really need dis? Not sure yet
                if (isConnectedToBSSID(mWifiManager, mSingleScanResult.BSSID))
                {
                    mWifiConnectionCallback.successfulConnect();
                    return;
                }
                if (connectToWifi(mContext, mWifiManager, mSingleScanResult, mPassword))
                    registerReceiver(mContext, mWifiConnectionReceiver.activateTimeoutHandler(), new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                else
                    mWifiConnectionCallback.errorConnect();
            }
            else
                mWifiConnectionCallback.errorConnect();
        }
    };

    @NonNull private final WifiConnectionCallback mWifiConnectionCallback = new WifiConnectionCallback()
    {
        @Override
        public void successfulConnect()
        {
            wifiLog("CONNECTED SUCCESSFULLY");
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            if (mConnectionSuccessListener != null)
                mConnectionSuccessListener.isSuccessful(true);
        }

        @Override
        public void errorConnect()
        {
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            if (mSingleScanResult != null)
                cleanPreviousConfiguration(mWifiManager, mSingleScanResult);
            reenableAllHotspots(mWifiManager);
            if (mConnectionSuccessListener != null)
            {
                wifiLog("DIDN'T CONNECT TO WIFI");
                mConnectionSuccessListener.isSuccessful(false);
            }
        }
    };

    private WifiUtils()
    {
        mTimeoutMillis = 30000;
        mWifiStateReceiver = new WifiStateReceiver(mWifiStateCallback);
        mWifiScanReceiver = new WifiScanReceiver(mWifiScanResultsCallback);
        mWifiConnectionReceiver = new WifiConnectionReceiver(mWifiConnectionCallback, 30000);
    }

    public static WifiUtilsListener withContext(@NonNull final Context context)
    {
        //INSTANCE.setContext(context);
        final WifiUtils wifiUtils = new WifiUtils();
        wifiUtils.setContext(context);
        return wifiUtils;
    }

    public static void wifiLog(final String text)
    {
        if (mEnableLog)
            Log.d(TAG, "WifiUtils: " + text);
    }

    public static void enableLog(final boolean enabled)
    {
        mEnableLog = enabled;
    }

    private void setContext(@NonNull final Context context)
    {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void enableWifi(final WifiStateListener wifiStateListener)
    {
        mWifiStateListener = wifiStateListener;
        if (mWifiManager.isWifiEnabled())
            mWifiStateCallback.onWifiEnabled();
        else
        {
            if (mWifiManager.setWifiEnabled(true))
                registerReceiver(mContext, mWifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            else
            {
                if (wifiStateListener != null)
                    wifiStateListener.isSuccess(false);
                if (mScanResultsListener != null)
                    mScanResultsListener.onScanResults(new ArrayList<ScanResult>());
                if (mConnectionWpsListener != null)
                    mConnectionWpsListener.isSuccessful(false);
                mWifiConnectionCallback.errorConnect();
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @Override
    public WifiConnectorBuilder scanWifi(final ScanResultsListener scanResultsListener)
    {
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password)
    {
        mSsid = ssid;
        mPassword = password;
        return this;
    }

    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String bssid, @NonNull final String password)
    {
        mSsid = ssid;
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @Override
    public WifiSuccessListener connectWithScanResult(@NonNull final String password,
                                                     @Nullable final ConnectionScanResultsListener connectionScanResultsListener)
    {
        mConnectionScanResultsListener = connectionScanResultsListener;
        mPassword = password;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiWpsSuccessListener connectWithWps(@NonNull final String bssid, @NonNull final String password)
    {
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @Override
    public WifiSuccessListener setTimeout(final long timeOutMillis)
    {
        mWifiConnectionReceiver.setTimeout(timeOutMillis);
        return this;
    }

    @Override
    public WifiWpsSuccessListener setWpsTimeout(final long timeOutMillis)
    {
        mTimeoutMillis = timeOutMillis;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiConnectorBuilder onConnectionWpsResult(@Nullable final ConnectionWpsListener successListener)
    {
        mConnectionWpsListener = successListener;
        return this;
    }


    @Override
    public WifiConnectorBuilder onConnectionResult(@Nullable final ConnectionSuccessListener successListener)
    {
        mConnectionSuccessListener = successListener;
        return this;
    }

    @Override
    public void start()
    {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        enableWifi(null);
    }

    @Override
    public void disableWifi()
    {
        if (mWifiManager.isWifiEnabled())
        {
            mWifiManager.setWifiEnabled(false);
            unregisterReceiver(mContext, mWifiStateReceiver);
            unregisterReceiver(mContext, mWifiScanReceiver);
            unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
        wifiLog("WiFi Disabled");
    }
}
