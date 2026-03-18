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
        // 허니웰 바코드 스캐너에서 Enter가 들어온 후에 "J" 데이터가 입력되어 다음 데이터를 읽어오는데 문제가 있음.
        if (barcodeBuffer.toString() == "J") {
            barcodeBuffer.clear()
        }

        if (event.action != KeyEvent.ACTION_DOWN) return

        val keyCode = event.keyCode

        // 엔터키 확인 (바코드의 끝)
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            val result = barcodeBuffer.toString()
            if (result.isNotEmpty()) {
                messageListener?.onMessageReceived(result)
                barcodeBuffer.clear()
            }
            return
        }

        // 숫자 및 알파벳 직접 매핑 (언어 영향 없음)
        val c = mapKeyCodeToChar(event)

        c?.let { barcodeBuffer.append(it) }
    }

    private fun mapKeyCodeToChar(event: KeyEvent): Char? {
        val keyCode = event.keyCode
        val isShift = event.isShiftPressed

        return when (keyCode) {
            // 숫자 및 숫자 키의 특수문자 (!@#$%^&*() )
            KeyEvent.KEYCODE_0 -> if (isShift) ')' else '0'
            KeyEvent.KEYCODE_1 -> if (isShift) '!' else '1'
            KeyEvent.KEYCODE_2 -> if (isShift) '@' else '2'
            KeyEvent.KEYCODE_3 -> if (isShift) '#' else '3'
            KeyEvent.KEYCODE_4 -> if (isShift) '$' else '4'
            KeyEvent.KEYCODE_5 -> if (isShift) '%' else '5'
            KeyEvent.KEYCODE_6 -> if (isShift) '^' else '6'
            KeyEvent.KEYCODE_7 -> if (isShift) '&' else '7'
            KeyEvent.KEYCODE_8 -> if (isShift) '*' else '8'
            KeyEvent.KEYCODE_9 -> if (isShift) '(' else '9'

            // 알파벳 (A-Z)
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
                val baseChar = (keyCode - KeyEvent.KEYCODE_A + 'A'.toInt()).toChar()
                if (isShift) baseChar else baseChar.lowercaseChar()
            }

            // 문장 부호 및 특수 기호
            KeyEvent.KEYCODE_MINUS -> if (isShift) '_' else '-'
            KeyEvent.KEYCODE_EQUALS -> if (isShift) '+' else '='
            KeyEvent.KEYCODE_LEFT_BRACKET -> if (isShift) '{' else '['
            KeyEvent.KEYCODE_RIGHT_BRACKET -> if (isShift) '}' else ']'
            KeyEvent.KEYCODE_BACKSLASH -> if (isShift) '|' else '\\'
            KeyEvent.KEYCODE_SEMICOLON -> if (isShift) ':' else ';'
            KeyEvent.KEYCODE_APOSTROPHE -> if (isShift) '"' else '\''
            KeyEvent.KEYCODE_GRAVE -> if (isShift) '~' else '`'
            KeyEvent.KEYCODE_COMMA -> if (isShift) '<' else ','
            KeyEvent.KEYCODE_PERIOD -> if (isShift) '>' else '.'
            KeyEvent.KEYCODE_SLASH -> if (isShift) '?' else '/'
            KeyEvent.KEYCODE_SPACE -> ' '

            else -> null // 매핑되지 않은 키는 무시
        }
    }

    fun setOnMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }

    interface MessageListener {
        fun onMessageReceived(message: String)
    }

}