package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BuiltInSampleGames
import com.example.model.FlashExeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flash Player", appName)
  }

  @Test
  fun `built-in sample swf files exist and are valid`() {
    val samples = BuiltInSampleGames.SAMPLE_GAMES
    assertTrue(samples.isNotEmpty())

    samples.forEach { sample ->
      val bytes = BuiltInSampleGames.getSampleSwfBytes(sample.builtInSampleKey ?: "")
      assertTrue("SWF bytes should not be empty for ${sample.title}", bytes.isNotEmpty())
      val isSwf = FlashExeParser.isDirectSwf(bytes)
      assertTrue("Header signature must be FWS/CWS/ZWS for ${sample.title}", isSwf)
    }
  }

  @Test
  fun `swf parser extracts header metadata correctly`() {
    val bytes = BuiltInSampleGames.getSampleSwfBytes("sample_space_blitz")
    val result = FlashExeParser.extractSwfFromBytes(bytes)
    assertTrue(result.isSuccess)
    assertNotNull(result.swfBytes)
    assertEquals(9, result.flashVersion)
    assertEquals("FWS", result.compressionType)
  }
}
