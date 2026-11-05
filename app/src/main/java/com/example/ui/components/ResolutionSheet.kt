package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewmodel.EditorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionSheet(uiState: EditorUiState, onDismiss: () -> Unit, onConfirm: (String, Int, Boolean) -> Unit) {
  var resolution by remember { mutableStateOf(uiState.resolutionSetting.label.takeIf { it in listOf("720P", "1080P", "4K") } ?: "1080P") }
  var fps by remember { mutableIntStateOf(uiState.resolutionSetting.fps) }
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Export settings", style = MaterialTheme.typography.headlineSmall)
      Text("Resolution", style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("720P", "1080P", "4K").forEach { option ->
          FilterChip(selected = resolution == option, onClick = { resolution = option }, label = { Text(option) })
        }
      }
      Text("Maximum frame rate", style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(24, 30, 60).forEach { option ->
          FilterChip(selected = fps == option, onClick = { fps = option }, label = { Text("$option fps") })
        }
      }
      Text("MP4 · H.264 video · AAC audio", style = MaterialTheme.typography.bodyMedium)
      Text("High resolutions need more storage and processing time. Frame rate depends on the source footage.", style = MaterialTheme.typography.bodySmall)
      Button(onClick = { onConfirm(resolution, fps, false) }, modifier = Modifier.fillMaxWidth()) { Text("Apply settings") }
      Spacer(Modifier.height(12.dp))
    }
  }
}
