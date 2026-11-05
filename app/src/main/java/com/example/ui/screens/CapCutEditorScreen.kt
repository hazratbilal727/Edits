package com.example.ui.screens

import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.LocalAudioDialog
import com.example.ui.components.SubtitleEditorDialog
import com.example.ui.components.LocalAudioPlayback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.AspectRatioOption
import com.example.model.MainTool
import com.example.model.SubMenuType
import com.example.model.speedSegments
import com.example.model.sourceTimeAt
import com.example.ui.components.ClipAnimationDialog
import com.example.ui.components.VideoCleanupDialog
import com.example.ui.components.AddTextDialog
import com.example.ui.components.CapCutBottomBar
import com.example.ui.components.CapCutTopBar
import com.example.ui.components.ExportDialog
import com.example.ui.components.ResolutionSheet
import com.example.ui.components.SfxDialog
import com.example.ui.components.TimelineView
import com.example.ui.components.TransitionsDialog
import com.example.ui.components.VideoPreviewArea
import com.example.ui.components.VoiceoverDialog
import com.example.ui.theme.CapCutBackground
import com.example.viewmodel.VideoEditorViewModel

@Composable
fun CapCutEditorScreen(
  viewModel: VideoEditorViewModel,
  onNavigateBack: () -> Unit,
  onOpenMediaPickerForAppend: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  var showCaptionTools by remember { mutableStateOf(false) }
  var subtitleId by remember { mutableStateOf<String?>(null) }
  val importSrt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) viewModel.importSubtitles(uri)
  }
  val saveSrt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
    if (uri != null) viewModel.saveSubtitles(uri)
  }
  var audioId by remember { mutableStateOf<String?>(null) }
  val importAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) viewModel.importLocalAudio(uri)
  }
  LocalAudioPlayback(uiState) { viewModel.showToast("Unable to play an imported audio track") }
  var isScrubbing by remember { mutableStateOf(false) }
  var clipDialog by remember { mutableStateOf<String?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pausePlayback(); viewModel.saveProject() }
  val leaveEditor = {
    viewModel.pausePlayback()
    viewModel.saveProject()
    onNavigateBack()
  }
  BackHandler {
    if (uiState.isFullscreen) {
      isScrubbing = false
      viewModel.toggleFullscreen()
    } else if (uiState.activeSubMenu != SubMenuType.NONE) {
      viewModel.closeSubMenu()
    } else {
      leaveEditor()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (!uiState.isFullscreen) CapCutTopBar(
        uiState = uiState,
        onBack = leaveEditor,
        onUndo = { viewModel.undo() },
        onRedo = { viewModel.redo() },
        onOpenResolutionSheet = { viewModel.openResolutionSheet(true) },
        onExport = { viewModel.triggerExport() },
        onRename = viewModel::renameProject
      )
    },
    bottomBar = {
      if (!uiState.isFullscreen) CapCutBottomBar(
        uiState = uiState,
        onMainToolClick = { tool ->
          viewModel.pausePlayback()
          when (tool) {
            MainTool.RATIO -> clipDialog = "Format"
            MainTool.EFFECTS -> clipDialog = "Effects"
            else -> viewModel.onMainToolClicked(tool)
          }
        },
        onCloseSubMenu = { viewModel.closeSubMenu() },
        onSplitClip = { viewModel.splitCurrentClip() },
        onTrimClip = { clipDialog = "Trim" },
        onCutClip = { clipDialog = "Cut" },
        onCropClip = { clipDialog = "Crop" },
        onMirrorClip = { viewModel.mirrorSelectedClip() },
        onDeleteClip = { viewModel.deleteSelectedClip() },
        onDuplicateClip = { viewModel.duplicateSelectedClip() },
        onRotateClip = { viewModel.rotateSelectedClip() },
        onReverseClip = { clipDialog = "Reverse" },
        onFreezeClip = { viewModel.freezeFrame() },
        onOpenSpeedMenu = { viewModel.openSpeedMenu() },
        onOpenSpeedCurve = {
          viewModel.pausePlayback()
          clipDialog = "Speed Curve"
        },
        onOpenKeyframes = {
          viewModel.pausePlayback()
          clipDialog = "Keyframes"
        },
        onOpenChromaKey = {
          viewModel.pausePlayback()
          clipDialog = "Chroma Key"
        },
        onOpenVideoDenoise = {
          viewModel.pausePlayback()
          clipDialog = "Noise Reduction"
        },
        onOpenAutoReframe = { clipDialog = "Auto Reframe" },
        onOpenVolumeMenu = { viewModel.openVolumeMenu() },
        onSetSpeed = { viewModel.setClipSpeed(it) },
        onSetVolume = { viewModel.setClipVolume(it) },
        onSetFilter = { viewModel.setFilter(it) },
        onSetFilterIntensity = { viewModel.setFilterIntensity(it) },
        onUpdateAdjust = { viewModel.updateAdjustSettings(it) },
        onResetAdjust = { viewModel.resetAdjustSettings() },
        onImportAudio = {
          viewModel.pausePlayback()
          importAudio.launch(arrayOf("audio/*"))
        },
        onMoveClip = viewModel::moveSelectedClip,
        onOpenClipFades = {
          viewModel.pausePlayback()
          clipDialog = "Video Fade"
        },
        onOpenCaptions = {
          viewModel.pausePlayback()
          showCaptionTools = true
        },
        onOpenSfxDialog = { viewModel.openSfxDialog() },
        onOpenVoiceoverDialog = { viewModel.openVoiceChanger() }
      )
    },
    containerColor = CapCutBackground
  ) { paddingValues ->
    val preview: @Composable (Modifier) -> Unit = { previewModifier ->

      VideoPreviewArea(
        uiState = uiState,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onToggleMute = { viewModel.toggleMute() },
        onToggleFullscreen = {
          isScrubbing = false
          viewModel.toggleFullscreen()
        },
        isScrubbing = isScrubbing,
        onBuffering = viewModel::setPreviewBuffering,
        onPlaybackError = { error ->
          viewModel.pausePlayback()
          viewModel.showToast(error)
        },
        modifier = previewModifier
      )

    }
    val timeline: @Composable () -> Unit = {
      // 2. Interactive Precision Timeline View
      if (!uiState.isFullscreen) TimelineView(
        uiState = uiState,
        onSeekTo = { viewModel.seekTo(it) },
        onScrubStarted = viewModel::pausePlayback,
        onScrubbingChange = { isScrubbing = it },
        onSelectClip = { id ->
          if (id != uiState.currentClipAtPlayhead?.id) {
            val start = uiState.clips.takeWhile { it.id != id }.sumOf { it.timelineDurationMs }
            viewModel.pausePlayback()
            viewModel.seekTo(start)
          }
          viewModel.selectClip(id)
        },
        onOpenTransition = { viewModel.openTransitionDialog(it) },
        onAppendMediaClick = onOpenMediaPickerForAppend,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onEditAudio = { id ->
          viewModel.pausePlayback()
          audioId = id
        },
        onRemoveAudio = { viewModel.removeAudioTrack(it) },
        onEditText = { id ->
          viewModel.pausePlayback()
          subtitleId = id
        },
        onRemoveText = { viewModel.removeTextTrack(it) },
        modifier = Modifier.fillMaxWidth()
      )
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(paddingValues)) {
      when {
        uiState.isFullscreen -> preview(Modifier.fillMaxSize())
        maxWidth >= 600.dp && maxWidth > maxHeight -> {
          Row(Modifier.fillMaxSize()) {
            preview(Modifier.weight(1f).fillMaxSize())
            Column(Modifier.width(maxWidth * 0.48f).fillMaxSize()) {
              timeline()
            }
          }
        }
        else -> {
          Column(Modifier.fillMaxSize()) {
            preview(Modifier.weight(1f))
            timeline()
          }
        }
      }
    }

    uiState.audioTracks.firstOrNull { it.id == audioId && it.uriString != null }?.let { track ->
      LocalAudioDialog(
        track, uiState.totalDurationMs,
        onApply = { start, end, volume ->
          viewModel.editLocalAudio(track.id, start, end, volume)
          audioId = null
        },
        onDismiss = { audioId = null }
      )
    }

    if (showCaptionTools) {
      AlertDialog(
        onDismissRequest = { showCaptionTools = false },
        title = { Text("Captions") },
        text = {
          Column {
            Text("Tap a caption in the timeline to edit its text and timing.")
            TextButton(onClick = {
              showCaptionTools = false
              viewModel.showAddTextModal(true)
            }) { Text("Add caption") }
            TextButton(enabled = uiState.totalDurationMs > 0L, onClick = {
              showCaptionTools = false
              importSrt.launch(arrayOf("*/*"))
            }) { Text("Import SRT") }
            TextButton(enabled = uiState.textTracks.isNotEmpty(), onClick = {
              showCaptionTools = false
              saveSrt.launch("captions.srt")
            }) { Text("Save SRT") }
          }
        },
        confirmButton = { TextButton(onClick = { showCaptionTools = false }) { Text("Done") } }
      )
    }

    uiState.textTracks.firstOrNull { it.id == subtitleId }?.let { track ->
      SubtitleEditorDialog(
        track, uiState.totalDurationMs,
        onApply = { text, start, end ->
          viewModel.editSubtitle(track.id, text, start, end)
          subtitleId = null
        },
        onDismiss = { subtitleId = null }
      )
    }

    val tool = clipDialog
    val clip = uiState.selectedClip
    if (tool == "Format") {
      AlertDialog(
        onDismissRequest = { clipDialog = null }, title = { Text("Canvas format") },
        text = {
          Column {
            AspectRatioOption.entries.forEach { ratio ->
              TextButton(onClick = { viewModel.setAspectRatio(ratio); clipDialog = null }) {
                Text((if (ratio == uiState.aspectRatio) "✓  " else "") + ratio.label)
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { clipDialog = null }) { Text("Done") } }
      )
    } else if (tool == "Effects") {
      AlertDialog(
        onDismissRequest = { clipDialog = null }, title = { Text("Clip effects") },
        text = {
          Column {
            Text("Choose a clip in the timeline, then apply an effect.")
            listOf("Chroma Key", "Noise Reduction", "Auto Reframe", "Video Fade", "Keyframes").forEach { effect ->
              TextButton(enabled = clip != null && (clip.isVideo || effect in listOf("Keyframes", "Video Fade")),
                onClick = { clipDialog = effect }) { Text(effect) }
            }
          }
        },
        confirmButton = { TextButton(onClick = { clipDialog = null }) { Text("Done") } }
      )
    } else if (tool == "Reverse") {
      AlertDialog(
        onDismissRequest = { clipDialog = null }, title = { Text("Reverse") },
        text = { Text("Reverse rendering is not available in this version.") },
        confirmButton = { TextButton(onClick = { clipDialog = null }) { Text("Done") } }
      )
    } else if (clip != null && (tool == "Speed Curve" || tool == "Keyframes")) {
      val timelineStart = uiState.clips.takeWhile { it.id != clip.id }.sumOf { it.timelineDurationMs }
      ClipAnimationDialog(
        clip = clip,
        sourceMs = clip.speedSegments().sourceTimeAt(uiState.currentTimeMs - timelineStart),
        editSpeed = tool == "Speed Curve",
        onSaveSpeed = viewModel::addSpeedPoint,
        onRemoveSpeed = viewModel::removeSpeedPoint,
        onSaveTransform = viewModel::addTransformKeyframe,
        onRemoveTransform = viewModel::removeTransformKeyframe,
        onDismiss = { clipDialog = null }
      )
    } else if (clip != null && (tool == "Chroma Key" || tool == "Noise Reduction")) {
      VideoCleanupDialog(
        clip = clip,
        editChroma = tool == "Chroma Key",
        onApply = viewModel::setVideoCleanup,
        onDismiss = { clipDialog = null }
      )
    } else if (clip != null && tool == "Auto Reframe") {
      AlertDialog(
        onDismissRequest = { clipDialog = null },
        title = { Text("Auto Reframe") },
        text = { Text("Follow a frontal face in the current canvas format. This replaces the clip’s existing transform keyframes.") },
        confirmButton = {
          TextButton(onClick = {
            clipDialog = null
            viewModel.autoReframeSelectedClip()
          }) { Text("Analyze") }
        },
        dismissButton = { TextButton(onClick = { clipDialog = null }) { Text("Cancel") } }
      )
    } else if (clip != null && tool == "Video Fade") {
      val maxFade = minOf(5000L, clip.timelineDurationMs).toFloat()
      var fadeIn by remember(clip.id) { mutableStateOf(clip.fadeInMs.toFloat().coerceAtMost(maxFade)) }
      var fadeOut by remember(clip.id) { mutableStateOf(clip.fadeOutMs.toFloat().coerceAtMost(maxFade)) }
      AlertDialog(
        onDismissRequest = { clipDialog = null },
        title = { Text("Video Fade") },
        text = {
          Column {
            Text("Fade in from black · ${fadeIn.toInt()} ms")
            Slider(fadeIn, { fadeIn = it }, valueRange = 0f..maxFade)
            Text("Fade out to black · ${fadeOut.toInt()} ms")
            Slider(fadeOut, { fadeOut = it }, valueRange = 0f..maxFade)
          }
        },
        confirmButton = {
          TextButton(onClick = {
            viewModel.setClipFades(fadeIn.toLong(), fadeOut.toLong())
            clipDialog = null
          }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = { clipDialog = null }) { Text("Cancel") } }
      )
    } else if (tool != null && clip != null) {
      var crop by remember(clip.id, tool) { mutableStateOf(clip.cropFraction) }
      var start by remember(clip.id, tool) { mutableStateOf(clip.startMs.toString()) }
      var end by remember(clip.id, tool) { mutableStateOf(clip.endMs.toString()) }
      val from = start.toLongOrNull()
      val to = end.toLongOrNull()
      val valid = from != null && to != null && to - from >= 100 &&
        (if (tool == "Cut") from - clip.startMs >= 100 && clip.endMs - to >= 100
        else from >= 0 && to <= clip.originalDurationMs)
      AlertDialog(
        onDismissRequest = { clipDialog = null },
        title = { Text(tool) },
        text = {
          if (tool == "Crop") {
            Column {
              Text("Center crop ${(crop * 100).toInt()}%")
              Slider(value = crop, onValueChange = { crop = it }, valueRange = 0f..0.8f)
            }
          } else {
            Column {
              Text(if (tool == "Cut") "Remove the range between these source times." else "Keep the range between these source times.")
              OutlinedTextField(start, { start = it }, label = { Text("Start (ms)") }, singleLine = true)
              OutlinedTextField(end, { end = it }, label = { Text("End (ms)") }, singleLine = true)
            }
          }
        },
        confirmButton = {
          TextButton(enabled = tool == "Crop" || valid, onClick = {
            when (tool) {
              "Crop" -> viewModel.setSelectedCrop(crop)
              "Cut" -> viewModel.cutSelectedRange(requireNotNull(from), requireNotNull(to))
              else -> viewModel.setSelectedTrim(requireNotNull(from), requireNotNull(to))
            }
            clipDialog = null
          }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = { clipDialog = null }) { Text("Cancel") } }
      )
    }

    // Modal Overlays
    if (uiState.showResolutionSheet) {
      ResolutionSheet(
        uiState = uiState,
        onDismiss = { viewModel.openResolutionSheet(false) },
        onConfirm = { label, fps, hdr -> viewModel.setResolution(label, fps, hdr) }
      )
    }

    if (uiState.showTransitionDialog) {
      TransitionsDialog(
        onDismiss = { viewModel.closeTransitionDialog() },
        onApply = { type, dur -> viewModel.applyTransition(type, dur) }
      )
    }

    if (uiState.showExportDialog) {
      ExportDialog(
        uiState = uiState,
        onDismiss = { viewModel.dismissExportDialog() }
      )
    }

    if (uiState.showSfxDialog) {
      SfxDialog(
        onDismiss = { viewModel.closeSfxDialog() },
        onSelectSfx = { title, dur -> viewModel.addSoundEffect(title, dur) }
      )
    }

    if (uiState.showVoiceChangerDialog) {
      VoiceoverDialog(
        maxDurationMs = uiState.totalDurationMs - uiState.currentTimeMs,
        onDismiss = { viewModel.closeVoiceChanger() },
        onUse = { viewModel.importLocalAudio(it) }
      )
    }

    if (uiState.showAddTextDialog) {
      AddTextDialog(
        onDismiss = { viewModel.showAddTextModal(false) },
        onConfirm = { text, colorHex, size -> viewModel.addTextTrack(text, colorHex, size) }
      )
    }
  }
}
