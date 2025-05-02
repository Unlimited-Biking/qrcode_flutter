package com.xzp.qrcode_flutter

import android.app.Activity
import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class QRCaptureViewFactory(
    private val activity: Activity,
    private val messenger: BinaryMessenger,
    private val addPermissionListener: (PluginRegistry.RequestPermissionsResultListener) -> Unit
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        return QRCaptureView(
            activity = activity,
            messenger = messenger,
            id = id,
            addPermissionListener = addPermissionListener
        )
    }
}
