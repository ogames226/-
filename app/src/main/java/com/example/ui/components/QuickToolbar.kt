package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioMode
import com.example.model.GamepadConfig
import com.example.model.TouchMouseMode
import com.example.ui.theme.*

@Composable
fun QuickToolbar(
    title: String,
    fps: Int,
    aspectRatio: AspectRatioMode,
    gamepadConfig: GamepadConfig,
    touchMouseMode: TouchMouseMode,
    isMuted: Boolean,
    onBack: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleGamepad: () -> Unit,
    onToggleMouseMode: () -> Unit,
    onToggleMute: () -> Unit,
    onRestartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Top Pill Bar in Bento Style
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BentoSurfaceHero,
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Game Title & FPS pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isExpanded = !isExpanded }
                        .background(BentoSurfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )

                    // FPS badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (fps >= 50) BentoGreenPulse.copy(alpha = 0.2f) else BentoLilac.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${fps} FPS",
                            color = if (fps >= 50) BentoGreenPulse else BentoLilac,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Menu",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Aspect Ratio Toggle
                ToolbarIconButton(
                    icon = Icons.Default.AspectRatio,
                    label = aspectRatio.name.replace("RATIO_", ""),
                    tint = BentoLilac,
                    onClick = onCycleAspectRatio
                )

                // Quick Gamepad Toggle
                ToolbarIconButton(
                    icon = if (gamepadConfig.isEnabled) Icons.Default.SportsEsports else Icons.Default.VideogameAssetOff,
                    label = if (gamepadConfig.isEnabled) "PAD ON" else "PAD OFF",
                    tint = if (gamepadConfig.isEnabled) BentoLilac else TextMuted,
                    onClick = onToggleGamepad
                )

                // Quick Mute Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFFF5449) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expand Menu Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expanded Control Tray in Bento Modular Grid
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BentoSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAYER CONTROLS",
                            color = BentoLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "RUFFLE WASM v0.1",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrayButton(
                            icon = Icons.Default.Refresh,
                            label = "Reload SWF",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onRestartGame()
                                isExpanded = false
                            }
                        )

                        TrayButton(
                            icon = if (touchMouseMode == TouchMouseMode.VIRTUAL_TRACKPAD) Icons.Default.Mouse else Icons.Default.TouchApp,
                            label = if (touchMouseMode == TouchMouseMode.VIRTUAL_TRACKPAD) "Mouse Mode" else "Direct Touch",
                            tint = if (touchMouseMode == TouchMouseMode.VIRTUAL_TRACKPAD) BentoLilac else TextSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = onToggleMouseMode
                        )

                        TrayButton(
                            icon = Icons.Default.Settings,
                            label = "Config",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onOpenSettings()
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BentoSurfaceContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = tint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrayButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BentoSurfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
