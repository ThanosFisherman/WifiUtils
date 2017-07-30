package com.thanosfisherman.wifiutils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.isConnectedToBSSID;
import static com.thanosfisherman.wifiutils.ConnectorUtils.matchScanResult;
import static com.thanosfisherman.wifiutils.ConnectorUtils.reenableAllHotspots;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.wifiLog;

public final class WifiConnector implements WifiConnectorBuilder, WifiConnectorBuilder.WifiUtilsListener, WifiConnectorBuilder.WifiSuccessListener
{
    @NonNull private WifiManager mWifiManager;
    @NonNull private Context mContext;
    @Nullable private String mSsid;
    @Nullable private String mBssid;
    @Nullable private String mPassword;
    @Nullable private ScanResult mSingleScanResult;
    @Nullable private WifiStateReceiver mWifiStateReceiver;
    @Nullable private WifiConnectionReceiver mWifiConnectionReceiver;
    @Nullable private WifiScanReceiver mWifiScanReceiver;
    @Nullable private ScanResultsListener mScanResultsListener;
    @Nullable private ConnectionScanResultsListener mConnectionScanResultsListener;
    @Nullable private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable private WifiStateListener mWifiStateListener;

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
                    if (mConnectionSuccessListener != null)
                        mConnectionSuccessListener.isSuccessful(false);
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
            unregisterReceiver(mContext, mWifiScanReceiver);

            final List<ScanResult> scanResultList = mWifiManager.getScanResults();
            if (mScanResultsListener != null)
                mScanResultsListener.onScanResults(scanResultList);

            if (mPassword == null)
            {
                mWifiConnectionCallback.errorConnect();
                return;
            }

            if (mConnectionScanResultsListener != null)
                mSingleScanResult = mConnectionScanResultsListener.onConnectWithScanResult(scanResultList);

            if (mSsid != null)
            {
                if (mBssid != null)
                    mSingleScanResult = matchScanResult(mSsid, mBssid, scanResultList);
                else
                    mSingleScanResult = matchScanResult(mSsid, scanResultList);
            }

            if (mSingleScanResult != null)
            {
                if (isConnectedToBSSID(mWifiManager, mSingleScanResult.BSSID))
                {
                    mWifiConnectionCallback.successfulConnect();
                    return;
                }
                if (connectToWifi(mContext, mWifiManager, mSingleScanResult, mPassword))
                    registerReceiver(mContext, mWifiConnectionReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
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
            wifiLog("COULDN'T CONNECT :(");
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            reenableAllHotspots(mWifiManager);
            if (mConnectionSuccessListener != null)
                mConnectionSuccessListener.isSuccessful(false);
        }
    };


    private WifiConnector(@NonNull Context context)
    {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mContext = context;
        mWifiStateReceiver = new WifiStateReceiver(mWifiStateCallback);
        mWifiScanReceiver = new WifiScanReceiver(mWifiScanResultsCallback);
        mWifiConnectionReceiver = new WifiConnectionReceiver(mWifiConnectionCallback);
    }

    public static WifiUtilsListener withContext(@NonNull Context context)
    {
        return new WifiConnector(context);
    }

    @Override
    public void enableWifi(WifiStateListener wifiStateListener)
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
                if (mConnectionSuccessListener != null)
                    mConnectionSuccessListener.isSuccessful(false);
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @Override
    public WifiConnectorBuilder scanWifi(ScanResultsListener scanResultsListener)
    {
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @Override
    public WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String password)
    {
        mSsid = ssid;
        mPassword = password;
        return this;
    }

    @Override
    public WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String bssid, @NonNull String password)
    {
        mSsid = ssid;
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @Override
    public WifiSuccessListener connectWithScanResult(@NonNull String password, ConnectionScanResultsListener connectionScanResultsListener)
    {
        mConnectionScanResultsListener = connectionScanResultsListener;
        mPassword = password;
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
        wifiLog("Disabling WiFi...");
    }

    @Override
    public WifiConnectorBuilder onConnectionResult(ConnectionSuccessListener successListener)
    {
        mConnectionSuccessListener = successListener;
        return this;
    }
}
