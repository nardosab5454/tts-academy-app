package com.example.data.repository

import android.util.Log
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.*
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
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

class TtsRepository(
  private val database: AppDatabase,
  val connectivityObserver: NetworkConnectivityObserver? = null,
  var firestoreRepository: FirestoreRepository? = null
) {

  fun isNetworkConnected(): Boolean = connectivityObserver?.isCurrentlyConnected() ?: false

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

    // Purge any legacy mock students from local storage
    userDao.deleteUserById("student_1")
    userDao.deleteUserById("student_2")

    // Invalidate local device bypass flag so all unlocks strictly rely on server verification
    prefDao.setPreference(UserPreferenceEntity("device_academic_materials_unlocked", "false"))
    _isDeviceUnlocked.value = false

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
        streakDays = 12,
        isPremium = true
      )
      userDao.insertUser(adminUser)
    } else if (existingAdmin.role != "ADMIN" || existingAdmin.password != "Spike@6776") {
      userDao.updateUser(existingAdmin.copy(role = "ADMIN", password = "Spike@6776", isPremium = true))
    }

    val existingAbebe = userDao.getUserByEmail("abebewottee@gmail.com")
    if (existingAbebe == null) {
      val abebeUser = UserEntity(
        id = "admin_abebe",
        name = "Abebe Wotte (Admin)",
        email = "abebewottee@gmail.com",
        password = "Admin@123",
        studentId = "SYS-ADMIN-02",
        department = "Academic Affairs",
        role = "ADMIN",
        avatarUrl = "avatar_admin",
        streakDays = 15,
        isPremium = true
      )
      userDao.insertUser(abebeUser)
    } else if (existingAbebe.role != "ADMIN") {
      userDao.updateUser(existingAbebe.copy(role = "ADMIN", isPremium = true))
    }

    val activeId = prefDao.getPreference("active_user_id")
    val user = if (!activeId.isNullOrBlank()) userDao.getUserById(activeId) else null
    val effectiveUser = if (user != null) {
      user.copy(isPremium = user.role == "ADMIN" || user.isPremium)
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

  suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
    userDao.getUserById(userId)
  }

  suspend fun insertUserFromCloud(user: UserEntity) = withContext(Dispatchers.IO) {
    userDao.insertUser(user)
  }

  suspend fun updateUserFromCloud(user: UserEntity) = withContext(Dispatchers.IO) {
    userDao.updateUser(user)
  }

  suspend fun upsertAnnouncementsFromCloud(announcements: List<AnnouncementEntity>) = withContext(Dispatchers.IO) {
    announcementDao.insertAnnouncements(announcements)
  }

  suspend fun upsertCoursesFromCloud(courses: List<CourseEntity>) = withContext(Dispatchers.IO) {
    courseDao.insertCourses(courses)
  }

  suspend fun upsertQuestionsFromCloud(questions: List<QuestionEntity>) = withContext(Dispatchers.IO) {
    questionDao.insertQuestions(questions)
  }

  suspend fun upsertNotesFromCloud(notes: List<NoteEntity>) = withContext(Dispatchers.IO) {
    noteDao.insertNotes(notes)
  }

  suspend fun upsertPastExamsFromCloud(exams: List<PastExamEntity>) = withContext(Dispatchers.IO) {
    pastExamDao.insertPastExams(exams)
  }

  suspend fun upsertAccessCodesFromCloud(codes: List<AccessCodeEntity>) = withContext(Dispatchers.IO) {
    accessCodeDao.insertAccessCodes(codes)
  }

  fun startRealtimeCloudSync(scope: CoroutineScope) {
    val fs = firestoreRepository ?: return

    // 1. Live stream of all users from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeAllUsers().collect { cloudUsers ->
          if (cloudUsers.isNotEmpty()) {
            cloudUsers.forEach { profile ->
              val cleanId = if (profile.userId.isNotBlank()) profile.userId else profile.id
              if (cleanId.isNotBlank()) {
                val existing = userDao.getUserById(cleanId)
                if (existing == null) {
                  userDao.insertUser(profile.toEntity())
                } else {
                  val updated = existing.copy(
                    name = profile.name.ifBlank { existing.name },
                    role = profile.role.ifBlank { existing.role },
                    department = profile.department.ifBlank { existing.department },
                    studentId = profile.studentId.ifBlank { existing.studentId },
                    isPremium = existing.isPremium || profile.isPremium
                  )
                  userDao.updateUser(updated)
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud users stream error", e)
      }
    }

    // 2. Live stream of announcements from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeAnnouncements().collect { cloudAnnouncements ->
          if (cloudAnnouncements.isNotEmpty()) {
            val entities = cloudAnnouncements.map { it.toEntity() }
            announcementDao.insertAnnouncements(entities)
            announcementDao.deleteAnnouncementsNotIn(entities.map { it.id })
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud announcements stream error", e)
      }
    }

    // 3. Live stream of courses from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeCourses().collect { cloudCourses ->
          if (cloudCourses.isNotEmpty()) {
            val entities = cloudCourses.map { it.toEntity() }
            courseDao.insertCourses(entities)
            courseDao.deleteCoursesNotIn(entities.map { it.id })
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud courses stream error", e)
      }
    }

    // 4. Live stream of questions from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeQuestions().collect { cloudQuestions ->
          if (cloudQuestions.isNotEmpty()) {
            val entities = cloudQuestions.map { it.toEntity() }
            questionDao.insertQuestions(entities)
            questionDao.deleteQuestionsNotIn(entities.map { it.id })
            entities.map { it.courseId }.distinct().forEach { cId ->
              courseDao.recalculateCounts(cId)
            }
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud questions stream error", e)
      }
    }

    // 5. Live stream of revision notes from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeNotes().collect { cloudNotes ->
          if (cloudNotes.isNotEmpty()) {
            val entities = cloudNotes.map { it.toEntity() }
            noteDao.insertNotes(entities)
            noteDao.deleteNotesNotIn(entities.map { it.id })
            entities.map { it.courseId }.distinct().forEach { cId ->
              courseDao.recalculateCounts(cId)
            }
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud notes stream error", e)
      }
    }

    // 6. Live stream of past exams from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observePastExams().collect { cloudExams ->
          if (cloudExams.isNotEmpty()) {
            val entities = cloudExams.map { it.toEntity() }
            pastExamDao.insertPastExams(entities)
            pastExamDao.deletePastExamsNotIn(entities.map { it.id })
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud past exams stream error", e)
      }
    }

    // 7. Live stream of access codes from cloud Firestore to local Room
    scope.launch(Dispatchers.IO) {
      try {
        fs.observeAccessCodes().collect { cloudCodes ->
          if (cloudCodes.isNotEmpty()) {
            val entities = cloudCodes.map { it.toEntity() }
            accessCodeDao.insertAccessCodes(entities)
            accessCodeDao.deleteAccessCodesNotIn(entities.map { it.code })
          }
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud access codes stream error", e)
      }
    }

    // 8. One-time upload of seed curriculum if cloud Firestore is empty
    scope.launch(Dispatchers.IO) {
      try {
        if (fs.isCloudDatabaseEmpty()) {
          seedDatabaseToCloud(fs)
        }
      } catch (e: Exception) {
        Log.e("TtsRepository", "Cloud seed check error", e)
      }
    }
  }

  suspend fun syncFromCloudNow() = withContext(Dispatchers.IO) {
    val fs = firestoreRepository ?: return@withContext
    if (!isNetworkConnected()) return@withContext
    try {
      val courses = fs.fetchLatestCourses().getOrNull()
      if (!courses.isNullOrEmpty()) {
        val entities = courses.map { it.toEntity() }
        courseDao.insertCourses(entities)
        courseDao.deleteCoursesNotIn(entities.map { it.id })
      }
      val questions = fs.fetchLatestQuestions().getOrNull()
      if (!questions.isNullOrEmpty()) {
        val entities = questions.map { it.toEntity() }
        questionDao.insertQuestions(entities)
        questionDao.deleteQuestionsNotIn(entities.map { it.id })
        entities.map { it.courseId }.distinct().forEach { cId ->
          courseDao.recalculateCounts(cId)
        }
      }
      val notes = fs.fetchLatestNotes().getOrNull()
      if (!notes.isNullOrEmpty()) {
        val entities = notes.map { it.toEntity() }
        noteDao.insertNotes(entities)
        noteDao.deleteNotesNotIn(entities.map { it.id })
        entities.map { it.courseId }.distinct().forEach { cId ->
          courseDao.recalculateCounts(cId)
        }
      }
      val exams = fs.fetchLatestPastExams().getOrNull()
      if (!exams.isNullOrEmpty()) {
        val entities = exams.map { it.toEntity() }
        pastExamDao.insertPastExams(entities)
        pastExamDao.deletePastExamsNotIn(entities.map { it.id })
      }
      val announcements = fs.fetchLatestAnnouncements().getOrNull()
      if (!announcements.isNullOrEmpty()) {
        val entities = announcements.map { it.toEntity() }
        announcementDao.insertAnnouncements(entities)
        announcementDao.deleteAnnouncementsNotIn(entities.map { it.id })
      }
    } catch (e: Exception) {
      Log.e("TtsRepository", "Failed explicit cloud sync", e)
    }
  }

  suspend fun seedDatabaseToCloud(fs: FirestoreRepository) = withContext(Dispatchers.IO) {
    try {
      val courses = courseDao.getAllCourses()
      courses.forEach { fs.saveCourse(it) }

      val questions = questionDao.getAllQuestions()
      questions.forEach { fs.saveQuestion(it) }

      SeedData.getNotes().forEach { fs.saveNote(it) }
      SeedData.getPastExams().forEach { fs.savePastExam(it) }
      SeedData.getAccessCodes().forEach { fs.saveAccessCode(it) }

      val welcomeAnn = AnnouncementEntity(
        id = "ann_freshman_welcome",
        title = "Welcome to Freshman Year 2026/27",
        message = "All digitized past exams, practice quizzes, and lecture revision notes are officially available on TTS Academy. Prepare early for your midterm and final exams!",
        category = "ACADEMIC",
        isUrgent = false,
        isPublished = true
      )
      fs.saveAnnouncement(welcomeAnn)
    } catch (e: Exception) {
      Log.e("TtsRepository", "Error seeding database to cloud", e)
    }
  }

  // --- Auth & Users ---
  fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

  suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to sign in. Please connect to the internet."))
    }

    val cleanEmail = email.trim()
    val cleanPassword = password.trim()
    val isAdminEmail = isSystemAdminEmail(cleanEmail)

    if (isAdminEmail) {
      val isPasswordValid = cleanPassword == "Spike@6776" || cleanPassword == "Admin@123"
      if (!isPasswordValid) {
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing == null || existing.password != cleanPassword) {
          return@withContext Result.failure(Exception("Incorrect administrator password."))
        }
      }
      val adminUser = UserEntity(
        id = if (cleanEmail.startsWith("spike", ignoreCase = true)) "admin_1" else "admin_abebe",
        name = if (cleanEmail.startsWith("spike", ignoreCase = true)) "System Administrator" else "Abebe Wotte (Admin)",
        email = cleanEmail,
        password = cleanPassword.ifBlank { "Spike@6776" },
        studentId = if (cleanEmail.startsWith("spike", ignoreCase = true)) "SYS-ADMIN-01" else "SYS-ADMIN-02",
        department = "Academic Affairs",
        role = "ADMIN",
        avatarUrl = "avatar_admin",
        streakDays = 15,
        isPremium = true
      )
      userDao.insertUser(adminUser)
      _currentUser.value = adminUser
      prefDao.setPreference(UserPreferenceEntity("active_user_id", adminUser.id))
      try {
        firestoreRepository?.syncUserToCloud(adminUser)
      } catch (_: Exception) {}
      return@withContext Result.success(adminUser)
    }

    // Require shared backend verification for all registered students
    val fs = firestoreRepository ?: return@withContext Result.failure(Exception("Cloud service unavailable."))
    val cloudResult = fs.getUserByEmail(cleanEmail)
    if (cloudResult.isFailure) {
      return@withContext Result.failure(Exception("Server verification failed. Please check your internet connection."))
    }
    val cloudProfile = cloudResult.getOrNull()
      ?: return@withContext Result.failure(Exception("Account with this email not found. Please register first."))

    if (cloudProfile.passwordHash.isNotBlank() && cloudProfile.passwordHash != cleanPassword) {
      return@withContext Result.failure(Exception("Incorrect password. Please try again."))
    }

    val entity = cloudProfile.toEntity().copy(password = cleanPassword)
    userDao.insertUser(entity)
    _currentUser.value = entity
    prefDao.setPreference(UserPreferenceEntity("active_user_id", entity.id))
    Result.success(entity)
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
      val updated = existing.copy(role = "ADMIN", isPremium = true)
      userDao.updateUser(updated)
      try {
        firestoreRepository?.syncUserToCloud(updated)
      } catch (_: Exception) {}
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
      correctAnswersCount = 0,
      isPremium = true
    )
    userDao.insertUser(newAdmin)
    try {
      firestoreRepository?.syncUserToCloud(newAdmin)
    } catch (_: Exception) {}
    Result.success(newAdmin)
  }

  suspend fun promoteUserByEmail(email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val existing = userDao.getUserByEmail(cleanEmail)
    if (existing != null) {
      userDao.updateUserRole(existing.id, "ADMIN")
      val updated = existing.copy(role = "ADMIN", isPremium = true)
      userDao.updateUser(updated)
      try {
        firestoreRepository?.syncUserToCloud(updated)
      } catch (_: Exception) {}
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
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to register. Please connect to the internet."))
    }
    val fs = firestoreRepository ?: return@withContext Result.failure(Exception("Cloud service unavailable. Please check your connection."))

    val cleanEmail = email.trim()
    val cleanPassword = password.trim()
    val cleanName = name.trim()
    val cleanStudentId = studentId.trim()
    val cleanDepartment = department.trim()

    val isAdminEmail = isSystemAdminEmail(cleanEmail)
    val assignedRole = if (isAdminEmail) "ADMIN" else role

    // Prevent duplicate registrations by querying shared cloud backend
    val existingCloud = fs.getUserByEmail(cleanEmail).getOrNull()
    if (existingCloud != null) {
      if (!isAdminEmail) {
        return@withContext Result.failure(Exception("An account with this email already exists. Please sign in."))
      }
    }

    val newUser = UserEntity(
      id = if (isAdminEmail) (if (cleanEmail.startsWith("spike", ignoreCase = true)) "admin_1" else "admin_abebe") else "user_" + UUID.randomUUID().toString().replace("-", "").take(10),
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
      isPremium = (assignedRole == "ADMIN")
    )

    // Verify and save to Firestore cloud first!
    val syncRes = fs.syncUserToCloud(newUser)
    if (syncRes.isFailure) {
      return@withContext Result.failure(Exception("Server registration failed. Please check your internet connection and try again."))
    }

    // Save locally only after cloud write succeeds
    userDao.insertUser(newUser)
    _currentUser.value = newUser
    prefDao.setPreference(UserPreferenceEntity("active_user_id", newUser.id))
    Result.success(newUser)
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    _currentUser.value = null
    prefDao.setPreference(UserPreferenceEntity("active_user_id", ""))
    _isDeviceUnlocked.value = false
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
    val cleanEmail = email.trim()
    val isAdminEmail = isSystemAdminEmail(cleanEmail)
    val effectiveRole = if (isAdminEmail) "ADMIN" else role

    val existing = userDao.getUserById(uid)
    val user = if (existing != null) {
      val updated = existing.copy(
        name = name.ifBlank { existing.name },
        email = cleanEmail.ifBlank { existing.email },
        role = if (isAdminEmail) "ADMIN" else existing.role,
        isPremium = if (isAdminEmail) true else existing.isPremium
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
        isPremium = (effectiveRole == "ADMIN")
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to save courses to central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.saveCourse(course)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to save course to cloud database.")
    courseDao.insertCourse(course)
  }

  suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to delete courses from central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.deleteCourse(courseId)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to delete course from cloud database.")
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to save questions to central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.saveQuestion(question)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to save question to central database.")
    questionDao.insertQuestion(question)
    courseDao.recalculateCounts(question.courseId)
  }

  suspend fun deleteQuestion(questionId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to delete questions from central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.deleteQuestion(questionId)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to delete question from central database.")
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to save notes to central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.saveNote(note)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to save revision note to central database.")
    noteDao.insertNote(note)
    courseDao.recalculateCounts(note.courseId)
  }

  suspend fun deleteNote(noteId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to delete notes from central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.deleteNote(noteId)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to delete note from central database.")
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to publish past exams to central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.savePastExam(exam)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to save past exam to central database.")
    pastExamDao.insertPastExam(exam)
  }

  suspend fun deletePastExam(examId: String) = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to delete past exams from central database.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.deletePastExam(examId)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to delete past exam from central database.")
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
    // Update live student statistics in cloud Firestore
    try {
      firestoreRepository?.updateUserStats(attempt.userId, attempt.totalQuestions, attempt.correctCount)
    } catch (e: Exception) {
      Log.e("TtsRepository", "Failed to sync user stats to cloud", e)
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to broadcast announcements.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.saveAnnouncement(announcement)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to save announcement to cloud database.")
    announcementDao.insertAnnouncement(announcement)
  }

  suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to delete announcements.")
    }
    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val res = fs.deleteAnnouncement(id)
    if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to delete announcement from cloud database.")
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
    if (!isNetworkConnected()) {
      throw IllegalStateException("Active internet connection required to generate access codes.")
    }
    val admin = _currentUser.value
    if (admin?.role != "ADMIN") {
      throw IllegalStateException("Unauthorized: Only system administrators can generate access codes.")
    }

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

    val fs = firestoreRepository ?: throw IllegalStateException("Cloud repository unavailable.")
    val saveRes = fs.saveAccessCode(entity)
    if (saveRes.isFailure) {
      throw saveRes.exceptionOrNull() ?: Exception("Failed to publish access code to cloud database.")
    }
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
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to validate access code. Please connect to the internet."))
    }
    val cleanCode = rawCode.trim().uppercase()
    if (cleanCode.length != 6) {
      return@withContext Result.failure(Exception("Access code must be exactly 6 characters."))
    }
    val fs = firestoreRepository ?: return@withContext Result.failure(Exception("Backend service unavailable. Please check your connection."))

    // Atomic server verification in Firestore
    val cloudResult = fs.redeemCloudAccessCode(cleanCode, userId, userEmail)
    if (cloudResult.isFailure) {
      val err = cloudResult.exceptionOrNull()
      return@withContext Result.failure(err ?: Exception("Access code validation failed on server."))
    }

    val now = System.currentTimeMillis()
    userDao.updateUserPremium(userId, true, cleanCode, now)
    val localCode = accessCodeDao.getAccessCode(cleanCode)
    if (localCode != null) {
      accessCodeDao.updateAccessCode(
        localCode.copy(
          isRedeemed = true,
          redeemedByUserId = userId,
          redeemedByEmail = userEmail,
          redeemedAt = now
        )
      )
    }

    // Update active user in memory - now unlocks across all devices where this account is used
    _currentUser.value = _currentUser.value?.copy(isPremium = true, activationCode = cleanCode, activatedAt = now)
    Result.success(true)
  }

  suspend fun loginWithAccessCode(rawCode: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to validate access code. Please connect to the internet."))
    }
    val cleanCode = rawCode.trim().uppercase()
    if (cleanCode.length != 6) {
      return@withContext Result.failure(Exception("Access code must be exactly 6 characters."))
    }
    val fs = firestoreRepository ?: return@withContext Result.failure(Exception("Backend service unavailable. Please check your connection."))

    val tempUserId = "student_code_" + UUID.randomUUID().toString().replace("-", "").take(8)
    val tempEmail = "passcode_$cleanCode@ttsacademy.edu"
    val cloudResult = fs.redeemCloudAccessCode(cleanCode, tempUserId, tempEmail)
    if (cloudResult.isFailure) {
      val err = cloudResult.exceptionOrNull()
      return@withContext Result.failure(err ?: Exception("Access code validation failed on server."))
    }

    val now = System.currentTimeMillis()
    val student = UserEntity(
      id = tempUserId,
      name = "Freshman Scholar",
      email = tempEmail,
      studentId = "TTS-" + cleanCode,
      department = "General Freshman Studies",
      role = "STUDENT",
      isPremium = true,
      activationCode = cleanCode,
      activatedAt = now
    )
    userDao.insertUser(student)
    fs.syncUserToCloud(student)

    _currentUser.value = student
    prefDao.setPreference(UserPreferenceEntity("active_user_id", student.id))
    Result.success(student)
  }

  suspend fun unlockDeviceMaterials() = withContext(Dispatchers.IO) {
    // Only premium users can unlock materials
    _currentUser.value?.let { user ->
      if (user.isPremium || user.role == "ADMIN") {
        _isDeviceUnlocked.value = true
      }
    }
  }

  suspend fun deleteAccessCode(code: String) = withContext(Dispatchers.IO) {
    accessCodeDao.deleteAccessCode(code)
  }

  // --- Download & Offline Access System ---
  fun getDownloadedKeysFlow(userId: String): Flow<List<String>> = prefDao.getDownloadedKeysFlow(userId)
  fun getAllDownloadedKeysFlow(): Flow<List<String>> = prefDao.getAllDownloadedKeysFlow()

  suspend fun markResourceDownloaded(
    userId: String,
    resourceType: String,
    resourceId: String,
    courseId: String? = null
  ) = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext
    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_${resourceType}_${resourceId}", "true"))
    if (courseId != null && courseId.isNotBlank()) {
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_COURSE_${courseId}", "true"))
    }
  }

  suspend fun isResourceDownloaded(
    userId: String,
    resourceType: String,
    resourceId: String,
    courseId: String? = null
  ): Boolean = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext false
    if (_currentUser.value?.role == "ADMIN") return@withContext true
    val itemDownloaded = prefDao.getPreference("dl_${userId}_${resourceType}_${resourceId}") == "true"
    if (itemDownloaded) return@withContext true
    if (courseId != null && courseId.isNotBlank()) {
      val courseDownloaded = prefDao.getPreference("dl_${userId}_COURSE_${courseId}") == "true"
      if (courseDownloaded) return@withContext true
    }
    false
  }

  suspend fun downloadCourseForOffline(userId: String, courseId: String): Result<Int> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to download courses for offline study."))
    }
    if (userId.isBlank()) {
      return@withContext Result.failure(Exception("User must be signed in to download content."))
    }

    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_COURSE_${courseId}", "true"))

    val notes = noteDao.getNotesForCourse(courseId)
    notes.forEach { note ->
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_NOTE_${note.id}", "true"))
    }

    val exams = pastExamDao.getPastExamsForCourse(courseId)
    exams.forEach { exam ->
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_EXAM_${exam.id}", "true"))
    }

    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_QUIZ_${courseId}", "true"))
    val topics = topicDao.getTopicsForCourse(courseId)
    topics.forEach { topic ->
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_QUIZ_${topic.id}", "true"))
    }

    val totalCount = 1 + notes.size + exams.size + topics.size
    Result.success(totalCount)
  }

  suspend fun downloadNoteForOffline(userId: String, noteId: String, courseId: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to download lecture notes."))
    }
    if (userId.isBlank()) {
      return@withContext Result.failure(Exception("User must be signed in to download content."))
    }
    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_NOTE_${noteId}", "true"))
    if (courseId != null && courseId.isNotBlank()) {
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_COURSE_${courseId}", "true"))
    }
    Result.success(true)
  }

  suspend fun downloadPastExamForOffline(userId: String, examId: String, courseId: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to download past examination papers."))
    }
    if (userId.isBlank()) {
      return@withContext Result.failure(Exception("User must be signed in to download content."))
    }
    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_EXAM_${examId}", "true"))
    if (courseId != null && courseId.isNotBlank()) {
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_COURSE_${courseId}", "true"))
    }
    Result.success(true)
  }

  suspend fun downloadQuizForOffline(userId: String, quizId: String, courseId: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected()) {
      return@withContext Result.failure(Exception("Active internet connection required to download quizzes."))
    }
    if (userId.isBlank()) {
      return@withContext Result.failure(Exception("User must be signed in to download content."))
    }
    prefDao.setPreference(UserPreferenceEntity("dl_${userId}_QUIZ_${quizId}", "true"))
    if (courseId != null && courseId.isNotBlank()) {
      prefDao.setPreference(UserPreferenceEntity("dl_${userId}_COURSE_${courseId}", "true"))
    }
    Result.success(true)
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
