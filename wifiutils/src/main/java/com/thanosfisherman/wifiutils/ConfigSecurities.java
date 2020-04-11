package com.thanosfisherman.wifiutils;

import android.annotation.TargetApi;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.thanosfisherman.elvis.Objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static com.thanosfisherman.wifiutils.WifiUtils.wifiLog;
import static com.thanosfisherman.wifiutils.utils.SSIDUtils.convertToQuotedString;

final class ConfigSecurities {
    static final String SECURITY_NONE = "OPEN";
    static final String SECURITY_WEP = "WEP";
    static final String SECURITY_PSK = "PSK";
    static final String SECURITY_EAP = "EAP";


    /**
     * Fill in the security fields of WifiConfiguration config.
     *
     * @param config   The object to fill.
     * @param security If is OPEN, password is ignored.
     * @param password Password of the network if security is not OPEN.
     */

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
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
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if (ConnectorUtils.isHexWepKey(password)) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = convertToQuotedString(password);
                }
                break;
            case SECURITY_PSK:
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                if (password.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = password;
                } else {
                    config.preSharedKey = convertToQuotedString(password);
                }
                break;
            case SECURITY_EAP:
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
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
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    static void setupWifiNetworkSpecifierSecurities(@NonNull WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder, String security, @NonNull final String password) {
        wifiLog("Setting up WifiNetworkSpecifier.Builder " + security);
        switch (security) {
            case SECURITY_NONE:
                // nothing to do
                break;
            case SECURITY_WEP:
                // no longer possible
                break;
            case SECURITY_PSK:
            case SECURITY_EAP:
                wifiNetworkSpecifierBuilder.setWpa2Passphrase(password);
                break;

            default:
                wifiLog("Invalid security type: " + security);
                break;
        }
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final WifiConfiguration configToFind) {
        final String ssid = configToFind.SSID;
        if (ssid == null || ssid.isEmpty()) {
            return null;
        }

        final String bssid = configToFind.BSSID != null ? configToFind.BSSID : "";

        final String security = getSecurity(configToFind);


        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            wifiLog("NULL configs");
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (Objects.equals(security, configSecurity)) {
                    return config;
                }
            }
        }
        wifiLog("Couldn't find " + ssid);
        return null;
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiManager, @NonNull final String ssid) {
        final List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        final String findSSID = ('"' + ssid + '"');

        for (final WifiConfiguration wifiConfiguration : configuredNetworks) {
            if (wifiConfiguration.SSID.equals(findSSID)) {
                return wifiConfiguration;
            }
        }

        return null;
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiManager, @NonNull final ScanResult scanResult) {
        if (scanResult.BSSID == null || scanResult.SSID == null || scanResult.SSID.isEmpty() || scanResult.BSSID.isEmpty()) {
            return null;
        }
        final String ssid = convertToQuotedString(scanResult.SSID);
        final String bssid = scanResult.BSSID;
        final String security = getSecurity(scanResult);

        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null) {
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (Objects.equals(security, configSecurity)) {
                    return config;
                }
            }
        }
        return null;
    }

    static String getSecurity(@NonNull WifiConfiguration config) {
        String security = SECURITY_NONE;
        final Collection<String> securities = new ArrayList<>();
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            // If we never set group ciphers, wpa_supplicant puts all of them.
            // For open, we don't set group ciphers.
            // For WEP, we specifically only set WEP40 and WEP104, so CCMP
            // and TKIP should not be there.
            if (config.wepKeys[0] != null) {
                security = SECURITY_WEP;
            } else {
                security = SECURITY_NONE;
            }
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            security = SECURITY_EAP;
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            security = SECURITY_PSK;
            securities.add(security);
        }

        wifiLog("Got Security Via WifiConfiguration " + securities);
        return security;
    }

    static String getSecurity(@NonNull ScanResult result) {
        String security = SECURITY_NONE;
        if (result.capabilities.contains(SECURITY_WEP)) {
            security = SECURITY_WEP;
        }
        if (result.capabilities.contains(SECURITY_PSK)) {
            security = SECURITY_PSK;
        }
        if (result.capabilities.contains(SECURITY_EAP)) {
            security = SECURITY_EAP;
        }

        wifiLog("ScanResult capabilities " + result.capabilities);
        wifiLog("Got security via ScanResult " + security);
        return security;
    }

    /**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getSecurityPrettyPlusWps(@Nullable ScanResult scanResult) {
        if (scanResult == null) {
            return "";
        }
        String result = getSecurity(scanResult);

        if (scanResult.capabilities.contains("WPS")) {
            result = result + ", WPS";
        }
        return result;
    }
}
