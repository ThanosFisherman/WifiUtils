package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.thanosfisherman.wifiutils.WeakHandler;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver
{
    @NonNull private WifiConnectionCallback mWifiConnectionCallback;
    private int attempts;
    private long mDelay;
    @NonNull private WeakHandler handler;
    @NonNull private final Runnable handlerCallback = new Runnable()
    {
        @Override
        public void run()
        {
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
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);

            if (state == null)
            {
                mWifiConnectionCallback.errorConnect();
                return;
            }

            wifiLog("Connection Broadcast action: " + state);
            switch (state)
            {
                case COMPLETED:
                    handler.removeCallbacks(handlerCallback);
                    mWifiConnectionCallback.successfulConnect();
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
                    if (attempts >= 1)
                    {
                        handler.removeCallbacks(handlerCallback);
                        mWifiConnectionCallback.errorConnect();
                        attempts = 0;
                    }
                    attempts++;

                    break;
                case AUTHENTICATING:
                    break;
            }
        }
    }

    public void setTimeout(long millis)
    {
        this.mDelay = millis;
    }

    public WifiConnectionReceiver activateTimeoutHandler()
    {
        handler.postDelayed(handlerCallback, mDelay);
        return this;
    }
}
