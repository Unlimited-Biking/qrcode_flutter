package com.xzp.qrcode_flutter

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener

class QRCaptureView(
    private val activity: Activity,
    private val messenger: BinaryMessenger,
    private val id: Int,
    private val addPermissionListener: (RequestPermissionsResultListener) -> Unit
) : PlatformView, MethodCallHandler {

    companion object {
        const val CAMERA_REQUEST_ID = 513469796
    }

    private val barcodeView: BarcodeView = BarcodeView(activity)
    private var cameraPermissionContinuation: Runnable? = null
    private val channel = MethodChannel(messenger, "plugins/qr_capture/method_$id")

    private var requestingPermission = false

    init {
        addPermissionListener(CameraRequestPermissionsListener())
        channel.setMethodCallHandler(this)

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                channel.invokeMethod("onCaptured", result.text)
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })

        barcodeView.resume()

        activity.application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(p0: Activity) {
                    if (p0 == activity) barcodeView.pause()
                }

                override fun onActivityResumed(p0: Activity) {
                    if (p0 == activity) barcodeView.resume()
                }

                override fun onActivityStarted(p0: Activity) {}
                override fun onActivityDestroyed(p0: Activity) {}
                override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
                override fun onActivityStopped(p0: Activity) {}
                override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            }
        )

        checkAndRequestPermission(null)
    }

    override fun getView(): View = barcodeView

    override fun dispose() {
        barcodeView.pause()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "checkAndRequestPermission" -> checkAndRequestPermission(result)
            "resume" -> barcodeView.resume()
            "pause" -> barcodeView.pause()
            "setTorchMode" -> {
                val isOn = call.arguments as Boolean
                barcodeView.setTorch(isOn)
            }
            else -> result.notImplemented()
        }
    }

    private fun checkAndRequestPermission(result: MethodChannel.Result?) {
        if (cameraPermissionContinuation != null) {
            result?.error("cameraPermission", "Camera permission request ongoing", null)
            return
        }

        cameraPermissionContinuation = Runnable {
            cameraPermissionContinuation = null
            if (!hasCameraPermission()) {
                result?.error("cameraPermission", "Camera permission not granted", null)
                return@Runnable
            }
            result?.success(true)
        }

        if (hasCameraPermission()) {
            cameraPermissionContinuation?.run()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestingPermission = true
                activity.requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_ID
                )
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                activity.checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED
    }

    private inner class CameraRequestPermissionsListener : RequestPermissionsResultListener {
        override fun onRequestPermissionsResult(id: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
            if (id == CAMERA_REQUEST_ID && grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                cameraPermissionContinuation?.run()
                return true
            }
            return false
        }
    }
}
