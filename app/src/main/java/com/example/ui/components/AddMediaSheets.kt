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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTextSheet(
  onDismiss: () -> Unit,
  onAddText: (String) -> Unit
) {
  var textInput by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = Color(0xFF1E2026),
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Add Text / Subtitle",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = CapCutTextPrimary
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = { Text("Enter your text...", color = CapCutTextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = CapCutTeal,
          unfocusedBorderColor = Color(0xFF383B46),
          cursorColor = CapCutTeal
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_caption_text")
      )

      Button(
        onClick = {
          if (textInput.isNotBlank()) {
            onAddText(textInput)
          }
        },
        enabled = textInput.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = CapCutTeal,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("submit_text_button")
      ) {
        Text("Add to Timeline", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAudioSheet(
  onDismiss: () -> Unit,
  onSelectTrack: (String) -> Unit
) {
  val sampleSounds = listOf(
    "Peaceful Nasheed (Acoustic)" to "0:28",
    "Calm Atmosphere & Wind" to "0:45",
    "Cinematic Epic Strings" to "0:30",
    "Lo-Fi Chill Beat" to "0:25"
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = Color(0xFF1E2026),
    contentColor = CapCutTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Add Soundtrack",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = CapCutTextPrimary
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sampleSounds.forEach { (title, duration) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF2B2D35))
              .clickable { onSelectTrack(title) }
              .padding(horizontal = 14.dp, vertical = 12.dp)
              .testTag("soundtrack_item_$title"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF1B3D3D)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Audiotrack,
                  contentDescription = null,
                  tint = CapCutTeal,
                  modifier = Modifier.size(18.dp)
                )
              }
              Column {
                Text(text = title, color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = duration, color = CapCutTextSecondary, fontSize = 11.sp)
              }
            }

            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CapCutTeal),
              contentAlignment = Alignment.Center
            ) {
              Text(text = "+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          }
        }
      }
    }
  }
}
