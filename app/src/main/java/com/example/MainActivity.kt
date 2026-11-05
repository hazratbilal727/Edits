package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CapCutEditorScreen
import com.example.ui.screens.CapCutHomeScreen
import com.example.ui.screens.CapCutMediaPickerScreen
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutTheme
import com.example.viewmodel.VideoEditorViewModel

enum class Screen {
  HOME,
  MEDIA_PICKER,
  EDITOR
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CapCutTheme {
        CapCutApp()
      }
    }
  }
}

@Composable
fun CapCutApp(editorViewModel: VideoEditorViewModel = viewModel()) {
  var currentScreen by remember { mutableStateOf(Screen.HOME) }
  var isPhotoModeOnly by remember { mutableStateOf(false) }
  var isAppendingMedia by remember { mutableStateOf(false) }

  BackHandler(enabled = currentScreen != Screen.HOME) {
    currentScreen = if (isAppendingMedia) Screen.EDITOR else Screen.HOME
    isAppendingMedia = false
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = CapCutBackground
  ) {
    AnimatedContent(
      targetState = currentScreen,
      transitionSpec = {
        if (targetState == Screen.MEDIA_PICKER) {
          (slideInVertically { it } + fadeIn()).togetherWith(fadeOut())
        } else if (initialState == Screen.MEDIA_PICKER && targetState == Screen.EDITOR) {
          (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
        } else if (initialState == Screen.MEDIA_PICKER && targetState == Screen.HOME) {
          fadeIn().togetherWith(slideOutVertically { it } + fadeOut())
        } else if (targetState == Screen.EDITOR) {
          (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
        } else {
          (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
        }
      },
      label = "screen_transition"
    ) { screen ->
      when (screen) {
        Screen.HOME -> {
          CapCutHomeScreen(
            onNewVideoClick = {
              isPhotoModeOnly = false
              isAppendingMedia = false
              currentScreen = Screen.MEDIA_PICKER
            },
            onEditPhotoClick = {
              isPhotoModeOnly = true
              isAppendingMedia = false
              currentScreen = Screen.MEDIA_PICKER
            },
            onOpenProject = { projectId ->
              currentScreen = Screen.EDITOR
            }
          )
        }

        Screen.MEDIA_PICKER -> {
          CapCutMediaPickerScreen(
            isPhotoModeOnly = isPhotoModeOnly,
            onClose = {
              currentScreen = if (isAppendingMedia) Screen.EDITOR else Screen.HOME
              isAppendingMedia = false
            },
            onMediaSelected = { selectedClips ->
              if (isAppendingMedia) {
                editorViewModel.appendClips(selectedClips)
              } else {
                editorViewModel.loadClips(selectedClips)
              }
              isAppendingMedia = false
              currentScreen = Screen.EDITOR
            }
          )
        }

        Screen.EDITOR -> {
          CapCutEditorScreen(
            viewModel = editorViewModel,
            onNavigateBack = { currentScreen = Screen.HOME },
            onOpenMediaPickerForAppend = {
              isPhotoModeOnly = false
              isAppendingMedia = true
              currentScreen = Screen.MEDIA_PICKER
            }
          )
        }
      }
    }
  }
}
