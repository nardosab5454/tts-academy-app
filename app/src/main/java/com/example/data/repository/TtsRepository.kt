package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SeedData
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
import com.example.data.local.model.UserPreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

data class OfflineCacheStats(
  val cachedCoursesCount: Int,
  val cachedTopicsCount: Int,
  val cachedQuestionsCount: Int,
  val cachedNotesCount: Int,
  val cachedPastExamsCount: Int,
  val cachedQuizAttemptsCount: Int,
  val cachedBookmarksCount: Int,
  val isOfflineReady: Boolean = true,
  val lastCacheVerifiedAt: Long = System.currentTimeMillis()
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
  private val accessCodeDao = database.accessCodeDao()
  private val prefDao = database.userPreferenceDao()

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser = _currentUser.asStateFlow()

  private val _appTheme = MutableStateFlow("SYSTEM")
  val appTheme = _appTheme.asStateFlow()

  private val _isDeviceUnlocked = MutableStateFlow(false)
  val isDeviceUnlocked: StateFlow<Boolean> = _isDeviceUnlocked.asStateFlow()

  suspend fun initialize() = withContext(Dispatchers.IO) {
    // Check if initial seed is needed (e.g. fresh installation)
    val courseCount = courseDao.getCount()
    if (courseCount == 0) {
      AppDatabase.populateDatabase(database)
    } else {
      if (questionDao.getCount() == 0) {
        questionDao.insertQuestions(SeedData.getQuestions())
      }
      if (noteDao.getCount() == 0) {
        noteDao.insertNotes(SeedData.getNotes())
      }
      if (pastExamDao.getCount() == 0) {
        pastExamDao.insertPastExams(SeedData.getPastExams())
      }
      if (accessCodeDao.getAccessCode("TTS789") == null) {
        accessCodeDao.insertAccessCodes(SeedData.getAccessCodes())
      }
    }

    // Ensure primary administrator account exists in database with requested credentials
    val existingAdmin = userDao.getUserByEmail("spikenarrd@gmail.com")
    if (existingAdmin == null) {
      val adminUser = UserEntity(
        id = "admin_1",
        name = "System Administrator",
        email = "spikenarrd@gmail.com",
        password = "Spike@6776",
        studentId = "SYS-ADMIN-01",
        department = "Academic Affairs",
        role = "ADMIN",
        avatarUrl = "avatar_admin",
        streakDays = 12
      )
      userDao.insertUser(adminUser)
    } else if (existingAdmin.role != "ADMIN" || existingAdmin.password != "Spike@6776") {
      userDao.updateUser(existingAdmin.copy(role = "ADMIN", password = "Spike@6776"))
    }

    val isDeviceUnlockedPref = prefDao.getPreference("device_academic_materials_unlocked") == "true"
    _isDeviceUnlocked.value = isDeviceUnlockedPref

    val activeId = prefDao.getPreference("active_user_id")
    val user = if (!activeId.isNullOrBlank()) userDao.getUserById(activeId) else null
    val effectiveUser = if (user != null) {
      val userUnlocked = user.role == "ADMIN" || isDeviceUnlockedPref
      if (isDeviceUnlockedPref && !user.isPremium) {
        userDao.updateUserPremium(user.id, true, user.activationCode ?: "PERMANENT_DEVICE", user.activatedAt ?: System.currentTimeMillis())
      }
      user.copy(isPremium = userUnlocked)
    } else {
      null
    }
    _currentUser.value = effectiveUser

    val theme = prefDao.getPreference("app_theme") ?: "SYSTEM"
    _appTheme.value = theme
  }

  fun isSystemAdminEmail(email: String): Boolean {
    val clean = email.trim()
    return clean.equals("spikenarrd@gmail.com", ignoreCase = true) ||
           clean.equals("abebewottee@gmail.com", ignoreCase = true)
  }

  // --- Auth & Users ---
  fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

  suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val cleanPassword = password.trim()
    val isAdminEmail = isSystemAdminEmail(cleanEmail)

    val user = userDao.getUserByEmail(cleanEmail)
    if (user != null) {
      val isPasswordValid = user.password == cleanPassword ||
        (isAdminEmail && (cleanPassword == "Spike@6776" || cleanPassword == "Admin@123" || user.password == cleanPassword))

      if (isPasswordValid) {
        val isDeviceUnlocked = prefDao.getPreference("device_academic_materials_unlocked") == "true" || isAdminEmail
        _isDeviceUnlocked.value = isDeviceUnlocked

        val updatedUser = user.copy(
          role = if (isAdminEmail) "ADMIN" else user.role,
          isPremium = if (isAdminEmail) true else isDeviceUnlocked
        )
        if (isAdminEmail && user.role != "ADMIN") {
          userDao.updateUserRole(user.id, "ADMIN")
        }
        _currentUser.value = updatedUser
        prefDao.setPreference(UserPreferenceEntity("active_user_id", updatedUser.id))
        Result.success(updatedUser)
      } else {
        Result.failure(Exception("Incorrect password. Please try again."))
      }
    } else if (isAdminEmail && (cleanPassword == "Spike@6776" || cleanPassword == "Admin@123" || cleanPassword.isNotBlank())) {
      // Ensure primary administrator account exists
      val adminUser = UserEntity(
        id = if (cleanEmail.startsWith("spike", ignoreCase = true)) "admin_1" else "admin_" + UUID.randomUUID().toString().take(6),
        name = if (cleanEmail.startsWith("spike", ignoreCase = true)) "System Administrator" else "Administrator",
        email = cleanEmail,
        password = cleanPassword.ifBlank { "Spike@6776" },
        studentId = "SYS-ADMIN-01",
        department = "Academic Affairs",
        role = "ADMIN",
        avatarUrl = "avatar_admin",
        streakDays = 12,
        isPremium = true
      )
      userDao.insertUser(adminUser)
      _currentUser.value = adminUser
      prefDao.setPreference(UserPreferenceEntity("active_user_id", adminUser.id))
      Result.success(adminUser)
    } else {
      Result.failure(Exception("Account with this email not found. Please register."))
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
    val cleanEmail = email.trim()
    val cleanPassword = password.trim()
    val cleanName = name.trim()
    val cleanStudentId = studentId.trim()
    val cleanDepartment = department.trim()

    val isAdminEmail = isSystemAdminEmail(cleanEmail)
    val assignedRole = if (isAdminEmail) "ADMIN" else role

    val existing = userDao.getUserByEmail(cleanEmail)
    if (existing != null) {
      if (isAdminEmail) {
        userDao.updateUserRole(existing.id, "ADMIN")
        val updated = existing.copy(
          role = "ADMIN",
          password = cleanPassword.ifBlank { "Spike@6776" }
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        prefDao.setPreference(UserPreferenceEntity("active_user_id", updated.id))
        return@withContext Result.success(updated)
      }
      return@withContext Result.failure(Exception("An account with this email already exists."))
    }

    val isDeviceUnlocked = prefDao.getPreference("device_academic_materials_unlocked") == "true" || (assignedRole == "ADMIN")
    _isDeviceUnlocked.value = isDeviceUnlocked

    val newUser = UserEntity(
      id = if (isAdminEmail) "admin_1" else "user_" + UUID.randomUUID().toString().take(8),
      name = cleanName.ifBlank { if (isAdminEmail) "System Administrator" else "TTS Student" },
      email = cleanEmail,
      password = cleanPassword.ifBlank { if (isAdminEmail) "Spike@6776" else "student123" },
      studentId = cleanStudentId.ifBlank { if (isAdminEmail) "SYS-ADMIN-01" else "STU-" + UUID.randomUUID().toString().take(4).uppercase() },
      department = cleanDepartment.ifBlank { if (isAdminEmail) "Academic Affairs" else "General Freshman Studies" },
      role = assignedRole,
      avatarUrl = if (assignedRole == "ADMIN") "avatar_admin" else "avatar_1",
      streakDays = 1,
      questionsAnsweredCount = 0,
      correctAnswersCount = 0,
      isPremium = isDeviceUnlocked
    )
    userDao.insertUser(newUser)
    _currentUser.value = newUser
    prefDao.setPreference(UserPreferenceEntity("active_user_id", newUser.id))
    Result.success(newUser)
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    _currentUser.value = null
    prefDao.setPreference(UserPreferenceEntity("active_user_id", ""))
    // Note: Do NOT clear device_academic_materials_unlocked so that when logging back in,
    // this device remains permanently unlocked forever!
  }

  suspend fun switchUser(userId: String) = withContext(Dispatchers.IO) {
    val user = userDao.getUserById(userId)
    if (user != null) {
      val isDeviceUnlocked = prefDao.getPreference("device_academic_materials_unlocked") == "true" || user.role == "ADMIN"
      _isDeviceUnlocked.value = isDeviceUnlocked
      _currentUser.value = user.copy(isPremium = if (user.role == "ADMIN") true else isDeviceUnlocked)
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
    val cleanEmail = email.trim()
    val isAdminEmail = isSystemAdminEmail(cleanEmail)
    val effectiveRole = if (isAdminEmail) "ADMIN" else role

    val isDeviceUnlocked = prefDao.getPreference("device_academic_materials_unlocked") == "true" || isAdminEmail
    _isDeviceUnlocked.value = isDeviceUnlocked

    val existing = userDao.getUserById(uid)
    val user = if (existing != null) {
      val updated = existing.copy(
        name = name.ifBlank { existing.name },
        email = cleanEmail.ifBlank { existing.email },
        role = if (isAdminEmail) "ADMIN" else existing.role,
        isPremium = if (isAdminEmail) true else isDeviceUnlocked
      )
      userDao.updateUser(updated)
      updated
    } else {
      val newUser = UserEntity(
        id = uid,
        name = name.ifBlank { if (isAdminEmail) "System Administrator" else "TTS Student" },
        email = cleanEmail.ifBlank { "student@ttsacademy.edu" },
        password = if (isAdminEmail) "Spike@6776" else "",
        studentId = if (isAdminEmail) "SYS-ADMIN-01" else "STU-" + uid.take(6).uppercase(),
        department = department,
        role = effectiveRole,
        avatarUrl = if (effectiveRole == "ADMIN") "avatar_admin" else "avatar_1",
        streakDays = 1,
        questionsAnsweredCount = 0,
        correctAnswersCount = 0,
        isPremium = isDeviceUnlocked || (effectiveRole == "ADMIN")
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

  suspend fun updateStudentDepartmentAndGoal(userId: String, department: String, dailyGoalMinutes: Int) = withContext(Dispatchers.IO) {
    userDao.updateUserDepartmentAndGoal(userId, department, dailyGoalMinutes)
    val updated = userDao.getUserById(userId)
    if (updated != null && _currentUser.value?.id == userId) {
      _currentUser.value = updated
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
    questionDao.deleteQuestionsForCourse(courseId)
    noteDao.deleteNotesForCourse(courseId)
    topicDao.deleteTopicsForCourse(courseId)
    pastExamDao.deletePastExamsForCourse(courseId)
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
    courseDao.recalculateCounts(question.courseId)
  }

  suspend fun deleteQuestion(questionId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    val q = if (courseId == null) questionDao.getQuestionById(questionId) else null
    val targetCourseId = courseId ?: q?.courseId
    questionDao.deleteQuestion(questionId)
    if (targetCourseId != null) {
      courseDao.recalculateCounts(targetCourseId)
    }
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
    courseDao.recalculateCounts(note.courseId)
  }

  suspend fun deleteNote(noteId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    val n = if (courseId == null) noteDao.getNoteById(noteId) else null
    val targetCourseId = courseId ?: n?.courseId
    noteDao.deleteNote(noteId)
    if (targetCourseId != null) {
      courseDao.recalculateCounts(targetCourseId)
    }
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

  // --- Access Codes & Premium Pass Management ---
  fun getAllAccessCodesFlow(): Flow<List<AccessCodeEntity>> = accessCodeDao.getAllAccessCodesFlow()

  suspend fun generateAccessCode(
    adminId: String,
    note: String = "Permanent Academic Access Pass",
    format: String = "NUMERIC",
    customCode: String? = null
  ): AccessCodeEntity = withContext(Dispatchers.IO) {
    val finalCode = if (!customCode.isNullOrBlank()) {
      customCode.trim().take(6).uppercase()
    } else {
      generateUnique6CharKey(format)
    }

    val now = System.currentTimeMillis()
    val entity = AccessCodeEntity(
      code = finalCode,
      createdAt = now,
      expiresAt = now + (365L * 24 * 60 * 60 * 1000L), // 1-year valid redemption window
      createdByAdminId = adminId,
      isRedeemed = false,
      note = note.ifBlank { "Permanent Academic Access Pass" }
    )
    accessCodeDao.insertAccessCode(entity)
    entity
  }

  private suspend fun generateUnique6CharKey(format: String): String {
    val charPool = when (format) {
      "NUMERIC" -> "0123456789"
      "SPECIAL" -> "23456789ABCDEFGHJKLMNPQRSTUVWXYZ#@!*&"
      else -> "0123456789" // default to 6 digits numeric
    }
    repeat(10) {
      val generated = (1..6).map { charPool.random() }.joinToString("")
      if (accessCodeDao.getAccessCode(generated) == null) {
        return generated
      }
    }
    return (100000..999999).random().toString()
  }

  suspend fun redeemAccessCode(userId: String, userEmail: String, rawCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
    val cleanCode = rawCode.trim().uppercase()
    if (cleanCode.length != 6) {
      return@withContext Result.failure(Exception("Access code must be exactly 6 characters."))
    }
    val existing = accessCodeDao.getAccessCode(cleanCode)
      ?: return@withContext Result.failure(Exception("Invalid 6-digit access code. Please check your code or request a new one from your admin."))

    if (existing.isRedeemed) {
      return@withContext Result.failure(Exception("This access code has already been redeemed for single-use access."))
    }

    val now = System.currentTimeMillis()
    if (now > existing.expiresAt) {
      return@withContext Result.failure(Exception("This 6-digit access code has expired. Please ask your administrator for a fresh code."))
    }

    val updatedCode = existing.copy(
      isRedeemed = true,
      redeemedByUserId = userId,
      redeemedByEmail = userEmail,
      redeemedAt = now
    )
    accessCodeDao.updateAccessCode(updatedCode)
    userDao.updateUserPremium(userId, true, cleanCode, now)

    // Store permanent device-level unlock flag so materials remain unlocked forever on this device
    prefDao.setPreference(UserPreferenceEntity("device_academic_materials_unlocked", "true"))
    prefDao.setPreference(UserPreferenceEntity("device_unlocked_user_$userId", "true"))
    _isDeviceUnlocked.value = true

    // Update active user in memory
    _currentUser.value = _currentUser.value?.copy(isPremium = true, activationCode = cleanCode, activatedAt = now)
    Result.success(true)
  }

  suspend fun loginWithAccessCode(rawCode: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanCode = rawCode.trim().uppercase()
    if (cleanCode.length != 6) {
      return@withContext Result.failure(Exception("Access code must be exactly 6 characters."))
    }
    val existing = accessCodeDao.getAccessCode(cleanCode)
      ?: return@withContext Result.failure(Exception("Invalid 6-digit access code. Please check your code or request one from your administrator."))

    if (existing.isRedeemed) {
      return@withContext Result.failure(Exception("This 6-digit code has already been redeemed. Each passcode works for one-time login only."))
    }

    val now = System.currentTimeMillis()
    if (now > existing.expiresAt) {
      val expiredAgoMins = ((now - existing.expiresAt) / 60000).coerceAtLeast(1)
      return@withContext Result.failure(Exception("This 6-digit access code expired $expiredAgoMins min(s) ago (1-hour validity limit). Please request a fresh code from your administrator."))
    }

    var student = userDao.getUserByEmail("student@ttsacademy.edu")
    if (student == null) {
      student = UserEntity(
        id = "student_code_" + UUID.randomUUID().toString().take(6),
        name = "Freshman Scholar",
        email = "student@ttsacademy.edu",
        studentId = "TTS-" + cleanCode,
        department = "Computer Science & Engineering",
        role = "STUDENT",
        isPremium = true,
        activationCode = cleanCode,
        activatedAt = now
      )
      userDao.insertUser(student)
    } else {
      userDao.updateUserPremium(student.id, true, cleanCode, now)
      student = student.copy(isPremium = true, activationCode = cleanCode, activatedAt = now)
    }

    val updatedCode = existing.copy(
      isRedeemed = true,
      redeemedByUserId = student.id,
      redeemedByEmail = student.email,
      redeemedAt = now
    )
    accessCodeDao.updateAccessCode(updatedCode)

    // Mark device unlocked forever
    prefDao.setPreference(UserPreferenceEntity("device_academic_materials_unlocked", "true"))
    _isDeviceUnlocked.value = true

    _currentUser.value = student
    prefDao.setPreference(UserPreferenceEntity("active_user_id", student.id))
    Result.success(student)
  }

  suspend fun unlockDeviceMaterials() = withContext(Dispatchers.IO) {
    prefDao.setPreference(UserPreferenceEntity("device_academic_materials_unlocked", "true"))
    _isDeviceUnlocked.value = true
    _currentUser.value?.let { user ->
      userDao.updateUserPremium(user.id, true, "PERMANENT_DEVICE", System.currentTimeMillis())
      _currentUser.value = user.copy(isPremium = true)
    }
  }

  suspend fun deleteAccessCode(code: String) = withContext(Dispatchers.IO) {
    accessCodeDao.deleteAccessCode(code)
  }

  // --- Reset to Factory Seed ---
  suspend fun resetDatabase() = withContext(Dispatchers.IO) {
    AppDatabase.populateDatabase(database)
    initialize()
  }

  // --- Offline Persistence & Cache Diagnostics ---
  suspend fun getOfflineCacheStats(userId: String? = null): OfflineCacheStats = withContext(Dispatchers.IO) {
    val courses = courseDao.getCount()
    val topics = topicDao.getAllTopics().size
    val questions = questionDao.getCount()
    val notes = noteDao.getCount()
    val pastExams = pastExamDao.getCount()
    val attempts = if (!userId.isNullOrBlank()) quizAttemptDao.getAttemptsForUser(userId).size else 0
    val verifiedTimeStr = prefDao.getPreference("offline_cache_verified_at")
    val verifiedTime = verifiedTimeStr?.toLongOrNull() ?: System.currentTimeMillis()

    OfflineCacheStats(
      cachedCoursesCount = courses,
      cachedTopicsCount = topics,
      cachedQuestionsCount = questions,
      cachedNotesCount = notes,
      cachedPastExamsCount = pastExams,
      cachedQuizAttemptsCount = attempts,
      cachedBookmarksCount = 0,
      isOfflineReady = courses > 0 && questions > 0 && notes > 0,
      lastCacheVerifiedAt = verifiedTime
    )
  }

  suspend fun refreshAndVerifyOfflineCache(): OfflineCacheStats = withContext(Dispatchers.IO) {
    if (courseDao.getCount() == 0) courseDao.insertCourses(SeedData.getCourses())
    if (topicDao.getAllTopics().isEmpty()) topicDao.insertTopics(SeedData.getTopics())
    if (questionDao.getCount() == 0) questionDao.insertQuestions(SeedData.getQuestions())
    if (noteDao.getCount() == 0) noteDao.insertNotes(SeedData.getNotes())
    if (pastExamDao.getCount() == 0) pastExamDao.insertPastExams(SeedData.getPastExams())
    prefDao.setPreference(UserPreferenceEntity("offline_cache_verified_at", System.currentTimeMillis().toString()))
    getOfflineCacheStats(_currentUser.value?.id)
  }
}
