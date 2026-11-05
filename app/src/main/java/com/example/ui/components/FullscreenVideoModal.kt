package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.TextTrackItem

@Composable
fun FullscreenVideoModal(
  isPlaying: Boolean,
  currentTimeMs: Long,
  activeCaptions: List<TextTrackItem>,
  onPlayPauseToggle: () -> Unit,
  onDismiss: () -> Unit
) {
  val currentCaption = activeCaptions.find { currentTimeMs in it.startMs..it.endMs }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable { onPlayPauseToggle() }
        .testTag("fullscreen_video_modal"),
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_video_preview),
        contentDescription = "Fullscreen preview",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
      )

      // Subtitle
      currentCaption?.let { cap ->
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 60.dp, start = 24.dp, end = 24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = cap.text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Close button
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp)
          .size(40.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.6f))
          .testTag("fullscreen_close_button")
      ) {
        Icon(Icons.Default.Close, contentDescription = "Exit fullscreen", tint = Color.White)
      }

      // Play/Pause indicator
      if (!isPlaying) {
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
          )
        }
      }
    }
  }
}
