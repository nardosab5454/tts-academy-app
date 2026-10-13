package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SeedData
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.BookmarkEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import com.example.data.local.model.UserPreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class WeakTopicSummary(
  val topicId: String,
  val topicTitle: String,
  val courseId: String,
  val courseTitle: String,
  val accuracyPercent: Int,
  val questionsAttempted: Int
)

class TtsRepository(private val database: AppDatabase) {

  private val userDao = database.userDao()
  private val courseDao = database.courseDao()
  private val topicDao = database.topicDao()
  private val questionDao = database.questionDao()
  private val noteDao = database.noteDao()
  private val pastExamDao = database.pastExamDao()
  private val quizAttemptDao = database.quizAttemptDao()
  private val bookmarkDao = database.bookmarkDao()
  private val announcementDao = database.announcementDao()
  private val prefDao = database.userPreferenceDao()

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser = _currentUser.asStateFlow()

  private val _appTheme = MutableStateFlow("SYSTEM")
  val appTheme = _appTheme.asStateFlow()

  suspend fun initialize() = withContext(Dispatchers.IO) {
    // Check if initial seed is needed (e.g. fresh installation)
    val courseCount = courseDao.getCount()
    if (courseCount == 0) {
      AppDatabase.populateDatabase(database)
    }

    val activeId = prefDao.getPreference("active_user_id") ?: "student_1"
    val user = userDao.getUserById(activeId) ?: userDao.getAllUsers().firstOrNull()
    _currentUser.value = user

    val theme = prefDao.getPreference("app_theme") ?: "SYSTEM"
    _appTheme.value = theme
  }

  // --- Auth & Users ---
  fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

  suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val cleanPassword = password.trim()

    val user = userDao.getUserByEmail(cleanEmail)
    if (user != null) {
      val isPasswordValid = user.password == cleanPassword ||
        (cleanEmail.equals("spikenarrd@gmail.com", ignoreCase = true) && cleanPassword == "Spike@6776")

      if (isPasswordValid) {
        _currentUser.value = user
        prefDao.setPreference(UserPreferenceEntity("active_user_id", user.id))
        Result.success(user)
      } else {
        Result.failure(Exception("Incorrect password."))
      }
    } else if (cleanEmail.equals("spikenarrd@gmail.com", ignoreCase = true) && cleanPassword == "Spike@6776") {
      // Ensure primary administrator account exists
      val adminUser = UserEntity(
        id = "admin_1",
        name = "System Administrator",
        email = "spikenarrd@gmail.com",
        password = "Spike@6776",
        studentId = "SYS-ADMIN-01",
        department = "Academic Affairs",
        role = "ADMIN",
        avatarUrl = "avatar_admin"
      )
      userDao.insertUser(adminUser)
      _currentUser.value = adminUser
      prefDao.setPreference(UserPreferenceEntity("active_user_id", adminUser.id))
      Result.success(adminUser)
    } else {
      Result.failure(Exception("Account with this email not found."))
    }
  }

  suspend fun setUserRole(userId: String, newRole: String) = withContext(Dispatchers.IO) {
    userDao.updateUserRole(userId, newRole)
    if (_currentUser.value?.id == userId) {
      val updated = userDao.getUserById(userId)
      _currentUser.value = updated
    }
  }

  suspend fun createAdminAccount(
    name: String,
    email: String,
    password: String,
    department: String
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val existing = userDao.getUserByEmail(cleanEmail)
    if (existing != null) {
      userDao.updateUserRole(existing.id, "ADMIN")
      val updated = existing.copy(role = "ADMIN")
      userDao.updateUser(updated)
      return@withContext Result.success(updated)
    }

    val newAdmin = UserEntity(
      id = "admin_" + UUID.randomUUID().toString().take(8),
      name = name.trim().ifBlank { "Administrator" },
      email = cleanEmail,
      password = password.trim().ifBlank { "Admin@123" },
      studentId = "ADM-" + UUID.randomUUID().toString().take(4).uppercase(),
      department = department.trim().ifBlank { "Academic Directorate" },
      role = "ADMIN",
      avatarUrl = "avatar_admin",
      streakDays = 1,
      questionsAnsweredCount = 0,
      correctAnswersCount = 0
    )
    userDao.insertUser(newAdmin)
    Result.success(newAdmin)
  }

  suspend fun promoteUserByEmail(email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val existing = userDao.getUserByEmail(cleanEmail)
    if (existing != null) {
      userDao.updateUserRole(existing.id, "ADMIN")
      val updated = existing.copy(role = "ADMIN")
      userDao.updateUser(updated)
      Result.success(updated)
    } else {
      createAdminAccount(
        name = cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
        email = cleanEmail,
        password = "Admin@${cleanEmail.take(4)}",
        department = "University Administration"
      )
    }
  }

  suspend fun register(
    name: String,
    email: String,
    password: String = "student123",
    studentId: String,
    department: String,
    role: String = "STUDENT"
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val existing = userDao.getUserByEmail(email.trim())
    if (existing != null) {
      return@withContext Result.failure(Exception("An account with this email already exists."))
    }
    val newUser = UserEntity(
      id = "user_" + UUID.randomUUID().toString().take(8),
      name = name.trim(),
      email = email.trim(),
      password = password.trim().ifBlank { "student123" },
      studentId = studentId.trim(),
      department = department.trim(),
      role = role,
      avatarUrl = if (role == "ADMIN") "avatar_admin" else "avatar_1",
      streakDays = 1,
      questionsAnsweredCount = 0,
      correctAnswersCount = 0
    )
    userDao.insertUser(newUser)
    _currentUser.value = newUser
    prefDao.setPreference(UserPreferenceEntity("active_user_id", newUser.id))
    Result.success(newUser)
  }

  suspend fun switchUser(userId: String) = withContext(Dispatchers.IO) {
    val user = userDao.getUserById(userId)
    if (user != null) {
      _currentUser.value = user
      prefDao.setPreference(UserPreferenceEntity("active_user_id", user.id))
    }
  }

  suspend fun switchActiveUser(userId: String) = switchUser(userId)

  suspend fun syncFirebaseUser(
    uid: String,
    email: String,
    name: String,
    role: String = "STUDENT",
    department: String = "Computer Science"
  ): UserEntity = withContext(Dispatchers.IO) {
    val existing = userDao.getUserById(uid)
    val user = if (existing != null) {
      val updated = existing.copy(
        name = name.ifBlank { existing.name },
        email = email.ifBlank { existing.email }
      )
      userDao.updateUser(updated)
      updated
    } else {
      val newUser = UserEntity(
        id = uid,
        name = name.ifBlank { "TTS Student" },
        email = email.ifBlank { "student@ttsacademy.edu" },
        password = "",
        studentId = "STU-" + uid.take(6).uppercase(),
        department = department,
        role = role,
        avatarUrl = if (role == "ADMIN") "avatar_admin" else "avatar_1",
        streakDays = 1,
        questionsAnsweredCount = 0,
        correctAnswersCount = 0
      )
      userDao.insertUser(newUser)
      newUser
    }
    _currentUser.value = user
    prefDao.setPreference(UserPreferenceEntity("active_user_id", user.id))
    user
  }

  suspend fun updateUserProfile(user: UserEntity) = withContext(Dispatchers.IO) {
    userDao.updateUser(user)
    if (_currentUser.value?.id == user.id) {
      _currentUser.value = user
    }
  }

  suspend fun setAppTheme(theme: String) = withContext(Dispatchers.IO) {
    _appTheme.value = theme
    prefDao.setPreference(UserPreferenceEntity("app_theme", theme))
  }

  // --- Courses & Topics ---
  fun getPublishedCoursesFlow(): Flow<List<CourseEntity>> = courseDao.getPublishedCoursesFlow()
  fun getAllCoursesFlow(): Flow<List<CourseEntity>> = courseDao.getAllCoursesFlow()

  suspend fun getCourseById(courseId: String): CourseEntity? = withContext(Dispatchers.IO) {
    courseDao.getCourseById(courseId)
  }

  fun getTopicsForCourseFlow(courseId: String): Flow<List<TopicEntity>> = topicDao.getTopicsForCourseFlow(courseId)
  suspend fun getTopicsForCourse(courseId: String): List<TopicEntity> = withContext(Dispatchers.IO) {
    topicDao.getTopicsForCourse(courseId)
  }

  suspend fun saveCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
    courseDao.insertCourse(course)
  }

  suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
    courseDao.deleteCourse(courseId)
  }

  suspend fun saveTopic(topic: TopicEntity) = withContext(Dispatchers.IO) {
    topicDao.insertTopic(topic)
  }

  suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
    topicDao.deleteTopic(topicId)
  }

  // --- Questions ---
  suspend fun getQuestionsForTopic(topicId: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
    questionDao.getQuestionsForTopic(topicId)
  }

  suspend fun getQuestionsForCourse(courseId: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
    questionDao.getQuestionsForCourse(courseId)
  }

  suspend fun getPastExamQuestionsForCourse(courseId: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
    questionDao.getPastExamQuestionsForCourse(courseId)
  }

  suspend fun getQuestionsForExamYear(courseId: String, year: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
    questionDao.getQuestionsForExamYear(courseId, year)
  }

  fun getAllQuestionsFlow(): Flow<List<QuestionEntity>> = questionDao.getAllQuestionsFlow()

  suspend fun searchQuestions(query: String): List<QuestionEntity> = withContext(Dispatchers.IO) {
    questionDao.searchQuestions(query)
  }

  suspend fun saveQuestion(question: QuestionEntity) = withContext(Dispatchers.IO) {
    questionDao.insertQuestion(question)
  }

  suspend fun deleteQuestion(questionId: String) = withContext(Dispatchers.IO) {
    questionDao.deleteQuestion(questionId)
  }

  // --- Notes ---
  fun getNotesForCourseFlow(courseId: String): Flow<List<NoteEntity>> = noteDao.getNotesForCourseFlow(courseId)
  fun getAllPublishedNotesFlow(): Flow<List<NoteEntity>> = noteDao.getAllPublishedNotesFlow()
  fun getAllNotesFlow(): Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()

  suspend fun getNoteById(id: String): NoteEntity? = withContext(Dispatchers.IO) {
    noteDao.getNoteById(id)
  }

  suspend fun searchNotes(query: String): List<NoteEntity> = withContext(Dispatchers.IO) {
    noteDao.searchNotes(query)
  }

  suspend fun saveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
    noteDao.insertNote(note)
  }

  suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
    noteDao.deleteNote(noteId)
  }

  // --- Past Exams ---
  fun getPastExamsForCourseFlow(courseId: String): Flow<List<PastExamEntity>> = pastExamDao.getPastExamsForCourseFlow(courseId)
  fun getAllPastExamsFlow(): Flow<List<PastExamEntity>> = pastExamDao.getAllPastExamsFlow()
  fun getAllAdminPastExamsFlow(): Flow<List<PastExamEntity>> = pastExamDao.getAllAdminPastExamsFlow()

  suspend fun getPastExamById(id: String): PastExamEntity? = withContext(Dispatchers.IO) {
    pastExamDao.getPastExamById(id)
  }

  suspend fun savePastExam(exam: PastExamEntity) = withContext(Dispatchers.IO) {
    pastExamDao.insertPastExam(exam)
  }

  suspend fun deletePastExam(examId: String) = withContext(Dispatchers.IO) {
    pastExamDao.deletePastExam(examId)
  }

  // --- Quiz Attempts & Analytics ---
  fun getAttemptsForUserFlow(userId: String): Flow<List<QuizAttemptEntity>> = quizAttemptDao.getAttemptsForUserFlow(userId)

  suspend fun recordQuizAttempt(attempt: QuizAttemptEntity) = withContext(Dispatchers.IO) {
    quizAttemptDao.insertAttempt(attempt)
    userDao.incrementUserStats(attempt.userId, attempt.totalQuestions, attempt.correctCount)
    // Reload active user state
    val updatedUser = userDao.getUserById(attempt.userId)
    if (_currentUser.value?.id == attempt.userId) {
      _currentUser.value = updatedUser
    }
  }

  suspend fun getWeakTopics(userId: String): List<WeakTopicSummary> = withContext(Dispatchers.IO) {
    val attempts = quizAttemptDao.getAttemptsForUser(userId)
    val topicAttempts = attempts.filter { it.topicId != null }
    if (topicAttempts.isEmpty()) return@withContext emptyList()

    val topicMap = topicAttempts.groupBy { it.topicId!! }
    val topics = topicDao.getAllTopics().associateBy { it.id }
    val courses = courseDao.getAllCourses().associateBy { it.id }

    topicMap.mapNotNull { (topicId, list) ->
      val totalQ = list.sumOf { it.totalQuestions }
      val totalCorrect = list.sumOf { it.correctCount }
      val accuracy = if (totalQ > 0) (totalCorrect * 100) / totalQ else 100
      val topic = topics[topicId]
      val course = courses[topic?.courseId ?: ""]

      if (accuracy < 75 && topic != null) {
        WeakTopicSummary(
          topicId = topicId,
          topicTitle = topic.title,
          courseId = topic.courseId,
          courseTitle = course?.title ?: "Course",
          accuracyPercent = accuracy,
          questionsAttempted = totalQ
        )
      } else null
    }.sortedBy { it.accuracyPercent }
  }

  // --- Bookmarks ---
  fun getBookmarksForUserFlow(userId: String): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForUserFlow(userId)

  suspend fun isBookmarked(userId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
    bookmarkDao.isBookmarked(userId, itemId) > 0
  }

  suspend fun toggleBookmark(
    userId: String,
    itemType: String,
    itemId: String,
    courseId: String,
    courseTitle: String,
    title: String
  ): Boolean = withContext(Dispatchers.IO) {
    val isCurrentlyBookmarked = bookmarkDao.isBookmarked(userId, itemId) > 0
    if (isCurrentlyBookmarked) {
      bookmarkDao.deleteBookmark(userId, itemId)
      false
    } else {
      val bookmark = BookmarkEntity(
        id = "bm_" + UUID.randomUUID().toString().take(8),
        userId = userId,
        itemType = itemType,
        itemId = itemId,
        courseId = courseId,
        courseTitle = courseTitle,
        title = title
      )
      bookmarkDao.insertBookmark(bookmark)
      true
    }
  }

  suspend fun deleteBookmark(id: String) = withContext(Dispatchers.IO) {
    bookmarkDao.deleteBookmarkById(id)
  }

  // --- Announcements ---
  fun getPublishedAnnouncementsFlow(): Flow<List<AnnouncementEntity>> = announcementDao.getPublishedAnnouncementsFlow()
  fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncementsFlow()

  suspend fun saveAnnouncement(announcement: AnnouncementEntity) = withContext(Dispatchers.IO) {
    announcementDao.insertAnnouncement(announcement)
  }

  suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
    announcementDao.deleteAnnouncement(id)
  }

  // --- Reset to Factory Seed ---
  suspend fun resetDatabase() = withContext(Dispatchers.IO) {
    AppDatabase.populateDatabase(database)
    initialize()
  }
}
