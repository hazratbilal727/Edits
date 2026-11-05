package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.viewmodel.EditorUiState

@Composable
fun CapCutTopBar(
  uiState: EditorUiState,
  onBack: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onOpenResolutionSheet: () -> Unit,
  onExport: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.statusBars)
      .height(56.dp)
      .background(CapCutSurface)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Back arrow
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .size(40.dp)
        .testTag("editor_back_button")
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = CapCutTextPrimary
      )
    }

    // Undo & Redo buttons
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onUndo,
        enabled = uiState.canUndo,
        modifier = Modifier
          .size(36.dp)
          .testTag("undo_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Undo,
          contentDescription = "Undo",
          tint = if (uiState.canUndo) CapCutTextPrimary else CapCutTextSecondary.copy(alpha = 0.4f)
        )
      }

      IconButton(
        onClick = onRedo,
        enabled = uiState.canRedo,
        modifier = Modifier
          .size(36.dp)
          .testTag("redo_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Redo,
          contentDescription = "Redo",
          tint = if (uiState.canRedo) CapCutTextPrimary else CapCutTextSecondary.copy(alpha = 0.4f)
        )
      }
    }

    // Right Action: Resolution pill + Export Button
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Resolution Pill (e.g. 1080P v)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.12f))
          .clickable { onOpenResolutionSheet() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("resolution_picker_pill"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${uiState.resolutionSetting.label} ${uiState.resolutionSetting.fps}",
            color = CapCutTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.width(2.dp))
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Resolution Settings",
            tint = CapCutTextPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Export Button (Cyan rounded button)
      Button(
        onClick = onExport,
        colors = ButtonDefaults.buttonColors(
          containerColor = CapCutTeal,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier
          .height(34.dp)
          .testTag("export_button")
      ) {
        Icon(
          imageVector = Icons.Default.FileDownload,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Export",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
