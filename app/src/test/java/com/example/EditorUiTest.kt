package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.model.VideoClip
import com.example.ui.screens.CapCutEditorScreen
import com.example.ui.screens.CapCutHomeScreen
import com.example.ui.theme.CapCutTheme
import com.example.viewmodel.VideoEditorViewModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w900dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorUiTest {
  @get:Rule val compose = createComposeRule()

  private fun editor(width: Int, height: Int, fontScale: Float = 1f): VideoEditorViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val model = VideoEditorViewModel(app)
    model.loadClips(listOf(VideoClip(
      "photo", "Mountain getaway", endMs = 6000, originalDurationMs = 6000,
      isVideo = false, uriString = "android.resource://${app.packageName}/${R.drawable.img_nature_mountains}"
    )))
    compose.setContent {
      CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
        CapCutTheme {
          Box(Modifier.requiredSize(width.dp, height.dp)) {
            CapCutEditorScreen(model, {}, {})
          }
        }
      }
    }
    compose.waitForIdle()
    return model
  }

  private fun capture(name: String) {
    val target = File("build/ui-previews/$name.png")
    target.parentFile.mkdirs()
    target.outputStream().use {
      compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    }
  }

  @Test fun portraitEditorAndCrop() {
    val model = editor(360, 800)
    compose.onNodeWithTag("video_preview_canvas").assertIsDisplayed()
    compose.onNodeWithTag("timeline_clip_photo").performClick()
    compose.onNodeWithText("Crop", useUnmergedTree = true).performClick()
    compose.onNodeWithText("Apply").performClick()
    assertEquals(0f, model.uiState.value.clips.single().cropFraction)
    capture("editor-portrait")
  }

  @Test fun smallPhoneWithLargeText() {
    editor(320, 640, 1.3f)
    compose.onNodeWithTag("export_button").assertIsDisplayed()
    compose.onNodeWithTag("timeline_play_pause_button").assertIsDisplayed()
    capture("editor-small-large-text")
  }

  @Test fun landscapeEditor() {
    editor(800, 400)
    compose.onNodeWithTag("video_preview_canvas").assertIsDisplayed()
    compose.onNodeWithTag("timeline_play_pause_button").assertIsDisplayed()
    capture("editor-landscape")
  }

  @Test fun homeStartsMediaImport() {
    var opened = false
    compose.setContent {
      CapCutTheme {
        Box(Modifier.requiredSize(360.dp, 800.dp)) {
          CapCutHomeScreen({ opened = true }, {}, {})
        }
      }
    }
    compose.onNodeWithText("Start a new edit").performClick()
    assertEquals(true, opened)
    capture("home")
  }
}
