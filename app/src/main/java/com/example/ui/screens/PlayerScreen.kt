package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.viewmodel.ActivePlayerState
import com.example.viewmodel.FlashPlayerViewModel

/**
 * Standard delegate composable calling GamePlayerScreen.
 */
@Composable
fun PlayerScreen(
    viewModel: FlashPlayerViewModel,
    playerState: ActivePlayerState,
    onBack: () -> Unit
) {
    GamePlayerScreen(
        viewModel = viewModel,
        playerState = playerState,
        onBack = onBack
    )
}
