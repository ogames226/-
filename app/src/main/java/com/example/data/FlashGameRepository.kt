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

    suspend fun updateGameTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        if (id > 0) {
            val entity = dao.getGameById(id)
            if (entity != null) {
                dao.updateGame(entity.copy(title = newTitle.trim().ifEmpty { entity.title }))
            }
        }
    }

    /**
     * Cleans a raw filename into a human-readable, formatted game title.
     * Supports English, Arabic, numbers, and symbols without mangling.
     */
    fun cleanGameTitle(originalName: String): String {
        var name = originalName
        if (name.contains("%")) {
            try {
                name = java.net.URLDecoder.decode(name, "UTF-8")
            } catch (_: Exception) {}
        }
        if (name.endsWith(".swf", ignoreCase = true) || name.endsWith(".exe", ignoreCase = true)) {
            name = name.substring(0, name.length - 4)
        }

        // Replace hyphens, underscores, and dots with spaces
        name = name.replace("[-_.]+".toRegex(), " ").trim()

        // Capitalize words if ASCII
        val words = name.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val formatted = words.joinToString(" ") { word ->
            if (word.isNotEmpty()) word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            else ""
        }

        return formatted.ifEmpty { "Flash Game" }
    }

    /**
     * Imports a user-selected URI (from SAF or Intent) into the app's internal game storage.
     * Parses EXE if necessary to extract embedded SWF.
     */
    suspend fun importGameFromUri(uri: Uri, originalName: String, customTitle: String? = null): Result<FlashGame> = withContext(Dispatchers.IO) {
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

            // Save extracted SWF into app internal files storage with safe ASCII filename on disk
            val targetFileName = "game_${System.currentTimeMillis()}.swf"
            val gamesDir = File(context.filesDir, "flash_games").apply { mkdirs() }
            val targetFile = File(gamesDir, targetFileName)

            FileOutputStream(targetFile).use { fos ->
                fos.write(extracted.swfBytes)
            }

            val gameTitle = if (!customTitle.isNullOrBlank()) {
                customTitle.trim()
            } else {
                cleanGameTitle(originalName)
            }

            val game = FlashGame(
                title = gameTitle,
                description = if (isExe) "Flash Projector EXE (Flash v${extracted.flashVersion}, ${extracted.compressionType})"
                else "Flash SWF (Flash v${extracted.flashVersion}, ${extracted.compressionType})",
                fileUri = Uri.fromFile(targetFile).toString(),
                filePath = targetFile.absolutePath,
                fileName = originalName,
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
     * Reads the raw SWF bytes for a given game.
     */
    suspend fun getGameSwfBytes(game: FlashGame): ByteArray = withContext(Dispatchers.IO) {
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

        if (FlashExeParser.isWindowsExe(bytes)) {
            val extracted = FlashExeParser.extractSwfFromBytes(bytes)
            extracted.swfBytes ?: bytes
        } else {
            bytes
        }
    }

    /**
     * Reads the SWF bytes for a given game (whether built-in, local file, or uri) and returns base64.
     */
    suspend fun getGameSwfBase64(game: FlashGame): String = withContext(Dispatchers.IO) {
        val finalBytes = getGameSwfBytes(game)
        Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    }
}
