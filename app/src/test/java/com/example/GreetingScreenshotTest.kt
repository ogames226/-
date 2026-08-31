package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.FlashGame
import com.example.ui.components.GameCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun gameCard_screenshot() {
    val sampleGame = FlashGame(
      id = 1,
      title = "Cyber Space Blitz",
      description = "Classic Flash Arcade shooter with vector aesthetics",
      fileUri = "",
      fileName = "space_blitz.swf",
      fileType = "SWF",
      isFavorite = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        GameCard(
          game = sampleGame,
          onClick = {},
          onToggleFavorite = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/game_card.png")
  }
}
