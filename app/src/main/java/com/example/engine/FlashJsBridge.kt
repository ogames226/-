package com.example.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.webkit.JavascriptInterface

class FlashJsBridge(
    private val context: Context,
    private val onLoaded: (title: String, width: Int, height: Int, frameRate: Double, totalFrames: Int) -> Unit = { _, _, _, _, _ -> },
    private val onError: (message: String) -> Unit = {},
    private val onFps: (fps: Int) -> Unit = {},
    private val base64Supplier: () -> String = { "" }
) {
    private val TAG = "FlashJsBridge"

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @JavascriptInterface
    fun getSwfBase64(): String {
        return base64Supplier()
    }

    @JavascriptInterface
    fun onPlayerLoaded(title: String, width: Int, height: Int, frameRate: Double, totalFrames: Int) {
        Log.d(TAG, "Ruffle loaded SWF: $title (${width}x${height}, ${frameRate}fps, frames=$totalFrames)")
        onLoaded(title, width, height, frameRate, totalFrames)
    }

    @JavascriptInterface
    fun onPlayerError(message: String) {
        Log.e(TAG, "Ruffle error: $message")
        onError(message)
    }

    @JavascriptInterface
    fun onFpsUpdate(fps: Int) {
        onFps(fps)
    }

    @JavascriptInterface
    fun requestVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(5, 200), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs.coerceIn(5, 200))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }
}
