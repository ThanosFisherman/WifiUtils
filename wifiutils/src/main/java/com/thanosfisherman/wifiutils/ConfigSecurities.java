package com.thanosfisherman.wifiutils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static com.thanosfisherman.wifiutils.ConnectorUtils.convertToQuotedString;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

final class ConfigSecurities
{
    static final int SECURITY_NONE = 0x00;
    static final int SECURITY_WEP = 0x01;
    static final int SECURITY_PSK = 0x02;
    static final int SECURITY_EAP = 0x03;
    @NonNull private static final String[] SECURITY_MODES = {"Open", "WEP", "WPA", "WPA2", "WPA_EAP", "IEEE8021X"};

    /**
     * Fill in the security fields of WifiConfiguration config.
     *
     * @param config   The object to fill.
     * @param security If is OPEN, password is ignored.
     * @param password Password of the network if security is not OPEN.
     */

    static void setupSecurity(@NonNull WifiConfiguration config, int security, @Nullable final String password)
    {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        final boolean isPassOk = password != null && !password.isEmpty();
        switch (security)
        {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                if (isPassOk)
                {
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if (ConnectorUtils.isHexWepKey(password))
                        config.wepKeys[0] = password;
                    else
                        config.wepKeys[0] = convertToQuotedString(password);
                }
                break;
            case SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (isPassOk)
                {
                    if (password.matches("[0-9A-Fa-f]{64}"))
                        config.preSharedKey = password;
                    else
                        config.preSharedKey = convertToQuotedString(password);
                }
                break;
            case SECURITY_EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

                if (isPassOk)
                    config.preSharedKey = convertToQuotedString(password);
                break;

            default:
                wifiLog("Invalid security type: " + security);
        }
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final WifiConfiguration configToFind)
    {
        final String ssid = configToFind.SSID;
        if (ssid.isEmpty())
        {
            return null;
        }

        final String bssid = configToFind.BSSID;

        final int security = getSecurity(configToFind);


        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null)
        {
            wifiLog("NULL configs");
            return null;
        }

        for (final WifiConfiguration config : configurations)
        {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID))
            {
                final int configSecurity = getSecurity(config);

                if (security == configSecurity)
                    return config;
            }
        }
        wifiLog("Couldn't find " + ssid);
        return null;
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final ScanResult scanResult)
    {
        final String ssid = convertToQuotedString(scanResult.SSID);
        if (ssid.isEmpty())
            return null;

        final String bssid = scanResult.BSSID;
        if (bssid == null)
            return null;

        final int security = getSecurity(scanResult);

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null)
            return null;

        for (final WifiConfiguration config : configurations)
        {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID))
            {
                final int configSecurity = getSecurity(config);
                if (security == configSecurity)
                    return config;
            }
        }
        return null;
    }

    static int getSecurity(@NonNull WifiConfiguration config)
    {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
            return SECURITY_PSK;
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X))
            return SECURITY_EAP;
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    static int getSecurity(@NonNull ScanResult result)
    {
        if (result.capabilities.contains("WEP"))
            return SECURITY_WEP;
        else if (result.capabilities.contains("PSK"))
            return SECURITY_PSK;
        else if (result.capabilities.contains("EAP"))
            return SECURITY_EAP;
        return SECURITY_NONE;
    }

    /**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getSecurityPretty(@Nullable ScanResult scanResult)
    {
        if (scanResult == null)
            return "";
        final String cap = scanResult.capabilities;
        String result = SECURITY_MODES[0];
        for (int i = 1; i < SECURITY_MODES.length; i++)
        {
            if (cap.contains(SECURITY_MODES[i]))
            {
                result = SECURITY_MODES[i];
                break;
            }
        }
        if (cap.contains("WPS"))
            result = result + ", WPS";
        return result;
    }

}
