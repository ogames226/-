package com.example.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlType
import com.example.model.GamepadConfig
import com.example.model.GamepadTheme
import com.example.model.KeyBinding
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualGamepadOverlay(
    config: GamepadConfig,
    modifier: Modifier = Modifier,
    onDirectionDown: (String, Int) -> Unit,
    onDirectionUp: (String, Int) -> Unit,
    onButtonDown: (KeyBinding) -> Unit,
    onButtonUp: (KeyBinding) -> Unit,
    onToggleTurbo: () -> Unit = {}
) {
    if (!config.isEnabled) return

    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val triggerHaptic: () -> Unit = {
        if (config.hapticsEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(18)
                }
            } catch (_: Exception) {}
        }
    }

    // Force Left-to-Right layout so D-Pad is always on Left and Action Buttons on Right regardless of RTL system locale
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(config.opacity)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Left Controller (D-Pad or Analog Joystick)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp, start = 4.dp)
            ) {
                if (config.controlType == ControlType.DPAD) {
                    VirtualDPad(
                        scale = config.scale,
                        theme = config.theme,
                        onDirectionDown = { dir, code ->
                            triggerHaptic()
                            onDirectionDown(dir, code)
                        },
                        onDirectionUp = onDirectionUp
                    )
                } else {
                    VirtualAnalogJoystick(
                        scale = config.scale,
                        theme = config.theme,
                        onDirectionChange = { dir, isDown, code ->
                            if (isDown) {
                                triggerHaptic()
                                onDirectionDown(dir, code)
                            } else {
                                onDirectionUp(dir, code)
                            }
                        }
                    )
                }
            }

            // Center Utility Buttons (Space, Enter, Esc, Turbo)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityButton(
                    label = "ESC",
                    binding = config.buttonEsc,
                    theme = config.theme,
                    onDown = { triggerHaptic(); onButtonDown(config.buttonEsc) },
                    onUp = { onButtonUp(config.buttonEsc) }
                )
                UtilityButton(
                    label = "SPACE",
                    binding = config.buttonSpace,
                    theme = config.theme,
                    onDown = { triggerHaptic(); onButtonDown(config.buttonSpace) },
                    onUp = { onButtonUp(config.buttonSpace) }
                )
                UtilityButton(
                    label = "ENTER",
                    binding = config.buttonEnter,
                    theme = config.theme,
                    onDown = { triggerHaptic(); onButtonDown(config.buttonEnter) },
                    onUp = { onButtonUp(config.buttonEnter) }
                )
                // Turbo mode indicator button
                TurboButton(
                    isTurbo = config.turboEnabled,
                    theme = config.theme,
                    onClick = {
                        triggerHaptic()
                        onToggleTurbo()
                    }
                )
            }

            // Right Action Cluster (A, B, X, Y)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 4.dp)
            ) {
                ActionCluster(
                    scale = config.scale,
                    theme = config.theme,
                    turboActive = config.turboEnabled,
                    bindingA = config.buttonA,
                    bindingB = config.buttonB,
                    bindingX = config.buttonX,
                    bindingY = config.buttonY,
                    onButtonDown = { binding ->
                        triggerHaptic()
                        onButtonDown(binding)
                    },
                    onButtonUp = onButtonUp
                )
            }
        }
    }
}

@Composable
fun VirtualDPad(
    scale: Float,
    theme: GamepadTheme,
    onDirectionDown: (String, Int) -> Unit,
    onDirectionUp: (String, Int) -> Unit
) {
    val size = (160 * scale).dp
    val buttonSize = (52 * scale).dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = when (theme) {
                        GamepadTheme.NEO_CYBERPUNK -> listOf(Color(0x9900E5FF), Color(0x330F1523))
                        GamepadTheme.RETRO_ARCADE -> listOf(Color(0xCC1E293B), Color(0x990B0F19))
                        GamepadTheme.MINIMAL_TRANSPARENT -> listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))
                        GamepadTheme.CLASSIC_CONSOLE -> listOf(Color(0xDD374151), Color(0xAA1F2937))
                    }
                )
            )
            .border(
                width = 2.dp,
                color = when (theme) {
                    GamepadTheme.NEO_CYBERPUNK -> CyberCyan
                    GamepadTheme.RETRO_ARCADE -> FlashFlame.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Up
        DPadArrow(
            modifier = Modifier.align(Alignment.TopCenter),
            icon = Icons.Default.KeyboardArrowUp,
            size = buttonSize,
            theme = theme,
            onDown = { onDirectionDown("UP", 38) },
            onUp = { onDirectionUp("UP", 38) }
        )
        // Down
        DPadArrow(
            modifier = Modifier.align(Alignment.BottomCenter),
            icon = Icons.Default.KeyboardArrowDown,
            size = buttonSize,
            theme = theme,
            onDown = { onDirectionDown("DOWN", 40) },
            onUp = { onDirectionUp("DOWN", 40) }
        )
        // Left
        DPadArrow(
            modifier = Modifier.align(Alignment.CenterStart),
            icon = Icons.Default.KeyboardArrowLeft,
            size = buttonSize,
            theme = theme,
            onDown = { onDirectionDown("LEFT", 37) },
            onUp = { onDirectionUp("LEFT", 37) }
        )
        // Right
        DPadArrow(
            modifier = Modifier.align(Alignment.CenterEnd),
            icon = Icons.Default.KeyboardArrowRight,
            size = buttonSize,
            theme = theme,
            onDown = { onDirectionDown("RIGHT", 39) },
            onUp = { onDirectionUp("RIGHT", 39) }
        )

        // Center hub
        Box(
            modifier = Modifier
                .size((36 * scale).dp)
                .clip(CircleShape)
                .background(Color(0x55000000))
        )
    }
}

@Composable
fun DPadArrow(
    modifier: Modifier,
    icon: ImageVector,
    size: Dp,
    theme: GamepadTheme,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown()
                        tryAwaitRelease()
                        isPressed = false
                        onUp()
                    }
                )
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPressed) {
                    when (theme) {
                        GamepadTheme.NEO_CYBERPUNK -> CyberCyan.copy(alpha = 0.5f)
                        GamepadTheme.RETRO_ARCADE -> FlashFlame.copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.4f)
                    }
                } else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "DPad Direction",
            tint = if (isPressed) Color.White else DPadArrowColor,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

@Composable
fun VirtualAnalogJoystick(
    scale: Float,
    theme: GamepadTheme,
    onDirectionChange: (direction: String, isDown: Boolean, keyCode: Int) -> Unit
) {
    val baseRadius = (80 * scale).dp
    val thumbRadius = (32 * scale).dp

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var activeDirs by remember { mutableStateOf(setOf<String>()) }

    Box(
        modifier = Modifier
            .size(baseRadius * 2)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xCC1E293B), Color(0x990F1523))
                )
            )
            .border(
                width = 2.dp,
                color = if (theme == GamepadTheme.NEO_CYBERPUNK) CyberCyan else FlashFlame.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                val maxRadiusPx = (baseRadius.toPx() - thumbRadius.toPx())
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(baseRadius.toPx(), baseRadius.toPx())
                        val diff = offset - center
                        val dist = sqrt(diff.x * diff.x + diff.y * diff.y)
                        val clampedDist = dist.coerceAtMost(maxRadiusPx)
                        val angle = atan2(diff.y, diff.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        activeDirs = updateJoystickDirections(angle, dist > maxRadiusPx * 0.25f, activeDirs, onDirectionChange)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val clampedDist = dist.coerceAtMost(maxRadiusPx)
                        val angle = atan2(newOffset.y, newOffset.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        activeDirs = updateJoystickDirections(angle, dist > maxRadiusPx * 0.25f, activeDirs, onDirectionChange)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        activeDirs.forEach { dir ->
                            val code = when (dir) { "UP" -> 38; "DOWN" -> 40; "LEFT" -> 37; else -> 39 }
                            onDirectionChange(dir, false, code)
                        }
                        activeDirs = emptySet()
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        activeDirs.forEach { dir ->
                            val code = when (dir) { "UP" -> 38; "DOWN" -> 40; "LEFT" -> 37; else -> 39 }
                            onDirectionChange(dir, false, code)
                        }
                        activeDirs = emptySet()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Cross guides
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            drawLine(Color(0x33FFFFFF), Offset(c.x - size.width/3, c.y), Offset(c.x + size.width/3, c.y), strokeWidth = 1.5f)
            drawLine(Color(0x33FFFFFF), Offset(c.x, c.y - size.height/3), Offset(c.x, c.y + size.height/3), strokeWidth = 1.5f)
        }

        // Thumb nub
        Box(
            modifier = Modifier
                .offset(
                    x = (thumbOffset.x / 2.5f).dp,
                    y = (thumbOffset.y / 2.5f).dp
                )
                .size(thumbRadius * 2)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (theme == GamepadTheme.NEO_CYBERPUNK) CyberCyan else FlashFlame,
                            if (theme == GamepadTheme.NEO_CYBERPUNK) CyberCyanDark else FlashFlameDark
                        )
                    )
                )
                .shadow(6.dp, CircleShape)
        )
    }
}

private fun updateJoystickDirections(
    angle: Float,
    isEngaged: Boolean,
    currentDirs: Set<String>,
    onDirectionChange: (String, Boolean, Int) -> Unit
): Set<String> {
    if (!isEngaged) {
        currentDirs.forEach { dir ->
            val code = when (dir) { "UP" -> 38; "DOWN" -> 40; "LEFT" -> 37; else -> 39 }
            onDirectionChange(dir, false, code)
        }
        return emptySet()
    }

    val deg = Math.toDegrees(angle.toDouble()).let { if (it < 0) it + 360 else it }
    val newDirs = mutableSetOf<String>()

    // 8-way sector mapping
    if (deg in 337.5..360.0 || deg in 0.0..22.5) {
        newDirs.add("RIGHT")
    } else if (deg in 22.5..67.5) {
        newDirs.add("RIGHT"); newDirs.add("DOWN")
    } else if (deg in 67.5..112.5) {
        newDirs.add("DOWN")
    } else if (deg in 112.5..157.5) {
        newDirs.add("LEFT"); newDirs.add("DOWN")
    } else if (deg in 157.5..202.5) {
        newDirs.add("LEFT")
    } else if (deg in 202.5..247.5) {
        newDirs.add("LEFT"); newDirs.add("UP")
    } else if (deg in 247.5..292.5) {
        newDirs.add("UP")
    } else if (deg in 292.5..337.5) {
        newDirs.add("RIGHT"); newDirs.add("UP")
    }

    // Released
    (currentDirs - newDirs).forEach { dir ->
        val code = when (dir) { "UP" -> 38; "DOWN" -> 40; "LEFT" -> 37; else -> 39 }
        onDirectionChange(dir, false, code)
    }

    // Pressed
    (newDirs - currentDirs).forEach { dir ->
        val code = when (dir) { "UP" -> 38; "DOWN" -> 40; "LEFT" -> 37; else -> 39 }
        onDirectionChange(dir, true, code)
    }

    return newDirs
}

@Composable
fun ActionCluster(
    scale: Float,
    theme: GamepadTheme,
    turboActive: Boolean,
    bindingA: KeyBinding,
    bindingB: KeyBinding,
    bindingX: KeyBinding,
    bindingY: KeyBinding,
    onButtonDown: (KeyBinding) -> Unit,
    onButtonUp: (KeyBinding) -> Unit
) {
    val size = (160 * scale).dp
    val buttonSize = (50 * scale).dp

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Top (X button)
        ActionButton(
            modifier = Modifier.align(Alignment.TopCenter),
            label = bindingX.keyLabel,
            binding = bindingX,
            color = ButtonXColor,
            size = buttonSize,
            theme = theme,
            turbo = turboActive,
            onDown = onButtonDown,
            onUp = onButtonUp
        )
        // Bottom (A button)
        ActionButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            label = bindingA.keyLabel,
            binding = bindingA,
            color = ButtonAColor,
            size = buttonSize,
            theme = theme,
            turbo = turboActive,
            onDown = onButtonDown,
            onUp = onButtonUp
        )
        // Left (Y button)
        ActionButton(
            modifier = Modifier.align(Alignment.CenterStart),
            label = bindingY.keyLabel,
            binding = bindingY,
            color = ButtonYColor,
            size = buttonSize,
            theme = theme,
            turbo = turboActive,
            onDown = onButtonDown,
            onUp = onButtonUp
        )
        // Right (B button)
        ActionButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            label = bindingB.keyLabel,
            binding = bindingB,
            color = ButtonBColor,
            size = buttonSize,
            theme = theme,
            turbo = turboActive,
            onDown = onButtonDown,
            onUp = onButtonUp
        )
    }
}

@Composable
fun ActionButton(
    modifier: Modifier,
    label: String,
    binding: KeyBinding,
    color: Color,
    size: Dp,
    theme: GamepadTheme,
    turbo: Boolean,
    onDown: (KeyBinding) -> Unit,
    onUp: (KeyBinding) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed, turbo) {
        if (isPressed && turbo) {
            while (isActive && isPressed) {
                onDown(binding)
                delay(60)
                onUp(binding)
                delay(60)
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(turbo) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (!turbo) onDown(binding)
                        tryAwaitRelease()
                        isPressed = false
                        if (!turbo) onUp(binding)
                    }
                )
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(color, color.copy(alpha = 0.7f))
                    } else {
                        when (theme) {
                            GamepadTheme.NEO_CYBERPUNK -> listOf(color.copy(alpha = 0.4f), Color(0x99182033))
                            GamepadTheme.MINIMAL_TRANSPARENT -> listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))
                            else -> listOf(color.copy(alpha = 0.25f), Color(0xDD1E293B))
                        }
                    }
                )
            )
            .border(
                width = 2.dp,
                color = if (isPressed) Color.White else color.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .shadow(if (isPressed) 8.dp else 2.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun UtilityButton(
    label: String,
    binding: KeyBinding,
    theme: GamepadTheme,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(34.dp)
            .defaultMinSize(minWidth = 54.dp)
            .clip(RoundedCornerShape(17.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown()
                        tryAwaitRelease()
                        isPressed = false
                        onUp()
                    }
                )
            }
            .background(
                if (isPressed) FlashFlame.copy(alpha = 0.8f) else Color(0x991E293B)
            )
            .border(
                width = 1.dp,
                color = if (isPressed) FlashFlame else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(17.dp)
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TurboButton(
    isTurbo: Boolean,
    theme: GamepadTheme,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .background(
                if (isTurbo) CyberCyan.copy(alpha = 0.3f) else Color(0x661E293B)
            )
            .border(
                width = 1.dp,
                color = if (isTurbo) CyberCyan else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(17.dp)
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Turbo Mode",
                tint = if (isTurbo) CyberCyan else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "TURBO",
                color = if (isTurbo) CyberCyan else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
