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
    @NonNull private final WifiConnectionCallback mWifiConnectionCallback;
    @Nullable private String mBssid;
    @Nullable private final WifiManager mWifiManager;
    private long mDelay;
    @NonNull private final WeakHandler handler;
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

    public WifiConnectionReceiver(@NonNull WifiConnectionCallback callback, @NonNull WifiManager wifiManager, long delayMillis)
    {
        this.mWifiConnectionCallback = callback;
        this.mWifiManager = wifiManager;
        this.mDelay = delayMillis;
        this.handler = new WeakHandler();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action))
        {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (state == null)
            {
                handler.removeCallbacks(handlerCallback);
                mWifiConnectionCallback.errorConnect();
                return;
            }

            wifiLog("Connection Broadcast action: " + state);

            if (isConnected(state))
            {
                if (isAlreadyConnected(mWifiManager, mBssid))
                {
                    handler.removeCallbacks(handlerCallback);
                    mWifiConnectionCallback.successfulConnect();
                }
            }
            else
            {
                wifiLog("Disconnected. Re-attempting to connect...");
                if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                {
                    wifiLog("Authentication error...");
                    handler.removeCallbacks(handlerCallback);
                    mWifiConnectionCallback.errorConnect();
                }
            }

        }
    }

    public void setTimeout(long millis)
    {
        this.mDelay = millis;
    }

    public WifiConnectionReceiver activateTimeoutHandler(@NonNull String bssid)
    {
        mBssid = bssid;
        handler.postDelayed(handlerCallback, mDelay);
        return this;
    }

    //unused
    private boolean isConnected(SupplicantState state)
    {
        switch (state)
        {
            case GROUP_HANDSHAKE:
            case COMPLETED:
                return true;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                return false;
            default:
                return false;
        }
    }
}
