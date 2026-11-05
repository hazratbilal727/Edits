package com.example.ui.screens

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.model.VideoClip
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun CapCutMediaPickerScreen(isPhotoModeOnly: Boolean, onClose: () -> Unit, onMediaSelected: (List<VideoClip>) -> Unit) {
  val context = LocalContext.current
  var sources by rememberSaveable { mutableStateOf(emptyList<String>()) }
  var started by rememberSaveable { mutableStateOf(false) }
  var clips by remember { mutableStateOf(emptyList<VideoClip>()) }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
    if (uris.isNotEmpty()) sources = (sources + uris.map(Uri::toString)).distinct()
  }
  val choose = {
    picker.launch(PickVisualMediaRequest(
      if (isPhotoModeOnly) ActivityResultContracts.PickVisualMedia.ImageOnly
      else ActivityResultContracts.PickVisualMedia.ImageAndVideo
    ))
  }
  LaunchedEffect(Unit) { if (!started) { started = true; choose() } }
  LaunchedEffect(sources) {
    loading = true
    error = null
    val imported = withContext(Dispatchers.IO) {
      sources.mapNotNull { source ->
        try {
          val uri = Uri.parse(source)
          val resolver = context.contentResolver
          runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
          val mime = resolver.getType(uri).orEmpty()
          val video = mime.startsWith("video/")
          require(video || mime.startsWith("image/"))
          val duration = if (video) {
            val retriever = MediaMetadataRetriever()
            try {
              retriever.setDataSource(context, uri)
              requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull())
            } finally { retriever.release() }
          } else 3000L
          require(duration >= 100)
          val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
          } ?: if (video) "Video" else "Photo"
          VideoClip(UUID.randomUUID().toString(), name, endMs = duration, originalDurationMs = duration, uriString = source, isVideo = video)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
          throw cancel
        } catch (_: Exception) { null }
      }
    }
    clips = imported
    loading = false
    if (imported.size != sources.size) error = "Some files could not be read. Try selecting them again."
  }
  Scaffold(
    containerColor = CapCutBackground,
    topBar = {
      Row(Modifier.statusBarsPadding().fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Column(Modifier.weight(1f)) {
          Text("Choose your media", style = MaterialTheme.typography.titleLarge)
          Text("Files appear in selection order", style = MaterialTheme.typography.bodySmall, color = CapCutTextSecondary)
        }
        IconButton(onClick = choose) { Icon(Icons.Default.Add, "Choose media") }
      }
    },
    bottomBar = {
      Surface(color = CapCutSurface) {
        Column(Modifier.navigationBarsPadding().fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
          Button(
            onClick = { onMediaSelected(clips) }, enabled = clips.isNotEmpty() && !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_media_confirm_button")
          ) { Text(if (loading) "Reading files…" else "Add ${clips.size} ${if (clips.size == 1) "clip" else "clips"}") }
        }
      }
    }
  ) { padding ->
    if (sources.isEmpty()) {
      Column(
        Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("Bring your story in", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Select videos and photos to begin.", color = CapCutTextSecondary)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = choose) { Text("Browse phone") }
      }
    } else if (loading) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
      LazyVerticalGrid(
        GridCells.Adaptive(120.dp), Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(clips, key = { _, clip -> clip.uriString.orEmpty() }) { index, clip ->
          Box(Modifier.aspectRatio(0.85f).clip(RoundedCornerShape(12.dp)).background(CapCutSurface)) {
            val request = remember(clip.uriString) {
              ImageRequest.Builder(context).data(Uri.parse(clip.uriString)).apply {
                if (clip.isVideo) decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
              }.build()
            }
            AsyncImage(request, clip.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Text(
              "${index + 1}", Modifier.align(Alignment.TopStart).padding(8.dp).background(CapCutBackground).padding(6.dp),
              color = CapCutTeal
            )
            IconButton(onClick = { sources = sources - clip.uriString }, modifier = Modifier.align(Alignment.TopEnd)) {
              Icon(Icons.Default.Close, "Remove ${clip.title}", tint = CapCutTeal)
            }
            Text(
              clip.title, Modifier.align(Alignment.BottomStart).fillMaxWidth().background(CapCutBackground.copy(alpha = 0.85f)).padding(8.dp),
              maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }
  }
}
