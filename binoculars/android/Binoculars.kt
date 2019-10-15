package com.luminagic.rinnegan.binoculars

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.Surface
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.FrameLayout
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.PluginResult
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.windowManager
import org.json.JSONArray

class Binoculars: CordovaPlugin() {
    private var binocularsView: BinocularsView? = null
    private var mCallbackContext: CallbackContext? = null

    private val REQUEST_MEDIA_PROJECTION = 1
    private var mResultCode: Int = 0
    private var mResultData: Intent? = null
    private var mSurface: Surface? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var density: Float = 1.0f
    private var screenW: Int = 0
    private var screenH: Int = 0

    override fun pluginInitialize() {
        super.pluginInitialize()

        val defaultDisplay = this.cordova.context.windowManager.defaultDisplay
        var dm: DisplayMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(dm)
        density = dm.density
        var screenSize: Point = Point()
        defaultDisplay.getRealSize(screenSize)
        screenW = screenSize.x.toInt()
        screenH = screenSize.y.toInt()
        mMediaProjectionManager = this.cordova.context.mediaProjectionManager
    }

    override fun execute(
            action: String?, args: JSONArray?, callbackContext: CallbackContext?): Boolean {
        mCallbackContext = callbackContext
        when(action) {
            "show" -> {
                show(args!!.getDouble(0) * density, args.getDouble(1) * density, args.getDouble(2) * density, args.getDouble(3) * density, args.getString(4))
                return true
            }
            "loadUrl" -> { // 未指定显示取悦，默认全屏显示
                show(0.0, 0.0, screenW.toDouble(), screenH.toDouble(), args!!.getString(0))
                return true
            }
            "close" -> {
                close()
                return true
            }
            "eval" -> {
                eval(args!!.getString(0), args!!.getString(1))
                return true
            }
        }

        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        tearDownMediaProjection()
    }

    fun show(x: Double, y: Double, width: Double, height: Double, url: String) {
        cordova.activity.runOnUiThread {
            var layout = FrameLayout.LayoutParams(width.toInt(), height.toInt()).apply {
                leftMargin = x.toInt()
                topMargin = y.toInt()
            }
            if (binocularsView == null) {
                val parent = webView.view.parent as ViewGroup
                binocularsView = parent.binocularsView(x.toInt(), y.toInt(), screenW, screenH, {
                    layoutParams = layout
                })

                mSurface = binocularsView!!.surfaceView!!.holder.surface
                binocularsView!!.webView.loadUrl(url)
                startScreenCapture()
            } else {
                binocularsView!!.layout(x.toInt(), y.toInt(), screenW, screenH)
                binocularsView!!.layoutParams = layout
                binocularsView!!.webView.loadUrl(url)
            }

            mCallbackContext!!.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
        }
    }

    fun close() {
        if (binocularsView == null) {
            return
        }
        cordova.activity.runOnUiThread {
            binocularsView!!.close()
            binocularsView = null
            val parent = webView.view.parent as ViewGroup
            parent.removeViewAt( 1) // 0 is SystemWebView
            stopScreenCapture()
            mCallbackContext!!.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
        }
    }

    fun eval(jsmethname: String, jsonparams: String) {
        cordova.activity.runOnUiThread {
            if (binocularsView != null ) {
                val js = "javascript:" + jsmethname + "(\'" + jsonparams + "\')"
                binocularsView!!.webView.evaluateJavascript(js, object : ValueCallback<String> {
                    override fun onReceiveValue(value: String?) {
                        // 返回结果
                    }
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            mResultCode = resultCode
            mResultData = intent
            setUpMediaProjection()
            setUpVirtualDisplay()
        }
    }

    private fun setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)
    }

    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    private fun startScreenCapture() {
        if (mSurface == null || this.cordova.activity == null) {
            return
        }
        this.cordova.startActivityForResult(this, mMediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    private fun setUpVirtualDisplay() {
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay("ScreenCapture",
                screenW, screenH, density.toInt(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null)
    }

    private fun stopScreenCapture() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay!!.release()
            mVirtualDisplay = null

        }
        tearDownMediaProjection()
    }
}