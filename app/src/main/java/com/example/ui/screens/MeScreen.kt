package com.example.ui.screens

import com.example.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun MeScreen() {
  val context = LocalContext.current
  val db = remember { AppDatabase.getDatabase(context) }
  val projects by db.projectDao().getAllProjects().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()

  var hapticFeedbackEnabled by remember { mutableStateOf(true) }
  var smartHdrEnabled by remember { mutableStateOf(true) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CapCutBackground)
      .windowInsetsPadding(WindowInsets.statusBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Header Profile Card
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(CapCutSurface)
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(CapCutTeal),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column {
        Text("Video Creator", color = CapCutTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(CapCutTeal)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("100% Offline Mode Active", color = CapCutTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Offline Stats Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatCard(label = "Projects", value = "${projects.size}", modifier = Modifier.weight(1f))
      StatCard(label = "Clips Edited", value = "${projects.sumOf { it.clipCount }}", modifier = Modifier.weight(1f))
      StatCard(label = "Storage", value = "28 MB", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Preferences Section
    Text("PREFERENCES", color = CapCutTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(CapCutSurface)
    ) {
      SettingsToggleItem(
        icon = Icons.Default.Vibration,
        title = "Haptic Vibration",
        subtitle = "Vibrate on split, cut and snap",
        checked = hapticFeedbackEnabled,
        onCheckedChange = { hapticFeedbackEnabled = it }
      )

      SettingsToggleItem(
        icon = Icons.Default.HighQuality,
        title = "High-Bitrate Export",
        subtitle = "Hardware-accelerated offline encoder",
        checked = smartHdrEnabled,
        onCheckedChange = { smartHdrEnabled = it }
      )

      SettingsActionItem(
        icon = Icons.Default.DeleteSweep,
        title = "Clear Cache",
        subtitle = "Free temporary preview buffer",
        onClick = {
          try {
            context.cacheDir.deleteRecursively()
          } catch (_: Exception) {}
          Toast.makeText(context, "Temporary cache cleared", Toast.LENGTH_SHORT).show()
        }
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // About Section
    Text("ABOUT", color = CapCutTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(CapCutSurface)
    ) {
      SettingsActionItem(
        icon = Icons.Default.Security,
        title = "Privacy & Offline Architecture",
        subtitle = "No cloud uploads, data remains on device",
        onClick = {
          Toast.makeText(context, "All processing occurs locally on-device", Toast.LENGTH_SHORT).show()
        }
      )

      SettingsActionItem(
        icon = Icons.Default.Info,
        title = "Version",
        subtitle = stringResource(R.string.app_name),
        onClick = {}
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(CapCutSurface)
      .padding(vertical = 14.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, color = CapCutTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(2.dp))
      Text(label, color = CapCutTextSecondary, fontSize = 11.sp)
    }
  }
}

@Composable
private fun SettingsToggleItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = CapCutTeal, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(title, color = CapCutTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = CapCutTextSecondary, fontSize = 11.sp)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CapCutTeal)
    )
  }
}

@Composable
private fun SettingsActionItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = CapCutTeal, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(title, color = CapCutTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = CapCutTextSecondary, fontSize = 11.sp)
      }
    }
    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = CapCutTextSecondary, modifier = Modifier.size(14.dp))
  }
}
