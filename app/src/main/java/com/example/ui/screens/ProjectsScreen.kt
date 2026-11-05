package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.ui.theme.CapCutBackground
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTeal
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProjectsScreen(
  onNewProjectClick: () -> Unit,
  onOpenProject: (String) -> Unit
) {
  val context = LocalContext.current
  val db = remember { AppDatabase.getDatabase(context) }
  val allProjects by db.projectDao().getAllProjects().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()

  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("All", "Drafts", "Exported")

  val filteredProjects = remember(allProjects, searchQuery, selectedTab) {
    allProjects.filter {
      searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNewProjectClick,
        containerColor = CapCutTeal,
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("projects_fab_new")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
          Text(text = "New project", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    },
    containerColor = CapCutBackground
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "My Projects",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = CapCutTextPrimary
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search projects...", color = CapCutTextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CapCutTextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CapCutTeal,
          unfocusedBorderColor = CapCutSurfaceVariant,
          focusedTextColor = CapCutTextPrimary,
          unfocusedTextColor = CapCutTextPrimary,
          focusedContainerColor = CapCutSurface,
          unfocusedContainerColor = CapCutSurface
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("projects_search_bar")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Tabs
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = CapCutBackground,
        contentColor = CapCutTeal,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = CapCutTeal,
            height = 2.dp
          )
        }
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = title,
                color = if (selectedTab == index) CapCutTeal else CapCutTextSecondary,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (filteredProjects.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = CapCutTextSecondary, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No matching projects found" else "No saved projects yet",
              color = CapCutTextSecondary,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap + New project to begin",
              color = CapCutTeal,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredProjects) { project ->
            ProjectCard(
              project = project,
              onClick = { onOpenProject(project.id) },
              onDelete = {
                scope.launch {
                  db.projectDao().deleteProjectById(project.id)
                  Toast.makeText(context, "Deleted project", Toast.LENGTH_SHORT).show()
                }
              }
            )
          }
        }
      }
    }
  }
}
