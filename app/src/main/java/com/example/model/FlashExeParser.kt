package com.example.model

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FlashExeParser {
    private const val TAG = "FlashExeParser"

    // SWF Magic Headers
    val MAGIC_FWS = byteArrayOf(0x46.toByte(), 0x57.toByte(), 0x53.toByte()) // Uncompressed
    val MAGIC_CWS = byteArrayOf(0x43.toByte(), 0x57.toByte(), 0x53.toByte()) // Zlib
    val MAGIC_ZWS = byteArrayOf(0x5A.toByte(), 0x57.toByte(), 0x53.toByte()) // LZMA

    // Flash Projector Footer Magic (0xFA123456)
    val PROJECTOR_FOOTER_MAGIC = byteArrayOf(0x56.toByte(), 0x34.toByte(), 0x12.toByte(), 0xFA.toByte())

    data class SwfExtractionResult(
        val isSuccess: Boolean,
        val swfBytes: ByteArray?,
        val flashVersion: Int = 0,
        val compressionType: String = "FWS",
        val fileSize: Int = 0,
        val errorMessage: String? = null
    )

    /**
     * Checks if the given bytes represent a direct SWF file.
     */
    fun isDirectSwf(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return (bytes[0] == 0x46.toByte() || bytes[0] == 0x43.toByte() || bytes[0] == 0x5A.toByte()) &&
                bytes[1] == 0x57.toByte() &&
                bytes[2] == 0x53.toByte()
    }

    /**
     * Checks if the given bytes represent a Windows PE Executable (Flash Projector .exe).
     */
    fun isWindowsExe(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        return bytes[0] == 0x4D.toByte() && bytes[1] == 0x5A.toByte() // 'MZ'
    }

    /**
     * Extracts an embedded .swf payload from a Flash Projector .exe or verifies a direct .swf file.
     */
    fun extractSwfFromBytes(bytes: ByteArray): SwfExtractionResult {
        if (bytes.isEmpty()) {
            return SwfExtractionResult(false, null, errorMessage = "Empty file data")
        }

        // Case 1: Direct SWF file
        if (isDirectSwf(bytes)) {
            val version = bytes[3].toInt() and 0xFF
            val comp = String(bytes.copyOfRange(0, 3), Charsets.US_ASCII)
            val declaredSize = readLittleEndianInt(bytes, 4)
            Log.d(TAG, "Direct SWF detected: $comp v$version, size: ${bytes.size} bytes (declared uncompressed: $declaredSize)")
            return SwfExtractionResult(
                isSuccess = true,
                swfBytes = bytes,
                flashVersion = version,
                compressionType = comp,
                fileSize = bytes.size
            )
        }

        // Case 2: Macromedia / Adobe Projector with Footer Signature (0xFA123456)
        if (bytes.size > 12) {
            val footerOffset = bytes.size - 8
            if (footerOffset >= 0 &&
                bytes[footerOffset] == PROJECTOR_FOOTER_MAGIC[0] &&
                bytes[footerOffset + 1] == PROJECTOR_FOOTER_MAGIC[1] &&
                bytes[footerOffset + 2] == PROJECTOR_FOOTER_MAGIC[2] &&
                bytes[footerOffset + 3] == PROJECTOR_FOOTER_MAGIC[3]
            ) {
                val swfLength = readLittleEndianInt(bytes, footerOffset + 4)
                val swfStart = footerOffset - swfLength
                if (swfStart >= 0 && swfLength > 0 && swfStart + swfLength <= bytes.size) {
                    val candidate = bytes.copyOfRange(swfStart, swfStart + swfLength)
                    if (isDirectSwf(candidate)) {
                        val version = candidate[3].toInt() and 0xFF
                        val comp = String(candidate.copyOfRange(0, 3), Charsets.US_ASCII)
                        Log.d(TAG, "Extracted SWF from Projector Footer: $comp v$version, length: $swfLength bytes")
                        return SwfExtractionResult(
                            isSuccess = true,
                            swfBytes = candidate,
                            flashVersion = version,
                            compressionType = comp,
                            fileSize = swfLength
                        )
                    }
                }
            }
        }

        // Case 3: Binary scan for SWF header signatures (FWS, CWS, ZWS)
        val foundOffsets = scanForSwfSignatures(bytes)
        if (foundOffsets.isNotEmpty()) {
            // Pick the best match (usually the last or longest embedded payload)
            for (offset in foundOffsets.reversed()) {
                val comp = String(bytes.copyOfRange(offset, offset + 3), Charsets.US_ASCII)
                val version = bytes[offset + 3].toInt() and 0xFF
                if (version in 1..40) {
                    val declaredUncompressedSize = readLittleEndianInt(bytes, offset + 4)
                    val remainingBytes = bytes.size - offset
                    
                    // For FWS (uncompressed), the file size in header is total length.
                    // For CWS/ZWS (compressed), payload extends to the end or projector boundary.
                    val extracted = if (comp == "FWS" && declaredUncompressedSize in 8..remainingBytes) {
                        bytes.copyOfRange(offset, offset + declaredUncompressedSize)
                    } else {
                        // Take from signature to end of file (or up to footer)
                        var end = bytes.size
                        if (bytes.size >= 8 &&
                            bytes[bytes.size - 8] == PROJECTOR_FOOTER_MAGIC[0] &&
                            bytes[bytes.size - 7] == PROJECTOR_FOOTER_MAGIC[1]
                        ) {
                            end = bytes.size - 8
                        }
                        bytes.copyOfRange(offset, end)
                    }

                    if (extracted.size >= 8) {
                        Log.d(TAG, "Binary scan extracted SWF at offset $offset: $comp v$version (${extracted.size} bytes)")
                        return SwfExtractionResult(
                            isSuccess = true,
                            swfBytes = extracted,
                            flashVersion = version,
                            compressionType = comp,
                            fileSize = extracted.size
                        )
                    }
                }
            }
        }

        return SwfExtractionResult(
            isSuccess = false,
            swfBytes = null,
            errorMessage = "No valid Flash SWF stream or Projector signature found in file"
        )
    }

    private fun scanForSwfSignatures(bytes: ByteArray): List<Int> {
        val offsets = mutableListOf<Int>()
        val maxScan = bytes.size - 8
        var i = 0
        while (i < maxScan) {
            val b = bytes[i]
            if (b == 0x46.toByte() || b == 0x43.toByte() || b == 0x5A.toByte()) { // F, C, Z
                if (bytes[i + 1] == 0x57.toByte() && bytes[i + 2] == 0x53.toByte()) { // W, S
                    val version = bytes[i + 3].toInt() and 0xFF
                    if (version in 1..40) {
                        offsets.add(i)
                    }
                }
            }
            i++
        }
        return offsets
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
}
