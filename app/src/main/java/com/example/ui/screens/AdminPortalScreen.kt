package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.UserEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import java.util.UUID

@Composable
fun AdminPortalScreen(
  courses: List<CourseEntity>,
  allUsers: List<UserEntity>,
  announcements: List<AnnouncementEntity>,
  onBackClick: () -> Unit,
  onSaveCourse: (CourseEntity) -> Unit,
  onDeleteCourse: (String) -> Unit,
  onSaveQuestion: (QuestionEntity) -> Unit,
  onSaveNote: (NoteEntity) -> Unit,
  onSavePastExam: (PastExamEntity) -> Unit,
  onPublishAnnouncement: (String, String, String, Boolean) -> Unit,
  onResetSeedData: () -> Unit,
  onUpdateUserRole: (String, String) -> Unit,
  onCreateAdmin: (String, String, String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Dashboard", "Courses", "Add Question", "Add Note", "Announcements", "Admins & Users")

  // Dialog states for creation
  var showNewCourseDialog by remember { mutableStateOf(false) }
  var showNewAnnouncementDialog by remember { mutableStateOf(false) }
  var showNewAdminDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("admin_portal_screen")
  ) {
    DetailTopBar(
      title = "Administrator Portal",
      subtitle = "Content & Role Management Dashboard",
      onBackClick = onBackClick
    )

    TabRow(selectedTabIndex = selectedTab) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(title, style = MaterialTheme.typography.labelMedium) }
        )
      }
    }

    when (selectedTab) {
      0 -> AdminOverviewTab(courses, allUsers, announcements, onResetSeedData)
      1 -> AdminCoursesTab(courses, { showNewCourseDialog = true }, onDeleteCourse)
      2 -> AdminAddQuestionTab(courses, onSaveQuestion)
      3 -> AdminAddNoteTab(courses, onSaveNote)
      4 -> AdminAnnouncementsTab(announcements, { showNewAnnouncementDialog = true })
      5 -> AdminStudentsTab(allUsers, { showNewAdminDialog = true }, onUpdateUserRole)
    }
  }

  // New Course Dialog
  if (showNewCourseDialog) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("STEM") }

    AlertDialog(
      onDismissRequest = { showNewCourseDialog = false },
      title = { Text("Create New Course") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. MATH 103)") }, singleLine = true)
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, singleLine = true)
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (STEM, Humanities, etc.)") }, singleLine = true)
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (code.isNotBlank() && title.isNotBlank()) {
              val newCourse = CourseEntity(
                id = "C_" + UUID.randomUUID().toString().take(6).uppercase(),
                code = code.trim(),
                title = title.trim(),
                description = desc.trim(),
                category = category.trim(),
                iconName = "school",
                totalQuestions = 0,
                totalNotes = 0,
                isPublished = true,
                orderIndex = courses.size + 1
              )
              onSaveCourse(newCourse)
              showNewCourseDialog = false
            }
          }
        ) {
          Text("Save Course")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewCourseDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // New Announcement Dialog
  if (showNewAnnouncementDialog) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showNewAnnouncementDialog = false },
      title = { Text("Publish Announcement") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
          OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message Body") }, maxLines = 4)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isUrgent = !isUrgent }
          ) {
            Text("Mark as Urgent / Alert Notice: ${if (isUrgent) "YES" else "NO"}", fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank() && message.isNotBlank()) {
              onPublishAnnouncement(title, message, "EXAM", isUrgent)
              showNewAnnouncementDialog = false
            }
          }
        ) {
          Text("Broadcast")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewAnnouncementDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // New Administrator Dialog
  if (showNewAdminDialog) {
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminDept by remember { mutableStateOf("Academic Directorate") }

    AlertDialog(
      onDismissRequest = { showNewAdminDialog = false },
      title = { Text("Create New Administrator") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("Full Name") }, singleLine = true)
          OutlinedTextField(value = adminEmail, onValueChange = { adminEmail = it }, label = { Text("Email Address") }, singleLine = true)
          OutlinedTextField(value = adminPassword, onValueChange = { adminPassword = it }, label = { Text("Password") }, singleLine = true)
          OutlinedTextField(value = adminDept, onValueChange = { adminDept = it }, label = { Text("Department / Directorate") }, singleLine = true)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (adminEmail.isNotBlank()) {
              onCreateAdmin(adminName, adminEmail, adminPassword, adminDept)
              showNewAdminDialog = false
            }
          }
        ) {
          Text("Save Admin")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewAdminDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun AdminOverviewTab(
  courses: List<CourseEntity>,
  users: List<UserEntity>,
  announcements: List<AnnouncementEntity>,
  onResetSeedData: () -> Unit
) {
  var showResetConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "System Metrics",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("Courses", "${courses.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        AdminStatCard("Students", "${users.filter { it.role == "STUDENT" }.size}", SuccessGreen, Modifier.weight(1f))
        AdminStatCard("Notices", "${announcements.size}", AcademicGold, Modifier.weight(1f))
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text("Academic Database Management", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "The TTS Academy curriculum database contains 12 freshman university courses, pre-seeded past exams, and question banks. Administrators can add new courses, questions with explanations, revision notes, and past exams at any time without updating the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedButton(
            onClick = { showResetConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Re-seed Factory Curriculum")
          }
        }
      }
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Reset to Factory Data?") },
      text = { Text("This will re-populate all 12 university courses, standard questions, notes, and past exams.") },
      confirmButton = {
        Button(
          onClick = {
            showResetConfirm = false
            onResetSeedData()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Reset Now")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun AdminStatCard(title: String, value: String, tint: Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(1.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = tint)
    }
  }
}

@Composable
private fun AdminCoursesTab(
  courses: List<CourseEntity>,
  onAddNew: () -> Unit,
  onDelete: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Manage Courses (${courses.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Button(onClick = onAddNew) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Course")
        }
      }
    }

    items(courses) { course ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${course.code}: ${course.title}", fontWeight = FontWeight.Bold)
            Text("${course.category} • ${course.totalQuestions} Questions", style = MaterialTheme.typography.bodySmall)
          }
          IconButton(onClick = { onDelete(course.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
          }
        }
      }
    }
  }
}

@Composable
private fun AdminAddQuestionTab(
  courses: List<CourseEntity>,
  onSaveQuestion: (QuestionEntity) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "") }
  var questionText by remember { mutableStateOf("") }
  var optA by remember { mutableStateOf("") }
  var optB by remember { mutableStateOf("") }
  var optC by remember { mutableStateOf("") }
  var optD by remember { mutableStateOf("") }
  var correctChoice by remember { mutableStateOf("A") }
  var generalExplanation by remember { mutableStateOf("") }
  var expA by remember { mutableStateOf("") }
  var expB by remember { mutableStateOf("") }
  var expC by remember { mutableStateOf("") }
  var expD by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text("Create Multiple-Choice Question", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      Text("Add authentic educational question with choice explanations.", style = MaterialTheme.typography.bodySmall)
    }

    item {
      OutlinedTextField(
        value = questionText,
        onValueChange = { questionText = it },
        label = { Text("Question Text") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
      )
    }

    item { OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth()) }

    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Correct Choice:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        listOf("A", "B", "C", "D").forEach { choice ->
          Surface(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .clickable { correctChoice = choice },
            color = if (correctChoice == choice) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = choice,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              color = if (correctChoice == choice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = generalExplanation,
        onValueChange = { generalExplanation = it },
        label = { Text("General Explanation (Why correct answer is right)") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
      )
    }

    item { OutlinedTextField(value = expA, onValueChange = { expA = it }, label = { Text("Explanation for A") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expB, onValueChange = { expB = it }, label = { Text("Explanation for B") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expC, onValueChange = { expC = it }, label = { Text("Explanation for C") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expD, onValueChange = { expD = it }, label = { Text("Explanation for D") }, modifier = Modifier.fillMaxWidth()) }

    item {
      Button(
        onClick = {
          if (questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
            val q = QuestionEntity(
              id = "Q_" + UUID.randomUUID().toString().take(6),
              courseId = selectedCourseId.ifBlank { courses.firstOrNull()?.id ?: "MATH_101" },
              topicId = "T_GEN",
              questionText = questionText.trim(),
              optionA = optA.trim(),
              optionB = optB.trim(),
              optionC = optC.trim(),
              optionD = optD.trim(),
              correctAnswer = correctChoice,
              explanationGeneral = generalExplanation.trim().ifBlank { "Option ($correctChoice) is the mathematically correct derivation." },
              explanationA = expA.trim().ifBlank { if (correctChoice == "A") "Correct answer." else "Incorrect choice." },
              explanationB = expB.trim().ifBlank { if (correctChoice == "B") "Correct answer." else "Incorrect choice." },
              explanationC = expC.trim().ifBlank { if (correctChoice == "C") "Correct answer." else "Incorrect choice." },
              explanationD = expD.trim().ifBlank { if (correctChoice == "D") "Correct answer." else "Incorrect choice." },
              difficulty = "MEDIUM",
              isPastExam = false,
              isVerified = true,
              isPublished = true
            )
            onSaveQuestion(q)
            // Clear inputs
            questionText = ""
            optA = ""
            optB = ""
            optC = ""
            optD = ""
            generalExplanation = ""
          }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Text("Save & Publish Question")
      }
    }
  }
}

@Composable
private fun AdminAddNoteTab(
  courses: List<CourseEntity>,
  onSaveNote: (NoteEntity) -> Unit
) {
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "") }
  var title by remember { mutableStateOf("") }
  var summary by remember { mutableStateOf("") }
  var formulas by remember { mutableStateOf("") }
  var markdownContent by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text("Create Revision Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }

    item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Note Title") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Concept Summary") }, modifier = Modifier.fillMaxWidth(), maxLines = 2) }
    item { OutlinedTextField(value = formulas, onValueChange = { formulas = it }, label = { Text("Key Formulas") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = markdownContent, onValueChange = { markdownContent = it }, label = { Text("Detailed Revision Text / Markdown") }, modifier = Modifier.fillMaxWidth(), minLines = 5) }

    item {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val note = NoteEntity(
              id = "NOTE_" + UUID.randomUUID().toString().take(6),
              courseId = selectedCourseId.ifBlank { courses.firstOrNull()?.id ?: "MATH_101" },
              topicId = "T_GEN",
              title = title.trim(),
              summary = summary.trim(),
              contentMarkdown = markdownContent.trim(),
              keyFormulas = formulas.trim(),
              readTimeMinutes = 5,
              isPublished = true
            )
            onSaveNote(note)
            title = ""
            summary = ""
            formulas = ""
            markdownContent = ""
          }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Text("Save Revision Note")
      }
    }
  }
}

@Composable
private fun AdminAnnouncementsTab(
  announcements: List<AnnouncementEntity>,
  onNewClick: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Announcements (${announcements.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Button(onClick = onNewClick) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Broadcast")
        }
      }
    }

    items(announcements) { ann ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = ann.title, fontWeight = FontWeight.Bold)
          Text(text = ann.message, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

@Composable
private fun AdminStudentsTab(
  users: List<UserEntity>,
  onAddAdminClick: () -> Unit,
  onUpdateRole: (String, String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Admin & User Management (${users.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("Promote students to administrator or create new admin accounts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAddAdminClick) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Admin")
        }
      }
    }

    items(users) { user ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = user.name, fontWeight = FontWeight.Bold)
              Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(text = "${user.studentId} • ${user.department}", style = MaterialTheme.typography.bodySmall)
            }
            Surface(
              color = if (user.role == "ADMIN") AcademicGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = user.role,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (user.role == "ADMIN") AcademicGold else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (user.role == "ADMIN") "Full Administrative Privileges" else "Standard Student Account",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.email.equals("spikenarrd@gmail.com", ignoreCase = true)) {
              Text(
                text = "Primary Admin",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AcademicGold
              )
            } else if (user.role == "ADMIN") {
              OutlinedButton(
                onClick = { onUpdateRole(user.id, "STUDENT") },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text("Revoke Admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
              }
            } else {
              Button(
                onClick = { onUpdateRole(user.id, "ADMIN") },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text("Make Admin", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }
  }
}
