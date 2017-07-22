package com.thanosfisherman.wifiutils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

import com.thanosfisherman.wifiutils.ReceiverCallbacks;

import static com.thanosfisherman.wifiutils.ConnectorUtils.wifiLog;


public final class WifiConnectionReceiver extends BroadcastReceiver
{
    private WifiConnectionCallback callback;

    public WifiConnectionReceiver(ReceiverCallbacks callback)
    {
        this.callback = (WifiConnectionCallback) callback;
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
            wifiLog("Broadcast action: " + state);
            switch (state)
            {
                case COMPLETED:
                    callback.successfulConnect();
                    break;
                case DISCONNECTED:
                    int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    wifiLog("Disconnected... Supplicant error: " + supl_error);
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                    {
                        wifiLog("Authentication error...");
                    }
                    callback.errorConnect();
                    break;
                case AUTHENTICATING:
                    wifiLog("Authenticating...");
                    break;
            }

        }
    }
}
