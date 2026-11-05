package com.example.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MediaPickerItem(
  val uri: String,
  val name: String,
  val durationMs: Long,
  val isVideo: Boolean,
  val dateAdded: Long = System.currentTimeMillis(),
  val isSelected: Boolean = false
)

data class ProjectItemData(
  val id: String,
  val title: String,
  val thumbnailUri: String? = null,
  val durationMs: Long,
  val clipCount: Int,
  val lastModified: Long
)

data class HomeToolItem(
  val id: String,
  val title: String,
  val icon: ImageVector
)

data class MeMenuItem(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val icon: ImageVector
)
