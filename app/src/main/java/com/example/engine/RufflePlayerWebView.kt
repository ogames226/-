package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.example.model.AspectRatioMode
import com.example.model.KeyBinding
import com.example.model.RenderQuality
import com.example.model.TouchMouseMode
import java.io.ByteArrayInputStream
import java.io.File

/**
 * WebAssembly-accelerated Flash Player WebView utilizing androidx.webkit.WebViewAssetLoader
 * to serve Ruffle and SWF binaries from https://appassets.androidplatform.net/
 */
@SuppressLint("SetJavaScriptEnabled")
open class RufflePlayerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RufflePlayerWebView"
        const val APP_ASSETS_ORIGIN = "https://appassets.androidplatform.net"
        const val INDEX_URL = "$APP_ASSETS_ORIGIN/assets/index.html"
    }

    private var isHtmlLoaded = false
    private var currentSwfBytes: ByteArray? = null
    private var currentSwfBase64: String? = null
    private var currentFilename: String? = null

    private var pendingBytes: ByteArray? = null
    private var pendingFilename: String? = null
    private var touchMouseMode: TouchMouseMode = TouchMouseMode.DIRECT_TOUCH

    var onGameLoadedCallback: ((title: String, width: Int, height: Int, fps: Double, frames: Int) -> Unit)? = null
    var onPlayerErrorCallback: ((message: String) -> Unit)? = null
    var onFpsUpdateCallback: ((fps: Int) -> Unit)? = null

    private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .setDomain("appassets.androidplatform.net")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(context))
        .addPathHandler("/games/", WebViewAssetLoader.InternalStoragePathHandler(context, File(context.filesDir, "flash_games")))
        .build()

    init {
        setupWebView()
    }

    private fun setupWebView() {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(0xFF080B11.toInt())

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        val bridge = FlashJsBridge(
            context = context,
            onLoaded = { title, width, height, frameRate, totalFrames ->
                post { onGameLoadedCallback?.invoke(title, width, height, frameRate, totalFrames) }
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
                Log.d(TAG, "[RuffleJS] ${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
                return true
            }
        }

        setupWebViewClient()
    }

    private fun setupWebViewClient() {
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null

                // AssetLoader resolution
                val assetResponse = assetLoader.shouldInterceptRequest(url)
                if (assetResponse != null) {
                    return assetResponse
                }

                val urlStr = url.toString()
                if (urlStr.contains("game.swf") || urlStr.contains("current_game.swf") ||
                    (currentFilename != null && urlStr.endsWith(currentFilename!!))
                ) {
                    val bytes = currentSwfBytes
                    if (bytes != null && bytes.isNotEmpty()) {
                        Log.d(TAG, "Serving direct SWF binary: ${bytes.size} bytes for $urlStr")
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
                Log.d(TAG, "Ruffle engine loaded: $url")
                isHtmlLoaded = true

                pendingBytes?.let { bytes ->
                    val filename = pendingFilename ?: "game.swf"
                    pendingBytes = null
                    pendingFilename = null
                    loadSwfBytes(bytes, filename)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.w(TAG, "WebView error: ${error?.description} on ${request?.url}")
            }
        }

        loadUrl(INDEX_URL)
    }

    fun loadSwfBytes(bytes: ByteArray, filename: String) {
        this.currentSwfBytes = bytes
        this.currentFilename = filename
        this.currentSwfBase64 = null

        if (!isHtmlLoaded) {
            pendingBytes = bytes
            pendingFilename = filename
            return
        }

        post {
            val safeName = filename.replace("'", "\\'")
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            currentSwfBase64 = b64

            if (bytes.size < 5_000_000) {
                evaluateJavascript("if (window.loadSwfFromBase64) { window.loadSwfFromBase64('$b64', '$safeName'); }", null)
            } else {
                evaluateJavascript("if (window.loadSwfDirectUrl) { window.loadSwfDirectUrl('$APP_ASSETS_ORIGIN/game.swf', '$safeName'); }", null)
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

    fun sendKeyEvent(eventType: String, keyCode: Int, key: String = "", code: String = "") {
        post {
            val script = "if (window.sendKeyEvent) { window.sendKeyEvent('$eventType', $keyCode, '$key', '$code'); }"
            evaluateJavascript(script, null)
        }
    }

    fun sendButtonDown(binding: KeyBinding) {
        sendKeyEvent("keydown", binding.jsKeyCode, binding.jsKey, binding.jsCode)
    }

    fun sendButtonUp(binding: KeyBinding) {
        sendKeyEvent("keyup", binding.jsKeyCode, binding.jsKey, binding.jsCode)
    }

    fun sendMouseEvent(eventType: String, normX: Float, normY: Float, button: Int = 0) {
        post {
            val script = "if (window.sendMouseEvent) { window.sendMouseEvent('$eventType', $normX, $normY, $button); }"
            evaluateJavascript(script, null)
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        post {
            evaluateJavascript("if (window.setAspectRatioMode) { window.setAspectRatioMode('${mode.name}'); }", null)
        }
    }

    fun setRenderQuality(quality: RenderQuality) {}

    fun setTouchMouseMode(mode: TouchMouseMode) {
        this.touchMouseMode = mode
        post {
            evaluateJavascript("if (window.setVirtualMouseMode) { window.setVirtualMouseMode(${mode == TouchMouseMode.VIRTUAL_TRACKPAD}); }", null)
        }
    }

    fun setAudioMuted(isMuted: Boolean) {
        post {
            evaluateJavascript("if (window.setAudioMuted) { window.setAudioMuted($isMuted); }", null)
        }
    }

    fun setPlaybackPaused(isPaused: Boolean) {
        post {
            evaluateJavascript("if (window.setPlaybackPaused) { window.setPlaybackPaused($isPaused); }", null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touchMouseMode == TouchMouseMode.VIRTUAL_TRACKPAD) {
            val width = width.toFloat().coerceAtLeast(1f)
            val height = height.toFloat().coerceAtLeast(1f)
            val normX = (event.x / width).coerceIn(0f, 1f)
            val normY = (event.y / height).coerceIn(0f, 1f)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> sendMouseEvent("mousedown", normX, normY, 0)
                MotionEvent.ACTION_MOVE -> sendMouseEvent("mousemove", normX, normY, 0)
                MotionEvent.ACTION_UP -> {
                    sendMouseEvent("mouseup", normX, normY, 0)
                    sendMouseEvent("click", normX, normY, 0)
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun destroyEngine() {
        try {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying engine", e)
        }
    }
}
