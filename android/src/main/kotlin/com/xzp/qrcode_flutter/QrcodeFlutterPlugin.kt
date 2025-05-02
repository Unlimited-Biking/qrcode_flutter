package com.xzp.qrcode_flutter

import android.graphics.BitmapFactory
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.lang.ref.WeakReference
import java.util.*

class QrcodeFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private var channel: MethodChannel? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var activityRef: WeakReference<android.app.Activity>? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "plugins/qr_capture/method")
        channel?.setMethodCallHandler(this)

        binding.platformViewRegistry.registerViewFactory(
            "plugins/qr_capture_view",
            QRCaptureViewFactory()
        )
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getQrCodeByImagePath" -> {
                val path = call.arguments as? String
                if (path == null) {
                    result.error("INVALID_ARGUMENT", "Image path null", null)
                    return
                }

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                options.inSampleSize = 1

                val bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap == null) {
                    result.success(emptyList<String>())
                    return
                }

                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val hints = Hashtable<DecodeHintType, String>()
                hints[DecodeHintType.CHARACTER_SET] = "utf-8"

                try {
                    val decodedResult = QRCodeReader().decode(binaryBitmap, hints)
                    result.success(listOf(decodedResult.text))
                } catch (e: Exception) {
                    result.success(emptyList<String>())
                }
            }

            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        activityRef = WeakReference(binding.activity)
    }

    override fun onDetachedFromActivity() {
        activityBinding = null
        activityRef = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }
}
