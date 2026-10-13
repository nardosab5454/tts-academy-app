package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.UserEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.ErrorRed
import com.example.ui.viewmodel.Screen

@Composable
fun SearchScreen(
  query: String,
  onQueryChange: (String) -> Unit,
  courseResults: List<CourseEntity>,
  noteResults: List<NoteEntity>,
  questionResults: List<QuestionEntity>,
  onCourseClick: (CourseEntity) -> Unit,
  onNoteClick: (NoteEntity) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("search_screen")
  ) {
    DetailTopBar(
      title = "Global Search",
      subtitle = "Search courses, notes, and questions",
      onBackClick = onBackClick
    )

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = { Text("Search any formula, topic, course, or exam question...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("global_search_input")
    )

    if (query.isBlank()) {
      EmptyStateView("Type a search term above to find learning materials.")
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Matching Courses
        if (courseResults.isNotEmpty()) {
          item {
            Text(
              text = "Courses (${courseResults.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
          items(courseResults) { course ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCourseClick(course) },
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "${course.code}: ${course.title}", fontWeight = FontWeight.Bold)
                  Text(text = course.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
              }
            }
          }
        }

        // Matching Notes
        if (noteResults.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Revision Notes (${noteResults.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
          items(noteResults) { note ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onNoteClick(note) },
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = note.title, fontWeight = FontWeight.Bold)
                  Text(text = note.summary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
              }
            }
          }
        }

        // Matching Questions
        if (questionResults.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Questions & Explanations (${questionResults.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
          items(questionResults) { q ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(text = q.questionText, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Answer: (${q.correctAnswer}) • ${q.explanationGeneral}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2
                )
              }
            }
          }
        }

        if (courseResults.isEmpty() && noteResults.isEmpty() && questionResults.isEmpty()) {
          item {
            EmptyStateView("No matching results found for '$query'.")
          }
        }
      }
    }
  }
}

@Composable
fun AnnouncementsScreen(
  announcements: List<AnnouncementEntity>,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("announcements_screen")
  ) {
    DetailTopBar(
      title = "Academic Announcements",
      subtitle = "Official Freshman Updates",
      onBackClick = onBackClick
    )

    if (announcements.isEmpty()) {
      EmptyStateView("No new announcements at this time.")
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(announcements) { ann ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  color = if (ann.isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = if (ann.isUrgent) "URGENT NOTICE" else ann.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (ann.isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                if (!ann.targetCourseTitle.isNullOrBlank()) {
                  Text(
                    text = ann.targetCourseTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = ann.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = ann.message,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(
  user: UserEntity?,
  allUsers: List<UserEntity>,
  currentTheme: String,
  onThemeSelect: (String) -> Unit,
  onLogin: (String, String, (Boolean, String) -> Unit) -> Unit,
  onSwitchUser: (String) -> Unit,
  onOpenAdminPortal: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("profile_screen"),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      DetailTopBar(
        title = "Student Profile & Settings",
        onBackClick = onBackClick
      )
    }

    // Profile Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = user?.name?.firstOrNull()?.uppercase() ?: "S",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
              )
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = user?.name ?: "Student",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = user?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Student ID", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(user?.studentId ?: "U2024", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Department", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(user?.department ?: "General Freshman", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Daily Target", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${user?.studyGoalMinutesPerDay ?: 45} mins/day", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // Cloud Database Status Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = "Cloud Database Connected",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Cloud Firestore Database",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Real-time sync active for progress & bookmarks",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Sign Out Button
    item {
      val context = LocalContext.current
      val coroutineScope = rememberCoroutineScope()
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              coroutineScope.launch {
                try {
                  CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}
                FirebaseAuth.getInstance().signOut()
              }
            }
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Sign Out",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Log out from your Google account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // System Console - Visible ONLY to authenticated administrators
    if (user?.role == "ADMIN") {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenAdminPortal() }
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Academic System Console",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Manage courses, questions, notes & user roles",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Theme Switcher Section
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "App Theme Mode",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(10.dp))

          ThemeOptionRow("System Default", Icons.Default.PhoneAndroid, currentTheme == "SYSTEM") {
            onThemeSelect("SYSTEM")
          }
          ThemeOptionRow("Light Mode", Icons.Default.LightMode, currentTheme == "LIGHT") {
            onThemeSelect("LIGHT")
          }
          ThemeOptionRow("Dark Mode", Icons.Default.DarkMode, currentTheme == "DARK") {
            onThemeSelect("DARK")
          }
        }
      }
    }

    // Student Profiles Switcher (Regular Students Only)
    val studentUsers = allUsers.filter { it.role == "STUDENT" }
    if (studentUsers.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Student Accounts",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Switch between enrolled student profiles.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            studentUsers.forEach { u ->
              val isCurrent = u.id == user?.id
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable { onSwitchUser(u.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(text = u.name, fontWeight = FontWeight.Bold)
                    Text(text = "${u.studentId} • ${u.department}", style = MaterialTheme.typography.bodySmall)
                  }
                  if (isCurrent) {
                    Text("Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                  } else {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch", tint = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      }
    }
  }


}

@Composable
private fun ThemeOptionRow(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onSelect: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onSelect)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(selected = isSelected, onClick = onSelect)
    Spacer(modifier = Modifier.width(8.dp))
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.width(10.dp))
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
  }
}
