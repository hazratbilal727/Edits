package com.example

import com.example.data.ProjectCodec
import com.example.data.ProjectContent
import com.example.model.*
import com.example.viewmodel.EditorUiState
import org.junit.Assert.*
import org.junit.Test

class ProjectContentTest {
  @Test fun restoresAllEditableTracksAndClipSettings() {
    val clip = VideoClip("clip", "Video", endMs = 5000, originalDurationMs = 5000,
      uriString = "content://media/external/video/1", speedPoints = listOf(SpeedPoint(2000, 0.5f)),
      transformKeyframes = listOf(TransformKeyframe(0), TransformKeyframe(4000, 2f)),
      chromaKey = ChromaKey(), fadeOutMs = 300)
    val state = EditorUiState(
      currentProjectId = "saved", projectTitle = "A saved edit", clips = listOf(clip),
      textTracks = listOf(TextTrackItem("caption", "Caption", 500, 2500)),
      audioTracks = listOf(AudioTrackItem("audio", "Music", 0, 4000, uriString = "file:///music.wav", sourceDurationMs = 4000)),
      aspectRatio = AspectRatioOption.RATIO_16_9, adjustSettings = AdjustSettings(contrast = 20f)
    )
    val content = requireNotNull(ProjectContent.from(state))
    val restored = ProjectCodec.decode(ProjectCodec.encode(content)).editorState()
    assertEquals(state.clips, restored.clips)
    assertEquals(state.audioTracks, restored.audioTracks)
    assertEquals(state.textTracks, restored.textTracks)
    assertEquals(state.adjustSettings, restored.adjustSettings)
    assertEquals(state.aspectRatio, restored.aspectRatio)
    assertEquals(8000L, restored.totalDurationMs)
    assertFalse(restored.isPlaying)
  }
}
