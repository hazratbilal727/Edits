package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.media.TextLayerRenderer
import com.example.model.TextTrackItem
import com.example.ui.theme.*

@Composable
fun TextToolsPanel(
  track: TextTrackItem, projectDuration: Long, editRequest: Int,
  onChange: (TextTrackItem) -> Unit, onDone: () -> Unit,
  onDuplicate: () -> Unit, onDelete: () -> Unit, onMoveLayer: (Int) -> Unit
) {
  val inputFocus = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  val isPhoto = track.photoOverlayPath != null
  var tab by remember(track.id) { mutableStateOf(if (isPhoto) "Edit" else "Style") }
  LaunchedEffect(editRequest) { if (editRequest > 0) tab = "Edit" }
  LaunchedEffect(tab, editRequest) {
    if (!isPhoto && tab == "Edit" && editRequest > 0) {
      androidx.compose.runtime.withFrameNanos {}
      inputFocus.requestFocus()
      keyboard?.show()
    }
  }
  val panelHeight = (LocalConfiguration.current.screenHeightDp * 0.27f).coerceIn(100f, 235f).dp
  Surface(color = CapCutSurface) {
    Column(Modifier.navigationBarsPadding().imePadding()) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
          (if (isPhoto) listOf("Edit", "Animate", "Timing") else listOf("Edit", "Style", "Animate", "Timing")).forEach { name ->
            TextButton(onClick = { tab = name; if (name != "Edit") { focusManager.clearFocus(); keyboard?.hide() } }) { Text(name, color = if (tab == name) CapCutTeal else CapCutTextSecondary) }
          }
        }
        IconButton(onClick = { focusManager.clearFocus(); keyboard?.hide(); onDone() }, modifier = Modifier.testTag("text_done")) { Icon(Icons.Default.Check, "Finish text editing") }
      }
      Column(
        Modifier.fillMaxWidth().heightIn(max = if (tab == "Edit") 128.dp else panelHeight).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        when (tab) {
          "Edit" -> {
            if (!isPhoto) OutlinedTextField(
              track.text, { onChange(track.copy(text = it.take(2000))) },
              modifier = Modifier.fillMaxWidth().focusRequester(inputFocus).testTag("text_content"), label = { Text("Text") }, minLines = 2, maxLines = 4
            )
            if (isPhoto) TextSlider("Opacity", track.opacity, 0f..1f) { onChange(track.copy(opacity = it)) }
            Text("Drag to move · Pinch to resize · Twist or drag the corner to rotate",
              style = MaterialTheme.typography.bodySmall, color = CapCutTextSecondary)
            Row {
              TextButton(onClick = onDuplicate) { Icon(Icons.Default.ContentCopy, null); Text("Duplicate") }
              TextButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null); Text("Delete") }
            }
          }
          "Style" -> {
            ChoiceRow("Font", listOf("Sans", "Serif", "Mono", "Condensed", "Script"),
              when (track.fontFamily) { "serif" -> "Serif"; "monospace" -> "Mono"; "sans-serif-condensed" -> "Condensed"; "cursive" -> "Script"; else -> "Sans" }
            ) { value ->
              onChange(track.copy(fontFamily = when (value) {
                "Serif" -> "serif"; "Mono" -> "monospace"; "Condensed" -> "sans-serif-condensed"; "Script" -> "cursive"; else -> "sans-serif"
              }))
            }
            ChoiceRow("Preset", listOf("Clean", "Caption", "Neon", "Label", "Headline"), track.style) { preset ->
              val clean = track.copy(style = preset, colorHex = "#FFFFFF", strokeWidth = 0f,
                shadow = false, backgroundHex = "#00000000", bold = true, italic = false)
              onChange(when (preset) {
                "Caption" -> clean.copy(strokeWidth = 2f, shadow = true)
                "Neon" -> clean.copy(colorHex = "#00E5FF", strokeWidth = 1f, shadow = true)
                "Label" -> clean.copy(colorHex = "#000000", backgroundHex = "#FFFFFF")
                "Headline" -> clean.copy(fontFamily = "sans-serif-condensed", fontSizeSp = 36f)
                else -> clean
              })
            }
            ColorChoices("Text color", track.colorHex) { onChange(track.copy(colorHex = it)) }
            var hex by remember(track.id, track.colorHex) { mutableStateOf(track.colorHex) }
            val validHex = hex.matches(Regex("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})"))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
              OutlinedTextField(
                hex, { hex = it.take(9) }, modifier = Modifier.weight(1f), label = { Text("Hex color") },
                singleLine = true, isError = !validHex
              )
              TextButton(enabled = validHex, onClick = { onChange(track.copy(colorHex = hex)) }) { Text("Apply") }
            }
            ColorChoices("Background", track.backgroundHex, true) { onChange(track.copy(backgroundHex = it)) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(track.bold, { onChange(track.copy(bold = !track.bold)) }, label = { Text("Bold") })
              FilterChip(track.italic, { onChange(track.copy(italic = !track.italic)) }, label = { Text("Italic") })
              FilterChip(track.shadow, { onChange(track.copy(shadow = !track.shadow)) }, label = { Text("Shadow") })
            }
            ChoiceRow("Alignment", listOf("left", "center", "right"), track.alignment) { onChange(track.copy(alignment = it)) }
            TextSlider("Size", track.fontSizeSp, 8f..96f) { onChange(track.copy(fontSizeSp = it)) }
            TextSlider("Outline", track.strokeWidth, 0f..5f) { onChange(track.copy(strokeWidth = it)) }
            TextSlider("Opacity", track.opacity, 0f..1f) { onChange(track.copy(opacity = it)) }
            TextSlider("Letter spacing", track.letterSpacing, -0.03f..0.3f) { onChange(track.copy(letterSpacing = it)) }
            TextSlider("Line spacing", track.lineSpacing, 0.9f..1.8f) { onChange(track.copy(lineSpacing = it)) }
            TextSlider("Scale", track.scale, 0.25f..6f) { onChange(track.copy(scale = it)) }
            TextSlider("Horizontal position", track.posX, 0.02f..0.98f) { onChange(track.copy(posX = it)) }
            TextSlider("Vertical position", track.posY, 0.02f..0.98f) { onChange(track.copy(posY = it)) }
            TextSlider("Rotation", normalizedRotation(track.rotation), -180f..180f) { onChange(track.copy(rotation = it)) }
            Row {
              TextButton(onClick = { onChange(track.copy(posX = 0.5f, posY = 0.5f, scale = 1f, rotation = 0f)) }) { Text("Center & reset transform") }
            }
            Row {
              TextButton(onClick = { onMoveLayer(-1) }) { Text("Send backward") }
              TextButton(onClick = { onMoveLayer(1) }) { Text("Bring forward") }
            }
          }
          "Animate" -> {
            ChoiceRow("In", listOf("None", "Fade", "Slide up", "Pop") + if (isPhoto) emptyList() else listOf("Typewriter"), track.entrance) { onChange(track.copy(entrance = it)) }
            ChoiceRow("Out", listOf("None", "Fade", "Slide down", "Shrink"), track.exit) { onChange(track.copy(exit = it)) }
            ChoiceRow("Loop", listOf("None", "Pulse", "Float"), track.loop) { onChange(track.copy(loop = it)) }
            TextSlider("Animation duration (ms)", track.animationDurationMs.toFloat(), 100f..1500f) {
              onChange(track.copy(animationDurationMs = it.toLong()))
            }
            Text("Tap Done and play the timeline to preview motion.", style = MaterialTheme.typography.bodySmall)
          }
          "Timing" -> {
            var start by remember(track.id, track.startMs) { mutableStateOf(track.startMs.toString()) }
            var end by remember(track.id, track.endMs) { mutableStateOf(track.endMs.toString()) }
            OutlinedTextField(start, { start = it }, label = { Text("Start (ms)") }, singleLine = true)
            OutlinedTextField(end, { end = it }, label = { Text("End (ms)") }, singleLine = true)
            val from = start.toLongOrNull()
            val to = end.toLongOrNull()
            TextButton(
              enabled = from != null && to != null && from >= 0 && to > from && to <= projectDuration,
              onClick = { onChange(track.copy(startMs = requireNotNull(from), endMs = requireNotNull(to))) }
            ) { Text("Apply timing") }
          }
        }
        Spacer(Modifier.height(10.dp))
      }
    }
  }
}

private fun normalizedRotation(rotation: Float): Float = ((rotation + 180f) % 360f + 360f) % 360f - 180f

@Composable
private fun ChoiceRow(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium, color = CapCutTextSecondary)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      choices.forEach { choice ->
        FilterChip(selected == choice, { onSelect(choice) }, label = { Text(choice.replaceFirstChar(Char::titlecase)) })
      }
    }
  }
}

@Composable
private fun ColorChoices(label: String, selected: String, transparent: Boolean = false, onSelect: (String) -> Unit) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium, color = CapCutTextSecondary)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      if (transparent) TextButton(onClick = { onSelect("#00000000") }) { Text("None") }
      listOf("#FFFFFF", "#000000", "#00E5FF", "#FFD166", "#FF4D4F", "#FF80AB", "#B388FF", "#69F0AE", "#2979FF", "#FF9100").forEach { hex ->
        Box(Modifier.size(44.dp).clickable { onSelect(hex) }.padding(5.dp).border(if (selected.equals(hex, true)) 2.dp else 1.dp,
          if (selected.equals(hex, true)) CapCutTeal else CapCutTextSecondary, CircleShape)
          .padding(4.dp).background(Color(TextLayerRenderer.parse(hex, android.graphics.Color.WHITE)), CircleShape)
          .semantics { contentDescription = "$label $hex" }.testTag("color_$hex"))
      }
    }
  }
}

@Composable
private fun TextSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
  Column {
    Text("$label · ${String.format(java.util.Locale.ROOT, "%.2f", value)}", style = MaterialTheme.typography.labelMedium)
    Slider(value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
  }
}
