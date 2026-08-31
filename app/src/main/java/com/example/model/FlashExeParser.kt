package com.example.model

import java.io.InputStream

object FlashExeParser {
    data class SwfExtractionResult(
        val isSuccess: Boolean,
        val swfBytes: ByteArray?,
        val flashVersion: Int = 0,
        val compressionType: String = "FWS",
        val fileSize: Int = 0,
        val errorMessage: String? = null
    )

    fun isDirectSwf(bytes: ByteArray): Boolean = ExeSwfExtractor.isDirectSwf(bytes)
    fun isWindowsExe(bytes: ByteArray): Boolean = ExeSwfExtractor.isWindowsExe(bytes)

    fun extractSwfFromBytes(bytes: ByteArray): SwfExtractionResult {
        val result = ExeSwfExtractor.extractSwf(bytes)
        return SwfExtractionResult(
            isSuccess = result.isSuccess,
            swfBytes = result.swfBytes,
            flashVersion = result.flashVersion,
            compressionType = result.compressionType,
            fileSize = result.fileSize,
            errorMessage = result.errorMessage
        )
    }
}

