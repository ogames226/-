package com.example.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Binary stream parser for extracting Flash SWF payloads from Windows Flash Projectors (.exe)
 * and raw SWF files (.swf).
 */
object ExeSwfExtractor {
    private const val TAG = "ExeSwfExtractor"

    // SWF Signatures: FWS (uncompressed), CWS (zlib compressed), ZWS (LZMA compressed)
    val SIGNATURE_FWS = byteArrayOf(0x46, 0x57, 0x53)
    val SIGNATURE_CWS = byteArrayOf(0x43, 0x57, 0x53)
    val SIGNATURE_ZWS = byteArrayOf(0x5A, 0x57, 0x53)

    // Standard Macromedia/Adobe Flash Projector Footer Magic (0xFA123456 in Little Endian)
    val PROJECTOR_FOOTER_MAGIC = byteArrayOf(0x56, 0x34, 0x12, 0xFA.toByte())

    data class ExtractionResult(
        val isSuccess: Boolean,
        val swfBytes: ByteArray?,
        val outputFile: File? = null,
        val flashVersion: Int = 0,
        val compressionType: String = "FWS",
        val fileSize: Int = 0,
        val isExeProjector: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Checks if the bytes begin with MZ (Windows PE Executable).
     */
    fun isWindowsExe(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        return bytes[0] == 0x4D.toByte() && bytes[1] == 0x5A.toByte() // 'MZ'
    }

    /**
     * Checks if the bytes begin directly with an SWF signature.
     */
    fun isDirectSwf(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val isSig = (bytes[0] == 0x46.toByte() || bytes[0] == 0x43.toByte() || bytes[0] == 0x5A.toByte()) &&
                bytes[1] == 0x57.toByte() &&
                bytes[2] == 0x53.toByte()
        val version = bytes[3].toInt() and 0xFF
        return isSig && version in 1..40
    }

    /**
     * Extracts an SWF stream from raw bytes and saves it to internal cache storage.
     */
    fun extractSwf(bytes: ByteArray, context: Context? = null, outputFileName: String = "extracted_game.swf"): ExtractionResult {
        if (bytes.isEmpty()) {
            return ExtractionResult(false, null, errorMessage = "Byte stream is empty")
        }

        val isExe = isWindowsExe(bytes)

        // Case 1: Direct SWF file
        if (isDirectSwf(bytes)) {
            val comp = String(bytes.copyOfRange(0, 3), Charsets.US_ASCII)
            val version = bytes[3].toInt() and 0xFF
            val declaredSize = readLittleEndianInt(bytes, 4)
            Log.d(TAG, "Direct SWF recognized: $comp v$version (${bytes.size} bytes, target size: $declaredSize)")

            val outFile = writeToCacheIfRequested(context, bytes, outputFileName)
            return ExtractionResult(
                isSuccess = true,
                swfBytes = bytes,
                outputFile = outFile,
                flashVersion = version,
                compressionType = comp,
                fileSize = bytes.size,
                isExeProjector = false
            )
        }

        // Case 2: Macromedia / Adobe Flash Projector Footer (0xFA123456)
        if (bytes.size > 12) {
            val footerPos = bytes.size - 8
            if (footerPos >= 0 &&
                bytes[footerPos] == PROJECTOR_FOOTER_MAGIC[0] &&
                bytes[footerPos + 1] == PROJECTOR_FOOTER_MAGIC[1] &&
                bytes[footerPos + 2] == PROJECTOR_FOOTER_MAGIC[2] &&
                bytes[footerPos + 3] == PROJECTOR_FOOTER_MAGIC[3]
            ) {
                val swfLength = readLittleEndianInt(bytes, footerPos + 4)
                val swfStart = footerPos - swfLength
                if (swfStart >= 0 && swfLength > 0 && swfStart + swfLength <= bytes.size) {
                    val candidate = bytes.copyOfRange(swfStart, swfStart + swfLength)
                    if (isDirectSwf(candidate)) {
                        val comp = String(candidate.copyOfRange(0, 3), Charsets.US_ASCII)
                        val version = candidate[3].toInt() and 0xFF
                        Log.d(TAG, "Extracted Flash Projector via footer magic: $comp v$version, size: $swfLength bytes")
                        val outFile = writeToCacheIfRequested(context, candidate, outputFileName)
                        return ExtractionResult(
                            isSuccess = true,
                            swfBytes = candidate,
                            outputFile = outFile,
                            flashVersion = version,
                            compressionType = comp,
                            fileSize = swfLength,
                            isExeProjector = true
                        )
                    }
                }
            }
        }

        // Case 3: Deep binary scan for SWF header signatures (FWS, CWS, ZWS)
        val matches = scanSignatures(bytes)
        if (matches.isNotEmpty()) {
            for (offset in matches.reversed()) {
                val comp = String(bytes.copyOfRange(offset, offset + 3), Charsets.US_ASCII)
                val version = bytes[offset + 3].toInt() and 0xFF
                if (version in 1..40) {
                    val declaredUncompressedSize = readLittleEndianInt(bytes, offset + 4)
                    val remaining = bytes.size - offset

                    val extractedBytes: ByteArray = if (comp == "FWS" && declaredUncompressedSize in 8..remaining) {
                        bytes.copyOfRange(offset, offset + declaredUncompressedSize)
                    } else {
                        var end = bytes.size
                        if (bytes.size >= 8 &&
                            bytes[bytes.size - 8] == PROJECTOR_FOOTER_MAGIC[0] &&
                            bytes[bytes.size - 7] == PROJECTOR_FOOTER_MAGIC[1]
                        ) {
                            end = bytes.size - 8
                        }
                        bytes.copyOfRange(offset, end)
                    }

                    if (extractedBytes.size >= 8) {
                        Log.d(TAG, "Extracted SWF via scan at offset $offset: $comp v$version (${extractedBytes.size} bytes)")
                        val outFile = writeToCacheIfRequested(context, extractedBytes, outputFileName)
                        return ExtractionResult(
                            isSuccess = true,
                            swfBytes = extractedBytes,
                            outputFile = outFile,
                            flashVersion = version,
                            compressionType = comp,
                            fileSize = extractedBytes.size,
                            isExeProjector = isExe
                        )
                    }
                }
            }
        }

        return ExtractionResult(
            isSuccess = false,
            swfBytes = null,
            errorMessage = "No valid SWF stream (FWS/CWS/ZWS) found in file"
        )
    }

    /**
     * Reads an InputStream and extracts the embedded SWF.
     */
    fun extractFromStream(inputStream: InputStream, context: Context? = null, outputFileName: String = "extracted_game.swf"): ExtractionResult {
        val bytes = inputStream.use { it.readBytes() }
        return extractSwf(bytes, context, outputFileName)
    }

    private fun writeToCacheIfRequested(context: Context?, bytes: ByteArray, fileName: String): File? {
        if (context == null) return null
        return try {
            val cacheDir = File(context.cacheDir, "extracted_swf").apply { mkdirs() }
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing extracted SWF to cache", e)
            null
        }
    }

    private fun scanSignatures(bytes: ByteArray): List<Int> {
        val offsets = mutableListOf<Int>()
        val max = bytes.size - 8
        var i = 0
        while (i < max) {
            val b = bytes[i]
            if (b == 0x46.toByte() || b == 0x43.toByte() || b == 0x5A.toByte()) {
                if (bytes[i + 1] == 0x57.toByte() && bytes[i + 2] == 0x53.toByte()) {
                    val ver = bytes[i + 3].toInt() and 0xFF
                    if (ver in 1..40) {
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
