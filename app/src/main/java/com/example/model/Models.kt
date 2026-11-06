package com.example.model

import androidx.compose.ui.graphics.Color

enum class TransitionType(val displayName: String) {
  NONE("None"),
  FADE_BLACK("Fade to Black"),
  FADE_WHITE("Fade to White"),
  DISSOLVE("Dissolve"),
  ZOOM_IN("Zoom In"),
  WIPE_LEFT("Wipe Left"),
  SLIDE_UP("Slide Up")
}

enum class AspectRatioOption(val label: String, val ratioFloat: Float, val widthRatio: Int, val heightRatio: Int) {
  RATIO_9_16("9:16 (TikTok/Reels)", 9f / 16f, 9, 16),
  RATIO_16_9("16:9 (YouTube)", 16f / 9f, 16, 9),
  RATIO_1_1("1:1 (Square)", 1f, 1, 1),
  RATIO_4_3("4:3", 4f / 3f, 4, 3),
  RATIO_3_4("3:4", 3f / 4f, 3, 4),
  RATIO_4_5("4:5 (Portrait)", 4f / 5f, 4, 5)
}

enum class MainTool(val id: String, val title: String) {
  EDIT("edit", "Edit"),
  AUDIO("audio", "Audio"),
  TEXT("text", "Text"),
  FILTERS("filters", "Filters"),
  ADJUST("adjust", "Adjust"),
  RATIO("ratio", "Ratio"),
  EFFECTS("effects", "Effects"),
  CANVAS("canvas", "Canvas")
}

enum class SubMenuType {
  NONE,
  CLIP_OPTIONS,
  SPEED,
  VOLUME,
  FILTER,
  ADJUST,
  TRANSITION,
  VOICE_EFFECT,
  AUDIO_OPTIONS
}

data class VideoClip(
  val id: String,
  val title: String,
  val startMs: Long = 0L,
  val endMs: Long,
  val originalDurationMs: Long,
  val thumbnailRes: Int = 0,
  val uriString: String? = null,
  val isVideo: Boolean = true,
  val speed: Float = 1.0f,
  val volume: Float = 1.0f,
  val transitionAfter: TransitionType = TransitionType.NONE,
  val transitionDurationMs: Long = 500L,
  val voiceEffect: String = "Normal",
  val isFrozen: Boolean = false,
  val isReversed: Boolean = false,
  val isSelected: Boolean = false,
  val rotationDegrees: Float = 0f,
  val isMirrored: Boolean = false,
  val cropFraction: Float = 0f,
  val speedPoints: List<SpeedPoint> = emptyList(),
  val transformKeyframes: List<TransformKeyframe> = emptyList(),
  val chromaKey: ChromaKey? = null,
  val videoDenoise: Float = 0f,
  val fadeInMs: Long = 0L,
  val fadeOutMs: Long = 0L,
  val isMuted: Boolean = false
) {
  val durationMs: Long
    get() = (endMs - startMs).coerceAtLeast(100L)

  val timelineDurationMs: Long = speedSegments().sumOf { it.timelineMs }.toLong().coerceAtLeast(1L)
}

data class AudioTrackItem(
  val id: String,
  val title: String,
  val startMs: Long,
  val endMs: Long,
  val volume: Float = 1.0f,
  val isMuted: Boolean = false,
  val category: String = "BGM",
  val uriString: String? = null,
  val sourceDurationMs: Long = 0L,
  val sourceOffsetMs: Long = 0L
) {
  val durationMs: Long
    get() = (endMs - startMs).coerceAtLeast(100L)
}

data class TextTrackItem(
  val id: String,
  val text: String,
  val startMs: Long,
  val endMs: Long,
  val style: String = "Default",
  val colorHex: String = "#FFFFFF",
  val fontSizeSp: Float = 24f,
  val posX: Float = 0.5f,
  val posY: Float = 0.7f,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val fontFamily: String = "sans-serif",
  val bold: Boolean = true,
  val italic: Boolean = false,
  val alignment: String = "center",
  val opacity: Float = 1f,
  val strokeHex: String = "#000000",
  val strokeWidth: Float = 0f,
  val shadow: Boolean = true,
  val backgroundHex: String = "#00000000",
  val letterSpacing: Float = 0f,
  val lineSpacing: Float = 1.05f,
  val entrance: String = "None",
  val exit: String = "None",
  val loop: String = "None",
  val animationDurationMs: Long = 400L,
  val isSticker: Boolean = false,
  val photoOverlayPath: String? = null
) {
  val durationMs: Long
    get() = (endMs - startMs).coerceAtLeast(100L)
}

data class FilterItem(
  val id: String,
  val name: String,
  val filterFactor: Float = 1.0f,
  val colorTone: Long = 0x00000000,
  val brightnessMultiplier: Float = 1.0f,
  val contrastMultiplier: Float = 1.0f,
  val saturationMultiplier: Float = 1.0f
)

data class AdjustSettings(
  val brightness: Float = 0f,   // -100 to 100
  val contrast: Float = 0f,     // -100 to 100
  val saturation: Float = 0f,   // -100 to 100
  val sharpness: Float = 0f,    // 0 to 100
  val vignette: Float = 0f,     // 0 to 100
  val warmth: Float = 0f        // -100 to 100
)

data class ResolutionSetting(
  val label: String = "1080P",
  val fps: Int = 30,
  val smartHdr: Boolean = false,
  val estimatedSizeMb: Float = 28.5f
)
