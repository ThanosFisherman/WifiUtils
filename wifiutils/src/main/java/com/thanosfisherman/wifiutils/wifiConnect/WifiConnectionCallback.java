package com.thanosfisherman.wifiutils.wifiConnect;

import com.thanosfisherman.wifiutils.ReceiverCallbacks;

public interface WifiConnectionCallback extends ReceiverCallbacks
{
    void successfulConnect();
    void errorConnect();
}
