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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.firestore.CloudAdminDiagnostic
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.example.ui.components.AdminClaimGuard
import com.example.ui.components.AdminClaimGuardMode
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
  cloudDiagnostic: CloudAdminDiagnostic? = null,
  lastAdminError: String? = null,
  isTestingCloudWrite: Boolean = false,
  onRefreshPermissions: () -> Unit = {},
  onRunWriteTest: () -> Unit = {},
  onClearAdminError: () -> Unit = {},
  onBackClick: () -> Unit,
  onSaveCourse: (CourseEntity, (Boolean, String?) -> Unit) -> Unit,
  onDeleteCourse: (String) -> Unit,
  onSaveQuestion: (QuestionEntity, (Boolean, String?) -> Unit) -> Unit,
  onDeleteQuestion: (String) -> Unit = {},
  onSaveNote: (NoteEntity, (Boolean, String?) -> Unit) -> Unit,
  onDeleteNote: (String) -> Unit = {},
  onSavePastExam: (PastExamEntity, (Boolean, String?) -> Unit) -> Unit,
  onDeletePastExam: (String) -> Unit = {},
  onSaveTopic: (TopicEntity, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
  onDeleteTopic: (String) -> Unit = {},
  onPublishAnnouncement: (String, String, String, Boolean, (Boolean, String?) -> Unit) -> Unit,
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
  var activeCourseIdForAdd by remember {
    mutableStateOf(courses.firstOrNull()?.id ?: "")
  }

  // Preserve user's course preference and only initialize or fallback if blank or non-existent
  LaunchedEffect(courses) {
    if (courses.isNotEmpty() && (activeCourseIdForAdd.isBlank() || courses.none { it.id == activeCourseIdForAdd })) {
      activeCourseIdForAdd = courses.first().id
    }
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

    AdminClaimGuard(
      mode = AdminClaimGuardMode.WARN_AND_GATE,
      allowFallbackAdminAccess = cloudDiagnostic == null || cloudDiagnostic.hasWriteReadyAuth,
      onClaimRefreshed = onRefreshPermissions,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
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

    val clipboardManager = LocalClipboardManager.current

    if (!lastAdminError.isNullOrBlank()) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_last_error_banner")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Last Cloud Write Error",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(
              onClick = onClearAdminError,
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Text(
            text = lastAdminError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                val report = buildString {
                  appendLine("Last Error: $lastAdminError")
                  if (cloudDiagnostic != null) {
                    appendLine()
                    append(cloudDiagnostic.toCopyableReport())
                  }
                }
                clipboardManager.setText(AnnotatedString(report))
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("admin_copy_error_button")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy error", modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Error", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = onRunWriteTest,
              enabled = !isTestingCloudWrite && !isSyncing,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("admin_error_banner_test_write_button")
            ) {
              Text(if (isTestingCloudWrite) "Testing..." else "Test Cloud Write", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    } else if (cloudDiagnostic != null && !cloudDiagnostic.hasWriteReadyAuth) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_cloud_permission_banner")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Cloud Admin Write Attention Needed (${cloudDiagnostic.signedInEmail ?: "Not signed in"})",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onErrorContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = cloudDiagnostic.diagnosticMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis
            )
          }
          OutlinedButton(
            onClick = onRefreshPermissions,
            enabled = !isSyncing && !isTestingCloudWrite,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("admin_banner_refresh_permissions_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh permissions", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Refresh", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    when (selectedTab) {
      0 -> AdminOverviewTab(
        courses = courses,
        users = allUsers,
        announcements = announcements,
        accessCodes = accessCodes,
        cloudDiagnostic = cloudDiagnostic,
        isTestingCloudWrite = isTestingCloudWrite,
        onRefreshPermissions = onRefreshPermissions,
        onRunWriteTest = onRunWriteTest,
        onResetSeedData = onResetSeedData,
        onSyncCloud = onSyncCloud,
        isSyncing = isSyncing
      )
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
        notes = notes,
        initialCourseId = activeCourseIdForAdd,
        onCourseSelected = { activeCourseIdForAdd = it },
        onSaveNote = onSaveNote,
        onSaveTopic = onSaveTopic
      )
      4 -> AdminPastExamTab(
        courses = courses,
        questions = questions,
        initialCourseId = activeCourseIdForAdd,
        onCourseSelected = { activeCourseIdForAdd = it },
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
        onCourseSelected = { activeCourseIdForAdd = it },
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
      8 -> AdminAccessCodesTab(accessCodes, lastAdminError, onGenerateCode, onDeleteCode)
    }
      }
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
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { if (!isSaving) showNewCourseDialog = false },
      title = { Text("Create New Course") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = code,
            onValueChange = { code = it; validationError = null },
            label = { Text("Course Code (e.g. MATH 103)") },
            singleLine = true,
            enabled = !isSaving
          )
          OutlinedTextField(
            value = title,
            onValueChange = { title = it; validationError = null },
            label = { Text("Course Title") },
            singleLine = true,
            enabled = !isSaving
          )
          OutlinedTextField(
            value = category,
            onValueChange = { category = it; validationError = null },
            label = { Text("Category (STEM, Humanities, etc.)") },
            singleLine = true,
            enabled = !isSaving
          )
          OutlinedTextField(
            value = desc,
            onValueChange = { desc = it; validationError = null },
            label = { Text("Description") },
            maxLines = 3,
            enabled = !isSaving
          )
          if (!validationError.isNullOrBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          enabled = !isSaving,
          onClick = {
            if (code.isBlank() || title.isBlank()) {
              validationError = "Please enter both course code and course title."
              return@Button
            }
            isSaving = true
            validationError = null
            val newCourse = CourseEntity(
              id = "C_" + UUID.randomUUID().toString().take(6).uppercase(),
              code = code.trim(),
              title = title.trim(),
              description = desc.trim(),
              category = category.trim().ifBlank { "General" },
              iconName = "school",
              totalQuestions = 0,
              totalNotes = 0,
              isPublished = true,
              orderIndex = courses.size + 1
            )
            onSaveCourse(newCourse) { success, errMsg ->
              isSaving = false
              if (success) {
                activeCourseIdForAdd = newCourse.id
                newlyAddedCourse = newCourse
                showNewCourseDialog = false
              } else {
                validationError = errMsg ?: "Failed to save course to cloud. Check connection and admin permissions."
              }
            }
          }
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Saving...")
          } else {
            Text("Save Course")
          }
        }
      },
      dismissButton = {
        TextButton(enabled = !isSaving, onClick = { showNewCourseDialog = false }) {
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
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { if (!isSaving) showNewAnnouncementDialog = false },
      title = { Text("Publish Announcement") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(value = title, onValueChange = { title = it; validationError = null }, label = { Text("Title") }, singleLine = true, enabled = !isSaving)
          OutlinedTextField(value = message, onValueChange = { message = it; validationError = null }, label = { Text("Message Body") }, maxLines = 4, enabled = !isSaving)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = !isSaving) { isUrgent = !isUrgent }
          ) {
            Text("Mark as Urgent / Alert Notice: ${if (isUrgent) "YES" else "NO"}", fontWeight = FontWeight.Bold)
          }
          if (!validationError.isNullOrBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          enabled = !isSaving,
          onClick = {
            if (title.isBlank() || message.isBlank()) {
              validationError = "Please enter both a title and a message body."
            } else {
              validationError = null
              isSaving = true
              onPublishAnnouncement(title, message, "EXAM", isUrgent) { success, errMsg ->
                isSaving = false
                if (success) {
                  showNewAnnouncementDialog = false
                } else {
                  validationError = errMsg ?: "Failed to broadcast announcement."
                }
              }
            }
          }
        ) {
          Text(if (isSaving) "Publishing..." else "Broadcast")
        }
      },
      dismissButton = {
        TextButton(enabled = !isSaving, onClick = { showNewAnnouncementDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // New Administrator Invite Dialog
  if (showNewAdminDialog) {
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminDept by remember { mutableStateOf("Academic Directorate") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showNewAdminDialog = false },
      title = { Text("Invite / Add Administrator") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Users sign in with Google Sign-In. When this Google email signs in, they will automatically receive Administrator privileges.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(value = adminName, onValueChange = { adminName = it; validationError = null }, label = { Text("Full Name") }, singleLine = true)
          OutlinedTextField(value = adminEmail, onValueChange = { adminEmail = it; validationError = null }, label = { Text("Google Email Address") }, singleLine = true)
          OutlinedTextField(value = adminDept, onValueChange = { adminDept = it; validationError = null }, label = { Text("Department / Directorate") }, singleLine = true)
          if (!validationError.isNullOrBlank()) {
            Text(
              text = validationError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val trimmedEmail = adminEmail.trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
              validationError = "Please enter a valid Google email address."
            } else {
              onCreateAdmin(adminName.trim(), trimmedEmail, "", adminDept.trim())
              showNewAdminDialog = false
            }
          }
        ) {
          Text("Save Admin Invite")
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
  cloudDiagnostic: CloudAdminDiagnostic? = null,
  isTestingCloudWrite: Boolean = false,
  onRefreshPermissions: () -> Unit = {},
  onRunWriteTest: () -> Unit = {},
  onResetSeedData: () -> Unit,
  onSyncCloud: () -> Unit = {},
  isSyncing: Boolean = false
) {
  val clipboardManager = LocalClipboardManager.current
  var copiedReport by remember { mutableStateOf(false) }
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
        AdminStatCard("Students", "${studentUsers.size}", AcademicBluePrimary, Modifier.weight(1f))
        AdminStatCard("Notices", "${announcements.size}", AcademicGold, Modifier.weight(1f))
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("Live Qs Practiced", "$totalQuestionsAnswered", AcademicCobalt, Modifier.weight(1f))
        AdminStatCard("Active Passes", "$activePasses", AcademicGold, Modifier.weight(1f))
        AdminStatCard("Redeemed Passes", "$redeemedPasses", AcademicBluePrimary, Modifier.weight(1f))
      }
    }

    item {
      val ready = cloudDiagnostic?.hasWriteReadyAuth == true
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (ready) {
            SuccessGreen.copy(alpha = 0.08f)
          } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
          }
        ),
        border = BorderStroke(
          1.dp,
          if (ready) SuccessGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Cloud Admin Write Permissions",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
              color = if (ready) SuccessGreen else MaterialTheme.colorScheme.error,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = when {
                  cloudDiagnostic?.writeTestAttempted == true && cloudDiagnostic.writeTestPassed -> "WRITE VERIFIED"
                  ready -> "READY"
                  else -> "CHECK NEEDED"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          if (cloudDiagnostic != null) {
            Text(
              text = "Account: ${cloudDiagnostic.signedInEmail ?: "Not signed in"} (${if (cloudDiagnostic.isEmailVerified) "Verified Google Email" else "Unverified"})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Cloud Role: ${cloudDiagnostic.userDocRole} • UID: ${cloudDiagnostic.signedInUid ?: "none"}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "DB: ${cloudDiagnostic.databaseId}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = cloudDiagnostic.diagnosticMessage,
              style = MaterialTheme.typography.bodySmall,
              color = if (ready) SuccessGreen else MaterialTheme.colorScheme.error
            )
          } else {
            Text(
              text = "Checking live Firestore admin token and role status...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onRunWriteTest,
              enabled = !isTestingCloudWrite && !isSyncing,
              modifier = Modifier
                .weight(1f)
                .testTag("admin_test_write_button")
            ) {
              if (isTestingCloudWrite) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Testing...", style = MaterialTheme.typography.labelMedium)
              } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Cloud Write", style = MaterialTheme.typography.labelMedium)
              }
            }

            OutlinedButton(
              onClick = onRefreshPermissions,
              enabled = !isSyncing && !isTestingCloudWrite,
              modifier = Modifier.testTag("admin_refresh_permissions_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Refresh", style = MaterialTheme.typography.labelMedium)
            }
          }

          if (cloudDiagnostic != null) {
            OutlinedButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(cloudDiagnostic.toCopyableReport()))
                copiedReport = true
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_copy_diagnostic_button")
            ) {
              Icon(
                if (copiedReport) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy diagnostic report",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (copiedReport) "Diagnostic Report Copied" else "Copy Full Cloud Diagnostic Report")
            }
          }
        }
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
            text = "Changes made here (new courses, questions, notes, announcements, and past exams) persist directly to Firestore cloud database and sync in real time across all devices.",
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
          Text("Curriculum Cloud Integrity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Safely verifies your local device cache against the live central cloud curriculum without overwriting any custom courses or notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedButton(
            onClick = onResetSeedData,
            enabled = !isSyncing,
            modifier = Modifier.testTag("admin_verify_curriculum_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Verify & Refresh Cloud Curriculum")
          }
        }
      }
    }
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
  onSaveCourse: (CourseEntity, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
  onDelete: (String) -> Unit,
  onSaveTopic: (TopicEntity, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
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
      onSave = { updatedCourse, done ->
        onSaveCourse(updatedCourse) { success, err ->
          done(success, err)
          if (success) courseToEdit = null
        }
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
  onSaveQuestion: (QuestionEntity, (Boolean, String?) -> Unit) -> Unit,
  onSaveTopic: (TopicEntity, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) }
) {
  var selectedCourseId by remember {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }

  LaunchedEffect(initialCourseId) {
    if (initialCourseId.isNotBlank() && selectedCourseId != initialCourseId) {
      selectedCourseId = initialCourseId
    }
  }

  LaunchedEffect(courses) {
    if (courses.isNotEmpty() && (selectedCourseId.isBlank() || courses.none { it.id == selectedCourseId })) {
      selectedCourseId = courses.first().id
      onCourseSelected(selectedCourseId)
    }
  }

  val courseTopics = topics.filter { it.courseId == selectedCourseId }
  var selectedTopicId by remember {
    mutableStateOf("")
  }

  LaunchedEffect(selectedCourseId, courseTopics) {
    if (courseTopics.isNotEmpty() && (selectedTopicId.isBlank() || courseTopics.none { it.id == selectedTopicId })) {
      selectedTopicId = courseTopics.first().id
    }
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
  var isSaving by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

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
          validationError = null
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
        onValueChange = { questionText = it; validationError = null },
        label = { Text("Question Text") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
      )
    }

    item { OutlinedTextField(value = optA, onValueChange = { optA = it; validationError = null }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optB, onValueChange = { optB = it; validationError = null }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth()) }
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

    if (!validationError.isNullOrBlank()) {
      item {
        Text(
          text = validationError!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall
        )
      }
    }

    item {
      Button(
        enabled = !isSaving,
        onClick = {
          val resolvedCourseId = selectedCourseId.ifBlank { courses.firstOrNull()?.id ?: "" }
          when {
            resolvedCourseId.isBlank() -> validationError = "Please create or select a course first."
            questionText.isBlank() -> validationError = "Please enter the question text."
            optA.isBlank() || optB.isBlank() -> validationError = "Please provide at least Option A and Option B."
            else -> {
              validationError = null
              isSaving = true
              val q = QuestionEntity(
                id = "Q_" + UUID.randomUUID().toString().take(6),
                courseId = resolvedCourseId,
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
              onSaveQuestion(q) { success, errMsg ->
                isSaving = false
                if (success) {
                  questionText = ""
                  optA = ""
                  optB = ""
                  optC = ""
                  optD = ""
                  generalExplanation = ""
                  expA = ""
                  expB = ""
                  expC = ""
                  expD = ""
                } else {
                  validationError = errMsg ?: "Failed to save question to cloud. Check admin permissions."
                }
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (isSaving) "Publishing Question..." else "Save & Publish Question to Selected Course", fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showAddSectionDialog && selectedCourseId.isNotBlank()) {
    CreateOrEditSectionDialog(
      courseId = selectedCourseId,
      existingCount = courseTopics.size,
      onDismiss = { showAddSectionDialog = false },
      onSave = { newSection, done ->
        onSaveTopic(newSection) { success, errMsg ->
          done(success, errMsg)
          if (success) {
            selectedTopicId = newSection.id
            showAddSectionDialog = false
          }
        }
      }
    )
  }
}

@Composable
private fun AdminAddNoteTab(
  courses: List<CourseEntity>,
  topics: List<TopicEntity> = emptyList(),
  notes: List<NoteEntity> = emptyList(),
  initialCourseId: String = "",
  onCourseSelected: (String) -> Unit = {},
  onSaveNote: (NoteEntity, (Boolean, String?) -> Unit) -> Unit,
  onSaveTopic: (TopicEntity, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) }
) {
  var selectedCourseId by remember {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }

  LaunchedEffect(initialCourseId) {
    if (initialCourseId.isNotBlank() && selectedCourseId != initialCourseId) {
      selectedCourseId = initialCourseId
    }
  }

  LaunchedEffect(courses) {
    if (courses.isNotEmpty() && (selectedCourseId.isBlank() || courses.none { it.id == selectedCourseId })) {
      selectedCourseId = courses.first().id
      onCourseSelected(selectedCourseId)
    }
  }

  val courseTopics = topics.filter { it.courseId == selectedCourseId }
  val courseNotes = notes.filter { it.courseId == selectedCourseId }.sortedBy { it.chapterNumber }
  val suggestedChapter = (courseNotes.maxOfOrNull { it.chapterNumber } ?: 0) + 1

  var selectedTopicId by remember {
    mutableStateOf("")
  }

  LaunchedEffect(selectedCourseId, courseTopics) {
    if (courseTopics.isNotEmpty() && (selectedTopicId.isBlank() || courseTopics.none { it.id == selectedTopicId })) {
      selectedTopicId = courseTopics.first().id
    }
  }

  var chapterNumber by remember { mutableIntStateOf(suggestedChapter) }

  // Update chapter number automatically when switching courses
  LaunchedEffect(selectedCourseId, courseNotes.size) {
    chapterNumber = suggestedChapter
  }

  var showAddSectionDialog by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var summary by remember { mutableStateOf("") }
  var formulas by remember { mutableStateOf("") }
  var markdownContent by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text("Create Revision Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      Text("Add comprehensive summaries and formulas to any freshman course, sorted from Chapter 1 to the last chapter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = {
          selectedCourseId = it
          validationError = null
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

    // Chapter & Sorting Order Picker Card
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "CHAPTER & PRESENTATION ORDER",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Controls exact sorting for students from Chapter 1 up to the last chapter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Stepper
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              OutlinedButton(
                onClick = { if (chapterNumber > 1) chapterNumber-- },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text("-", fontWeight = FontWeight.Bold)
              }
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
              ) {
                Text(
                  text = "Chapter $chapterNumber",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
              OutlinedButton(
                onClick = { chapterNumber++ },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text("+", fontWeight = FontWeight.Bold)
              }
            }
          }

          // Quick Chapter Chips (1 to 12)
          Text(
            text = "Select Chapter Number:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items((1..12).toList()) { ch ->
              FilterChip(
                selected = chapterNumber == ch,
                onClick = { chapterNumber = ch },
                label = { Text("Ch $ch") }
              )
            }
          }

          if (courseNotes.isNotEmpty()) {
            Text(
              text = "Already created in this course: " + courseNotes.joinToString { "Ch ${it.chapterNumber}: ${it.title.take(16)}" },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    item { OutlinedTextField(value = title, onValueChange = { title = it; validationError = null }, label = { Text("Note Title (e.g. Limits & Continuity)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Concept Summary") }, modifier = Modifier.fillMaxWidth(), maxLines = 2) }
    item { OutlinedTextField(value = formulas, onValueChange = { formulas = it }, label = { Text("Key Formulas") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = markdownContent, onValueChange = { markdownContent = it }, label = { Text("Detailed Revision Text / Markdown") }, modifier = Modifier.fillMaxWidth(), minLines = 5) }

    if (!validationError.isNullOrBlank()) {
      item {
        Text(
          text = validationError!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall
        )
      }
    }

    item {
      Button(
        enabled = !isSaving,
        onClick = {
          val resolvedCourseId = selectedCourseId.ifBlank { courses.firstOrNull()?.id ?: "" }
          when {
            resolvedCourseId.isBlank() -> validationError = "Please create or select a course first."
            title.isBlank() -> validationError = "Please enter a note title."
            else -> {
              validationError = null
              isSaving = true
              val note = NoteEntity(
                id = "NOTE_" + UUID.randomUUID().toString().take(6),
                courseId = resolvedCourseId,
                topicId = selectedTopicId.ifBlank { "T_GEN" },
                title = title.trim(),
                chapterNumber = chapterNumber,
                summary = summary.trim(),
                contentMarkdown = markdownContent.trim().ifBlank { summary.trim() },
                keyFormulas = formulas.trim(),
                readTimeMinutes = 5,
                isPublished = true
              )
              onSaveNote(note) { success, errMsg ->
                isSaving = false
                if (success) {
                  title = ""
                  summary = ""
                  formulas = ""
                  markdownContent = ""
                } else {
                  validationError = errMsg ?: "Failed to save note to cloud. Check admin permissions."
                }
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isSaving) "Publishing Note..." else "Save Chapter $chapterNumber Revision Note",
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  if (showAddSectionDialog && selectedCourseId.isNotBlank()) {
    CreateOrEditSectionDialog(
      courseId = selectedCourseId,
      existingCount = courseTopics.size,
      onDismiss = { showAddSectionDialog = false },
      onSave = { newSection, done ->
        onSaveTopic(newSection) { success, errMsg ->
          done(success, errMsg)
          if (success) {
            selectedTopicId = newSection.id
            showAddSectionDialog = false
          }
        }
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
  var validationError by remember { mutableStateOf<String?>(null) }

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
          Text("Make any user an administrator anytime, or create new admin invites.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            text = "Type any registered student email address or Google staff email to grant full admin rights.",
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
              onValueChange = { targetEmailInput = it; validationError = null },
              placeholder = { Text("e.g. alex.morgan@gmail.com") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val trimmed = targetEmailInput.trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                  validationError = "Please enter a valid email address."
                } else {
                  validationError = null
                  onPromoteEmail(trimmed)
                  targetEmailInput = ""
                }
              },
              enabled = targetEmailInput.isNotBlank(),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Make Admin")
            }
          }
          if (!validationError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = validationError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
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
  lastAdminError: String?,
  onGenerateCode: (note: String, format: String, customCode: String?, onResult: (String) -> Unit) -> Unit,
  onDeleteCode: (String) -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  var noteInput by remember { mutableStateOf("Freshman Semester Pass") }
  var selectedFormat by remember { mutableStateOf("ALPHANUMERIC") }
  var customCodeInput by remember { mutableStateOf("") }
  var lastGeneratedCode by remember { mutableStateOf<String?>(null) }
  var isCopied by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

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
              "SPECIAL" to "Letters & (- _)"
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
            onValueChange = { noteInput = it; validationError = null },
            label = { Text("Note / Batch Label") },
            placeholder = { Text("e.g. Semester 1 Pass / Engineering Batch") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customCodeInput,
            onValueChange = { raw ->
              val sanitized = raw.uppercase().filter { ch ->
                ch in 'A'..'Z' || ch in '0'..'9' || ch == '_' || ch == '-'
              }.take(6)
              customCodeInput = sanitized
              validationError = null
            },
            label = { Text("Custom 6-Char Code (Optional)") },
            placeholder = { Text("Letters, digits, _ or - (blank = auto)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          if (!validationError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = validationError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              val trimmedCustom = customCodeInput.trim()
              if (trimmedCustom.isNotBlank() && trimmedCustom.length != 6) {
                validationError = "Custom code must be exactly 6 characters (A-Z, 0-9, _ or -)."
                return@Button
              }
              validationError = null
              onGenerateCode(noteInput.ifBlank { "Freshman Semester Pass" }, selectedFormat, trimmedCustom.ifBlank { null }) { gen ->
                val clean = gen.takeIf { it.isNotBlank() }
                lastGeneratedCode = clean
                isCopied = false
                if (clean != null) {
                  customCodeInput = ""
                } else {
                  validationError = lastAdminError ?: "Unable to generate access code. Check connection and admin access."
                }
              }
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

