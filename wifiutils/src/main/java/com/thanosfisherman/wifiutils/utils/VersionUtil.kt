package com.thanosfisherman.wifiutils.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

object VersionUtil{

    @SuppressLint("AnnotateVersionCheck")
    fun is29AndAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q


    @RequiresApi(Build.VERSION_CODES.Q)
    fun getPanelIntent()  =  Intent(Settings.Panel.ACTION_WIFI)
}