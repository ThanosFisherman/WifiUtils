package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver
{
    private WifiConnectionCallback callback;
    private int attempts;

    public WifiConnectionReceiver(WifiConnectionCallback callback)
    {
        this.callback = callback;
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
                callback.errorConnect();
                return;
            }
            wifiLog("Connection Broadcast action: " + state);
            switch (state)
            {
                case COMPLETED:
                    callback.successfulConnect();
                    break;
                case DISCONNECTED:
                    final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                    {
                        wifiLog("Authentication error...");
                        callback.errorConnect();
                        return;
                    }
                    wifiLog("Disconnected. reattempting " + attempts);
                    if (attempts >= 1)
                    {
                        callback.errorConnect();
                        attempts = 0;
                    }
                    attempts++;

                    break;
                case AUTHENTICATING:
                    break;
            }

        }
    }
}
