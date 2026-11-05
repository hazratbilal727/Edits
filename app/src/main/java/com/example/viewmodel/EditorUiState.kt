package com.example.viewmodel

import com.example.model.AdjustSettings
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackItem
import com.example.model.FilterItem
import com.example.model.ResolutionSetting
import com.example.model.SubMenuType
import com.example.model.TextTrackItem
import com.example.model.VideoClip

val DEFAULT_FILTER = FilterItem("original", "Original", 1f, 0x00000000)

val AVAILABLE_FILTERS = listOf(
  DEFAULT_FILTER,
  FilterItem("cine", "Cinema", 1f, 0x220055AA, 1.05f, 1.2f, 1.1f),
  FilterItem("vintage", "Vintage", 1f, 0x33BB7733, 0.95f, 0.9f, 0.8f),
  FilterItem("noir", "B&W Noir", 1f, 0x00000000, 1.0f, 1.4f, 0.0f),
  FilterItem("teal_orange", "Teal/Orange", 1f, 0x22FF6600, 1.1f, 1.25f, 1.2f),
  FilterItem("sunset", "Warm Sunset", 1f, 0x33FFAA00, 1.1f, 1.05f, 1.3f),
  FilterItem("emerald", "Emerald", 1f, 0x2200AA55, 1.0f, 1.1f, 1.15f)
)

data class EditorUiState(
  val currentProjectId: String? = null,
  val projectTitle: String = "New Project",
  val currentTimeMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val isPlaying: Boolean = false,
  val isMuted: Boolean = false,
  val activeSubMenu: SubMenuType = SubMenuType.NONE,
  val selectedClipId: String? = null,
  val clips: List<VideoClip> = emptyList(),
  val audioTracks: List<AudioTrackItem> = emptyList(),
  val textTracks: List<TextTrackItem> = emptyList(),
  val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
  val selectedFilter: FilterItem = DEFAULT_FILTER,
  val filterIntensity: Float = 1.0f,
  val adjustSettings: AdjustSettings = AdjustSettings(),
  val resolutionSetting: ResolutionSetting = ResolutionSetting(),
  val showResolutionSheet: Boolean = false,
  val showExportDialog: Boolean = false,
  val exportedFile: String? = null,
  val exportProgress: Float = 0f,
  val isExportFinished: Boolean = false,
  val showAddTextDialog: Boolean = false,
  val showAddAudioDialog: Boolean = false,
  val showTransitionDialog: Boolean = false,
  val selectedTransitionClipId: String? = null,
  val showVoiceChangerDialog: Boolean = false,
  val showSfxDialog: Boolean = false,
  val keyframes: Set<Long> = emptySet(),
  val isFullscreen: Boolean = false,
  val pipOverlayEnabled: Boolean = false,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val toastMessage: String? = null
) {
  val selectedClip: VideoClip?
    get() = clips.firstOrNull { it.id == selectedClipId }

  val currentClipAtPlayhead: VideoClip?
    get() {
      var accumulated = 0L
      for (clip in clips) {
        val duration = clip.timelineDurationMs
        if (currentTimeMs >= accumulated && currentTimeMs < accumulated + duration) {
          return clip
        }
        accumulated += duration
      }
      return clips.lastOrNull()
    }
}
