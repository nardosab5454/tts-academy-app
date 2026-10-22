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
}
