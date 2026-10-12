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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.model.CourseEntity
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppHeader
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AnnouncementsScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PastExamDetailScreen
import com.example.ui.screens.PastExamsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuizResultScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ReviewAnswersScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
        TtsAcademyApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun TtsAcademyApp(viewModel: MainViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
  val publishedCourses by viewModel.publishedCourses.collectAsStateWithLifecycle()
  val allPublishedNotes by viewModel.allPublishedNotes.collectAsStateWithLifecycle()
  val allPastExams by viewModel.allPastExams.collectAsStateWithLifecycle()
  val announcements by viewModel.announcements.collectAsStateWithLifecycle()

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

  val snackbarHostState = remember { SnackbarHostState() }

  // Handle Toast/Snackbar feedback
  LaunchedEffect(message) {
    message?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessage()
    }
  }

  // Handle system back navigation
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
          unreadAnnouncementsCount = announcements.size,
          onNotificationsClick = { viewModel.navigateTo(Screen.ANNOUNCEMENTS) },
          onSearchClick = { viewModel.navigateTo(Screen.SEARCH) },
          onProfileClick = { viewModel.navigateTo(Screen.PROFILE) }
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
            onCourseClick = { viewModel.selectCourse(it) },
            onNavigate = { viewModel.navigateTo(it) },
            onStartQuickPractice = { viewModel.startCoursePractice(it, false) }
          )
        }

        Screen.COURSES -> {
          CoursesScreen(
            courses = publishedCourses,
            onCourseClick = { viewModel.selectCourse(it) }
          )
        }

        Screen.COURSE_DETAIL -> {
          selectedCourse?.let { course ->
            CourseDetailScreen(
              course = course,
              topics = courseTopics,
              notes = courseNotes,
              pastExams = coursePastExams,
              onBackClick = { viewModel.navigateBack() },
              onStartTopicPractice = { topic, isExamMode ->
                viewModel.startTopicQuiz(topic, course, isExamMode)
              },
              onStartCoursePractice = { crs, isExamMode ->
                viewModel.startCoursePractice(crs, isExamMode)
              },
              onNoteClick = { viewModel.selectNote(it) },
              onPastExamClick = { viewModel.selectPastExam(it) }
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
            onExit = { viewModel.navigateBack() }
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
            onBackClick = { viewModel.navigateBack() }
          )
        }

        Screen.PAST_EXAMS -> {
          PastExamsScreen(
            pastExams = allPastExams,
            courses = publishedCourses,
            onExamClick = { viewModel.selectPastExam(it) }
          )
        }

        Screen.PAST_EXAM_DETAIL -> {
          selectedExam?.let { exam ->
            val course = publishedCourses.find { it.id == exam.courseId }
            PastExamDetailScreen(
              exam = exam,
              course = course,
              onBackClick = { viewModel.navigateBack() },
              onStartExam = { ex, crs -> viewModel.startExamQuiz(ex, crs) },
              onDownloadPaper = {
                viewModel.showMessage("Downloading verified examination paper '${exam.title}.pdf' to offline cache.")
              }
            )
          }
        }

        Screen.NOTES -> {
          NotesScreen(
            notes = allPublishedNotes,
            courses = publishedCourses,
            onNoteClick = { viewModel.selectNote(it) }
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
            onCourseClick = { viewModel.selectCourse(it) },
            onNoteClick = { viewModel.selectNote(it) },
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
                if (note != null) viewModel.selectNote(note)
              } else {
                val crs = publishedCourses.find { it.id == bm.courseId }
                if (crs != null) viewModel.startCoursePractice(crs, false)
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
            onBackClick = { viewModel.navigateBack() }
          )
        }

        Screen.PROFILE -> {
          ProfileScreen(
            user = currentUser,
            allUsers = allUsers,
            currentTheme = appTheme,
            onThemeSelect = { viewModel.setAppTheme(it) },
            onLogin = { email, pass, cb -> viewModel.login(email, pass, cb) },
            onSwitchUser = { viewModel.switchUser(it) },
            onOpenAdminPortal = { viewModel.navigateTo(Screen.ADMIN_PORTAL) },
            onBackClick = { viewModel.navigateBack() }
          )
        }

        Screen.ADMIN_PORTAL -> {
          AdminPortalScreen(
            courses = publishedCourses,
            allUsers = allUsers,
            announcements = announcements,
            onBackClick = { viewModel.navigateBack() },
            onSaveCourse = { viewModel.adminSaveCourse(it) },
            onDeleteCourse = { viewModel.adminDeleteCourse(it) },
            onSaveQuestion = { viewModel.adminSaveQuestion(it) },
            onSaveNote = { viewModel.adminSaveNote(it) },
            onSavePastExam = { viewModel.adminSavePastExam(it) },
            onPublishAnnouncement = { t, m, c, u -> viewModel.adminPublishAnnouncement(t, m, c, u) },
            onResetSeedData = { viewModel.adminResetData() },
            onUpdateUserRole = { userId, role -> viewModel.updateUserRole(userId, role) },
            onCreateAdmin = { name, email, pass, dept -> viewModel.createAdmin(name, email, pass, dept) }
          )
        }

        Screen.AUTH -> {
          // Handled or routed to profile/switch account
          viewModel.navigateTo(Screen.PROFILE)
        }
      }
    }
  }
}
