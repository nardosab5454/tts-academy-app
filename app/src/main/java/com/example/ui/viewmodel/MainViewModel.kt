package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.BookmarkEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.FirestoreBookmark
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreUserProfile
import com.example.data.repository.TtsRepository
import com.example.data.repository.WeakTopicSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
  AUTH
}

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

  private val database = AppDatabase.getDatabase(application, viewModelScope)
  val repository = TtsRepository(database)
  val firestoreRepository = FirestoreRepository(application)

  fun syncWithFirebaseUser(firebaseUser: FirebaseUser?) {
    if (firebaseUser == null) return
    viewModelScope.launch {
      val uid = firebaseUser.uid
      val email = firebaseUser.email ?: "student@ttsacademy.edu"
      val name = firebaseUser.displayName ?: "TTS Student"
      val profileResult = firestoreRepository.getOrCreateUserProfile(uid, email, name)
      profileResult.onSuccess { profile ->
        repository.syncFirebaseUser(
          uid = uid,
          email = email,
          name = profile.name,
          role = profile.role,
          department = profile.department
        )
        loadUserData()
      }
    }
  }

  // Current active screen and navigation history
  private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val screenBackStack = mutableListOf<Screen>()

  // Active user and Theme
  val currentUser: StateFlow<UserEntity?> = repository.currentUser
  val appTheme: StateFlow<String> = repository.appTheme

  // Data streams (Eagerly shared for instant live updates across the platform)
  val publishedCourses: StateFlow<List<CourseEntity>> = repository.getPublishedCoursesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allCourses: StateFlow<List<CourseEntity>> = repository.getAllCoursesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allPublishedNotes: StateFlow<List<NoteEntity>> = repository.getAllPublishedNotesFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allPastExams: StateFlow<List<PastExamEntity>> = repository.getAllPastExamsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val announcements: StateFlow<List<AnnouncementEntity>> = repository.getPublishedAnnouncementsFlow()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
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

  // Gemini AI Tutor state
  private val _isAiLoading = MutableStateFlow(false)
  val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

  private val _aiResponse = MutableStateFlow<String?>(null)
  val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

  private val _aiActiveContext = MutableStateFlow<String?>(null)
  val aiActiveContext: StateFlow<String?> = _aiActiveContext.asStateFlow()

  // Feedback banner / snackbar
  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message.asStateFlow()

  init {
    viewModelScope.launch {
      repository.initialize()
    }
    viewModelScope.launch {
      repository.currentUser.collect { user ->
        if (user != null) {
          loadUserData(user.id)
        } else {
          _userAttempts.value = emptyList()
          _userBookmarks.value = emptyList()
          _weakTopics.value = emptyList()
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
        showMessage("Welcome back, ${user.name}.")
        onResult(true, "")
      }.onFailure { err ->
        onResult(false, err.message ?: "Authentication failed.")
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
        showMessage("Account created! Welcome, ${user.name}.")
        onResult(true, "")
      }.onFailure { err ->
        onResult(false, err.message ?: "Registration failed.")
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      try {
        FirebaseAuth.getInstance().signOut()
      } catch (_: Exception) {}
      repository.signOut()
      _currentScreen.value = Screen.DASHBOARD
      screenBackStack.clear()
    }
  }

  fun updateUserRole(userId: String, newRole: String) {
    viewModelScope.launch {
      repository.setUserRole(userId, newRole)
      showMessage("User role updated to $newRole.")
    }
  }

  fun createAdmin(name: String, email: String, password: String, department: String) {
    viewModelScope.launch {
      val res = repository.createAdminAccount(name, email, password, department)
      res.onSuccess { admin ->
        showMessage("Admin account created for ${admin.name}.")
      }.onFailure { err ->
        showMessage("Failed to create admin: ${err.message}")
      }
    }
  }

  fun promoteUserByEmail(email: String) {
    viewModelScope.launch {
      val res = repository.promoteUserByEmail(email)
      res.onSuccess { user ->
        showMessage("Granted full administrator privileges to ${user.name} (${user.email}).")
      }.onFailure { err ->
        showMessage("Could not promote user: ${err.message}")
      }
    }
  }

  // --- Gemini AI Student Clarification & Research ---
  fun askGeminiTutor(
    prompt: String,
    courseTitle: String? = null,
    contextInfo: String? = null,
    onResult: ((String) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isAiLoading.value = true
      _aiActiveContext.value = courseTitle ?: "Freshman University Topic"
      val explanation = com.example.data.remote.GeminiTutorService.askTutor(
        prompt = prompt,
        courseTitle = courseTitle,
        contextInfo = contextInfo
      )
      _aiResponse.value = explanation
      _isAiLoading.value = false
      onResult?.invoke(explanation)
    }
  }

  fun clearAiTutorState() {
    _aiResponse.value = null
    _isAiLoading.value = false
    _aiActiveContext.value = null
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

  private fun loadUserData(userId: String? = null) {
    val uid = userId ?: currentUser.value?.id ?: return
    viewModelScope.launch {
      repository.getAttemptsForUserFlow(uid).collect { attempts ->
        _userAttempts.value = attempts
        _weakTopics.value = repository.getWeakTopics(uid)
      }
    }
    viewModelScope.launch {
      repository.getBookmarksForUserFlow(uid).collect { bms ->
        _userBookmarks.value = bms
      }
    }
  }

  fun selectCourse(course: CourseEntity) {
    _selectedCourse.value = course
    viewModelScope.launch {
      _courseTopics.value = repository.getTopicsForCourse(course.id)
      repository.getNotesForCourseFlow(course.id).collect { notes ->
        _courseNotes.value = notes
      }
    }
    viewModelScope.launch {
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
    var correct = 0
    val answersLog = mutableListOf<String>()

    current.questions.forEach { q ->
      val selected = current.selectedAnswers[q.id] ?: ""
      val isCorrect = selected.equals(q.correctAnswer, ignoreCase = true)
      if (isCorrect) correct++
      answersLog.add("${q.id}|$selected|${q.correctAnswer}|$isCorrect")
    }

    val incorrect = total - correct
    val scorePercent = (correct * 100) / total
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

      // Also persist to real cloud Firestore database
      val fbUser = FirebaseAuth.getInstance().currentUser
      if (fbUser != null) {
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

  // --- Admin operations ---
  fun adminSaveCourse(course: CourseEntity) {
    viewModelScope.launch {
      repository.saveCourse(course)
      showMessage("Course '${course.title}' saved successfully.")
    }
  }

  fun adminDeleteCourse(courseId: String) {
    viewModelScope.launch {
      repository.deleteCourse(courseId)
      showMessage("Course removed.")
    }
  }

  fun adminSaveQuestion(question: QuestionEntity) {
    viewModelScope.launch {
      repository.saveQuestion(question)
      showMessage("Question saved successfully.")
    }
  }

  fun adminDeleteQuestion(questionId: String) {
    viewModelScope.launch {
      repository.deleteQuestion(questionId)
      showMessage("Question deleted.")
    }
  }

  fun adminSaveNote(note: NoteEntity) {
    viewModelScope.launch {
      repository.saveNote(note)
      showMessage("Revision note '${note.title}' saved.")
    }
  }

  fun adminDeleteNote(noteId: String) {
    viewModelScope.launch {
      repository.deleteNote(noteId)
      showMessage("Note deleted.")
    }
  }

  fun adminSavePastExam(exam: PastExamEntity) {
    viewModelScope.launch {
      repository.savePastExam(exam)
      showMessage("Past exam '${exam.title}' published.")
    }
  }

  fun adminDeletePastExam(examId: String) {
    viewModelScope.launch {
      repository.deletePastExam(examId)
      showMessage("Past exam removed.")
    }
  }

  fun adminPublishAnnouncement(title: String, message: String, category: String, isUrgent: Boolean) {
    viewModelScope.launch {
      val ann = AnnouncementEntity(
        id = "ann_" + UUID.randomUUID().toString().take(8),
        title = title.trim(),
        message = message.trim(),
        category = category,
        isUrgent = isUrgent
      )
      repository.saveAnnouncement(ann)
      showMessage("Announcement broadcasted to all students.")
    }
  }

  fun adminDeleteAnnouncement(id: String) {
    viewModelScope.launch {
      repository.deleteAnnouncement(id)
      showMessage("Announcement removed.")
    }
  }

  fun adminResetData() {
    viewModelScope.launch {
      repository.resetDatabase()
      loadUserData()
      showMessage("Platform database re-seeded to factory standard curriculum.")
    }
  }
}
