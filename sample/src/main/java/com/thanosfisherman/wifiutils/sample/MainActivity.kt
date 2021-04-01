package com.thanosfisherman.wifiutils.sample

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.thanosfisherman.wifiutils.TypeEnum
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionErrorCode
import com.thanosfisherman.wifiutils.wifiDisconnect.DisconnectionSuccessListener
import com.thanosfisherman.wifiutils.wifiRemove.RemoveErrorCode
import com.thanosfisherman.wifiutils.wifiRemove.RemoveSuccessListener
import kotlinx.android.synthetic.main.activity_main.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 555)
        WifiUtils.forwardLog { _, tag, message ->
            val customTag = "${tag}.${this::class.simpleName}"
            Log.i(customTag, message)
        }
        WifiUtils.enableLog(true)
        button_connect.setOnClickListener { connectWithWpa(applicationContext) }
        button_connect_hidden.setOnClickListener { connectHidden(applicationContext) }
        button_disconnect.setOnClickListener { disconnect(applicationContext) }
        button_remove.setOnClickListener { remove(applicationContext) }
        button_check.setOnClickListener { check(applicationContext) }
        button_check_internet_connection.setOnClickListener { checkInternetConnection(applicationContext) }
    }

    private fun connectWithWpa(context: Context) {
        WifiUtils.withContext(context)
                .connectWith(textview_ssid.text.toString(), textview_password.text.toString())
                .setTimeout(15000)
                .onConnectionResult(object : ConnectionSuccessListener {
                    override fun success() {
                        Toast.makeText(context, "SUCCESS!", Toast.LENGTH_SHORT).show()
                    }

                    override fun failed(errorCode: ConnectionErrorCode) {
                        Toast.makeText(context, "EPIC FAIL!$errorCode", Toast.LENGTH_SHORT).show()
                    }
                })
                .start()
    }

    private fun connectHidden(context: Context) {
        WifiUtils.withContext(context)
                .connectWith(textview_ssid.text.toString(), textview_password.text.toString(),TypeEnum.EAP)
                .onConnectionResult(object : ConnectionSuccessListener {
                    override fun success() {
                        Toast.makeText(context, "SUCCESS!", Toast.LENGTH_SHORT).show()
                    }

                    override fun failed(errorCode: ConnectionErrorCode) {
                        Toast.makeText(context, "EPIC FAIL!$errorCode", Toast.LENGTH_SHORT).show()
                    }
                })
                .start()
    }

    private fun disconnect(context: Context) {
        WifiUtils.withContext(context)
                .disconnect(object : DisconnectionSuccessListener {
                    override fun success() {
                        Toast.makeText(context, "Disconnect success!", Toast.LENGTH_SHORT).show()
                    }

                    override fun failed(errorCode: DisconnectionErrorCode) {
                        Toast.makeText(context, "Failed to disconnect: $errorCode", Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun remove(context: Context) {
        WifiUtils.withContext(context)
                .remove(textview_ssid.text.toString(), object : RemoveSuccessListener {
                    override fun success() {
                        Toast.makeText(context, "Remove success!", Toast.LENGTH_SHORT).show()
                    }

                    override fun failed(errorCode: RemoveErrorCode) {
                        Toast.makeText(context, "Failed to disconnect and remove: $errorCode", Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun check(context: Context) {
        val result = WifiUtils.withContext(context).isWifiConnected(textview_ssid.text.toString())
        Toast.makeText(context, "Wifi Connect State: $result", Toast.LENGTH_SHORT).show()
    }


    private fun checkInternetConnection(context: Context) {
        val executorService: ExecutorService = Executors.newFixedThreadPool(4)
        executorService.execute {
            try {
                val timeoutMs = 1500
                val sock = Socket()
                val sockaddr: SocketAddress = InetSocketAddress("8.8.8.8", 53)
                sock.connect(sockaddr, timeoutMs)
                sock.close()
                runOnUiThread {
                    Toast.makeText(context, "Connecting to internet successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(context, "Cant connect to internet", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}