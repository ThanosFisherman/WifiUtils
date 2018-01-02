package com.thanosfisherman.wifiutils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.thanosfisherman.wifiutils.ConnectorUtils.convertToQuotedString;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;

final class ConfigSecurities {
    static final String SECURITY_NONE = "OPEN";
    static final String SECURITY_WEP = "WEP";
    static final String SECURITY_WPA = "WPA";
    static final String SECURITY_WPA2 = "WPA2";
    static final String SECURITY_WPA_EAP = "WPA_EAP";
    static final String SECURITY_IEEE8021X = "IEEE8021X";


    /**
     * Fill in the security fields of WifiConfiguration config.
     *
     * @param config   The object to fill.
     * @param security If is OPEN, password is ignored.
     * @param password Password of the network if security is not OPEN.
     */

    static void setupSecurity(@NonNull WifiConfiguration config, String security, @NonNull final String password) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        wifiLog("Setting up security " + security);
        switch (security) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if (ConnectorUtils.isHexWepKey(password))
                    config.wepKeys[0] = password;
                else
                    config.wepKeys[0] = convertToQuotedString(password);
                break;
            case SECURITY_WPA:
            case SECURITY_WPA2:
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                if (password.matches("[0-9A-Fa-f]{64}"))
                    config.preSharedKey = password;
                else
                    config.preSharedKey = convertToQuotedString(password);
                break;
            case SECURITY_WPA_EAP:
            case SECURITY_IEEE8021X:
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                config.preSharedKey = convertToQuotedString(password);
                break;

            default:
                wifiLog("Invalid security type: " + security);
        }
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final WifiConfiguration configToFind) {
        final String ssid = configToFind.SSID;
        if (ssid.isEmpty()) {
            return null;
        }

        final String bssid = configToFind.BSSID;

        final String security = getSecurity(configToFind);


        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            wifiLog("NULL configs");
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (Objects.equals(security, configSecurity))
                    return config;
            }
        }
        wifiLog("Couldn't find " + ssid);
        return null;
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final String ssid) {

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            wifiLog("NULL configs");
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            //wifiLog(config.SSID);
            if (Objects.equals(config.SSID, ssid)) {
                return config;
            }
        }
        wifiLog("Couldn't find " + ssid);
        return null;
    }
    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final ScanResult scanResult) {
        final String ssid = convertToQuotedString(scanResult.SSID);
        if (ssid.isEmpty())
            return null;

        final String bssid = scanResult.BSSID;
        if (bssid == null)
            return null;

        final String security = getSecurity(scanResult);

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null)
            return null;

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (Objects.equals(security, configSecurity))
                    return config;
            }
        }
        return null;
    }

    static String getSecurity(@NonNull WifiConfiguration config) {
        String security = SECURITY_NONE;
        List<String> securities = new ArrayList<>();
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            // If we never set group ciphers, wpa_supplicant puts all of them.
            // For open, we don't set group ciphers.
            // For WEP, we specifically only set WEP40 and WEP104, so CCMP
            // and TKIP should not be there.
            if (!config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.CCMP)
                    && (config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40)
                    || config.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104))) {
                security = SECURITY_WEP;
                securities.add(security);
            } else {
                security = SECURITY_NONE;
                securities.add(security);
            }
        }
        if (config.allowedProtocols.get(WifiConfiguration.Protocol.WPA)) {
            security = SECURITY_WPA;
            securities.add(security);
        }
        if (config.allowedProtocols.get(WifiConfiguration.Protocol.RSN)) {
            security = SECURITY_WPA2;
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            security = "psk";
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            security = SECURITY_WPA_EAP;
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            security = SECURITY_IEEE8021X;
            securities.add(security);
        }
        wifiLog("Got Security Via WifiConfiguration " + securities);
        return security;
    }

    static String getSecurity(@NonNull ScanResult result) {
        String security = SECURITY_NONE;
        if (result.capabilities.contains(SECURITY_WEP))
            security = SECURITY_WEP;
        else if (result.capabilities.contains(SECURITY_WPA2))
            security = SECURITY_WPA2;
        else if (result.capabilities.contains(SECURITY_WPA))
            security = SECURITY_WPA;
        else if (result.capabilities.contains(SECURITY_WPA_EAP))
            security = SECURITY_WPA_EAP;
        else if (result.capabilities.contains(SECURITY_IEEE8021X))
            security = SECURITY_IEEE8021X;
        wifiLog("ScanResult capabilities " + result.capabilities);
        wifiLog("Got security via ScanResult " + security);
        return security;
    }

    /**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getSecurityPrettyPlusWps(@Nullable ScanResult scanResult) {
        if (scanResult == null)
            return "";
        String result = getSecurity(scanResult);

        if (scanResult.capabilities.contains("WPS"))
            result = result + ", WPS";
        return result;
    }
}
