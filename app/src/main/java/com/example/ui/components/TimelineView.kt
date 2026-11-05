package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.EditorUiState
import kotlin.math.*

private data class ClipSpan(val clip: VideoClip, val startMs: Long)

@Composable
fun TimelineView(
  uiState: EditorUiState,
  onSeekTo: (Long) -> Unit,
  onScrubStarted: () -> Unit,
  onScrubbingChange: (Boolean) -> Unit,
  onSelectClip: (String) -> Unit,
  onDeselect: () -> Unit,
  onOpenTransition: (String) -> Unit,
  onAppendMediaClick: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onAddAudio: () -> Unit,
  onAddText: () -> Unit,
  onToggleClipMute: (String) -> Unit,
  onEditCover: () -> Unit,
  onBeginEdit: () -> Unit,
  onTrimClip: (String, Long, Long) -> Unit,
  onAudioTiming: (String, Long, Long, Long) -> Unit,
  onTextTiming: (String, Long, Long) -> Unit,
  onEditAudio: (String) -> Unit,
  onRemoveAudio: (String) -> Unit,
  onEditText: (String) -> Unit,
  onRemoveText: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var zoom by rememberSaveable(uiState.currentProjectId) { mutableFloatStateOf(64f) }
  val latestTime by rememberUpdatedState(uiState.currentTimeMs)
  val latestDuration by rememberUpdatedState(uiState.totalDurationMs)
  val seek by rememberUpdatedState(onSeekTo)
  val pause by rememberUpdatedState(onScrubStarted)
  val scrubbing by rememberUpdatedState(onScrubbingChange)
  val density = LocalDensity.current
  val pxPerMs = with(density) { zoom.dp.toPx() } / 1000.0
  val latestScale by rememberUpdatedState(pxPerMs)
  var rawPosition by remember { mutableDoubleStateOf(0.0) }
  var tracking by remember { mutableStateOf(false) }
  val scroll = rememberScrollableState { delta ->
    if (!tracking) {
      tracking = true
      rawPosition = latestTime.toDouble()
      pause()
      scrubbing(true)
    }
    val previous = rawPosition
    rawPosition = (previous - delta / latestScale).coerceIn(0.0, latestDuration.toDouble())
    seek(rawPosition.roundToLong())
    ((previous - rawPosition) * latestScale).toFloat()
  }
  LaunchedEffect(scroll) {
    snapshotFlow { scroll.isScrollInProgress }.collect { active ->
      if (!active && tracking) {
        tracking = false
        scrubbing(false)
      }
    }
  }
  LaunchedEffect(uiState.isPlaying) { if (uiState.isPlaying) scroll.stopScroll() }
  DisposableEffect(Unit) { onDispose { scrubbing(false) } }

  val spans = remember(uiState.clips) {
    var time = 0L
    uiState.clips.map { clip -> ClipSpan(clip, time).also { time += clip.timelineDurationMs } }
  }
  val audioLanes = remember(uiState.audioTracks) { packLanes(uiState.audioTracks, { it.startMs }, { it.endMs }) }
  val textLanes = remember(uiState.textTracks) { packLanes(uiState.textTracks, { it.startMs }, { it.endMs }) }
  val bodyLimit = (LocalConfiguration.current.screenHeightDp * 0.32f).coerceIn(130f, 245f).dp

  Column(modifier.fillMaxWidth().background(CapCutTimelineBg).testTag("mobile_timeline")) {
    Row(
      Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(formatTime(uiState.currentTimeMs), color = CapCutTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
      Text(" / " + formatTime(uiState.totalDurationMs), color = CapCutTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
      Spacer(Modifier.weight(1f))
      IconButton(onClick = onTogglePlayPause, enabled = uiState.clips.isNotEmpty(), modifier = Modifier.testTag("timeline_play_pause_button")) {
        Icon(if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (uiState.isPlaying) "Pause" else "Play")
      }
      IconButton(onClick = { zoom = (zoom / 1.4f).coerceAtLeast(24f) }, enabled = zoom > 24f,
        modifier = Modifier.testTag("timeline_zoom_out")) { Icon(Icons.Default.Remove, "Zoom out timeline") }
      IconButton(onClick = { zoom = (zoom * 1.4f).coerceAtMost(960f) }, enabled = zoom < 960f,
        modifier = Modifier.testTag("timeline_zoom_in")) { Icon(Icons.Default.Add, "Zoom in timeline") }
    }

    BoxWithConstraints(
      Modifier.fillMaxWidth().heightIn(max = bodyLimit)
        .scrollable(scroll, Orientation.Horizontal)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
              val event = awaitPointerEvent()
              if (event.changes.count { it.pressed } >= 2) {
                pause()
                val factor = event.calculateZoom()
                if (factor != 1f) zoom = (zoom * factor).coerceIn(24f, 960f)
                event.changes.forEach { it.consume() }
              }
            } while (event.changes.any { it.pressed })
          }
        }.pointerInput(onDeselect) { detectTapGestures(onTap = { onDeselect() }) }
        .testTag("timeline_scrub_surface")
    ) {
      val viewportWidth = constraints.maxWidth.toFloat()
      val origin = viewportWidth / 2f - (uiState.currentTimeMs * pxPerMs).toFloat()
      val viewWidth = maxWidth
      Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        ViewportRuler(uiState.currentTimeMs, uiState.totalDurationMs, zoom) { time ->
          pause()
          onSeekTo(time)
        }
        if (uiState.selectedTextId == null) {
          Box(Modifier.fillMaxWidth().height(64.dp)) {
            if (origin > 12f) {
              TimelineStartTools(
                uiState.currentClipAtPlayhead, uiState.coverUri ?: uiState.clips.firstOrNull()?.uriString,
                uiState.coverUri == null && uiState.clips.firstOrNull()?.isVideo == true,
                onToggleClipMute, onEditCover,
                Modifier.offset { IntOffset((origin - viewportWidth / 2).roundToInt(), 0) }.width(viewWidth / 2 - 8.dp)
              )
            }
            spans.forEachIndexed { index, span ->
              val left = origin + (span.startMs * pxPerMs).toFloat()
              val right = left + (span.clip.timelineDurationMs * pxPerMs).toFloat()
              if (right > 0 && left < viewportWidth) {
                key(span.clip.id) {
                  TimelineClip(
                    span.clip, left, right, viewportWidth, pxPerMs,
                    selected = span.clip.id == uiState.selectedClipId,
                    onSelect = { onSelectClip(span.clip.id) }, onBeginEdit = onBeginEdit,
                    onTrim = { from, to -> onTrimClip(span.clip.id, from, to) }
                  )
                }
                if (index < spans.lastIndex && right in 12f..(viewportWidth - 12f)) {
                  IconButton(
                    onClick = { onOpenTransition(span.clip.id) },
                    modifier = Modifier.offset { IntOffset((right - with(density) { 16.dp.toPx() }).roundToInt(), 0) }
                      .align(Alignment.CenterStart).size(32.dp).testTag("transition_button_${span.clip.id}")
                  ) {
                    Icon(Icons.Default.UnfoldMore, "Transition between clips",
                      Modifier.size(18.dp).background(CapCutSurface, RoundedCornerShape(4.dp)),
                      tint = if (span.clip.fadeOutMs > 0) CapCutTeal else CapCutTextPrimary)
                  }
                }
              }
            }
            val end = origin + (uiState.totalDurationMs * pxPerMs).toFloat()
            val buttonSize = with(density) { 44.dp.toPx() }
            val buttonX = minOf(end + with(density) { 8.dp.toPx() }, viewportWidth - buttonSize - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
            if (uiState.clips.isNotEmpty()) {
              IconButton(
                onClick = onAppendMediaClick,
                modifier = Modifier.offset { IntOffset(buttonX.roundToInt(), 0) }
                  .align(Alignment.CenterStart).size(44.dp).background(CapCutTextPrimary, RoundedCornerShape(10.dp))
                  .testTag("timeline_append_media_button")
              ) { Icon(Icons.Default.Add, "Add media", tint = CapCutBackground, modifier = Modifier.size(28.dp)) }
            }
          }
        }

        if (uiState.selectedTextId == null) {
          audioLanes.forEach { lane ->
            Box(Modifier.fillMaxWidth().height(40.dp)) {
              lane.forEach { track ->
                TimelineTrack(track.id, track.title, track.startMs, track.endMs, origin, viewportWidth, pxPerMs,
                  CapCutAudioTrack, Icons.Default.MusicNote,
                  onEdit = { onEditAudio(track.id) }, onRemove = { onRemoveAudio(track.id) },
                  onBegin = onBeginEdit,
                  onMove = { start ->
                    onAudioTiming(track.id, start, start + track.endMs - track.startMs, track.sourceOffsetMs)
                  },
                  onTrim = { leftEdge, delta ->
                    if (leftEdge) {
                      val lower = maxOf(0L, track.startMs - track.sourceOffsetMs)
                      val start = (track.startMs + delta).coerceIn(lower, track.endMs - 100)
                      onAudioTiming(track.id, start, track.endMs, track.sourceOffsetMs + start - track.startMs)
                    } else {
                      val maxEnd = minOf(uiState.totalDurationMs, track.startMs + track.sourceDurationMs - track.sourceOffsetMs)
                      if (maxEnd >= track.startMs + 100) onAudioTiming(track.id, track.startMs,
                        (track.endMs + delta).coerceIn(track.startMs + 100, maxEnd), track.sourceOffsetMs)
                    }
                  }, projectDuration = uiState.totalDurationMs)
              }
            }
          }
          AddTrackLane("Add audio", Icons.Default.MusicNote, origin, onAddAudio, "timeline_add_audio")
        }

        textLanes.forEach { lane ->
          Box(Modifier.fillMaxWidth().height(40.dp)) {
            lane.forEach { track ->
              TimelineTrack(track.id, track.text, track.startMs, track.endMs, origin, viewportWidth, pxPerMs,
                CapCutTextTrack, Icons.Default.TextFields,
                onEdit = { onEditText(track.id) }, onRemove = { onRemoveText(track.id) },
                onBegin = onBeginEdit,
                onMove = { start -> onTextTiming(track.id, start, start + track.endMs - track.startMs) },
                onTrim = { leftEdge, delta ->
                  if (leftEdge) onTextTiming(track.id, (track.startMs + delta).coerceIn(0, track.endMs - 100), track.endMs)
                  else onTextTiming(track.id, track.startMs, (track.endMs + delta).coerceIn(track.startMs + 100, uiState.totalDurationMs))
                }, projectDuration = uiState.totalDurationMs,
                selected = track.id == uiState.selectedTextId)
            }
          }
        }
        AddTrackLane("Add text", Icons.Default.TextFields, origin, onAddText, "timeline_add_text")
      }
      Canvas(Modifier.matchParentSize().testTag("timeline_playhead_cursor")) {
        drawLine(CapCutTextPrimary, Offset(size.width / 2, 23.dp.toPx()), Offset(size.width / 2, size.height),
          2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
      }
    }
  }
}

@Composable
private fun ViewportRuler(position: Long, duration: Long, zoom: Float, onSeek: (Long) -> Unit) {
  val scale = with(LocalDensity.current) { zoom.dp.toPx() } / 1000f
  val seek by rememberUpdatedState(onSeek)
  val time by rememberUpdatedState(position)
  val total by rememberUpdatedState(duration)
  val paint = remember {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      color = CapCutTextSecondary.toArgb()
      textAlign = android.graphics.Paint.Align.CENTER
      typeface = android.graphics.Typeface.MONOSPACE
    }
  }
  Canvas(Modifier.fillMaxWidth().height(24.dp).pointerInput(scale) {
    detectTapGestures { point ->
      seek((time + (point.x - size.width / 2) / scale).roundToLong().coerceIn(0, total))
    }
  }) {
    val major = when { zoom < 40f -> 5000L; zoom < 110f -> 2000L; else -> 1000L }
    val half = if (zoom > 400f) 100L else major / 2
    val startTime = position - size.width / 2 / scale
    val first = floor(startTime / half).toLong().coerceAtLeast(0)
    val last = ceil((position + size.width / 2 / scale) / half).toLong().coerceAtMost(duration / half)
    paint.textSize = 10.sp.toPx()
    for (index in first..last) {
      val at = index * half
      val x = size.width / 2 + (at - position) * scale
      if (at % major == 0L) {
        drawContext.canvas.nativeCanvas.drawText(formatTime(at), x, 13.dp.toPx(), paint)
      } else drawCircle(CapCutTextSecondary, 1.2.dp.toPx(), Offset(x, 10.dp.toPx()))
    }
  }
}

@Composable
private fun TimelineStartTools(
  clip: VideoClip?, cover: String?, coverIsVideo: Boolean, onMute: (String) -> Unit, onCover: () -> Unit, modifier: Modifier
) {
  Row(modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
    verticalAlignment = Alignment.CenterVertically) {
    Column(
      Modifier.width(58.dp).fillMaxHeight().clickable(enabled = clip != null) { clip?.let { onMute(it.id) } }
        .testTag("timeline_clip_mute"),
      horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
      val muted = clip?.isMuted == true || clip?.volume == 0f
      Icon(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, Modifier.size(23.dp), tint = CapCutTextSecondary)
      Text(if (muted) "Unmute clip" else "Mute clip", fontSize = 10.sp, color = CapCutTextSecondary,
        modifier = Modifier.padding(top = 4.dp), maxLines = 2)
    }
    Box(
      Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)).background(CapCutSurfaceVariant)
        .clickable(enabled = clip != null, onClick = onCover).testTag("timeline_cover"),
      contentAlignment = Alignment.Center
    ) {
      TimelineThumbnail(cover, coverIsVideo, 0L, Modifier.matchParentSize())
      Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
      Text("Cover", fontSize = 11.sp, color = Color.White)
    }
  }
}

@Composable
private fun TimelineClip(
  clip: VideoClip, left: Float, right: Float, viewportWidth: Float, pxPerMs: Double,
  selected: Boolean, onSelect: () -> Unit, onBeginEdit: () -> Unit, onTrim: (Long, Long) -> Unit
) {
  val density = LocalDensity.current
  val visibleLeft = maxOf(0f, left)
  val visibleRight = minOf(viewportWidth, right)
  val width = (visibleRight - visibleLeft).coerceAtLeast(1f)
  val frameWidth = with(density) { 46.dp.toPx() }
  val segments = remember(clip) { clip.speedSegments() }
  Box(
    Modifier.offset { IntOffset(visibleLeft.roundToInt(), 0) }.width(with(density) { width.toDp() })
      .fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(CapCutSurfaceVariant)
      .clickable(onClick = onSelect).testTag("timeline_clip_${clip.id}")
  ) {
    val first = floor((visibleLeft - left) / frameWidth).toInt().coerceAtLeast(0)
    val last = ceil((visibleRight - left) / frameWidth).toInt()
    for (index in first until last) {
      val frameLeft = index * frameWidth + left - visibleLeft
      val localTime = (index * frameWidth / pxPerMs).toLong().coerceIn(0L, clip.timelineDurationMs - 1)
      val sourceTime = segments.sourceTimeAt(localTime).coerceAtMost(clip.endMs - 1)
      TimelineThumbnail(clip.uriString, clip.isVideo, sourceTime,
        Modifier.offset { IntOffset(frameLeft.roundToInt(), 0) }.width(46.dp).fillMaxHeight())
    }
    Box(Modifier.matchParentSize().border(if (selected) 2.dp else 0.5.dp,
      if (selected) CapCutTextPrimary else CapCutSurfaceVariant, RoundedCornerShape(5.dp)))
    if (clip.isMuted || clip.volume == 0f) {
      Icon(Icons.Default.VolumeOff, "Clip muted", Modifier.padding(5.dp).size(15.dp).background(Color.Black.copy(alpha = 0.6f)),
        tint = Color.White)
    }
    if (selected && left >= -1f) {
      TrimHandle(true, onBeginEdit) { delta ->
        val (from, to) = clip.trimAtTimelineOffset(true, (delta / pxPerMs).roundToLong())
        onTrim(from, to)
      }
    }
    if (selected && right <= viewportWidth + 1f) {
      TrimHandle(false, onBeginEdit) { delta ->
        val (from, to) = clip.trimAtTimelineOffset(false, (delta / pxPerMs).roundToLong())
        onTrim(from, to)
      }
    }
  }
}

@Composable
private fun BoxScope.TrimHandle(left: Boolean, onBegin: () -> Unit, onDelta: (Float) -> Unit) {
  val delta by rememberUpdatedState(onDelta)
  val begin by rememberUpdatedState(onBegin)
  var accumulated by remember { mutableFloatStateOf(0f) }
  var gestureDelta by remember { mutableStateOf(onDelta) }
  Box(
    Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).width(18.dp).fillMaxHeight()
      .background(CapCutTextPrimary)
      .draggable(
        rememberDraggableState { pixels -> accumulated += pixels; gestureDelta(accumulated) },
        Orientation.Horizontal,
        onDragStarted = { accumulated = 0f; gestureDelta = delta; begin() }
      ).testTag(if (left) "timeline_trim_start" else "timeline_trim_end"),
    contentAlignment = Alignment.Center
  ) { Box(Modifier.width(2.dp).height(19.dp).background(CapCutBackground, RoundedCornerShape(2.dp))) }
}

@Composable
private fun AddTrackLane(
  label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
  origin: Float, onClick: () -> Unit, tag: String
) {
  val density = LocalDensity.current
  Box(Modifier.fillMaxWidth().height(40.dp)) {
    val start = maxOf(0f, origin)
    Row(
      Modifier.offset { IntOffset(start.roundToInt(), 0) }.fillMaxWidth().fillMaxHeight()
        .background(CapCutSurfaceVariant).clickable(onClick = onClick).padding(horizontal = 12.dp).testTag(tag),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = CapCutTextSecondary)
      Spacer(Modifier.width(7.dp))
      Text(label, color = CapCutTextSecondary, fontSize = 13.sp)
    }
    if (origin > with(density) { 48.dp.toPx() }) {
      Icon(icon, null, Modifier.offset { IntOffset((origin - with(density) { 42.dp.toPx() }).roundToInt(), 0) }
        .align(Alignment.CenterStart).size(28.dp), tint = CapCutTextSecondary)
    }
  }
}

@Composable
private fun TimelineTrack(
  id: String, title: String, start: Long, end: Long, origin: Float, viewport: Float, pxPerMs: Double,
  color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector,
  onEdit: () -> Unit, onRemove: () -> Unit, onBegin: () -> Unit,
  onMove: (Long) -> Unit, onTrim: (Boolean, Long) -> Unit, projectDuration: Long,
  selected: Boolean = false
) {
  val density = LocalDensity.current
  val left = origin + (start * pxPerMs).toFloat()
  val right = origin + (end * pxPerMs).toFloat()
  if (right <= 0 || left >= viewport) return
  val visibleLeft = maxOf(0f, left)
  val visibleWidth = (minOf(viewport, right) - visibleLeft).coerceAtLeast(1f)
  val move by rememberUpdatedState(onMove)
  val begin by rememberUpdatedState(onBegin)
  val originalStart = rememberUpdatedState(start)
  val originalEnd = rememberUpdatedState(end)
  Box(
    Modifier.offset { IntOffset(visibleLeft.roundToInt(), 0) }.width(with(density) { visibleWidth.toDp() })
      .fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color)
      .border(if (selected) 1.5.dp else 0.dp, CapCutTeal, RoundedCornerShape(5.dp))
      .clickable(onClick = onEdit)
      .pointerInput(id, pxPerMs) {
        var initial = 0L
        var duration = 0L
        var pixels = 0f
        detectDragGesturesAfterLongPress(
          onDragStart = { begin(); initial = originalStart.value; duration = originalEnd.value - initial; pixels = 0f },
          onDrag = { change, amount ->
            change.consume()
            pixels += amount.x
            val maxStart = (projectDuration - duration).coerceAtLeast(0L)
            move((initial + pixels / pxPerMs).roundToLong().coerceIn(0L, maxStart))
          }
        )
      }.testTag("timeline_track_$id")
  ) {
    Row(Modifier.fillMaxSize().padding(horizontal = 19.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, null, Modifier.size(13.dp), tint = CapCutTextSecondary)
      Spacer(Modifier.width(5.dp))
      Text(title, Modifier.weight(1f), color = CapCutTextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Remove $title", Modifier.size(12.dp)) }
    }
    if (left >= 0) TrimHandle(true, begin) { onTrim(true, (it / pxPerMs).roundToLong()) }
    if (right <= viewport) TrimHandle(false, begin) { onTrim(false, (it / pxPerMs).roundToLong()) }
  }
}

@Composable
private fun TimelineThumbnail(source: String?, video: Boolean, timeMs: Long, modifier: Modifier) {
  val context = LocalContext.current
  val request = remember(context, source, video, timeMs) {
    source?.let {
      ImageRequest.Builder(context).data(Uri.parse(it)).size(180, 120).apply {
        if (video) {
          decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
          videoFrameMillis(timeMs)
        }
      }.build()
    }
  }
  AsyncImage(request, null, modifier, contentScale = ContentScale.Crop)
}

private fun <T> packLanes(items: List<T>, start: (T) -> Long, end: (T) -> Long): List<List<T>> {
  val lanes = mutableListOf<MutableList<T>>()
  items.sortedBy(start).forEach { item ->
    val lane = lanes.firstOrNull { end(it.last()) <= start(item) } ?: mutableListOf<T>().also { lanes.add(it) }
    lane.add(item)
  }
  return lanes
}
