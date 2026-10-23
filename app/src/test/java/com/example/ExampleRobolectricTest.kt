package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TTS Academy", appName)
  }

  @Test
  fun `verify freshman curriculum seed courses count`() {
    val courses = SeedData.getCourses()
    assertTrue("Curriculum should have 11 to 13 courses", courses.size in 11..13)
    val math = courses.find { it.code == "MATH 101" }
    assertNotNull("MATH 101 should exist", math)
    assertEquals("Calculus I", math?.title)
  }

  @Test
  fun `verify question structure with choice explanations`() {
    val questions = SeedData.getQuestions()
    assertTrue("Questions list should not be empty", questions.isNotEmpty())
    questions.forEach { q ->
      assertTrue("Question text should not be empty", q.questionText.isNotBlank())
      assertTrue("Option A should not be empty", q.optionA.isNotBlank())
      assertTrue("Option B should not be empty", q.optionB.isNotBlank())
      assertTrue("Option C should not be empty", q.optionC.isNotBlank())
      assertTrue("Option D should not be empty", q.optionD.isNotBlank())
      assertTrue("Correct answer should be A, B, C, or D", q.correctAnswer in listOf("A", "B", "C", "D"))
      assertTrue("General explanation must be present", q.explanationGeneral.isNotBlank())
      assertTrue("Explanation for A must be present", q.explanationA.isNotBlank())
      assertTrue("Explanation for B must be present", q.explanationB.isNotBlank())
      assertTrue("Explanation for C must be present", q.explanationC.isNotBlank())
      assertTrue("Explanation for D must be present", q.explanationD.isNotBlank())
    }
  }

  @Test
  fun `verify admin account seed credentials`() {
    val users = SeedData.getDefaultUsers()
    val admin = users.find { it.email == "spikenarrd@gmail.com" }
    assertNotNull("Admin spikenarrd@gmail.com must exist", admin)
    assertEquals("ADMIN", admin?.role)
    assertEquals("Spike@6776", admin?.password)
  }

  @Test
  fun `verify room persistence of study materials and offline quiz progress`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    com.example.data.local.AppDatabase.populateDatabase(db)
    val repo = com.example.data.repository.TtsRepository(db)
    repo.initialize()

    val stats = repo.getOfflineCacheStats("student_1")
    assertTrue("Courses should be cached locally in Room", stats.cachedCoursesCount > 0)
    assertTrue("Questions should be cached locally in Room", stats.cachedQuestionsCount > 0)
    assertTrue("Notes should be cached locally in Room", stats.cachedNotesCount > 0)
    assertTrue("Past exams should be cached locally in Room", stats.cachedPastExamsCount > 0)
    assertTrue("System should report offline ready", stats.isOfflineReady)

    // Record offline quiz attempt
    val testAttempt = com.example.data.local.model.QuizAttemptEntity(
      id = "test_offline_attempt",
      userId = "student_1",
      courseId = "math101",
      courseTitle = "Calculus I",
      topicId = "derivatives",
      topicTitle = "Derivatives",
      examId = null,
      isExamMode = false,
      totalQuestions = 5,
      correctCount = 4,
      incorrectCount = 1,
      scorePercent = 80,
      timeSpentSeconds = 120,
      answersJson = "q1:A:true;q2:B:false"
    )

    repo.recordQuizAttempt(testAttempt)
    val savedAttempts = db.quizAttemptDao().getAttemptsForUser("student_1")
    assertEquals(1, savedAttempts.size)
    assertEquals(80, savedAttempts[0].scorePercent)
    assertEquals("Calculus I", savedAttempts[0].courseTitle)

    val updatedStats = repo.getOfflineCacheStats("student_1")
    assertEquals(1, updatedStats.cachedQuizAttemptsCount)

    db.close()
  }

  @Test
  fun `verify offline access requires resource to be downloaded`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    com.example.data.local.AppDatabase.populateDatabase(db)
    val repo = com.example.data.repository.TtsRepository(db)
    repo.initialize()

    val testStudentId = "test_student_123"

    // 1. Before downloading, resource is not downloaded
    val beforeDownload = repo.isResourceDownloaded(testStudentId, "NOTE", "note_math_01", "math101")
    org.junit.Assert.assertFalse("Resource must NOT be marked downloaded initially", beforeDownload)

    // 2. Mark specific note as downloaded (simulating online first-time access or download)
    repo.markResourceDownloaded(testStudentId, "NOTE", "note_math_01")
    val afterDownload = repo.isResourceDownloaded(testStudentId, "NOTE", "note_math_01")
    assertTrue("Specific note must now be marked downloaded", afterDownload)

    // Other notes remain un-downloaded
    val otherNote = repo.isResourceDownloaded(testStudentId, "NOTE", "note_phys_01")
    org.junit.Assert.assertFalse("Other note should remain un-downloaded", otherNote)

    // 3. Mark course as downloaded
    repo.markResourceDownloaded(testStudentId, "COURSE", "phys101", "phys101")
    val courseItem = repo.isResourceDownloaded(testStudentId, "NOTE", "note_phys_01", "phys101")
    assertTrue("Item belonging to downloaded course must be accessible offline", courseItem)

    db.close()
  }

  @Test
  fun `verify login and access code require active internet connection`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    com.example.data.local.AppDatabase.populateDatabase(db)
    val repo = com.example.data.repository.TtsRepository(db)
    repo.initialize()

    // Offline login should fail with explicit internet requirement message
    val loginRes = repo.login("student@ttsacademy.edu", "student123")
    assertTrue("Login while offline must fail", loginRes.isFailure)
    assertTrue("Error message must state internet is required",
      loginRes.exceptionOrNull()?.message?.contains("internet", ignoreCase = true) == true)

    // Offline code redemption must fail
    val redeemRes = repo.redeemAccessCode("stu_1", "stu_1@tts.edu", "739104")
    assertTrue("Code redemption while offline must fail", redeemRes.isFailure)
    assertTrue("Error message must state internet is required",
      redeemRes.exceptionOrNull()?.message?.contains("internet", ignoreCase = true) == true)

    db.close()
  }

  @Test
  fun `verify course and section hierarchy management and curriculum synchronization`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    com.example.data.local.AppDatabase.populateDatabase(db)
    val repo = com.example.data.repository.TtsRepository(db)
    repo.initialize()

    // 1. Admin creates a new course
    val newCourse = com.example.data.local.model.CourseEntity(
      id = "COSC_101",
      code = "COSC 101",
      title = "Introduction to Computer Science",
      description = "Algorithms, programming paradigms, and data structures.",
      category = "COMPUTING",
      iconName = "code",
      totalQuestions = 0,
      totalNotes = 0,
      isPublished = true,
      orderIndex = 14
    )
    repo.saveCourse(newCourse)

    val retrievedCourse = repo.getCourseById("COSC_101")
    assertNotNull("New course must be persisted", retrievedCourse)
    assertEquals("Introduction to Computer Science", retrievedCourse?.title)

    // 2. Admin creates two sections (topics) within the course
    val section1 = com.example.data.local.model.TopicEntity(
      id = "SEC_ALGO",
      courseId = "COSC_101",
      title = "Algorithms & Complexity",
      description = "Big O notation and sorting algorithms",
      orderIndex = 1
    )
    val section2 = com.example.data.local.model.TopicEntity(
      id = "SEC_DATA",
      courseId = "COSC_101",
      title = "Data Structures",
      description = "Arrays, Linked Lists, Trees, and Hash Maps",
      orderIndex = 2
    )
    repo.saveTopic(section1)
    repo.saveTopic(section2)

    val courseSections = repo.getTopicsForCourse("COSC_101")
    assertEquals("Course must have 2 sections", 2, courseSections.size)
    assertTrue("Algorithms section exists", courseSections.any { it.id == "SEC_ALGO" })
    assertTrue("Data Structures section exists", courseSections.any { it.id == "SEC_DATA" })

    // 3. Admin creates question and note inside section 1
    val question = com.example.data.local.model.QuestionEntity(
      id = "Q_ALGO_1",
      courseId = "COSC_101",
      topicId = "SEC_ALGO",
      questionText = "What is the worst-case time complexity of QuickSort?",
      optionA = "O(n log n)",
      optionB = "O(n^2)",
      optionC = "O(n)",
      optionD = "O(1)",
      correctAnswer = "B",
      explanationGeneral = "QuickSort exhibits O(n^2) when poor pivot choices occur.",
      explanationA = "O(n log n) is best and average case.",
      explanationB = "O(n^2) is worst-case.",
      explanationC = "O(n) is linear scan.",
      explanationD = "O(1) is constant time.",
      difficulty = "MEDIUM",
      isPastExam = false,
      isVerified = true,
      isPublished = true
    )
    repo.saveQuestion(question)

    val note = com.example.data.local.model.NoteEntity(
      id = "NOTE_ALGO_1",
      courseId = "COSC_101",
      topicId = "SEC_ALGO",
      title = "Big-O Asymptotic Analysis",
      summary = "Foundations of computational complexity",
      contentMarkdown = "Big-O gives an upper bound on growth rate.",
      keyFormulas = "T(n) <= c * f(n)",
      readTimeMinutes = 8,
      isPublished = true
    )
    repo.saveNote(note)

    // Verify retrieval under course
    val savedQuestions = repo.getQuestionsForCourse("COSC_101")
    assertEquals(1, savedQuestions.size)
    assertEquals("SEC_ALGO", savedQuestions[0].topicId)

    val savedNotes = repo.getNotesForCourse("COSC_101")
    assertEquals(1, savedNotes.size)
    assertEquals("SEC_ALGO", savedNotes[0].topicId)

    // 4. Update section title
    val updatedSection1 = section1.copy(title = "Algorithms & Time Complexity")
    repo.saveTopic(updatedSection1)
    val refreshedSections = repo.getTopicsForCourse("COSC_101")
    assertEquals("Algorithms & Time Complexity", refreshedSections.find { it.id == "SEC_ALGO" }?.title)

    // 5. Delete section 2
    repo.deleteTopic("SEC_DATA")
    val sectionsAfterDelete = repo.getTopicsForCourse("COSC_101")
    assertEquals(1, sectionsAfterDelete.size)
    org.junit.Assert.assertNull(sectionsAfterDelete.find { it.id == "SEC_DATA" })

    // 6. Delete course and ensure cleanup
    repo.deleteCourse("COSC_101")
    org.junit.Assert.assertNull("Deleted course must no longer exist", repo.getCourseById("COSC_101"))
    val remainingSections = repo.getTopicsForCourse("COSC_101")
    assertEquals(0, remainingSections.size)

    db.close()
  }
}

