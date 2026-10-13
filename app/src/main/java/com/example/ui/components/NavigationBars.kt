package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.UserEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicGold
import com.example.ui.viewmodel.Screen

@Composable
fun AppHeader(
  user: UserEntity?,
  unreadAnnouncementsCount: Int,
  onNotificationsClick: () -> Unit,
  onSearchClick: () -> Unit,
  onProfileClick: () -> Unit,
  onAiTutorClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        // App emblem
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AcademicBluePrimary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "TTS Academy",
            tint = AcademicGold,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "TTS ACADEMY",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = "Freshman University Learning",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (onAiTutorClick != null) {
          IconButton(
            onClick = onAiTutorClick,
            modifier = Modifier.testTag("ai_tutor_icon_button")
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Gemini AI Tutor",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }

        IconButton(
          onClick = onSearchClick,
          modifier = Modifier.testTag("search_icon_button")
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onNotificationsClick,
          modifier = Modifier.testTag("notifications_icon_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadAnnouncementsCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                  Text("$unreadAnnouncementsCount")
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Announcements",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Profile Avatar Button
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onProfileClick)
            .testTag("profile_avatar_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = user?.name?.firstOrNull()?.uppercase() ?: "S",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }
  }
}

@Composable
fun DetailTopBar(
  title: String,
  subtitle: String? = null,
  onBackClick: () -> Unit,
  isBookmarked: Boolean = false,
  onBookmarkClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBackClick,
        modifier = Modifier.testTag("detail_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (onBookmarkClick != null) {
        IconButton(
          onClick = onBookmarkClick,
          modifier = Modifier.testTag("detail_bookmark_button")
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
            tint = if (isBookmarked) AcademicGold else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun AppBottomNavigation(
  currentScreen: Screen,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier.testTag("app_bottom_nav"),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp
  ) {
    NavigationBarItem(
      selected = currentScreen == Screen.DASHBOARD,
      onClick = { onNavigate(Screen.DASHBOARD) },
      icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
      label = { Text("Home") },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
      )
    )

    NavigationBarItem(
      selected = currentScreen == Screen.COURSES || currentScreen == Screen.COURSE_DETAIL,
      onClick = { onNavigate(Screen.COURSES) },
      icon = { Icon(Icons.Default.School, contentDescription = "Courses") },
      label = { Text("Courses") },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
      )
    )

    NavigationBarItem(
      selected = currentScreen == Screen.NOTES || currentScreen == Screen.NOTE_DETAIL,
      onClick = { onNavigate(Screen.NOTES) },
      icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Notes") },
      label = { Text("Notes") },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
      )
    )

    NavigationBarItem(
      selected = currentScreen == Screen.PAST_EXAMS || currentScreen == Screen.PAST_EXAM_DETAIL,
      onClick = { onNavigate(Screen.PAST_EXAMS) },
      icon = { Icon(Icons.Default.Assignment, contentDescription = "Past Exams") },
      label = { Text("Exams") },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
      )
    )

    NavigationBarItem(
      selected = currentScreen == Screen.PROGRESS,
      onClick = { onNavigate(Screen.PROGRESS) },
      icon = { Icon(Icons.Default.BarChart, contentDescription = "Progress") },
      label = { Text("Progress") },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
      )
    )
  }
}
