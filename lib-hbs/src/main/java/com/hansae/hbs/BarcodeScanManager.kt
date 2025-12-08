package com.hansae.hbs

import android.view.KeyEvent

object BarcodeScanManager {
    private var barcodeBuffer = StringBuilder()
    private var messageListener: MessageListener? = null

    private var instance: BarcodeScanManager? = null

    fun getInstance(): BarcodeScanManager {
        return instance ?: synchronized(this) {
            instance ?: BarcodeScanManager.apply {
                instance = this
            }
        }
    }

    fun eventKeyToBarcode(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_UP) return

        val unicodeChar = event.unicodeChar
        if (unicodeChar <= 0) return // 유효하지 않음. 여기서 return true 해야 EditText에 안 들어감

        val char = unicodeChar.toChar()

        if (char == '\n' || char == '\r' || event.action == KeyEvent.KEYCODE_ENTER) {
            messageListener?.onMessageReceived(barcodeBuffer.toString())
            barcodeBuffer.clear()
        } else {
            barcodeBuffer.append(char)
        }

        return // 키 이벤트 소비
    }

    fun setOnMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }

    interface MessageListener {
        fun onMessageReceived(message: String)
    }

}