package com.thanosfisherman.wifiutils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WpsInfo;
import android.os.Build;
import android.os.PatternMatcher;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.thanosfisherman.elvis.Objects;
import com.thanosfisherman.wifiutils.utils.SSIDUtils;
import com.thanosfisherman.wifiutils.utils.VersionUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.DisconnectCallbackHolder;
import com.thanosfisherman.wifiutils.wifiConnect.WifiConnectionCallback;
import com.thanosfisherman.wifiutils.wifiWps.ConnectionWpsListener;

import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static com.thanosfisherman.elvis.Elvis.of;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;
import static com.thanosfisherman.wifiutils.utils.SSIDUtils.convertToQuotedString;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isAndroidQOrLater;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isJellyBeanOrLater;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isLollipopOrLater;
import static com.thanosfisherman.wifiutils.utils.VersionUtils.isMarshmallowOrLater;

@SuppressLint("MissingPermission")
public final class ConnectorUtils {
    private static final int MAX_PRIORITY = 99999;

    public static boolean isAlreadyConnected(@Nullable WifiManager wifiManager, @Nullable String bssid) {
        if (bssid != null && wifiManager != null) {
            if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getBSSID() != null &&
                    wifiManager.getConnectionInfo().getIpAddress() != 0 &&
                    Objects.equals(bssid, wifiManager.getConnectionInfo().getBSSID())) {
                wifiLog("Already connected to: " + wifiManager.getConnectionInfo().getSSID() + "  BSSID: " + wifiManager.getConnectionInfo().getBSSID());
                return true;
            }
        }
        return false;
    }

    public static boolean isAlreadyConnected2(@Nullable WifiManager wifiManager, @Nullable String ssid) {
        if (ssid != null && wifiManager != null) {
            if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null &&
                    wifiManager.getConnectionInfo().getIpAddress() != 0 &&
                    Objects.equals(ssid, wifiManager.getConnectionInfo().getSSID())) {
                wifiLog("Already connected to: " + wifiManager.getConnectionInfo().getSSID() + "  BSSID: " + wifiManager.getConnectionInfo().getBSSID());
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static boolean isConnectedToNetworkLollipop(@Nullable ConnectivityManager connectivityManager) {

//        final ConnectivityManager connMgr =
//                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;
        boolean isWifiConn = false;
        for (Network network : connectivityManager.getAllNetworks()) {
            final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo != null && ConnectivityManager.TYPE_WIFI == networkInfo.getType()) {
                isWifiConn |= networkInfo.isConnected();
            }
        }
        return isWifiConn;
    }

    public static boolean isAlreadyConnected(@Nullable ConnectivityManager connectivityManager) {
        if (isLollipopOrLater()) {
            return isConnectedToNetworkLollipop(connectivityManager);
        }
        return of(connectivityManager).next(manager -> manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).next(NetworkInfo::getState).next(state -> state == NetworkInfo.State.CONNECTED).getBoolean();
    }

    public static boolean isAlreadyConnected(@Nullable WifiManager wifiManager, @Nullable ConnectivityManager connectivityManager, @Nullable String ssid) {

        boolean result = isAlreadyConnected(connectivityManager);

        if (result) {
            if (ssid != null && wifiManager != null) {
                String quotedSsid = ssid;
                if (VersionUtils.isJellyBeanOrLater()) {
                    quotedSsid = SSIDUtils.convertToQuotedString(ssid);
                }
                final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String tempSSID = wifiInfo.getSSID();
                result = tempSSID != null && tempSSID.equals(quotedSsid);
            }
        }
        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean checkForExcessOpenNetworkAndSave(@NonNull final ContentResolver resolver, @NonNull final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        final int numOpenNetworksKept = isJellyBeanOrLater()
                ? Settings.Secure.getInt(resolver, Settings.Global.WIFI_NUM_OPEN_NETWORKS_KEPT, 10)
                : Settings.Secure.getInt(resolver, Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);

        for (int i = configurations.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configurations.get(i);
            if (Objects.equals(ConfigSecurities.SECURITY_NONE, ConfigSecurities.getSecurity(config))) {
                tempCount++;
                if (tempCount >= numOpenNetworksKept) {
                    modified = true;
                    wifiMgr.removeNetwork(config.networkId);
                }
            }
        }
        return !modified || wifiMgr.saveConfiguration();

    }

    private static int getMaxPriority(@Nullable final WifiManager wifiManager) {
        if (wifiManager == null) {
            return 0;
        }
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations) {
            if (config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    private static int shiftPriorityAndSave(@Nullable final WifiManager wifiMgr) {
        if (wifiMgr == null) {
            return 0;
        }
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }


    @Nullable
    private static String trimQuotes(@Nullable String str) {
        if (str != null && !str.isEmpty()) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }
        return str;
    }

    @SuppressWarnings("unused")
    public static int getPowerPercentage(int power) {
        int i;
        if (power <= -93) {
            i = 0;
        } else if (-25 <= power && power <= 0) {
            i = 100;
        } else {
            i = 125 + power;
        }
        return i;
    }

    public static boolean isHexWepKey(@Nullable String wepKey) {
        final int passwordLen = wepKey == null ? 0 : wepKey.length();
        return (passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && wepKey.matches("[0-9A-Fa-f]*");
    }


    private static void sortByPriority(@NonNull final List<WifiConfiguration> configurations) {
        Collections.sort(configurations, (o1, o2) -> o1.priority - o2.priority);
    }

    @SuppressWarnings("unused")
    public static int frequencyToChannel(int freq) {
        if (2412 <= freq && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (5170 <= freq && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    static void registerReceiver(@NonNull final Context context, @Nullable final BroadcastReceiver receiver, @NonNull final IntentFilter filter) {
        if (receiver != null) {
            try {
                context.registerReceiver(receiver, filter);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    static void unregisterReceiver(@NonNull final Context context, @Nullable final BroadcastReceiver receiver) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean connectToWifi(@NonNull final Context context, @Nullable final WifiManager wifiManager, @Nullable final ConnectivityManager connectivityManager, @NonNull WeakHandler handler, @NonNull final ScanResult scanResult, @NonNull final String password, @NonNull WifiConnectionCallback wifiConnectionCallback, boolean patternMatch, @Nullable String ssid) {
        if (wifiManager == null || connectivityManager == null) {
            return false;
        }

        if (isAndroidQOrLater()) {
            return connectAndroidQ(wifiManager, connectivityManager, handler, wifiConnectionCallback, scanResult, password, patternMatch, ssid);
        }

        return connectPreAndroidQ(context, wifiManager, scanResult, password);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean connectToWifiHidden(@NonNull final Context context,
                                       @Nullable final WifiManager wifiManager,
                                       @Nullable final ConnectivityManager connectivityManager,
                                       @NonNull WeakHandler handler,
//                                       @NonNull final ScanResult scanResult,
                                       @NonNull final String ssid,
                                       @Nullable final String type,
                                       @NonNull final String password,
                                       @NonNull WifiConnectionCallback wifiConnectionCallback) {
        if (wifiManager == null || connectivityManager == null || type == null) {
            return false;
        }

        if (isAndroidQOrLater()) {
            return connectAndroidQHidden(wifiManager, connectivityManager, handler, wifiConnectionCallback, ssid, type, password);
        }

        return connectPreAndroidQHidden(context, wifiManager, ssid, type, password);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectPreAndroidQ(@NonNull final Context context, @Nullable final WifiManager wifiManager, @NonNull final ScanResult scanResult, @NonNull final String password) {
        if (wifiManager == null) {
            return false;
        }

        WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        if (config != null && password.isEmpty()) {
            wifiLog("PASSWORD WAS EMPTY. TRYING TO CONNECT TO EXISTING NETWORK CONFIGURATION");
            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        if (!cleanPreviousConfiguration(wifiManager, config)) {
            wifiLog("COULDN'T REMOVE PREVIOUS CONFIG, CONNECTING TO EXISTING ONE");
            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        final String security = ConfigSecurities.getSecurity(scanResult);

        if (Objects.equals(ConfigSecurities.SECURITY_NONE, security)) {
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);
        }

        config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        ConfigSecurities.setupSecurity(config, security, password);

        int id = wifiManager.addNetwork(config);
        wifiLog("Network ID: " + id);
        if (id == -1) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            wifiLog("Couldn't save wifi config");
            return false;
        }
        // We have to retrieve the WifiConfiguration after save
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null) {
            wifiLog("Error getting wifi config after save. (config == null)");
            return false;
        }

        return connectToConfiguredNetwork(wifiManager, config, true);
    }


    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectPreAndroidQHidden(@NonNull final Context context, @Nullable final WifiManager wifiManager, @NonNull final String ssid, @NonNull final String type, @NonNull final String password) {
        if (wifiManager == null) {
            return false;
        }
//
        WifiConfiguration config;

        final String security = ConfigSecurities.getSecurity(type);

        if (Objects.equals(ConfigSecurities.SECURITY_NONE, security)) {
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);
        }

        config = new WifiConfiguration();
        config.SSID = convertToQuotedString(ssid);
        ConfigSecurities.setupSecurityHidden(config, security, password);

        int id = wifiManager.addNetwork(config);
        wifiLog("Hidden-Network ID: " + id);
        if (id == -1) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            wifiLog("Couldn't save wifi config");
            return false;
        }
        // We have to retrieve the WifiConfiguration after save
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null) {
            wifiLog("Error getting wifi config after save. (config == null)");
            return false;
        }

        return connectToConfiguredNetwork(wifiManager, config, true);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectToConfiguredNetwork(@Nullable WifiManager wifiManager, @Nullable WifiConfiguration config, boolean reassociate) {
        if (config == null || wifiManager == null) {
            return false;
        }

        if (isMarshmallowOrLater()) {
            return disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
        }

        // Make it the highest priority.
        int newPri = getMaxPriority(wifiManager) + 1;
        if (newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiManager);
            config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
            if (config == null) {
                return false;
            }
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiManager.updateNetwork(config);
        if (networkId == -1) {
            return false;
        }

        // Do not disable others
        if (!wifiManager.enableNetwork(networkId, false)) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        return config != null && disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean connectAndroidQ(@Nullable WifiManager wifiManager, @Nullable ConnectivityManager connectivityManager, @NonNull WeakHandler handler, @NonNull WifiConnectionCallback wifiConnectionCallback, @NonNull ScanResult scanResult, @NonNull String password, boolean patternMatch, @Nullable String ssid) {
        if (connectivityManager == null) {
            return false;
        }

        WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder = new WifiNetworkSpecifier.Builder();

        if (patternMatch) {
            wifiNetworkSpecifierBuilder.setSsidPattern(new PatternMatcher(ssid != null ? ssid : scanResult.SSID, PatternMatcher.PATTERN_PREFIX));
        } else {
            wifiNetworkSpecifierBuilder
                    .setSsid(scanResult.SSID)
                    .setBssid(MacAddress.fromString(scanResult.BSSID));
        }

        final String security = ConfigSecurities.getSecurity(scanResult);

        ConfigSecurities.setupWifiNetworkSpecifierSecurities(wifiNetworkSpecifierBuilder, security, password);

        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(wifiNetworkSpecifierBuilder.build())
//                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build();

        // cleanup previous connections just in case
        DisconnectCallbackHolder.getInstance().disconnect();

        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);

                wifiLog("AndroidQ+ connected to wifi ");

                // TODO: should this actually be in the success listener on WifiUtils?
                // We could pass the networkrequest maybe?

                // bind so all api calls are performed over this new network
                // if we don't bind, connection with the wifi network is immediately dropped

                DisconnectCallbackHolder.getInstance().bindProcessToNetwork(network);
                connectivityManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);

                // On some Android 10 devices, connection is made and than immediately lost due to a firmware bug,
                // read more here: https://github.com/ThanosFisherman/WifiUtils/issues/63.
                handler.postDelayed(() -> {
                    if (isAlreadyConnected(wifiManager, of(scanResult).next(scanResult1 -> scanResult1.BSSID).get())) {
                        wifiConnectionCallback.successfulConnect();
                    } else {
                        wifiConnectionCallback.errorConnect(ConnectionErrorCode.ANDROID_10_IMMEDIATELY_DROPPED_CONNECTION);
                    }
                }, 500);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();

                wifiLog("AndroidQ+ could not connect to wifi");

                wifiConnectionCallback.errorConnect(ConnectionErrorCode.USER_CANCELLED);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                wifiLog("onLost");

                // cancel connecting if needed, this prevents 'request loops' on some oneplus/redmi phones
                DisconnectCallbackHolder.getInstance().unbindProcessFromNetwork();
                DisconnectCallbackHolder.getInstance().disconnect();

            }
        };

        DisconnectCallbackHolder.getInstance().addNetworkCallback(networkCallback, connectivityManager);

        wifiLog("connecting with Android 10");
        DisconnectCallbackHolder.getInstance().requestNetwork(networkRequest);

        return true;
    }

    // FIXME: we should use WifiNetworkSuggestion api to connect WLAN on Android 10, I`ll fix it soon.
    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean connectAndroidQHidden(@Nullable WifiManager wifiManager, @Nullable ConnectivityManager connectivityManager, @NonNull WeakHandler handler, @NonNull WifiConnectionCallback wifiConnectionCallback, @NonNull String ssid, @NonNull String type, String password) {
        if (connectivityManager == null) {
            return false;
        }

        WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder = new WifiNetworkSpecifier.Builder()
                .setIsHiddenSsid(true)
                .setSsid(ssid);

        final String security = ConfigSecurities.getSecurity(type);

        ConfigSecurities.setupWifiNetworkSpecifierSecurities(wifiNetworkSpecifierBuilder, security, password);

        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .setNetworkSpecifier(wifiNetworkSpecifierBuilder.build())
                .build();

//        // cleanup previous connections just in case
        DisconnectCallbackHolder.getInstance().disconnect();

        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                wifiLog("AndroidQ+ connected to wifi ");
                // TODO: should this actually be in the success listener on WifiUtils?
                // We could pass the networkrequest maybe?

                // bind so all api calls are performed over this new network
                // if we don't bind, connection with the wifi network is immediately dropped

                DisconnectCallbackHolder.getInstance().bindProcessToNetwork(network);
                connectivityManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);

                // On some Android 10 devices, connection is made and than immediately lost due to a firmware bug,
                // read more here: https://github.com/ThanosFisherman/WifiUtils/issues/63.
                handler.postDelayed(() -> {
                    if (isAlreadyConnected(wifiManager, ssid)) {
                        wifiConnectionCallback.successfulConnect();
                    } else {
                        wifiConnectionCallback.errorConnect(ConnectionErrorCode.ANDROID_10_IMMEDIATELY_DROPPED_CONNECTION);
                    }
                }, 500);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();

                wifiLog("AndroidQ+ could not connect to wifi");

                wifiConnectionCallback.errorConnect(ConnectionErrorCode.USER_CANCELLED);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                wifiLog("onLost");

                // cancel connecting if needed, this prevents 'request loops' on some oneplus/redmi phones
                DisconnectCallbackHolder.getInstance().unbindProcessFromNetwork();
                DisconnectCallbackHolder.getInstance().disconnect();

            }

            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                wifiLog("onLost");
            }
        };

        DisconnectCallbackHolder.getInstance().addNetworkCallback(networkCallback, connectivityManager);

        wifiLog("connecting with Android 10");
        DisconnectCallbackHolder.getInstance().requestNetwork(networkRequest);

        return true;
    }

    private static boolean disableAllButOne(@Nullable final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        if (wifiManager == null) {
            return false;
        }
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || config == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;

        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig == null) {
                continue;
            }
            if (wifiConfig.networkId == config.networkId) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            } else {
                wifiManager.disableNetwork(wifiConfig.networkId);
            }
        }
        wifiLog("disableAllButOne " + result);
        return result;
    }


    @SuppressWarnings("UnusedReturnValue")
    private static boolean disableAllButOne(@Nullable final WifiManager wifiManager, @Nullable final ScanResult scanResult) {
        if (wifiManager == null) {
            return false;
        }
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || scanResult == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;
        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig == null) {
                continue;
            }
            if (Objects.equals(scanResult.BSSID, wifiConfig.BSSID) && Objects.equals(scanResult.SSID, trimQuotes(wifiConfig.SSID))) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            } else {
                wifiManager.disableNetwork(wifiConfig.networkId);
            }
        }
        return result;
    }

    public static boolean reEnableNetworkIfPossible(@Nullable final WifiManager wifiManager, @Nullable final ScanResult scanResult) {
        if (wifiManager == null) {
            return false;
        }
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || scanResult == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;
        for (WifiConfiguration wifiConfig : configurations)
            if (Objects.equals(scanResult.BSSID, wifiConfig.BSSID) && Objects.equals(scanResult.SSID, trimQuotes(wifiConfig.SSID))) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
                break;
            }
        wifiLog("reEnableNetworkIfPossible " + result);
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static void connectWps(@Nullable final WifiManager wifiManager, @NonNull WeakHandler handler, @NonNull final ScanResult scanResult, @NonNull String pin, long timeOutMillis,
                           @NonNull final ConnectionWpsListener connectionWpsListener) {
        if (wifiManager == null) {
            connectionWpsListener.isSuccessful(false);
            return;
        }

        final WpsInfo wpsInfo = new WpsInfo();
        final Runnable handlerTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                wifiManager.cancelWps(null);
                wifiLog("Connection with WPS has timed out");
                cleanPreviousConfiguration(wifiManager, scanResult);
                connectionWpsListener.isSuccessful(false);
                handler.removeCallbacks(this);
            }
        };

        final WifiManager.WpsCallback wpsCallback = new WifiManager.WpsCallback() {
            @Override
            public void onStarted(String pin) {
            }

            @Override
            public void onSucceeded() {
                handler.removeCallbacks(handlerTimeoutRunnable);
                wifiLog("CONNECTED With WPS successfully");
                connectionWpsListener.isSuccessful(true);
            }

            @Override
            public void onFailed(int reason) {
                handler.removeCallbacks(handlerTimeoutRunnable);
                final String reasonStr;
                switch (reason) {
                    case 3:
                        reasonStr = "WPS_OVERLAP_ERROR";
                        break;
                    case 4:
                        reasonStr = "WPS_WEP_PROHIBITED";
                        break;
                    case 5:
                        reasonStr = "WPS_TKIP_ONLY_PROHIBITED";
                        break;
                    case 6:
                        reasonStr = "WPS_AUTH_FAILURE";
                        break;
                    case 7:
                        reasonStr = "WPS_TIMED_OUT";
                        break;
                    default:
                        reasonStr = String.valueOf(reason);
                }
                wifiLog("FAILED to connect with WPS. Reason: " + reasonStr);
                cleanPreviousConfiguration(wifiManager, scanResult);
                reenableAllHotspots(wifiManager);
                connectionWpsListener.isSuccessful(false);
            }
        };

        wifiLog("Connecting with WPS...");
        wpsInfo.setup = WpsInfo.KEYPAD;
        wpsInfo.BSSID = scanResult.BSSID;
        wpsInfo.pin = pin;
        wifiManager.cancelWps(null);

        if (!cleanPreviousConfiguration(wifiManager, scanResult)) {
            disableAllButOne(wifiManager, scanResult);
        }

        handler.postDelayed(handlerTimeoutRunnable, timeOutMillis);
        wifiManager.startWps(wpsInfo, wpsCallback);
    }

    @RequiresPermission(ACCESS_WIFI_STATE)
    static boolean disconnectFromWifi(@NonNull final WifiManager wifiManager) {
        return wifiManager.disconnect();
    }

    @RequiresPermission(ACCESS_WIFI_STATE)
    static boolean removeWifi(@NonNull final WifiManager wifiManager, @NonNull final String ssid) {
        final WifiConfiguration wifiConfiguration = ConfigSecurities.getWifiConfiguration(wifiManager, ssid);
        return cleanPreviousConfiguration(wifiManager, wifiConfiguration);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean cleanPreviousConfiguration(@Nullable final WifiManager wifiManager, @NonNull final ScanResult scanResult) {
        if (wifiManager == null) {
            return false;
        }
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        final WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        wifiLog("Attempting to remove previous network config...");
        if (config == null) {
            return true;
        }

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    static boolean cleanPreviousConfiguration(@Nullable final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        if (wifiManager == null) {
            return false;
        }

        wifiLog("Attempting to remove previous network config...");
        if (config == null) {
            return true;
        }

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    static void reenableAllHotspots(@Nullable WifiManager wifi) {
        if (wifi == null) {
            return;
        }
        final List<WifiConfiguration> configurations = wifi.getConfiguredNetworks();
        if (configurations != null && !configurations.isEmpty()) {
            for (final WifiConfiguration config : configurations) {
                wifi.enableNetwork(config.networkId, false);
            }
        }
    }

    @Nullable
    static ScanResult matchScanResultSsid(@NonNull String ssid, @NonNull Iterable<ScanResult> results, boolean mPatternMatch) {
        for (ScanResult result : results) {
            if (mPatternMatch ? result.SSID.startsWith(ssid) : Objects.equals(result.SSID, ssid)) {
                return result;
            }
        }
        return null;
    }

    @Nullable
    static ScanResult matchScanResult(@NonNull String ssid, @NonNull String bssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results) {
            if (Objects.equals(result.SSID, ssid) && Objects.equals(result.BSSID, bssid)) {
                return result;
            }
        }
        return null;
    }

    @Nullable
    static ScanResult matchScanResultBssid(@NonNull String bssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results) {
            if (Objects.equals(result.BSSID, bssid)) {
                return result;
            }
        }
        return null;
    }
}