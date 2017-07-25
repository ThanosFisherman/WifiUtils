package com.thanosfisherman.wifiutils.sample;

import android.Manifest;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.thanosfisherman.wifiutils.ConnectorUtils;
import com.thanosfisherman.wifiutils.WifiConnector;

import java.util.List;

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
        button.setOnClickListener(v -> WifiConnector.withContext(getApplicationContext())
                                                    .wifiScan(this::findScanResult)
                                                    .connectWithScanResult("dgfdfgdf", isSucess ->
                                                    {
                                                        if (isSucess)
                                                            Log.i("Main", "CONNECTED LELELEL");
                                                    }).start());

    }

    @Nullable
    ScanResult findScanResult(List<ScanResult> results)
    {
        Log.i("Main", results.toString());
        for (ScanResult result : results)
        {
            if ("lelelelelel".equals(result.SSID))
                return result;
        }
        return null;
    }
}
