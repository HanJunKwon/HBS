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

//        registerReceiver(barcodeScanBroadcast, IntentFilter(ACTION_BARCODE_SCAN))
    }

    var buffer = StringBuilder()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return true

        val unicodeChar = event.unicodeChar
        if (unicodeChar <= 0) return true // 유효하지 않음. 여기서 return true 해야 EditText에 안 들어감

        val char = unicodeChar.toChar()

        if (char == '\n') {
            val result = buffer.toString()
            Log.d(">>>", "barcode = $result")
            buffer.clear()

            // JSON 파싱 예시
            try {
                val json = JSONObject(result)
                Log.d(">>>", "Parsed JSON: $json")
            } catch (e: JSONException) {
                Log.e(">>>", "JSON parse error", e)
            }

        } else {
            buffer.append(char)
        }

        return true // 키 이벤트 소비
    }

    private fun hidKeyCodeToChar(modifier: Int, keyCode: Int): Char? {
        val shift = modifier.toInt() and 0x22 != 0  // Left(0x02) or Right(0x20) Shift

        return when (keyCode.toInt() and 0xFF) {
            in 0x04..0x1D -> { // a ~ z
                val base = if (shift) 'A' else 'a'
                base + (keyCode - 0x04)
            }
            in 0x1E..0x27 -> { // 1 ~ 0
                val normal = listOf('1','2','3','4','5','6','7','8','9','0')
                val shifted = listOf('!','@','#','$','%','^','&','*','(',')')
                if (shift) shifted[keyCode - 0x1E] else normal[keyCode - 0x1E]
            }
            0x28 -> '\n' // Enter
            0x2C -> ' '  // Space
            0x2D -> if (shift) '_' else '-'
            0x2E -> if (shift) '+' else '='
            0x2F -> if (shift) '{' else '['
            0x30 -> if (shift) '}' else ']'
            0x31 -> if (shift) '|' else '\\'
            0x33 -> if (shift) ':' else ';'
            0x34 -> if (shift) '"' else '\''
            0x35 -> if (shift) '~' else '`'
            0x36 -> if (shift) '<' else ','
            0x37 -> if (shift) '>' else '.'
            0x38 -> if (shift) '?' else '/'
            else -> null // 지원하지 않는 키는 null 처리
        }
    }

    override fun onResume() {
        super.onResume()
//        startService(Intent(this, BarcodeScanService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(barcodeScanBroadcast)
    }
}