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
    val topic = TopicEntity(
      id = topicId,
      courseId = courseId,
      title = "Limits & Derivatives",
      description = "Section 1",
      orderIndex = 1
    )
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveTopic(topic) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveTopic(topic.copy(title = "Limits & Continuity")) }.isSuccess)

    val question = QuestionEntity(
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
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveQuestion(question) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveQuestion(question.copy(difficulty = "MEDIUM")) }.isSuccess)

    val note = NoteEntity(
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
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveNote(note) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveNote(note.copy(summary = "Updated derivatives")) }.isSuccess)

    val exam = PastExamEntity(
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
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.savePastExam(exam) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.savePastExam(exam.copy(totalQuestions = 2)) }.isSuccess)

    val ann = AnnouncementEntity(
      id = "ANN_TEST_$suffix",
      title = "Cloud Sync Active",
      message = "All admin updates synced.",
      category = "EXAM",
      isUrgent = false,
      isPublished = true
    )
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveAnnouncement(ann) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveAnnouncement(ann.copy(isUrgent = true)) }.isSuccess)

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

    // Verify diagnostic reports write-ready admin access
    val diag = withTimeout(DEFAULT_TIMEOUT_MS) { repository.diagnoseAdminAccess() }
    assertTrue(diag.hasWriteReadyAuth)
    assertEquals("ADMIN", diag.userDocRole)
    assertTrue(diag.isEmailVerified)

    // Verify courses list fetch returns the created course
    val coursesResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.fetchLatestCourses() }
    assertTrue(coursesResult.isSuccess)
    assertTrue(coursesResult.getOrThrow().any { it.id == courseId })
  }

  @Test
  fun student_saveCourse_failsWithPermissionDenied(): Unit = runBlocking {
    val studentUid = signInVerifiedTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.getOrCreateUserProfile(studentUid, ALICE_EMAIL, "Alice Freshman")
      }.isSuccess
    )

    val diag = withTimeout(DEFAULT_TIMEOUT_MS) { repository.diagnoseAdminAccess() }
    assertFalse(diag.hasWriteReadyAuth)
    assertEquals("STUDENT", diag.userDocRole)

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

  @Test
  fun legacyProductionDocs_withExtraFieldsAndNumericCreatedAt_updateCleanly(): Unit = runBlocking {
    val adminUid = signInVerifiedTestUser(ADMIN_EMAIL)
    val studentUid = "legacy_stu_${UUID.randomUUID().toString().take(6)}"
    val suffix = UUID.randomUUID().toString().take(6).uppercase()
    val courseId = "C_LEGACY_$suffix"
    val noteId = "NOTE_LEGACY_$suffix"
    val annId = "ANN_LEGACY_$suffix"

    // Seed legacy documents containing unknown keys and numeric (integer) createdAt via emulator owner REST API
    seedEmulatorDoc(
      collection = "users",
      docId = adminUid,
      fieldsJson = """
        "id": {"stringValue": "$adminUid"},
        "userId": {"stringValue": "$adminUid"},
        "email": {"stringValue": "$ADMIN_EMAIL"},
        "name": {"stringValue": "Legacy Admin"},
        "passwordHash": {"stringValue": "old_hash"},
        "avatarUrl": {"stringValue": "avatar_admin"},
        "streakDays": {"integerValue": "7"},
        "isPremium": {"booleanValue": false},
        "createdAt": {"integerValue": "1710000000000"}
      """.trimIndent()
    )
    seedEmulatorDoc(
      collection = "users",
      docId = studentUid,
      fieldsJson = """
        "id": {"stringValue": "$studentUid"},
        "userId": {"stringValue": "$studentUid"},
        "email": {"stringValue": "legacy_student_$suffix@ttsacademy.edu"},
        "name": {"stringValue": "Legacy Student"},
        "role": {"stringValue": "STUDENT"},
        "isPremium": {"booleanValue": false},
        "avatarUrl": {"stringValue": "avatar_1"},
        "createdAt": {"integerValue": "1710000000000"}
      """.trimIndent()
    )
    seedEmulatorDoc(
      collection = "courses",
      docId = courseId,
      fieldsJson = """
        "id": {"stringValue": "$courseId"},
        "code": {"stringValue": "PHYS $suffix"},
        "title": {"stringValue": "Legacy Physics"},
        "category": {"stringValue": "STEM"},
        "isPublished": {"booleanValue": true},
        "legacyExtraKey": {"stringValue": "drop_me"},
        "createdAt": {"integerValue": "1710000000000"}
      """.trimIndent()
    )
    seedEmulatorDoc(
      collection = "notes",
      docId = noteId,
      fieldsJson = """
        "id": {"stringValue": "$noteId"},
        "courseId": {"stringValue": "$courseId"},
        "topicId": {"stringValue": "T_GEN"},
        "title": {"stringValue": "Legacy Kinematics"},
        "chapterNumber": {"integerValue": "1"},
        "contentMarkdown": {"stringValue": "# Kinematics"},
        "isPublished": {"booleanValue": true},
        "readTimeMinutes": {"integerValue": "15"},
        "createdAt": {"integerValue": "1710000000000"}
      """.trimIndent()
    )
    seedEmulatorDoc(
      collection = "announcements",
      docId = annId,
      fieldsJson = """
        "id": {"stringValue": "$annId"},
        "title": {"stringValue": "Legacy Notice"},
        "content": {"stringValue": "Old notice"},
        "isUrgent": {"booleanValue": false},
        "isPublished": {"booleanValue": true},
        "targetCourseId": {"stringValue": "$courseId"},
        "createdAt": {"integerValue": "1710000000000"}
      """.trimIndent()
    )

    val repository = FirestoreRepository(firestore)

    // 1. Admin profile sync should clean extra keys, convert createdAt to Timestamp, and promote to ADMIN
    val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(adminUid, ADMIN_EMAIL, "Legacy Admin")
    }
    assertTrue("getOrCreateUserProfile failed: ${profileRes.exceptionOrNull()?.message}", profileRes.isSuccess)
    assertEquals("ADMIN", profileRes.getOrThrow().role)
    val adminSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("users").document(adminUid).get().await()
    }
    assertFalse("Legacy extra field avatarUrl should be removed", adminSnap.contains("avatarUrl"))
    assertNotNull("createdAt should be converted to Timestamp", adminSnap.getTimestamp("createdAt"))

    // 2. Updating legacy course, note, and announcement should succeed and strip unknown keys
    val courseRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveCourse(
        CourseEntity(
          id = courseId,
          code = "PHYS $suffix",
          title = "Updated Physics",
          description = "Clean update",
          category = "STEM",
          iconName = "science",
          totalQuestions = 5,
          totalNotes = 1,
          isPublished = true,
          orderIndex = 2
        )
      )
    }
    assertTrue("saveCourse failed on legacy doc: ${courseRes.exceptionOrNull()?.message}", courseRes.isSuccess)

    val noteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveNote(
        NoteEntity(
          id = noteId,
          courseId = courseId,
          topicId = "T_GEN",
          title = "Updated Kinematics",
          chapterNumber = 1,
          summary = "Clean note update",
          contentMarkdown = "# Updated Motion",
          keyFormulas = "v = u + at",
          isPublished = true
        )
      )
    }
    assertTrue("saveNote failed on legacy doc: ${noteRes.exceptionOrNull()?.message}", noteRes.isSuccess)

    val annRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveAnnouncement(
        AnnouncementEntity(
          id = annId,
          title = "Updated Broadcast",
          message = "Clean announcement broadcast",
          category = "GENERAL",
          isUrgent = true,
          isPublished = true
        )
      )
    }
    assertTrue("saveAnnouncement failed on legacy doc: ${annRes.exceptionOrNull()?.message}", annRes.isSuccess)

    // 3. Promoting legacy student doc with extra keys and numeric createdAt should succeed cleanly
    val roleRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.updateUserRole(studentUid, "ADMIN")
    }
    assertTrue("updateUserRole failed on legacy user doc: ${roleRes.exceptionOrNull()?.message}", roleRes.isSuccess)
  }

  private fun seedEmulatorDoc(collection: String, docId: String, fieldsJson: String) {
    val deleteUrl = java.net.URL(
      "http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/$collection/$docId"
    )
    (deleteUrl.openConnection() as java.net.HttpURLConnection).run {
      requestMethod = "DELETE"
      setRequestProperty("Authorization", "Bearer owner")
      responseCode
      disconnect()
    }

    val createUrl = java.net.URL(
      "http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/$collection?documentId=$docId"
    )
    val conn = (createUrl.openConnection() as java.net.HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Authorization", "Bearer owner")
    }
    val payload = """{"fields":{$fieldsJson}}"""
    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
    val status = conn.responseCode
    val errText = if (status !in 200..299) {
      conn.errorStream?.bufferedReader()?.readText() ?: ""
    } else ""
    conn.disconnect()
    check(status in 200..299) { "Failed to seed emulator doc $collection/$docId: HTTP $status $errText" }
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
