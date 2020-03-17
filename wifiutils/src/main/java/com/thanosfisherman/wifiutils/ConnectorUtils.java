package com.thanosfisherman.wifiutils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WpsInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.thanosfisherman.elvis.Objects;
import com.thanosfisherman.wifiutils.wifiWps.ConnectionWpsListener;

import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

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


    @SuppressWarnings("UnusedReturnValue")
    private static boolean checkForExcessOpenNetworkAndSave(@NonNull final ContentResolver resolver, @NonNull final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        final int numOpenNetworksKept = Build.VERSION.SDK_INT >= 17
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
        if (wifiManager == null)
            return 0;
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
        if (wifiMgr == null)
            return 0;
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
        if (str != null && !str.isEmpty())
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        return str;
    }

    @SuppressWarnings("unused")
    public static int getPowerPercentage(int power) {
        int i;
        if (power <= -93)
            i = 0;
        else if (-25 <= power && power <= 0)
            i = 100;
        else
            i = 125 + power;
        return i;
    }

    @NonNull
    static String convertToQuotedString(@NonNull String string) {
        if (TextUtils.isEmpty(string))
            return "";

        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"'))
            return string;

        return "\"" + string + "\"";
    }

    static boolean isHexWepKey(@Nullable String wepKey) {
        final int passwordLen = wepKey == null ? 0 : wepKey.length();
        return (passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && wepKey.matches("[0-9A-Fa-f]*");
    }


    private static void sortByPriority(@NonNull final List<WifiConfiguration> configurations) {
        Collections.sort(configurations, (o1, o2) -> o1.priority - o2.priority);
    }

    @SuppressWarnings("unused")
    public static int frequencyToChannel(int freq) {
        if (2412 <= freq && freq <= 2484)
            return (freq - 2412) / 5 + 1;
        else if (5170 <= freq && freq <= 5825)
            return (freq - 5170) / 5 + 34;
        else
            return -1;
    }

    static void registerReceiver(@NonNull Context context, @Nullable BroadcastReceiver receiver, @NonNull IntentFilter filter) {
        if (receiver != null) {
            try {
                context.registerReceiver(receiver, filter);
            } catch (Exception e) {
            }
        }
    }

    static void unregisterReceiver(@NonNull Context context, @Nullable BroadcastReceiver receiver) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean connectToWifi(@NonNull Context context, @Nullable WifiManager wifiManager, @NonNull ScanResult scanResult, @NonNull String password) {
        if (wifiManager == null)
            return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return connectAndroidQ(connectivityManager, scanResult, password);
        }

        return connectPreAndroidQ(context, wifiManager, scanResult, password);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectPreAndroidQ(@NonNull Context context, @Nullable WifiManager wifiManager, @NonNull ScanResult scanResult, @NonNull String password) {
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

        if (Objects.equals(ConfigSecurities.SECURITY_NONE, security))
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);

        config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        ConfigSecurities.setupSecurity(config, security, password);

        int id = wifiManager.addNetwork(config);
        wifiLog("Network ID: " + id);
        if (id == -1)
            return false;

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
        if (config == null || wifiManager == null)
            return false;

        if (Build.VERSION.SDK_INT >= 23)
            return disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());

        // Make it the highest priority.
        int newPri = getMaxPriority(wifiManager) + 1;
        if (newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiManager);
            config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
            if (config == null)
                return false;
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiManager.updateNetwork(config);
        if (networkId == -1)
            return false;

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
    private static boolean connectAndroidQ(@Nullable ConnectivityManager connectivityManager, @NonNull ScanResult scanResult, @NonNull String password) {
        if (connectivityManager == null) {
            return false;
        }

        WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder = new WifiNetworkSpecifier.Builder()
                .setSsid(scanResult.SSID)
                .setBssid(MacAddress.fromString(scanResult.BSSID));

        final String security = ConfigSecurities.getSecurity(scanResult);

        ConfigSecurities.setupWifiNetworkSpecifierSecurities(wifiNetworkSpecifierBuilder, security, password);


        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifierBuilder.build())
                .build();

        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);

                wifiLog("AndroidQ+ connected to wifi ");
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();

                wifiLog("AndroidQ+ could not connect to wifi");
            }
        });

        return true;
    }

    private static boolean disableAllButOne(@Nullable final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        if (wifiManager == null)
            return false;
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || config == null || configurations.isEmpty())
            return false;
        boolean result = false;

        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig == null)
                continue;
            if (wifiConfig.networkId == config.networkId)
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            else
                wifiManager.disableNetwork(wifiConfig.networkId);
        }
        wifiLog("disableAllButOne " + result);
        return result;
    }


    @SuppressWarnings("UnusedReturnValue")
    private static boolean disableAllButOne(@Nullable final WifiManager wifiManager, @Nullable final ScanResult scanResult) {
        if (wifiManager == null)
            return false;
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || scanResult == null || configurations.isEmpty())
            return false;
        boolean result = false;
        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig == null)
                continue;
            if (Objects.equals(scanResult.BSSID, wifiConfig.BSSID) && Objects.equals(scanResult.SSID, trimQuotes(wifiConfig.SSID)))
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            else
                wifiManager.disableNetwork(wifiConfig.networkId);
        }
        return result;
    }

    public static boolean reEnableNetworkIfPossible(@Nullable final WifiManager wifiManager, @Nullable final ScanResult scanResult) {
        if (wifiManager == null)
            return false;
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || scanResult == null || configurations.isEmpty())
            return false;
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
    static void connectWps(@Nullable final WifiManager wifiManager, @NonNull final ScanResult scanResult, @NonNull String pin, long timeOutMillis,
                           @NonNull final ConnectionWpsListener connectionWpsListener) {
        if (wifiManager == null) {
            connectionWpsListener.isSuccessful(false);
            return;
        }
        final WeakHandler handler = new WeakHandler();
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

        if (!cleanPreviousConfiguration(wifiManager, scanResult))
            disableAllButOne(wifiManager, scanResult);

        handler.postDelayed(handlerTimeoutRunnable, timeOutMillis);
        wifiManager.startWps(wpsInfo, wpsCallback);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean cleanPreviousConfiguration(@Nullable final WifiManager wifiManager, @NonNull final ScanResult scanResult) {
        if (wifiManager == null)
            return false;
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        final WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        wifiLog("Attempting to remove previous network config...");
        if (config == null)
            return true;

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    static boolean cleanPreviousConfiguration(@Nullable final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        if (wifiManager == null)
            return false;

        wifiLog("Attempting to remove previous network config...");
        if (config == null)
            return true;

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    static void reenableAllHotspots(@Nullable WifiManager wifi) {
        if (wifi == null)
            return;
        final List<WifiConfiguration> configurations = wifi.getConfiguredNetworks();
        if (configurations != null && !configurations.isEmpty())
            for (final WifiConfiguration config : configurations)
                wifi.enableNetwork(config.networkId, false);
    }

    @Nullable
    static ScanResult matchScanResultSsid(@NonNull String ssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results)
            if (Objects.equals(result.SSID, ssid))
                return result;
        return null;
    }

    @Nullable
    static ScanResult matchScanResult(@NonNull String ssid, @NonNull String bssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results)
            if (Objects.equals(result.SSID, ssid) && Objects.equals(result.BSSID, bssid))
                return result;
        return null;
    }

    @Nullable
    static ScanResult matchScanResultBssid(@NonNull String bssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results)
            if (Objects.equals(result.BSSID, bssid))
                return result;
        return null;
    }
}
