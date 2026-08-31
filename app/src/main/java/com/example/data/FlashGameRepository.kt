package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.BuiltInSampleGames
import com.example.model.FlashExeParser
import com.example.model.FlashGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FlashGameRepository(
    private val context: Context,
    private val dao: FlashGameDao
) {
    private val TAG = "FlashGameRepository"

    val allGames: Flow<List<FlashGame>> = dao.getAllGames().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteGames: Flow<List<FlashGame>> = dao.getFavoriteGames().map { list ->
        list.map { it.toDomain() }
    }

    val recentGames: Flow<List<FlashGame>> = dao.getRecentGames().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getGameById(id: Long): FlashGame? {
        if (id < 0) {
            return BuiltInSampleGames.SAMPLE_GAMES.find { it.id == id }
        }
        return dao.getGameById(id)?.toDomain()
    }

    suspend fun saveGame(game: FlashGame): Long = withContext(Dispatchers.IO) {
        val entity = FlashGameEntity.fromDomain(game)
        dao.insertGame(entity)
    }

    suspend fun updateGame(game: FlashGame) = withContext(Dispatchers.IO) {
        val entity = FlashGameEntity.fromDomain(game)
        dao.updateGame(entity)
    }

    suspend fun deleteGame(game: FlashGame) = withContext(Dispatchers.IO) {
        if (game.filePath.isNotEmpty()) {
            val file = File(game.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        if (game.id > 0) {
            dao.deleteGameById(game.id)
        }
    }

    suspend fun recordPlaySession(gameId: Long, durationMinutes: Int = 1) = withContext(Dispatchers.IO) {
        if (gameId > 0) {
            dao.updatePlaySession(gameId, System.currentTimeMillis(), durationMinutes)
        }
    }

    suspend fun toggleFavorite(gameId: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        if (gameId > 0) {
            dao.toggleFavorite(gameId, isFav)
        }
    }

    /**
     * Imports a user-selected URI (from SAF or Intent) into the app's internal game storage.
     * Parses EXE if necessary to extract embedded SWF.
     */
    suspend fun importGameFromUri(uri: Uri, originalName: String): Result<FlashGame> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open stream for URI: $uri"))

            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(Exception("File is empty"))
            }

            val isExe = originalName.endsWith(".exe", ignoreCase = true) || FlashExeParser.isWindowsExe(rawBytes)
            val extracted = FlashExeParser.extractSwfFromBytes(rawBytes)

            if (!extracted.isSuccess || extracted.swfBytes == null) {
                return@withContext Result.failure(
                    Exception(extracted.errorMessage ?: "Failed to extract or validate Flash SWF file")
                )
            }

            // Save extracted SWF into app internal files storage
            val safeName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val baseName = if (safeName.contains(".")) safeName.substringBeforeLast(".") else safeName
            val targetFileName = "${baseName}_${System.currentTimeMillis()}.swf"
            val gamesDir = File(context.filesDir, "flash_games").apply { mkdirs() }
            val targetFile = File(gamesDir, targetFileName)

            FileOutputStream(targetFile).use { fos ->
                fos.write(extracted.swfBytes)
            }

            val gameTitle = baseName.replace("_", " ").trim()
                .split(" ")
                .filter { it.isNotEmpty() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            val game = FlashGame(
                title = gameTitle.ifEmpty { "Flash Game" },
                description = if (isExe) "Flash Projector EXE (Flash v${extracted.flashVersion}, ${extracted.compressionType})"
                else "Flash SWF (Flash v${extracted.flashVersion}, ${extracted.compressionType})",
                fileUri = Uri.fromFile(targetFile).toString(),
                filePath = targetFile.absolutePath,
                fileName = targetFileName,
                fileType = if (isExe) "EXE" else "SWF",
                fileSize = extracted.swfBytes.size.toLong(),
                isFavorite = false,
                lastPlayed = System.currentTimeMillis()
            )

            val insertedId = saveGame(game)
            Result.success(game.copy(id = insertedId))
        } catch (e: Exception) {
            Log.e(TAG, "Error importing game", e)
            Result.failure(e)
        }
    }

    /**
     * Reads the SWF bytes for a given game (whether built-in, local file, or uri) and returns base64.
     */
    suspend fun getGameSwfBase64(game: FlashGame): String = withContext(Dispatchers.IO) {
        val bytes = if (game.isBuiltIn && game.builtInSampleKey != null) {
            BuiltInSampleGames.getSampleSwfBytes(game.builtInSampleKey)
        } else if (game.filePath.isNotEmpty()) {
            val file = File(game.filePath)
            if (file.exists()) file.readBytes() else ByteArray(0)
        } else {
            try {
                val uri = Uri.parse(game.fileUri)
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading game bytes from uri", e)
                ByteArray(0)
            }
        }

        // Validate or extract if it was an unparsed EXE
        val finalBytes = if (FlashExeParser.isWindowsExe(bytes)) {
            val extracted = FlashExeParser.extractSwfFromBytes(bytes)
            extracted.swfBytes ?: bytes
        } else {
            bytes
        }

        Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    }
}
