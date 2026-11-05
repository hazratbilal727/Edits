package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CapCutHomeScreen(onNewVideoClick: () -> Unit, onEditPhotoClick: () -> Unit, onOpenProject: (String) -> Unit) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  Scaffold(
    containerColor = CapCutBackground,
    bottomBar = {
      NavigationBar(containerColor = CapCutSurface) {
        listOf("Create" to Icons.Default.AddCircle, "Projects" to Icons.Default.Folder).forEachIndexed { index, item ->
          NavigationBarItem(
            selected = tab == index, onClick = { tab = index },
            icon = { Icon(item.second, null) }, label = { Text(item.first) }
          )
        }
      }
    }
  ) { padding ->
    if (tab == 1) {
      ProjectLibrary(onOpenProject, onNewVideoClick, Modifier.padding(padding))
    } else {
      LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        item {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.edits_logo), null, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
            Column {
              Text("Edits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
              Text("Your story, frame by frame", color = CapCutTextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        item {
          Card(
            onClick = onNewVideoClick, shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CapCutTeal), modifier = Modifier.fillMaxWidth()
          ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Icon(Icons.Default.Add, null, Modifier.size(32.dp), tint = CapCutBackground)
              Text("Start a new edit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CapCutBackground)
              Text("Choose videos and photos from your phone.", color = CapCutBackground)
              Text("New project  →", color = CapCutBackground, fontWeight = FontWeight.SemiBold)
            }
          }
        }
        item {
          OutlinedButton(onClick = onEditPhotoClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Default.Image, null)
            Spacer(Modifier.width(10.dp))
            Text("Create from photos")
          }
        }
        item {
          Text("Made for your next idea", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(10.dp))
          Text(
            "Trim and arrange clips, shape the color, animate with keyframes, and add music and captions. Your edits are saved on this device.",
            color = CapCutTextSecondary, style = MaterialTheme.typography.bodyLarge
          )
        }
        item {
          TextButton(onClick = { tab = 1 }) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Continue a saved project")
          }
        }
      }
    }
  }
}

@Composable
fun ProjectLibrary(onOpenProject: (String) -> Unit, onNewProject: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val dao = remember(context) { AppDatabase.getDatabase(context).projectDao() }
  val projects by dao.getAllProjects().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()
  var query by rememberSaveable { mutableStateOf("") }
  var deleting by remember { mutableStateOf<ProjectEntity?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  val visible = remember(projects, query) { projects.filter { it.title.contains(query.trim(), true) } }

  LazyColumn(
    modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Projects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        FilledTonalIconButton(onClick = onNewProject) { Icon(Icons.Default.Add, "New project") }
      }
    }
    item {
      OutlinedTextField(
        query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search projects") },
        leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp)
      )
    }
    if (visible.isEmpty()) {
      item {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Default.VideoLibrary, null, Modifier.size(44.dp), tint = CapCutTextSecondary)
          Text(if (query.isBlank()) "Your next story starts here" else "No projects found", style = MaterialTheme.typography.titleMedium)
          if (query.isBlank()) Button(onClick = onNewProject) { Text("Create a project") }
        }
      }
    }
    items(visible, key = { it.id }) { project ->
      Card(
        onClick = { onOpenProject(project.id) }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CapCutSurface)
      ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(CapCutSurfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Movie, null, tint = CapCutTextSecondary)
            if (project.thumbnailUri != null) AsyncImage(project.thumbnailUri, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
          }
          Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${project.clipCount} clips · ${project.durationMs / 1000}s", color = CapCutTextSecondary)
          }
          IconButton(onClick = { deleting = project }) { Icon(Icons.Default.DeleteOutline, "Delete ${project.title}") }
        }
      }
    }
    if (error != null) item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
  }
  deleting?.let { project ->
    AlertDialog(
      onDismissRequest = { deleting = null }, title = { Text("Delete project?") },
      text = { Text("The saved edit “${project.title}” will be deleted. Original media stays on your phone.") },
      confirmButton = {
        TextButton(onClick = {
          scope.launch {
            try { dao.deleteProjectById(project.id) } catch (_: Exception) { error = "Could not delete this project." }
            deleting = null
          }
        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
    )
  }
}
