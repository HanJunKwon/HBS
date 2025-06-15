package com.hansae.hbs

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log
import com.hansae.hbs.HidManager.Companion.ACTION_USB_PERMISSION

class BarcodeScanService: Service() {
    private var hidManager: HidManager? = null

    private val permissionBroadcast = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent) {
            Log.e(">>>", "Received intent: ${intent.action}")
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            hidManager?.connectToUsbDevice(device)
                            hidManager?.setOnMessageListener(object: HidManager.MessageListener {
                                override fun onMessageReceived(message: String) {
                                    sendBroadcast(Intent().apply {
                                        action = ACTION_BARCODE_SCAN
                                        putExtra("barcode", message)
                                    })
                                }
                            })
                        }
                    } else {
                        Log.e(">>>", "permission denied for device $device")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        hidManager = HidManager(this)

        registerReceiver(permissionBroadcast, IntentFilter(ACTION_USB_PERMISSION))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(">>>", "BarcodeScanService started")

        hidManager?.let {
            it.getHidDevices().firstOrNull()?.let { device ->
                if (!it.hasPermission(device)) {
                    it.requestPermission(device)
                } else {
                    it.connectToUsbDevice(device)
                    it.setOnMessageListener(object: HidManager.MessageListener {
                        override fun onMessageReceived(message: String) {
                            sendBroadcast(Intent().apply {
                                action = ACTION_BARCODE_SCAN
                                putExtra("barcode", message)
                            })
                        }
                    })
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_BARCODE_SCAN = "com.kwon.hbs.BARCODE_SCAN"
    }
}