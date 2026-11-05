package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.viewmodel.EditorUiState

@Composable
fun CapCutTopBar(
  uiState: EditorUiState, onBack: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit,
  onOpenResolutionSheet: () -> Unit, onExport: () -> Unit,
  onRename: (String) -> Unit, modifier: Modifier = Modifier
) {
  var renaming by remember { mutableStateOf(false) }
  Surface(color = CapCutSurface, modifier = modifier.fillMaxWidth()) {
    Column(Modifier.statusBarsPadding()) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save and close")
        }
        IconButton(onClick = onUndo, enabled = uiState.canUndo, modifier = Modifier.testTag("undo_button")) {
          Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
        }
        IconButton(onClick = onRedo, enabled = uiState.canRedo, modifier = Modifier.testTag("redo_button")) {
          Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onOpenResolutionSheet, modifier = Modifier.testTag("resolution_picker_pill")) {
          Text(uiState.resolutionSetting.label, maxLines = 1)
          Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
        }
        Button(
          onClick = onExport, enabled = uiState.clips.isNotEmpty(),
          contentPadding = PaddingValues(horizontal = 14.dp),
          modifier = Modifier.padding(end = 8.dp).testTag("export_button")
        ) { Text("Export") }
      }
      TextButton(
        onClick = { renaming = true },
        modifier = Modifier.fillMaxWidth().testTag("project_title"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
      ) {
        Text(uiState.projectTitle, Modifier.weight(1f), color = CapCutTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Default.Edit, "Rename project", Modifier.size(16.dp), tint = CapCutTextSecondary)
      }
    }
  }
  if (renaming) {
    var name by remember { mutableStateOf(uiState.projectTitle) }
    AlertDialog(
      onDismissRequest = { renaming = false }, title = { Text("Project name") },
      text = { OutlinedTextField(name, { name = it.take(80) }, singleLine = true, label = { Text("Name") }) },
      confirmButton = {
        TextButton(enabled = name.isNotBlank(), onClick = { onRename(name); renaming = false }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
    )
  }
}
