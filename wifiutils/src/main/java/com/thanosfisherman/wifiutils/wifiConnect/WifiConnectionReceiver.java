package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.thanosfisherman.wifiutils.WeakHandler;

import static com.thanosfisherman.wifiutils.ConnectorUtils.isAlreadyConnected;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver
{
    @NonNull private WifiConnectionCallback mWifiConnectionCallback;
    @Nullable private String mBssid;
    @Nullable private WifiManager mWifiManager;
    private long mDelay;
    @NonNull private WeakHandler handler;
    @NonNull private final Runnable handlerCallback = new Runnable()
    {
        @Override
        public void run()
        {
            wifiLog("Connection Timed out...");
            if (isAlreadyConnected(mWifiManager, mBssid))
                mWifiConnectionCallback.successfulConnect();
            else
                mWifiConnectionCallback.errorConnect();
            handler.removeCallbacks(this);
        }
    };

    public WifiConnectionReceiver(@NonNull WifiConnectionCallback callback, long delayMillis)
    {
        this.mWifiConnectionCallback = callback;
        this.mDelay = delayMillis;
        this.handler = new WeakHandler();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
        {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

            if (state == null)
            {
                mWifiConnectionCallback.errorConnect();
                return;
            }

            wifiLog("Connection Broadcast action: " + state);
            switch (state)
            {
                case COMPLETED:
                    if (isAlreadyConnected(mWifiManager, mBssid))
                    {
                        handler.removeCallbacks(handlerCallback);
                        mWifiConnectionCallback.successfulConnect();
                        return;
                    }
                    break;
                case DISCONNECTED:
                    final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                    {
                        wifiLog("Authentication error...");
                        handler.removeCallbacks(handlerCallback);
                        mWifiConnectionCallback.errorConnect();
                        return;
                    }
                    wifiLog("Disconnected. Re-attempting to connect...");
                    break;
            }
        }
    }

    public void setTimeout(long millis)
    {
        this.mDelay = millis;
    }

    public WifiConnectionReceiver activateTimeoutHandler(@NonNull WifiManager wifiManager, @NonNull String bssid)
    {
        mWifiManager = wifiManager;
        mBssid = bssid;
        handler.postDelayed(handlerCallback, mDelay);
        return this;
    }
}
