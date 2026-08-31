package com.example.model

enum class AspectRatioMode(val displayName: String, val cssClass: String) {
    CONTAIN("Original Fit (4:3 / 16:9)", "aspect-fit"),
    RATIO_4_3("Classic 4:3", "aspect-4-3"),
    RATIO_16_9("Widescreen 16:9", "aspect-16-9"),
    STRETCH("Full Stretch", "aspect-stretch")
}

enum class RenderQuality(val label: String, val value: String) {
    LOW("Low (Fastest)", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High (Standard)", "high"),
    BEST("Best (Crisp)", "best")
}

enum class ControlType {
    DPAD,
    ANALOG_JOYSTICK
}

enum class TouchMouseMode {
    DIRECT_TOUCH,      // Direct screen touch simulates mouse clicks & drags
    VIRTUAL_TRACKPAD   // Screen acts as a trackpad with visible cursor and L/R buttons
}

enum class GamepadTheme(val displayName: String) {
    RETRO_ARCADE("Retro Arcade"),
    NEO_CYBERPUNK("Neo Cyberpunk"),
    CLASSIC_CONSOLE("Classic Console"),
    MINIMAL_TRANSPARENT("Minimal Glass")
}

data class KeyBinding(
    val actionName: String,
    val keyLabel: String,
    val jsKey: String,
    val jsCode: String,
    val jsKeyCode: Int
)

data class GamepadConfig(
    val isEnabled: Boolean = true,
    val opacity: Float = 0.75f,
    val scale: Float = 1.0f,
    val controlType: ControlType = ControlType.DPAD,
    val hapticsEnabled: Boolean = true,
    val theme: GamepadTheme = GamepadTheme.RETRO_ARCADE,
    val buttonA: KeyBinding = KeyBinding("Action A / Jump", "A", "z", "KeyZ", 90),
    val buttonB: KeyBinding = KeyBinding("Action B / Attack", "B", "x", "KeyX", 88),
    val buttonX: KeyBinding = KeyBinding("Action X / Special", "X", "c", "KeyC", 67),
    val buttonY: KeyBinding = KeyBinding("Action Y / Secondary", "Y", "v", "KeyV", 86),
    val buttonSpace: KeyBinding = KeyBinding("Space / Fire", "SPACE", " ", "Space", 32),
    val buttonEnter: KeyBinding = KeyBinding("Enter / Start", "ENTER", "Enter", "Enter", 13),
    val buttonEsc: KeyBinding = KeyBinding("Esc / Menu", "ESC", "Escape", "Escape", 27),
    val turboEnabled: Boolean = false
)

data class PlayerSettings(
    val quality: RenderQuality = RenderQuality.HIGH,
    val aspectRatio: AspectRatioMode = AspectRatioMode.CONTAIN,
    val letterbox: Boolean = true,
    val touchMouseMode: TouchMouseMode = TouchMouseMode.DIRECT_TOUCH,
    val enableHardwareAcceleration: Boolean = true,
    val audioVolume: Float = 1.0f,
    val isMuted: Boolean = false,
    val targetFps: Int = 60,
    val showFpsCounter: Boolean = true,
    val showTouchRipple: Boolean = true
)

data class FlashGame(
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
    val preferredAspectRatio: AspectRatioMode = AspectRatioMode.CONTAIN,
    val preferredGamepad: GamepadConfig = GamepadConfig(),
    val thumbnailUri: String? = null,
    val isBuiltIn: Boolean = false,
    val builtInSampleKey: String? = null
)
