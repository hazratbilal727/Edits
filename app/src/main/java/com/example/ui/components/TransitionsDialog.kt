package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.TransitionType
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionsDialog(
  onDismiss: () -> Unit,
  onApply: (TransitionType, Long) -> Unit
) {
  var selectedType by remember { mutableStateOf(TransitionType.DISSOLVE) }
  var durationSec by remember { mutableFloatStateOf(0.5f) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CapCutSurface,
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag("transitions_dialog")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Select Transition", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CapCutTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Transition Types Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(160.dp)
      ) {
        items(TransitionType.entries.toTypedArray()) { type ->
          val isSelected = type == selectedType
          Box(
            modifier = Modifier
              .height(65.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) CapCutTeal.copy(alpha = 0.2f) else CapCutSurfaceVariant)
              .border(1.5.dp, if (isSelected) CapCutTeal else Color.Transparent, RoundedCornerShape(8.dp))
              .clickable { selectedType = type }
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = type.displayName,
              color = if (isSelected) CapCutTeal else CapCutTextPrimary,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Duration Slider
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Duration", color = CapCutTextSecondary, fontSize = 12.sp)
        Text("${String.format("%.1f", durationSec)}s", color = CapCutTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
      Slider(
        value = durationSec,
        onValueChange = { durationSec = it },
        valueRange = 0.2f..2.0f,
        colors = SliderDefaults.colors(thumbColor = CapCutTeal, activeTrackColor = CapCutTeal)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Apply button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(CapCutTeal)
          .clickable {
            onApply(selectedType, (durationSec * 1000).toLong())
          }
          .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Apply Transition", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
