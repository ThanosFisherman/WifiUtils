package com.thanosfisherman.wifiutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionFailListener;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionReceiver;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanCallback;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanReceiver;
import com.thanosfisherman.wifiutils.wifiState.WifiStateCallback;
import com.thanosfisherman.wifiutils.wifiState.WifiStateReceiver;

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.wifiutils.ConnectorUtils.MAX_PRIORITY;
import static com.thanosfisherman.wifiutils.ConnectorUtils.checkForExcessOpenNetworkAndSave;
import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.convertToQuotedString;
import static com.thanosfisherman.wifiutils.ConnectorUtils.getMaxPriority;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.shiftPriorityAndSave;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.wifiLog;

public final class WifiConnector implements WifiConnectorBuilder,
                                            WifiConnectorBuilder.WifiState,
                                            WifiConnectorBuilder.ScanBuilder,
                                            WifiConnectorBuilder.WifiConnectionBuilder
{
    @NonNull private WifiManager mWifiManager;
    @NonNull private Context mContext;
    @Nullable private String mSsid;
    @Nullable private String mBssid;
    @Nullable private String mPassword;
    @Nullable private ScanResult mScanResult;
    @Nullable private WifiStateReceiver mWifiStateReceiver;
    @Nullable private WifiConnectionReceiver mWifiConnectionReceiver;
    @Nullable private WifiScanReceiver mWifiScanReceiver;
    @Nullable private ScanResultsListener mScanResultsListener;
    @Nullable private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable private ConnectionFailListener mConnectionFailListener;
    private int methodCalls;
    @NonNull private final ReceiverCallbacks mWifiStateCallback = new WifiStateCallback()
    {
        @Override
        public void onWifiEnabled()
        {
            wifiLog("WIFI ENABLED...");
            if (mScanResultsListener != null)
            {
                methodCalls++;
                WifiConnector.this.startScan(mScanResultsListener);
            }
        }

        @Override
        public void onWifiDisabled()
        {

        }
    };
    @NonNull private final ReceiverCallbacks mWifiConnectionCallback = new WifiConnectionCallback()
    {
        @Override
        public void successfulConnect()
        {
            if (mConnectionSuccessListener != null)
                unregisterReceiver(mContext, mWifiConnectionReceiver);
        }

        @Override
        public void errorConnect()
        {
            if (mConnectionFailListener != null)
                unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
    };

    @NonNull private final ReceiverCallbacks mWifiScanCallback = new WifiScanCallback() {
        @Override
        public void onScanResultsReady(List<ScanResult> scanResults)
        {

        }
    };

    private WifiConnector(@NonNull Context context)
    {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mContext = context;
    }

    public static WifiConnectorBuilder.WifiState withContext(@NonNull Context context)
    {
        return new WifiConnector(context);
    }

    @Override
    public WifiConnectorBuilder connectWith(String ssid)
    {
        return null;
    }

    @Override
    public WifiConnectorBuilder connectWith(String ssid, String bssid)
    {
        return null;
    }

    @Override
    public ScanBuilder enableWifi()
    {
        if (mWifiManager.isWifiEnabled())
            methodCalls++;
        else
        {

            if (mWifiManager.setWifiEnabled(true))
                registerReceiver(mContext, mWifiStateReceiver, mWifiStateCallback, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            else
                //TODO: propagate this event to onError if available
                wifiLog("COULDN't enable wifi");
        }
        wifiLog("enabling Wifi... " + methodCalls);
        return this;
    }

    @Override
    public WifiConnectionBuilder startScan(ScanResultsListener scanResultsListener)
    {
        mScanResultsListener = scanResultsListener;
        if (methodCalls >= 1)
        {
            unregisterReceiver(mContext, mWifiStateReceiver);
            //registerReceiver(mContext,mWifiScanReceiver, mWifiScanCallback,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            if (mWifiManager.startScan())
                mScanResult = scanResultsListener.onScanResults(mWifiManager.getScanResults());
            else
                mScanResult = scanResultsListener.onScanResults(new ArrayList<ScanResult>());
            methodCalls++;
        }
        wifiLog("startScan... " + methodCalls);
        return this;
    }

    @Override
    public WifiConnectorBuilder connectWithScanResult(@Nullable String password)
    {

        if (mScanResult != null && methodCalls >= 2)
        {
            wifiLog("connectWithScanResult..." + methodCalls);
            unregisterReceiver(mContext, mWifiScanReceiver);
            registerReceiver(mContext, mWifiConnectionReceiver, mWifiConnectionCallback, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            connectToWifi(mContext, mWifiManager, mScanResult, password);
        }
        return this;
    }

    @Override
    public WifiConnectorBuilder onErrorListener(@NonNull ConnectionFailListener connectionFailListener)
    {
        mConnectionFailListener = connectionFailListener;
        if (mScanResult == null && methodCalls >= 3)
            connectionFailListener.onConnectionFailed();
        return this;
    }

    @Override
    public WifiConnectorBuilder onSuccess(@NonNull ConnectionSuccessListener connectionSuccessListener)
    {
        mConnectionSuccessListener = connectionSuccessListener;
        return this;
    }

    @Override
    public void disableWifi()
    {
        if (mWifiManager.isWifiEnabled())
        {
            methodCalls = 0;
            mWifiManager.setWifiEnabled(false);
            unregisterReceiver(mContext, mWifiStateReceiver);
            unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
        wifiLog("Disabling WiFi...");
    }

}
