package com.example.data

import com.example.model.*
import com.example.viewmodel.EditorUiState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ProjectContent(
  val id: String,
  val title: String,
  val clips: List<VideoClip>,
  val audioTracks: List<AudioTrackItem>,
  val textTracks: List<TextTrackItem>,
  val aspectRatio: AspectRatioOption,
  val filter: FilterItem,
  val filterIntensity: Float,
  val adjustments: AdjustSettings,
  val resolution: ResolutionSetting,
  val coverUri: String? = null,
  val isMuted: Boolean = false
) {
  fun editorState() = EditorUiState(
    currentProjectId = id, projectTitle = title, clips = clips, coverUri = coverUri, isMuted = isMuted,
    audioTracks = audioTracks, textTracks = textTracks, aspectRatio = aspectRatio,
    selectedFilter = filter, filterIntensity = filterIntensity,
    adjustSettings = adjustments, resolutionSetting = resolution,
    totalDurationMs = clips.sumOf { it.timelineDurationMs }, selectedClipId = clips.firstOrNull()?.id
  )

  companion object {
    fun from(state: EditorUiState): ProjectContent? {
      val id = state.currentProjectId ?: return null
      return ProjectContent(
        id, state.projectTitle, state.clips.map { it.copy(isSelected = false) },
        state.audioTracks, state.textTracks, state.aspectRatio,
        state.selectedFilter, state.filterIntensity, state.adjustSettings, state.resolutionSetting, state.coverUri, state.isMuted
      )
    }
  }
}

object ProjectCodec {
  private val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(ProjectContent::class.java)
  fun encode(content: ProjectContent): String = adapter.toJson(content)
  fun decode(json: String): ProjectContent = requireNotNull(adapter.fromJson(json))
}
