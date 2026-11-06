package com.example

import android.app.Application
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
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
import org.junit.Assert.assertTrue
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
    compose.mainClock.advanceTimeBy(500)
    compose.waitForIdle()
    val target = File("build/ui-previews/$name.png")
    target.parentFile.mkdirs()
    compose.onNodeWithTag(when { name == "home" -> "home_screen"; name.startsWith("timeline-") -> "mobile_timeline"; else -> "editor_screen" }).captureRoboImage(target.absolutePath)
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

  @Test fun textCanBeStyledAndMovedWithTouch() {
    val model = editor(360, 800)
    compose.runOnIdle { model.addTextTrack("Touch title") }
    compose.waitForIdle()
    capture("editor-text-before-drag")
    val initial = model.uiState.value.textTracks.single()
    compose.onNodeWithTag("text_layer_canvas", useUnmergedTree = true).performTouchInput {
      val start = androidx.compose.ui.geometry.Offset(width * initial.posX, height * initial.posY)
      swipe(start, start + androidx.compose.ui.geometry.Offset(20f, -20f), 350)
    }
    compose.runOnIdle {
      val moved = model.uiState.value.textTracks.single()
      assertTrue("Drag should move X from ${initial.posX} to ${moved.posX}", moved.posX > initial.posX)
      assertTrue("Drag should move Y from ${initial.posY} to ${moved.posY}", moved.posY < initial.posY)
      model.updateTextLayer(moved.copy(colorHex = "#00E5FF", rotation = 12f, backgroundHex = "#BB000000", entrance = "Fade"))
    }
    val styled = compose.runOnIdle { model.uiState.value.textTracks.single() }
    compose.onNodeWithTag("text_layer_canvas", useUnmergedTree = true).performTouchInput {
      val center = androidx.compose.ui.geometry.Offset(width * styled.posX, height * styled.posY)
      down(0, center + androidx.compose.ui.geometry.Offset(-15f, 0f))
      down(1, center + androidx.compose.ui.geometry.Offset(15f, 0f))
      moveTo(0, center + androidx.compose.ui.geometry.Offset(-22f, -10f))
      moveTo(1, center + androidx.compose.ui.geometry.Offset(22f, 10f))
      up(0)
      up(1)
    }
    compose.runOnIdle {
      assertTrue(model.uiState.value.textTracks.single().scale > styled.scale)
      assertTrue(kotlin.math.abs(model.uiState.value.textTracks.single().rotation - styled.rotation) > 1f)
    }
    compose.onNodeWithTag("text_done").assertIsDisplayed()
    capture("editor-text-tools")
  }

  @Test fun timelineAddsTextMutesClipAndScrubs() {
    val model = editor(360, 800)
    compose.onNodeWithTag("timeline_append_media_button").assertIsDisplayed()
    compose.onNodeWithTag("timeline_add_audio", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithTag("timeline_add_text", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithTag("timeline_cover", useUnmergedTree = true).performClick()
    compose.onNodeWithText("Use this frame").performClick()
    compose.waitUntil(5000) { model.uiState.value.coverUri != null }
    compose.onNodeWithTag("timeline_clip_mute", useUnmergedTree = true).performClick()
    compose.runOnIdle { assertTrue(model.uiState.value.clips.single().isMuted) }
    capture("timeline-reference")
    compose.onNodeWithTag("timeline_scrub_surface", useUnmergedTree = true).performTouchInput {
      swipe(androidx.compose.ui.geometry.Offset(width * 0.75f, height - 4f),
        androidx.compose.ui.geometry.Offset(width * 0.25f, height - 4f), 400)
    }
    compose.runOnIdle { assertTrue(model.uiState.value.currentTimeMs > 0L) }
    compose.onNodeWithTag("timeline_add_text", useUnmergedTree = true).performClick()
    compose.runOnIdle { assertEquals(1, model.uiState.value.textTracks.size) }
  }

  @Test fun timelineStacksTextAndAddsStickerInItsOwnLane() {
    val model = editor(360, 800)
    compose.runOnIdle {
      model.addTextTrack("First")
      model.addTextTrack("Second")
      model.finishTextEditing()
    }
    val text = model.uiState.value.textTracks
    val first = compose.onNodeWithTag("timeline_track_${text[0].id}", useUnmergedTree = true)
    val second = compose.onNodeWithTag("timeline_track_${text[1].id}", useUnmergedTree = true)
    first.performScrollTo()
    second.performScrollTo()
    assertTrue(second.fetchSemanticsNode().positionInRoot.y > first.fetchSemanticsNode().positionInRoot.y)
    compose.onNodeWithTag("timeline_add_sticker", useUnmergedTree = true).performScrollTo().performClick()
    compose.onNodeWithText("🔥").performClick()
    compose.runOnIdle {
      assertEquals("🔥", model.uiState.value.textTracks.single { it.isSticker }.text)
      model.finishTextEditing()
    }
    compose.onNodeWithTag("timeline_project_filter", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    capture("timeline-layer-tools")
  }

  @Test fun timelinePinchZoomKeepsThePlayheadTime() {
    val model = editor(360, 800)
    val surface = compose.onNodeWithTag("timeline_scrub_surface", useUnmergedTree = true)
    val before = surface.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
      .filter(Char::isDigit).toInt()
    surface.performTouchInput {
      val y = height - 3f
      down(0, androidx.compose.ui.geometry.Offset(width * 0.4f, y))
      down(1, androidx.compose.ui.geometry.Offset(width * 0.6f, y))
      moveTo(0, androidx.compose.ui.geometry.Offset(width * 0.25f, y))
      moveTo(1, androidx.compose.ui.geometry.Offset(width * 0.75f, y))
      up(0)
      up(1)
    }
    val after = surface.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
      .filter(Char::isDigit).toInt()
    assertTrue(after > before)
    compose.runOnIdle { assertEquals(0L, model.uiState.value.currentTimeMs) }
  }

  @Test fun timelineHandleTrimsRealClipDuration() {
    val model = editor(360, 800)
    compose.onNodeWithTag("timeline_clip_photo").performClick()
    compose.onNodeWithTag("timeline_trim_start", useUnmergedTree = true).performTouchInput {
      swipe(center, center + androidx.compose.ui.geometry.Offset(50f, 0f), 400)
    }
    compose.runOnIdle {
      assertTrue(model.uiState.value.clips.single().startMs > 0)
      assertTrue(model.uiState.value.totalDurationMs < 6000)
    }
  }

  @Test fun longTimelineOnlyMeasuresVisibleMedia() {
    val model = editor(360, 800)
    compose.runOnIdle {
      val clip = model.uiState.value.clips.single().copy(endMs = 86400000L, originalDurationMs = 86400000L)
      model.loadClips(listOf(clip))
      model.seekTo(43200000L)
    }
    compose.onNodeWithTag("timeline_clip_photo").assertIsDisplayed()
    compose.onNodeWithTag("timeline_play_pause_button").assertIsDisplayed()
  }

  @Test fun homeSettingsOpensVersionDetails() {
    compose.setContent {
      CapCutTheme {
        Box(Modifier.requiredSize(360.dp, 800.dp)) {
          CapCutHomeScreen({}, {}, {})
        }
      }
    }
    compose.onNodeWithText("Settings").performClick()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.onNodeWithText("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})").assertIsDisplayed()
    compose.onNodeWithText("Done").performClick()
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
