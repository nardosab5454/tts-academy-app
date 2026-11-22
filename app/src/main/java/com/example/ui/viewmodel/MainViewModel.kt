package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.BookmarkEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import com.example.data.firestore.CloudAdminDiagnostic
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.FirestoreBookmark
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreUserProfile
import com.example.data.remote.GeminiTutorService
import com.example.data.remote.TutorContextPayload
import com.example.data.remote.TutorMode
import com.example.data.repository.OfflineCacheStats
import com.example.data.repository.TtsRepository
import com.example.data.repository.WeakTopicSummary
import com.example.util.NetworkConnectivityObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class Screen {
  DASHBOARD,
  COURSES,
  COURSE_DETAIL,
  QUIZ,
  QUIZ_RESULT,
  REVIEW_ANSWERS,
  PAST_EXAMS,
  PAST_EXAM_DETAIL,
  NOTES,
  NOTE_DETAIL,
  SEARCH,
  BOOKMARKS,
  PROGRESS,
  ANNOUNCEMENTS,
  PROFILE,
  ADMIN_PORTAL,
  AI_TUTOR,
  AUTH,
  PREMIUM_GATE
}

sealed interface AiChatUiState {
  data object Idle : AiChatUiState
  data object Loading : AiChatUiState
  data class Streaming(val partialText: String) : AiChatUiState
  data class Success(val text: String) : AiChatUiState
  data class Error(val message: String, val failedPrompt: String? = null) : AiChatUiState
}

data class TutorMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val modeLabel: String = TutorMode.HYDROGEN.label,
  val modelName: String = TutorMode.HYDROGEN.modelName,
  val contextBadge: String? = null,
  val isStreaming: Boolean = false,
  val isError: Boolean = false
)

data class ActiveQuizState(
  val title: String = "",
  val courseId: String = "",
  val courseTitle: String = "",
  val topicId: String? = null,
  val topicTitle: String? = null,
  val examId: String? = null,
  val isExamMode: Boolean = false,
  val questions: List<QuestionEntity> = emptyList(),
  val currentIndex: Int = 0,
  val selectedAnswers: Map<String, String> = emptyMap(), // questionId -> "A"|"B"|"C"|"D"
  val submittedInPractice: Set<String> = emptySet(), // questionId submitted in practice mode
  val startTimeMillis: Long = System.currentTimeMillis(),
  val elapsedSeconds: Int = 0,
  val isCompleted: Boolean = false,
  val resultAttempt: QuizAttemptEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val connectivityObserver = NetworkConnectivityObserver(application)
  val isOnline: StateFlow<Boolean> = connectivityObserver.observe()
    .stateIn(viewModelScope, SharingStarted.Eagerly, connectivityObserver.isCurrentlyConnected())

  val firestoreRepository = FirestoreRepository(application)
  private val database = AppDatabase.getDatabase(application, viewModelScope)
  val repository = TtsRepository(database, connectivityObserver, firestoreRepository)

  private val _offlineCacheStats = MutableStateFlow(
    OfflineCacheStats(
      cachedCoursesCount = 12,
      cachedTopicsCount = 36,
      cachedQuestionsCount = 50,
      cachedNotesCount = 12,
      cachedPastExamsCount = 6,
      cachedQuizAttemptsCount = 0,
      cachedBookmarksCount = 0
    )
  )
  val offlineCacheStats: StateFlow<OfflineCacheStats> = _offlineCacheStats.asStateFlow()

  fun refreshOfflineCache() {
    viewModelScope.launch {
      val stats = repository.refreshAndVerifyOfflineCache()
      _offlineCacheStats.value = stats
      // Silent cache synchronization without disturbing student UX
    }
  }

  private val _isAuthenticating = MutableStateFlow(false)
  val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

  private val _cloudAdminDiagnostic = MutableStateFlow<CloudAdminDiagnostic?>(null)
  val cloudAdminDiagnostic: StateFlow<CloudAdminDiagnostic?> = _cloudAdminDiagnostic.asStateFlow()

  fun syncWithFirebaseUser(firebaseUser: FirebaseUser?) {
    if (firebaseUser == null) return
    viewModelScope.launch {
      _isAuthenticating.value = true
      try {
        initJob.join()
        try {
          firebaseUser.getIdToken(true).await()
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {}

        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: "student@ttsacademy.edu"
        val name = firebaseUser.displayName ?: "TTS Student"
        val profileResult = firestoreRepository.getOrCreateUserProfile(uid, email, name)
        profileResult.onSuccess { profile ->
          repository.syncFirebaseUser(
            uid = uid,
            email = email,
            name = profile.name.ifBlank { name },
            role = profile.role,
            department = profile.department,
            isPremium = profile.isPremium,
            studentId = profile.studentId
          )
          loadUserData(uid)
          val report = repository.syncFromCloudNow()
          if (profile.role == "ADMIN") {
            _cloudAdminDiagnostic.value = firestoreRepository.diagnoseAdminAccess()
          } else {
            _cloudAdminDiagnostic.value = null
          }
          if (report.errors.isNotEmpty() && isOnline.value) {
            showMessage("Cloud sync warning: ${report.errors.first()}")
          }
        }.onFailure { err ->
          val fallbackRole = if (repository.isSystemAdminEmail(email)) "ADMIN" else "STUDENT"
          val localUser = repository.syncFirebaseUser(
            uid = uid,
            email = email,
            name = name,
            role = fallbackRole
          )
          loadUserData(localUser.id)
          if (isOnline.value) {
            repository.syncFromCloudNow()
          }
          _cloudAdminDiagnostic.value = firestoreRepository.diagnoseAdminAccess()
          if (isOnline.value) {
            showMessage("Cloud profile sync failed: ${err.message}")
          }
        }
      } finally {
        _isAuthenticating.value = false
      }
    }
  }

  fun refreshAdminSession(showStatus: Boolean = true) {
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        initJob.join()
        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser == null) {
          _cloudAdminDiagnostic.value = firestoreRepository.diagnoseAdminAccess()
          if (showStatus) {
            showMessage("Firebase Google session missing. Please sign out and sign in with Google.")
          }
          return@launch
        }

        try {
          fbUser.getIdToken(true).await()
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {}

        val email = fbUser.email ?: currentUser.value?.email ?: ""
        val name = fbUser.displayName ?: currentUser.value?.name ?: "TTS Admin"
        val profileResult = firestoreRepository.getOrCreateUserProfile(fbUser.uid, email, name)
        profileResult.onSuccess { profile ->
          repository.syncFirebaseUser(
            uid = fbUser.uid,
            email = email,
            name = profile.name.ifBlank { name },
            role = profile.role,
            department = profile.department,
            isPremium = profile.isPremium,
            studentId = profile.studentId
          )
          loadUserData(fbUser.uid)
        }.onFailure { err ->
          if (showStatus) {
            showMessage("Admin cloud verification failed: ${err.message}")
          }
        }

        val report = repository.syncFromCloudNow()
        val diag = firestoreRepository.diagnoseAdminAccess()
        _cloudAdminDiagnostic.value = diag

        if (showStatus) {
          val summary = if (diag.hasWriteReadyAuth) {
            "Cloud Admin verified (${diag.signedInEmail}, role=${diag.userDocRole}). Synced ${report.coursesCount} courses."
          } else {
            diag.diagnosticMessage
          }
          showMessage(summary)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _cloudAdminDiagnostic.value = firestoreRepository.diagnoseAdminAccess()
        if (showStatus) {
          showMessage("Permission check failed: ${e.message}")
        }
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun clearSessionOnFirebaseSignOut() {
    viewModelScope.launch {
      _cloudAdminDiagnostic.value = null
      repository.signOut()
      _currentScreen.value = Screen.DASHBOARD
      screenBackStack.clear()
    }
  }

  // Current active screen and navigation history
  private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val screenBackStack = mutableListOf<Screen>()

  // Active user and Theme
  val currentUser: StateFlow<UserEntity?> = repository.currentUser
  val appTheme: StateFlow<String> = repository.appTheme
  val isDeviceUnlocked: StateFlow<Boolean> = repository.isDeviceUnlocked

  // Data streams (Eagerly shared for instant live updates across the platform)
  val publishedCourses: StateFlow<List<CourseEntity>> = repository.getPublishedCoursesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allCourses: StateFlow<List<CourseEntity>> = repository.getAllCoursesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allTopics: StateFlow<List<TopicEntity>> = repository.getAllTopicsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allPublishedNotes: StateFlow<List<NoteEntity>> = repository.getAllPublishedNotesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allNotes: StateFlow<List<NoteEntity>> = repository.getAllNotesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allPastExams: StateFlow<List<PastExamEntity>> = repository.getAllPastExamsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allAdminPastExams: StateFlow<List<PastExamEntity>> = repository.getAllAdminPastExamsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allQuestions: StateFlow<List<QuestionEntity>> = repository.getAllQuestionsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val announcements: StateFlow<List<AnnouncementEntity>> = repository.getPublishedAnnouncementsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allAdminAnnouncements: StateFlow<List<AnnouncementEntity>> = repository.getAllAnnouncementsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _viewedAnnouncementIds = MutableStateFlow<Set<String>>(emptySet())
  val unreadAnnouncementsCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(
    announcements,
    _viewedAnnouncementIds
  ) { list, viewedIds ->
    list.count { it.id !in viewedIds }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

  fun markAnnouncementsAsViewed() {
    val currentIds = announcements.value.map { it.id }.toSet()
    _viewedAnnouncementIds.value = _viewedAnnouncementIds.value + currentIds
  }

  val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allAccessCodes: StateFlow<List<AccessCodeEntity>> = repository.getAllAccessCodesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // Selected item states
  private val _selectedCourse = MutableStateFlow<CourseEntity?>(null)
  val selectedCourse: StateFlow<CourseEntity?> = _selectedCourse.asStateFlow()

  private val _courseTopics = MutableStateFlow<List<TopicEntity>>(emptyList())
  val courseTopics: StateFlow<List<TopicEntity>> = _courseTopics.asStateFlow()

  private val _courseNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
  val courseNotes: StateFlow<List<NoteEntity>> = _courseNotes.asStateFlow()

  private val _coursePastExams = MutableStateFlow<List<PastExamEntity>>(emptyList())
  val coursePastExams: StateFlow<List<PastExamEntity>> = _coursePastExams.asStateFlow()

  private val _selectedNote = MutableStateFlow<NoteEntity?>(null)
  val selectedNote: StateFlow<NoteEntity?> = _selectedNote.asStateFlow()

  private val _selectedExam = MutableStateFlow<PastExamEntity?>(null)
  val selectedExam: StateFlow<PastExamEntity?> = _selectedExam.asStateFlow()

  // Active quiz session
  private val _activeQuiz = MutableStateFlow(ActiveQuizState())
  val activeQuiz: StateFlow<ActiveQuizState> = _activeQuiz.asStateFlow()

  // User attempts and bookmarks
  private val _userAttempts = MutableStateFlow<List<QuizAttemptEntity>>(emptyList())
  val userAttempts: StateFlow<List<QuizAttemptEntity>> = _userAttempts.asStateFlow()

  private val _userBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
  val userBookmarks: StateFlow<List<BookmarkEntity>> = _userBookmarks.asStateFlow()

  private val _weakTopics = MutableStateFlow<List<WeakTopicSummary>>(emptyList())
  val weakTopics: StateFlow<List<WeakTopicSummary>> = _weakTopics.asStateFlow()

  // Search state
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _searchCourseResults = MutableStateFlow<List<CourseEntity>>(emptyList())
  val searchCourseResults: StateFlow<List<CourseEntity>> = _searchCourseResults.asStateFlow()

  private val _searchNoteResults = MutableStateFlow<List<NoteEntity>>(emptyList())
  val searchNoteResults: StateFlow<List<NoteEntity>> = _searchNoteResults.asStateFlow()

  private val _searchQuestionResults = MutableStateFlow<List<QuestionEntity>>(emptyList())
  val searchQuestionResults: StateFlow<List<QuestionEntity>> = _searchQuestionResults.asStateFlow()

  // Gemini AI Tutor state (Firebase AI Logic real-time streaming & context-aware)
  private val _isAiLoading = MutableStateFlow(false)
  val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

  private val _aiChatUiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
  val aiChatUiState: StateFlow<AiChatUiState> = _aiChatUiState.asStateFlow()

  private val _aiResponse = MutableStateFlow<String?>(null)
  val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

  private val _aiActiveContext = MutableStateFlow<String?>(null)
  val aiActiveContext: StateFlow<String?> = _aiActiveContext.asStateFlow()

  private val _pinnedContextTitle = MutableStateFlow<String?>(null)
  val pinnedContextTitle: StateFlow<String?> = _pinnedContextTitle.asStateFlow()

  private val _pinnedContextSnippet = MutableStateFlow<String?>(null)
  val pinnedContextSnippet: StateFlow<String?> = _pinnedContextSnippet.asStateFlow()

  private val _selectedTutorMode = MutableStateFlow(TutorMode.HYDROGEN)
  val selectedTutorMode: StateFlow<TutorMode> = _selectedTutorMode.asStateFlow()

  private val _chatCourseId = MutableStateFlow<String?>(null)
  val chatCourseId: StateFlow<String?> = _chatCourseId.asStateFlow()

  private val _includeAppContext = MutableStateFlow(true)
  val includeAppContext: StateFlow<Boolean> = _includeAppContext.asStateFlow()

  private val _tutorConversation = MutableStateFlow<List<TutorMessage>>(emptyList())
  val tutorConversation: StateFlow<List<TutorMessage>> = _tutorConversation.asStateFlow()

  private var aiStreamJob: Job? = null

  // Feedback banner / snackbar
  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message.asStateFlow()

  // Offline Download Tracking
  private val _downloadedKeys = MutableStateFlow<Set<String>>(emptySet())
  val downloadedKeys: StateFlow<Set<String>> = _downloadedKeys.asStateFlow()

  private val _isDownloading = MutableStateFlow(false)
  val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  fun syncNow() {
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        initJob.join()
        val report = repository.syncFromCloudNow()
        when {
          !report.isAuthenticated -> {
            showMessage("Please sign in with Google to synchronize with the central cloud database.")
          }
          report.errors.isNotEmpty() -> {
            showMessage("Cloud sync error: ${report.errors.first()}")
          }
          report.coursesCount == 0 && report.notesCount == 0 && report.questionsCount == 0 -> {
            showMessage("Connected to cloud database, but no courses or notes were found in Firestore.")
          }
          else -> {
            showMessage("Synced from cloud: ${report.coursesCount} courses, ${report.notesCount} notes, ${report.questionsCount} questions.")
          }
        }
      } catch (e: Exception) {
        showMessage("Sync failed: ${e.message}")
      } finally {
        _isSyncing.value = false
      }
    }
  }

  private val initJob: Job

  init {
    repository.firestoreRepository = firestoreRepository
    initJob = viewModelScope.launch {
      repository.initialize()
      repository.startRealtimeCloudSync(viewModelScope)
      _offlineCacheStats.value = repository.getOfflineCacheStats(repository.currentUser.value?.id)
    }
    viewModelScope.launch {
      repository.currentUser.collect { user ->
        if (user != null) {
          loadUserData(user.id)
          _offlineCacheStats.value = repository.getOfflineCacheStats(user.id)
        } else {
          _userAttempts.value = emptyList()
          _userBookmarks.value = emptyList()
          _weakTopics.value = emptyList()
          _downloadedKeys.value = emptySet()
          _offlineCacheStats.value = repository.getOfflineCacheStats(null)
        }
      }
    }
    viewModelScope.launch {
      isOnline.collect { online ->
        if (online) {
          initJob.join()
          repository.syncFromCloudNow()
        }
      }
    }
  }

  fun showMessage(msg: String) {
    _message.value = msg
  }

  fun clearMessage() {
    _message.value = null
  }

  fun navigateTo(screen: Screen) {
    if (_currentScreen.value != screen) {
      screenBackStack.add(_currentScreen.value)
      _currentScreen.value = screen
      if (screen in listOf(Screen.ANNOUNCEMENTS, Screen.NOTES, Screen.COURSES, Screen.DASHBOARD, Screen.PAST_EXAMS)) {
        viewModelScope.launch {
          try {
            initJob.join()
            repository.syncFromCloudNow()
          } catch (_: Exception) {}
        }
      }
    }
  }

  fun navigateBack(): Boolean {
    if (screenBackStack.isNotEmpty()) {
      val prev = screenBackStack.removeAt(screenBackStack.lastIndex)
      _currentScreen.value = prev
      return true
    }
    return false
  }

  fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.login(email, password)
      res.onSuccess { user ->
        loadUserData(user.id)
        _currentScreen.value = Screen.DASHBOARD
        showMessage("Welcome back, ${user.name}.")
        onResult(true, "")
      }.onFailure { err ->
        onResult(false, err.message ?: "Authentication failed.")
      }
    }
  }

  fun loginWithAccessCode(code: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.loginWithAccessCode(code)
      res.onSuccess { user ->
        loadUserData(user.id)
        _currentScreen.value = Screen.DASHBOARD
        showMessage("Welcome, ${user.name}! Academic materials unlocked on this device.")
        onResult(true, "Success")
      }.onFailure { err ->
        onResult(false, err.message ?: "Invalid or expired access code.")
      }
    }
  }

  fun register(
    name: String,
    email: String,
    password: String,
    studentId: String,
    department: String,
    onResult: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch {
      val res = repository.register(name, email, password, studentId, department)
      res.onSuccess { user ->
        loadUserData(user.id)
        _currentScreen.value = Screen.DASHBOARD
        showMessage("Account created! Welcome to TTS Academy, ${user.name}.")
        onResult(true, "")
      }.onFailure { err ->
        onResult(false, err.message ?: "Registration failed.")
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      _cloudAdminDiagnostic.value = null
      try {
        androidx.credentials.CredentialManager.create(getApplication())
          .clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
      } catch (_: Exception) {}
      try {
        FirebaseAuth.getInstance().signOut()
      } catch (_: Exception) {}
      repository.signOut()
      _currentScreen.value = Screen.DASHBOARD
      screenBackStack.clear()
    }
  }

  fun updateUserRole(userId: String, newRole: String) {
    if (currentUser.value?.role != "ADMIN" || !hasFirebaseCloudSession()) {
      showMessage("Google Administrator session required to update user roles.")
      return
    }
    viewModelScope.launch {
      val res = repository.setUserRole(userId, newRole)
      res.onSuccess {
        showMessage("User role updated to $newRole.")
      }.onFailure { err ->
        showMessage("Failed to update user role: ${err.message}")
      }
    }
  }

  fun updateStudentDepartmentAndGoal(department: String, dailyGoalMinutes: Int) {
    val uid = currentUser.value?.id ?: return
    viewModelScope.launch {
      val cleanDept = department.trim().ifBlank { "General Freshman Studies" }
      val safeGoal = dailyGoalMinutes.coerceIn(10, 360)
      repository.updateStudentDepartmentAndGoal(uid, cleanDept, safeGoal)
      showMessage("Profile updated: $cleanDept • ${safeGoal}m daily target")
    }
  }

  fun createAdmin(name: String, email: String, password: String, department: String) {
    if (currentUser.value?.role != "ADMIN" || !hasFirebaseCloudSession()) {
      showMessage("Google Administrator session required to add administrators.")
      return
    }
    viewModelScope.launch {
      val res = repository.createAdminAccount(name, email, password, department)
      res.onSuccess { admin ->
        showMessage("Admin privileges granted for ${admin.email}.")
      }.onFailure { err ->
        showMessage("Failed to add administrator: ${err.message}")
      }
    }
  }

  fun promoteUserByEmail(email: String) {
    if (currentUser.value?.role != "ADMIN" || !hasFirebaseCloudSession()) {
      showMessage("Google Administrator session required to promote users.")
      return
    }
    viewModelScope.launch {
      val res = repository.promoteUserByEmail(email)
      res.onSuccess { user ->
        showMessage("Granted full administrator privileges to ${user.name} (${user.email}).")
      }.onFailure { err ->
        showMessage("Could not promote user: ${err.message}")
      }
    }
  }

  // --- Gemini AI Context-Aware Realtime Chatbot ---
  fun switchTutorMode(mode: TutorMode) {
    _selectedTutorMode.value = mode
  }

  fun selectChatCourse(courseId: String?) {
    _chatCourseId.value = courseId
  }

  fun setIncludeAppContext(enabled: Boolean) {
    _includeAppContext.value = enabled
  }

  fun clearPinnedContext() {
    _pinnedContextTitle.value = null
    _pinnedContextSnippet.value = null
  }

  fun openContextAwareChat(
    course: CourseEntity? = null,
    contextTitle: String? = null,
    contextSnippet: String? = null
  ) {
    if (course != null) {
      _chatCourseId.value = course.id
    } else if (_chatCourseId.value == null) {
      _chatCourseId.value = _selectedCourse.value?.id ?: publishedCourses.value.firstOrNull()?.id
    }
    if (!contextSnippet.isNullOrBlank()) {
      _pinnedContextTitle.value = contextTitle ?: "Pinned Study Material"
      _pinnedContextSnippet.value = contextSnippet
      _aiActiveContext.value = contextTitle
    }
    navigateTo(Screen.AI_TUTOR)
  }

  private fun resolveActiveChatCourse(overrideCourseTitle: String?): CourseEntity? {
    val available = publishedCourses.value
    if (!overrideCourseTitle.isNullOrBlank()) {
      available.find { it.title.equals(overrideCourseTitle, ignoreCase = true) }?.let { return it }
    }
    val explicitId = _chatCourseId.value ?: _selectedCourse.value?.id
    if (!explicitId.isNullOrBlank()) {
      available.find { it.id == explicitId }?.let { return it }
    }
    return available.firstOrNull()
  }

  private fun buildStudentChatContext(
    overrideCourseTitle: String?,
    overrideContextSnippet: String?
  ): TutorContextPayload {
    val user = currentUser.value
    val attempts = _userAttempts.value
    val totalAttemptQ = attempts.sumOf { it.totalQuestions }.coerceAtLeast(user?.questionsAnsweredCount ?: 0)
    val correctAttemptQ = attempts.sumOf { it.correctCount }.coerceAtLeast(user?.correctAnswersCount ?: 0)
    val accuracy = if (totalAttemptQ > 0) ((correctAttemptQ * 100) / totalAttemptQ).coerceIn(0, 100) else 0

    val weakSummary = _weakTopics.value.take(5).joinToString("; ") {
      "${it.topicTitle} (${it.accuracyPercent}% accuracy)"
    }

    val course = resolveActiveChatCourse(overrideCourseTitle)
    val topicsForCourse = if (course != null) {
      allTopics.value.filter { it.courseId == course.id }.sortedBy { it.orderIndex }
    } else emptyList()
    val notesForCourse = if (course != null) {
      allPublishedNotes.value.filter { it.courseId == course.id }.sortedBy { it.chapterNumber }
    } else emptyList()
    val examsForCourse = if (course != null) {
      allPastExams.value.filter { it.courseId == course.id }
    } else emptyList()

    val topicsSummary = topicsForCourse.take(10).joinToString(", ") { it.title }
    val notesSummary = notesForCourse.take(6).joinToString("\n") { n ->
      val formulas = if (n.keyFormulas.isNotBlank()) " | Formulas: ${n.keyFormulas.take(140)}" else ""
      "- Ch.${n.chapterNumber} ${n.title}: ${n.summary.take(160)}$formulas"
    }
    val examsSummary = examsForCourse.take(4).joinToString(", ") { "${it.university} ${it.academicYear} ${it.examType}" }

    val effectiveSnippet = overrideContextSnippet?.takeIf { it.isNotBlank() } ?: _pinnedContextSnippet.value
    val effectiveTitle = when {
      !overrideContextSnippet.isNullOrBlank() -> _pinnedContextTitle.value ?: overrideCourseTitle ?: "Focused Study Item"
      else -> _pinnedContextTitle.value
    }

    return TutorContextPayload(
      studentName = user?.name ?: "Student",
      department = user?.department ?: "Freshman University",
      dailyGoalMinutes = user?.studyGoalMinutesPerDay ?: 45,
      streakDays = user?.streakDays ?: 1,
      questionsAnswered = totalAttemptQ,
      overallAccuracyPercent = accuracy,
      weakTopicsSummary = weakSummary,
      activeCourseCode = course?.code,
      activeCourseTitle = course?.title ?: overrideCourseTitle,
      activeCourseCategory = course?.category,
      activeCourseDescription = course?.description,
      courseTopicsSummary = topicsSummary,
      courseNotesSummary = notesSummary,
      coursePastExamsSummary = examsSummary,
      pinnedContextTitle = effectiveTitle,
      pinnedContextSnippet = effectiveSnippet,
      includeAppContext = _includeAppContext.value
    )
  }

  fun askGeminiTutor(
    prompt: String,
    courseTitle: String? = null,
    contextInfo: String? = null,
    contextLabel: String? = null,
    onResult: ((String) -> Unit)? = null
  ) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) return

    if (!contextInfo.isNullOrBlank()) {
      _pinnedContextTitle.value = contextLabel ?: courseTitle ?: "Focused Academic Item"
      _pinnedContextSnippet.value = contextInfo
    }
    val resolvedCourse = resolveActiveChatCourse(courseTitle)
    if (resolvedCourse != null && _chatCourseId.value == null) {
      _chatCourseId.value = resolvedCourse.id
    }

    val activeMode = _selectedTutorMode.value
    val contextPayload = buildStudentChatContext(courseTitle, contextInfo)
    val badgeText = buildString {
      if (_includeAppContext.value) {
        append(resolvedCourse?.code ?: contextPayload.activeCourseTitle ?: "Live Context")
        if (!contextPayload.pinnedContextSnippet.isNullOrBlank()) {
          append(" • Pinned")
        }
      } else {
        append("General Chat")
      }
    }

    aiStreamJob?.cancel()
    aiStreamJob = viewModelScope.launch {
      val priorHistory = _tutorConversation.value
        .filter { !it.isStreaming && !it.isError && it.text.isNotBlank() }
        .map { it.text to it.isUser }

      val userMsg = TutorMessage(
        isUser = true,
        text = cleanPrompt,
        modeLabel = activeMode.label,
        modelName = activeMode.modelName,
        contextBadge = badgeText
      )
      val streamingAiId = UUID.randomUUID().toString()
      val initialAiMsg = TutorMessage(
        id = streamingAiId,
        isUser = false,
        text = "",
        modeLabel = activeMode.label,
        modelName = activeMode.modelName,
        contextBadge = badgeText,
        isStreaming = true
      )

      _tutorConversation.value = _tutorConversation.value + userMsg + initialAiMsg
      _isAiLoading.value = true
      _aiChatUiState.value = AiChatUiState.Loading
      _aiActiveContext.value = badgeText

      if (!isOnline.value) {
        val offlineMsg = "You appear to be offline. Connect to the internet to chat with Gemini AI in real time."
        _tutorConversation.value = _tutorConversation.value.map { msg ->
          if (msg.id == streamingAiId) {
            msg.copy(text = offlineMsg, isStreaming = false, isError = true)
          } else msg
        }
        _isAiLoading.value = false
        _aiChatUiState.value = AiChatUiState.Error(offlineMsg, cleanPrompt)
        return@launch
      }

      try {
        var finalOutput = ""
        GeminiTutorService.streamTutorResponse(
          prompt = cleanPrompt,
          mode = activeMode,
          contextPayload = contextPayload,
          history = priorHistory
        ).collect { accumulated ->
          finalOutput = accumulated
          _aiChatUiState.value = AiChatUiState.Streaming(accumulated)
          _tutorConversation.value = _tutorConversation.value.map { msg ->
            if (msg.id == streamingAiId) {
              msg.copy(text = accumulated, isStreaming = true, isError = false)
            } else msg
          }
        }

        _tutorConversation.value = _tutorConversation.value.map { msg ->
          if (msg.id == streamingAiId) {
            msg.copy(text = finalOutput, isStreaming = false, isError = false)
          } else msg
        }
        _aiResponse.value = finalOutput
        _aiChatUiState.value = AiChatUiState.Success(finalOutput)
        _isAiLoading.value = false
        onResult?.invoke(finalOutput)
      } catch (e: CancellationException) {
        _tutorConversation.value = _tutorConversation.value.filterNot { it.id == streamingAiId && it.text.isBlank() }
        _isAiLoading.value = false
        throw e
      } catch (e: Exception) {
        val errorMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
          ?: "Gemini (${activeMode.modelName}) could not respond right now. Please retry or switch to Hydrogen (General)."
        _tutorConversation.value = _tutorConversation.value.map { msg ->
          if (msg.id == streamingAiId) {
            msg.copy(text = errorMsg, isStreaming = false, isError = true)
          } else msg
        }
        _isAiLoading.value = false
        _aiChatUiState.value = AiChatUiState.Error(errorMsg, cleanPrompt)
      }
    }
  }

  fun retryLastGeminiMessage(fallbackToHydrogen: Boolean = false) {
    val errState = _aiChatUiState.value as? AiChatUiState.Error ?: return
    val prompt = errState.failedPrompt ?: return
    if (fallbackToHydrogen) {
      _selectedTutorMode.value = TutorMode.HYDROGEN
    }
    // Remove the trailing failed error exchange before retrying cleanly
    val list = _tutorConversation.value.toMutableList()
    if (list.lastOrNull()?.isError == true) {
      list.removeAt(list.lastIndex)
    }
    if (list.lastOrNull()?.isUser == true && list.lastOrNull()?.text == prompt) {
      list.removeAt(list.lastIndex)
    }
    _tutorConversation.value = list
    askGeminiTutor(prompt = prompt)
  }

  fun clearAiTutorState() {
    aiStreamJob?.cancel()
    _tutorConversation.value = emptyList()
    _aiResponse.value = null
    _isAiLoading.value = false
    _aiChatUiState.value = AiChatUiState.Idle
    _aiActiveContext.value = null
    _pinnedContextTitle.value = null
    _pinnedContextSnippet.value = null
  }

  // --- Access Code & Premium Management ---
  fun redeemAccessCode(code: String, onResult: (Boolean, String) -> Unit) {
    val user = currentUser.value
    if (user == null) {
      onResult(false, "User session not found. Please log in.")
      return
    }
    viewModelScope.launch {
      val result = repository.redeemAccessCode(user.id, user.email, code)
      result.onSuccess {
        loadUserData(user.id)
        showMessage("Premium Account Activated! Welcome to TTS Academy.")
        _currentScreen.value = Screen.DASHBOARD
        onResult(true, "Activation successful!")
      }.onFailure { err ->
        onResult(false, err.message ?: "Invalid code.")
      }
    }
  }

  fun generateAccessCode(
    note: String,
    format: String = "NUMERIC",
    customCode: String? = null,
    onResult: (String) -> Unit
  ) {
    val admin = currentUser.value
    val adminId = admin?.id ?: "admin_1"
    viewModelScope.launch {
      try {
        val newCode = repository.generateAccessCode(adminId, note, format, customCode)
        showMessage("Generated Access Code: ${newCode.code}")
        onResult(newCode.code)
      } catch (e: Exception) {
        showMessage("Failed to generate access code: ${e.message}")
        onResult("")
      }
    }
  }

  fun generateQuickAdminAccessCode(onResult: (AccessCodeEntity?) -> Unit) {
    val admin = currentUser.value
    val adminId = admin?.id ?: "admin_1"
    viewModelScope.launch {
      try {
        val newCode = repository.generateAccessCode(adminId, "Permanent Academic Pass", "NUMERIC", null)
        showMessage("Generated Access Code: ${newCode.code}")
        onResult(newCode)
      } catch (e: Exception) {
        showMessage("Failed to generate access code: ${e.message}")
        onResult(null)
      }
    }
  }

  fun deleteAccessCode(code: String) {
    if (currentUser.value?.role != "ADMIN" || !hasFirebaseCloudSession()) {
      showMessage("Google Administrator session required to delete access codes.")
      return
    }
    viewModelScope.launch {
      try {
        repository.deleteAccessCode(code)
        showMessage("Removed access code $code.")
      } catch (e: Exception) {
        showMessage("Failed to remove access code: ${e.message}")
      }
    }
  }

  fun downloadExamPdf(
    context: android.content.Context,
    exam: PastExamEntity,
    course: CourseEntity?
  ) {
    viewModelScope.launch {
      try {
        val examQuestions = repository.getQuestionsForExamYear(exam.courseId, exam.academicYear)
        val questions = if (examQuestions.isNotEmpty()) examQuestions else repository.getQuestionsForCourse(exam.courseId)
        val result = com.example.util.ExamPdfGenerator.generateExamPdf(context, exam, course, questions)
        result.onSuccess { file ->
          showMessage("Exam paper downloaded: ${file.name}")
          try {
            val intent = com.example.util.ExamPdfGenerator.createOpenPdfIntent(context, file)
            context.startActivity(intent)
          } catch (_: Exception) {
            showMessage("PDF saved to downloads: ${file.name}")
          }
        }.onFailure { err ->
          showMessage("Failed to generate PDF: ${err.message}")
        }
      } catch (e: Exception) {
        showMessage("Error preparing exam PDF: ${e.message}")
      }
    }
  }

  fun switchUser(userId: String) {
    viewModelScope.launch {
      repository.switchUser(userId)
      loadUserData()
      showMessage("Switched account.")
    }
  }

  fun setAppTheme(theme: String) {
    viewModelScope.launch {
      repository.setAppTheme(theme)
    }
  }

  private var userAttemptsJob: Job? = null
  private var userBookmarksJob: Job? = null
  private var userDownloadsJob: Job? = null
  private var courseTopicsJob: Job? = null
  private var courseNotesJob: Job? = null
  private var coursePastExamsJob: Job? = null

  private fun loadUserData(userId: String? = null) {
    val uid = userId ?: currentUser.value?.id ?: return
    userAttemptsJob?.cancel()
    userAttemptsJob = viewModelScope.launch {
      repository.getAttemptsForUserFlow(uid).collect { attempts ->
        _userAttempts.value = attempts
        _weakTopics.value = repository.getWeakTopics(uid)
      }
    }
    userBookmarksJob?.cancel()
    userBookmarksJob = viewModelScope.launch {
      repository.getBookmarksForUserFlow(uid).collect { bms ->
        _userBookmarks.value = bms
      }
    }
    userDownloadsJob?.cancel()
    userDownloadsJob = viewModelScope.launch {
      repository.getDownloadedKeysFlow(uid).collect { keys ->
        _downloadedKeys.value = keys.toSet()
      }
    }
  }

  fun isDownloaded(resourceType: String, resourceId: String, courseId: String? = null): Boolean {
    if (currentUser.value?.role == "ADMIN") return true
    val uid = currentUser.value?.id ?: return false
    val keys = _downloadedKeys.value
    if ("dl_${uid}_${resourceType}_${resourceId}" in keys) return true
    if (courseId != null && courseId.isNotBlank() && "dl_${uid}_COURSE_${courseId}" in keys) return true
    return false
  }

  fun markDownloaded(resourceType: String, resourceId: String, courseId: String? = null) {
    val uid = currentUser.value?.id ?: return
    viewModelScope.launch {
      repository.markResourceDownloaded(uid, resourceType, resourceId, courseId)
      val newKeys = mutableSetOf("dl_${uid}_${resourceType}_${resourceId}")
      if (courseId != null && courseId.isNotBlank()) {
        newKeys.add("dl_${uid}_COURSE_${courseId}")
      }
      _downloadedKeys.value = _downloadedKeys.value + newKeys
    }
  }

  fun downloadCourse(course: CourseEntity, onComplete: ((Boolean) -> Unit)? = null) {
    val user = currentUser.value
    if (user == null) {
      showMessage("Please sign in to download course materials.")
      onComplete?.invoke(false)
      return
    }
    if (!isOnline.value) {
      showMessage("Active internet connection required to download courses for offline study.")
      onComplete?.invoke(false)
      return
    }

    _isDownloading.value = true
    viewModelScope.launch {
      val res = repository.downloadCourseForOffline(user.id, course.id)
      _isDownloading.value = false
      res.onSuccess { count ->
        showMessage("Course '${course.title}' downloaded successfully for offline study ($count items saved).")
        onComplete?.invoke(true)
      }.onFailure { err ->
        showMessage("Download failed: ${err.message}")
        onComplete?.invoke(false)
      }
    }
  }

  fun downloadNote(note: NoteEntity, course: CourseEntity? = null, onComplete: ((Boolean) -> Unit)? = null) {
    val user = currentUser.value
    if (user == null) {
      showMessage("Please sign in to download notes.")
      onComplete?.invoke(false)
      return
    }
    if (!isOnline.value) {
      showMessage("Active internet connection required to download lecture notes.")
      onComplete?.invoke(false)
      return
    }
    _isDownloading.value = true
    viewModelScope.launch {
      val res = repository.downloadNoteForOffline(user.id, note.id, course?.id)
      _isDownloading.value = false
      res.onSuccess {
        showMessage("Lecture note '${note.title}' downloaded for offline study.")
        onComplete?.invoke(true)
      }.onFailure { err ->
        showMessage("Download failed: ${err.message}")
        onComplete?.invoke(false)
      }
    }
  }

  fun downloadPastExam(exam: PastExamEntity, course: CourseEntity? = null, onComplete: ((Boolean) -> Unit)? = null) {
    val user = currentUser.value
    if (user == null) {
      showMessage("Please sign in to download past exams.")
      onComplete?.invoke(false)
      return
    }
    if (!isOnline.value) {
      showMessage("Active internet connection required to download past examinations.")
      onComplete?.invoke(false)
      return
    }
    _isDownloading.value = true
    viewModelScope.launch {
      val res = repository.downloadPastExamForOffline(user.id, exam.id, course?.id)
      _isDownloading.value = false
      res.onSuccess {
        showMessage("Exam paper '${exam.title}' downloaded for offline study.")
        onComplete?.invoke(true)
      }.onFailure { err ->
        showMessage("Download failed: ${err.message}")
        onComplete?.invoke(false)
      }
    }
  }

  fun downloadQuiz(quizId: String, courseId: String? = null, onComplete: ((Boolean) -> Unit)? = null) {
    val user = currentUser.value
    if (user == null) {
      showMessage("Please sign in to download quizzes.")
      onComplete?.invoke(false)
      return
    }
    if (!isOnline.value) {
      showMessage("Active internet connection required to download quizzes.")
      onComplete?.invoke(false)
      return
    }
    _isDownloading.value = true
    viewModelScope.launch {
      val res = repository.downloadQuizForOffline(user.id, quizId, courseId)
      _isDownloading.value = false
      res.onSuccess {
        showMessage("Practice quiz downloaded for offline study.")
        onComplete?.invoke(true)
      }.onFailure { err ->
        showMessage("Download failed: ${err.message}")
        onComplete?.invoke(false)
      }
    }
  }

  fun selectCourse(course: CourseEntity) {
    _selectedCourse.value = course
    courseTopicsJob?.cancel()
    courseTopicsJob = viewModelScope.launch {
      repository.getTopicsForCourseFlow(course.id).collect { topics ->
        _courseTopics.value = topics
      }
    }
    courseNotesJob?.cancel()
    courseNotesJob = viewModelScope.launch {
      repository.getNotesForCourseFlow(course.id).collect { notes ->
        _courseNotes.value = notes
      }
    }
    coursePastExamsJob?.cancel()
    coursePastExamsJob = viewModelScope.launch {
      repository.getPastExamsForCourseFlow(course.id).collect { exams ->
        _coursePastExams.value = exams
      }
    }
    navigateTo(Screen.COURSE_DETAIL)
  }

  fun selectNote(note: NoteEntity) {
    _selectedNote.value = note
    navigateTo(Screen.NOTE_DETAIL)
  }

  fun selectPastExam(exam: PastExamEntity) {
    _selectedExam.value = exam
    navigateTo(Screen.PAST_EXAM_DETAIL)
  }

  // --- Quiz Handling ---
  fun startTopicQuiz(topic: TopicEntity, course: CourseEntity, isExamMode: Boolean) {
    viewModelScope.launch {
      val questions = repository.getQuestionsForTopic(topic.id)
      if (questions.isEmpty()) {
        showMessage("No practice questions available for this topic yet.")
        return@launch
      }
      _activeQuiz.value = ActiveQuizState(
        title = "${topic.title} Practice",
        courseId = course.id,
        courseTitle = course.title,
        topicId = topic.id,
        topicTitle = topic.title,
        isExamMode = isExamMode,
        questions = questions.shuffled(),
        currentIndex = 0,
        startTimeMillis = System.currentTimeMillis()
      )
      navigateTo(Screen.QUIZ)
    }
  }

  fun startCoursePractice(course: CourseEntity, isExamMode: Boolean) {
    viewModelScope.launch {
      val questions = repository.getQuestionsForCourse(course.id)
      if (questions.isEmpty()) {
        showMessage("No questions available for ${course.title} yet.")
        return@launch
      }
      _activeQuiz.value = ActiveQuizState(
        title = "${course.code} Comprehensive Practice",
        courseId = course.id,
        courseTitle = course.title,
        isExamMode = isExamMode,
        questions = questions.shuffled().take(15),
        currentIndex = 0,
        startTimeMillis = System.currentTimeMillis()
      )
      navigateTo(Screen.QUIZ)
    }
  }

  fun startExamQuiz(exam: PastExamEntity, course: CourseEntity) {
    viewModelScope.launch {
      val questions = repository.getPastExamQuestionsForCourse(course.id)
      val examQuestions = if (questions.isNotEmpty()) questions else repository.getQuestionsForCourse(course.id)
      if (examQuestions.isEmpty()) {
        showMessage("Digitized questions for this examination are being uploaded.")
        return@launch
      }
      _activeQuiz.value = ActiveQuizState(
        title = exam.title,
        courseId = course.id,
        courseTitle = course.title,
        examId = exam.id,
        isExamMode = true,
        questions = examQuestions,
        currentIndex = 0,
        startTimeMillis = System.currentTimeMillis()
      )
      navigateTo(Screen.QUIZ)
    }
  }

  fun selectAnswer(questionId: String, answer: String) {
    val current = _activeQuiz.value
    val newAnswers = current.selectedAnswers.toMutableMap()
    newAnswers[questionId] = answer
    val newSubmitted = current.submittedInPractice + questionId
    _activeQuiz.value = current.copy(
      selectedAnswers = newAnswers,
      submittedInPractice = newSubmitted
    )
  }

  fun submitPracticeAnswer(questionId: String) {
    val current = _activeQuiz.value
    if (current.selectedAnswers.containsKey(questionId)) {
      _activeQuiz.value = current.copy(
        submittedInPractice = current.submittedInPractice + questionId
      )
    }
  }

  fun nextQuestion() {
    val current = _activeQuiz.value
    if (current.currentIndex < current.questions.lastIndex) {
      _activeQuiz.value = current.copy(currentIndex = current.currentIndex + 1)
    }
  }

  fun previousQuestion() {
    val current = _activeQuiz.value
    if (current.currentIndex > 0) {
      _activeQuiz.value = current.copy(currentIndex = current.currentIndex - 1)
    }
  }

  fun jumpToQuestion(index: Int) {
    val current = _activeQuiz.value
    if (index in current.questions.indices) {
      _activeQuiz.value = current.copy(currentIndex = index)
    }
  }

  fun finishQuiz() {
    val current = _activeQuiz.value
    if (current.questions.isEmpty()) return

    val total = current.questions.size
    if (total == 0) {
      _currentScreen.value = Screen.DASHBOARD
      return
    }
    var correct = 0
    val answersLog = mutableListOf<String>()

    current.questions.forEach { q ->
      val selected = current.selectedAnswers[q.id] ?: ""
      val isCorrect = selected.equals(q.correctAnswer, ignoreCase = true)
      if (isCorrect) correct++
      answersLog.add("${q.id}|$selected|${q.correctAnswer}|$isCorrect")
    }

    val incorrect = (total - correct).coerceAtLeast(0)
    val scorePercent = ((correct * 100) / total).coerceIn(0, 100)
    val timeSpent = ((System.currentTimeMillis() - current.startTimeMillis) / 1000).toInt()
    val userId = currentUser.value?.id ?: "student_1"

    val attempt = QuizAttemptEntity(
      id = "att_" + UUID.randomUUID().toString().take(8),
      userId = userId,
      courseId = current.courseId,
      courseTitle = current.courseTitle,
      topicId = current.topicId,
      topicTitle = current.topicTitle,
      examId = current.examId,
      isExamMode = current.isExamMode,
      totalQuestions = total,
      correctCount = correct,
      incorrectCount = incorrect,
      scorePercent = scorePercent,
      timeSpentSeconds = timeSpent,
      answersJson = answersLog.joinToString(";")
    )

    viewModelScope.launch {
      repository.recordQuizAttempt(attempt)
      loadUserData()
      _activeQuiz.value = current.copy(
        isCompleted = true,
        resultAttempt = attempt
      )
      navigateTo(Screen.QUIZ_RESULT)

      // Update offline cache statistics for quiz progress
      _offlineCacheStats.value = repository.getOfflineCacheStats(userId)

      // Also persist to real cloud Firestore database if online
      try {
        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser != null && isOnline.value) {
          val fsAttempt = FirestoreQuizAttempt(
            id = attempt.id,
            userId = fbUser.uid,
            courseId = attempt.courseId,
            topicId = attempt.topicId ?: "",
            score = attempt.correctCount.toLong(),
            totalQuestions = attempt.totalQuestions.toLong(),
            percentage = attempt.scorePercent.toLong()
          )
          firestoreRepository.recordQuizAttempt(fsAttempt)
        }
      } catch (_: Exception) {}
    }
  }

  fun retakeQuiz() {
    val current = _activeQuiz.value
    _activeQuiz.value = current.copy(
      currentIndex = 0,
      selectedAnswers = emptyMap(),
      submittedInPractice = emptySet(),
      startTimeMillis = System.currentTimeMillis(),
      isCompleted = false,
      resultAttempt = null
    )
    navigateTo(Screen.QUIZ)
  }

  // --- Bookmarking ---
  fun toggleBookmark(itemType: String, itemId: String, courseId: String, courseTitle: String, title: String) {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      val isSaved = repository.toggleBookmark(user.id, itemType, itemId, courseId, courseTitle, title)
      showMessage(if (isSaved) "Saved to bookmarks" else "Removed from bookmarks")
      val fbUser = FirebaseAuth.getInstance().currentUser
      if (fbUser != null && isSaved) {
        val fsBookmark = FirestoreBookmark(
          id = "bm_${UUID.randomUUID().toString().take(8)}",
          userId = fbUser.uid,
          questionId = itemId,
          courseId = courseId,
          topicId = ""
        )
        firestoreRepository.addBookmark(fsBookmark)
      }
    }
  }

  fun isItemBookmarked(itemId: String): Boolean {
    return _userBookmarks.value.any { it.itemId == itemId }
  }

  fun removeBookmark(bookmarkId: String) {
    viewModelScope.launch {
      repository.deleteBookmark(bookmarkId)
      showMessage("Bookmark removed")
    }
  }

  // --- Search ---
  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
    if (query.isBlank()) {
      _searchCourseResults.value = emptyList()
      _searchNoteResults.value = emptyList()
      _searchQuestionResults.value = emptyList()
      return
    }

    viewModelScope.launch {
      _searchCourseResults.value = publishedCourses.value.filter {
        it.title.contains(query, ignoreCase = true) ||
          it.code.contains(query, ignoreCase = true) ||
          it.description.contains(query, ignoreCase = true)
      }
      _searchNoteResults.value = repository.searchNotes(query)
      _searchQuestionResults.value = repository.searchQuestions(query)
    }
  }

  private fun hasFirebaseCloudSession(): Boolean = FirebaseAuth.getInstance().currentUser != null

  private fun validateAdminCloudSession(): String? {
    val fbUser = FirebaseAuth.getInstance().currentUser
    if (fbUser == null) {
      val msg = "Google Admin sign-in session expired or missing. Please sign out and sign in with Google."
      showMessage(msg)
      return msg
    }
    val isAuthorizedAdmin = currentUser.value?.role == "ADMIN" ||
      repository.isSystemAdminEmail(fbUser.email ?: "")
    if (!isAuthorizedAdmin) {
      val msg = "Unauthorized: Administrator permissions required."
      showMessage(msg)
      return msg
    }
    return null
  }

  private fun requireAdminCloudSession(): Boolean = validateAdminCloudSession() == null

  private suspend fun refreshDiagnosticIfPermissionError(e: Exception) {
    if (e.message?.contains("permission", ignoreCase = true) == true) {
      _cloudAdminDiagnostic.value = firestoreRepository.diagnoseAdminAccess()
    }
  }

  // --- Admin operations ---
  fun adminSaveCourse(course: CourseEntity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        repository.saveCourse(course)
        showMessage("Course '${course.title}' published to central cloud database.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to save course."
        showMessage("Failed to save course: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeleteCourse(courseId: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deleteCourse(courseId)
        showMessage("Course removed from central database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to delete course: ${e.message}")
      }
    }
  }

  fun adminSaveTopic(topic: TopicEntity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        repository.saveTopic(topic)
        showMessage("Section '${topic.title}' published to central cloud database.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to save section."
        showMessage("Failed to save section: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeleteTopic(topicId: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deleteTopic(topicId)
        showMessage("Section removed from central database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to delete section: ${e.message}")
      }
    }
  }

  fun adminSaveQuestion(question: QuestionEntity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        repository.saveQuestion(question)
        showMessage("Question published to central cloud database & synced.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to save question."
        showMessage("Failed to save question: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeleteQuestion(questionId: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deleteQuestion(questionId)
        showMessage("Question deleted from central database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to delete question: ${e.message}")
      }
    }
  }

  fun adminSaveNote(note: NoteEntity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        repository.saveNote(note)
        showMessage("Revision note '${note.title}' published to central cloud database.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to save note."
        showMessage("Failed to save note: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeleteNote(noteId: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deleteNote(noteId)
        showMessage("Note deleted from central database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to delete note: ${e.message}")
      }
    }
  }

  fun adminSavePastExam(exam: PastExamEntity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        repository.savePastExam(exam)
        showMessage("Past exam '${exam.title}' published to central cloud database.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to publish past exam."
        showMessage("Failed to publish past exam: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeletePastExam(examId: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deletePastExam(examId)
        showMessage("Past exam removed from central database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to delete past exam: ${e.message}")
      }
    }
  }

  fun adminPublishAnnouncement(
    title: String,
    message: String,
    category: String,
    isUrgent: Boolean,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
  ) {
    validateAdminCloudSession()?.let { err ->
      onResult(false, err)
      return
    }
    viewModelScope.launch {
      try {
        val ann = AnnouncementEntity(
          id = "ann_" + UUID.randomUUID().toString().take(8),
          title = title.trim(),
          message = message.trim(),
          category = category,
          isUrgent = isUrgent
        )
        repository.saveAnnouncement(ann)
        repository.syncFromCloudNow()
        showMessage("Announcement broadcasted to all students & central cloud.")
        onResult(true, null)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        val msg = e.localizedMessage ?: e.message ?: "Failed to broadcast announcement."
        showMessage("Failed to broadcast announcement: $msg")
        onResult(false, msg)
      }
    }
  }

  fun adminDeleteAnnouncement(id: String) {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.deleteAnnouncement(id)
        repository.syncFromCloudNow()
        showMessage("Announcement removed from central cloud database.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to remove announcement: ${e.message}")
      }
    }
  }

  fun adminResetData() {
    if (!requireAdminCloudSession()) return
    viewModelScope.launch {
      try {
        repository.resetDatabase()
        loadUserData()
        showMessage("Platform database re-seeded to factory standard curriculum.")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        refreshDiagnosticIfPermissionError(e)
        showMessage("Failed to re-seed database: ${e.message}")
      }
    }
  }
}
