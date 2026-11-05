package com.example.ui.components

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.io.File

@Suppress("DEPRECATION")
@Composable
fun VoiceoverDialog(maxDurationMs: Long, onUse: (Uri) -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val file = remember { File.createTempFile("voiceover-", ".m4a", context.filesDir) }
  var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
  var recording by remember { mutableStateOf(false) }
  var ready by remember { mutableStateOf(false) }
  var kept by remember { mutableStateOf(false) }
  var elapsed by remember { mutableLongStateOf(0L) }
  var error by remember { mutableStateOf<String?>(null) }
  fun stop() {
    val active = recorder ?: return
    try {
      active.stop()
      ready = true
    } catch (_: RuntimeException) {
      ready = false
      error = "Record for at least a second, then try again."
    } finally {
      active.release()
      recorder = null
      recording = false
    }
  }
  fun start() {
    error = null
    ready = false
    try {
      val next = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
      recorder = next
      next.setAudioSource(MediaRecorder.AudioSource.MIC)
      next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      next.setAudioEncodingBitRate(128000)
      next.setAudioSamplingRate(44100)
      next.setOutputFile(file.absolutePath)
      next.prepare()
      next.start()
      elapsed = 0L
      recording = true
    } catch (_: Exception) {
      recorder?.release()
      recorder = null
      error = "Microphone unavailable. Check its permission and try again."
    }
  }
  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) start() else error = "Microphone permission is needed to record a voiceover."
  }
  DisposableEffect(Unit) {
    onDispose {
      recorder?.let { active -> runCatching { active.stop() }; active.release() }
      if (!kept) file.delete()
    }
  }
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stop() }
  LaunchedEffect(recording) {
    if (recording) {
      val started = android.os.SystemClock.elapsedRealtime()
      while (recording) {
        delay(100)
        elapsed = android.os.SystemClock.elapsedRealtime() - started
        if (elapsed >= maxDurationMs.coerceAtMost(600000L)) stop()
      }
    }
  }
  AlertDialog(
    onDismissRequest = { stop(); onDismiss() },
    title = { Text("Voiceover") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Record from the current playhead position.")
        Text(formatTime(elapsed), style = MaterialTheme.typography.headlineMedium)
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        OutlinedButton(
          enabled = maxDurationMs > 0,
          onClick = { if (recording) stop() else permission.launch(Manifest.permission.RECORD_AUDIO) },
          modifier = Modifier.fillMaxWidth()
        ) { Text(if (recording) "Stop recording" else if (ready) "Record again" else "Start recording") }
      }
    },
    confirmButton = {
      TextButton(enabled = ready && !recording, onClick = {
        kept = true
        onUse(Uri.fromFile(file))
        onDismiss()
      }) { Text("Use recording") }
    },
    dismissButton = { TextButton(onClick = { stop(); onDismiss() }) { Text("Cancel") } }
  )
}
