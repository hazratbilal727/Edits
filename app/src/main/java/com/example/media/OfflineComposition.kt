package com.example.media

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import com.example.model.TextTrackItem
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.C
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import com.example.model.fadeLevelAt
import android.graphics.Matrix
import com.example.model.speedSegments
import com.example.model.transformAt
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.viewmodel.EditorUiState
import kotlin.math.roundToInt

@UnstableApi
object OfflineComposition {
  fun create(state: EditorUiState): Composition {
    require(state.clips.isNotEmpty()) { "Add a photo or video before exporting." }
    require(state.clips.none { it.isReversed || it.isFrozen }) {
      "Export requires clips without legacy reverse or freeze flags."
    }
    require(state.clips.none {
      it.transitionAfter != com.example.model.TransitionType.NONE || it.voiceEffect != "Normal"
    }) { "Remove unsupported transitions and voice effects before exporting." }
    require(state.isMuted || state.audioTracks.none { it.uriString == null && !it.isMuted && it.volume > 0f }) {
      "Remove demo audio tracks before exporting. Import a local audio file to include music."
    }

    val items = state.clips.flatMap { clip ->
      val segments = clip.speedSegments()
      val starts = segments.runningFold(0.0) { time, segment -> time + segment.timelineMs }
      segments.mapIndexed { index, segment ->
        val source = requireNotNull(clip.uriString) { "Import local media before exporting." }
        val media = MediaItem.Builder().setUri(Uri.parse(source))
        if (clip.isVideo) {
          media.setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
              .setStartPositionMs(segment.startMs)
              .setEndPositionMs(segment.endMs)
              .build()
          )
        } else {
          media.setImageDurationMs(clip.durationMs)
        }

        val effects = mutableListOf<Effect>()
        clip.cleanupEffect()?.let(effects::add)
        if (clip.cropFraction > 0f) {
          val edge = 1f - clip.cropFraction
          effects.add(Crop(-edge, edge, -edge, edge))
        }
        if (clip.rotationDegrees != 0f || clip.isMirrored) {
          effects.add(ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(-clip.rotationDegrees)
            .setScale(if (clip.isMirrored) -1f else 1f, 1f)
            .build())
        }
        effects.addAll(colorEffects(state))
        val ratio = state.aspectRatio.ratioFloat
        val shortEdge = when (state.resolutionSetting.label) {
          "720P" -> 720
          "4K" -> 2160
          else -> 1080
        }
        val width = if (ratio >= 1f) (shortEdge * ratio / 2).roundToInt() * 2 else shortEdge
        val height = if (ratio >= 1f) shortEdge else (shortEdge / ratio / 2).roundToInt() * 2
        effects.add(Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT))

        if (clip.transformKeyframes.isNotEmpty()) {
          effects.add(MatrixTransformation { timeUs ->
            val sourceMs = segment.startMs + (timeUs * segment.rate / 1000.0).toLong()
            val keyframe = clip.transformAt(sourceMs.coerceIn(segment.startMs, segment.endMs))
            Matrix().apply {
              setScale(keyframe.scale, keyframe.scale)
              postTranslate(keyframe.x * 2f, -keyframe.y * 2f)
            }
          })
        }

        if (clip.fadeInMs > 0 || clip.fadeOutMs > 0) {
          effects.add(RgbMatrix { timeUs, _ ->
            val gain = clip.fadeLevelAt((starts[index] + timeUs / 1000.0).toLong())
            floatArrayOf(
              gain, 0f, 0f, 0f,
              0f, gain, 0f, 0f,
              0f, 0f, gain, 0f,
              0f, 0f, 0f, 1f
            )
          })
        }

        EditedMediaItem.Builder(media.build())
          .setRemoveAudio(state.isMuted || clip.isMuted || clip.volume == 0f)
          .setEffects(Effects(audioGain(clip.volume), effects))
          .apply {
            setFrameRate(state.resolutionSetting.fps)
            if (segment.rate != 1f) {
              setSpeed(object : SpeedProvider {
                override fun getSpeed(timeUs: Long): Float = segment.rate
                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
              })
            }
          }
          .build()
      }
    }
    val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(items)
    val audio = if (state.isMuted) emptyList() else state.audioTracks.mapNotNull { track ->
      val source = track.uriString ?: return@mapNotNull null
      val duration = minOf(track.endMs, state.totalDurationMs) - track.startMs
      if (duration <= 0 || track.isMuted || track.volume <= 0f) return@mapNotNull null
      val media = MediaItem.Builder()
        .setUri(Uri.parse(source))
        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setStartPositionMs(track.sourceOffsetMs).setEndPositionMs(track.sourceOffsetMs + duration).build())
        .build()
      val item = EditedMediaItem.Builder(media)
        .setRemoveVideo(true)
        .setEffects(Effects(audioGain(track.volume), emptyList()))
        .build()
      EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
        .apply { if (track.startMs > 0L) addGap(track.startMs * 1000) }
        .addItem(item)
        .build()
    }
    val builder = Composition.Builder(listOf(sequence) + audio)
    if (state.textTracks.isNotEmpty()) {
      builder.setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(captions(state))))))
    }
    return builder.build()
  }

  private fun audioGain(gain: Float) = listOf(
    ToInt16PcmAudioProcessor(),
    ChannelMixingAudioProcessor().apply {
      for (channels in 1..8) {
        val matrix = if (channels <= 2) ChannelMixingMatrix.createForConstantGain(channels, 2)
        else ChannelMixingMatrix(channels, 2, FloatArray(channels * 2) { 1f / channels })
        putChannelMixingMatrix(matrix.scaleBy(gain.coerceIn(0f, 1f)))
      }
    }
  )

  fun colorEffects(state: EditorUiState): List<Effect> = buildList {
    val adjust = state.adjustSettings
    val filter = state.selectedFilter
    val intensity = state.filterIntensity
    val brightness = adjust.brightness / 100f + (filter.brightnessMultiplier - 1f) * intensity
    val contrast = adjust.contrast / 100f + (filter.contrastMultiplier - 1f) * intensity
    val saturation = adjust.saturation + (filter.saturationMultiplier - 1f) * intensity * 100f
    if (brightness != 0f) add(Brightness(brightness.coerceIn(-1f, 1f)))
    if (contrast != 0f) add(Contrast(contrast.coerceIn(-0.99f, 0.99f)))
    if (saturation != 0f) {
      add(HslAdjustment.Builder().adjustSaturation(saturation.coerceIn(-100f, 100f)).build())
    }
    if (adjust.warmth != 0f) {
      val warmth = adjust.warmth / 250f
      add(RgbAdjustment.Builder().setRedScale(1f + warmth).setBlueScale(1f - warmth).build())
    }
  }

  private fun captions(state: EditorUiState): CanvasOverlay = object : CanvasOverlay(true) {
    private val renderer = TextLayerRenderer()
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
      val timeMs = presentationTimeUs / 1000
      state.textTracks.forEach { track ->
        if (timeMs in track.startMs until track.endMs) renderer.draw(canvas, track, timeMs)
      }
    }
  }
}