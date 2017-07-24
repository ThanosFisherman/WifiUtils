package com.thanosfisherman.wifiutils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thanosfisherman.wifiutils.wifiConnect.ConnectionStateListener;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionReceiver;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanCallback;
import com.thanosfisherman.wifiutils.wifiScan.WifiScanReceiver;
import com.thanosfisherman.wifiutils.wifiState.WifiStateCallback;
import com.thanosfisherman.wifiutils.wifiState.WifiStateListener;
import com.thanosfisherman.wifiutils.wifiState.WifiStateReceiver;

import java.util.ArrayList;

import static com.thanosfisherman.wifiutils.ConnectorUtils.connectToWifi;
import static com.thanosfisherman.wifiutils.ConnectorUtils.registerReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.unregisterReceiver;
import static com.thanosfisherman.wifiutils.ConnectorUtils.wifiLog;

public final class WifiConnector implements WifiConnectorBuilder, WifiConnectorBuilder.WifiState, WifiConnectorBuilder.WifiConnectionBuilder
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
    @Nullable private ConnectionStateListener mConnectionStateListener;
    @Nullable private WifiStateListener mWifiStateListener;

    @NonNull private final WifiStateCallback mWifiStateCallback = new WifiStateCallback()
    {
        @Override
        public void onWifiEnabled()
        {
            wifiLog("WIFI ENABLED...");
            if (mWifiStateListener != null)
                mWifiStateListener.isSuccess(true);

            if (mScanResultsListener != null)
            {
                wifiLog("START SCANNING....");
                unregisterReceiver(mContext, mWifiStateReceiver);
                if (mWifiManager.startScan())
                    registerReceiver(mContext, mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                else
                {
                    mScanResult = mScanResultsListener.onScanResults(new ArrayList<ScanResult>());
                    //TODO: CALL onError() ?
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
            if (mScanResultsListener != null)
                mScanResult = mScanResultsListener.onScanResults(mWifiManager.getScanResults());
            unregisterReceiver(mContext, mWifiScanReceiver);
        }
    };

    @NonNull private final WifiConnectionCallback mWifiConnectionCallback = new WifiConnectionCallback()
    {
        @Override
        public void successfulConnect()
        {
            if (mConnectionStateListener != null)
                unregisterReceiver(mContext, mWifiConnectionReceiver);
        }

        @Override
        public void errorConnect()
        {
            unregisterReceiver(mContext, mWifiConnectionReceiver);
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

    public static WifiConnectorBuilder.WifiState withContext(@NonNull Context context)
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
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @Override
    public WifiConnectionBuilder wifiScan(ScanResultsListener scanResultsListener)
    {
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @Override
    public WifiConnectorBuilder connectWith(String ssid, ConnectionStateListener connectionStateListener)
    {
        mConnectionStateListener = connectionStateListener;
        return this;
    }

    @Override
    public WifiConnectorBuilder connectWith(String ssid, String bssid, ConnectionStateListener connectionStateListener)
    {
        mConnectionStateListener = connectionStateListener;
        return this;
    }


    @Override
    public WifiConnectorBuilder connectWithScanResult(String password, ConnectionStateListener connectionStateListener)
    {
        if (mScanResult != null)
        {
            unregisterReceiver(mContext, mWifiScanReceiver);
            registerReceiver(mContext, mWifiConnectionReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            connectToWifi(mContext, mWifiManager, mScanResult, password);
        }
        return this;
    }

    @Override
    public void start()
    {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        if (mScanResultsListener != null)
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

}
