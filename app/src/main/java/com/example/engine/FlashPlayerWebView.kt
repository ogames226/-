package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.AspectRatioMode
import com.example.model.KeyBinding
import com.example.model.RenderQuality
import com.example.model.TouchMouseMode

@SuppressLint("SetJavaScriptEnabled")
class FlashPlayerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val TAG = "FlashPlayerWebView"

    private var isHtmlLoaded = false
    private var pendingBase64: String? = null
    private var pendingFilename: String? = null
    private var touchMouseMode: TouchMouseMode = TouchMouseMode.DIRECT_TOUCH

    var onPlayerLoadedCallback: ((title: String, width: Int, height: Int, fps: Double, frames: Int) -> Unit)? = null
    var onPlayerErrorCallback: ((String) -> Unit)? = null
    var onFpsUpdateCallback: ((Int) -> Unit)? = null

    init {
        setupWebView()
    }

    private fun setupWebView() {
        // Hardware Acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(0xFF080B11.toInt())

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Bridge setup
        val bridge = FlashJsBridge(
            context = context,
            onLoaded = { title, w, h, fps, frames ->
                post { onPlayerLoadedCallback?.invoke(title, w, h, fps, frames) }
            },
            onError = { msg ->
                post { onPlayerErrorCallback?.invoke(msg) }
            },
            onFps = { fps ->
                post { onFpsUpdateCallback?.invoke(fps) }
            }
        )
        addJavascriptInterface(bridge, "AndroidBridge")

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("FlashRuffleJS", "[${it.messageLevel()}] ${it.message()} -- line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isHtmlLoaded = true
                Log.d(TAG, "Ruffle Host Page loaded successfully")
                pendingBase64?.let { b64 ->
                    val filename = pendingFilename ?: "game.swf"
                    pendingBase64 = null
                    pendingFilename = null
                    loadSwfBase64(b64, filename)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.w(TAG, "WebView resource error: ${error?.description}")
            }
        }

        // Load host page
        loadPlayerContainer()
    }

    fun loadPlayerContainer(aspectRatio: AspectRatioMode = AspectRatioMode.CONTAIN) {
        val html = RuffleHtmlTemplate.buildPlayerHtml(aspectRatio.cssClass)
        loadDataWithBaseURL("https://ruffle.ai-studio.local/", html, "text/html", "UTF-8", null)
    }

    fun loadSwfBase64(base64Data: String, filename: String) {
        if (!isHtmlLoaded) {
            pendingBase64 = base64Data
            pendingFilename = filename
            return
        }

        post {
            // Escape filename string for JS
            val safeName = filename.replace("'", "\\'")
            evaluateJavascript("if (window.loadSwfFromBase64) { window.loadSwfFromBase64('$base64Data', '$safeName'); }", null)
        }
    }

    fun sendKeyDown(binding: KeyBinding) {
        sendKeyEvent(binding.jsKey, binding.jsCode, binding.jsKeyCode, "keydown")
    }

    fun sendKeyUp(binding: KeyBinding) {
        sendKeyEvent(binding.jsKey, binding.jsCode, binding.jsKeyCode, "keyup")
    }

    fun sendDirectionKeyDown(direction: String, keyCode: Int) {
        val (key, code) = when (direction.uppercase()) {
            "UP" -> "ArrowUp" to "ArrowUp"
            "DOWN" -> "ArrowDown" to "ArrowDown"
            "LEFT" -> "ArrowLeft" to "ArrowLeft"
            "RIGHT" -> "ArrowRight" to "ArrowRight"
            else -> direction to direction
        }
        sendKeyEvent(key, code, keyCode, "keydown")
    }

    fun sendDirectionKeyUp(direction: String, keyCode: Int) {
        val (key, code) = when (direction.uppercase()) {
            "UP" -> "ArrowUp" to "ArrowUp"
            "DOWN" -> "ArrowDown" to "ArrowDown"
            "LEFT" -> "ArrowLeft" to "ArrowLeft"
            "RIGHT" -> "ArrowRight" to "ArrowRight"
            else -> direction to direction
        }
        sendKeyEvent(key, code, keyCode, "keyup")
    }

    fun sendKeyEvent(key: String, code: String, keyCode: Int, eventType: String) {
        post {
            val js = "window.sendKeyEvent('$key', '$code', $keyCode, '$eventType');"
            evaluateJavascript(js, null)
        }
    }

    fun sendMouseEvent(type: String, x: Float, y: Float, button: Int = 0) {
        post {
            val js = "window.sendMouseEvent('$type', ${x.toInt()}, ${y.toInt()}, $button);"
            evaluateJavascript(js, null)
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        post {
            evaluateJavascript("window.setAspectRatio('${mode.cssClass}');", null)
        }
    }

    fun setRenderQuality(quality: RenderQuality) {
        post {
            evaluateJavascript("window.setQuality('${quality.value}');", null)
        }
    }

    fun setVolume(volume: Float) {
        post {
            evaluateJavascript("window.setVolume($volume);", null)
        }
    }

    fun setTouchMouseMode(mode: TouchMouseMode) {
        this.touchMouseMode = mode
        post {
            evaluateJavascript("window.setVirtualCursorVisible(${mode == TouchMouseMode.VIRTUAL_TRACKPAD});", null)
        }
    }

    fun togglePlayPause() {
        post {
            evaluateJavascript("window.togglePlayPause();", null)
        }
    }

    fun restartGame() {
        post {
            evaluateJavascript("window.restartSwf();", null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        // Handle direct touch to mouse tracking simulation if enabled
        if (touchMouseMode == TouchMouseMode.DIRECT_TOUCH) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sendMouseEvent("mousedown", event.x, event.y, 0)
                }
                MotionEvent.ACTION_MOVE -> {
                    sendMouseEvent("mousemove", event.x, event.y, 0)
                }
                MotionEvent.ACTION_UP -> {
                    sendMouseEvent("mouseup", event.x, event.y, 0)
                    sendMouseEvent("click", event.x, event.y, 0)
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
