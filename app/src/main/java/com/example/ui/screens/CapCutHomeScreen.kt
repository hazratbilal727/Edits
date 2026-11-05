package com.example.ui.screens

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.model.HomeToolItem
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceElevated
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CapCutHomeScreen(
  onNewVideoClick: () -> Unit,
  onEditPhotoClick: () -> Unit,
  onOpenProject: (String) -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()

  val homeTools = remember {
    listOf(
      HomeToolItem("autocut", "AutoCut", Icons.Default.SmartDisplay),
      HomeToolItem("retouch", "Retouch", Icons.Default.Face),
      HomeToolItem("captions", "Captions", Icons.Default.Subtitles),
      HomeToolItem("effects", "Effects", Icons.Default.AutoAwesome),
      HomeToolItem("background", "Cutout", Icons.Default.ContentCut),
      HomeToolItem("prompter", "Prompter", Icons.Default.CameraAlt),
      HomeToolItem("extract_audio", "Extract Audio", Icons.Default.Mic),
      HomeToolItem("speed", "Speed Curve", Icons.Default.Speed)
    )
  }

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = CapCutSurface,
        contentColor = CapCutTextPrimary,
        tonalElevation = 0.dp
      ) {
        NavigationBarItem(
          icon = { Icon(Icons.Default.ContentCut, contentDescription = "Edit") },
          label = { Text("Edit", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CapCutTeal,
            selectedTextColor = CapCutTeal,
            unselectedIconColor = CapCutTextSecondary,
            unselectedTextColor = CapCutTextSecondary,
            indicatorColor = Color.Transparent
          )
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
          label = { Text("Projects", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CapCutTeal,
            selectedTextColor = CapCutTeal,
            unselectedIconColor = CapCutTextSecondary,
            unselectedTextColor = CapCutTextSecondary,
            indicatorColor = Color.Transparent
          )
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Person, contentDescription = "Me") },
          label = { Text("Me", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CapCutTeal,
            selectedTextColor = CapCutTeal,
            unselectedIconColor = CapCutTextSecondary,
            unselectedTextColor = CapCutTextSecondary,
            indicatorColor = Color.Transparent
          )
        )
      }
    },
    containerColor = CapCutBackground
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (selectedTab) {
        0 -> EditTabContent(
          homeTools = homeTools,
          onNewVideoClick = onNewVideoClick,
          onEditPhotoClick = onEditPhotoClick,
          onOpenProject = onOpenProject,
          onToolClick = { toolId ->
            when (toolId) {
              "autocut", "captions", "effects" -> onNewVideoClick()
              "retouch", "background" -> onEditPhotoClick()
              else -> Toast.makeText(context, "Tool ready in offline editor", Toast.LENGTH_SHORT).show()
            }
          }
        )
        1 -> ProjectsScreen(
          onNewProjectClick = onNewVideoClick,
          onOpenProject = onOpenProject
        )
        2 -> MeScreen()
      }
    }
  }
}

@Composable
private fun EditTabContent(
  homeTools: List<HomeToolItem>,
  onNewVideoClick: () -> Unit,
  onEditPhotoClick: () -> Unit,
  onOpenProject: (String) -> Unit,
  onToolClick: (String) -> Unit
) {
  val context = LocalContext.current
  val db = remember { AppDatabase.getDatabase(context) }
  val projects by db.projectDao().getAllProjects().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()

  Box(modifier = Modifier.fillMaxSize()) {
    // Top background gradient glow
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF003B99).copy(alpha = 0.45f),
              Color(0xFF001F54).copy(alpha = 0.2f),
              CapCutBackground
            )
          )
        )
    )

    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      state = gridState,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Header item
      item(span = { GridItemSpan(3) }) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 4.dp, bottom = 8.dp)
        ) {
          // App brand and search
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Image(
                painter = painterResource(R.drawable.edits_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
              )
              Text(
                text = stringResource(R.string.app_name),
                color = CapCutTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CapCutSurfaceVariant)
                .clickable { onToolClick("search") }
                .testTag("home_search_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = CapCutTextPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Video create",
            color = CapCutTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNewVideoClick() }
          ) {
            Text(
              text = "Get started",
              color = CapCutTextPrimary,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              tint = CapCutTextPrimary,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Hero Action Cards: "+ New video" & "Edit photo"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // "+ New video" Card
            Box(
              modifier = Modifier
                .weight(1.3f)
                .height(115.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF))
                  )
                )
                .clickable { onNewVideoClick() }
                .padding(14.dp)
                .testTag("hero_new_video_card")
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                  )
                }
                Column {
                  Text(
                    text = "New video",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Pick gallery clips",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                  )
                }
              }
            }

            // "Edit photo" Card
            Box(
              modifier = Modifier
                .weight(1f)
                .height(115.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CapCutSurfaceElevated)
                .clickable { onEditPhotoClick() }
                .padding(14.dp)
                .testTag("hero_edit_photo_card")
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CapCutPurple.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = CapCutPurple,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Column {
                  Text(
                    text = "Edit photo",
                    color = CapCutTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Effects & retouch",
                    color = CapCutTextSecondary,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }
      }

      // Tools Row (Span 3)
      item(span = { GridItemSpan(3) }) {
        Column {
          Text(
            text = "Popular tools",
            color = CapCutTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            homeTools.take(4).forEach { tool ->
              HomeToolButton(tool) { onToolClick(tool.id) }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            homeTools.drop(4).forEach { tool ->
              HomeToolButton(tool) { onToolClick(tool.id) }
            }
          }
        }
      }

      // Recent Projects Header
      item(span = { GridItemSpan(3) }) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Projects",
            color = CapCutTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${projects.size} drafts",
            color = CapCutTextSecondary,
            fontSize = 12.sp
          )
        }
      }

      // Projects List or Empty State
      if (projects.isEmpty()) {
        item(span = { GridItemSpan(3) }) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(CapCutSurface)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Folder, contentDescription = null, tint = CapCutTextSecondary, modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No projects saved yet", color = CapCutTextSecondary, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text("Tap + New video above to create your first edit", color = CapCutTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      } else {
        items(projects) { project ->
          ProjectCard(
            project = project,
            onClick = { onOpenProject(project.id) },
            onDelete = {
              scope.launch {
                db.projectDao().deleteProjectById(project.id)
                Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
              }
            }
          )
        }
      }
    }
  }
}

@Composable
private fun HomeToolButton(tool: HomeToolItem, onClick: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(72.dp)
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier
        .size(50.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(CapCutSurfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(tool.icon, contentDescription = tool.title, tint = CapCutTeal, modifier = Modifier.size(24.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = tool.title,
      color = CapCutTextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun ProjectCard(
  project: ProjectEntity,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(CapCutSurface)
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .background(CapCutSurfaceVariant)
    ) {
      if (project.thumbnailUri != null) {
        AsyncImage(
          model = project.thumbnailUri,
          contentDescription = project.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = CapCutTextSecondary, modifier = Modifier.size(32.dp))
        }
      }

      // Duration badge
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(6.dp)
          .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
          .padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        val totalSec = project.durationMs / 1000
        Text(text = String.format("%02d:%02d", totalSec / 60, totalSec % 60), color = Color.White, fontSize = 9.sp)
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = project.title, color = CapCutTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(text = dateFormat.format(Date(project.lastModified)), color = CapCutTextSecondary, fontSize = 10.sp)
      }

      Box {
        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = CapCutTextSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
          modifier = Modifier.background(CapCutSurface)
        ) {
          DropdownMenuItem(
            text = { Text("Delete Project", color = Color(0xFFFF4D4F), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D4F)) },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }
  }
}
