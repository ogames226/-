package com.example.ui.screens

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.FlashPlayerWebView
import com.example.model.AspectRatioMode
import com.example.model.ControlType
import com.example.model.GamepadTheme
import com.example.model.RenderQuality
import com.example.model.TouchMouseMode
import com.example.ui.components.KeyMappingDialog
import com.example.ui.components.QuickToolbar
import com.example.ui.components.VirtualGamepadOverlay
import com.example.ui.components.VirtualMouseOverlay
import com.example.ui.theme.*
import com.example.viewmodel.ActivePlayerState
import com.example.viewmodel.FlashPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: FlashPlayerViewModel,
    playerState: ActivePlayerState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<FlashPlayerWebView?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showKeyMappingDialog by remember { mutableStateOf(false) }

    // Send SWF data when Base64 stream is ready
    LaunchedEffect(playerState.swfBase64, webViewInstance) {
        val b64 = playerState.swfBase64
        val wv = webViewInstance
        if (b64 != null && wv != null) {
            wv.loadSwfBase64(b64, playerState.game?.fileName ?: "game.swf")
        }
    }

    // Aspect ratio updates
    LaunchedEffect(playerState.aspectRatio, webViewInstance) {
        webViewInstance?.setAspectRatio(playerState.aspectRatio)
    }

    // Quality updates
    LaunchedEffect(playerState.playerSettings.quality, webViewInstance) {
        webViewInstance?.setRenderQuality(playerState.playerSettings.quality)
    }

    // Volume & Mute updates
    LaunchedEffect(playerState.playerSettings.isMuted, playerState.playerSettings.audioVolume, webViewInstance) {
        val vol = if (playerState.playerSettings.isMuted) 0f else playerState.playerSettings.audioVolume
        webViewInstance?.setVolume(vol)
    }

    // Mouse mode updates
    LaunchedEffect(playerState.playerSettings.touchMouseMode, webViewInstance) {
        webViewInstance?.setTouchMouseMode(playerState.playerSettings.touchMouseMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .testTag("flash_player_screen")
    ) {
        // Embedded Hardware-Accelerated WebView
        AndroidView(
            factory = { ctx ->
                FlashPlayerWebView(ctx).apply {
                    onPlayerLoadedCallback = { title, w, h, fps, frames ->
                        viewModel.onGameLoadedInEngine(title, w, h, fps, frames)
                    }
                    onPlayerErrorCallback = { err ->
                        viewModel.onEngineError(err)
                    }
                    onFpsUpdateCallback = { fps ->
                        viewModel.onFpsUpdated(fps)
                    }
                    setAspectRatio(playerState.aspectRatio)
                    setRenderQuality(playerState.playerSettings.quality)
                    webViewInstance = this
                }
            },
            update = { wv ->
                webViewInstance = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Virtual Mouse Trackpad Overlay (if enabled)
        if (playerState.playerSettings.touchMouseMode == TouchMouseMode.VIRTUAL_TRACKPAD) {
            VirtualMouseOverlay(
                onMouseMove = { x, y ->
                    webViewInstance?.sendMouseEvent("mousemove", x, y, 0)
                },
                onMouseDown = { x, y, btn ->
                    webViewInstance?.sendMouseEvent("mousedown", x, y, btn)
                },
                onMouseUp = { x, y, btn ->
                    webViewInstance?.sendMouseEvent("mouseup", x, y, btn)
                    webViewInstance?.sendMouseEvent("click", x, y, btn)
                }
            )
        }

        // Virtual Gamepad Controls Layer
        VirtualGamepadOverlay(
            config = playerState.gamepadConfig,
            modifier = Modifier.fillMaxSize(),
            onDirectionDown = { dir, code ->
                webViewInstance?.sendDirectionKeyDown(dir, code)
            },
            onDirectionUp = { dir, code ->
                webViewInstance?.sendDirectionKeyUp(dir, code)
            },
            onButtonDown = { binding ->
                webViewInstance?.sendKeyDown(binding)
            },
            onButtonUp = { binding ->
                webViewInstance?.sendKeyUp(binding)
            },
            onToggleTurbo = {
                viewModel.toggleTurbo()
            }
        )

        // Floating Quick Toolbar (Top)
        QuickToolbar(
            title = playerState.game?.title ?: "Flash Movie",
            fps = playerState.currentFps,
            aspectRatio = playerState.aspectRatio,
            gamepadConfig = playerState.gamepadConfig,
            touchMouseMode = playerState.playerSettings.touchMouseMode,
            isMuted = playerState.playerSettings.isMuted,
            onBack = {
                viewModel.closePlayer()
                onBack()
            },
            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
            onToggleGamepad = { viewModel.toggleGamepad() },
            onToggleMouseMode = { viewModel.toggleMouseMode() },
            onToggleMute = { viewModel.toggleMute() },
            onRestartGame = { webViewInstance?.restartGame() },
            onOpenSettings = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }

    // Player Settings Bottom Sheet in Bento Style
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = BentoSurfaceElevated,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Emulation & Control Options",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showSettingsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Aspect Ratio Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aspect Ratio Scaling", color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioMode.values().forEach { mode ->
                            val isSelected = playerState.aspectRatio == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BentoLilac else BentoSurfaceContainer,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BentoLilac else BentoBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.setAspectRatio(mode) }
                            ) {
                                Text(
                                    text = when (mode) {
                                        AspectRatioMode.CONTAIN -> "Fit"
                                        AspectRatioMode.RATIO_4_3 -> "4:3"
                                        AspectRatioMode.RATIO_16_9 -> "16:9"
                                        AspectRatioMode.STRETCH -> "Stretch"
                                    },
                                    color = if (isSelected) BentoLilacDark else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Control Type (D-Pad vs Joystick)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Left Controller Type", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Switch between 4-way D-Pad and Analog Joystick", color = TextSecondary, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = playerState.gamepadConfig.controlType == ControlType.DPAD,
                            onClick = {
                                viewModel.updateGamepadConfig(
                                    playerState.gamepadConfig.copy(controlType = ControlType.DPAD)
                                )
                            },
                            label = { Text("D-Pad") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoLilac,
                                selectedLabelColor = BentoLilacDark
                            )
                        )
                        FilterChip(
                            selected = playerState.gamepadConfig.controlType == ControlType.ANALOG_JOYSTICK,
                            onClick = {
                                viewModel.updateGamepadConfig(
                                    playerState.gamepadConfig.copy(controlType = ControlType.ANALOG_JOYSTICK)
                                )
                            },
                            label = { Text("Joystick") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoLilac,
                                selectedLabelColor = BentoLilacDark
                            )
                        )
                    }
                }

                // Gamepad Opacity Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gamepad Opacity", color = TextPrimary, fontSize = 13.sp)
                        Text("${(playerState.gamepadConfig.opacity * 100).toInt()}%", color = BentoLilac, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = playerState.gamepadConfig.opacity,
                        onValueChange = {
                            viewModel.updateGamepadConfig(playerState.gamepadConfig.copy(opacity = it))
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = BentoLilac,
                            activeTrackColor = BentoLilac,
                            inactiveTrackColor = BentoSurfaceContainer
                        )
                    )
                }

                // Gamepad Scale Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gamepad Size Scale", color = TextPrimary, fontSize = 13.sp)
                        Text(String.format("%.1fx", playerState.gamepadConfig.scale), color = BentoLilac, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = playerState.gamepadConfig.scale,
                        onValueChange = {
                            viewModel.updateGamepadConfig(playerState.gamepadConfig.copy(scale = it))
                        },
                        valueRange = 0.7f..1.3f,
                        colors = SliderDefaults.colors(
                            thumbColor = BentoLilac,
                            activeTrackColor = BentoLilac,
                            inactiveTrackColor = BentoSurfaceContainer
                        )
                    )
                }

                // Haptic Feedback & Key Mapping Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showSettingsSheet = false
                            showKeyMappingDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoSurfaceContainer)
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Key Map")
                    }

                    Button(
                        onClick = {
                            viewModel.updateGamepadConfig(
                                playerState.gamepadConfig.copy(hapticsEnabled = !playerState.gamepadConfig.hapticsEnabled)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (playerState.gamepadConfig.hapticsEnabled) BentoLilac.copy(alpha = 0.2f) else BentoSurfaceContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (playerState.gamepadConfig.hapticsEnabled) BentoLilac else BentoBorder
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (playerState.gamepadConfig.hapticsEnabled) BentoLilac else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.gamepadConfig.hapticsEnabled) "Haptics: ON" else "Haptics: OFF",
                            color = if (playerState.gamepadConfig.hapticsEnabled) BentoLilac else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Key Mapping Dialog
    if (showKeyMappingDialog) {
        KeyMappingDialog(
            currentConfig = playerState.gamepadConfig,
            onDismiss = { showKeyMappingDialog = false },
            onSave = { updated ->
                viewModel.updateGamepadConfig(updated)
            }
        )
    }
}
