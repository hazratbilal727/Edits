package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.TransformKeyframe
import com.example.model.VideoClip
import com.example.model.speedSegments
import com.example.model.transformAt
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun ClipAnimationDialog(
  clip: VideoClip,
  sourceMs: Long,
  editSpeed: Boolean,
  onSaveSpeed: (Long, Float) -> Unit,
  onRemoveSpeed: (Long) -> Unit,
  onSaveTransform: (TransformKeyframe) -> Unit,
  onRemoveTransform: (Long) -> Unit,
  onDismiss: () -> Unit
) {
  var sourceOffset by remember(clip.id) {
    mutableFloatStateOf((sourceMs - clip.startMs).coerceIn(0L, clip.endMs - clip.startMs - 1).toFloat())
  }
  val source = clip.startMs + sourceOffset.roundToLong()
  val transform = clip.transformAt(source)
  val panLimit = maxOf(0.5f, kotlin.math.abs(transform.x), kotlin.math.abs(transform.y))
  val segment = clip.speedSegments().last { it.startMs <= source }
  var rate by remember(clip.id, source) { mutableFloatStateOf(segment.rate) }
  var scale by remember(clip.id, source) { mutableFloatStateOf(transform.scale) }
  var x by remember(clip.id, source) { mutableFloatStateOf(transform.x) }
  var y by remember(clip.id, source) { mutableFloatStateOf(transform.y) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (editSpeed) "Speed Curve" else "Transform Keyframes") },
    text = {
      EditorDialogContent {
        Text("Source time ${seconds(source)} s")
        Slider(
          value = sourceOffset,
          onValueChange = { sourceOffset = it },
          valueRange = 0f..(clip.endMs - clip.startMs - 1).coerceAtLeast(1).toFloat()
        )

        if (editSpeed) {
          Text("Speed changes at each point and holds until the next point.")
          SliderLabel("Rate", "${decimal(rate)}×", rate, 0.25f..4f) { rate = it }
          clip.speedPoints.filter { it.sourceMs in clip.startMs until clip.endMs }.forEach { point ->
            PointRow("${seconds(point.sourceMs)} s · ${decimal(point.rate)}×") { onRemoveSpeed(point.sourceMs) }
          }
        } else {
          Text("Zoom and position interpolate between keyframes.")
          SliderLabel("Zoom", "${decimal(scale)}×", scale, 0.5f..maxOf(6f, transform.scale)) { scale = it }
          SliderLabel("Horizontal", "${(x * 100).toInt()}%", x, -panLimit..panLimit) { x = it }
          SliderLabel("Vertical", "${(y * 100).toInt()}%", y, -panLimit..panLimit) { y = it }
          clip.transformKeyframes.filter { it.sourceMs in clip.startMs until clip.endMs }.forEach { keyframe ->
            PointRow("${seconds(keyframe.sourceMs)} s · ${decimal(keyframe.scale)}×") {
              onRemoveTransform(keyframe.sourceMs)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        if (editSpeed) onSaveSpeed(source, rate)
        else onSaveTransform(TransformKeyframe(source, scale, x, y))
        onDismiss()
      }) { Text("Save point") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
  )
}

@Composable
private fun SliderLabel(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
  Text("$label  $valueLabel")
  Slider(value = value, onValueChange = onChange, valueRange = range)
}

@Composable
private fun PointRow(label: String, onRemove: () -> Unit) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label)
    TextButton(onClick = onRemove) { Text("Remove") }
  }
}

private fun seconds(ms: Long): String = decimal(ms / 1000f)
private fun decimal(value: Float): String = String.format(Locale.US, "%.2f", value)
