package com.thanosfisherman.wifiutils.sample;

import android.Manifest;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.thanosfisherman.wifiutils.ConnectorUtils;
import com.thanosfisherman.wifiutils.WifiConnector;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 555);
        final Button button = (Button) findViewById(R.id.button);
        ConnectorUtils.enableLog(true);
        button.setOnClickListener(v ->
                                  {
                                      WifiConnector.withContext(getApplicationContext())
                                                   .connectWith("lelelelelel", "asfsafasd")
                                                   .onConnectionResult(this::checkResult)
                                                   .start();
                                  });
    }

    private void checkResult(boolean isSuccess)
    {
        if (isSuccess)
            Toast.makeText(MainActivity.this, "CONNECTED YAY", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "COULDN'T CONNECT", Toast.LENGTH_SHORT).show();
    }
}
