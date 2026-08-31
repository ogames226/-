package com.example.engine

import android.content.Context
import android.util.AttributeSet

/**
 * Type alias and subclass for backward compatibility with RuffleEngineWebView.
 */
class FlashPlayerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RuffleEngineWebView(context, attrs, defStyleAttr)
