package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AspectRatioMode
import com.example.model.ControlType
import com.example.model.FlashGame
import com.example.model.GamepadConfig
import com.example.model.GamepadTheme

@Entity(tableName = "flash_games")
data class FlashGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val fileUri: String,
    val filePath: String = "",
    val fileName: String,
    val fileType: String, // "SWF", "EXE", "URL", "BUILTIN"
    val fileSize: Long = 0,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0,
    val playTimeMinutes: Int = 0,
    val preferredAspectRatio: String = AspectRatioMode.CONTAIN.name,
    val gamepadEnabled: Boolean = true,
    val gamepadOpacity: Float = 0.75f,
    val gamepadScale: Float = 1.0f,
    val gamepadControlType: String = ControlType.DPAD.name,
    val gamepadTheme: String = GamepadTheme.RETRO_ARCADE.name,
    val gamepadHaptics: Boolean = true,
    val thumbnailUri: String? = null,
    val isBuiltIn: Boolean = false,
    val builtInSampleKey: String? = null
) {
    fun toDomain(): FlashGame {
        val aspectMode = try {
            AspectRatioMode.valueOf(preferredAspectRatio)
        } catch (_: Exception) {
            AspectRatioMode.CONTAIN
        }

        val ctrlType = try {
            ControlType.valueOf(gamepadControlType)
        } catch (_: Exception) {
            ControlType.DPAD
        }

        val gpTheme = try {
            GamepadTheme.valueOf(gamepadTheme)
        } catch (_: Exception) {
            GamepadTheme.RETRO_ARCADE
        }

        return FlashGame(
            id = id,
            title = title,
            description = description,
            fileUri = fileUri,
            filePath = filePath,
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSize,
            isFavorite = isFavorite,
            lastPlayed = lastPlayed,
            playTimeMinutes = playTimeMinutes,
            preferredAspectRatio = aspectMode,
            preferredGamepad = GamepadConfig(
                isEnabled = gamepadEnabled,
                opacity = gamepadOpacity,
                scale = gamepadScale,
                controlType = ctrlType,
                theme = gpTheme,
                hapticsEnabled = gamepadHaptics
            ),
            thumbnailUri = thumbnailUri,
            isBuiltIn = isBuiltIn,
            builtInSampleKey = builtInSampleKey
        )
    }

    companion object {
        fun fromDomain(game: FlashGame): FlashGameEntity {
            return FlashGameEntity(
                id = if (game.id < 0) 0 else game.id,
                title = game.title,
                description = game.description,
                fileUri = game.fileUri,
                filePath = game.filePath,
                fileName = game.fileName,
                fileType = game.fileType,
                fileSize = game.fileSize,
                isFavorite = game.isFavorite,
                lastPlayed = game.lastPlayed,
                playTimeMinutes = game.playTimeMinutes,
                preferredAspectRatio = game.preferredAspectRatio.name,
                gamepadEnabled = game.preferredGamepad.isEnabled,
                gamepadOpacity = game.preferredGamepad.opacity,
                gamepadScale = game.preferredGamepad.scale,
                gamepadControlType = game.preferredGamepad.controlType.name,
                gamepadTheme = game.preferredGamepad.theme.name,
                gamepadHaptics = game.preferredGamepad.hapticsEnabled,
                thumbnailUri = game.thumbnailUri,
                isBuiltIn = game.isBuiltIn,
                builtInSampleKey = game.builtInSampleKey
            )
        }
    }
}
