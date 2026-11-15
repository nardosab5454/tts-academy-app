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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

data class CloudSyncReport(
  val coursesCount: Int = 0,
  val topicsCount: Int = 0,
  val questionsCount: Int = 0,
  val notesCount: Int = 0,
  val pastExamsCount: Int = 0,
  val announcementsCount: Int = 0,
  val errors: List<String> = emptyList(),
  val isAuthenticated: Boolean = true
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

  private suspend fun reconcileCoursesFromCloud(entities: List<CourseEntity>) {
    if (entities.isEmpty()) return
    courseDao.insertCourses(entities)
    val validCourseIds = entities.map { it.id }
    courseDao.deleteCoursesNotIn(validCourseIds)
    topicDao.deleteTopicsForCoursesNotIn(validCourseIds)
    questionDao.deleteQuestionsForCoursesNotIn(validCourseIds)
    noteDao.deleteNotesForCoursesNotIn(validCourseIds)
    pastExamDao.deletePastExamsForCoursesNotIn(validCourseIds)
    validCourseIds.forEach { courseDao.recalculateCounts(it) }
  }

  fun startRealtimeCloudSync(scope: CoroutineScope) {
    val fs = firestoreRepository ?: return
    val auth = FirebaseAuth.getInstance()

    // Attach Firestore snapshot listeners only while signed into Firebase Auth
    scope.launch(Dispatchers.IO) {
      currentUser.collectLatest { user ->
        if (user != null && auth.currentUser != null) {
          coroutineScope {
            if (user.role == "ADMIN") {
              launch {
                try {
                  fs.observeAllUsers().collect { cloudUsers ->
                    if (cloudUsers.isNotEmpty()) {
                      cloudUsers.forEach { profile ->
                        val cleanId = if (profile.userId.isNotBlank()) profile.userId else profile.id
                        if (cleanId.isNotBlank()) {
                          val existing = userDao.getUserById(cleanId)
                          val isAdmin = profile.role.equals("ADMIN", ignoreCase = true)
                          val isPrem = profile.isPremium || isAdmin
                          val currentUid = _currentUser.value?.id
                          if (existing == null) {
                            val entity = profile.toEntity().copy(isPremium = isPrem, role = if (isAdmin) "ADMIN" else profile.role)
                            userDao.insertUser(entity)
                            if (currentUid == cleanId) {
                              _currentUser.value = entity
                            }
                          } else {
                            val updated = existing.copy(
                              name = profile.name.ifBlank { existing.name },
                              role = if (isAdmin) "ADMIN" else profile.role.ifBlank { existing.role },
                              department = profile.department.ifBlank { existing.department },
                              studentId = profile.studentId.ifBlank { existing.studentId },
                              isPremium = existing.isPremium || isPrem
                            )
                            userDao.updateUser(updated)
                            if (currentUid == cleanId) {
                              _currentUser.value = updated
                            }
                          }
                        }
                      }
                    }
                  }
                } catch (e: Exception) {
                  Log.e("TtsRepository", "Cloud users stream error", e)
                }
              }
              launch {
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
            }

            // Announcements stream
            launch {
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

            // Courses stream
            launch {
              try {
                fs.observeCourses().collect { cloudCourses ->
                  if (cloudCourses.isNotEmpty()) {
                    reconcileCoursesFromCloud(cloudCourses.map { it.toEntity() })
                  }
                }
              } catch (e: Exception) {
                Log.e("TtsRepository", "Cloud courses stream error", e)
              }
            }

            // Topics stream
            launch {
              try {
                fs.observeTopics().collect { cloudTopics ->
                  if (cloudTopics.isNotEmpty()) {
                    val entities = cloudTopics.map { it.toEntity() }
                    topicDao.insertTopics(entities)
                    topicDao.deleteTopicsNotIn(entities.map { it.id })
                  }
                }
              } catch (e: Exception) {
                Log.e("TtsRepository", "Cloud topics stream error", e)
              }
            }

            // Questions stream
            launch {
              try {
                fs.observeQuestions().collect { cloudQuestions ->
                  if (cloudQuestions.isNotEmpty()) {
                    val entities = cloudQuestions.map { it.toEntity() }
                    questionDao.insertQuestions(entities)
                    questionDao.deleteQuestionsNotIn(entities.map { it.id })
                    courseDao.getAllCourses().forEach { c ->
                      courseDao.recalculateCounts(c.id)
                    }
                  }
                }
              } catch (e: Exception) {
                Log.e("TtsRepository", "Cloud questions stream error", e)
              }
            }

            // Notes stream
            launch {
              try {
                fs.observeNotes().collect { cloudNotes ->
                  if (cloudNotes.isNotEmpty()) {
                    val entities = cloudNotes.map { it.toEntity() }
                    noteDao.insertNotes(entities)
                    noteDao.deleteNotesNotIn(entities.map { it.id })
                    courseDao.getAllCourses().forEach { c ->
                      courseDao.recalculateCounts(c.id)
                    }
                  }
                }
              } catch (e: Exception) {
                Log.e("TtsRepository", "Cloud notes stream error", e)
              }
            }

            // Past exams stream
            launch {
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
          }
        }
      }
    }
  }

  suspend fun syncFromCloudNow(): CloudSyncReport = withContext(Dispatchers.IO) {
    val fs = firestoreRepository ?: return@withContext CloudSyncReport(
      isAuthenticated = false,
      errors = listOf("Cloud repository unavailable.")
    )
    val fbUser = FirebaseAuth.getInstance().currentUser ?: return@withContext CloudSyncReport(
      isAuthenticated = false,
      errors = listOf("Google sign-in session required to sync from cloud.")
    )

    // Refresh signed-in user's own cloud profile so role promotions/invites apply immediately
    fs.getOrCreateUserProfile(
      userId = fbUser.uid,
      email = fbUser.email ?: (_currentUser.value?.email ?: ""),
      name = fbUser.displayName ?: (_currentUser.value?.name ?: "TTS Student")
    ).onSuccess { cloudSelf ->
      val isAdmin = cloudSelf.role.equals("ADMIN", ignoreCase = true)
      val existingSelf = userDao.getUserById(fbUser.uid)
      if (existingSelf != null) {
        val updatedSelf = existingSelf.copy(
          role = if (isAdmin) "ADMIN" else "STUDENT",
          isPremium = existingSelf.isPremium || isAdmin || cloudSelf.isPremium,
          department = cloudSelf.department.ifBlank { existingSelf.department },
          studentId = cloudSelf.studentId.ifBlank { existingSelf.studentId }
        )
        userDao.updateUser(updatedSelf)
        if (_currentUser.value?.id == fbUser.uid) {
          _currentUser.value = updatedSelf
        }
      }
    }

    val errors = mutableListOf<String>()
    var coursesCount = 0
    var topicsCount = 0
    var questionsCount = 0
    var notesCount = 0
    var pastExamsCount = 0
    var announcementsCount = 0

    // Sync Announcements
    fs.fetchLatestAnnouncements().onSuccess { announcements ->
      announcementsCount = announcements.size
      if (announcements.isNotEmpty()) {
        val entities = announcements.map { it.toEntity() }
        announcementDao.insertAnnouncements(entities)
        announcementDao.deleteAnnouncementsNotIn(entities.map { it.id })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Announcements sync error", e)
      errors.add("Announcements: ${e.message ?: "sync failed"}")
    }

    // Sync Courses (reconcile local Room with authoritative cloud courses when non-empty)
    fs.fetchLatestCourses().onSuccess { courses ->
      coursesCount = courses.size
      if (courses.isNotEmpty()) {
        reconcileCoursesFromCloud(courses.map { it.toEntity() })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Courses sync error", e)
      errors.add("Courses: ${e.message ?: "sync failed"}")
    }

    // Sync Sections / Topics
    fs.fetchLatestTopics().onSuccess { topics ->
      topicsCount = topics.size
      if (topics.isNotEmpty()) {
        val entities = topics.map { it.toEntity() }
        topicDao.insertTopics(entities)
        topicDao.deleteTopicsNotIn(entities.map { it.id })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Topics sync error", e)
      errors.add("Topics: ${e.message ?: "sync failed"}")
    }

    // Sync Questions
    fs.fetchLatestQuestions().onSuccess { questions ->
      questionsCount = questions.size
      if (questions.isNotEmpty()) {
        val entities = questions.map { it.toEntity() }
        questionDao.insertQuestions(entities)
        questionDao.deleteQuestionsNotIn(entities.map { it.id })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Questions sync error", e)
      errors.add("Questions: ${e.message ?: "sync failed"}")
    }

    // Sync Revision Notes
    fs.fetchLatestNotes().onSuccess { notes ->
      notesCount = notes.size
      if (notes.isNotEmpty()) {
        val entities = notes.map { it.toEntity() }
        noteDao.insertNotes(entities)
        noteDao.deleteNotesNotIn(entities.map { it.id })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Notes sync error", e)
      errors.add("Notes: ${e.message ?: "sync failed"}")
    }

    // Sync Past Exams
    fs.fetchLatestPastExams().onSuccess { exams ->
      pastExamsCount = exams.size
      if (exams.isNotEmpty()) {
        val entities = exams.map { it.toEntity() }
        pastExamDao.insertPastExams(entities)
        pastExamDao.deletePastExamsNotIn(entities.map { it.id })
      }
    }.onFailure { e ->
      Log.e("TtsRepository", "Past exams sync error", e)
      errors.add("Past exams: ${e.message ?: "sync failed"}")
    }

    // Refresh note/question counters on all synced courses
    courseDao.getAllCourses().forEach { c ->
      courseDao.recalculateCounts(c.id)
    }

    // Sync Users & Roles (only for authenticated admins)
    if (FirebaseAuth.getInstance().currentUser != null && _currentUser.value?.role == "ADMIN") {
      fs.fetchLatestUsers().onSuccess { users ->
        if (users.isNotEmpty()) {
          users.forEach { profile ->
            val cleanId = if (profile.userId.isNotBlank()) profile.userId else profile.id
            if (cleanId.isNotBlank()) {
              val existing = userDao.getUserById(cleanId)
              val isAdmin = profile.role.equals("ADMIN", ignoreCase = true)
              val isPrem = profile.isPremium || isAdmin
              val currentUid = _currentUser.value?.id
              if (existing == null) {
                val entity = profile.toEntity().copy(isPremium = isPrem, role = if (isAdmin) "ADMIN" else profile.role)
                userDao.insertUser(entity)
                if (currentUid == cleanId) {
                  _currentUser.value = entity
                }
              } else {
                val updated = existing.copy(
                  name = profile.name.ifBlank { existing.name },
                  role = if (isAdmin) "ADMIN" else profile.role.ifBlank { existing.role },
                  department = profile.department.ifBlank { existing.department },
                  studentId = profile.studentId.ifBlank { existing.studentId },
                  isPremium = existing.isPremium || isPrem
                )
                userDao.updateUser(updated)
                if (currentUid == cleanId) {
                  _currentUser.value = updated
                }
              }
            }
          }
        }
      }.onFailure { e ->
        Log.e("TtsRepository", "Users sync error", e)
      }
    }

    CloudSyncReport(
      coursesCount = coursesCount,
      topicsCount = topicsCount,
      questionsCount = questionsCount,
      notesCount = notesCount,
      pastExamsCount = pastExamsCount,
      announcementsCount = announcementsCount,
      errors = errors,
      isAuthenticated = true
    )
  }

  suspend fun seedDatabaseToCloud(fs: FirestoreRepository) = withContext(Dispatchers.IO) {
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Sign-In required to publish curriculum data to cloud.")
    }
    try {
      val courses = courseDao.getAllCourses().ifEmpty { SeedData.getCourses() }
      courses.forEach { fs.saveCourse(it).getOrThrow() }

      val topics = topicDao.getAllTopics().ifEmpty { SeedData.getTopics() }
      topics.forEach { fs.saveTopic(it).getOrThrow() }

      val questions = questionDao.getAllQuestions().ifEmpty { SeedData.getQuestions() }
      questions.forEach { fs.saveQuestion(it).getOrThrow() }

      val notes = noteDao.getAllNotes().ifEmpty { SeedData.getNotes() }
      notes.forEach { fs.saveNote(it).getOrThrow() }

      val exams = pastExamDao.getAllPastExams().ifEmpty { SeedData.getPastExams() }
      exams.forEach { fs.savePastExam(it).getOrThrow() }

      val codes = accessCodeDao.getAllAccessCodes().ifEmpty { SeedData.getAccessCodes() }
      codes.forEach { fs.saveAccessCode(it).getOrThrow() }

      val welcomeAnn = AnnouncementEntity(
        id = "ann_freshman_welcome",
        title = "Welcome to Freshman Year 2026/27",
        message = "All digitized past exams, practice quizzes, and lecture revision notes are officially available on TTS Academy. Prepare early for your midterm and final exams!",
        category = "ACADEMIC",
        isUrgent = false,
        isPublished = true
      )
      fs.saveAnnouncement(welcomeAnn).getOrThrow()
    } catch (e: Exception) {
      Log.e("TtsRepository", "Error seeding database to cloud", e)
      throw e
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

  suspend fun setUserRole(userId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
    val isAdmin = newRole.equals("ADMIN", ignoreCase = true)
    val normalizedRole = if (isAdmin) "ADMIN" else "STUDENT"
    val user = userDao.getUserById(userId)
    val updated = if (user != null) {
      user.copy(role = normalizedRole, isPremium = if (isAdmin) true else user.isPremium)
    } else null

    val fs = firestoreRepository
    if (fs != null && FirebaseAuth.getInstance().currentUser != null) {
      val cloudRes = fs.updateUserRole(userId, normalizedRole)
      if (cloudRes.isFailure) {
        return@withContext Result.failure(
          cloudRes.exceptionOrNull() ?: IllegalStateException("Failed to update user role in cloud database.")
        )
      }
      if (!isAdmin && user != null && user.email.isNotBlank()) {
        fs.deleteAdminInvite(user.email)
      } else if (isAdmin && user != null && user.email.isNotBlank()) {
        fs.saveAdminInvite(user.email, user.department)
      }
    }

    userDao.updateUserRole(userId, normalizedRole)
    if (updated != null) {
      userDao.updateUser(updated)
      if (_currentUser.value?.id == userId) {
        _currentUser.value = updated
      }
    }
    Result.success(Unit)
  }

  suspend fun createAdminAccount(
    name: String,
    email: String,
    password: String,
    department: String
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    val cleanDept = department.trim().ifBlank { "Academic Directorate" }
    val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }

    val fs = firestoreRepository
    if (fs != null && FirebaseAuth.getInstance().currentUser != null) {
      // Check if user already exists in Firestore by email
      val existingCloud = fs.getUserByEmail(cleanEmail).getOrNull()
      if (existingCloud != null) {
        val cleanId = existingCloud.userId.ifBlank { existingCloud.id }
        fs.updateUserRole(cleanId, "ADMIN").getOrElse {
          return@withContext Result.failure(it)
        }
        val entity = existingCloud.toEntity().copy(
          role = "ADMIN",
          isPremium = true,
          department = cleanDept
        )
        userDao.insertUser(entity)
        if (_currentUser.value?.id == cleanId) {
          _currentUser.value = entity
        }
        return@withContext Result.success(entity)
      }

      // User hasn't signed in with Google yet -> store pre-authorized Admin Invite
      fs.saveAdminInvite(cleanEmail, cleanDept).getOrElse {
        return@withContext Result.failure(it)
      }
    }

    val existingLocal = userDao.getUserByEmail(cleanEmail)
    if (existingLocal != null) {
      val updated = existingLocal.copy(role = "ADMIN", isPremium = true, department = cleanDept)
      userDao.updateUser(updated)
      return@withContext Result.success(updated)
    }

    val invitedAdmin = UserEntity(
      id = "invite_" + UUID.randomUUID().toString().replace("-", "").take(8),
      name = cleanName,
      email = cleanEmail,
      password = password.trim(),
      studentId = "ADM-INVITE",
      department = cleanDept,
      role = "ADMIN",
      avatarUrl = "avatar_admin",
      streakDays = 1,
      isPremium = true
    )
    userDao.insertUser(invitedAdmin)
    Result.success(invitedAdmin)
  }

  suspend fun promoteUserByEmail(email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }

    val fs = firestoreRepository
    if (fs != null && FirebaseAuth.getInstance().currentUser != null) {
      val cloudUser = fs.getUserByEmail(cleanEmail).getOrNull()
      if (cloudUser != null) {
        val cleanId = cloudUser.userId.ifBlank { cloudUser.id }
        fs.updateUserRole(cleanId, "ADMIN").getOrElse {
          return@withContext Result.failure(it)
        }
        val updated = cloudUser.toEntity().copy(role = "ADMIN", isPremium = true)
        userDao.insertUser(updated)
        if (_currentUser.value?.id == cleanId) {
          _currentUser.value = updated
        }
        return@withContext Result.success(updated)
      } else {
        fs.saveAdminInvite(cleanEmail, "University Administration").getOrElse {
          return@withContext Result.failure(it)
        }
      }
    }

    val existing = userDao.getUserByEmail(cleanEmail)
    if (existing != null) {
      val updated = existing.copy(role = "ADMIN", isPremium = true)
      userDao.updateUser(updated)
      if (_currentUser.value?.id == existing.id) {
        _currentUser.value = updated
      }
      Result.success(updated)
    } else {
      createAdminAccount(
        name = cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
        email = cleanEmail,
        password = "",
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
    department: String = "Computer Science",
    isPremium: Boolean = false,
    studentId: String = ""
  ): UserEntity = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val isAdminEmail = isSystemAdminEmail(cleanEmail) || role.equals("ADMIN", ignoreCase = true)
    val effectiveRole = if (isAdminEmail) "ADMIN" else "STUDENT"
    val effectivePremium = isAdminEmail || isPremium

    val existing = userDao.getUserById(uid)
    val user = if (existing != null) {
      val updated = existing.copy(
        name = name.ifBlank { existing.name },
        email = cleanEmail.ifBlank { existing.email },
        role = if (isAdminEmail) "ADMIN" else effectiveRole,
        department = department.ifBlank { existing.department },
        studentId = studentId.ifBlank { existing.studentId },
        isPremium = effectivePremium || existing.isPremium
      )
      userDao.updateUser(updated)
      updated
    } else {
      val newUser = UserEntity(
        id = uid,
        name = name.ifBlank { if (isAdminEmail) "System Administrator" else "TTS Student" },
        email = cleanEmail.ifBlank { "student@ttsacademy.edu" },
        password = "",
        studentId = studentId.ifBlank { if (isAdminEmail) "SYS-ADMIN-01" else "STU-" + uid.take(6).uppercase() },
        department = department.ifBlank { "General Freshman Studies" },
        role = effectiveRole,
        avatarUrl = if (effectiveRole == "ADMIN") "avatar_admin" else "avatar_1",
        streakDays = 1,
        questionsAnsweredCount = 0,
        correctAnswersCount = 0,
        isPremium = effectivePremium
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
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to save courses to cloud.")
    }
    fs.saveCourse(course).getOrElse { throw it }
    courseDao.insertCourse(course)
  }

  suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete courses from cloud.")
    }
    val courseTopics = topicDao.getTopicsForCourse(courseId)
    fs.deleteCourse(courseId).getOrElse { throw it }
    courseTopics.forEach { topic ->
      try { fs.deleteTopic(topic.id) } catch (_: Exception) {}
    }
    courseDao.deleteCourse(courseId)
    questionDao.deleteQuestionsForCourse(courseId)
    noteDao.deleteNotesForCourse(courseId)
    topicDao.deleteTopicsForCourse(courseId)
    pastExamDao.deletePastExamsForCourse(courseId)
  }

  fun getAllTopicsFlow(): Flow<List<TopicEntity>> = topicDao.getAllTopicsFlow()

  suspend fun saveTopic(topic: TopicEntity) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to save sections to cloud.")
    }
    fs.saveTopic(topic).getOrElse { throw it }
    topicDao.insertTopic(topic)
  }

  suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete sections from cloud.")
    }
    fs.deleteTopic(topicId).getOrElse { throw it }
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
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to save questions to cloud.")
    }
    fs.saveQuestion(question).getOrElse { throw it }
    questionDao.insertQuestion(question)
    courseDao.recalculateCounts(question.courseId)
  }

  suspend fun deleteQuestion(questionId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete questions from cloud.")
    }
    fs.deleteQuestion(questionId).getOrElse { throw it }
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

  suspend fun getNotesForCourse(courseId: String): List<NoteEntity> = withContext(Dispatchers.IO) {
    noteDao.getNotesForCourse(courseId)
  }

  suspend fun searchNotes(query: String): List<NoteEntity> = withContext(Dispatchers.IO) {
    noteDao.searchNotes(query)
  }

  suspend fun saveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to save notes to cloud.")
    }
    fs.saveNote(note).getOrElse { throw it }
    noteDao.insertNote(note)
    courseDao.recalculateCounts(note.courseId)
  }

  suspend fun deleteNote(noteId: String, courseId: String? = null) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete notes from cloud.")
    }
    fs.deleteNote(noteId).getOrElse { throw it }
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
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to save past exams to cloud.")
    }
    fs.savePastExam(exam).getOrElse { throw it }
    pastExamDao.insertPastExam(exam)
  }

  suspend fun deletePastExam(examId: String) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete past exams from cloud.")
    }
    fs.deletePastExam(examId).getOrElse { throw it }
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
    // Update live student statistics in cloud Firestore when authenticated with Firebase
    try {
      if (firestoreRepository != null && FirebaseAuth.getInstance().currentUser != null) {
        firestoreRepository?.updateUserStats(attempt.userId, attempt.totalQuestions, attempt.correctCount)
      }
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
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to broadcast announcements to cloud.")
    }
    fs.saveAnnouncement(announcement).getOrElse { throw it }
    announcementDao.insertAnnouncement(announcement)
  }

  suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
    val fs = firestoreRepository
      ?: throw IllegalStateException("Cloud database repository unavailable.")
    if (FirebaseAuth.getInstance().currentUser == null) {
      throw IllegalStateException("Google Administrator sign-in required to delete announcements from cloud.")
    }
    fs.deleteAnnouncement(id).getOrElse { throw it }
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
    val fbUser = FirebaseAuth.getInstance().currentUser
      ?: throw IllegalStateException("Please sign in with your Google Admin account to publish access codes.")
    val admin = _currentUser.value
    if (admin?.role != "ADMIN") {
      throw IllegalStateException("Unauthorized: Only system administrators can generate access codes.")
    }

    val finalCode = if (!customCode.isNullOrBlank()) {
      val sanitized = customCode.trim().uppercase()
      if (!sanitized.matches(Regex("^[A-Z0-9_-]{6}$"))) {
        throw IllegalArgumentException("Custom access code must be 6 characters using A-Z, 0-9, '-' or '_'.")
      }
      sanitized
    } else {
      generateUnique6CharKey(format)
    }

    val now = System.currentTimeMillis()
    val entity = AccessCodeEntity(
      code = finalCode,
      createdAt = now,
      expiresAt = now + (365L * 24 * 60 * 60 * 1000L), // 1-year valid redemption window
      createdByAdminId = fbUser.uid.ifBlank { adminId },
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
      "ALPHANUMERIC" -> "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
      "NUMERIC" -> "0123456789"
      "SPECIAL" -> "23456789ABCDEFGHJKLMNPQRSTUVWXYZ-_"
      else -> "0123456789"
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
    firestoreRepository?.let { fs ->
      if (FirebaseAuth.getInstance().currentUser != null) {
        val res = fs.deleteAccessCode(code)
        if (res.isFailure) {
          val err = res.exceptionOrNull()?.message ?: "Unknown cloud error"
          throw IllegalStateException("Failed to delete access code from cloud database: $err")
        }
      }
    }
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
    firestoreRepository?.let { seedDatabaseToCloud(it) }
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
