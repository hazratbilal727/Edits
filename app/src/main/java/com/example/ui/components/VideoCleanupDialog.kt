package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.model.ChromaKey
import com.example.model.VideoClip

@Composable
fun VideoCleanupDialog(
  clip: VideoClip,
  editChroma: Boolean,
  onApply: (ChromaKey?, Float) -> Unit,
  onDismiss: () -> Unit
) {
  var key by remember(clip.id) { mutableStateOf(clip.chromaKey ?: ChromaKey()) }
  var strength by remember(clip.id) { mutableFloatStateOf(clip.videoDenoise) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (editChroma) "Chroma Key" else "Video Noise Reduction") },
    text = {
      EditorDialogContent {
        if (editChroma) {
          Text("Screen color")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !key.blue, onClick = { key = key.copy(blue = false) }, label = { Text("Green") })
            FilterChip(selected = key.blue, onClick = { key = key.copy(blue = true) }, label = { Text("Blue") })
          }
          Text("Replacement")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !key.whiteBackground,
              onClick = { key = key.copy(whiteBackground = false) },
              label = { Text("Black") }
            )
            FilterChip(
              selected = key.whiteBackground,
              onClick = { key = key.copy(whiteBackground = true) },
              label = { Text("White") }
            )
          }
          Text("Threshold ${(key.threshold * 100).toInt()}%")
          Slider(value = key.threshold, onValueChange = { key = key.copy(threshold = it) }, valueRange = 0.05f..0.8f)
        } else {
          Text("Smooths fine grain in the video while preserving stronger edges.")
          Text("Strength ${(strength * 100).toInt()}%")
          Slider(value = strength, onValueChange = { strength = it })
        }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        onApply(if (editChroma) key else clip.chromaKey, if (editChroma) clip.videoDenoise else strength)
        onDismiss()
      }) { Text("Apply") }
    },
    dismissButton = {
      Row {
        if ((editChroma && clip.chromaKey != null) || (!editChroma && clip.videoDenoise > 0f)) {
          TextButton(onClick = {
            onApply(if (editChroma) null else clip.chromaKey, if (editChroma) clip.videoDenoise else 0f)
            onDismiss()
          }) { Text("Reset") }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}
