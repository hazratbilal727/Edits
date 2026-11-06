package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdjustSettings
import com.example.model.FilterItem
import com.example.model.MainTool
import com.example.model.SubMenuType
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.viewmodel.AVAILABLE_FILTERS
import com.example.viewmodel.EditorUiState

@Composable
fun CapCutBottomBar(
  uiState: EditorUiState,
  onMainToolClick: (MainTool) -> Unit,
  onCloseSubMenu: () -> Unit,
  onSplitClip: () -> Unit,
  onTrimClip: () -> Unit,
  onCutClip: () -> Unit,
  onCropClip: () -> Unit,
  onMirrorClip: () -> Unit,
  onDeleteClip: () -> Unit,
  onDuplicateClip: () -> Unit,
  onRotateClip: () -> Unit,
  onFreezeClip: () -> Unit,
  onOpenSpeedMenu: () -> Unit,
  onOpenSpeedCurve: () -> Unit,
  onOpenKeyframes: () -> Unit,
  onOpenChromaKey: () -> Unit,
  onOpenVideoDenoise: () -> Unit,
  onOpenAutoReframe: () -> Unit,
  onOpenVolumeMenu: () -> Unit,
  onSetSpeed: (Float) -> Unit,
  onSetVolume: (Float) -> Unit,
  onSetFilter: (FilterItem) -> Unit,
  onSetFilterIntensity: (Float) -> Unit,
  onUpdateAdjust: (AdjustSettings) -> Unit,
  onResetAdjust: () -> Unit,
  onImportAudio: () -> Unit,
  onExtractAudio: () -> Unit,
  onMoveClip: (Int) -> Unit,
  onOpenClipFades: () -> Unit,
  onOpenCaptions: () -> Unit,
  onOpenSfxDialog: () -> Unit,
  onOpenVoiceoverDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(CapCutSurface)
      .navigationBarsPadding()
      .padding(vertical = 8.dp)
  ) {
    when (uiState.activeSubMenu) {
      SubMenuType.NONE -> {
        // Main Tools Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ToolIconItem(MainTool.EDIT.title, Icons.Default.ContentCut) { onMainToolClick(MainTool.EDIT) }
          ToolIconItem(MainTool.AUDIO.title, Icons.Default.Audiotrack) { onMainToolClick(MainTool.AUDIO) }
          ToolIconItem(MainTool.TEXT.title, Icons.Default.TextFields) { onMainToolClick(MainTool.TEXT) }
          ToolIconItem("Captions", Icons.Default.Subtitles, onOpenCaptions)
          ToolIconItem(MainTool.FILTERS.title, Icons.Default.AutoAwesome) { onMainToolClick(MainTool.FILTERS) }
          ToolIconItem(MainTool.ADJUST.title, Icons.Default.Tune) { onMainToolClick(MainTool.ADJUST) }
          ToolIconItem("Format", Icons.Default.AspectRatio) { onMainToolClick(MainTool.RATIO) }
          ToolIconItem(MainTool.EFFECTS.title, Icons.Default.AutoFixHigh) { onMainToolClick(MainTool.EFFECTS) }
        }
      }

      SubMenuType.CLIP_OPTIONS -> {
        // Actions for selected clip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Done / Back check
          IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
          }

          ToolIconItem("Split", Icons.Default.CallSplit, onSplitClip)
          val selectedIndex = uiState.clips.indexOfFirst { it.id == uiState.selectedClipId }
          if (selectedIndex > 0) ToolIconItem("Move Earlier", Icons.Default.ContentCut) { onMoveClip(-1) }
          if (selectedIndex >= 0 && selectedIndex < uiState.clips.lastIndex) {
            ToolIconItem("Move Later", Icons.Default.ContentCut) { onMoveClip(1) }
          }
          ToolIconItem("Trim", Icons.Default.ContentCut, onTrimClip)
          ToolIconItem("Cut", Icons.Default.ContentCut, onCutClip)
          ToolIconItem("Crop", Icons.Default.AspectRatio, onCropClip)
          ToolIconItem("Mirror", Icons.Default.AspectRatio, onMirrorClip)
          if (uiState.selectedClip?.isVideo == true) {
            ToolIconItem("Speed", Icons.Default.Speed, onOpenSpeedMenu)
            ToolIconItem("Speed Curve", Icons.Default.Speed, onOpenSpeedCurve)
            ToolIconItem("Chroma Key", Icons.Default.AutoFixHigh, onOpenChromaKey)
            ToolIconItem("Noise Reduction", Icons.Default.Tune, onOpenVideoDenoise)
            ToolIconItem("Auto Reframe", Icons.Default.AspectRatio, onOpenAutoReframe)
          }
          ToolIconItem("Keyframes", Icons.Default.AutoFixHigh, onOpenKeyframes)
          ToolIconItem("Video Fade", Icons.Default.AutoFixHigh, onOpenClipFades)
          ToolIconItem("Volume", Icons.Default.VolumeUp, onOpenVolumeMenu)
          ToolIconItem("Rotate", Icons.Default.RotateRight, onRotateClip)
          ToolIconItem("Freeze", Icons.Default.AcUnit, onFreezeClip)
          ToolIconItem("Duplicate", Icons.Default.ContentCopy, onDuplicateClip)
          ToolIconItem("Delete", Icons.Default.Delete, onDeleteClip)
        }
      }

      SubMenuType.SPEED -> {
        // Speed Adjustment Slider
        val clip = uiState.selectedClip
        var currentSpeed by remember(clip?.id, clip?.speed) { mutableStateOf(clip?.speed ?: 1f) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Clip Speed: ${String.format("%.1f", currentSpeed)}x", color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
            }
          }
          Slider(
            value = currentSpeed,
            onValueChange = { currentSpeed = it },
            onValueChangeFinished = { onSetSpeed(currentSpeed) },
            valueRange = 0.25f..4f,
            steps = 14,
            colors = SliderDefaults.colors(thumbColor = CapCutTeal, activeTrackColor = CapCutTeal)
          )
        }
      }

      SubMenuType.VOLUME -> {
        // Volume Adjustment Slider
        var currentVol by remember(uiState.selectedClipId, uiState.selectedClip?.volume) { mutableStateOf(uiState.selectedClip?.volume ?: 1f) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Clip Volume: ${(currentVol * 100).toInt()}%", color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
            }
          }
          Slider(
            value = currentVol,
            onValueChange = { currentVol = it },
            onValueChangeFinished = { onSetVolume(currentVol) },
            valueRange = 0.0f..2.0f,
            colors = SliderDefaults.colors(thumbColor = CapCutTeal, activeTrackColor = CapCutTeal)
          )
        }
      }

      SubMenuType.FILTER -> {
        // Filter Selection Cards
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Filters", color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            AVAILABLE_FILTERS.forEach { filter ->
              val isSelected = filter.id == uiState.selectedFilter.id
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSetFilter(filter) }
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C34))
                    .border(2.dp, if (isSelected) CapCutTeal else Color.Transparent, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(
                        if (filter.colorTone != 0L) Color(filter.colorTone).copy(alpha = 0.7f) else Color(0xFF555566)
                      )
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = filter.name,
                  color = if (isSelected) CapCutTeal else CapCutTextSecondary,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }
        }
      }

      SubMenuType.ADJUST -> {
        // Adjustments: Brightness, Contrast, Saturation, Warmth
        var selectedAdjustmentTab by remember { mutableStateOf("Brightness") }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              listOf("Brightness", "Contrast", "Saturation", "Warmth").forEach { tab ->
                Text(
                  text = tab,
                  color = if (selectedAdjustmentTab == tab) CapCutTeal else CapCutTextSecondary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.clickable { selectedAdjustmentTab = tab }
                )
              }
            }
            IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(30.dp)) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
            }
          }

          val currentVal = when (selectedAdjustmentTab) {
            "Brightness" -> uiState.adjustSettings.brightness
            "Contrast" -> uiState.adjustSettings.contrast
            "Saturation" -> uiState.adjustSettings.saturation
            else -> uiState.adjustSettings.warmth
          }

          var sliderValue by remember(selectedAdjustmentTab, currentVal) { mutableStateOf(currentVal) }
          Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
              val newVal = sliderValue
              val updated = when (selectedAdjustmentTab) {
                "Brightness" -> uiState.adjustSettings.copy(brightness = newVal)
                "Contrast" -> uiState.adjustSettings.copy(contrast = newVal)
                "Saturation" -> uiState.adjustSettings.copy(saturation = newVal)
                else -> uiState.adjustSettings.copy(warmth = newVal)
              }
              onUpdateAdjust(updated)
            },
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(thumbColor = CapCutTeal, activeTrackColor = CapCutTeal)
          )
        }
      }

      SubMenuType.AUDIO_OPTIONS -> {
        // Audio sub-menu
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onCloseSubMenu, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutTeal)
          }
          ToolIconItem("Import Audio", Icons.Default.MusicNote, onImportAudio)
          ToolIconItem("Extract Audio", Icons.Default.VideoLibrary, onExtractAudio)
          ToolIconItem("Sound FX", Icons.Default.Audiotrack, onOpenSfxDialog)
          ToolIconItem("Voiceover", Icons.Default.Mic, onOpenVoiceoverDialog)
        }
      }

      else -> {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.End
        ) {
          IconButton(onClick = onCloseSubMenu) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextPrimary)
          }
        }
      }
    }
  }
}

@Composable
private fun ToolIconItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable { onClick() }
      .sizeIn(minWidth = 56.dp, minHeight = 64.dp)
      .padding(vertical = 6.dp, horizontal = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(CapCutSurfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = CapCutTextPrimary,
        modifier = Modifier.size(20.dp)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      color = CapCutTextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
