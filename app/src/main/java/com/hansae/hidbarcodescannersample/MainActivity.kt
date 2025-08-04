package com.hansae.hidbarcodescannersample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hansae.hbs.BarcodeScanManager
import com.hansae.hbs.BarcodeScanService
import com.hansae.hbs.BarcodeScanService.Companion.ACTION_BARCODE_SCAN
import com.hansae.hbs.HidManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val barcodeScanBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e(">>>", "Received intent: ${intent?.action}")
            when (intent?.action) {
                ACTION_BARCODE_SCAN -> {
                    val barcode = intent.getStringExtra("barcode")
                    if (barcode != null) {
                        // Handle the scanned barcode
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@MainActivity, barcode, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e(">>>", "No barcode received")
                    }
                }
                else -> {
                    Log.e(">>>", "Unknown action received: ${intent?.action}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        registerReceiver(barcodeScanBroadcast, IntentFilter(ACTION_BARCODE_SCAN))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        BarcodeScanManager.getInstance().eventKeyToBarcode(event)
        return true // 키 이벤트 소비
    }


    override fun onResume() {
        super.onResume()
        startService(Intent(this, BarcodeScanService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(barcodeScanBroadcast)
    }
}