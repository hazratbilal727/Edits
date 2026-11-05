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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary

data class SfxItem(val title: String, val durationMs: Long, val tag: String)

val OFFLINE_SFX_LIST = listOf(
  SfxItem("Camera Shutter", 800L, "Pop"),
  SfxItem("Pop Bubble", 500L, "UI"),
  SfxItem("Whoosh Fast", 1200L, "Motion"),
  SfxItem("Ding Bell", 1500L, "Chime"),
  SfxItem("Applause Crowd", 3500L, "Ambience"),
  SfxItem("Keyboard Typing", 2500L, "Office"),
  SfxItem("Laser Beam", 900L, "Game"),
  SfxItem("Success Fanfare", 3000L, "Music")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SfxDialog(
  onDismiss: () -> Unit,
  onSelectSfx: (title: String, durationMs: Long) -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CapCutSurface,
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Offline Sound Effects", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CapCutTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(240.dp)
      ) {
        items(OFFLINE_SFX_LIST) { sfx ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CapCutSurfaceVariant)
              .clickable { onSelectSfx(sfx.title, sfx.durationMs) }
              .padding(12.dp)
          ) {
            Column {
              Text(text = sfx.title, color = CapCutTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = "${sfx.tag} • ${String.format("%.1fs", sfx.durationMs / 1000f)}", color = CapCutTeal, fontSize = 11.sp)
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

val VOICE_EFFECTS = listOf("Normal", "Deep Male", "Chipmunk", "Robot Synth", "Echo Cave", "Megaphone", "Studio Mic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerDialog(
  onDismiss: () -> Unit,
  onSelectEffect: (String) -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CapCutSurface,
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Voice Changer Effects", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CapCutTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(200.dp)
      ) {
        items(VOICE_EFFECTS) { effect ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CapCutSurfaceVariant)
              .clickable { onSelectEffect(effect) }
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(text = effect, color = CapCutTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTextDialog(
  onDismiss: () -> Unit,
  onConfirm: (text: String, colorHex: String, fontSizeSp: Float) -> Unit
) {
  var textInput by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf("#FFFFFF") }
  val colors = listOf("#FFFFFF", "#00E5FF", "#FFD166", "#FF4D4F", "#9D4EDD", "#00FF66")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = CapCutSurface,
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Add Text Overlay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CapCutTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = { Text("Enter your caption or title...", color = CapCutTextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CapCutTeal,
          unfocusedBorderColor = CapCutSurfaceVariant,
          focusedTextColor = CapCutTextPrimary,
          unfocusedTextColor = CapCutTextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text("TEXT COLOR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CapCutTextSecondary)
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { hex ->
          val isSelected = hex == selectedColorHex
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(android.graphics.Color.parseColor(hex)))
              .border(2.dp, if (isSelected) CapCutTeal else Color.Transparent, CircleShape)
              .clickable { selectedColorHex = hex }
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(if (textInput.isNotBlank()) CapCutTeal else CapCutSurfaceVariant)
          .clickable(enabled = textInput.isNotBlank()) {
            onConfirm(textInput.trim(), selectedColorHex, 26f)
          }
          .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Add to Video", color = if (textInput.isNotBlank()) Color.Black else CapCutTextSecondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
