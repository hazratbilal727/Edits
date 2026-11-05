package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.viewmodel.EditorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionSheet(
  uiState: EditorUiState,
  onDismiss: () -> Unit,
  onConfirm: (label: String, fps: Int, smartHdr: Boolean) -> Unit
) {
  var selectedRes by remember { mutableStateOf(uiState.resolutionSetting.label) }
  var selectedFps by remember { mutableIntStateOf(uiState.resolutionSetting.fps) }
  var smartHdr by remember { mutableStateOf(uiState.resolutionSetting.smartHdr) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = CapCutSurface,
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag("resolution_modal_sheet")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Export Quality Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CapCutTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Resolution section
      Text("RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CapCutTextSecondary)
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("720P", "1080P", "2K/4K").forEach { res ->
          val isSelected = res == selectedRes
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) CapCutTeal else CapCutSurfaceVariant)
              .clickable { selectedRes = res }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = res,
              color = if (isSelected) Color.Black else CapCutTextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Frame Rate section
      Text("FRAME RATE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CapCutTextSecondary)
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(24, 30, 60).forEach { fps ->
          val isSelected = fps == selectedFps
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) CapCutTeal else CapCutSurfaceVariant)
              .clickable { selectedFps = fps }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${fps}fps",
              color = if (isSelected) Color.Black else CapCutTextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Smart HDR toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Smart HDR", color = CapCutTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          Text("Optimize dynamic range offline", color = CapCutTextSecondary, fontSize = 11.sp)
        }
        Switch(
          checked = smartHdr,
          onCheckedChange = { smartHdr = it },
          colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CapCutTeal)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Done button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(CapCutTeal)
          .clickable {
            onConfirm(selectedRes, selectedFps, smartHdr)
          }
          .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
