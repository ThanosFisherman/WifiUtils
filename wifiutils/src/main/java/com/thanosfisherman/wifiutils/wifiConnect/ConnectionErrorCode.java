package com.thanosfisherman.wifiutils.wifiConnect;

/**
 * Errors that can occur when trying to connect to a wifi network.
 */
public enum ConnectionErrorCode {
    /**
     * Starting Android 10, apps are no longer allowed to enable wifi.
     * User has to manually do this.
     */
    COULD_NOT_ENABLE_WIFI,
    /**
     * Starting Android 9, it's only allowed to scan 4 times per 2 minuts in a foreground app.
     * https://developer.android.com/guide/topics/connectivity/wifi-scan
     */
    COULD_NOT_SCAN,
    /**
     * If the wifi network is not in range, the security type is unknown and WifiUtils doesn't support
     * connecting to the network.
     */
    DID_NOT_FIND_NETWORK_BY_SCANNING,
    /**
     * Authentication error occurred while trying to connect.
     * The password could be incorrect or the user could have a saved network configuration with a
     * different password!
     */
    AUTHENTICATION_ERROR_OCCURRED,
    /**
     * Could not connect in the timeout window.
     */
    TIMEOUT_OCCURRED,
    COULD_NOT_CONNECT,
}
