package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlashGameRepository
import com.example.data.FlashPlayerDatabase
import com.example.model.AspectRatioMode
import com.example.model.BuiltInSampleGames
import com.example.model.FlashGame
import com.example.model.GamepadConfig
import com.example.model.PlayerSettings
import com.example.model.RenderQuality
import com.example.model.TouchMouseMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashLibraryUiState(
    val games: List<FlashGame> = emptyList(),
    val sampleGames: List<FlashGame> = BuiltInSampleGames.SAMPLE_GAMES,
    val searchQuery: String = "",
    val selectedFilter: GameFilter = GameFilter.ALL,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class GameFilter {
    ALL,
    FAVORITES,
    SWF_ONLY,
    EXE_PROJECTORS,
    BUILT_IN
}

data class ActivePlayerState(
    val game: FlashGame? = null,
    val swfBase64: String? = null,
    val isLoaded: Boolean = false,
    val currentFps: Int = 60,
    val aspectRatio: AspectRatioMode = AspectRatioMode.CONTAIN,
    val gamepadConfig: GamepadConfig = GamepadConfig(),
    val playerSettings: PlayerSettings = PlayerSettings(),
    val errorMessage: String? = null
)

class FlashPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "FlashPlayerViewModel"

    private val repository: FlashGameRepository = FlashGameRepository(
        context = application,
        dao = FlashPlayerDatabase.getDatabase(application).flashGameDao()
    )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(GameFilter.ALL)
    private val _isImporting = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<Pair<String?, Boolean>>(null to false) // message to isError

    // Combine local DB games with built-in samples and search filter
    val libraryUiState: StateFlow<FlashLibraryUiState> = combine(
        repository.allGames,
        _searchQuery,
        _selectedFilter,
        _isImporting,
        _userMessage
    ) { userGames, query, filter, importing, message ->
        val allCombined = (BuiltInSampleGames.SAMPLE_GAMES + userGames)
        val filtered = allCombined.filter { game ->
            val matchesQuery = query.isEmpty() ||
                    game.title.contains(query, ignoreCase = true) ||
                    game.fileName.contains(query, ignoreCase = true) ||
                    game.description.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                GameFilter.ALL -> true
                GameFilter.FAVORITES -> game.isFavorite
                GameFilter.SWF_ONLY -> game.fileType == "SWF"
                GameFilter.EXE_PROJECTORS -> game.fileType == "EXE"
                GameFilter.BUILT_IN -> game.isBuiltIn
            }

            matchesQuery && matchesFilter
        }

        FlashLibraryUiState(
            games = filtered,
            sampleGames = BuiltInSampleGames.SAMPLE_GAMES,
            searchQuery = query,
            selectedFilter = filter,
            isImporting = importing,
            errorMessage = if (message.second) message.first else null,
            successMessage = if (!message.second) message.first else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlashLibraryUiState()
    )

    // Active Player State
    private val _playerState = MutableStateFlow(ActivePlayerState())
    val playerState: StateFlow<ActivePlayerState> = _playerState.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: GameFilter) {
        _selectedFilter.value = filter
    }

    fun clearMessage() {
        _userMessage.value = null to false
    }

    fun importGameFromUri(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.importGameFromUri(uri, displayName)
            _isImporting.value = false

            result.onSuccess { game ->
                _userMessage.value = "Imported '${game.title}' (${game.fileType}) successfully!" to false
            }.onFailure { error ->
                _userMessage.value = (error.message ?: "Failed to import game") to true
            }
        }
    }

    fun importDirectSwfUrl(url: String, title: String) {
        viewModelScope.launch {
            try {
                val game = FlashGame(
                    title = title.ifEmpty { "Flash Online Game" },
                    description = "Online SWF Stream: $url",
                    fileUri = url,
                    fileName = url.substringAfterLast("/").ifEmpty { "game.swf" },
                    fileType = "SWF",
                    lastPlayed = System.currentTimeMillis()
                )
                repository.saveGame(game)
                _userMessage.value = "Added online Flash game to library" to false
            } catch (e: Exception) {
                _userMessage.value = "Error adding URL: ${e.message}" to true
            }
        }
    }

    fun toggleFavorite(game: FlashGame) {
        viewModelScope.launch {
            if (game.id > 0) {
                repository.toggleFavorite(game.id, !game.isFavorite)
            }
        }
    }

    fun deleteGame(game: FlashGame) {
        viewModelScope.launch {
            repository.deleteGame(game)
            _userMessage.value = "Deleted '${game.title}'" to false
        }
    }

    fun launchGame(game: FlashGame) {
        viewModelScope.launch {
            _playerState.value = ActivePlayerState(
                game = game,
                aspectRatio = game.preferredAspectRatio,
                gamepadConfig = game.preferredGamepad
            )

            // Update last played
            if (game.id > 0) {
                repository.recordPlaySession(game.id, 1)
            }

            try {
                val b64 = repository.getGameSwfBase64(game)
                _playerState.update { it.copy(swfBase64 = b64) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed preparing SWF base64", e)
                _playerState.update { it.copy(errorMessage = "Failed to load game stream: ${e.message}") }
            }
        }
    }

    fun onGameLoadedInEngine(title: String, width: Int, height: Int, fps: Double, frames: Int) {
        _playerState.update {
            it.copy(isLoaded = true, currentFps = fps.toInt().coerceAtLeast(30))
        }
    }

    fun onFpsUpdated(fps: Int) {
        _playerState.update { it.copy(currentFps = fps) }
    }

    fun onEngineError(msg: String) {
        _playerState.update { it.copy(errorMessage = msg) }
    }

    fun cycleAspectRatio() {
        val current = _playerState.value.aspectRatio
        val next = when (current) {
            AspectRatioMode.CONTAIN -> AspectRatioMode.RATIO_4_3
            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.RATIO_16_9
            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.CONTAIN
        }
        setAspectRatio(next)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _playerState.update { it.copy(aspectRatio = mode) }
    }

    fun toggleGamepad() {
        _playerState.update {
            val updated = it.gamepadConfig.copy(isEnabled = !it.gamepadConfig.isEnabled)
            it.copy(gamepadConfig = updated)
        }
    }

    fun toggleTurbo() {
        _playerState.update {
            val updated = it.gamepadConfig.copy(turboEnabled = !it.gamepadConfig.turboEnabled)
            it.copy(gamepadConfig = updated)
        }
    }

    fun updateGamepadConfig(newConfig: GamepadConfig) {
        _playerState.update { it.copy(gamepadConfig = newConfig) }
    }

    fun updatePlayerSettings(newSettings: PlayerSettings) {
        _playerState.update { it.copy(playerSettings = newSettings) }
    }

    fun toggleMouseMode() {
        _playerState.update {
            val currentMode = it.playerSettings.touchMouseMode
            val nextMode = if (currentMode == TouchMouseMode.DIRECT_TOUCH) {
                TouchMouseMode.VIRTUAL_TRACKPAD
            } else {
                TouchMouseMode.DIRECT_TOUCH
            }
            it.copy(playerSettings = it.playerSettings.copy(touchMouseMode = nextMode))
        }
    }

    fun toggleMute() {
        _playerState.update {
            it.copy(playerSettings = it.playerSettings.copy(isMuted = !it.playerSettings.isMuted))
        }
    }

    fun closePlayer() {
        _playerState.value = ActivePlayerState()
    }
}
