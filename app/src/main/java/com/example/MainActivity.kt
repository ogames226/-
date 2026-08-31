package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GamePlayerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FlashPlayerViewModel

const val ROUTE_LIBRARY = "library"
const val ROUTE_PLAYER = "player"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {

    private val viewModel: FlashPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent (e.g. file opened from external file explorer)
        handleIntent(intent)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkCanvas
                    ) {
                        FlashAppNavigation(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (uri != null) {
            var fileName = "opened_game.swf"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }
            viewModel.importGameFromUri(uri, fileName)
        }
    }
}

@Composable
fun FlashAppNavigation(viewModel: FlashPlayerViewModel) {
    val navController = rememberNavController()
    val libraryUiState by viewModel.libraryUiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIBRARY,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) }
    ) {
        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                viewModel = viewModel,
                uiState = libraryUiState,
                onLaunchGame = {
                    navController.navigate(ROUTE_PLAYER)
                },
                onNavigateSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                }
            )
        }

        composable(ROUTE_PLAYER) {
            GamePlayerScreen(
                viewModel = viewModel,
                playerState = playerState,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

