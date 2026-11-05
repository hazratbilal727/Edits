package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun EditorDialogContent(content: @Composable ColumnScope.() -> Unit) {
  val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).coerceIn(100f, 420f).dp
  Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()), content = content)
}
