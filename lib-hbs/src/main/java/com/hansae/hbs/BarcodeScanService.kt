package com.hansae.hbs

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log
import com.hansae.hbs.BarcodeScanManager
import com.hansae.hbs.HidManager.Companion.ACTION_USB_PERMISSION

class BarcodeScanService: Service() {
    private var hidManager: HidManager? = null
    private var barcodeSCanManager = BarcodeScanManager.getInstance()

    private val permissionBroadcast = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent) {
            Log.d(">>>", "Received intent: ${intent.action}")
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    Log.d(">>>", "granted permission: ${intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)}")
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            hidManager?.connectToUsbDevice(device)
                            hidManager?.setOnMessageListener(object: HidManager.MessageListener {
                                override fun onMessageReceived(message: String) {
                                    sendBroadcast(Intent().apply {
                                        action = ACTION_BARCODE_SCAN
                                        putExtra(ACTION_KEY_BARCODE, message)
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

        barcodeSCanManager.setOnMessageListener(object: HidManager.MessageListener {
            override fun onMessageReceived(message: String) {
                Log.d(">>>", "BarcodeScanService received message: $message")
                sendBroadcast(Intent().apply {
                    action = ACTION_BARCODE_SCAN
                    putExtra(ACTION_KEY_BARCODE, message)
                })
            }
        })

        registerReceiver(permissionBroadcast, IntentFilter(ACTION_USB_PERMISSION), RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(">>>", "BarcodeScanService onStartCommand")

        Log.d(">>>", "HID Manager is null? ${hidManager == null}")


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(permissionBroadcast)
        super.onDestroy()
    }

    companion object {
        const val ACTION_BARCODE_SCAN = "com.kwon.hbs.BARCODE_SCAN"

        const val ACTION_KEY_BARCODE = "barcode"
    }
}