WifiUtils
---

**WiFi Utils** is a library that provides a set of convenience methods for managing WiFi State, WiFi Scan, And
WiFi Connection to Hotspots. If you have ever worked with `WifiManager` you should know how painful it is to make a simple wifi network scan or even worse
to connect to a hotspot programmatically. So that's what my new library is all about. To make it easier for me and hopefully for other developers as well
to do those kind of tasks from Java code. So lets jump right in some code examples.

### Enabling/Disabling WiFi
turn on device's wifi using the following:

```java
 WifiUtils.withContext(getApplicationContext()).enableWifi(this::checkResult);
```

Where `checkResult` could be a custom-defined method of your own that would deal accordingly in each situation. For Example:

```java
  private void checkResult(boolean isSuccess)
    {
        if (isSuccess)
            Toast.makeText(MainActivity.this, "WIFI ENABLED", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "COULDN'T ENABLE WIFI", Toast.LENGTH_SHORT).show();
    }
```

If you don't want to deal with call backs you can just pass `null` to `enableWifi` method like so.

```java
 WifiUtils.withContext(getApplicationContext()).enableWifi(null);
```

Similarly you can turn off the wifi using this:

```java
WifiUtils.withContext(getApplicationContext()).disableWifi();
```

### Scanning for WiFi Networks
You can easily perform a WiFi Network scan like so:

```java
WifiUtils.withContext(getApplicationContext()).scanWifi(this::getScanResults).start();

private void getScanResults(@NonNull final List<ScanResult> results)
 {
    if (results.isEmpty())
    {
        Log.i(TAG, "SCAN RESULTS IT'S EMPTY");
        return;
    }
    Log.i(TAG, "GOT SCAN RESULTS " + results);
 }
```

### Connecting to WiFi Networks
Now lets get to the interesting stuff. You can connect to any WiFi network programmatically knowing only SSID and WPA/WPA2 key: 

```java
   WifiUtils.withContext(getApplicationContext())
                        .connectWith("MitsarasWiFi", "MitsarasPassword123")
                        .onConnectionResult(this::checkResult)
                        .start();
```

Again checkResult could be something like:

```java
  private void checkResult(boolean isSuccess)
    {
        if (isSuccess)
            Toast.makeText(MainActivity.this, "CONNECTED YAY", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "COULDN'T CONNECT", Toast.LENGTH_SHORT).show();
    }
```

There are also a few other options that would allow you to do the same job: For example you can connect using SSID, BSSID and WPA/WPA2 Key:

```java
 WifiUtils.withContext(getApplicationContext())
                      .connectWith("MitsarasWiFi", "AB:CD:EF:12:34:56", "MitsarasPassword123")
                      .onConnectionResult(this::checkResult)
                      .start();
```

Lastly WifiUtils can also connect using a specified `scanResult` after a WiFi Scan is complete, for example:

```java
WifiUtils.withContext(getApplicationContext())
                     .connectWithScanResult("MitsarasPasword123", scanResults -> scanResults.get(0))
                     .onConnectionResult(this::checkResult)
                     .start();
```

The above example will perform a WiFi Scan and `connectWithScanResult` will return a `List<ScanResult> scanResults` with all the available WiFi networks
around. The method then expects you to Return a single `scanResult` out of the list of results of your choice so that it can try to connect to it. The rest is
pretty much the same.

### Canceling an ongoing connection
You have two options to cancel a connection in progress.

* If Connection takes too long to complete and just hangs in there without calling back `onConnectionResult` You can specify a **TimeOut** in milliseconds.

```java
WifiUtils.withContext(getApplicationContext())
                     .connectWith("MitsarasWiFi", "MitsarasPassword123")
                     .setTimeout(15000)
                     .onConnectionResult(this::checkResult)
                     .start();
```

The Connection will fail in 15 seconds. The default timeOut is 30 seconds.

* You can also cancel an ongoing connection immediately using the following:

```java
 WifiConnectorBuilder.WifiUtilsBuilder builder = WifiUtils.withContext(getApplicationContext());
 builder.connectWith("MitsarasWiFi", "MitsarasPassword123")
 .onConnectionResult(this::checkResult)
 .start();
 builder.cancelAutoConnect();
```

### Connecting with WPS keys.
On Androids 5.0 and greater there is also an option to connect using WPS keys. This library makes it easier and safer to connect using WPS than the stock android
API.

```java
WifiUtils.withContext(getApplicationContext())
                     .connectWithWps("d8:74:95:e6:f5:f8", "51362485")
                     .onConnectionWpsResult(this::checkResult)
                     .start();
```

### Enable Logging
If you want to receive some extra logging info comming from WiFi Utils you can enable its logging capabilities with `WifiUtils.enableLog(true);`

### Permissions
Damn You are required to set a few permissions in order for this lib to work correctly :(

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/> <!-- for Android 6 and above -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/> <!-- for Android 6 and above -->
```
### Add it to your project

[![Download](https://api.bintray.com/packages/thanosfisherman/maven/wifiutils/images/download.svg)](https://bintray.com/thanosfisherman/maven/wifiutils/_latestVersion)

Add the following to your **app module** `build.gradle` file
    
```groovy
    dependencies {
       compile 'com.thanosfisherman.wifiutils:wifiutils:<latest version here>'
    }
```
    
### Apps using this library

My app of course [GWPA Finder](https://play.google.com/store/apps/details?id=com.Fisherman.Greekwpa) Duh :P

---

### Contributing?
There are a few more things left to be covered in this tutorial. Hopefully I will improve upon this in the future.

Feel free to add/correct/fix something to this library, I will be glad to improve it with your help.

License
-------
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

    Copyright 2017 Thanos Psaridis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
