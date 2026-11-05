package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.speedSegments
import com.example.model.timelineTimeAt
import com.example.ui.theme.CapCutAudioTrack
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.ui.theme.CapCutTextTrack
import com.example.ui.theme.CapCutTimelineBg
import com.example.viewmodel.EditorUiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MIN_ZOOM = 32f
private const val MAX_ZOOM = 144f

@Composable
fun TimelineView(
  uiState: EditorUiState,
  onSeekTo: (Long) -> Unit,
  onScrubStarted: () -> Unit,
  onScrubbingChange: (Boolean) -> Unit,
  onSelectClip: (String) -> Unit,
  onOpenTransition: (String) -> Unit,
  onAppendMediaClick: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onEditAudio: (String) -> Unit,
  onRemoveAudio: (String) -> Unit,
  onEditText: (String) -> Unit,
  onRemoveText: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var zoom by remember { mutableFloatStateOf(64f) }
  var dragPositionMs by remember { mutableFloatStateOf(0f) }
  val scroll = rememberScrollState()
  val position by rememberUpdatedState(uiState.currentTimeMs)
  val seek by rememberUpdatedState(onSeekTo)
  val pause by rememberUpdatedState(onScrubStarted)
  val pxPerMs = with(LocalDensity.current) { zoom.dp.toPx() } / 1000f
  val trackWidth = (uiState.totalDurationMs * zoom / 1000f).dp
  val drag = rememberDraggableState { delta ->
    dragPositionMs = (dragPositionMs - delta / pxPerMs).coerceIn(0f, uiState.totalDurationMs.toFloat())
    seek(dragPositionMs.roundToLong())
  }

  LaunchedEffect(scroll, pxPerMs) {
    snapshotFlow { (position * pxPerMs).roundToInt() }.collect { scroll.scrollTo(it) }
  }

  Column(modifier.fillMaxWidth().background(CapCutTimelineBg)) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onTogglePlayPause,
          enabled = uiState.totalDurationMs > 0,
          modifier = Modifier.testTag("timeline_play_pause_button")
        ) {
          Icon(
            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (uiState.isPlaying) "Pause" else "Play",
            tint = CapCutTeal
          )
        }
        Text(
          "${formatTime(uiState.currentTimeMs)} / ${formatTime(uiState.totalDurationMs)}",
          color = CapCutTextPrimary,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { zoom = (zoom / 1.5f).coerceAtLeast(MIN_ZOOM) }, enabled = zoom > MIN_ZOOM) {
          Icon(Icons.Default.Remove, "Zoom out timeline")
        }
        IconButton(onClick = { zoom = (zoom * 1.5f).coerceAtMost(MAX_ZOOM) }, enabled = zoom < MAX_ZOOM) {
          Icon(Icons.Default.Add, "Zoom in timeline")
        }
      }
    }

    BoxWithConstraints(
      Modifier.fillMaxWidth()
        .draggable(
          state = drag,
          orientation = Orientation.Horizontal,
          onDragStarted = {
            pause()
            onScrubbingChange(true)
            dragPositionMs = position.toFloat()
          },
          onDragStopped = { onScrubbingChange(false) }
        )
        .padding(bottom = 10.dp)
    ) {
      val availableWidth = maxWidth
      val center = availableWidth / 2
      val laneWidth = trackWidth + 48.dp
      Row(
        Modifier.horizontalScroll(scroll, enabled = false).padding(start = center, end = center)
      ) {
        Column(Modifier.width(laneWidth), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          TimelineRuler(
            scrollPosition = { scroll.value },
            viewportWidth = with(LocalDensity.current) { availableWidth.toPx() },
            leadingSpace = with(LocalDensity.current) { center.toPx() },
            widthDpPerSecond = zoom,
            durationMs = uiState.totalDurationMs,
            onTap = {
              pause()
              seek(it)
            }
          )
          if (uiState.selectedTextId == null) Row(verticalAlignment = Alignment.CenterVertically) {
            uiState.clips.forEachIndexed { index, clip ->
              Box(Modifier.width((clip.timelineDurationMs * zoom / 1000f).dp).height(60.dp)) {
                ClipTile(clip, clip.id == uiState.selectedClipId) { onSelectClip(clip.id) }
                if (index < uiState.clips.lastIndex) {
                  Box(
                    Modifier.align(Alignment.CenterEnd).size(22.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(if (clip.fadeOutMs == 0L) CapCutTimelineBg else CapCutTeal)
                      .clickable { onOpenTransition(clip.id) }
                      .testTag("transition_button_${clip.id}"),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.AutoFixHigh, "Transition", Modifier.size(12.dp))
                  }
                }
              }
            }
            IconButton(
              onClick = onAppendMediaClick,
              modifier = Modifier.testTag("timeline_append_media_button")
            ) { Icon(Icons.Default.Add, "Add media", tint = CapCutTeal) }
          }
          val audioLanes = remember(uiState.audioTracks) {
            lanes(uiState.audioTracks, { it.startMs }, { it.endMs })
          }
          val textLanes = remember(uiState.textTracks) {
            lanes(uiState.textTracks, { it.startMs }, { it.endMs })
          }
          if (audioLanes.isNotEmpty() || textLanes.isNotEmpty()) {
            Column(
              Modifier.fillMaxWidth().heightIn(max = 88.dp).verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              audioLanes.forEach { lane ->
                Box(Modifier.fillMaxWidth().height(27.dp)) {
                  lane.forEach { track ->
                    TrackTile(track.title, track.startMs, track.endMs, zoom, CapCutAudioTrack,
                      onEdit = { onEditAudio(track.id) }) { onRemoveAudio(track.id) }
                  }
                }
              }
              textLanes.forEach { lane ->
                Box(Modifier.fillMaxWidth().height(27.dp)) {
                  lane.forEach { track ->
                    TrackTile(track.text, track.startMs, track.endMs, zoom, CapCutTextTrack,
                      onEdit = { onEditText(track.id) }) { onRemoveText(track.id) }
                  }
                }
              }
            }
          }
        }
      }
      Box(
        Modifier.align(Alignment.TopCenter).matchParentSize()
          .testTag("timeline_playhead_cursor")
      ) {
        Canvas(Modifier.matchParentSize()) {
          val centerX = size.width / 2
          drawLine(CapCutTeal, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 2.dp.toPx())
          drawCircle(CapCutTeal, radius = 3.dp.toPx(), center = Offset(centerX, 4.dp.toPx()))
        }
      }
    }
  }
}

@Composable
private fun TimelineRuler(
  scrollPosition: () -> Int,
  viewportWidth: Float,
  leadingSpace: Float,
  widthDpPerSecond: Float,
  durationMs: Long,
  onTap: (Long) -> Unit
) {
  val callback by rememberUpdatedState(onTap)
  val density = LocalDensity.current
  val pxPerSecond = with(density) { widthDpPerSecond.dp.toPx() }
  val stepSeconds = if (widthDpPerSecond < 48f) 2 else 1
  val paint = remember {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      color = CapCutTextSecondary.toArgb()
      typeface = android.graphics.Typeface.MONOSPACE
    }
  }
  Canvas(
    Modifier.fillMaxWidth().height(23.dp)
      .pointerInput(pxPerSecond) {
        detectTapGestures { point -> callback((point.x / pxPerSecond * 1000).roundToLong()) }
      }
  ) {
    val stepWidth = pxPerSecond * stepSeconds
    val left = scrollPosition() - leadingSpace
    val first = (left / stepWidth).toInt().coerceAtLeast(0)
    val last = ((left + viewportWidth) / stepWidth).toInt()
      .coerceAtMost((durationMs / (1000L * stepSeconds)).toInt())
    paint.textSize = 9.sp.toPx()
    for (index in first..last) {
      val x = index * stepWidth
      drawContext.canvas.nativeCanvas.drawText(formatTime(index * stepSeconds * 1000L), x + 3.dp.toPx(), 11.dp.toPx(), paint)
      drawLine(CapCutTextSecondary.copy(alpha = 0.55f), Offset(x, 15.dp.toPx()), Offset(x, size.height))
    }
  }
}

@Composable
private fun ClipTile(clip: VideoClip, selected: Boolean, onClick: () -> Unit) {
  val context = LocalContext.current
  val thumbnail = remember(context, clip.uriString, clip.thumbnailRes, clip.isVideo, clip.startMs) {
    val data = clip.uriString?.let(android.net.Uri::parse) ?: clip.thumbnailRes.takeIf { it != 0 }
    data?.let {
      ImageRequest.Builder(context).data(it).size(240, 112)
        .apply {
          if (clip.isVideo && clip.uriString != null) {
            decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
            videoFrameMillis(clip.startMs)
          }
        }
        .build()
    }
  }
  val shape = RoundedCornerShape(8.dp)
  val markers = remember(clip) {
    val segments = clip.speedSegments()
    val visible = clip.startMs until clip.endMs
    val speedMarks = clip.speedPoints.filter { it.sourceMs in visible }
      .map { segments.timelineTimeAt(it.sourceMs) to CapCutTeal }
    val transformMarks = clip.transformKeyframes.filter { it.sourceMs in visible }
      .map { segments.timelineTimeAt(it.sourceMs) to CapCutPurple }
    speedMarks + transformMarks
  }
  Box(
    Modifier.fillMaxWidth().fillMaxHeight().padding(end = 2.dp)
      .clip(shape)
      .background(Brush.horizontalGradient(listOf(Color(0xFF284452), CapCutSurface)))
      .border(if (selected) 2.dp else 1.dp, if (selected) CapCutTeal else Color(0xFF405260), shape)
      .clickable(onClick = onClick)
      .testTag("timeline_clip_${clip.id}")
  ) {
    if (thumbnail != null) {
      AsyncImage(
        model = thumbnail, contentDescription = null,
        modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop
      )
      Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
    }
    if (markers.isNotEmpty()) {
      Canvas(Modifier.matchParentSize()) {
        markers.forEach { (position, color) ->
          val x = size.width * (position.toFloat() / clip.timelineDurationMs).coerceIn(0f, 1f)
          drawCircle(color, radius = 3.dp.toPx(), center = Offset(x.coerceIn(0f, size.width), 7.dp.toPx()))
        }
      }
    }
    Text(
      clip.title, Modifier.align(Alignment.BottomStart).padding(horizontal = 6.dp, vertical = 5.dp),
      color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
      maxLines = 1, overflow = TextOverflow.Ellipsis
    )
  }
}

private fun <T> lanes(items: List<T>, start: (T) -> Long, end: (T) -> Long): List<List<T>> {
  val lanes = mutableListOf<MutableList<T>>()
  items.sortedBy(start).forEach { item ->
    val lane = lanes.firstOrNull { end(it.last()) <= start(item) }
      ?: mutableListOf<T>().also { lanes.add(it) }
    lane.add(item)
  }
  return lanes
}

@Composable
private fun TrackTile(title: String, start: Long, end: Long, zoom: Float, color: Color, onEdit: (() -> Unit)? = null, onRemove: () -> Unit) {
  Row(
    Modifier.offset(x = (start * zoom / 1000f).dp)
      .width(((end - start).coerceAtLeast(0) * zoom / 1000f).dp)
      .fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color)
      .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
      .padding(horizontal = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      title, Modifier.weight(1f), color = CapCutTextPrimary, fontSize = 10.sp,
      maxLines = 1, overflow = TextOverflow.Ellipsis
    )
    IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
      Icon(Icons.Default.Close, "Remove $title", Modifier.size(12.dp), tint = CapCutPurple)
    }
  }
}
