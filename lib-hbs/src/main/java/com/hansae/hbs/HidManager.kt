package com.hansae.hbs

import android.app.PendingIntent
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class HidManager(private val context: Context) {
    private var usbManager: UsbManager = context.getSystemService(USB_SERVICE) as UsbManager
    private var messageListener: MessageListener? = null
    private var hidDeviceConnectionListener: HidDeviceConnectionListener? = null

    private var isMuteStartTime = 0L

    fun getHidDevices(): List<UsbDevice> {
        for (device in usbManager.deviceList.values) {
            Log.d(">>>", "getHidDevices() :: " +
                    "Device: ${device.deviceName}, " +
                    "Class: ${device.deviceClass}, " +
                    "Interfaces: ${device.interfaceCount}, " +
                    "Manufacture: ${device.manufacturerName}, " +
                    "Product: ${device.productName}")
        }

        return usbManager.deviceList.values.filter { device ->
            (device.deviceClass == UsbConstants.USB_CLASS_PER_INTERFACE && device.manufacturerName?.contains("Honeywell", ignoreCase = true) == true) ||
            (device.deviceClass == UsbConstants.USB_CLASS_HID) ||
                    (device.deviceClass == UsbConstants.USB_CLASS_PER_INTERFACE && (0 until device.interfaceCount).any {
                        device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_HID
                    })
        }
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun requestPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    fun connectToUsbDevice(device: UsbDevice) {
        if (device.interfaceCount == 0) {
            Log.e(">>>", "No interfaces found on device ${device.deviceName}")
            return
        }

        val usbInterface = device.getInterface(0)
        val endpoint = (0 until usbInterface.endpointCount)
            .map { usbInterface.getEndpoint(it) }
            .firstOrNull { it.direction == UsbConstants.USB_DIR_IN }

        if (endpoint == null) {
            Log.e(">>>", "No IN endpoint found")
            return
        }

        val connection = usbManager.openDevice(device)
        if (connection == null || !connection.claimInterface(usbInterface, true)) {
            Log.e(">>>", "Cannot open connection or claim interface")
            return
        }

        Log.d(">>>", "Connected to HID device: ${device.deviceName}")

        val buffer = ByteArray(endpoint.maxPacketSize)
        val stringBuilder = StringBuilder()

        Thread {
            while (true) {
                val receivedBytes = connection.bulkTransfer(endpoint, buffer, buffer.size, 1000)

                if (System.currentTimeMillis() - isMuteStartTime < DATE_RECEIVE_DELAY) {
                    continue // Mute 상태가 유지되는 동안 데이터 수신을 건너뜀
                }

                if (receivedBytes > 0) {
                    val modifier = buffer[0]
                    val keyCode = buffer[2]

                    Log.d(">>>", "Received data: modifier=$modifier, keyCode=$keyCode")

                    val char = hidKeyCodeToChar(modifier, keyCode)
                    if (char != null) {
                        if (char == '\n') {
                            isMuteStartTime = System.currentTimeMillis()
                            messageListener?.onMessageReceived(stringBuilder.toString())
                            stringBuilder.clear()
                        } else {
                            stringBuilder.append(char)
                        }
                    }
                }
            }
        }.start()
    }

    fun setOnMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }

    fun setOnHidDeviceConnectionListener(listener: HidDeviceConnectionListener) {
        this.hidDeviceConnectionListener = listener
    }

    private fun hidKeyCodeToChar(modifier: Byte, keyCode: Byte): Char? {
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

    interface MessageListener {
        fun onMessageReceived(message: String)
    }

    interface HidDeviceConnectionListener {
        fun onDeviceConnected(device: UsbDevice)
        fun onDeviceDisconnected(device: UsbDevice)
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.kwon.hbs.USB_PERMISSION"

        private const val DATE_RECEIVE_DELAY = 100 // ms
    }
}