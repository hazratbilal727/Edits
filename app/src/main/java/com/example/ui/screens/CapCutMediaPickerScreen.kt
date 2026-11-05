package com.example.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaPickerItem
import com.example.model.VideoClip
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import java.util.UUID

@Composable
fun CapCutMediaPickerScreen(
  isPhotoModeOnly: Boolean = false,
  onClose: () -> Unit,
  onMediaSelected: (List<VideoClip>) -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(if (isPhotoModeOnly) 1 else 0) }
  val tabs = listOf("All", "Photos", "Videos")

  val deviceMediaList = remember { mutableStateListOf<MediaPickerItem>() }
  val selectedItems = remember { mutableStateListOf<MediaPickerItem>() }

  // Android Photo Picker Launcher for picking real photos/videos directly from device gallery
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      val clips = uris.mapIndexedNotNull { idx, uri ->
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val isVideo = mimeType?.startsWith("video/") == true
        val duration = when {
          isVideo -> runCatching {
            val retriever = android.media.MediaMetadataRetriever()
            try {
              retriever.setDataSource(context, uri)
              retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
              retriever.release()
            }
          }.getOrNull() ?: return@mapIndexedNotNull null
          mimeType?.startsWith("image/") == true -> 3000L
          else -> return@mapIndexedNotNull null
        }
        if (duration < 100L) return@mapIndexedNotNull null
        runCatching {
          resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        VideoClip(
          id = UUID.randomUUID().toString(),
          title = "Clip ${idx + 1}",
          endMs = duration,
          originalDurationMs = duration,
          uriString = uri.toString(),
          isVideo = isVideo
        )
      }
      if (clips.isNotEmpty()) onMediaSelected(clips)
    }
  }

  // Load real device media ONLY when user taps + and enters this picker screen
  LaunchedEffect(Unit) {
    loadLocalDeviceMedia(context) { items ->
      deviceMediaList.clear()
      deviceMediaList.addAll(items)
    }
  }

  val filteredMedia = remember(selectedTab, deviceMediaList.size) {
    when (selectedTab) {
      1 -> deviceMediaList.filter { !it.isVideo }
      2 -> deviceMediaList.filter { it.isVideo }
      else -> deviceMediaList
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CapCutBackground)
      .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextPrimary)
      }

      Text(
        text = if (isPhotoModeOnly) "Select Photo" else "Select Media",
        color = CapCutTextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )

      // Button to open system gallery picker directly
      Button(
        onClick = {
          val mediaType = if (isPhotoModeOnly) {
            ActivityResultContracts.PickVisualMedia.ImageOnly
          } else {
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
          }
          galleryLauncher.launch(PickVisualMediaRequest(mediaType))
        },
        colors = ButtonDefaults.buttonColors(containerColor = CapCutSurfaceVariant, contentColor = CapCutTeal),
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp).testTag("open_system_gallery_button")
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    // Tabs
    ScrollableTabRow(
      selectedTabIndex = selectedTab,
      containerColor = CapCutBackground,
      contentColor = CapCutTeal,
      edgePadding = 16.dp,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = CapCutTeal,
          height = 2.dp
        )
      }
    ) {
      tabs.forEachIndexed { index, tabTitle ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = {
            Text(
              text = tabTitle,
              fontSize = 14.sp,
              fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == index) CapCutTeal else CapCutTextSecondary
            )
          }
        )
      }
    }

    // Media Grid
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (filteredMedia.isEmpty()) {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CapCutTextSecondary, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No media detected in gallery yet",
            color = CapCutTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            colors = ButtonDefaults.buttonColors(containerColor = CapCutTeal, contentColor = Color.Black)
          ) {
            Text("Open Gallery Picker", fontWeight = FontWeight.Bold)
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          items(filteredMedia) { item ->
            val isSelected = selectedItems.any { it.uri == item.uri }
            val selectedIndex = selectedItems.indexOfFirst { it.uri == item.uri }

            Box(
              modifier = Modifier
                .aspectRatio(1f)
                .background(CapCutSurface)
                .clickable {
                  if (isSelected) {
                    selectedItems.removeAll { it.uri == item.uri }
                  } else {
                    selectedItems.add(item)
                  }
                }
            ) {
              AsyncImage(
                model = Uri.parse(item.uri),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )

              // Video Duration Badge
              if (item.isVideo) {
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text(text = formatDuration(item.durationMs), color = Color.White, fontSize = 9.sp)
                  }
                }
              }

              // Checkbox Badge
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) CapCutTeal else Color.Black.copy(alpha = 0.45f))
                  .border(1.5.dp, if (isSelected) CapCutTeal else Color.White, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Text(
                    text = "${selectedIndex + 1}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }
      }
    }

    // Bottom Action Bar: "Add (N)" button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(CapCutSurface)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (selectedItems.isNotEmpty()) "${selectedItems.size} selected" else "Select clips to edit",
        color = CapCutTextSecondary,
        fontSize = 13.sp
      )

      Button(
        onClick = {
          if (selectedItems.isNotEmpty()) {
            val clips = selectedItems.map { media ->
              VideoClip(
                id = UUID.randomUUID().toString(),
                title = media.name,
                startMs = 0L,
                endMs = if (media.isVideo) media.durationMs.coerceAtLeast(100L) else 3000L,
                originalDurationMs = if (media.isVideo) media.durationMs.coerceAtLeast(100L) else 3000L,
                uriString = media.uri,
                isVideo = media.isVideo
              )
            }
            if (clips.isNotEmpty()) onMediaSelected(clips)
          }
        },
        enabled = selectedItems.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
          containerColor = CapCutTeal,
          contentColor = Color.Black,
          disabledContainerColor = CapCutSurfaceVariant,
          disabledContentColor = CapCutTextSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("add_media_confirm_button")
      ) {
        Text(
          text = if (selectedItems.isNotEmpty()) "Add (${selectedItems.size})" else "Add",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }
  }
}

private fun formatDuration(ms: Long): String {
  val sec = (ms / 1000).coerceAtLeast(0)
  val m = sec / 60
  val s = sec % 60
  return String.format("%02d:%02d", m, s)
}

// Scans local media from device safely
private fun loadLocalDeviceMedia(context: Context, onResult: (List<MediaPickerItem>) -> Unit) {
  val list = mutableListOf<MediaPickerItem>()
  try {
    // 1. Query Videos
    val videoProjection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.DATE_ADDED
    )
    val videoCursor = context.contentResolver.query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      videoProjection,
      null,
      null,
      "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )
    videoCursor?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

      var count = 0
      while (cursor.moveToNext() && count < 50) {
        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: "Video"
        val dur = cursor.getLong(durCol).coerceAtLeast(1000L)
        val date = cursor.getLong(dateCol)
        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        list.add(MediaPickerItem(uri.toString(), name, dur, isVideo = true, dateAdded = date))
        count++
      }
    }

    // 2. Query Images
    val imgProjection = arrayOf(
      MediaStore.Images.Media._ID,
      MediaStore.Images.Media.DISPLAY_NAME,
      MediaStore.Images.Media.DATE_ADDED
    )
    val imgCursor = context.contentResolver.query(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      imgProjection,
      null,
      null,
      "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )
    imgCursor?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
      val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

      var count = 0
      while (cursor.moveToNext() && count < 50) {
        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: "Photo"
        val date = cursor.getLong(dateCol)
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        list.add(MediaPickerItem(uri.toString(), name, 3000L, isVideo = false, dateAdded = date))
        count++
      }
    }
  } catch (_: Exception) {}

  list.sortByDescending { it.dateAdded }
  onResult(list)
}
