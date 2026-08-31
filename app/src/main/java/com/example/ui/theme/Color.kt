package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bento Grid Palette Tokens
val BentoCanvas = Color(0xFF1C1B1F)           // #1C1B1F Dark Canvas / Scaffold background
val BentoSurfaceHero = Color(0xFF332D41)       // #332D41 Hero Bento Card background
val BentoSurfaceContainer = Color(0xFF49454F)  // #49454F Secondary Bento Card container
val BentoSurfaceElevated = Color(0xFF2B2930)   // #2B2930 Bottom bar & container surface
val BentoSurfaceCard = Color(0xFF25232A)       // #25232A Game item card background

// Bento Accent & Theme Highlights
val BentoLilac = Color(0xFFD0BCFF)             // #D0BCFF Primary Accent Lilac
val BentoLilacLight = Color(0xFFE8DEF8)        // #E8DEF8 Pill indicator / Container light
val BentoLilacDark = Color(0xFF381E72)         // #381E72 Deep Purple for high contrast text on Lilac
val BentoOnDarkLilac = Color(0xFF1D192B)       // #1D192B Text on light pill container

// Status & Accents
val BentoGreenPulse = Color(0xFF4ADE80)        // #4ADE80 Green-400 Now Playing / Live pulse
val BentoBorder = Color(0xFF49454F)            // #49454F Bento card outline
val BentoBorderLight = Color(0xFF605D66)       // #605D66 Active border

// Legacy compatibility aliases for smooth UI integration
val FlashFlame = BentoLilac
val FlashFlameLight = BentoLilacLight
val FlashFlameDark = BentoLilacDark
val FlashAmber = Color(0xFFFFD54F)
val FlashYellow = Color(0xFFFFE082)

val CyberCyan = BentoLilac
val CyberCyanDark = BentoLilacDark
val CyberNeonGreen = BentoGreenPulse
val CyberPurple = BentoSurfaceHero

// Text hierarchy
val TextPrimary = Color(0xFFE6E1E5)            // #E6E1E5 Primary readable text
val TextSecondary = Color(0xFFCCC4D1)          // #CCC4D1 Secondary text
val TextMuted = Color(0xFF938F99)              // #938F99 Subdued metadata / labels
val TextInactive = Color(0xFFCAC4D0)           // #CAC4D0 Navigation icons / inactive

// Dark Surfaces
val DarkCanvas = BentoCanvas
val DarkSurface1 = BentoSurfaceHero
val DarkSurface2 = BentoSurfaceContainer
val DarkSurface3 = BentoSurfaceElevated
val DarkSurfaceElevated = BentoSurfaceElevated
val DarkBorder = BentoBorder
val DarkBorderGlow = Color(0x66D0BCFF)

// Gamepad Button Colors tuned for Bento Grid
val ButtonAColor = BentoGreenPulse
val ButtonBColor = Color(0xFFFF5449)
val ButtonXColor = BentoLilac
val ButtonYColor = Color(0xFFFFD54F)
val DPadColor = Color(0xFF2B2930)
val DPadArrowColor = BentoLilac
val JoystickBaseColor = Color(0x66332D41)
val JoystickThumbColor = BentoLilac
