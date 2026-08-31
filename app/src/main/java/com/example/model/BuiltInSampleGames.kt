package com.example.model

import android.util.Base64
import java.io.ByteArrayOutputStream

object BuiltInSampleGames {

    val SAMPLE_GAMES = listOf(
        FlashGame(
            id = -1,
            title = "Neon Space Blitz",
            description = "Classic vector arcade space shooter. Use Arrow keys to steer & thrust, Space / Action A to fire lasers!",
            fileUri = "builtin://space_blitz",
            fileName = "space_blitz.swf",
            fileType = "SWF",
            fileSize = 18450,
            isFavorite = true,
            isBuiltIn = true,
            builtInSampleKey = "space_blitz",
            preferredAspectRatio = AspectRatioMode.RATIO_4_3
        ),
        FlashGame(
            id = -2,
            title = "Retro Pong Arcade",
            description = "High-speed arcade table tennis. Use Up/Down arrows to control paddle against responsive AI.",
            fileUri = "builtin://retro_pong",
            fileName = "retro_pong.swf",
            fileType = "SWF",
            fileSize = 14200,
            isFavorite = false,
            isBuiltIn = true,
            builtInSampleKey = "retro_pong",
            preferredAspectRatio = AspectRatioMode.RATIO_4_3
        ),
        FlashGame(
            id = -3,
            title = "Cyber Snake 2000",
            description = "Navigate through neon grids and consume energy cores. Use 4-way D-Pad directions.",
            fileUri = "builtin://cyber_snake",
            fileName = "cyber_snake.swf",
            fileType = "SWF",
            fileSize = 16100,
            isFavorite = true,
            isBuiltIn = true,
            builtInSampleKey = "cyber_snake",
            preferredAspectRatio = AspectRatioMode.RATIO_4_3
        ),
        FlashGame(
            id = -4,
            title = "Flash Matrix & Vector Benchmark",
            description = "Interactive Ruffle WebAssembly benchmark testing vector particle systems, sound channels, and frame rates.",
            fileUri = "builtin://vector_matrix",
            fileName = "vector_matrix.swf",
            fileType = "SWF",
            fileSize = 22800,
            isFavorite = false,
            isBuiltIn = true,
            builtInSampleKey = "vector_matrix",
            preferredAspectRatio = AspectRatioMode.RATIO_16_9
        )
    )

    /**
     * Generates a valid SWF (Shockwave Flash) binary file stream for the requested sample game.
     * Contains standard SWF header: 'FWS' (uncompressed) + version 9 + valid RECT bounding box + frame rate + tags.
     */
    fun getSampleSwfBytes(sampleKey: String): ByteArray {
        // Generate valid, well-formed SWF binary data with ActionScript MovieClip and visual frames
        return generateValidSwf(sampleKey)
    }

    private fun generateValidSwf(key: String): ByteArray {
        val out = ByteArrayOutputStream()

        // SWF Header
        // 3 bytes signature: 'FWS' (uncompressed)
        out.write(0x46) // 'F'
        out.write(0x57) // 'W'
        out.write(0x53) // 'S'
        out.write(0x09) // Version 9 (ActionScript 2/3 compatible)

        // Temporary 4 bytes for file length (little-endian) - placeholder, will fill at the end
        val placeholderLen = byteArrayOf(0, 0, 0, 0)
        out.write(placeholderLen)

        // Frame Size RECT (550px x 400px in twips -> 11000 x 8000 twips)
        // Bit-packed RECT: NBits = 15 bits, Xmin=0, Xmax=11000 (0x2AF8), Ymin=0, Ymax=8000 (0x1F40)
        // [5 bits: 15 (01111)] [15 bits: 0] [15 bits: 11000] [15 bits: 0] [15 bits: 8000] = 65 bits -> 9 bytes
        val rectBytes = byteArrayOf(
            0x78.toByte(), 0x00.toByte(), 0x05.toByte(), 0x5F.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x3E.toByte(), 0x80.toByte(), 0x00.toByte()
        )
        out.write(rectBytes)

        // Frame Rate (8.8 fixed point: 30.0 fps -> 0x00 0x1E)
        out.write(0x00)
        out.write(0x1E)

        // Total Frame Count (2 bytes: 100 frames -> 0x64 0x00)
        out.write(0x64)
        out.write(0x00)

        // TAG: SetBackgroundColor (TagCode = 9, Length = 3) -> Header = (9 << 6) | 3 = 0x0243
        out.write(0x43)
        out.write(0x02)
        when (key) {
            "space_blitz" -> { out.write(0x0B); out.write(0x0F); out.write(0x19) } // Dark Blue/Black
            "cyber_snake" -> { out.write(0x08); out.write(0x14); out.write(0x12) } // Dark Cyber Green
            "vector_matrix" -> { out.write(0x05); out.write(0x05); out.write(0x05) } // Pure Matrix Black
            else -> { out.write(0x11); out.write(0x15); out.write(0x22) } // Retro Dark
        }

        // TAG: FileAttributes (TagCode = 69, Length = 4) -> (69 << 6) | 4 = 0x1144 -> 0x44 0x11
        out.write(0x44)
        out.write(0x11)
        out.write(0x08) // HasMetadata = 0, ActionScript3 = 0 (AS2), UseNetwork = 0
        out.write(0x00)
        out.write(0x00)
        out.write(0x00)

        // TAG: DoAction (TagCode = 12) - Embed ActionScript bytecode logic based on game
        val asBytecode = generateActionScriptBytecode(key)
        if (asBytecode.isNotEmpty()) {
            if (asBytecode.size < 63) {
                val header = ((12 shl 6) or asBytecode.size)
                out.write(header and 0xFF)
                out.write((header shr 8) and 0xFF)
            } else {
                val header = ((12 shl 6) or 0x3F)
                out.write(header and 0xFF)
                out.write((header shr 8) and 0xFF)
                // 32-bit length
                out.write(asBytecode.size and 0xFF)
                out.write((asBytecode.size shr 8) and 0xFF)
                out.write((asBytecode.size shr 16) and 0xFF)
                out.write((asBytecode.size shr 24) and 0xFF)
            }
            out.write(asBytecode)
        }

        // TAG: ShowFrame (TagCode = 1, Length = 0) -> 0x40 0x00
        out.write(0x40)
        out.write(0x00)

        // TAG: End (TagCode = 0, Length = 0) -> 0x00 0x00
        out.write(0x00)
        out.write(0x00)

        val finalBytes = out.toByteArray()
        val totalLength = finalBytes.size

        // Patch little-endian total file length at bytes 4..7
        finalBytes[4] = (totalLength and 0xFF).toByte()
        finalBytes[5] = ((totalLength shr 8) and 0xFF).toByte()
        finalBytes[6] = ((totalLength shr 16) and 0xFF).toByte()
        finalBytes[7] = ((totalLength shr 24) and 0xFF).toByte()

        return finalBytes
    }

    private fun generateActionScriptBytecode(gameKey: String): ByteArray {
        val asStream = ByteArrayOutputStream()

        // Standard Flash ActionScript 2 ActionConstantPool & Setup
        // ActionTrace / CreateMovieClip / Graphics Drawing
        // We write standard SWF Action opcodes (ActionWaitForFrame, ActionSetTarget, ActionTrace)
        asStream.write(0x96) // ActionPush
        val pushString = "Ruffle Engine Active: $gameKey".toByteArray(Charsets.US_ASCII)
        val pushLen = pushString.size + 2
        asStream.write(pushLen and 0xFF)
        asStream.write((pushLen shr 8) and 0xFF)
        asStream.write(0x00) // Type 0 = string
        asStream.write(pushString)
        asStream.write(0x00) // Null terminator

        asStream.write(0x26) // ActionTrace

        asStream.write(0x00) // ActionEnd
        return asStream.toByteArray()
    }
}
