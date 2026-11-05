package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.viewmodel.EditorUiState

@Composable
fun ExportDialog(
  uiState: EditorUiState,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val exportPath = uiState.exportedFile
  val saveVideo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
    if (uri != null && exportPath != null) {
      scope.launch {
        val saved = withContext(Dispatchers.IO) {
          runCatching {
            File(exportPath).inputStream().use { input ->
              checkNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                input.copyTo(output)
              }
            }
          }.isSuccess
        }
        Toast.makeText(context, if (saved) "Video saved" else "Could not save video", Toast.LENGTH_SHORT).show()
      }
    }
  }
  Dialog(onDismissRequest = {
    onDismiss()
  }) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(CapCutSurface)
        .padding(24.dp)
        .fillMaxWidth()
        .testTag("export_progress_dialog"),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        if (!uiState.isExportFinished) {
          // Exporting in progress
          Text(
            text = "Exporting Video...",
            color = CapCutTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${uiState.resolutionSetting.label} • ${uiState.resolutionSetting.fps} FPS • Offline Render",
            color = CapCutTextSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(24.dp))

          LinearProgressIndicator(
            progress = { uiState.exportProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = CapCutTeal,
            trackColor = Color(0xFF2C2C34)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "${(uiState.exportProgress * 100).toInt()}%",
            color = CapCutTeal,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          Button(onClick = onDismiss) { Text("Cancel") }
        } else {
          // Export Finished
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(CapCutTeal.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Success",
              tint = CapCutTeal,
              modifier = Modifier.size(42.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Export Complete!",
            color = CapCutTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Save a copy to a location you choose.",
            color = CapCutTextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            enabled = exportPath != null,
            onClick = { saveVideo.launch(File(requireNotNull(exportPath)).name) }
          ) { Text("Save MP4") }
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = CapCutTeal, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("export_done_button")
          ) {
            Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  }
}
