package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.firestore.model.FirestoreBookmark
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreStudyNote
import com.example.data.firestore.model.FirestoreUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirestoreRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = FirebaseAuth.getInstance()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  suspend fun getOrCreateUserProfile(
    userId: String,
    email: String,
    name: String,
    role: String = "STUDENT"
  ): Result<FirestoreUserProfile> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    require(uid == userId) { "Authenticated user UID does not match requested user profile ID." }
    val docRef = db.collection("users").document(uid)
    try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        val profile = snapshot.toObject(FirestoreUserProfile::class.java)
          ?: FirestoreUserProfile(userId = uid, email = email, name = name, role = role)
        Result.success(profile)
      } else {
        val newProfile = hashMapOf(
          "userId" to uid,
          "email" to email,
          "name" to name.ifBlank { "TTS Student" },
          "role" to role,
          "department" to "General Freshman Studies",
          "enrolledCourses" to listOf("math101", "phys101", "chem101", "cs101"),
          "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(newProfile).await()
        Result.success(
          FirestoreUserProfile(
            userId = uid,
            email = email,
            name = name.ifBlank { "TTS Student" },
            role = role,
            department = "General Freshman Studies",
            enrolledCourses = listOf("math101", "phys101", "chem101", "cs101")
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordQuizAttempt(attempt: FirestoreQuizAttempt): Result<String> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    val attemptId = attempt.id.ifBlank { "attempt_${UUID.randomUUID().toString().replace("-", "")}" }
    val docRef = db.collection("users").document(uid).collection("quiz_attempts").document(attemptId)
    try {
      val payload = hashMapOf(
        "id" to attemptId,
        "userId" to uid,
        "courseId" to attempt.courseId,
        "topicId" to attempt.topicId,
        "score" to attempt.score,
        "totalQuestions" to attempt.totalQuestions,
        "percentage" to attempt.percentage,
        "completedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(attemptId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeQuizAttempts(userId: String): Flow<List<FirestoreQuizAttempt>> = flow {
    val uid = requireUserId()
    require(uid == userId) { "Cross-user data observation not permitted." }
    val path = "users/$uid/quiz_attempts"
    val colRef = db.collection("users").document(uid).collection("quiz_attempts")
    colRef.snapshots()
      .map { snapshot -> snapshot.toObjects(FirestoreQuizAttempt::class.java) }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
        throw e
      }
      .collect { emit(it) }
  }

  suspend fun addBookmark(bookmark: FirestoreBookmark): Result<String> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    val bookmarkId = bookmark.id.ifBlank { "bm_${UUID.randomUUID().toString().replace("-", "")}" }
    val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkId)
    try {
      val payload = hashMapOf(
        "id" to bookmarkId,
        "userId" to uid,
        "questionId" to bookmark.questionId,
        "courseId" to bookmark.courseId,
        "topicId" to bookmark.topicId,
        "savedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(bookmarkId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeBookmark(userId: String, bookmarkId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkId)
    try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeBookmarks(userId: String): Flow<List<FirestoreBookmark>> = flow {
    val uid = requireUserId()
    require(uid == userId) { "Cross-user bookmark observation not permitted." }
    val path = "users/$uid/bookmarks"
    val colRef = db.collection("users").document(uid).collection("bookmarks")
    colRef.snapshots()
      .map { snapshot -> snapshot.toObjects(FirestoreBookmark::class.java) }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
        throw e
      }
      .collect { emit(it) }
  }

  suspend fun createStudyNote(note: FirestoreStudyNote): Result<String> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    val noteId = note.id.ifBlank { "note_${UUID.randomUUID().toString().replace("-", "")}" }
    val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
    try {
      val payload = hashMapOf(
        "id" to noteId,
        "userId" to uid,
        "courseId" to note.courseId,
        "title" to note.title,
        "content" to note.content,
        "tags" to note.tags,
        "createdAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(noteId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateStudyNote(note: FirestoreStudyNote): Result<Unit> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    require(note.id.isNotBlank()) { "Note ID cannot be blank for update." }
    val docRef = db.collection("users").document(uid).collection("notes").document(note.id)
    try {
      val updates = hashMapOf<String, Any>(
        "title" to note.title,
        "content" to note.content,
        "tags" to note.tags,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeStudyNotes(userId: String): Flow<List<FirestoreStudyNote>> = flow {
    val uid = requireUserId()
    require(uid == userId) { "Cross-user note observation not permitted." }
    val path = "users/$uid/notes"
    val colRef = db.collection("users").document(uid).collection("notes")
    colRef.snapshots()
      .map { snapshot -> snapshot.toObjects(FirestoreStudyNote::class.java) }
      .catch { e ->
        if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
        throw e
      }
      .collect { emit(it) }
  }
}
