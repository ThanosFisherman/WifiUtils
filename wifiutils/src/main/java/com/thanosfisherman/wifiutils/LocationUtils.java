package com.thanosfisherman.wifiutils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import androidx.annotation.NonNull;
import android.util.Log;

import static com.thanosfisherman.wifiutils.utils.Elvis.of;

public class LocationUtils {
    private static final String TAG = LocationUtils.class.getSimpleName();

    public static final int GOOD_TO_GO = 1000;
    public static final int NO_LOCATION_AVAILABLE = 1111;
    public static final int LOCATION_DISABLED = 1112;

    public static int checkLocationAvailability(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PackageManager packMan = context.getPackageManager();
            if (packMan.hasSystemFeature(PackageManager.FEATURE_LOCATION)) {
                if (!isLocationEnabled(context)) {
                    Log.d(TAG, "Location DISABLED");
                    return LOCATION_DISABLED;
                }
            } else {
                Log.d(TAG, "NO GPS SENSOR");
                return NO_LOCATION_AVAILABLE;
            }
        }
        Log.d(TAG, "GPS GOOD TO GO");
        return GOOD_TO_GO;
    }

    private static boolean isLocationEnabled(@NonNull Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return of(manager).next(locationManager -> locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).getBoolean() ||
                of(manager).next(locationManager -> locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).getBoolean();
    }
}
