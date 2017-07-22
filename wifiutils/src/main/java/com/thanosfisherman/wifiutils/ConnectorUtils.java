package com.thanosfisherman.wifiutils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.thanosfisherman.wifiutils.wifiState.WifiStateReceiver;

import java.util.Comparator;
import java.util.List;

public final class ConnectorUtils
{

    @NonNull private static final String TAG = ConnectorUtils.class.getSimpleName();
    static final int MAX_PRIORITY = 99999;
    private static boolean mEnableLog;


    public static boolean isConnectedToBSSID(WifiManager wifiManager, String BSSID)
    {
        if (wifiManager.getConnectionInfo().getBSSID() != null && wifiManager.getConnectionInfo().getBSSID().equals(BSSID))
        {
            wifiLog("Already connected to: " + wifiManager.getConnectionInfo().getSSID() + "  BSSID: " + wifiManager.getConnectionInfo().getBSSID() + "  " +
                    BSSID);
            return true;
        }
        return false;
    }

    public static void enableLog(boolean enabled)
    {
        mEnableLog = enabled;
    }

    public static void wifiLog(String text)
    {
        if (mEnableLog)
            Log.d(TAG, "WifiConnector: " + text);
    }


    static boolean checkForExcessOpenNetworkAndSave(final ContentResolver resolver, final WifiManager wifiMgr)
    {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        final int numOpenNetworksKept = Build.VERSION.SDK_INT >= 17 ? Settings.Secure.getInt(resolver, Settings.Global.WIFI_NUM_OPEN_NETWORKS_KEPT, 10)
                : Settings.Secure.getInt(resolver, Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);

        for (int i = configurations.size() - 1; i >= 0; i--)
        {
            final WifiConfiguration config = configurations.get(i);
            if (ConfigSecurities.SECURITY_NONE == ConfigSecurities.getSecurity(config))
            {
                tempCount++;
                if (tempCount >= numOpenNetworksKept)
                {
                    modified = true;
                    wifiMgr.removeNetwork(config.networkId);
                }
            }
        }
        return !modified || wifiMgr.saveConfiguration();

    }

    static int getMaxPriority(final WifiManager wifiManager)
    {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations)
        {
            if (config.priority > pri)
            {
                pri = config.priority;
            }
        }
        return pri;
    }

    static int shiftPriorityAndSave(final WifiManager wifiMgr)
    {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++)
        {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }


    private static String trimQuotes(String str)
    {
        if (!str.isEmpty())
        {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }
        return str;
    }

    public static int getPowerPercentage(int power)
    {
        int i;
        if (power <= -93)
            i = 0;
        else if (-25 <= power && power <= 0)
            i = 100;
        else
            i = 125 + power;
        return i;
    }

    public static String convertToQuotedString(String string)
    {
        if (TextUtils.isEmpty(string))
            return "";

        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"'))
            return string;

        return "\"" + string + "\"";
    }

    public static boolean isHexWepKey(@Nullable String wepKey)
    {
        final int passwordLen = wepKey == null ? 0 : wepKey.length();
        return passwordLen != 0 && (passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && wepKey.matches("[0-9A-Fa-f]*");
    }


    private static void sortByPriority(final List<WifiConfiguration> configurations)
    {
        java.util.Collections.sort(configurations, new Comparator<WifiConfiguration>()
        {
            @Override
            public int compare(WifiConfiguration o1, WifiConfiguration o2)
            {
                return o1.priority - o2.priority;
            }
        });
    }

    public static int frequencyToChannel(int freq)
    {
        if (2412 <= freq && freq <= 2484)
            return (freq - 2412) / 5 + 1;
        else if (5170 <= freq && freq <= 5825)
            return (freq - 5170) / 5 + 34;
        else
            return -1;
    }

    public static boolean enableWifiAndScan(WifiManager wifi)
    {
        if (wifi.isWifiEnabled())
        {
            if (wifi.startScan())
                return true;
        }
        else if (wifi.setWifiEnabled(true))
        {
            if (wifi.startScan())
                return true;
        }
        return false;
    }

    public static void reenableAllHotspots(WifiManager wifi)
    {
        final List<WifiConfiguration> configurations = wifi.getConfiguredNetworks();
        if (configurations != null && !configurations.isEmpty())
        {
            for (final WifiConfiguration config : configurations)
            {
                wifi.enableNetwork(config.networkId, false);
            }
        }
    }

    static void registerReceiver(@NonNull Context context, @Nullable BroadcastReceiver receiver, @NonNull ReceiverCallbacks callbacks,
                                 @NonNull IntentFilter filter)
    {
        if (receiver == null)
        {
            receiver = new WifiStateReceiver(callbacks);
            try
            {
                context.registerReceiver(receiver, filter);
            }
            catch (Exception e)
            {
                wifiLog("Exception on registering broadcast for listening Wifi State:");
                receiver = null;
            }
        }
    }

    static void unregisterReceiver(Context context, BroadcastReceiver receiver)
    {
        if (receiver != null)
        {
            try
            {
                context.unregisterReceiver(receiver);
                receiver = null;
            }
            catch (Exception e)
            {
                wifiLog("Exception on unregistering broadcast for listening Wifi State:");
            }
        }
    }

    public static void connectToWifi(@NonNull Context context, @NonNull WifiManager wifiManager, @NonNull ScanResult scanResult, @Nullable String password)
    {
        if (ConnectorUtils.isConnectedToBSSID(wifiManager, "wifiConfiguration.bssid"))
        {
            //connectionResultListener.errorConnect(SAME_NETWORK);
            wifiLog("Already connected to this network");
            return;
        }
        cleanPreviousConfiguration(wifiManager, scanResult);

        final int security = ConfigSecurities.getSecurity(scanResult);

        if (ConfigSecurities.SECURITY_NONE == security)
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        ConfigSecurities.setupSecurity(config, security, password);

        int id = wifiManager.addNetwork(config);
        wifiLog("Network ID: " + id);
        if (id == -1)
        {
            //TODO: Error Listener here
            return;
        }

        if (!wifiManager.saveConfiguration())
        {
            wifiLog("Couldn't save wifi config");
            //TODO: Error Listener here
            return;
        }
        // We have to retrieve the WifiConfiguration after save
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null)
        {
            wifiLog("Error getting wifi config after save. (config == null)");
            //TODO: Error Listener here
            return;
        }
        if (!connectToConfiguredNetwork(wifiManager, config, true))
        {
            //TODO: LISTENER FAILED
        }
    }

    public static boolean connectToConfiguredNetwork(@NonNull WifiManager wifiManager, @NonNull WifiConfiguration config, boolean reassociate)
    {
        if (Build.VERSION.SDK_INT >= 23)
            return wifiManager.enableNetwork(config.networkId, true) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());

        int oldPri = config.priority;
        // Make it the highest priority.
        int newPri = getMaxPriority(wifiManager) + 1;
        if (newPri > MAX_PRIORITY)
        {
            newPri = shiftPriorityAndSave(wifiManager);
            config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
            if (config == null)
            {
                return false;
            }
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiManager.updateNetwork(config);
        if (networkId == -1)
        {
            return false;
        }

        // Do not disable others
        if (!wifiManager.enableNetwork(networkId, false))
        {
            config.priority = oldPri;
            return false;
        }

        if (!wifiManager.saveConfiguration())
        {
            config.priority = oldPri;
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null)
        {
            return false;
        }

        // Disable others, but do not save.
        // Just to force the WifiManager to connect to it.
        if (!wifiManager.enableNetwork(config.networkId, true))
        {
            return false;
        }

        return reassociate ? wifiManager.reassociate() : wifiManager.reconnect();
    }

    public static void cleanPreviousConfiguration(@NonNull final WifiManager wifiManager, @NonNull final ScanResult scanResult)
    {

        final WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        if (config != null)
        {
            wifiLog("Found previous network confing. Cleaning nao");
            wifiManager.removeNetwork(config.networkId);
            wifiManager.saveConfiguration();
        }
    }
}
