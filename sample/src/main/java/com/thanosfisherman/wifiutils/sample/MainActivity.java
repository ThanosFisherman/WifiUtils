package com.thanosfisherman.wifiutils.sample;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 555);
        final Button button = findViewById(R.id.button);
        WifiUtils.enableLog(true);
        //TODO: CHECK IF LOCATION SERVICES ARE ON
        button.setOnClickListener(v -> connectWithWpa());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void connectWithWps() {
        WifiUtils.withContext(getApplicationContext()).connectWithWps("d8:74:95:e6:f5:f8", "51362485").onConnectionWpsResult(this::checkResult).start();
    }

    private void connectWithWpa() {
        String ote = "conn-x828678";
        String otePass = "146080828678";

        WifiUtils.withContext(getApplicationContext())
                .connectWith(ote, otePass)
                .setTimeout(40000)
                .onConnectionResult(new ConnectionSuccessListener() {
                    @Override
                    public void success() {
                        Toast.makeText(MainActivity.this, "SUCCESS!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failed(@NonNull ConnectionErrorCode errorCode) {
                        Toast.makeText(MainActivity.this, "EPIC FAIL!" + errorCode.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
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
