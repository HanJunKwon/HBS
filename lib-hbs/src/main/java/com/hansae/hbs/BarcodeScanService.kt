package com.hansae.hbs

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BarcodeScanService: Service() {
    private var barcodeScanManager = BarcodeScanManager.getInstance()

    override fun onCreate() {
        super.onCreate()

        barcodeScanManager.setOnMessageListener(object: BarcodeScanManager.MessageListener {
            override fun onMessageReceived(message: String) {
                sendBroadcast(Intent().apply {
                    action = ACTION_BARCODE_SCAN
                    putExtra(ACTION_KEY_BARCODE, message)
                })
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_BARCODE_SCAN = "com.kwon.hbs.BARCODE_SCAN"

        const val ACTION_KEY_BARCODE = "barcode"
    }
}