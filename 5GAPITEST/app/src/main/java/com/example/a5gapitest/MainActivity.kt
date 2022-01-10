package com.example.a5gapitest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // trueなら定額制
        val network = NetworkCapabilities()
        val payment = network.hasCapability(NET_CAPABILITY_NOT_METERED) || network.hasCapability(
            NET_CAPABILITY_TEMPORARILY_NOT_METERED
        )
        Log.d("5g", "定額制?: ${payment.toString()}")
        // network band
        // Connectivity Manager
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Network Capabilities of Active Network
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)
        val downSpeed = nc?.linkDownstreamBandwidthKbps.toString()
        val upSpeed = nc?.linkUpstreamBandwidthKbps.toString()

        // Toast to Display DownSpeed and UpSpeed
        Toast.makeText(applicationContext,
            "Up Speed: $upSpeed kbps \nDown Speed: $downSpeed kbps",
            Toast.LENGTH_LONG).show()

        Log.d("5g", "DOWN: $downSpeed")
        Log.d("5g", "UP: $upSpeed")

        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API30以上かどうか
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    )
                ) {
                    // 1度許可を取ったが外されている場合
                    Toast.makeText(this, "パーミッションがOFFになっています。", Toast.LENGTH_SHORT).show()
                    permissionIntent()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                    )
                }
            } else {
                // SDKバージョンが問題なく、全てのパーミッションが取れている場合
                Toast.makeText(this, "5Gの通信状況を確認します。", Toast.LENGTH_SHORT).show()
                getNetworkType()
            }

        } else {
            Toast.makeText(this, "APIレベルがたりません。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun permissionIntent() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            // requestPermissionsで設定した順番で結果が格納されている
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された(されている)ので処理を続行
                getNetworkType()
            } else {
                // パーミッションのリクエストに対して許可せずアプリに戻った場合、ここが走る
                Toast.makeText(this, "パーミッションが許可されていません。", Toast.LENGTH_SHORT).show()
                permissionIntent()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getNetworkType() {
        // 5G?
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(object : PhoneStateListener() {

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                super.onDisplayInfoChanged(telephonyDisplayInfo)

                var fiveG = when (telephonyDisplayInfo.overrideNetworkType) {
                    // LTE等(4G回線)
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> "LTE等"
                    // LTE等(キャリアアグリゲーション)
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE等(キャリアアグリゲーション)"
                    //  LTE Advanced Pro（5Ge）
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE Advanced Pro（5Ge）"
                    // 5G NR（Sub-6）ネットワーク
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G NR（Sub-6）"
                    // 5G mmWave（5G+ / 5G UW）ネットワーク
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> "5G mmWave（5G+ / 5G UW）"
                    else -> "その他"
                }
                Log.d("5g", "通信状態: $fiveG")
            }
        }, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
    }
}