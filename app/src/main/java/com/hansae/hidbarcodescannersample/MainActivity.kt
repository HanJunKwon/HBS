package com.hansae.hidbarcodescannersample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hansae.hbs.HidManager

class MainActivity : AppCompatActivity() {
    private var hidManager: HidManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hidManager = HidManager(this)
        hidManager?.let {
            it.getHidDevices().firstOrNull()?.let { device ->
                Log.e(">>>", "has permission: ${it.hasPermission(device)}")
                if (!it.hasPermission(device)) {
                    it.requestPermission(device)
                } else {
                    it.connectToUsbDevice(device)
                    it.setOnMessageListener(object: HidManager.MessageListener {
                        override fun onMessageReceived(message: String) {
                            Toast.makeText(this@MainActivity, "Received message: $message", Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        }
    }
}