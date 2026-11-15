package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.firestore.model.FirestoreAccessCode
import com.example.data.firestore.model.FirestoreAnnouncement
import com.example.data.firestore.model.FirestoreBookmark
import com.example.data.firestore.model.FirestoreCourse
import com.example.data.firestore.model.FirestoreNote
import com.example.data.firestore.model.FirestorePastExam
import com.example.data.firestore.model.FirestoreQuestion
import com.example.data.firestore.model.FirestoreQuizAttempt
import com.example.data.firestore.model.FirestoreStudyNote
import com.example.data.firestore.model.FirestoreTopic
import com.example.data.firestore.model.FirestoreUserProfile
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

  private fun isBootstrappedAdminEmail(email: String): Boolean {
    val clean = email.trim().lowercase()
    return clean == "spikenarrd@gmail.com" || clean == "abebewottee@gmail.com"
  }

  private suspend fun prepareTimestamps(
    docRef: com.google.firebase.firestore.DocumentReference,
    payload: MutableMap<String, Any>
  ) {
    val snapshot = try {
      docRef.get().await()
    } catch (_: Exception) {
      null
    }
    if (snapshot == null || !snapshot.exists() || snapshot.getTimestamp("createdAt") == null) {
      payload["createdAt"] = FieldValue.serverTimestamp()
    }
    payload["updatedAt"] = FieldValue.serverTimestamp()
  }

  private suspend fun isEmailInvitedAdmin(email: String): Boolean {
    val clean = email.trim().lowercase()
    if (clean.isBlank()) return false
    return try {
      db.collection("admin_invites").document(clean).get().await().exists()
    } catch (_: Exception) {
      false
    }
  }

  // --- Users & Profiles ---

  suspend fun getOrCreateUserProfile(
    userId: String,
    email: String,
    name: String,
    role: String = "STUDENT",
    department: String = "General Freshman Studies"
  ): Result<FirestoreUserProfile> = withContext(Dispatchers.IO) {
    val uid = auth.currentUser?.uid ?: userId
    val docRef = db.collection("users").document(uid)
    val cleanEmail = email.trim().ifBlank { auth.currentUser?.email ?: "student@ttsacademy.edu" }
    val cleanName = name.trim().ifBlank { auth.currentUser?.displayName ?: "TTS Student" }
    try {
      val snapshot = docRef.get().await()
      val isVerifiedBootstrapped = isBootstrappedAdminEmail(cleanEmail)
      val isInvited = if (!isVerifiedBootstrapped) isEmailInvitedAdmin(cleanEmail) else false
      val shouldBeAdmin = isVerifiedBootstrapped || isInvited || role.equals("ADMIN", ignoreCase = true)

      if (snapshot.exists()) {
        val existingProfile = snapshot.toFirestoreUserProfile()
        if (shouldBeAdmin && (!existingProfile.role.equals("ADMIN", ignoreCase = true) || !existingProfile.isPremium)) {
          val promoteUpdates = hashMapOf<String, Any>(
            "role" to "ADMIN",
            "isPremium" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          if (snapshot.getTimestamp("createdAt") == null) {
            promoteUpdates["createdAt"] = FieldValue.serverTimestamp()
          }
          try {
            docRef.update(promoteUpdates).await()
          } catch (_: Exception) {}
          Result.success(existingProfile.copy(role = "ADMIN", isPremium = true))
        } else {
          Result.success(existingProfile)
        }
      } else {
        val effectiveRole = if (shouldBeAdmin) "ADMIN" else "STUDENT"
        val effectivePremium = shouldBeAdmin
        val studentCode = if (shouldBeAdmin) "ADM-" + uid.take(6).uppercase() else "STU-" + uid.take(6).uppercase()
        val newProfile = hashMapOf<String, Any>(
          "userId" to uid,
          "email" to cleanEmail,
          "name" to cleanName,
          "role" to effectiveRole,
          "department" to department.ifBlank { "General Freshman Studies" },
          "studentId" to studentCode,
          "isPremium" to effectivePremium,
          "activationCode" to "",
          "questionsAnsweredCount" to 0L,
          "correctAnswersCount" to 0L,
          "enrolledCourses" to listOf("MATH_101", "PHYS_101", "CHEM_101", "CS_101"),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(newProfile).await()
        Result.success(
          FirestoreUserProfile(
            id = uid,
            userId = uid,
            email = cleanEmail,
            name = cleanName,
            role = effectiveRole,
            department = department.ifBlank { "General Freshman Studies" },
            studentId = studentCode,
            isPremium = effectivePremium,
            enrolledCourses = listOf("MATH_101", "PHYS_101", "CHEM_101", "CS_101")
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

  suspend fun syncUserToCloud(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("users").document(user.id)
    try {
      val payload = hashMapOf<String, Any>(
        "userId" to user.id,
        "email" to user.email.trim(),
        "name" to user.name.trim().ifBlank { "TTS Student" },
        "role" to if (user.role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT",
        "department" to user.department,
        "studentId" to user.studentId,
        "isPremium" to (user.isPremium || user.role.equals("ADMIN", ignoreCase = true)),
        "activationCode" to (user.activationCode ?: ""),
        "questionsAnsweredCount" to user.questionsAnsweredCount.toLong().coerceAtLeast(0L),
        "correctAnswersCount" to user.correctAnswersCount.toLong().coerceAtLeast(0L)
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserStats(userId: String, questionsDelta: Int, correctDelta: Int): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("users").document(userId)
    try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val email = auth.currentUser?.email ?: "student@ttsacademy.edu"
        val name = auth.currentUser?.displayName ?: "TTS Student"
        getOrCreateUserProfile(userId, email, name).getOrThrow()
      }
      val payload = hashMapOf<String, Any>(
        "questionsAnsweredCount" to FieldValue.increment(questionsDelta.toLong()),
        "correctAnswersCount" to FieldValue.increment(correctDelta.toLong()),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (snapshot.exists() && snapshot.getTimestamp("createdAt") == null) {
        payload["createdAt"] = FieldValue.serverTimestamp()
      }
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("users").document(userId)
    try {
      val normalizedRole = if (newRole.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        return@withContext Result.failure(IllegalStateException("User profile '$userId' does not exist in cloud database yet."))
      }
      val payload = hashMapOf<String, Any>(
        "role" to normalizedRole,
        "isPremium" to (normalizedRole == "ADMIN"),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (snapshot.getTimestamp("createdAt") == null) {
        payload["createdAt"] = FieldValue.serverTimestamp()
      }
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveAdminInvite(email: String, department: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()
    val docRef = db.collection("admin_invites").document(cleanEmail)
    try {
      val invitedBy = requireUserId()
      val payload = hashMapOf<String, Any>(
        "email" to cleanEmail,
        "department" to department.trim().ifBlank { "Academic Directorate" },
        "invitedBy" to invitedBy
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAdminInvite(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()
    val docRef = db.collection("admin_invites").document(cleanEmail)
    try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getUserByEmail(email: String): Result<FirestoreUserProfile?> = withContext(Dispatchers.IO) {
    try {
      val cleanEmail = email.trim()
      val snapshot = db.collection("users")
        .whereEqualTo("email", cleanEmail)
        .limit(1)
        .get()
        .await()
      val profile = snapshot.documents.firstOrNull()?.toFirestoreUserProfile()
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun isCloudDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
    try {
      val cSnap = db.collection("courses").limit(1).get().await()
      val qSnap = db.collection("questions").limit(1).get().await()
      cSnap.isEmpty || qSnap.isEmpty
    } catch (_: Exception) {
      false
    }
  }

  fun observeAllUsers(): Flow<List<FirestoreUserProfile>> = callbackFlow {
    val collectionRef = db.collection("users")
    val registration = collectionRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, collectionRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreUserProfile() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  // --- Announcements Sync ---

  fun observeAnnouncements(): Flow<List<FirestoreAnnouncement>> = callbackFlow {
    val colRef = db.collection("announcements")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreAnnouncement() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveAnnouncement(announcement: AnnouncementEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("announcements").document(announcement.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to announcement.id,
        "title" to announcement.title,
        "content" to announcement.message,
        "message" to announcement.message,
        "category" to announcement.category,
        "isUrgent" to announcement.isUrgent,
        "isPublished" to announcement.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("announcements").document(id)
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

  // --- Courses Sync ---

  fun observeCourses(): Flow<List<FirestoreCourse>> = callbackFlow {
    val colRef = db.collection("courses")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreCourse() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveCourse(course: CourseEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("courses").document(course.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to course.id,
        "code" to course.code,
        "title" to course.title,
        "description" to course.description,
        "category" to course.category.ifBlank { "STEM" },
        "iconName" to course.iconName,
        "totalQuestions" to course.totalQuestions.toLong(),
        "totalNotes" to course.totalNotes.toLong(),
        "isPublished" to course.isPublished,
        "orderIndex" to course.orderIndex.toLong()
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCourse(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("courses").document(id)
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

  // --- Sections / Topics Sync ---

  fun observeTopics(): Flow<List<FirestoreTopic>> = callbackFlow {
    val colRef = db.collection("topics")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreTopic() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  fun observeTopicsForCourse(courseId: String): Flow<List<FirestoreTopic>> = callbackFlow {
    val colRef = db.collection("topics")
    val registration = colRef.whereEqualTo("courseId", courseId).addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreTopic() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveTopic(topic: TopicEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("topics").document(topic.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to topic.id,
        "courseId" to topic.courseId,
        "title" to topic.title,
        "description" to topic.description,
        "orderIndex" to topic.orderIndex.toLong()
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteTopic(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("topics").document(id)
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

  // --- Questions Sync ---

  fun observeQuestions(): Flow<List<FirestoreQuestion>> = callbackFlow {
    val colRef = db.collection("questions")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreQuestion() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveQuestion(q: QuestionEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("questions").document(q.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to q.id,
        "courseId" to q.courseId,
        "topicId" to q.topicId.ifBlank { "T_GEN" },
        "questionText" to q.questionText,
        "optionA" to q.optionA,
        "optionB" to q.optionB,
        "optionC" to q.optionC,
        "optionD" to q.optionD,
        "correctAnswer" to q.correctAnswer,
        "explanationGeneral" to q.explanationGeneral,
        "explanationA" to q.explanationA,
        "explanationB" to q.explanationB,
        "explanationC" to q.explanationC,
        "explanationD" to q.explanationD,
        "difficulty" to q.difficulty,
        "isPastExam" to q.isPastExam,
        "examYear" to (q.examYear ?: ""),
        "examType" to (q.examType ?: ""),
        "isVerified" to q.isVerified,
        "isPublished" to q.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteQuestion(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("questions").document(id)
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

  // --- Revision Notes Sync ---

  fun observeNotes(): Flow<List<FirestoreNote>> = callbackFlow {
    val colRef = db.collection("notes")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreNote() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveNote(note: NoteEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("notes").document(note.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to note.id,
        "courseId" to note.courseId,
        "topicId" to note.topicId.ifBlank { "T_GEN" },
        "title" to note.title,
        "chapterNumber" to note.chapterNumber.coerceAtLeast(1).toLong(),
        "summary" to note.summary,
        "keyFormulas" to note.keyFormulas,
        "contentMarkdown" to note.contentMarkdown,
        "content" to note.contentMarkdown,
        "isPublished" to note.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteNote(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("notes").document(id)
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

  // --- Past Exams Sync ---

  fun observePastExams(): Flow<List<FirestorePastExam>> = callbackFlow {
    val colRef = db.collection("past_exams")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestorePastExam() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun savePastExam(exam: PastExamEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("past_exams").document(exam.id)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to exam.id,
        "courseId" to exam.courseId,
        "university" to exam.university.ifBlank { "Addis Ababa University (AAU)" },
        "academicYear" to exam.academicYear.ifBlank { "2024" },
        "semester" to exam.semester,
        "examType" to exam.examType.ifBlank { "FINAL" },
        "title" to exam.title,
        "instructions" to exam.instructions,
        "totalQuestions" to exam.totalQuestions.toLong().coerceAtLeast(0L),
        "durationMinutes" to exam.durationMinutes.toLong().coerceIn(1L, 600L),
        "isDigitized" to exam.isDigitized,
        "isVerified" to exam.isVerified,
        "documentSummary" to exam.documentSummary,
        "isPublished" to exam.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePastExam(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("past_exams").document(id)
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

  // --- Access Codes Sync ---

  fun observeAccessCodes(): Flow<List<FirestoreAccessCode>> = callbackFlow {
    val colRef = db.collection("access_codes")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreAccessCode() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveAccessCode(code: AccessCodeEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("access_codes").document(code.code)
    try {
      val adminCreatorId = code.createdByAdminId.ifBlank { auth.currentUser?.uid ?: "admin_1" }
      val payload = hashMapOf<String, Any>(
        "code" to code.code,
        "createdByAdminId" to adminCreatorId,
        "isRedeemed" to code.isRedeemed,
        "redeemedByUserId" to (code.redeemedByUserId ?: ""),
        "redeemedByEmail" to (code.redeemedByEmail ?: ""),
        "note" to code.note,
        "expiresAt" to com.google.firebase.Timestamp(java.util.Date(code.expiresAt))
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun redeemCloudAccessCode(code: String, userId: String, userEmail: String): Result<Boolean> = withContext(Dispatchers.IO) {
    val clean = code.trim().uppercase()
    val effectiveUid = auth.currentUser?.uid ?: userId
    val codeDocRef = db.collection("access_codes").document(clean)
    val userDocRef = db.collection("users").document(effectiveUid)

    try {
      // Ensure user profile exists before running the atomic redemption transaction
      if (!userDocRef.get().await().exists()) {
        val email = userEmail.ifBlank { auth.currentUser?.email ?: "student@ttsacademy.edu" }
        val name = auth.currentUser?.displayName ?: "TTS Student"
        getOrCreateUserProfile(effectiveUid, email, name).getOrThrow()
      }

      db.runTransaction { transaction ->
        val snapshot = transaction.get(codeDocRef)
        val userSnap = transaction.get(userDocRef)
        if (!snapshot.exists()) {
          throw IllegalStateException("Invalid 6-digit access code. Please check your code or request a new one from your administrator.")
        }
        val isRedeemed = snapshot.getBoolean("isRedeemed") ?: false
        if (isRedeemed) {
          throw IllegalStateException("This 6-digit access code has already been redeemed for single-use access.")
        }
        val expiresAt = snapshot.getTimestamp("expiresAt")?.toDate()?.time
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
          throw IllegalStateException("This 6-digit access code has expired. Please request a new code from your administrator.")
        }
        if (!userSnap.exists()) {
          throw IllegalStateException("Student cloud profile not initialized. Please try again.")
        }

        val codeUpdates = hashMapOf<String, Any>(
          "isRedeemed" to true,
          "redeemedByUserId" to effectiveUid,
          "redeemedByEmail" to userEmail,
          "redeemedAt" to FieldValue.serverTimestamp()
        )
        transaction.update(codeDocRef, codeUpdates)

        val userUpdates = hashMapOf<String, Any>(
          "isPremium" to true,
          "activationCode" to clean,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        if (userSnap.getTimestamp("createdAt") == null) {
          userUpdates["createdAt"] = FieldValue.serverTimestamp()
        }
        transaction.update(userDocRef, userUpdates)
        true
      }.await()

      Result.success(true)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, codeDocRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAccessCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("access_codes").document(code)
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

  // --- Student Quiz Attempts & Bookmarks ---

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

  fun observeUserQuizAttempts(userId: String): Flow<List<FirestoreQuizAttempt>> = callbackFlow {
    val colRef = db.collection("users").document(userId).collection("quiz_attempts")
    val listener = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        Log.w("FirestoreRepo", "Transient error observing quiz attempts: ${error.message}")
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val attempts = snapshot.documents.map { it.toFirestoreQuizAttempt() }
        trySend(attempts)
      }
    }
    awaitClose { listener.remove() }
  }

  fun observeQuizAttempts(userId: String): Flow<List<FirestoreQuizAttempt>> = observeUserQuizAttempts(userId)

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

  suspend fun addBookmark(bookmark: FirestoreBookmark): Result<Unit> = withContext(Dispatchers.IO) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmark.id)
    try {
      val payload = hashMapOf(
        "id" to bookmark.id,
        "userId" to uid,
        "questionId" to bookmark.questionId,
        "courseId" to bookmark.courseId,
        "topicId" to bookmark.topicId,
        "savedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeBookmark(bookmarkId: String): Result<Unit> = withContext(Dispatchers.IO) {
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

  suspend fun fetchLatestCourses(): Result<List<FirestoreCourse>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("courses").get().await()
      Result.success(snap.documents.map { it.toFirestoreCourse() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestQuestions(): Result<List<FirestoreQuestion>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("questions").get().await()
      Result.success(snap.documents.map { it.toFirestoreQuestion() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestNotes(): Result<List<FirestoreNote>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("notes").get().await()
      Result.success(snap.documents.map { it.toFirestoreNote() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestPastExams(): Result<List<FirestorePastExam>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("past_exams").get().await()
      Result.success(snap.documents.map { it.toFirestorePastExam() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestTopics(): Result<List<FirestoreTopic>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("topics").get().await()
      Result.success(snap.documents.map { it.toFirestoreTopic() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestAnnouncements(): Result<List<FirestoreAnnouncement>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("announcements").get().await()
      Result.success(snap.documents.map { it.toFirestoreAnnouncement() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchLatestUsers(): Result<List<FirestoreUserProfile>> = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("users").get().await()
      Result.success(snap.documents.map { it.toFirestoreUserProfile() })
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

private fun DocumentSnapshot.toFirestoreCourse(): FirestoreCourse {
  val data = this.data ?: return FirestoreCourse(id = this.id)
  return FirestoreCourse(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    code = data["code"] as? String ?: "",
    title = data["title"] as? String ?: "",
    description = data["description"] as? String ?: "",
    category = data["category"] as? String ?: "STEM",
    iconName = data["iconName"] as? String ?: "calculate",
    totalQuestions = (data["totalQuestions"] as? Number)?.toLong() ?: 0L,
    totalNotes = (data["totalNotes"] as? Number)?.toLong() ?: 0L,
    isPublished = data["isPublished"] as? Boolean ?: true,
    orderIndex = (data["orderIndex"] as? Number)?.toLong() ?: 0L,
    createdAt = data["createdAt"] as? Timestamp,
    updatedAt = data["updatedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreTopic(): FirestoreTopic {
  val data = this.data ?: return FirestoreTopic(id = this.id)
  return FirestoreTopic(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    courseId = data["courseId"] as? String ?: "",
    title = data["title"] as? String ?: "",
    description = data["description"] as? String ?: "",
    orderIndex = (data["orderIndex"] as? Number)?.toLong() ?: 0L,
    createdAt = data["createdAt"] as? Timestamp,
    updatedAt = data["updatedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreQuestion(): FirestoreQuestion {
  val data = this.data ?: return FirestoreQuestion(id = this.id)
  return FirestoreQuestion(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    courseId = data["courseId"] as? String ?: "",
    topicId = data["topicId"] as? String ?: "T_GEN",
    questionText = data["questionText"] as? String ?: "",
    optionA = data["optionA"] as? String ?: "",
    optionB = data["optionB"] as? String ?: "",
    optionC = data["optionC"] as? String ?: "",
    optionD = data["optionD"] as? String ?: "",
    correctAnswer = data["correctAnswer"] as? String ?: "A",
    explanationGeneral = data["explanationGeneral"] as? String ?: "",
    explanationA = data["explanationA"] as? String ?: "",
    explanationB = data["explanationB"] as? String ?: "",
    explanationC = data["explanationC"] as? String ?: "",
    explanationD = data["explanationD"] as? String ?: "",
    difficulty = data["difficulty"] as? String ?: "MEDIUM",
    isPastExam = data["isPastExam"] as? Boolean ?: false,
    examYear = data["examYear"] as? String ?: "",
    examType = data["examType"] as? String ?: "",
    isVerified = data["isVerified"] as? Boolean ?: true,
    isPublished = data["isPublished"] as? Boolean ?: true,
    createdAt = data["createdAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreNote(): FirestoreNote {
  val data = this.data ?: return FirestoreNote(id = this.id)
  val parsedChapter = (data["chapterNumber"] as? Number)?.toLong()
    ?: (data["chapter"] as? Number)?.toLong()
    ?: 1L
  return FirestoreNote(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    courseId = data["courseId"] as? String ?: "",
    topicId = (data["topicId"] as? String)?.ifBlank { "T_GEN" } ?: "T_GEN",
    title = data["title"] as? String ?: "",
    chapterNumber = parsedChapter,
    summary = data["summary"] as? String ?: "",
    keyFormulas = data["keyFormulas"] as? String ?: "",
    contentMarkdown = (data["contentMarkdown"] as? String) ?: (data["content"] as? String) ?: "",
    isPublished = (data["isPublished"] as? Boolean) ?: (data["isPublished"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    createdAt = data["createdAt"] as? Timestamp,
    updatedAt = data["updatedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestorePastExam(): FirestorePastExam {
  val data = this.data ?: return FirestorePastExam(id = this.id)
  return FirestorePastExam(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    courseId = data["courseId"] as? String ?: "",
    university = (data["university"] as? String)?.ifBlank { "Addis Ababa University (AAU)" } ?: "Addis Ababa University (AAU)",
    academicYear = data["academicYear"] as? String ?: "",
    semester = data["semester"] as? String ?: "Semester 1",
    examType = data["examType"] as? String ?: "FINAL",
    title = data["title"] as? String ?: "",
    instructions = data["instructions"] as? String ?: "",
    totalQuestions = (data["totalQuestions"] as? Number)?.toLong() ?: 0L,
    durationMinutes = (data["durationMinutes"] as? Number)?.toLong() ?: 60L,
    isDigitized = (data["isDigitized"] as? Boolean) ?: (data["isDigitized"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    isVerified = (data["isVerified"] as? Boolean) ?: (data["isVerified"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    documentSummary = data["documentSummary"] as? String ?: "",
    isPublished = (data["isPublished"] as? Boolean) ?: (data["isPublished"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    createdAt = data["createdAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreAnnouncement(): FirestoreAnnouncement {
  val data = this.data ?: return FirestoreAnnouncement(id = this.id)
  return FirestoreAnnouncement(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    title = data["title"] as? String ?: "",
    content = (data["content"] as? String) ?: (data["message"] as? String) ?: "",
    category = data["category"] as? String ?: "GENERAL",
    author = data["author"] as? String ?: "TTS Directorate",
    isUrgent = (data["isUrgent"] as? Boolean) ?: (data["isUrgent"]?.toString()?.toBooleanStrictOrNull()) ?: false,
    isPublished = (data["isPublished"] as? Boolean) ?: (data["isPublished"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    createdAt = data["createdAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreAccessCode(): FirestoreAccessCode {
  val data = this.data ?: return FirestoreAccessCode(code = this.id)
  return FirestoreAccessCode(
    code = (data["code"] as? String)?.ifBlank { this.id } ?: this.id,
    createdByAdminId = data["createdByAdminId"] as? String ?: "admin_1",
    isRedeemed = data["isRedeemed"] as? Boolean ?: false,
    redeemedByUserId = data["redeemedByUserId"] as? String ?: "",
    redeemedByEmail = data["redeemedByEmail"] as? String ?: "",
    note = data["note"] as? String ?: "Permanent Academic Pass",
    createdAt = data["createdAt"] as? Timestamp,
    expiresAt = data["expiresAt"] as? Timestamp,
    redeemedAt = data["redeemedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreUserProfile(): FirestoreUserProfile {
  val data = this.data ?: return FirestoreUserProfile(id = this.id, userId = this.id)
  return FirestoreUserProfile(
    id = this.id,
    userId = (data["userId"] as? String)?.ifBlank { this.id } ?: this.id,
    email = data["email"] as? String ?: "",
    name = data["name"] as? String ?: "",
    role = data["role"] as? String ?: "STUDENT",
    department = data["department"] as? String ?: "",
    studentId = data["studentId"] as? String ?: "",
    isPremium = data["isPremium"] as? Boolean ?: false,
    activationCode = data["activationCode"] as? String ?: "",
    passwordHash = "",
    questionsAnsweredCount = (data["questionsAnsweredCount"] as? Number)?.toLong() ?: 0L,
    correctAnswersCount = (data["correctAnswersCount"] as? Number)?.toLong() ?: 0L,
    enrolledCourses = (data["enrolledCourses"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    createdAt = data["createdAt"] as? Timestamp,
    updatedAt = data["updatedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreQuizAttempt(): FirestoreQuizAttempt {
  val data = this.data ?: return FirestoreQuizAttempt(id = this.id)
  return FirestoreQuizAttempt(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    userId = data["userId"] as? String ?: "",
    courseId = data["courseId"] as? String ?: "",
    topicId = data["topicId"] as? String ?: "",
    score = (data["score"] as? Number)?.toLong() ?: 0L,
    totalQuestions = (data["totalQuestions"] as? Number)?.toLong() ?: 0L,
    percentage = (data["percentage"] as? Number)?.toLong() ?: 0L,
    completedAt = data["completedAt"] as? Timestamp
  )
}
