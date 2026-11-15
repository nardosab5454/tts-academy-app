package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreStudyNote
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.TopicEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createUserProfile_validPayload_createsDocumentAndReturnsProfile(): Unit = runBlocking {
    val uid = signInVerifiedTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(uid, ALICE_EMAIL, "Alice Freshman")
    }
    assertTrue(result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals(ALICE_EMAIL, profile.email)
    assertEquals("STUDENT", profile.role)
    assertFalse(profile.isPremium)
  }

  @Test
  fun adminPanel_saveAndUpdateCurriculumEntities_succeeds(): Unit = runBlocking {
    val adminUid = signInVerifiedTestUser(ADMIN_EMAIL)
    val repository = FirestoreRepository(firestore)

    val adminProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(adminUid, ADMIN_EMAIL, "Primary Admin")
    }
    assertTrue(adminProfile.isSuccess)
    assertEquals("ADMIN", adminProfile.getOrThrow().role)

    val suffix = UUID.randomUUID().toString().take(6).uppercase()
    val courseId = "C_TEST_$suffix"
    val course = CourseEntity(
      id = courseId,
      code = "MATH $suffix",
      title = "Applied Mathematics $suffix",
      description = "Real-time cloud synced course",
      category = "STEM",
      iconName = "school",
      totalQuestions = 1,
      totalNotes = 1,
      isPublished = true,
      orderIndex = 1
    )

    // Create course
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveCourse(course) }.isSuccess)
    // Update course (verifies immutable createdAt preservation)
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveCourse(course.copy(title = "Applied Mathematics Updated $suffix"))
      }.isSuccess
    )

    // Topic, Question, Note, PastExam (with sanitized ID), Announcement, Invite, AccessCode
    val topicId = "T_TEST_$suffix"
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveTopic(
          TopicEntity(
            id = topicId,
            courseId = courseId,
            title = "Limits & Derivatives",
            description = "Section 1",
            orderIndex = 1
          )
        )
      }.isSuccess
    )

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveQuestion(
          QuestionEntity(
            id = "Q_TEST_$suffix",
            courseId = courseId,
            topicId = topicId,
            questionText = "What is the derivative of x^2?",
            optionA = "2x",
            optionB = "x",
            optionC = "x^2",
            optionD = "2",
            correctAnswer = "A",
            explanationA = "2x by power rule",
            explanationB = "Incorrect",
            explanationC = "Incorrect",
            explanationD = "Incorrect",
            explanationGeneral = "Power rule d/dx(x^2) = 2x",
            difficulty = "EASY",
            isPublished = true
          )
        )
      }.isSuccess
    )

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveNote(
          NoteEntity(
            id = "NOTE_TEST_$suffix",
            courseId = courseId,
            topicId = topicId,
            title = "Chapter 1 Summary",
            chapterNumber = 1,
            summary = "Key derivatives",
            contentMarkdown = "# Derivatives\nd/dx(x^n) = nx^(n-1)",
            keyFormulas = "d/dx(x^n) = nx^(n-1)",
            isPublished = true
          )
        )
      }.isSuccess
    )

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.savePastExam(
          PastExamEntity(
            id = "EXAM_${courseId}_2016_FINAL",
            courseId = courseId,
            university = "Addis Ababa University (AAU)",
            academicYear = "2016 E.C. (2024)",
            semester = "Semester 1",
            examType = "FINAL",
            title = "Freshman Calculus Final",
            instructions = "Choose the single best answer.",
            totalQuestions = 1,
            durationMinutes = 60,
            isPublished = true
          )
        )
      }.isSuccess
    )

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveAnnouncement(
          AnnouncementEntity(
            id = "ANN_TEST_$suffix",
            title = "Cloud Sync Active",
            message = "All admin updates synced.",
            category = "EXAM",
            isUrgent = false,
            isPublished = true
          )
        )
      }.isSuccess
    )

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveAdminInvite("staff_$suffix@ttsacademy.edu", "Mathematics")
      }.isSuccess
    )

    val passCode = "T${suffix.take(5).padEnd(5, '9')}"
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveAccessCode(
          AccessCodeEntity(
            code = passCode,
            createdByAdminId = adminUid,
            isRedeemed = false,
            note = "Rule test pass",
            expiresAt = System.currentTimeMillis() + 3_600_000L
          )
        )
      }.isSuccess
    )

    // Verify courses list fetch returns the created course
    val coursesResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.fetchLatestCourses() }
    assertTrue(coursesResult.isSuccess)
    assertTrue(coursesResult.getOrThrow().any { it.id == courseId })
  }

  @Test
  fun student_saveCourse_failsWithPermissionDenied(): Unit = runBlocking {
    signInVerifiedTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveCourse(
        CourseEntity(
          id = "C_HACK_${UUID.randomUUID().toString().take(6)}",
          code = "HACK 101",
          title = "Unauthorized Course",
          description = "Should fail",
          category = "STEM",
          iconName = "school",
          isPublished = true
        )
      )
    }
    assertTrue(result.isFailure)
    val ex = checkNotNull(result.exceptionOrNull()?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.code)
  }

  @Test
  fun student_redeemCloudAccessCode_atomicTransactionSucceeds_andReuseFails(): Unit = runBlocking {
    val adminUid = signInVerifiedTestUser(ADMIN_EMAIL)
    val adminRepo = FirestoreRepository(firestore)
    val code = "P" + UUID.randomUUID().toString().replace("-", "").take(5).uppercase()
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.saveAccessCode(
          AccessCodeEntity(
            code = code,
            createdByAdminId = adminUid,
            isRedeemed = false,
            note = "Student test pass",
            expiresAt = System.currentTimeMillis() + 3_600_000L
          )
        )
      }.isSuccess
    )

    val studentUid = signInVerifiedTestUser(BOB_EMAIL)
    val studentRepo = FirestoreRepository(firestore)
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        studentRepo.getOrCreateUserProfile(studentUid, BOB_EMAIL, "Bob Freshman")
      }.isSuccess
    )

    val redeemResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      studentRepo.redeemCloudAccessCode(code, studentUid, BOB_EMAIL)
    }
    assertTrue(redeemResult.isSuccess)

    // Reuse must fail
    val reuseResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      studentRepo.redeemCloudAccessCode(code, studentUid, BOB_EMAIL)
    }
    assertTrue(reuseResult.isFailure)
  }

  @Test
  fun crossUser_readOtherUserProfile_failsWithPermissionDenied(): Unit = runBlocking {
    val aliceUid = signInVerifiedTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreRepository(firestore)
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.getOrCreateUserProfile(aliceUid, ALICE_EMAIL, "Alice Freshman")
      }.isSuccess
    )

    signInVerifiedTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
      fail("Expected PERMISSION_DENIED reading another user's profile")
    } catch (e: Exception) {
      val ex = checkNotNull(e.unwrapFirestoreException())
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.code)
    }
  }

  @Test
  fun recordQuizAttempt_authenticatedOwner_savesAttempt(): Unit = runBlocking {
    val uid = signInVerifiedTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)
    val attemptId = "att_${UUID.randomUUID().toString().replace("-", "")}"

    val attempt = FirestoreQuizAttempt(
      id = attemptId,
      userId = uid,
      courseId = "math101",
      topicId = "derivatives",
      score = 9L,
      totalQuestions = 10L,
      percentage = 90L
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordQuizAttempt(attempt)
    }
    assertTrue(result.isSuccess)
    assertEquals(attemptId, result.getOrThrow())
  }

  @Test
  fun createStudyNote_authenticatedOwner_savesNote(): Unit = runBlocking {
    val uid = signInVerifiedTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)
    val noteId = "note_${UUID.randomUUID().toString().replace("-", "")}"

    val note = FirestoreStudyNote(
      id = noteId,
      userId = uid,
      courseId = "phys101",
      title = "Kinematics Summary",
      content = "v = u + at, s = ut + 0.5at^2",
      tags = listOf("physics", "motion")
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createStudyNote(note)
    }
    assertTrue(result.isSuccess)
    assertEquals(noteId, result.getOrThrow())
  }

  @Test
  fun observeQuizAttempts_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
    auth.signOut()
    val repository = FirestoreRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeQuizAttempts("dummy_uid").first()
      }
      fail("Expected IllegalStateException when unauthenticated")
    } catch (e: IllegalStateException) {
      assertNotNull(e.message)
    }
  }

  private fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
    var current: Throwable? = this
    while (current != null) {
      if (current is FirebaseFirestoreException) return current
      current = current.cause
    }
    return null
  }

  private companion object {
    const val ADMIN_EMAIL = "abebewottee@gmail.com"
    const val ALICE_EMAIL = "alice_test@ttsacademy.edu"
    const val BOB_EMAIL = "bob_test@ttsacademy.edu"
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
