package com.example.ui.components

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.media.TextLayerRenderer
import com.example.model.TextTrackItem
import com.example.ui.theme.CapCutTeal
import kotlin.math.*

@Composable
fun EditableTextOverlay(
  tracks: List<TextTrackItem>, selectedId: String?, timeMs: Long, playing: Boolean,
  onSelect: (String?) -> Unit, onEdit: (String) -> Unit,
  onGestureStart: () -> Unit, onTransform: (TextTrackItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val renderer = remember { TextLayerRenderer() }
  val latestTracks by rememberUpdatedState(tracks)
  val latestTime by rememberUpdatedState(timeMs)
  val latestSelected by rememberUpdatedState(selectedId)
  val select by rememberUpdatedState(onSelect)
  val edit by rememberUpdatedState(onEdit)
  val begin by rememberUpdatedState(onGestureStart)
  val transform by rememberUpdatedState(onTransform)

  Canvas(
    modifier.testTag("text_layer_canvas").pointerInput(renderer) {
      var lastTapId: String? = null
      var lastTapTime = 0L
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val visible = latestTracks.filter { latestTime in it.startMs until it.endMs }
        val hit = visible.asReversed().firstOrNull { track ->
          val point = renderer.localPoint(track, PointF(down.position.x, down.position.y), size.width, size.height)
          val box = renderer.geometry(track, size.width)
          abs(point.x) <= box.width / 2 + 18.dp.toPx() / track.scale &&
            abs(point.y) <= box.height / 2 + 18.dp.toPx() / track.scale
        }
        if (hit != null) {
          down.consume()
          select(hit.id)
          val local = renderer.localPoint(hit, PointF(down.position.x, down.position.y), size.width, size.height)
          val box = renderer.geometry(hit, size.width)
          val handle = hit.id == latestSelected &&
            abs(local.x - box.width / 2) < 24.dp.toPx() / hit.scale &&
            abs(local.y - box.height / 2) < 24.dp.toPx() / hit.scale
          var draft = hit
          var changed = false
          val center = Offset(hit.posX * size.width, hit.posY * size.height)
          val initialVector = down.position - center
          val initialDistance = initialVector.getDistance().coerceAtLeast(1f)
          val initialAngle = atan2(initialVector.y, initialVector.x)
          while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            val rotation = event.calculateRotation()
            if (pan.getDistance() > 0.2f || abs(zoom - 1f) > 0.001f || abs(rotation) > 0.1f) {
              if (!changed) { begin(); changed = true }
              draft = if (handle && event.changes.count { it.pressed } == 1) {
                val vector = event.changes.first { it.pressed }.position - center
                val angle = atan2(vector.y, vector.x)
                hit.copy(
                  scale = (hit.scale * vector.getDistance() / initialDistance).coerceIn(0.25f, 6f),
                  rotation = (hit.rotation + Math.toDegrees((angle - initialAngle).toDouble()).toFloat()) % 360f
                )
              } else {
                val x = (draft.posX + pan.x / size.width).coerceIn(0.02f, 0.98f)
                val y = (draft.posY + pan.y / size.height).coerceIn(0.02f, 0.98f)
                draft.copy(
                  posX = if (abs(x - 0.5f) < 0.008f) 0.5f else x,
                  posY = if (abs(y - 0.5f) < 0.008f) 0.5f else y,
                  scale = (draft.scale * zoom).coerceIn(0.25f, 6f),
                  rotation = (draft.rotation + rotation) % 360f
                )
              }
              transform(draft)
            }
            event.changes.forEach { it.consume() }
          }
          if (!changed) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (lastTapId == hit.id && now - lastTapTime < 350) edit(hit.id)
            lastTapId = hit.id
            lastTapTime = now
          }
        } else if (latestSelected != null) {
          down.consume()
          select(null)
        }
      }
    }
  ) {
    val visible = tracks.filter { timeMs in it.startMs until it.endMs }
    visible.forEach { renderer.draw(drawContext.canvas.nativeCanvas, it, timeMs, !playing && it.id == selectedId) }
    val selected = visible.firstOrNull { it.id == selectedId }
    if (selected != null && !playing) {
      val box = renderer.geometry(selected, size.width.toInt())
      val center = Offset(selected.posX * size.width, selected.posY * size.height)
      val width = box.width * selected.scale
      val height = box.height * selected.scale
      rotate(selected.rotation, center) {
        drawRect(CapCutTeal, Offset(center.x - width / 2, center.y - height / 2),
          androidx.compose.ui.geometry.Size(width, height), style = Stroke(1.5.dp.toPx()))
        val corner = Offset(center.x + width / 2, center.y + height / 2)
        drawCircle(Color.Black, 13.dp.toPx(), corner)
        drawCircle(CapCutTeal, 13.dp.toPx(), corner, style = Stroke(2.dp.toPx()))
        drawLine(CapCutTeal, corner - Offset(5.dp.toPx(), 5.dp.toPx()), corner + Offset(5.dp.toPx(), 5.dp.toPx()), 2.dp.toPx())
      }
      if (selected.posX == 0.5f) drawLine(CapCutTeal.copy(alpha = 0.4f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
      if (selected.posY == 0.5f) drawLine(CapCutTeal.copy(alpha = 0.4f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
    }
  }
}
