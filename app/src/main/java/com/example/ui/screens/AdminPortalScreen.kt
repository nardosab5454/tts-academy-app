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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicBlueDark
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import java.util.UUID

@Composable
fun AdminPortalScreen(
  courses: List<CourseEntity>,
  allUsers: List<UserEntity>,
  announcements: List<AnnouncementEntity>,
  accessCodes: List<AccessCodeEntity> = emptyList(),
  questions: List<QuestionEntity> = emptyList(),
  notes: List<NoteEntity> = emptyList(),
  pastExams: List<PastExamEntity> = emptyList(),
  topics: List<TopicEntity> = emptyList(),
  onBackClick: () -> Unit,
  onSaveCourse: (CourseEntity) -> Unit,
  onDeleteCourse: (String) -> Unit,
  onSaveQuestion: (QuestionEntity) -> Unit,
  onDeleteQuestion: (String) -> Unit = {},
  onSaveNote: (NoteEntity) -> Unit,
  onDeleteNote: (String) -> Unit = {},
  onSavePastExam: (PastExamEntity) -> Unit,
  onDeletePastExam: (String) -> Unit = {},
  onSaveTopic: (TopicEntity) -> Unit = {},
  onDeleteTopic: (String) -> Unit = {},
  onPublishAnnouncement: (String, String, String, Boolean) -> Unit,
  onResetSeedData: () -> Unit,
  onUpdateUserRole: (String, String) -> Unit,
  onCreateAdmin: (String, String, String, String) -> Unit,
  onPromoteEmail: (String) -> Unit = {},
  onDeleteAnnouncement: (String) -> Unit = {},
  onGenerateCode: (String, String, String?, (String) -> Unit) -> Unit = { _, _, _, _ -> },
  onDeleteCode: (String) -> Unit = {},
  onSyncCloud: () -> Unit = {},
  isSyncing: Boolean = false,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var activeCourseIdForAdd by remember(courses) {
    mutableStateOf(courses.firstOrNull()?.id ?: "")
  }

  val tabs = listOf(
    "Dashboard",
    "Courses",
    "Add Question",
    "Add Note",
    "Past Year Questions",
    "Customize Content",
    "Announcements",
    "Admins & Users",
    "Access Codes"
  )

  // Dialog states for creation
  var showNewCourseDialog by remember { mutableStateOf(false) }
  var showNewAnnouncementDialog by remember { mutableStateOf(false) }
  var showNewAdminDialog by remember { mutableStateOf(false) }
  var newlyAddedCourse by remember { mutableStateOf<CourseEntity?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("admin_portal_screen")
  ) {
    DetailTopBar(
      title = "Administrator Portal",
      subtitle = "Curriculum, Content & Roles Management",
      onBackClick = onBackClick,
      onRefreshClick = onSyncCloud,
      isRefreshing = isSyncing
    )

    ScrollableTabRow(
      selectedTabIndex = selectedTab,
      edgePadding = 16.dp
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(title, style = MaterialTheme.typography.labelMedium) }
        )
      }
    }

    when (selectedTab) {
      0 -> AdminOverviewTab(courses, allUsers, announcements, accessCodes, onResetSeedData, onSyncCloud, isSyncing)
      1 -> AdminCoursesTab(
        courses = courses,
        questions = questions,
        notes = notes,
        topics = topics,
        onAddNew = { showNewCourseDialog = true },
        onSaveCourse = onSaveCourse,
        onDelete = onDeleteCourse,
        onSaveTopic = onSaveTopic,
        onDeleteTopic = onDeleteTopic,
        onAddQuestionForCourse = { cId ->
          activeCourseIdForAdd = cId
          selectedTab = 2 // Switch to Add Question
        },
        onAddNoteForCourse = { cId ->
          activeCourseIdForAdd = cId
          selectedTab = 3 // Switch to Add Note
        },
        onManageCourse = { cId ->
          activeCourseIdForAdd = cId
          selectedTab = 5 // Switch to Customize Content
        }
      )
      2 -> AdminAddQuestionTab(
        courses = courses,
        topics = topics,
        initialCourseId = activeCourseIdForAdd,
        onCourseSelected = { activeCourseIdForAdd = it },
        onSaveQuestion = onSaveQuestion,
        onSaveTopic = onSaveTopic
      )
      3 -> AdminAddNoteTab(
        courses = courses,
        topics = topics,
        initialCourseId = activeCourseIdForAdd,
        onCourseSelected = { activeCourseIdForAdd = it },
        onSaveNote = onSaveNote,
        onSaveTopic = onSaveTopic
      )
      4 -> AdminPastExamTab(
        courses = courses,
        questions = questions,
        initialCourseId = activeCourseIdForAdd,
        onSaveQuestion = onSaveQuestion,
        onSavePastExam = onSavePastExam,
        onDeleteQuestion = onDeleteQuestion
      )
      5 -> AdminManageContentTab(
        courses = courses,
        questions = questions,
        notes = notes,
        topics = topics,
        initialCourseId = activeCourseIdForAdd,
        onSaveQuestion = onSaveQuestion,
        onDeleteQuestion = onDeleteQuestion,
        onSaveNote = onSaveNote,
        onDeleteNote = onDeleteNote,
        onSaveTopic = onSaveTopic,
        onDeleteTopic = onDeleteTopic,
        onSwitchToAddQuestion = { cId ->
          activeCourseIdForAdd = cId
          selectedTab = 2
        },
        onSwitchToAddNote = { cId ->
          activeCourseIdForAdd = cId
          selectedTab = 3
        }
      )
      6 -> AdminAnnouncementsTab(announcements, { showNewAnnouncementDialog = true }, onDeleteAnnouncement)
      7 -> AdminStudentsTab(allUsers, { showNewAdminDialog = true }, onUpdateUserRole, onPromoteEmail)
      8 -> AdminAccessCodesTab(accessCodes, onGenerateCode, onDeleteCode)
    }
  }

  // Follow-up dialog after creating a course
  if (newlyAddedCourse != null) {
    val course = newlyAddedCourse!!
    AlertDialog(
      onDismissRequest = { newlyAddedCourse = null },
      title = { Text("Course Created Successfully!") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("You added '${course.code}: ${course.title}'. What would you like to populate in this newly added course now?")
        }
      },
      confirmButton = {
        Button(
          onClick = {
            activeCourseIdForAdd = course.id
            selectedTab = 2 // Add Question
            newlyAddedCourse = null
          }
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Add Question")
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              activeCourseIdForAdd = course.id
              selectedTab = 3 // Add Note
              newlyAddedCourse = null
            }
          ) {
            Text("+ Add Note")
          }
          TextButton(onClick = { newlyAddedCourse = null }) {
            Text("Done")
          }
        }
      }
    )
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
              activeCourseIdForAdd = newCourse.id
              newlyAddedCourse = newCourse
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
  accessCodes: List<AccessCodeEntity> = emptyList(),
  onResetSeedData: () -> Unit,
  onSyncCloud: () -> Unit = {},
  isSyncing: Boolean = false
) {
  var showResetConfirm by remember { mutableStateOf(false) }
  val studentUsers = users.filter { it.role == "STUDENT" }
  val totalQuestionsAnswered = users.sumOf { it.questionsAnsweredCount }
  val activePasses = accessCodes.count { !it.isRedeemed && System.currentTimeMillis() <= it.expiresAt }
  val redeemedPasses = accessCodes.count { it.isRedeemed }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Real-Time System Metrics",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("Courses", "${courses.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        AdminStatCard("Students", "${studentUsers.size}", SuccessGreen, Modifier.weight(1f))
        AdminStatCard("Notices", "${announcements.size}", AcademicGold, Modifier.weight(1f))
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("Live Qs Practiced", "$totalQuestionsAnswered", AcademicCobalt, Modifier.weight(1f))
        AdminStatCard("Active 1-Hr Passes", "$activePasses", AcademicGold, Modifier.weight(1f))
        AdminStatCard("Redeemed Passes", "$redeemedPasses", SuccessGreen, Modifier.weight(1f))
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              "Central Cloud Database (Multi-Device)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Changes made here (new courses, questions, notes, and past exams) persist directly to Firestore cloud database and sync in real time across all user devices (PC, Android phones, and tablets).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = onSyncCloud,
            enabled = !isSyncing,
            modifier = Modifier.testTag("admin_sync_cloud_button")
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Synchronizing with Cloud...")
            } else {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Synchronize with Central Cloud")
            }
          }
        }
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
          Text("Academic Factory Data Management", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "The TTS Academy factory curriculum contains 12 freshman university courses, pre-seeded past exams, and question banks. Resetting re-populates the cloud and local databases with fresh baseline data.",
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
  questions: List<QuestionEntity> = emptyList(),
  notes: List<NoteEntity> = emptyList(),
  topics: List<TopicEntity> = emptyList(),
  onAddNew: () -> Unit,
  onSaveCourse: (CourseEntity) -> Unit = {},
  onDelete: (String) -> Unit,
  onSaveTopic: (TopicEntity) -> Unit = {},
  onDeleteTopic: (String) -> Unit = {},
  onAddQuestionForCourse: (String) -> Unit,
  onAddNoteForCourse: (String) -> Unit,
  onManageCourse: (String) -> Unit
) {
  var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }
  var courseToEdit by remember { mutableStateOf<CourseEntity?>(null) }
  var courseForSections by remember { mutableStateOf<CourseEntity?>(null) }

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
        Column {
          Text("Curriculum Courses (${courses.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("Create, edit, and organize courses and course sections.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
          onClick = onAddNew,
          colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Course")
        }
      }
    }

    items(courses) { course ->
      val courseTopics = topics.filter { it.courseId == course.id }
      val courseQCount = questions.count { it.courseId == course.id }
      val courseNCount = notes.count { it.courseId == course.id }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = AcademicBluePrimary.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = course.code,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AcademicBluePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = course.category,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = course.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = course.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
              )
            }

            Row {
              IconButton(onClick = { courseToEdit = course }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Course", tint = MaterialTheme.colorScheme.primary)
              }
              IconButton(onClick = { courseToDelete = course }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Course", tint = MaterialTheme.colorScheme.error)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          Spacer(modifier = Modifier.height(10.dp))

          // Live counts banner
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${courseTopics.size} Sections • $courseQCount Questions • $courseNCount Revision Notes",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = AcademicCobalt
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Action buttons right inside the course card!
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { courseForSections = course },
              modifier = Modifier.weight(1.1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sections (${courseTopics.size})", style = MaterialTheme.typography.labelSmall)
            }

            Button(
              onClick = { onAddQuestionForCourse(course.id) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Question", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { onAddNoteForCourse(course.id) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Note", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { onManageCourse(course.id) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Resources", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }

  // Edit Course Dialog
  if (courseToEdit != null) {
    EditCourseDialog(
      course = courseToEdit!!,
      onDismiss = { courseToEdit = null },
      onSave = { updatedCourse ->
        onSaveCourse(updatedCourse)
        courseToEdit = null
      }
    )
  }

  // Manage Sections Dialog
  if (courseForSections != null) {
    val targetCourse = courseForSections!!
    ManageSectionsDialog(
      course = targetCourse,
      sections = topics.filter { it.courseId == targetCourse.id },
      questions = questions.filter { it.courseId == targetCourse.id },
      notes = notes.filter { it.courseId == targetCourse.id },
      onDismiss = { courseForSections = null },
      onSaveSection = onSaveTopic,
      onDeleteSection = onDeleteTopic
    )
  }

  // Delete Course Confirmation Dialog
  if (courseToDelete != null) {
    val targetCourse = courseToDelete!!
    AlertDialog(
      onDismissRequest = { courseToDelete = null },
      title = { Text("Delete ${targetCourse.code}?") },
      text = { Text("Are you sure you want to remove '${targetCourse.title}' from the curriculum? All associated sections and questions will also be deleted.") },
      confirmButton = {
        Button(
          onClick = {
            onDelete(targetCourse.id)
            courseToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { courseToDelete = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun AdminAddQuestionTab(
  courses: List<CourseEntity>,
  topics: List<TopicEntity> = emptyList(),
  initialCourseId: String = "",
  onCourseSelected: (String) -> Unit = {},
  onSaveQuestion: (QuestionEntity) -> Unit,
  onSaveTopic: (TopicEntity) -> Unit = {}
) {
  var selectedCourseId by remember(initialCourseId, courses) {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }
  val courseTopics = topics.filter { it.courseId == selectedCourseId }
  var selectedTopicId by remember(selectedCourseId, courseTopics) {
    mutableStateOf(courseTopics.firstOrNull()?.id ?: "")
  }
  var showAddSectionDialog by remember { mutableStateOf(false) }
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
      Text("Add questions to newly added or existing freshman courses with choice explanations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = {
          selectedCourseId = it
          onCourseSelected(it)
        }
      )
    }

    // Section Selector
    item {
      AdminSectionSelector(
        sections = courseTopics,
        selectedSectionId = selectedTopicId,
        onSectionSelected = { selectedTopicId = it },
        onAddNewSection = { showAddSectionDialog = true }
      )
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
              topicId = selectedTopicId.ifBlank { "T_GEN" },
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
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save & Publish Question to Selected Course", fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showAddSectionDialog && selectedCourseId.isNotBlank()) {
    CreateOrEditSectionDialog(
      courseId = selectedCourseId,
      existingCount = courseTopics.size,
      onDismiss = { showAddSectionDialog = false },
      onSave = { newSection ->
        onSaveTopic(newSection)
        selectedTopicId = newSection.id
        showAddSectionDialog = false
      }
    )
  }
}

@Composable
private fun AdminAddNoteTab(
  courses: List<CourseEntity>,
  topics: List<TopicEntity> = emptyList(),
  initialCourseId: String = "",
  onCourseSelected: (String) -> Unit = {},
  onSaveNote: (NoteEntity) -> Unit,
  onSaveTopic: (TopicEntity) -> Unit = {}
) {
  var selectedCourseId by remember(initialCourseId, courses) {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }
  val courseTopics = topics.filter { it.courseId == selectedCourseId }
  var selectedTopicId by remember(selectedCourseId, courseTopics) {
    mutableStateOf(courseTopics.firstOrNull()?.id ?: "")
  }
  var showAddSectionDialog by remember { mutableStateOf(false) }
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
      Text("Add comprehensive summaries and formulas to any freshman course.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = {
          selectedCourseId = it
          onCourseSelected(it)
        }
      )
    }

    // Section Selector
    item {
      AdminSectionSelector(
        sections = courseTopics,
        selectedSectionId = selectedTopicId,
        onSectionSelected = { selectedTopicId = it },
        onAddNewSection = { showAddSectionDialog = true }
      )
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
              topicId = selectedTopicId.ifBlank { "T_GEN" },
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
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Revision Note to Selected Course", fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showAddSectionDialog && selectedCourseId.isNotBlank()) {
    CreateOrEditSectionDialog(
      courseId = selectedCourseId,
      existingCount = courseTopics.size,
      onDismiss = { showAddSectionDialog = false },
      onSave = { newSection ->
        onSaveTopic(newSection)
        selectedTopicId = newSection.id
        showAddSectionDialog = false
      }
    )
  }
}


@Composable
private fun AdminAnnouncementsTab(
  announcements: List<AnnouncementEntity>,
  onNewClick: () -> Unit,
  onDelete: (String) -> Unit = {}
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
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = ann.title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ann.message, style = MaterialTheme.typography.bodySmall)
          }
          IconButton(onClick = { onDelete(ann.id) }) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Notice",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminStudentsTab(
  users: List<UserEntity>,
  onAddAdminClick: () -> Unit,
  onUpdateRole: (String, String) -> Unit,
  onPromoteEmail: (String) -> Unit
) {
  var targetEmailInput by remember { mutableStateOf("") }

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
          Text("Admin & Role Management (${users.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("Make any user an administrator anytime, or create new admin profiles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAddAdminClick) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Admin")
        }
      }
    }

    // Direct "Make Anyone Admin Anytime" Card
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Grant Administrator Access to Any Email",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Type any registered student email address or new staff email to immediately grant full admin rights.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = targetEmailInput,
              onValueChange = { targetEmailInput = it },
              placeholder = { Text("e.g. alex.morgan@university.edu") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (targetEmailInput.isNotBlank()) {
                  onPromoteEmail(targetEmailInput.trim())
                  targetEmailInput = ""
                }
              },
              enabled = targetEmailInput.isNotBlank(),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Make Admin")
            }
          }
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

            if (user.email.equals("spikenarrd@gmail.com", ignoreCase = true) || user.email.equals("abebewottee@gmail.com", ignoreCase = true)) {
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

@Composable
private fun AdminAccessCodesTab(
  accessCodes: List<AccessCodeEntity>,
  onGenerateCode: (note: String, format: String, customCode: String?, onResult: (String) -> Unit) -> Unit,
  onDeleteCode: (String) -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  var noteInput by remember { mutableStateOf("Freshman Semester Pass") }
  var selectedFormat by remember { mutableStateOf("ALPHANUMERIC") }
  var customCodeInput by remember { mutableStateOf("") }
  var lastGeneratedCode by remember { mutableStateOf<String?>(null) }
  var isCopied by remember { mutableStateOf(false) }

  val activeCount = accessCodes.count { !it.isRedeemed }
  val redeemedCount = accessCodes.count { it.isRedeemed }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Summary Banner
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text("Active Keys", style = MaterialTheme.typography.labelMedium)
            Text("$activeCount", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Ready for students", style = MaterialTheme.typography.bodySmall)
          }
        }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text("Redeemed", style = MaterialTheme.typography.labelMedium)
            Text("$redeemedCount", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Enrolled payers", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }

    // Code Generator Widget Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate New 6-Digit Access Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
          Spacer(modifier = Modifier.height(8.dp))

          Text("Create single-use 6-character access pass codes for students paying for access.", style = MaterialTheme.typography.bodySmall)

          Spacer(modifier = Modifier.height(12.dp))

          Text("Key Format:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              "ALPHANUMERIC" to "Letters & Digits",
              "NUMERIC" to "6 Numbers",
              "SPECIAL" to "Special Chars"
            ).forEach { (formatKey, label) ->
              val isSelected = selectedFormat == formatKey
              Surface(
                onClick = { selectedFormat = formatKey },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = 8.dp),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("Note / Batch Label") },
            placeholder = { Text("e.g. Semester 1 Pass / Engineering Batch") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customCodeInput,
            onValueChange = { if (it.length <= 6) customCodeInput = it.uppercase() },
            label = { Text("Custom 6-Char Code (Optional)") },
            placeholder = { Text("Leave blank to auto-generate") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              onGenerateCode(noteInput, selectedFormat, customCodeInput.ifBlank { null }) { gen ->
                lastGeneratedCode = gen
                isCopied = false
              }
              customCodeInput = ""
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Access Code", fontWeight = FontWeight.Bold)
          }

          if (lastGeneratedCode != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("New Code Ready:", style = MaterialTheme.typography.labelSmall)
                  Text(
                    text = lastGeneratedCode ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                Button(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(lastGeneratedCode ?: ""))
                    isCopied = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (isCopied) "Copied!" else "Copy")
                }
              }
            }
          }
        }
      }
    }

    // List of All Access Codes
    item {
      Text(
        text = "Access Codes Archive (${accessCodes.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    if (accessCodes.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No access codes generated yet. Tap 'Generate Access Code' above to create single-use passcodes for paying students.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      items(accessCodes) { item ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = item.code,
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                  ),
                  color = if (item.isRedeemed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                  color = if (item.isRedeemed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = if (item.isRedeemed) "REDEEMED" else "ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (item.isRedeemed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              if (item.isRedeemed && !item.redeemedByEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Redeemed by: ${item.redeemedByEmail}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(item.code)) }
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(
                onClick = { onDeleteCode(item.code) }
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Code", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }
  }
}

