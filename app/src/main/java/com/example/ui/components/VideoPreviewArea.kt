package com.example.ui.components

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.media.cleanupEffect
import com.example.media.OfflineComposition
import androidx.media3.common.Effect
import com.example.model.VideoClip
import com.example.model.TransformKeyframe
import com.example.model.speedSegments
import com.example.model.sourceTimeAt
import com.example.model.transformAt
import com.example.model.fadeLevelAt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.viewmodel.EditorUiState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPreviewArea(
  uiState: EditorUiState,
  onTogglePlayPause: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onBuffering: (Boolean) -> Unit,
  onPlaybackError: (String) -> Unit,
  isScrubbing: Boolean,
  modifier: Modifier = Modifier
) {

  val currentClip = uiState.currentClipAtPlayhead
  val timelineStart = remember(uiState.clips, currentClip?.id) {
    uiState.clips.takeWhile { it.id != currentClip?.id }.sumOf { it.timelineDurationMs }
  }
  val previewColors = remember(uiState.selectedFilter, uiState.filterIntensity, uiState.adjustSettings) { OfflineComposition.colorEffects(uiState) }
  val localTime = uiState.currentTimeMs - timelineStart

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(CapCutBackground)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    // Aspect Ratio Container
    BoxWithConstraints(
      modifier = Modifier
        .aspectRatio(uiState.aspectRatio.ratioFloat)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.Black)
        .clickable { onTogglePlayPause() }
        .testTag("video_preview_canvas"),
      contentAlignment = Alignment.Center
    ) {
      // 1. Media Content (Real Video URI or Real Image URI or Aesthetic Demo Canvas)
      if (currentClip?.uriString != null) {
        val uri = Uri.parse(currentClip.uriString)
        val segments = remember(currentClip) { currentClip.speedSegments() }
        val sourceMs = segments.sourceTimeAt(localTime)
          .coerceIn(currentClip.startMs, currentClip.endMs - 1)
        val transform = currentClip.transformAt(sourceMs)
        if (currentClip.isVideo) {
          LocalVideoPreview(
            clip = currentClip,
            positionMs = sourceMs - currentClip.startMs,
            colorEffects = previewColors,
            rate = segments.last { it.startMs <= sourceMs }.rate,
            transform = transform,
            playing = uiState.isPlaying,
            muted = uiState.isMuted,
            scrubbing = isScrubbing,
            onBuffering = onBuffering,
            onError = onPlaybackError
          )
        } else {
          // Photo Content via Coil AsyncImage
          AsyncImage(
            model = uri,
            contentDescription = "Selected Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .animatedClipTransform(transform)
              .graphicsLayer {
                rotationZ = currentClip.rotationDegrees
                val zoom = 1f / (1f - currentClip.cropFraction)
                scaleX = if (currentClip.isMirrored) -zoom else zoom
                scaleY = zoom
              }
          )
        }
      } else {
        // Built-in Demo Canvas with gradient and motion
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                  Color(0xFF1F1C2C),
                  Color(0xFF928DAB).copy(alpha = 0.5f),
                  Color(0xFF0F2027)
                )
              )
            )
            .rotate(currentClip?.rotationDegrees ?: 0f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = currentClip?.title ?: "No Media Selected",
              color = CapCutTextPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp
            )
            Text(
              text = if (currentClip?.isVideo == true) "Video Clip" else "Photo",
              color = CapCutTextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }

      val fadeLevel = currentClip?.fadeLevelAt(localTime) ?: 1f
      if (fadeLevel < 1f) {
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 1f - fadeLevel)))
      }

      // 3. Text Overlay Tracks active at currentTimeMs
      val activeTexts = uiState.textTracks.filter {
        uiState.currentTimeMs in it.startMs until it.endMs
      }
      Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 55.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        activeTexts.forEach { textTrack ->
          val textColor = runCatching { Color(android.graphics.Color.parseColor(textTrack.colorHex)) }
            .getOrDefault(Color.White)
          Text(
            text = textTrack.text,
            color = textColor,
            fontSize = textTrack.fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
              .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Playback affordance
      if (!uiState.isPlaying && !isScrubbing && currentClip != null) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      // Preview controls
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.45f))
          .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Mute button
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = if (uiState.isMuted) "Unmute" else "Mute",
            tint = if (uiState.isMuted) CapCutTeal else Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        // Preview format
        Text(
          text = uiState.aspectRatio.label.substringBefore(" ("),
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )

        // Fullscreen toggle
        IconButton(
          onClick = onToggleFullscreen,
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Fullscreen,
            contentDescription = if (uiState.isFullscreen) "Exit fullscreen" else "Fullscreen",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LocalVideoPreview(
  clip: VideoClip,
  positionMs: Long,
  rate: Float,
  colorEffects: List<Effect>,
  transform: TransformKeyframe,
  playing: Boolean,
  muted: Boolean,
  scrubbing: Boolean,
  onBuffering: (Boolean) -> Unit,
  onError: (String) -> Unit
) {
  val context = LocalContext.current
  val player = remember(context) { ExoPlayer.Builder(context).build() }
  val bufferingCallback = rememberUpdatedState(onBuffering)
  val errorCallback = rememberUpdatedState(onError)
  DisposableEffect(player) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        bufferingCallback.value(playbackState == Player.STATE_BUFFERING)
      }

      override fun onPlayerError(error: PlaybackException) {
        bufferingCallback.value(false)
        errorCallback.value("Unable to play this clip")
      }
    }
    player.addListener(listener)
    onDispose {
      player.removeListener(listener)
      player.release()
      bufferingCallback.value(false)
    }
  }

  val media = remember(clip.uriString, clip.startMs, clip.endMs) {
    MediaItem.Builder()
      .setUri(requireNotNull(clip.uriString))
      .setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder()
          .setStartPositionMs(clip.startMs)
          .setEndPositionMs(clip.endMs)
          .build()
      )
      .build()
  }
  val videoEffects = remember(clip.chromaKey, clip.videoDenoise, colorEffects) { listOfNotNull(clip.cleanupEffect()) + colorEffects }
  LaunchedEffect(player, videoEffects) {
    player.setVideoEffects(videoEffects)
  }
  LaunchedEffect(player, media) {
    player.setMediaItem(media, positionMs)
    player.prepare()
  }

  SideEffect {
    if (player.isScrubbingModeEnabled != scrubbing) player.setScrubbingModeEnabled(scrubbing)
    player.volume = if (muted) 0f else clip.volume.coerceIn(0f, 1f)
    if (player.playbackParameters.speed != rate) player.setPlaybackSpeed(rate)
    if (player.currentMediaItem == media) {
      val tolerance = if (playing) 400L else 8L
      val restarting = playing && player.playbackState == Player.STATE_ENDED && positionMs == 0L
      if (restarting || kotlin.math.abs(player.currentPosition - positionMs) > tolerance) {
        player.seekTo(positionMs)
      }
    }
    player.playWhenReady = playing
  }
  ContentFrame(
    player = player,
    modifier = Modifier.fillMaxSize().animatedClipTransform(transform).graphicsLayer {
      rotationZ = clip.rotationDegrees
      val zoom = 1f / (1f - clip.cropFraction)
      scaleX = if (clip.isMirrored) -zoom else zoom
      scaleY = zoom
    },
    surfaceType = SURFACE_TYPE_TEXTURE_VIEW
  )
}

private fun Modifier.animatedClipTransform(transform: TransformKeyframe): Modifier = graphicsLayer {
  scaleX = transform.scale
  scaleY = transform.scale
  translationX = transform.x * size.width
  translationY = transform.y * size.height
}

fun formatTime(ms: Long): String {
  val totalSeconds = (ms / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format("%02d:%02d", minutes, seconds)
}
