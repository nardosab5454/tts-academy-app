package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.SuccessGreen

@Composable
fun NotesScreen(
  notes: List<NoteEntity>,
  courses: List<CourseEntity>,
  onNoteClick: (NoteEntity) -> Unit,
  isUnlocked: Boolean = true,
  onUnlockClick: () -> Unit = {},
  isNoteDownloaded: ((NoteEntity) -> Boolean)? = null,
  onDownloadNote: ((NoteEntity) -> Unit)? = null,
  onRefreshCloud: (() -> Unit)? = null,
  isRefreshing: Boolean = false,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCourseId by remember { mutableStateOf<String?>("All") }

  val courseMap = remember(courses) { courses.associateBy { it.id } }

  val filteredNotes = notes.filter { note ->
    (selectedCourseId == "All" || note.courseId == selectedCourseId) &&
      (searchQuery.isBlank() ||
        note.title.contains(searchQuery, ignoreCase = true) ||
        note.summary.contains(searchQuery, ignoreCase = true) ||
        note.contentMarkdown.contains(searchQuery, ignoreCase = true))
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("notes_screen")
  ) {
    if (!isUnlocked) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Lecture Notes Locked",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Enter your admin 6-digit access code to unlock all study notes and formula sheets.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onUnlockClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicCobalt)
          ) {
            Text("Unlock", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Search Bar with Cloud Sync Refresh Action
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search notes, definitions, formulas...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("notes_search_input")
      )

      if (onRefreshCloud != null) {
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = onRefreshCloud,
          enabled = !isRefreshing,
          modifier = Modifier.testTag("notes_refresh_button")
        ) {
          if (isRefreshing) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync Notes from Cloud",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // Course Filter Chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      item {
        FilterChip(
          selected = selectedCourseId == "All",
          onClick = { selectedCourseId = "All" },
          label = { Text("All Courses") }
        )
      }
      items(courses) { course ->
        FilterChip(
          selected = selectedCourseId == course.id,
          onClick = { selectedCourseId = course.id },
          label = { Text(course.code) }
        )
      }
    }

    if (filteredNotes.isEmpty()) {
      EmptyStateView("No revision notes matching your search.")
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredNotes) { note ->
          val course = courseMap[note.courseId]
          val downloaded = isNoteDownloaded?.invoke(note) == true
          NoteCardItem(
            note = note,
            courseCode = course?.code ?: "COURSE",
            isUnlocked = isUnlocked,
            isDownloaded = downloaded,
            onDownload = if (!downloaded && isUnlocked && onDownloadNote != null) { { onDownloadNote(note) } } else null,
            onClick = { onNoteClick(note) }
          )
        }
      }
    }
  }
}

@Composable
private fun NoteCardItem(
  note: NoteEntity,
  courseCode: String,
  isUnlocked: Boolean = true,
  isDownloaded: Boolean = false,
  onDownload: (() -> Unit)? = null,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(2.dp),
    border = if (!isUnlocked) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = courseCode,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (!isUnlocked) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Locked",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          } else if (isDownloaded) {
            Surface(
              color = SuccessGreen.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Offline Ready",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = SuccessGreen
                )
              }
            }
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onDownload != null) {
            IconButton(
              onClick = onDownload,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download note for offline study",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            text = "${note.readTimeMinutes} min read",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = note.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = note.summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (note.keyFormulas.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = note.keyFormulas,
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Read Revision Note",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
fun NoteDetailScreen(
  note: NoteEntity,
  course: CourseEntity?,
  isBookmarked: Boolean,
  onBackClick: () -> Unit,
  onToggleBookmark: () -> Unit,
  onAskAiTutor: ((NoteEntity) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("note_detail_screen")
  ) {
    DetailTopBar(
      title = note.title,
      subtitle = "${course?.title ?: "Revision Note"} • ${note.readTimeMinutes} min read",
      onBackClick = onBackClick,
      isBookmarked = isBookmarked,
      onBookmarkClick = onToggleBookmark
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Summary Callout Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Key Takeaways & Concept Summary",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = note.summary,
              style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
              color = MaterialTheme.colorScheme.onSurface
            )

            if (onAskAiTutor != null) {
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = { onAskAiTutor(note) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Research & Clarify with Gemini AI", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // Key Formulas Banner
      if (note.keyFormulas.isNotBlank()) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Functions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Crucial Formulas",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = note.keyFormulas,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      // Note Body Markdown
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = note.contentMarkdown,
              style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                fontSize = 15.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}
