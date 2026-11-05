package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.model.TransitionType

@Composable
fun TransitionsDialog(onDismiss: () -> Unit, onApply: (TransitionType, Long) -> Unit) {
  var fade by remember { mutableStateOf(true) }
  var duration by remember { mutableFloatStateOf(400f) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Between clips") },
    text = {
      EditorDialogContent {
        FilterChip(selected = !fade, onClick = { fade = false }, label = { Text("Straight cut") })
        FilterChip(selected = fade, onClick = { fade = true }, label = { Text("Fade through black") })
        if (fade) {
          Text("Fade length on each side · ${duration.toInt()} ms")
          Slider(duration, { duration = it }, valueRange = 100f..1500f)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onApply(if (fade) TransitionType.FADE_BLACK else TransitionType.NONE, duration.toLong()) }) { Text("Apply") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
