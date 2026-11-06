package com.example.viewmodel

import android.app.Application
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import com.example.data.AppDatabase
import com.example.data.ProjectContent
import com.example.data.ProjectCodec
import com.example.data.ProjectEntity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import com.example.media.OfflineComposition
import com.example.media.reframeFaces
import java.io.File
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AdjustSettings
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackItem
import com.example.model.FilterItem
import com.example.model.MainTool
import com.example.model.ResolutionSetting
import com.example.model.SubMenuType
import com.example.model.TextTrackItem
import com.example.model.SubRip
import com.example.model.SubtitleCue
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.ChromaKey
import com.example.model.SpeedPoint
import com.example.model.TransformKeyframe
import com.example.model.speedSegments
import com.example.model.sourceTimeAt
import com.example.model.timelineTimeAt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(kotlinx.coroutines.FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
class VideoEditorViewModel(application: Application, private val savedStateHandle: SavedStateHandle = SavedStateHandle()) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(EditorUiState())
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private var exportJob: Job? = null
  private var exporter: Transformer? = null
  private var exportFile: File? = null
  private var reframeJob: Job? = null
  private var previewBuffering = false
  private var playbackJob: Job? = null
  private val undoStack = mutableListOf<EditorUiState>()
  private val redoStack = mutableListOf<EditorUiState>()

  private val toneGenerator by lazy {
    try {
      ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (_: Exception) {
      null
    }
  }

  private val projectDao = AppDatabase.getDatabase(application).projectDao()

  init {
    savedStateHandle.get<String>("projectId")?.let { openProject(it) {} }
    viewModelScope.launch {
      _uiState.map(ProjectContent::from).distinctUntilChanged().debounce(600).collect { content ->
        if (content != null) persistProject(content)
      }
    }
  }

  private suspend fun persistProject(content: ProjectContent) {
    try {
      val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { ProjectCodec.encode(content) }
      projectDao.insertProject(ProjectEntity(
        content.id, content.title, content.clips.sumOf { it.timelineDurationMs },
        content.coverUri ?: content.clips.firstOrNull()?.uriString, content.clips.size,
        content.aspectRatio.name, System.currentTimeMillis(), json
      ))
    } catch (error: kotlinx.coroutines.CancellationException) {
      throw error
    } catch (_: Exception) {
      showToast("Could not save this project. Check device storage.")
    }
  }

  fun saveProject() {
    val content = ProjectContent.from(_uiState.value) ?: return
    viewModelScope.launch { persistProject(content) }
  }

  fun renameProject(title: String) {
    val trimmed = title.trim().take(80)
    if (trimmed.isEmpty()) return
    saveSnapshot()
    _uiState.update { it.copy(projectTitle = trimmed) }
  }

  fun openProject(id: String, onLoaded: () -> Unit) {
    viewModelScope.launch {
      try {
        val entity = requireNotNull(projectDao.getProjectById(id))
        val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { ProjectCodec.decode(entity.clipsJson) }
        pausePlayback()
        undoStack.clear()
        redoStack.clear()
        savedStateHandle["projectId"] = content.id
        _uiState.value = content.editorState()
        onLoaded()
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        showToast("This project could not be opened. Its saved media or edit data may be unavailable.")
      }
    }
  }

  private fun saveSnapshot() {
    undoStack.add(_uiState.value.copy())
    if (undoStack.size > 20) undoStack.removeAt(0)
    redoStack.clear()
    updateUndoRedoStatus()
  }

  private fun updateUndoRedoStatus() {
    _uiState.update {
      it.copy(
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty()
      )
    }
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val current = _uiState.value
    redoStack.add(current)
    val previous = undoStack.removeAt(undoStack.lastIndex)
    pausePlayback()
    _uiState.value = previous.copy(isPlaying = false, showExportDialog = false)
    updateUndoRedoStatus()
    showToast("Undo performed")
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val current = _uiState.value
    undoStack.add(current)
    val next = redoStack.removeAt(redoStack.lastIndex)
    pausePlayback()
    _uiState.value = next.copy(isPlaying = false, showExportDialog = false)
    updateUndoRedoStatus()
    showToast("Redo performed")
  }

  fun loadClips(newClips: List<VideoClip>, projectTitle: String = "My Edit") {
    val id = UUID.randomUUID().toString()
    savedStateHandle["projectId"] = id
    pausePlayback()
    undoStack.clear()
    redoStack.clear()
    _uiState.update { previous ->
      EditorUiState(
        currentProjectId = id, projectTitle = projectTitle, clips = newClips,
        totalDurationMs = calculateTotalDuration(newClips),
        selectedClipId = null,
        resolutionSetting = previous.resolutionSetting
      )
    }
  }

  fun appendClips(moreClips: List<VideoClip>) {
    saveSnapshot()
    val updated = _uiState.value.clips + moreClips
    val total = calculateTotalDuration(updated)
    _uiState.update {
      it.copy(
        clips = updated,
        totalDurationMs = total
      )
    }
    showToast("Added ${moreClips.size} media clips")
  }

  private fun calculateTotalDuration(clips: List<VideoClip>): Long {
    return clips.sumOf { it.timelineDurationMs }
  }

  fun togglePlayPause() {
    if (_uiState.value.isPlaying) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  fun setPreviewBuffering(buffering: Boolean) {
    previewBuffering = buffering
  }

  fun startPlayback() {
    val state = _uiState.value
    if (state.totalDurationMs <= 0L) return
    playbackJob?.cancel()
    _uiState.update {
      it.copy(isPlaying = true, currentTimeMs = if (it.currentTimeMs >= it.totalDurationMs) 0L else it.currentTimeMs)
    }
    playbackJob = viewModelScope.launch {
      var lastTick = android.os.SystemClock.elapsedRealtime()
      while (_uiState.value.isPlaying) {
        delay(16)
        val tick = android.os.SystemClock.elapsedRealtime()
        val elapsed = tick - lastTick
        lastTick = tick
        if (!previewBuffering) {
          _uiState.update {
            val position = (it.currentTimeMs + elapsed).coerceAtMost(it.totalDurationMs)
            it.copy(currentTimeMs = position, isPlaying = position < it.totalDurationMs)
          }
        }
      }
    }
  }

  fun pausePlayback() {
    playbackJob?.cancel()
    _uiState.update { it.copy(isPlaying = false) }
  }

  fun seekTo(timeMs: Long) {
    val bounded = timeMs.coerceIn(0L, _uiState.value.totalDurationMs)
    _uiState.update { it.copy(currentTimeMs = bounded) }
  }

  fun scrubRelative(deltaMs: Long) {
    seekTo(_uiState.value.currentTimeMs + deltaMs)
  }

  fun toggleMute() {
    _uiState.update { it.copy(isMuted = !it.isMuted) }
    showToast(if (_uiState.value.isMuted) "Project muted" else "Project unmuted")
  }

  fun selectClip(clipId: String) {
    _uiState.update {
      it.copy(
        selectedClipId = clipId,
        selectedTextId = null,
        activeSubMenu = SubMenuType.CLIP_OPTIONS,
        clips = it.clips.map { c -> c.copy(isSelected = c.id == clipId) }
      )
    }
  }

  fun deselectClip() {
    _uiState.update {
      it.copy(
        selectedClipId = null,
        activeSubMenu = SubMenuType.NONE,
        clips = it.clips.map { c -> c.copy(isSelected = false) }
      )
    }
  }

  fun onMainToolClicked(tool: MainTool) {
    pausePlayback()
    when (tool) {
      MainTool.EDIT -> {
        val selected = _uiState.value.selectedClipId ?: _uiState.value.clips.firstOrNull()?.id
        _uiState.update {
          it.copy(
            selectedClipId = selected,
            activeSubMenu = SubMenuType.CLIP_OPTIONS
          )
        }
      }
      MainTool.AUDIO -> {
        _uiState.update { it.copy(activeSubMenu = SubMenuType.AUDIO_OPTIONS) }
      }
      MainTool.TEXT -> {
        _uiState.update { it.copy(showAddTextDialog = true) }
      }
      MainTool.FILTERS -> {
        _uiState.update { it.copy(activeSubMenu = SubMenuType.FILTER) }
      }
      MainTool.ADJUST -> {
        _uiState.update { it.copy(activeSubMenu = SubMenuType.ADJUST) }
      }
      MainTool.RATIO -> {
        cycleAspectRatio()
      }
      MainTool.EFFECTS -> {
        _uiState.update { it.copy(showVoiceChangerDialog = true) }
      }
      MainTool.CANVAS -> {
        showToast("Canvas background set to fit")
      }
    }
  }

  fun closeSubMenu() {
    _uiState.update { it.copy(activeSubMenu = SubMenuType.NONE) }
  }

  fun splitCurrentClip() {
    val state = _uiState.value
    val currentTime = state.currentTimeMs
    val clips = state.clips.toMutableList()

    var accumulated = 0L
    var targetIndex = -1
    for (i in clips.indices) {
      val clipDuration = clips[i].timelineDurationMs
      if (currentTime > accumulated && currentTime < accumulated + clipDuration) {
        targetIndex = i
        break
      }
      accumulated += clipDuration
    }

    if (targetIndex != -1) {
      val target = clips[targetIndex]
      val firstEnd = target.speedSegments().sourceTimeAt(currentTime - accumulated)
      if (firstEnd - target.startMs < 100 || target.endMs - firstEnd < 100) {
        showToast("Leave at least 100 ms on each side of the split")
        return
      }
      saveSnapshot()
      pausePlayback()
      val firstPart = target.copy(id = UUID.randomUUID().toString(), endMs = firstEnd, transitionAfter = TransitionType.NONE, fadeOutMs = 0L)
      val secondPart = target.copy(id = UUID.randomUUID().toString(), startMs = firstEnd, fadeInMs = 0L)

      clips.removeAt(targetIndex)
      clips.add(targetIndex, firstPart)
      clips.add(targetIndex + 1, secondPart)

      val total = calculateTotalDuration(clips)
      _uiState.update {
        it.copy(
          clips = clips,
          totalDurationMs = total,
          selectedClipId = secondPart.id
        )
      }
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
      showToast("Clip split at current position")
    } else {
      showToast("Move playhead inside a clip to split")
    }
  }

  fun deleteSelectedClip() {
    val selectedId = _uiState.value.selectedClipId ?: return
    val remaining = _uiState.value.clips.filter { it.id != selectedId }
    if (remaining.isEmpty()) {
      showToast("Cannot delete the only clip")
      return
    }
    saveSnapshot()
    pausePlayback()
    val total = calculateTotalDuration(remaining)
    _uiState.update {
      it.copy(
        clips = remaining,
        currentTimeMs = it.currentTimeMs.coerceAtMost(total),
        totalDurationMs = total,
        selectedClipId = remaining.firstOrNull()?.id,
        activeSubMenu = SubMenuType.NONE
      )
    }
    showToast("Clip deleted")
  }

  fun duplicateSelectedClip() {
    saveSnapshot()
    val selected = _uiState.value.selectedClip ?: return
    val duplicate = selected.copy(
      id = UUID.randomUUID().toString(),
      title = "${selected.title} (Copy)"
    )
    val index = _uiState.value.clips.indexOfFirst { it.id == selected.id }
    val mutable = _uiState.value.clips.toMutableList()
    mutable.add(index + 1, duplicate)
    val total = calculateTotalDuration(mutable)
    _uiState.update {
      it.copy(
        clips = mutable,
        totalDurationMs = total,
        selectedClipId = duplicate.id
      )
    }
    showToast("Clip duplicated")
  }

  fun setClipSpeed(newSpeed: Float) {
    val clip = _uiState.value.selectedClip?.takeIf { it.isVideo } ?: return
    updateSelectedClip(clip.copy(speed = newSpeed.coerceIn(0.25f, 4f), speedPoints = emptyList()))
  }

  fun addSpeedPoint(sourceMs: Long, rate: Float) {
    val clip = _uiState.value.selectedClip?.takeIf { it.isVideo } ?: return
    if (sourceMs !in clip.startMs until clip.endMs || rate !in 0.25f..4f) return
    val points = clip.speedPoints.filterNot { it.sourceMs == sourceMs } + SpeedPoint(sourceMs, rate)
    updateSelectedClip(clip.copy(speedPoints = points.sortedBy { it.sourceMs }))
  }

  fun removeSpeedPoint(sourceMs: Long) {
    val clip = _uiState.value.selectedClip ?: return
    updateSelectedClip(clip.copy(speedPoints = clip.speedPoints.filterNot { it.sourceMs == sourceMs }))
  }

  fun addTransformKeyframe(keyframe: TransformKeyframe) {
    val clip = _uiState.value.selectedClip ?: return
    if (keyframe.sourceMs !in clip.startMs until clip.endMs) return
    val keys = clip.transformKeyframes.filterNot { it.sourceMs == keyframe.sourceMs } + keyframe
    updateSelectedClip(clip.copy(transformKeyframes = keys.sortedBy { it.sourceMs }))
  }

  fun removeTransformKeyframe(sourceMs: Long) {
    val clip = _uiState.value.selectedClip ?: return
    updateSelectedClip(clip.copy(transformKeyframes = clip.transformKeyframes.filterNot { it.sourceMs == sourceMs }))
  }

  fun setClipFades(fadeInMs: Long, fadeOutMs: Long) {
    val clip = _uiState.value.selectedClip ?: return
    updateSelectedClip(clip.copy(
      fadeInMs = fadeInMs.coerceIn(0L, clip.timelineDurationMs),
      fadeOutMs = fadeOutMs.coerceIn(0L, clip.timelineDurationMs)
    ))
  }

  fun setVideoCleanup(chromaKey: ChromaKey?, denoise: Float) {
    val clip = _uiState.value.selectedClip?.takeIf { it.isVideo } ?: return
    updateSelectedClip(clip.copy(chromaKey = chromaKey, videoDenoise = denoise.coerceIn(0f, 1f)))
  }

  fun autoReframeSelectedClip() {
    if (reframeJob?.isActive == true) return showToast("Auto Reframe is already running")
    val state = _uiState.value
    val clip = state.selectedClip?.takeIf { it.isVideo && it.uriString != null } ?: return
    if (clip.rotationDegrees != 0f || clip.isMirrored || clip.cropFraction > 0f) {
      return showToast("Reset rotation, mirror, and crop before using Auto Reframe")
    }
    pausePlayback()
    showToast("Analyzing faces on this device")
    reframeJob = viewModelScope.launch {
      try {
        val keys = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          reframeFaces(getApplication(), clip, state.aspectRatio.ratioFloat)
        }
        val current = _uiState.value
        when {
          current.selectedClip != clip || current.aspectRatio != state.aspectRatio ->
            showToast("Clip or format changed during Auto Reframe")
          keys.isEmpty() -> showToast("No frontal face was found in this clip")
          else -> {
            updateSelectedClip(clip.copy(transformKeyframes = keys))
            showToast("Auto Reframe added ${keys.size} keyframes")
          }
        }
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        showToast("Could not analyze this video")
      }
    }
  }

  fun setClipVolume(newVolume: Float) {
    saveSnapshot()
    val selectedId = _uiState.value.selectedClipId ?: return
    val updated = _uiState.value.clips.map {
      if (it.id == selectedId) it.copy(volume = newVolume) else it
    }
    _uiState.update { it.copy(clips = updated) }
  }

  fun openSpeedMenu() {
    if (_uiState.value.selectedClip != null) _uiState.update { it.copy(activeSubMenu = SubMenuType.SPEED) }
  }

  fun openVolumeMenu() {
    if (_uiState.value.selectedClip != null) _uiState.update { it.copy(activeSubMenu = SubMenuType.VOLUME) }
  }

  fun mirrorSelectedClip() {
    val selected = _uiState.value.selectedClip ?: return
    updateSelectedClip(selected.copy(isMirrored = !selected.isMirrored))
  }

  fun setSelectedCrop(fraction: Float) {
    val selected = _uiState.value.selectedClip ?: return
    updateSelectedClip(selected.copy(cropFraction = fraction.coerceIn(0f, 0.8f)))
  }

  fun setSelectedTrim(startMs: Long, endMs: Long) {
    val selected = _uiState.value.selectedClip ?: return
    if (startMs < 0 || endMs > selected.originalDurationMs || endMs - startMs < 100) return
    updateSelectedClip(selected.copy(startMs = startMs, endMs = endMs))
  }

  fun cutSelectedRange(startMs: Long, endMs: Long) {
    val clip = _uiState.value.selectedClip ?: return
    if (startMs - clip.startMs < 100 || clip.endMs - endMs < 100 || endMs - startMs < 100) return
    saveSnapshot()
    pausePlayback()
    _uiState.update { state ->
      val clips = state.clips.flatMap {
        if (it.id == clip.id) listOf(
          clip.copy(endMs = startMs, transitionAfter = TransitionType.NONE, fadeOutMs = 0L),
          clip.copy(id = UUID.randomUUID().toString(), startMs = endMs, isSelected = false, fadeInMs = 0L)
        ) else listOf(it)
      }
      val duration = calculateTotalDuration(clips)
      state.copy(clips = clips, totalDurationMs = duration, currentTimeMs = state.currentTimeMs.coerceAtMost(duration))
    }
  }

  private fun updateSelectedClip(clip: VideoClip) {
    val before = _uiState.value
    val previous = before.selectedClip ?: return
    if (clip == previous) return
    val clipStart = calculateTotalDuration(before.clips.takeWhile { it.id != clip.id })
    val localTime = before.currentTimeMs - clipStart
    val newTime = when {
      localTime < 0 -> before.currentTimeMs
      localTime >= previous.timelineDurationMs ->
        before.currentTimeMs + clip.timelineDurationMs - previous.timelineDurationMs
      else -> {
        val source = previous.speedSegments().sourceTimeAt(localTime)
        clipStart + clip.speedSegments().timelineTimeAt(source)
      }
    }
    saveSnapshot()
    pausePlayback()
    _uiState.update { state ->
      val clips = state.clips.map { if (it.id == clip.id) clip else it }
      val duration = calculateTotalDuration(clips)
      state.copy(clips = clips, totalDurationMs = duration, currentTimeMs = newTime.coerceIn(0L, duration))
    }
  }

  fun rotateSelectedClip() {
    saveSnapshot()
    val selectedId = _uiState.value.selectedClipId ?: return
    val updated = _uiState.value.clips.map {
      if (it.id == selectedId) {
        val newRotation = (it.rotationDegrees + 90f) % 360f
        it.copy(rotationDegrees = newRotation)
      } else it
    }
    _uiState.update { it.copy(clips = updated) }
    showToast("Rotated 90°")
  }

  fun reverseSelectedClip() {
    saveSnapshot()
    val selectedId = _uiState.value.selectedClipId ?: return
    val updated = _uiState.value.clips.map {
      if (it.id == selectedId) it.copy(isReversed = !it.isReversed) else it
    }
    _uiState.update { it.copy(clips = updated) }
    val isRev = updated.firstOrNull { it.id == selectedId }?.isReversed == true
    showToast(if (isRev) "Clip reversed (offline playback)" else "Clip normal playback")
  }

  fun freezeFrame() {
    val state = _uiState.value
    val selected = state.selectedClip ?: return
    val source = selected.uriString ?: return showToast("Import a video to freeze a frame")
    val start = calculateTotalDuration(state.clips.takeWhile { it.id != selected.id })
    val position = selected.speedSegments().sourceTimeAt(state.currentTimeMs - start).coerceAtMost(selected.endMs - 1)
    pausePlayback()
    viewModelScope.launch {
      val app = getApplication<Application>()
      val frame = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          if (!selected.isVideo) return@withContext source
          val retriever = android.media.MediaMetadataRetriever()
          try {
            retriever.setDataSource(app, android.net.Uri.parse(source))
            val bitmap = checkNotNull(retriever.getFrameAtTime(position * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST))
            val file = File.createTempFile("freeze-", ".jpg", app.filesDir)
            try {
              file.outputStream().use {
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it))
              }
              android.net.Uri.fromFile(file).toString()
            } catch (error: Exception) {
              file.delete()
              throw error
            } finally {
              bitmap.recycle()
            }
          } finally {
            retriever.release()
          }
        }
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        showToast("Could not read a frame from this clip")
        return@launch
      }

      val clips = _uiState.value.clips.toMutableList()
      val index = clips.indexOf(selected)
      if (index < 0) return@launch
      saveSnapshot()
      val frozen = selected.copy(
        id = UUID.randomUUID().toString(),
        title = "${selected.title} [Freeze]",
        startMs = 0,
        endMs = 3000,
        originalDurationMs = 3000,
        uriString = frame,
        isVideo = false,
        speed = 1f,
        speedPoints = emptyList(),
        transformKeyframes = emptyList(),
        chromaKey = null,
        videoDenoise = 0f,
        fadeInMs = 0L,
        fadeOutMs = 0L,
        isFrozen = false,
        isReversed = false,
        transitionAfter = TransitionType.NONE,
        voiceEffect = "Normal",
        volume = 0f
      )
      clips.add(index + 1, frozen)
      _uiState.update {
        it.copy(clips = clips, totalDurationMs = calculateTotalDuration(clips), selectedClipId = frozen.id)
      }
    }
  }

  fun setFilter(filter: FilterItem) {
    saveSnapshot()
    _uiState.update { it.copy(selectedFilter = filter) }
    showToast("Filter applied: ${filter.name}")
  }

  fun setFilterIntensity(intensity: Float) {
    saveSnapshot()
    _uiState.update { it.copy(filterIntensity = intensity) }
  }

  fun updateAdjustSettings(adjustSettings: AdjustSettings) {
    saveSnapshot()
    _uiState.update { it.copy(adjustSettings = adjustSettings) }
  }

  fun resetAdjustSettings() {
    saveSnapshot()
    _uiState.update { it.copy(adjustSettings = AdjustSettings()) }
    showToast("Adjustments reset")
  }

  fun setAspectRatio(ratio: AspectRatioOption) {
    saveSnapshot()
    _uiState.update { it.copy(aspectRatio = ratio) }
    showToast("Canvas aspect ratio: ${ratio.label}")
  }

  private fun cycleAspectRatio() {
    val current = _uiState.value.aspectRatio
    val options = AspectRatioOption.entries
    val nextIndex = (options.indexOf(current) + 1) % options.size
    setAspectRatio(options[nextIndex])
  }

  fun addAudioTrack(title: String, category: String = "BGM") {
    saveSnapshot()
    val newTrack = AudioTrackItem(
      id = UUID.randomUUID().toString(),
      title = title,
      startMs = _uiState.value.currentTimeMs,
      endMs = (_uiState.value.currentTimeMs + 6000L).coerceAtMost(_uiState.value.totalDurationMs),
      category = category
    )
    _uiState.update { it.copy(audioTracks = it.audioTracks + newTrack) }
    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
    showToast("Added audio track: $title")
  }

  fun importLocalAudio(uri: android.net.Uri, titleOverride: String? = null) {
    val app = getApplication<Application>()
    val state = _uiState.value
    val start = state.currentTimeMs
    if (start >= state.totalDurationMs) return showToast("Move the playhead before the end to add audio")
    pausePlayback()
    viewModelScope.launch {
      val track = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          val retriever = android.media.MediaMetadataRetriever()
          val duration = try {
            retriever.setDataSource(app, uri)
            require(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes")
            requireNotNull(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull())
          } finally {
            retriever.release()
          }
          require(duration > 0)
          runCatching {
            app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          val name = titleOverride ?: if (uri.scheme == "file") "Voiceover" else app.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Imported audio"
          AudioTrackItem(
            UUID.randomUUID().toString(), name, start, minOf(start + duration, state.totalDurationMs),
            volume = 0.35f, uriString = uri.toString(), sourceDurationMs = duration
          )
        }
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        return@launch showToast("Could not read this audio file")
      }
      val current = _uiState.value
      if (current.clips != state.clips) return@launch showToast("Timeline changed. Select the audio file again")
      saveSnapshot()
      _uiState.update { it.copy(audioTracks = it.audioTracks + track) }
    }
  }

  fun editLocalAudio(id: String, start: Long, end: Long, volume: Float) {
    val state = _uiState.value
    val track = state.audioTracks.firstOrNull { it.id == id && it.uriString != null } ?: return
    if (start < 0 || end > state.totalDurationMs || end <= start || end - start > track.sourceDurationMs - track.sourceOffsetMs) return
    saveSnapshot()
    pausePlayback()
    _uiState.update {
      it.copy(audioTracks = it.audioTracks.map { item ->
        if (item.id == id) item.copy(startMs = start, endMs = end, volume = volume.coerceIn(0f, 1f)) else item
      })
    }
  }

  fun moveSelectedClip(direction: Int) {
    val state = _uiState.value
    val index = state.clips.indexOfFirst { it.id == state.selectedClipId }
    val target = index + direction
    if (index < 0 || target !in state.clips.indices) return
    val updated = state.clips.toMutableList()
    val clip = updated.removeAt(index)
    updated.add(target, clip)
    val previousStart = state.clips.take(index).sumOf { it.timelineDurationMs }
    val localTime = (state.currentTimeMs - previousStart).coerceIn(0L, clip.timelineDurationMs - 1)
    val position = updated.take(target).sumOf { it.timelineDurationMs } + localTime
    saveSnapshot()
    pausePlayback()
    _uiState.update { it.copy(clips = updated, currentTimeMs = position) }
  }

  fun removeAudioTrack(id: String) {
    saveSnapshot()
    _uiState.update { it.copy(audioTracks = it.audioTracks.filter { t -> t.id != id }) }
    showToast("Removed audio track")
  }

  fun addSoundEffect(title: String, durationMs: Long) {
    pausePlayback()
    _uiState.update { it.copy(showSfxDialog = false) }
    viewModelScope.launch {
      try {
        val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
          com.example.media.createSoundEffect(getApplication<Application>().filesDir, title, durationMs)
        }
        importLocalAudio(android.net.Uri.fromFile(file), title)
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) { showToast("Could not create this sound.") }
    }
  }

  fun toggleTimelineClipMute(id: String) {
    val clip = _uiState.value.clips.firstOrNull { it.id == id } ?: return
    saveSnapshot()
    _uiState.update { state ->
      state.copy(clips = state.clips.map {
        if (it.id != id) it
        else if (clip.volume == 0f) it.copy(volume = 1f, isMuted = false)
        else it.copy(isMuted = !clip.isMuted)
      })
    }
  }

  fun beginTimelineEdit() {
    pausePlayback()
    saveSnapshot()
  }

  fun trimTimelineClip(id: String, start: Long, end: Long) {
    val state = _uiState.value
    val clip = state.clips.firstOrNull { it.id == id } ?: return
    val maximum = if (clip.isVideo) clip.originalDurationMs else 60000L
    if (start < 0 || end - start < 100 || end > maximum) return
    val updated = clip.copy(startMs = start, endMs = end, originalDurationMs = maxOf(clip.originalDurationMs, end))
    val clips = state.clips.map { if (it.id == id) updated else it }
    val duration = calculateTotalDuration(clips)
    _uiState.update { it.copy(clips = clips, selectedClipId = id, selectedTextId = null,
      totalDurationMs = duration, currentTimeMs = it.currentTimeMs.coerceAtMost(duration)) }
  }

  fun setTimelineTextTiming(id: String, start: Long, end: Long) {
    if (start < 0 || end - start < 100 || end > _uiState.value.totalDurationMs) return
    _uiState.update { it.copy(textTracks = it.textTracks.map { track ->
      if (track.id == id) track.copy(startMs = start, endMs = end) else track
    }) }
  }

  fun setTimelineAudioTiming(id: String, start: Long, end: Long, sourceOffset: Long) {
    val track = _uiState.value.audioTracks.firstOrNull { it.id == id } ?: return
    if (start < 0 || end - start < 100 || end > _uiState.value.totalDurationMs ||
      sourceOffset < 0 || sourceOffset + end - start > track.sourceDurationMs) return
    _uiState.update { it.copy(audioTracks = it.audioTracks.map { item ->
      if (item.id == id) item.copy(startMs = start, endMs = end, sourceOffsetMs = sourceOffset) else item
    }) }
  }

  fun useCurrentFrameAsCover() {
    val state = _uiState.value
    val clip = state.currentClipAtPlayhead ?: return
    val source = clip.uriString ?: return
    pausePlayback()
    if (!clip.isVideo) {
      saveSnapshot()
      _uiState.update { it.copy(coverUri = source) }
      showToast("Project cover updated")
      return
    }
    val clipStart = state.clips.takeWhile { it.id != clip.id }.sumOf { it.timelineDurationMs }
    val frameTime = clip.speedSegments().sourceTimeAt(state.currentTimeMs - clipStart).coerceAtMost(clip.endMs - 1)
    viewModelScope.launch {
      try {
        val cover = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          val retriever = android.media.MediaMetadataRetriever()
          try {
            retriever.setDataSource(getApplication(), android.net.Uri.parse(source))
            val frame = checkNotNull(retriever.getFrameAtTime(frameTime * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST))
            val file = File.createTempFile("cover-", ".jpg", getApplication<Application>().filesDir)
            try {
              file.outputStream().use { check(frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)) }
              android.net.Uri.fromFile(file).toString()
            } catch (error: Exception) { file.delete(); throw error }
            finally { frame.recycle() }
          } finally { retriever.release() }
        }
        if (_uiState.value.currentProjectId != state.currentProjectId) return@launch
        saveSnapshot()
        _uiState.update { it.copy(coverUri = cover) }
        showToast("Project cover updated")
      } catch (error: kotlinx.coroutines.CancellationException) { throw error }
      catch (_: Exception) { showToast("Could not read this frame for the cover") }
    }
  }

  fun selectText(id: String?) {
    if (id == null) { finishTextEditing(); return }
    pausePlayback()
    val state = _uiState.value
    val track = state.textTracks.firstOrNull { it.id == id }
    if (track != null && state.selectedTextId != id) saveSnapshot()
    _uiState.update {
      it.copy(selectedTextId = track?.id, activeSubMenu = SubMenuType.NONE,
        currentTimeMs = track?.let { text -> it.currentTimeMs.coerceIn(text.startMs, text.endMs - 1) } ?: it.currentTimeMs)
    }
  }

  fun beginTextTransform() {
    pausePlayback()
    saveSnapshot()
  }

  fun updateTextLayer(track: TextTrackItem) {
    if (track.startMs < 0 || track.endMs <= track.startMs || track.endMs > _uiState.value.totalDurationMs) return
    val safe = track.copy(
      fontSizeSp = track.fontSizeSp.coerceIn(8f, 96f), scale = track.scale.coerceIn(0.25f, 6f),
      posX = track.posX.coerceIn(0.02f, 0.98f), posY = track.posY.coerceIn(0.02f, 0.98f),
      opacity = track.opacity.coerceIn(0f, 1f), strokeWidth = track.strokeWidth.coerceIn(0f, 5f),
      text = track.text.take(2000)
    )
    _uiState.update {
      val changedTiming = it.textTracks.firstOrNull { item -> item.id == safe.id }
        ?.let { previous -> previous.startMs != safe.startMs || previous.endMs != safe.endMs } == true
      it.copy(
        textTracks = it.textTracks.map { item -> if (item.id == safe.id) safe else item },
        currentTimeMs = if (changedTiming && it.selectedTextId == safe.id) it.currentTimeMs.coerceIn(safe.startMs, safe.endMs - 1) else it.currentTimeMs
      )
    }
  }

  fun duplicateSelectedText() {
    val state = _uiState.value
    val track = state.textTracks.firstOrNull { it.id == state.selectedTextId } ?: return
    saveSnapshot()
    val copy = track.copy(id = UUID.randomUUID().toString(), posX = (track.posX + 0.04f).coerceAtMost(0.95f),
      posY = (track.posY + 0.04f).coerceAtMost(0.95f))
    _uiState.update { it.copy(textTracks = it.textTracks + copy, selectedTextId = copy.id) }
  }

  fun moveSelectedText(direction: Int) {
    val state = _uiState.value
    val index = state.textTracks.indexOfFirst { it.id == state.selectedTextId }
    val target = index + direction
    if (index < 0 || target !in state.textTracks.indices) return
    saveSnapshot()
    val tracks = state.textTracks.toMutableList()
    val track = tracks.removeAt(index)
    tracks.add(target, track)
    _uiState.update { it.copy(textTracks = tracks) }
  }

  fun finishTextEditing() {
    val selected = _uiState.value.selectedTextId
    _uiState.update {
      it.copy(selectedTextId = null, textTracks = it.textTracks.filterNot { track -> track.id == selected && track.text.isBlank() })
    }
  }

  fun addSticker(emoji: String) = addTextTrack(emoji, fontSizeSp = 48f, isSticker = true)

  fun importPhotoOverlay(uri: android.net.Uri) {
    val projectId = _uiState.value.currentProjectId ?: return
    if (_uiState.value.totalDurationMs < 100L) return
    val app = getApplication<Application>()
    viewModelScope.launch {
      val photo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
          val resolver = app.contentResolver
          val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
            val source = android.graphics.ImageDecoder.createSource(resolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
              val longest = maxOf(info.size.width, info.size.height)
              decoder.setTargetSampleSize(((longest.toLong() + 1023) / 1024).toInt().coerceAtLeast(1))
              decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
          } else {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
            options.inJustDecodeBounds = false
            options.inSampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2
            checkNotNull(resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) })
          }
          val directory = java.io.File(app.filesDir, "photo-overlays").apply { mkdirs() }
          val file = java.io.File(directory, UUID.randomUUID().toString() + ".png")
          try {
            file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            file
          } catch (failure: Exception) {
            file.delete()
            throw failure
          } finally {
            bitmap.recycle()
          }
        }.getOrNull()
      }
      if (photo == null) return@launch showToast("Could not import the photo overlay")
      if (_uiState.value.currentProjectId != projectId || _uiState.value.totalDurationMs < 100L) {
        photo.delete()
        return@launch
      }
      addTextTrack("Photo overlay", photoOverlayPath = photo.absolutePath)
    }
  }

  fun addTextTrack(
    text: String, colorHex: String = "#FFFFFF", fontSizeSp: Float = 26f,
    isSticker: Boolean = false, photoOverlayPath: String? = null
  ) {
    if (text.isBlank() || _uiState.value.totalDurationMs < 100) return
    val start = _uiState.value.currentTimeMs.coerceAtMost(_uiState.value.totalDurationMs - 100)
    saveSnapshot()
    val newTrack = TextTrackItem(
      id = UUID.randomUUID().toString(),
      text = text.trim(),
      startMs = start,
      endMs = (start + 3500L).coerceAtMost(_uiState.value.totalDurationMs),
      colorHex = colorHex,
      fontSizeSp = fontSizeSp,
      isSticker = isSticker,
      photoOverlayPath = photoOverlayPath
    )
    _uiState.update { it.copy(textTracks = it.textTracks + newTrack, showAddTextDialog = false, selectedTextId = newTrack.id, activeSubMenu = SubMenuType.NONE) }
    showToast("Added text overlay")
  }

  fun importSubtitles(uri: android.net.Uri) {
    val state = _uiState.value
    if (state.totalDurationMs <= 0L) return showToast("Add a clip before importing subtitles")
    val resolver = getApplication<Application>().contentResolver
    viewModelScope.launch {
      val cues = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          val text = checkNotNull(resolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { reader ->
            buildString {
              val buffer = CharArray(4096)
              while (true) {
                val count = reader.read(buffer)
                if (count == -1) break
                require(length + count <= 1_000_000)
                append(buffer, 0, count)
              }
            }
          }
          SubRip.parse(text)
        }
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        return@launch showToast("Could not read this UTF-8 SRT file")
      }
      val current = _uiState.value
      if (current.clips != state.clips) return@launch showToast("Timeline changed. Select the SRT file again")
      val tracks = cues.filter { it.startMs < current.totalDurationMs }.map { cue ->
        TextTrackItem(UUID.randomUUID().toString(), cue.text, cue.startMs, minOf(cue.endMs, current.totalDurationMs))
      }
      if (tracks.isEmpty()) return@launch showToast("No subtitles fall within this timeline")
      saveSnapshot()
      _uiState.update { it.copy(textTracks = it.textTracks + tracks) }
      showToast("Imported ${tracks.size} subtitles")
    }
  }

  fun saveSubtitles(uri: android.net.Uri) {
    val state = _uiState.value
    val cues = state.textTracks.mapNotNull { track ->
      val end = minOf(track.endMs, state.totalDurationMs)
      if (end <= track.startMs || track.text.isBlank()) null else SubtitleCue(track.startMs, end, track.text)
    }
    val resolver = getApplication<Application>().contentResolver
    viewModelScope.launch {
      try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          checkNotNull(resolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use {
            it.write(SubRip.encode(cues))
          }
        }
        showToast("Subtitles saved")
      } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
      } catch (_: Exception) {
        showToast("Could not save subtitles")
      }
    }
  }

  fun editSubtitle(id: String, text: String, start: Long, end: Long) {
    val state = _uiState.value
    if (start < 0 || end <= start || end > state.totalDurationMs || text.isBlank()) return
    if (state.textTracks.none { it.id == id }) return
    saveSnapshot()
    _uiState.update {
      it.copy(textTracks = it.textTracks.map { item ->
        if (item.id == id) item.copy(text = text.trim(), startMs = start, endMs = end) else item
      })
    }
  }

  fun removeTextTrack(id: String) {
    saveSnapshot()
    _uiState.update { it.copy(textTracks = it.textTracks.filter { t -> t.id != id }, selectedTextId = if (it.selectedTextId == id) null else it.selectedTextId) }
    showToast("Removed text track")
  }

  fun addAutoCaptions() {
    showToast("Automatic transcription is unavailable. Use Text to add manual captions.")
  }

  fun openTransitionDialog(clipId: String) {
    _uiState.update {
      it.copy(
        selectedTransitionClipId = clipId,
        showTransitionDialog = true
      )
    }
  }

  fun closeTransitionDialog() {
    _uiState.update { it.copy(showTransitionDialog = false) }
  }

  fun applyTransition(type: TransitionType, durationMs: Long) {
    val state = _uiState.value
    val index = state.clips.indexOfFirst { it.id == state.selectedTransitionClipId }
    if (index < 0 || index >= state.clips.lastIndex) return
    saveSnapshot()
    pausePlayback()
    _uiState.update { current ->
      current.copy(
        showTransitionDialog = false,
        clips = current.clips.mapIndexed { position, clip ->
          val duration = if (type == TransitionType.NONE) 0L else minOf(durationMs, clip.timelineDurationMs / 2)
          when (position) {
            index -> clip.copy(transitionAfter = TransitionType.NONE, fadeOutMs = duration)
            index + 1 -> clip.copy(fadeInMs = duration)
            else -> clip
          }
        }
      )
    }
  }

  fun openVoiceChanger() {
    pausePlayback()
    _uiState.update { it.copy(showVoiceChangerDialog = true) }
  }

  fun closeVoiceChanger() {
    _uiState.update { it.copy(showVoiceChangerDialog = false) }
  }

  fun applyVoiceEffect(effect: String) {
    saveSnapshot()
    val selectedId = _uiState.value.selectedClipId ?: return
    val updated = _uiState.value.clips.map {
      if (it.id == selectedId) it.copy(voiceEffect = effect) else it
    }
    _uiState.update {
      it.copy(
        clips = updated,
        showVoiceChangerDialog = false
      )
    }
    showToast("Applied voice effect: $effect")
  }

  fun openSfxDialog() {
    _uiState.update { it.copy(showSfxDialog = true) }
  }

  fun closeSfxDialog() {
    _uiState.update { it.copy(showSfxDialog = false) }
  }

  fun openResolutionSheet(show: Boolean) {
    _uiState.update { it.copy(showResolutionSheet = show) }
  }

  fun setResolution(label: String, fps: Int, smartHdr: Boolean) {
    val estSize = when (label) {
      "720P" -> 14.2f
      "1080P" -> 28.5f
      "2K/4K" -> 64.0f
      else -> 25.0f
    }
    _uiState.update {
      it.copy(
        resolutionSetting = ResolutionSetting(label, fps, smartHdr, estSize),
        showResolutionSheet = false
      )
    }
    showToast("Export set to $label ${fps}fps")
  }

  fun triggerExport() {
    if (exporter != null) return
    pausePlayback()
    try {
      val snapshot = _uiState.value
      val composition = OfflineComposition.create(snapshot)
      val app = getApplication<Application>()
      val directory = File(app.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: app.filesDir, "Edits")
      check(directory.isDirectory || directory.mkdirs()) { "Could not create export folder." }
      val output = File(directory, "Edit-${UUID.randomUUID()}.mp4")
      val transformer = Transformer.Builder(app)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .addListener(object : Transformer.Listener {
          override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            exportJob?.cancel()
            exporter = null
            exportFile = null
            _uiState.update { it.copy(exportProgress = 1f, isExportFinished = true, exportedFile = output.path) }
          }

          override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            cancelExport()
            showToast("Export failed: ${exportException.message ?: "media encoder error"}")
          }
        })
        .build()
      exporter = transformer
      exportFile = output
      _uiState.update {
        it.copy(showExportDialog = true, exportProgress = 0f, isExportFinished = false, exportedFile = null)
      }
      transformer.start(composition, output.path)
      exportJob = viewModelScope.launch {
        val progress = ProgressHolder()
        while (exporter === transformer) {
          if (transformer.getProgress(progress) == Transformer.PROGRESS_STATE_AVAILABLE) {
            _uiState.update { it.copy(exportProgress = progress.progress / 100f) }
          }
          delay(150)
        }
      }
    } catch (error: Exception) {
      cancelExport()
      showToast(error.message ?: "Could not start export")
    }
  }

  private fun cancelExport() {
    exportJob?.cancel()
    exporter?.cancel()
    exporter = null
    exportFile?.delete()
    exportFile = null
    _uiState.update { it.copy(showExportDialog = false) }
  }

  override fun onCleared() {
    cancelExport()
    super.onCleared()
  }

  fun dismissExportDialog() {
    cancelExport()
  }

  fun toggleFullscreen() {
    _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
  }

  fun togglePipOverlay() {
    _uiState.update { it.copy(pipOverlayEnabled = !it.pipOverlayEnabled) }
    showToast(if (_uiState.value.pipOverlayEnabled) "PIP overlay enabled" else "PIP overlay hidden")
  }

  fun showAddTextModal(show: Boolean) {
    _uiState.update { it.copy(showAddTextDialog = show) }
  }

  fun showAddAudioModal(show: Boolean) {
    _uiState.update { it.copy(showAddAudioDialog = show) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  fun showToast(msg: String) {
    _uiState.update { it.copy(toastMessage = msg) }
  }
}
