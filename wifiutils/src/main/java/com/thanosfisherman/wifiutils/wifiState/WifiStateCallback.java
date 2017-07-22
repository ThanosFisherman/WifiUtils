package com.thanosfisherman.wifiutils.wifiState;

import com.thanosfisherman.wifiutils.ReceiverCallbacks;

public interface WifiStateCallback extends ReceiverCallbacks
{
    void onWifiEnabled();
    void onWifiDisabled();
}
