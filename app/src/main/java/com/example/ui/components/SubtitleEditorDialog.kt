package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.model.TextTrackItem

@Composable
fun SubtitleEditorDialog(
  track: TextTrackItem,
  projectDuration: Long,
  onApply: (String, Long, Long) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember(track.id) { mutableStateOf(track.text) }
  var start by remember(track.id) { mutableStateOf(track.startMs.toString()) }
  var end by remember(track.id) { mutableStateOf(track.endMs.toString()) }
  val from = start.toLongOrNull()
  val to = end.toLongOrNull()
  val valid = text.isNotBlank() && from != null && to != null && from >= 0 && to > from && to <= projectDuration
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit caption") },
    text = {
      EditorDialogContent {
        OutlinedTextField(text, { text = it }, label = { Text("Caption") }, maxLines = 4)
        OutlinedTextField(start, { start = it }, label = { Text("Start (ms)") }, singleLine = true)
        OutlinedTextField(end, { end = it }, label = { Text("End (ms)") }, singleLine = true)
      }
    },
    confirmButton = {
      TextButton(enabled = valid, onClick = { onApply(text, requireNotNull(from), requireNotNull(to)) }) { Text("Apply") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
