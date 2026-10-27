package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.UserEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
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
  currentTheme: String = "SYSTEM",
  onToggleTheme: (() -> Unit)? = null,
  isOnline: Boolean = true,
  modifier: Modifier = Modifier
) {
  val isDark = when (currentTheme) {
    "LIGHT" -> false
    "DARK" -> true
    else -> isSystemInDarkTheme()
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // App emblem & title
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(AcademicBluePrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "TTS Academy",
              tint = AcademicGold,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "TTS ACADEMY",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = user?.department ?: "Freshman University",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Action icons: AI Tutor, Theme Toggle (Day/Dark), Search, Notifications, Avatar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          if (onAiTutorClick != null) {
            IconButton(
              onClick = onAiTutorClick,
              modifier = Modifier
                .size(40.dp)
                .testTag("ai_tutor_icon_button")
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Gemini AI Tutor",
                tint = AcademicGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Day and Dark Mode toggle button
          if (onToggleTheme != null) {
            IconButton(
              onClick = onToggleTheme,
              modifier = Modifier
                .size(40.dp)
                .testTag("theme_toggle_button")
            ) {
              Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Switch to Day Mode" else "Switch to Dark Mode",
                tint = if (isDark) AcademicGold else AcademicCobalt,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          IconButton(
            onClick = onSearchClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("search_icon_button")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("notifications_icon_button")
          ) {
            BadgedBox(
              badge = {
                if (unreadAnnouncementsCount > 0) {
                  Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                  ) {
                    Text("$unreadAnnouncementsCount", fontSize = 9.sp)
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Announcements",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Profile Avatar Button (Squircle with sleek border)
          Surface(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onProfileClick)
              .testTag("profile_avatar_button"),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = user?.name?.firstOrNull()?.uppercase() ?: "S",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }

      if (!isOnline) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth().testTag("offline_warning_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CloudOff,
              contentDescription = "Offline indicator",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Offline Mode: Previously downloaded materials remain accessible.",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
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
  onRefreshClick: (() -> Unit)? = null,
  isRefreshing: Boolean = false,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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

      if (onRefreshClick != null) {
        IconButton(
          onClick = onRefreshClick,
          enabled = !isRefreshing,
          modifier = Modifier.testTag("detail_refresh_button")
        ) {
          if (isRefreshing) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync from Cloud",
              tint = MaterialTheme.colorScheme.primary
            )
          }
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
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("app_bottom_nav"),
      shape = RoundedCornerShape(32.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      shadowElevation = 10.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val navItems = listOf(
          ModernNavItem(
            label = "Home",
            icon = Icons.Default.Home,
            isSelected = currentScreen == Screen.DASHBOARD,
            tag = "nav_tab_home",
            onClick = { onNavigate(Screen.DASHBOARD) }
          ),
          ModernNavItem(
            label = "Courses",
            icon = Icons.Default.School,
            isSelected = currentScreen == Screen.COURSES || currentScreen == Screen.COURSE_DETAIL,
            tag = "nav_tab_courses",
            onClick = { onNavigate(Screen.COURSES) }
          ),
          ModernNavItem(
            label = "Notes",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            isSelected = currentScreen == Screen.NOTES || currentScreen == Screen.NOTE_DETAIL,
            tag = "nav_tab_notes",
            onClick = { onNavigate(Screen.NOTES) }
          ),
          ModernNavItem(
            label = "Exams",
            icon = Icons.AutoMirrored.Filled.Assignment,
            isSelected = currentScreen == Screen.PAST_EXAMS || currentScreen == Screen.PAST_EXAM_DETAIL,
            tag = "nav_tab_exams",
            onClick = { onNavigate(Screen.PAST_EXAMS) }
          ),
          ModernNavItem(
            label = "Progress",
            icon = Icons.Default.BarChart,
            isSelected = currentScreen == Screen.PROGRESS,
            tag = "nav_tab_progress",
            onClick = { onNavigate(Screen.PROGRESS) }
          )
        )

        navItems.forEach { item ->
          ModernNavigationTabItem(
            item = item,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

private data class ModernNavItem(
  val label: String,
  val icon: ImageVector,
  val isSelected: Boolean,
  val tag: String,
  val onClick: () -> Unit
)

@Composable
private fun ModernNavigationTabItem(
  item: ModernNavItem,
  modifier: Modifier = Modifier
) {
  val animatedIconColor by animateColorAsState(
    targetValue = if (item.isSelected) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    },
    animationSpec = tween(200),
    label = "tabIcon"
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = item.onClick)
      .padding(vertical = 4.dp)
      .testTag(item.tag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        tint = animatedIconColor,
        modifier = Modifier.size(22.dp)
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = item.label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
          fontSize = 11.sp,
          letterSpacing = 0.2.sp
        ),
        color = animatedIconColor,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(3.dp))

      // Elegant active dot indicator matching reference image
      Box(
        modifier = Modifier
          .size(4.dp)
          .clip(CircleShape)
          .background(if (item.isSelected) AcademicGold else Color.Transparent)
      )
    }
  }
}
