package com.example.engine

import android.content.Context
import android.util.AttributeSet

/**
 * Open subclass for backward compatibility with existing usages.
 */
open class RuffleEngineWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RufflePlayerWebView(context, attrs, defStyleAttr)
