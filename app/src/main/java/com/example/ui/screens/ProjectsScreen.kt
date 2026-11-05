package com.example.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun ProjectsScreen(onNewProjectClick: () -> Unit, onOpenProject: (String) -> Unit) {
  ProjectLibrary(onOpenProject, onNewProjectClick)
}
