package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreStudyNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createUserProfile_validPayload_createsDocumentAndReturnsProfile(): Unit = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FirestoreRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getOrCreateUserProfile(uid, ALICE_EMAIL, "Alice Freshman")
    }
    assertTrue(result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals(ALICE_EMAIL, profile.email)
  }

  @Test
  fun recordQuizAttempt_authenticatedOwner_savesAttempt(): Unit = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
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
    val uid = signInTestUser(ALICE_EMAIL)
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

  private companion object {
    const val ALICE_EMAIL = "alice_test@ttsacademy.edu"
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
