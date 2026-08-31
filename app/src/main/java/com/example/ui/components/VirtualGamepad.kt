package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.ControlType
import com.example.model.GamepadConfig
import com.example.model.GamepadTheme
import com.example.model.KeyBinding

/**
 * Docked responsive Virtual Gamepad overlay for Flash Games.
 * Left Zone: D-Pad / Analog Joystick (anchored to bottom-left).
 * Right Zone: Action Buttons A, B, X, Y (anchored to bottom-right).
 * Center Zone: Utility Buttons (ESC, SPACE, ENTER, TURBO).
 */
@Composable
fun VirtualGamepad(
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Left Zone: D-Pad or Analog Joystick (anchored to bottom-left)
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

            // Center Zone: Utility Buttons (ESC, SPACE, ENTER, TURBO)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
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
                TurboButton(
                    isTurbo = config.turboEnabled,
                    theme = config.theme,
                    onClick = {
                        triggerHaptic()
                        onToggleTurbo()
                    }
                )
            }

            // Right Zone: Action Buttons (A, B, X, Y) (anchored to bottom-right)
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
