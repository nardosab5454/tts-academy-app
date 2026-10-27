package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.model.CourseEntity
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppHeader
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.AnnouncementsScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PastExamDetailScreen
import com.example.ui.screens.PastExamsScreen
import com.example.ui.screens.PremiumGateScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuizResultScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ReviewAnswersScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.components.UnlockMaterialsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(
      android.view.WindowManager.LayoutParams.FLAG_SECURE,
      android.view.WindowManager.LayoutParams.FLAG_SECURE
    )
    enableEdgeToEdge()
    setContent {
      val viewModel: MainViewModel = viewModel()
      val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

      val darkTheme = when (appTheme) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = darkTheme) {
        AppContent(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
  var firebaseUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      firebaseUser = auth.currentUser
      auth.currentUser?.let { user ->
        viewModel.syncWithFirebaseUser(user)
      }
    }
    FirebaseAuth.getInstance().addAuthStateListener(listener)
    onDispose {
      FirebaseAuth.getInstance().removeAuthStateListener(listener)
    }
  }

  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

  if (currentUser == null) {
    SignInScreen(
      viewModel = viewModel,
      onAuthSuccess = {
        val user = FirebaseAuth.getInstance().currentUser
        firebaseUser = user
        user?.let { viewModel.syncWithFirebaseUser(it) }
      }
    )
  } else {
    TtsAcademyApp(viewModel = viewModel)
  }
}

@Composable
fun TtsAcademyApp(viewModel: MainViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
  val publishedCourses by viewModel.publishedCourses.collectAsStateWithLifecycle()
  val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
  val allPublishedNotes by viewModel.allPublishedNotes.collectAsStateWithLifecycle()
  val allPastExams by viewModel.allPastExams.collectAsStateWithLifecycle()
  val announcements by viewModel.announcements.collectAsStateWithLifecycle()
  val unreadAnnouncementsCount by viewModel.unreadAnnouncementsCount.collectAsStateWithLifecycle()

  val selectedCourse by viewModel.selectedCourse.collectAsStateWithLifecycle()
  val courseTopics by viewModel.courseTopics.collectAsStateWithLifecycle()
  val courseNotes by viewModel.courseNotes.collectAsStateWithLifecycle()
  val coursePastExams by viewModel.coursePastExams.collectAsStateWithLifecycle()

  val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
  val selectedExam by viewModel.selectedExam.collectAsStateWithLifecycle()
  val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()

  val userAttempts by viewModel.userAttempts.collectAsStateWithLifecycle()
  val userBookmarks by viewModel.userBookmarks.collectAsStateWithLifecycle()
  val weakTopics by viewModel.weakTopics.collectAsStateWithLifecycle()

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val searchCourseResults by viewModel.searchCourseResults.collectAsStateWithLifecycle()
  val searchNoteResults by viewModel.searchNoteResults.collectAsStateWithLifecycle()
  val searchQuestionResults by viewModel.searchQuestionResults.collectAsStateWithLifecycle()

  val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
  val message by viewModel.message.collectAsStateWithLifecycle()
  val allAccessCodes by viewModel.allAccessCodes.collectAsStateWithLifecycle()
  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
  val isDeviceUnlocked by viewModel.isDeviceUnlocked.collectAsStateWithLifecycle()
  val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()
  val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
  val allTopics by viewModel.allTopics.collectAsStateWithLifecycle()
  val downloadedKeys by viewModel.downloadedKeys.collectAsStateWithLifecycle()
  val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  // Access verified through server account status
  val isUnlocked = currentUser?.role == "ADMIN" || currentUser?.isPremium == true
  var showUnlockDialog by remember { mutableStateOf(false) }
  var pendingUnlockAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var pendingTargetTitle by remember { mutableStateOf<String?>(null) }

  fun performLockedAction(
    targetTitle: String,
    resourceType: String = "GENERAL",
    resourceId: String = "",
    courseId: String? = null,
    action: () -> Unit
  ) {
    if (!isUnlocked) {
      if (!isOnline) {
        viewModel.showMessage("Active internet connection required to validate access code. Please connect to the internet.")
      } else {
        pendingTargetTitle = targetTitle
        pendingUnlockAction = action
        showUnlockDialog = true
      }
      return
    }

    val isDownloaded = viewModel.isDownloaded(resourceType, resourceId, courseId)

    if (isOnline) {
      // While online, automatically mark as downloaded for future offline use
      if (resourceId.isNotBlank()) {
        viewModel.markDownloaded(resourceType, resourceId, courseId)
      }
      action()
    } else {
      // Offline mode: allowed if downloaded, if user is admin, or if general screen transition
      if (isDownloaded || currentUser?.role == "ADMIN" || resourceType == "GENERAL") {
        action()
      } else {
        viewModel.showMessage("'$targetTitle' has not been downloaded for offline study yet. Please connect to the internet to download it.")
      }
    }
  }

  // Automatically clear notification badge count when user views Announcements
  LaunchedEffect(currentScreen) {
    if (currentScreen == Screen.ANNOUNCEMENTS) {
      viewModel.markAnnouncementsAsViewed()
    }
  }

  // Handle Toast/Snackbar feedback
  LaunchedEffect(message) {
    message?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessage()
    }
  }

  // Handle system back navigation (disable on Dashboard to exit smoothly)
  BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
    if (!viewModel.navigateBack()) {
      viewModel.navigateTo(Screen.DASHBOARD)
    }
  }

  val isMainTabScreen = currentScreen in listOf(
    Screen.DASHBOARD,
    Screen.COURSES,
    Screen.NOTES,
    Screen.PAST_EXAMS,
    Screen.PROGRESS
  )

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (isMainTabScreen) {
        AppHeader(
          user = currentUser,
          unreadAnnouncementsCount = unreadAnnouncementsCount,
          onNotificationsClick = {
            viewModel.markAnnouncementsAsViewed()
            viewModel.navigateTo(Screen.ANNOUNCEMENTS)
          },
          onSearchClick = { viewModel.navigateTo(Screen.SEARCH) },
          onProfileClick = { viewModel.navigateTo(Screen.PROFILE) },
          onAiTutorClick = { viewModel.navigateTo(Screen.AI_TUTOR) },
          currentTheme = appTheme,
          onToggleTheme = {
            val isDark = when (appTheme) {
              "LIGHT" -> false
              "DARK" -> true
              else -> false
            }
            viewModel.setAppTheme(if (isDark) "LIGHT" else "DARK")
          },
          isOnline = isOnline
        )
      }
    },
    bottomBar = {
      if (isMainTabScreen) {
        AppBottomNavigation(
          currentScreen = currentScreen,
          onNavigate = { viewModel.navigateTo(it) }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        Screen.DASHBOARD -> {
          DashboardScreen(
            user = currentUser,
            courses = publishedCourses,
            announcements = announcements,
            attempts = userAttempts,
            isUnlocked = isUnlocked,
            onUnlockClick = {
              performLockedAction("Academic Materials") {}
            },
            onCourseClick = { course ->
              performLockedAction(course.title, "COURSE", course.id, course.id) {
                viewModel.selectCourse(course)
              }
            },
            onNavigate = { viewModel.navigateTo(it) },
            onStartQuickPractice = { course ->
              performLockedAction("${course.title} Practice Quiz", "QUIZ", course.id, course.id) {
                viewModel.startCoursePractice(course, false)
              }
            },
            onAnnouncementsClick = {
              viewModel.markAnnouncementsAsViewed()
              viewModel.navigateTo(Screen.ANNOUNCEMENTS)
            },
            isOnline = isOnline,
            isCourseDownloaded = { course ->
              viewModel.isDownloaded("COURSE", course.id, course.id)
            }
          )
        }

        Screen.COURSES -> {
          CoursesScreen(
            courses = publishedCourses,
            onCourseClick = { course ->
              performLockedAction(course.title, "COURSE", course.id, course.id) {
                viewModel.selectCourse(course)
              }
            },
            onRefreshCloud = { viewModel.syncNow() },
            isRefreshing = isSyncing
          )
        }

        Screen.COURSE_DETAIL -> {
          selectedCourse?.let { course ->
            val isCourseDownloaded = viewModel.isDownloaded("COURSE", course.id, course.id)
            CourseDetailScreen(
              course = course,
              topics = courseTopics,
              notes = courseNotes,
              pastExams = coursePastExams,
              onBackClick = { viewModel.navigateBack() },
              onStartTopicPractice = { topic, isExamMode ->
                performLockedAction(topic.title, "QUIZ", topic.id, course.id) {
                  viewModel.startTopicQuiz(topic, course, isExamMode)
                }
              },
              onStartCoursePractice = { crs, isExamMode ->
                performLockedAction(crs.title, "QUIZ", crs.id, crs.id) {
                  viewModel.startCoursePractice(crs, isExamMode)
                }
              },
              onNoteClick = { note ->
                performLockedAction(note.title, "NOTE", note.id, note.courseId) {
                  viewModel.selectNote(note)
                }
              },
              onPastExamClick = { exam ->
                performLockedAction(exam.title, "EXAM", exam.id, exam.courseId) {
                  viewModel.selectPastExam(exam)
                }
              },
              isCourseDownloaded = isCourseDownloaded,
              isDownloading = isDownloading,
              onDownloadCourse = {
                viewModel.downloadCourse(course)
              },
              isNoteDownloaded = { note ->
                viewModel.isDownloaded("NOTE", note.id, note.courseId)
              },
              isExamDownloaded = { exam ->
                viewModel.isDownloaded("EXAM", exam.id, exam.courseId)
              },
              onDownloadNote = { note ->
                viewModel.downloadNote(note, course)
              },
              onDownloadExam = { exam ->
                viewModel.downloadPastExam(exam, course)
              }
            )
          }
        }

        Screen.QUIZ -> {
          val currentQuestion = activeQuiz.questions.getOrNull(activeQuiz.currentIndex)
          val isBookmarked = currentQuestion?.let { viewModel.isItemBookmarked(it.id) } ?: false

          QuizScreen(
            state = activeQuiz,
            isBookmarked = isBookmarked,
            onSelectAnswer = { qId, ans -> viewModel.selectAnswer(qId, ans) },
            onSubmitPracticeAnswer = { qId -> viewModel.submitPracticeAnswer(qId) },
            onNext = { viewModel.nextQuestion() },
            onPrevious = { viewModel.previousQuestion() },
            onJumpToIndex = { viewModel.jumpToQuestion(it) },
            onFinishQuiz = { viewModel.finishQuiz() },
            onToggleBookmark = {
              currentQuestion?.let { q ->
                viewModel.toggleBookmark(
                  itemType = "QUESTION",
                  itemId = q.id,
                  courseId = activeQuiz.courseId,
                  courseTitle = activeQuiz.courseTitle,
                  title = q.questionText.take(60)
                )
              }
            },
            onExit = { viewModel.navigateBack() },
            onAskAiTutor = { q, selectedAns ->
              val context = "Question: ${q.questionText}\nOption A: ${q.optionA}\nOption B: ${q.optionB}\nOption C: ${q.optionC}\nOption D: ${q.optionD}\nCorrect Answer: (${q.correctAnswer})" + (if (selectedAns != null) "\nStudent Selected: ($selectedAns)" else "") + "\nOfficial Explanation: ${q.explanationGeneral}"
              val prompt = if (selectedAns != null && selectedAns != q.correctAnswer) {
                "Explain step-by-step why (${q.correctAnswer}) is correct and why my choice ($selectedAns) is incorrect with common student pitfalls."
              } else {
                "Explain this freshman question step-by-step with formulas, intuitive analogies, and key exam tips."
              }
              viewModel.askGeminiTutor(prompt, activeQuiz.courseTitle, context)
              viewModel.navigateTo(Screen.AI_TUTOR)
            }
          )
        }

        Screen.QUIZ_RESULT -> {
          activeQuiz.resultAttempt?.let { attempt ->
            QuizResultScreen(
              attempt = attempt,
              onReviewAnswers = { viewModel.navigateTo(Screen.REVIEW_ANSWERS) },
              onRetakeQuiz = { viewModel.retakeQuiz() },
              onReturnToCourse = { viewModel.navigateTo(Screen.COURSE_DETAIL) }
            )
          }
        }

        Screen.REVIEW_ANSWERS -> {
          ReviewAnswersScreen(
            questions = activeQuiz.questions,
            selectedAnswers = activeQuiz.selectedAnswers,
            onBackClick = { viewModel.navigateBack() },
            onAskAiTutor = { q, selectedAns ->
              val context = "Question: ${q.questionText}\nOption A: ${q.optionA}\nOption B: ${q.optionB}\nOption C: ${q.optionC}\nOption D: ${q.optionD}\nCorrect Answer: (${q.correctAnswer})" + (if (selectedAns != null) "\nStudent Selected: ($selectedAns)" else "") + "\nExplanation: ${q.explanationGeneral}"
              viewModel.askGeminiTutor("Provide deep academic analysis of this exam question.", activeQuiz.courseTitle, context)
              viewModel.navigateTo(Screen.AI_TUTOR)
            }
          )
        }

        Screen.PAST_EXAMS -> {
          PastExamsScreen(
            pastExams = allPastExams,
            courses = publishedCourses,
            isUnlocked = isUnlocked,
            onUnlockClick = {
              performLockedAction("Past Examination Papers") {}
            },
            onExamClick = { exam ->
              performLockedAction(exam.title, "EXAM", exam.id, exam.courseId) {
                viewModel.selectPastExam(exam)
              }
            },
            onDownloadExam = { exam ->
              performLockedAction(exam.title, "EXAM", exam.id, exam.courseId) {
                val course = publishedCourses.find { it.id == exam.courseId }
                viewModel.downloadPastExam(exam, course)
              }
            },
            isExamDownloaded = { exam ->
              viewModel.isDownloaded("EXAM", exam.id, exam.courseId)
            },
            onRefreshCloud = { viewModel.syncNow() },
            isRefreshing = isSyncing
          )
        }

        Screen.PAST_EXAM_DETAIL -> {
          selectedExam?.let { exam ->
            val course = publishedCourses.find { it.id == exam.courseId }
            PastExamDetailScreen(
              exam = exam,
              course = course,
              onBackClick = { viewModel.navigateBack() },
              onStartExam = { ex, crs ->
                performLockedAction(ex.title, "EXAM", ex.id, crs.id) {
                  viewModel.startExamQuiz(ex, crs)
                }
              },
              onDownloadPaper = {
                performLockedAction(exam.title, "EXAM", exam.id, course?.id) {
                  viewModel.downloadExamPdf(context, exam, course)
                }
              }
            )
          }
        }

        Screen.NOTES -> {
          NotesScreen(
            notes = allPublishedNotes,
            courses = publishedCourses,
            isUnlocked = isUnlocked,
            onUnlockClick = {
              performLockedAction("Lecture Notes & Formulas") {}
            },
            onNoteClick = { note ->
              performLockedAction(note.title, "NOTE", note.id, note.courseId) {
                viewModel.selectNote(note)
              }
            },
            isNoteDownloaded = { note ->
              viewModel.isDownloaded("NOTE", note.id, note.courseId)
            },
            onDownloadNote = { note ->
              val course = publishedCourses.find { it.id == note.courseId }
              viewModel.downloadNote(note, course)
            },
            onRefreshCloud = { viewModel.syncNow() },
            isRefreshing = isSyncing
          )
        }

        Screen.NOTE_DETAIL -> {
          selectedNote?.let { note ->
            val course = publishedCourses.find { it.id == note.courseId }
            val isBookmarked = viewModel.isItemBookmarked(note.id)
            NoteDetailScreen(
              note = note,
              course = course,
              isBookmarked = isBookmarked,
              onBackClick = { viewModel.navigateBack() },
              onToggleBookmark = {
                viewModel.toggleBookmark(
                  itemType = "NOTE",
                  itemId = note.id,
                  courseId = note.courseId,
                  courseTitle = course?.title ?: "Note",
                  title = note.title
                )
              },
              onAskAiTutor = { n ->
                val context = "Course: ${course?.title ?: ""}\nTopic: ${n.title}\nKey Formulas: ${n.keyFormulas}\nSummary: ${n.summary}\nFull Content: ${n.contentMarkdown.take(600)}"
                viewModel.askGeminiTutor(
                  prompt = "Provide an in-depth pedagogical explanation of '${n.title}', break down the core formulas, and give practical university exam tips.",
                  courseTitle = course?.title,
                  contextInfo = context
                )
                viewModel.navigateTo(Screen.AI_TUTOR)
              }
            )
          }
        }

        Screen.SEARCH -> {
          SearchScreen(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            courseResults = searchCourseResults,
            noteResults = searchNoteResults,
            questionResults = searchQuestionResults,
            onCourseClick = { course ->
              performLockedAction(course.title) {
                viewModel.selectCourse(course)
              }
            },
            onNoteClick = { note ->
              performLockedAction(note.title) {
                viewModel.selectNote(note)
              }
            },
            onBackClick = { viewModel.navigateBack() }
          )
        }

        Screen.BOOKMARKS -> {
          BookmarksScreen(
            bookmarks = userBookmarks,
            onRemoveBookmark = { viewModel.removeBookmark(it) },
            onOpenItem = { bm ->
              if (bm.itemType == "NOTE") {
                val note = allPublishedNotes.find { it.id == bm.itemId }
                if (note != null) {
                  performLockedAction(note.title) {
                    viewModel.selectNote(note)
                  }
                }
              } else {
                val crs = publishedCourses.find { it.id == bm.courseId }
                if (crs != null) {
                  performLockedAction(crs.title) {
                    viewModel.startCoursePractice(crs, false)
                  }
                }
              }
            },
            onBackClick = { viewModel.navigateBack() }
          )
        }

        Screen.PROGRESS -> {
          ProgressScreen(
            user = currentUser,
            attempts = userAttempts,
            weakTopics = weakTopics,
            courses = publishedCourses,
            onPracticeWeakTopic = { wt ->
              val course = publishedCourses.find { it.id == wt.courseId }
              if (course != null) {
                val topic = courseTopics.find { it.id == wt.topicId }
                  ?: com.example.data.local.model.TopicEntity(wt.topicId, wt.courseId, wt.topicTitle, "", 0)
                viewModel.startTopicQuiz(topic, course, false)
              }
            }
          )
        }

        Screen.ANNOUNCEMENTS -> {
          AnnouncementsScreen(
            announcements = announcements,
            onBackClick = { viewModel.navigateBack() },
            onRefreshCloud = { viewModel.syncNow() },
            isRefreshing = isSyncing
          )
        }

        Screen.PROFILE -> {
          ProfileScreen(
            user = currentUser,
            allUsers = allUsers,
            currentTheme = appTheme,
            accessCodes = allAccessCodes,
            onGenerateCode = { note, format, custom, cb -> viewModel.generateAccessCode(note, format, custom, cb) },
            onDeleteCode = { viewModel.deleteAccessCode(it) },
            onThemeSelect = { viewModel.setAppTheme(it) },
            onUpdateSettings = { dept, goal -> viewModel.updateStudentDepartmentAndGoal(dept, goal) },
            onRedeemAccessCode = { code, cb -> viewModel.redeemAccessCode(code, cb) },
            onLogin = { email, pass, cb -> viewModel.login(email, pass, cb) },
            onSwitchUser = { viewModel.switchUser(it) },
            onOpenAdminPortal = { viewModel.navigateTo(Screen.ADMIN_PORTAL) },
            onBackClick = { viewModel.navigateBack() },
            onSignOut = { viewModel.signOut() },
            isOnline = isOnline
          )
        }

        Screen.ADMIN_PORTAL -> {
          if (currentUser?.role == "ADMIN") {
            AdminPortalScreen(
              courses = allCourses,
              allUsers = allUsers,
              announcements = announcements,
              accessCodes = allAccessCodes,
              questions = allQuestions,
              notes = allNotes,
              topics = allTopics,
              pastExams = allPastExams,
              onBackClick = { viewModel.navigateBack() },
              onSaveCourse = { viewModel.adminSaveCourse(it) },
              onDeleteCourse = { viewModel.adminDeleteCourse(it) },
              onSaveQuestion = { viewModel.adminSaveQuestion(it) },
              onDeleteQuestion = { viewModel.adminDeleteQuestion(it) },
              onSaveNote = { viewModel.adminSaveNote(it) },
              onDeleteNote = { viewModel.adminDeleteNote(it) },
              onSaveTopic = { viewModel.adminSaveTopic(it) },
              onDeleteTopic = { viewModel.adminDeleteTopic(it) },
              onSavePastExam = { viewModel.adminSavePastExam(it) },
              onDeletePastExam = { viewModel.adminDeletePastExam(it) },
              onPublishAnnouncement = { t, m, c, u -> viewModel.adminPublishAnnouncement(t, m, c, u) },
              onResetSeedData = { viewModel.adminResetData() },
              onUpdateUserRole = { userId, role -> viewModel.updateUserRole(userId, role) },
              onCreateAdmin = { name, email, pass, dept -> viewModel.createAdmin(name, email, pass, dept) },
              onPromoteEmail = { email -> viewModel.promoteUserByEmail(email) },
              onDeleteAnnouncement = { viewModel.adminDeleteAnnouncement(it) },
              onGenerateCode = { note, format, customCode, onResult ->
                viewModel.generateAccessCode(note, format, customCode, onResult)
              },
              onDeleteCode = { code -> viewModel.deleteAccessCode(code) },
              onSyncCloud = { viewModel.syncNow() },
              isSyncing = isSyncing
            )
          } else {
            LaunchedEffect(Unit) {
              viewModel.showMessage("Access denied: Administrator privileges required.")
              viewModel.navigateTo(Screen.DASHBOARD)
            }
          }
        }

        Screen.AI_TUTOR -> {
          val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
          val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
          val aiContext by viewModel.aiActiveContext.collectAsStateWithLifecycle()
          val tutorConversation by viewModel.tutorConversation.collectAsStateWithLifecycle()

          AiTutorScreen(
            courses = publishedCourses,
            initialContext = aiContext,
            aiResponse = aiResponse,
            isLoading = isAiLoading,
            conversation = tutorConversation,
            onAskGemini = { prompt, courseTitle, context ->
              viewModel.askGeminiTutor(prompt, courseTitle, context)
            },
            onClearConversation = { viewModel.clearAiTutorState() },
            onBackClick = { viewModel.navigateBack() },
            onShowMessage = { viewModel.showMessage(it) }
          )
        }

        Screen.PREMIUM_GATE -> {
          PremiumGateScreen(
            currentUser = currentUser,
            onRedeemCode = { code, onResult ->
              viewModel.redeemAccessCode(code, onResult)
            },
            onSignOut = {
              viewModel.signOut()
            }
          )
        }

        Screen.AUTH -> {
          // Handled or routed to profile/switch account
          viewModel.navigateTo(Screen.PROFILE)
        }
      }
    }

    if (showUnlockDialog) {
      UnlockMaterialsDialog(
        onDismiss = {
          showUnlockDialog = false
          pendingUnlockAction = null
          pendingTargetTitle = null
        },
        targetTitle = pendingTargetTitle,
        onUnlockWithCode = { code, onResult ->
          if (!isOnline) {
            onResult(false, "Active internet connection required to validate access code. Please connect to the internet.")
          } else {
            viewModel.redeemAccessCode(code, onResult)
          }
        },
        onUnlockSuccess = {
          showUnlockDialog = false
          val action = pendingUnlockAction
          pendingUnlockAction = null
          pendingTargetTitle = null
          action?.invoke()
          viewModel.showMessage("🎉 Academic materials unlocked for your account!")
        }
      )
    }
  }
}
