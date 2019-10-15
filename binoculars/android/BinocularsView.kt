package com.luminagic.rinnegan.binoculars

import android.content.Context
import android.graphics.Color
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView

class BinocularsView(context: Context, left: Int, top: Int, width: Int, height: Int) : FrameLayout(context) {
    private lateinit var _webView: WebView
    val webView: WebView get() = _webView

    private lateinit var _surfaceView: SurfaceView
    val surfaceView: SurfaceView get() = _surfaceView

    init {
        linearLayout {
            orientation = LinearLayout.HORIZONTAL
            _webView = webView().lparams(MATCH_PARENT, MATCH_PARENT, 1.0f).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                webViewClient = WebViewClient()
            }
            linearLayout().lparams(5, MATCH_PARENT, 0.0f).apply {
                backgroundColor = Color.BLACK
            }
            linearLayout().lparams(MATCH_PARENT, MATCH_PARENT, 1.0f).apply {
                linearLayout().lparams(width, height, 1.0f).apply {
                    leftPadding = -left
                    topPadding = -top
                    _surfaceView = surfaceView().lparams(MATCH_PARENT, MATCH_PARENT, 1.0f).apply {
                    }
                }
            }
        }
    }

    fun close() {
        _webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        _webView.clearHistory()
        _webView.destroy()
    }
}

inline fun ViewManager.binocularsView(left: Int, top: Int, width: Int, height: Int, init: BinocularsView.() -> Unit = {}): BinocularsView {
    return ankoView({ BinocularsView(it, left, top, width, height) }, theme = 0, init = init)
}
