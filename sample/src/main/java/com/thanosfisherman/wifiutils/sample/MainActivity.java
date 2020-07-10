package com.thanosfisherman.wifiutils.sample;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.thanosfisherman.wifiutils.Logger;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionSuccessListener;

public class MainActivity extends AppCompatActivity {
    private static final String SSID = "conn-x828678";
    private static final String PASSWORD = "146080828678";
    private ConnectionSuccessListener successListener = new ConnectionSuccessListener() {
        @Override
        public void success() {
            Toast.makeText(MainActivity.this, "SUCCESS!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void failed(@NonNull ConnectionErrorCode errorCode) {
            Toast.makeText(MainActivity.this, "EPIC FAIL!" + errorCode.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 555);
        WifiUtils.enableLog(true);
        //TODO: CHECK IF LOCATION SERVICES ARE ON

        final Button buttonConnect = findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(v -> connectWithWpa());

        final Button buttonDisconnect = findViewById(R.id.button_disconnect);
        buttonDisconnect.setOnClickListener(v -> disconnect(v.getContext()));

        WifiUtils.forwardLog((priority, tag, message) ->  {
            String customTag = tag + "_" + MainActivity.class.getSimpleName();
            Log.i(customTag, message);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void connectWithWps() {
        WifiUtils.withContext(getApplicationContext()).connectWithWps("d8:74:95:e6:f5:f8", "51362485").onConnectionWpsResult(this::checkResult).start();
    }

    private void connectWithWpa() {
        WifiUtils.withContext(getApplicationContext())
                .connectWith(SSID, PASSWORD)
                .setTimeout(40000)
                .onConnectionResult(successListener)
                .start();
    }

    private void disconnect(final Context context) {
        WifiUtils.withContext(context)
                .disconnect(new DisconnectionSuccessListener() {
                    @Override
                    public void success() {
                        Toast.makeText(MainActivity.this, "Disconnect success!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failed(@NonNull DisconnectionErrorCode errorCode) {
                        Toast.makeText(MainActivity.this, "Failed to disconnect: " + errorCode.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void enableWifi() {
        WifiUtils.withContext(getApplicationContext()).enableWifi(this::checkResult);
        //or without the callback
        //WifiUtils.withContext(getApplicationContext()).enableWifi();
    }

    private void checkResult(boolean isSuccess) {
        if (isSuccess)
            Toast.makeText(MainActivity.this, "SUCCESS!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "EPIC FAIL!", Toast.LENGTH_SHORT).show();
    }
}
