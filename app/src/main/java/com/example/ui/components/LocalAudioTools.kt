package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AudioTrackItem
import com.example.viewmodel.EditorUiState
import kotlin.math.abs

@Composable
fun LocalAudioPlayback(state: EditorUiState, onError: () -> Unit) {
  state.audioTracks.forEach { track ->
    val source = track.uriString ?: return@forEach
    val duration = minOf(track.endMs, state.totalDurationMs) - track.startMs
    val localPosition = state.currentTimeMs - track.startMs
    if (duration > 0 && localPosition in -750L until duration && !track.isMuted && track.volume > 0f) {
      androidx.compose.runtime.key(track.id) {
        LocalTrackPlayer(source, localPosition.coerceAtLeast(0) + track.sourceOffsetMs, state.isPlaying && localPosition >= 0, if (state.isMuted) 0f else track.volume, onError)
      }
    }
  }
}

@Composable
private fun LocalTrackPlayer(source: String, position: Long, playing: Boolean, volume: Float, onError: () -> Unit) {
  val context = LocalContext.current
  val player = remember(context, source) { ExoPlayer.Builder(context).build() }
  val latestError = androidx.compose.runtime.rememberUpdatedState(onError)
  DisposableEffect(player) {
    val listener = object : Player.Listener {
      override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        latestError.value()
      }
    }
    player.addListener(listener)
    onDispose {
      player.removeListener(listener)
      player.release()
    }
  }
  LaunchedEffect(player) {
    player.setMediaItem(MediaItem.fromUri(source), position.coerceAtLeast(0))
    player.prepare()
  }
  SideEffect {
    val active = position >= 0L && playing
    val target = position.coerceAtLeast(0L)
    player.volume = volume
    if (player.mediaItemCount > 0 && abs(player.currentPosition - target) > (if (active) 250L else 30L)) {
      player.seekTo(target)
    }
    player.playWhenReady = active
  }
}

@Composable
fun LocalAudioDialog(
  track: AudioTrackItem,
  projectDuration: Long,
  onApply: (Long, Long, Float) -> Unit,
  onDismiss: () -> Unit
) {
  var start by remember(track.id) { mutableStateOf(track.startMs.toString()) }
  var end by remember(track.id) { mutableStateOf(track.endMs.toString()) }
  var volume by remember(track.id) { mutableStateOf(track.volume) }
  val from = start.toLongOrNull()
  val to = end.toLongOrNull()
  val valid = from != null && to != null && from >= 0 && to <= projectDuration &&
    to > from && to - from <= track.sourceDurationMs - track.sourceOffsetMs
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(track.title) },
    text = {
      EditorDialogContent {
        Text("Set the range on the project timeline. Source starts at ${track.sourceOffsetMs} ms.")
        OutlinedTextField(start, { start = it }, label = { Text("Start (ms)") }, singleLine = true)
        OutlinedTextField(end, { end = it }, label = { Text("End (ms)") }, singleLine = true)
        Text("Volume ${(volume * 100).toInt()}%")
        Slider(volume, { volume = it })
      }
    },
    confirmButton = {
      TextButton(enabled = valid, onClick = { onApply(requireNotNull(from), requireNotNull(to), volume) }) { Text("Apply") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
