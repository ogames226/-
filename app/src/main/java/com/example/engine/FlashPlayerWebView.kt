package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Base64
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
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
class FlashPlayerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val TAG = "FlashPlayerWebView"

    private var isHtmlLoaded = false
    private var currentSwfBytes: ByteArray? = null
    private var currentSwfBase64: String? = null
    private var currentFilename: String? = null

    private var pendingBytes: ByteArray? = null
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
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
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
            },
            base64Supplier = {
                currentSwfBase64 ?: currentSwfBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
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
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (url.contains("current_game.swf") || (currentFilename != null && url.endsWith(currentFilename!!))) {
                    val bytes = currentSwfBytes
                    if (bytes != null && bytes.isNotEmpty()) {
                        Log.d(TAG, "Serving SWF binary via local intercept (${bytes.size} bytes)")
                        return WebResourceResponse(
                            "application/x-shockwave-flash",
                            "binary",
                            ByteArrayInputStream(bytes)
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isHtmlLoaded = true
                Log.d(TAG, "Ruffle Host Page loaded successfully")
                pendingBytes?.let { bytes ->
                    val filename = pendingFilename ?: "game.swf"
                    pendingBytes = null
                    pendingFilename = null
                    loadSwfBytes(bytes, filename)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.w(TAG, "WebView resource error: ${error?.description} on ${request?.url}")
            }
        }

        // Load host page
        loadPlayerContainer()
    }

    fun loadPlayerContainer(aspectRatio: AspectRatioMode = AspectRatioMode.CONTAIN) {
        val html = RuffleHtmlTemplate.buildPlayerHtml(aspectRatio.cssClass)
        loadDataWithBaseURL("https://ruffle.ai-studio.local/", html, "text/html", "UTF-8", null)
    }

    fun loadSwfBytes(bytes: ByteArray, filename: String) {
        this.currentSwfBytes = bytes
        this.currentFilename = filename
        this.currentSwfBase64 = null // Lazily computed if needed

        if (!isHtmlLoaded) {
            pendingBytes = bytes
            pendingFilename = filename
            return
        }

        post {
            val safeName = filename.replace("'", "\\'")
            // Primary strategy: Load via local intercept URL
            // Secondary strategy: Call loadSwfDirectUrl or fallback to base64 if small
            if (bytes.size < 3_000_000) {
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                currentSwfBase64 = b64
                evaluateJavascript("if (window.loadSwfFromBase64) { window.loadSwfFromBase64('$b64', '$safeName'); }", null)
            } else {
                evaluateJavascript("if (window.loadSwfDirectUrl) { window.loadSwfDirectUrl('https://ruffle.ai-studio.local/current_game.swf', '$safeName'); }", null)
            }
        }
    }

    fun loadSwfBase64(base64Data: String, filename: String) {
        this.currentSwfBase64 = base64Data
        this.currentFilename = filename

        try {
            this.currentSwfBytes = Base64.decode(base64Data, Base64.DEFAULT)
        } catch (_: Exception) {}

        if (!isHtmlLoaded) {
            pendingBytes = currentSwfBytes
            pendingFilename = filename
            return
        }

        post {
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
