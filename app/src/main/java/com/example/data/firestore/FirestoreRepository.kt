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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class CloudAdminDiagnostic(
  val signedInUid: String? = null,
  val signedInEmail: String? = null,
  val isEmailVerified: Boolean = false,
  val databaseId: String = "",
  val userDocExists: Boolean = false,
  val userDocRole: String = "UNKNOWN",
  val isBootstrappedEmail: Boolean = false,
  val isInvitedEmail: Boolean = false,
  val canReadCoursesFromServer: Boolean = false,
  val hasWriteReadyAuth: Boolean = false,
  val diagnosticMessage: String = ""
)

class FirestoreRepository(private val db: FirebaseFirestore) {

  private var configuredDatabaseId: String = "default"

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  ) {
    configuredDatabaseId = context.applicationContext.getString(R.string.firestore_database_id)
  }

  private val auth = FirebaseAuth.getInstance()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  private fun isBootstrappedAdminEmail(email: String): Boolean {
    val clean = email.trim().lowercase()
    return clean == "spikenarrd@gmail.com" || clean == "abebewottee@gmail.com"
  }

  private fun sanitizeDocId(rawId: String, fallbackPrefix: String): String {
    val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").take(128)
    return if (cleaned.isNotBlank() && cleaned.any { it.isLetterOrDigit() }) {
      cleaned
    } else {
      "${fallbackPrefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    }
  }

  private fun formatCloudError(
    e: Exception,
    operationType: OperationType,
    path: String
  ): Exception {
    handleFirestoreError(e, operationType, path)
    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
      val fbUser = auth.currentUser
      val email = fbUser?.email ?: "not signed in"
      val verified = fbUser?.isEmailVerified == true
      val reason = when {
        fbUser == null -> "Firebase session is missing. Please sign out and sign in with Google."
        !verified -> "Google account ($email) email is not verified in auth token."
        else -> "Firestore rejected ${operationType.value} on '$path' for $email (uid=${fbUser.uid}). Tap Refresh Permissions or ensure this email has ADMIN role in cloud."
      }
      return IllegalStateException("Missing or insufficient permissions: $reason", e)
    }
    return e
  }

  private suspend fun prepareTimestamps(
    docRef: com.google.firebase.firestore.DocumentReference,
    payload: MutableMap<String, Any>
  ) {
    val snapshot = try {
      docRef.get(Source.SERVER).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        throw e
      }
      try {
        docRef.get(Source.DEFAULT).await()
      } catch (_: Exception) {
        null
      }
    } catch (_: Exception) {
      try {
        docRef.get(Source.DEFAULT).await()
      } catch (_: Exception) {
        null
      }
    }
    if (snapshot == null || !snapshot.exists() || snapshot.getTimestamp("createdAt") == null) {
      payload["createdAt"] = FieldValue.serverTimestamp()
    }
    payload["updatedAt"] = FieldValue.serverTimestamp()
  }

  private suspend fun isEmailInvitedAdmin(email: String): Boolean {
    val clean = email.trim().lowercase()
    if (clean.isBlank()) return false
    val docRef = db.collection("admin_invites").document(clean)
    return try {
      docRef.get(Source.SERVER).await().exists()
    } catch (_: Exception) {
      try {
        docRef.get(Source.DEFAULT).await().exists()
      } catch (_: Exception) {
        false
      }
    }
  }

  suspend fun diagnoseAdminAccess(): CloudAdminDiagnostic = withContext(Dispatchers.IO) {
    val fbUser = auth.currentUser
    if (fbUser == null) {
      return@withContext CloudAdminDiagnostic(
        databaseId = configuredDatabaseId,
        diagnosticMessage = "Not signed into Firebase Auth. Sign in with your Google Admin account."
      )
    }
    val uid = fbUser.uid
    val email = fbUser.email?.trim() ?: ""
    val verified = fbUser.isEmailVerified
    val bootstrapped = isBootstrappedAdminEmail(email)
    val invited = if (!bootstrapped && email.isNotBlank()) isEmailInvitedAdmin(email) else false

    var userDocExists = false
    var userDocRole = "NONE"
    var canReadCourses = false
    var serverProbeError: String? = null

    try {
      val userSnap = db.collection("users").document(uid).get(Source.SERVER).await()
      userDocExists = userSnap.exists()
      userDocRole = userSnap.getString("role") ?: if (userDocExists) "MISSING_ROLE" else "NONE"
    } catch (e: Exception) {
      serverProbeError = e.message
    }

    try {
      db.collection("courses").limit(1).get(Source.SERVER).await()
      canReadCourses = true
    } catch (e: Exception) {
      if (serverProbeError == null) serverProbeError = e.message
    }

    val writeReady = verified && (bootstrapped || invited || userDocRole.equals("ADMIN", ignoreCase = true))
    val msg = when {
      serverProbeError != null && !canReadCourses -> "Server check failed: $serverProbeError"
      !verified -> "Google email ($email) is not verified in token."
      writeReady -> "Cloud admin verified ($email • DB role=$userDocRole${if (bootstrapped) " • Bootstrapped" else if (invited) " • Invited" else ""})."
      else -> "Signed in as $email, but cloud role is '$userDocRole' (not ADMIN)."
    }

    CloudAdminDiagnostic(
      signedInUid = uid,
      signedInEmail = email,
      isEmailVerified = verified,
      databaseId = configuredDatabaseId,
      userDocExists = userDocExists,
      userDocRole = userDocRole,
      isBootstrappedEmail = bootstrapped,
      isInvitedEmail = invited,
      canReadCoursesFromServer = canReadCourses,
      hasWriteReadyAuth = writeReady,
      diagnosticMessage = msg
    )
  }

  // --- Users & Profiles ---

  suspend fun getOrCreateUserProfile(
    userId: String,
    email: String,
    name: String,
    role: String = "STUDENT",
    department: String = "General Freshman Studies"
  ): Result<FirestoreUserProfile> = withContext(Dispatchers.IO) {
    val currentFbUser = auth.currentUser
    val uid = sanitizeDocId(currentFbUser?.uid ?: userId, "user")
    val docRef = db.collection("users").document(uid)
    val cleanEmail = (currentFbUser?.email ?: email).trim().ifBlank { "student@ttsacademy.edu" }.take(150)
    val cleanName = name.trim().ifBlank { currentFbUser?.displayName ?: "TTS Student" }.take(100)
    val cleanDept = department.trim().ifBlank { "General Freshman Studies" }.take(150)

    try {
      val snapshot = try {
        docRef.get(Source.SERVER).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        docRef.get(Source.DEFAULT).await()
      }

      val isVerifiedBootstrapped = isBootstrappedAdminEmail(cleanEmail) && (currentFbUser?.isEmailVerified != false)
      val isInvited = if (!isVerifiedBootstrapped) isEmailInvitedAdmin(cleanEmail) else false
      val shouldBeAdmin = isVerifiedBootstrapped || isInvited || role.equals("ADMIN", ignoreCase = true)

      if (snapshot.exists()) {
        val existingProfile = snapshot.toFirestoreUserProfile()
        val effectiveRole = if (shouldBeAdmin || existingProfile.role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
        val effectivePremium = existingProfile.isPremium || effectiveRole == "ADMIN"
        val missingRequiredFields =
          snapshot.getString("userId").isNullOrBlank() ||
          snapshot.getString("email").isNullOrBlank() ||
          snapshot.getString("name").isNullOrBlank() ||
          snapshot.getString("role").isNullOrBlank() ||
          snapshot.getBoolean("isPremium") == null ||
          snapshot.getTimestamp("createdAt") == null

        val needsRolePromotion = effectiveRole == "ADMIN" &&
          (!existingProfile.role.equals("ADMIN", ignoreCase = true) || !existingProfile.isPremium)

        if (needsRolePromotion || missingRequiredFields) {
          val promoteUpdates = hashMapOf<String, Any>(
            "userId" to uid,
            "email" to (existingProfile.email.ifBlank { cleanEmail }).take(150),
            "name" to (existingProfile.name.ifBlank { cleanName }).take(100),
            "role" to effectiveRole,
            "isPremium" to effectivePremium,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          if (snapshot.getTimestamp("createdAt") == null) {
            promoteUpdates["createdAt"] = FieldValue.serverTimestamp()
          }
          docRef.set(promoteUpdates, SetOptions.merge()).await()
          Result.success(
            existingProfile.copy(
              userId = uid,
              email = existingProfile.email.ifBlank { cleanEmail },
              name = existingProfile.name.ifBlank { cleanName },
              role = effectiveRole,
              isPremium = effectivePremium
            )
          )
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
          "department" to cleanDept,
          "studentId" to studentCode.take(50),
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
            department = cleanDept,
            studentId = studentCode,
            isPremium = effectivePremium,
            enrolledCourses = listOf("MATH_101", "PHYS_101", "CHEM_101", "CS_101")
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.GET, docRef.path))
    }
  }

  suspend fun syncUserToCloud(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
    val uid = sanitizeDocId(user.id, "user")
    val docRef = db.collection("users").document(uid)
    try {
      val normalizedRole = if (user.role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
      val payload = hashMapOf<String, Any>(
        "userId" to uid,
        "email" to user.email.trim().ifBlank { "student@ttsacademy.edu" }.take(150),
        "name" to user.name.trim().ifBlank { "TTS Student" }.take(100),
        "role" to normalizedRole,
        "department" to user.department.trim().take(150),
        "studentId" to user.studentId.trim().take(50),
        "isPremium" to (user.isPremium || normalizedRole == "ADMIN"),
        "activationCode" to (user.activationCode ?: "").trim().take(20),
        "questionsAnsweredCount" to user.questionsAnsweredCount.toLong().coerceIn(0L, 1_000_000L),
        "correctAnswersCount" to user.correctAnswersCount.toLong().coerceIn(0L, 1_000_000L)
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
    }
  }

  suspend fun updateUserStats(userId: String, questionsDelta: Int, correctDelta: Int): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("users").document(userId)
    try {
      val snapshot = docRef.get(Source.SERVER).await()
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
      Result.failure(formatCloudError(e, OperationType.UPDATE, docRef.path))
    }
  }

  suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
    val docRef = db.collection("users").document(userId)
    try {
      val normalizedRole = if (newRole.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
      val snapshot = docRef.get(Source.SERVER).await()
      if (!snapshot.exists()) {
        return@withContext Result.failure(IllegalStateException("User profile '$userId' does not exist in cloud database yet."))
      }
      val payload = hashMapOf<String, Any>(
        "userId" to userId,
        "role" to normalizedRole,
        "isPremium" to (normalizedRole == "ADMIN"),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (snapshot.getString("email").isNullOrBlank()) {
        payload["email"] = "student@ttsacademy.edu"
      }
      if (snapshot.getString("name").isNullOrBlank()) {
        payload["name"] = "TTS Member"
      }
      if (snapshot.getTimestamp("createdAt") == null) {
        payload["createdAt"] = FieldValue.serverTimestamp()
      }
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.UPDATE, docRef.path))
    }
  }

  suspend fun saveAdminInvite(email: String, department: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase().take(150)
    val docRef = db.collection("admin_invites").document(cleanEmail)
    try {
      val invitedBy = sanitizeDocId(requireUserId(), "admin")
      val payload = hashMapOf<String, Any>(
        "email" to cleanEmail,
        "department" to department.trim().ifBlank { "Academic Directorate" }.take(150),
        "invitedBy" to invitedBy
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
    }
  }

  suspend fun deleteAdminInvite(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase().take(150)
    val docRef = db.collection("admin_invites").document(cleanEmail)
    try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
      val firstMatch = snapshot.documents.firstOrNull()?.toFirestoreUserProfile()
      if (firstMatch != null || cleanEmail == cleanEmail.lowercase()) {
        Result.success(firstMatch)
      } else {
        val lowerSnapshot = db.collection("users")
          .whereEqualTo("email", cleanEmail.lowercase())
          .limit(1)
          .get()
          .await()
        Result.success(lowerSnapshot.documents.firstOrNull()?.toFirestoreUserProfile())
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun isCloudDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
    try {
      val cSnap = db.collection("courses").limit(1).get().await()
      val nSnap = db.collection("notes").limit(1).get().await()
      val qSnap = db.collection("questions").limit(1).get().await()
      cSnap.isEmpty && nSnap.isEmpty && qSnap.isEmpty
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
    val docId = sanitizeDocId(announcement.id, "ann")
    val docRef = db.collection("announcements").document(docId)
    try {
      val safeBody = announcement.message.trim().ifBlank { announcement.title.trim() }.ifBlank { "Notice" }.take(5000)
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "title" to announcement.title.trim().ifBlank { "Announcement" }.take(200),
        "content" to safeBody,
        "message" to safeBody,
        "category" to announcement.category.trim().ifBlank { "GENERAL" }.take(50),
        "isUrgent" to announcement.isUrgent,
        "isPublished" to announcement.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(course.id, "C")
    val docRef = db.collection("courses").document(docId)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "code" to course.code.trim().ifBlank { "COURSE" }.take(50),
        "title" to course.title.trim().ifBlank { "Untitled Course" }.take(200),
        "description" to course.description.trim().take(2000),
        "category" to course.category.trim().ifBlank { "STEM" }.take(100),
        "iconName" to course.iconName.trim().ifBlank { "school" }.take(60),
        "totalQuestions" to course.totalQuestions.toLong().coerceIn(0L, 100_000L),
        "totalNotes" to course.totalNotes.toLong().coerceIn(0L, 100_000L),
        "isPublished" to course.isPublished,
        "orderIndex" to course.orderIndex.toLong().coerceIn(0L, 10_000L)
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(topic.id, "T")
    val docRef = db.collection("topics").document(docId)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to sanitizeDocId(topic.courseId, "C"),
        "title" to topic.title.trim().ifBlank { "General Section" }.take(200),
        "description" to topic.description.trim().take(2000),
        "orderIndex" to topic.orderIndex.toLong().coerceIn(0L, 10_000L)
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(q.id, "Q")
    val docRef = db.collection("questions").document(docId)
    try {
      val normalizedAnswer = q.correctAnswer.trim().uppercase().takeIf { it in listOf("A", "B", "C", "D") } ?: "A"
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to sanitizeDocId(q.courseId, "C"),
        "topicId" to sanitizeDocId(q.topicId.ifBlank { "T_GEN" }, "T"),
        "questionText" to q.questionText.trim().ifBlank { "Question" }.take(5000),
        "optionA" to q.optionA.trim().ifBlank { "Option A" }.take(1000),
        "optionB" to q.optionB.trim().ifBlank { "Option B" }.take(1000),
        "optionC" to q.optionC.trim().take(1000),
        "optionD" to q.optionD.trim().take(1000),
        "correctAnswer" to normalizedAnswer,
        "explanationGeneral" to q.explanationGeneral.trim().take(5000),
        "explanationA" to q.explanationA.trim().take(2000),
        "explanationB" to q.explanationB.trim().take(2000),
        "explanationC" to q.explanationC.trim().take(2000),
        "explanationD" to q.explanationD.trim().take(2000),
        "difficulty" to q.difficulty.trim().ifBlank { "MEDIUM" }.take(30),
        "isPastExam" to q.isPastExam,
        "examYear" to (q.examYear ?: "").trim().take(50),
        "examType" to (q.examType ?: "").trim().take(50),
        "isVerified" to q.isVerified,
        "isPublished" to q.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(note.id, "NOTE")
    val docRef = db.collection("notes").document(docId)
    try {
      val safeMarkdown = note.contentMarkdown.trim().take(100_000)
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to sanitizeDocId(note.courseId, "C"),
        "topicId" to sanitizeDocId(note.topicId.ifBlank { "T_GEN" }, "T"),
        "title" to note.title.trim().ifBlank { "Revision Note" }.take(200),
        "chapterNumber" to note.chapterNumber.coerceIn(1, 500).toLong(),
        "summary" to note.summary.trim().take(5000),
        "keyFormulas" to note.keyFormulas.trim().take(10_000),
        "contentMarkdown" to safeMarkdown,
        "content" to safeMarkdown,
        "isPublished" to note.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(exam.id, "EXAM")
    val docRef = db.collection("past_exams").document(docId)
    try {
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to sanitizeDocId(exam.courseId, "C"),
        "university" to exam.university.trim().ifBlank { "Addis Ababa University (AAU)" }.take(150),
        "academicYear" to exam.academicYear.trim().ifBlank { "2024" }.take(50),
        "semester" to exam.semester.trim().take(50),
        "examType" to exam.examType.trim().ifBlank { "FINAL" }.take(50),
        "title" to exam.title.trim().ifBlank { "Past Examination" }.take(200),
        "instructions" to exam.instructions.trim().take(5000),
        "totalQuestions" to exam.totalQuestions.toLong().coerceIn(0L, 1000L),
        "durationMinutes" to exam.durationMinutes.toLong().coerceIn(1L, 600L),
        "isDigitized" to exam.isDigitized,
        "isVerified" to exam.isVerified,
        "documentSummary" to exam.documentSummary.trim().take(5000),
        "isPublished" to exam.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
      Result.failure(formatCloudError(e, OperationType.DELETE, docRef.path))
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
    val docId = sanitizeDocId(code.code, "PASS")
    val docRef = db.collection("access_codes").document(docId)
    try {
      val adminCreatorId = sanitizeDocId(
        auth.currentUser?.uid ?: code.createdByAdminId.ifBlank { "admin_1" },
        "admin"
      )
      val payload = hashMapOf<String, Any>(
        "code" to docId,
        "createdByAdminId" to adminCreatorId,
        "isRedeemed" to code.isRedeemed,
        "redeemedByUserId" to (code.redeemedByUserId ?: "").trim().take(128),
        "redeemedByEmail" to (code.redeemedByEmail ?: "").trim().take(150),
        "note" to code.note.trim().take(300),
        "expiresAt" to com.google.firebase.Timestamp(java.util.Date(code.expiresAt))
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload, SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.WRITE, docRef.path))
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
    category = (data["category"] as? String)?.ifBlank { "STEM" } ?: "STEM",
    iconName = (data["iconName"] as? String)?.ifBlank { "calculate" } ?: "calculate",
    totalQuestions = (data["totalQuestions"] as? Number)?.toLong() ?: (data["totalQuestions"]?.toString()?.toLongOrNull()) ?: 0L,
    totalNotes = (data["totalNotes"] as? Number)?.toLong() ?: (data["totalNotes"]?.toString()?.toLongOrNull()) ?: 0L,
    isPublished = (data["isPublished"] as? Boolean) ?: (data["isPublished"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    orderIndex = (data["orderIndex"] as? Number)?.toLong() ?: (data["orderIndex"]?.toString()?.toLongOrNull()) ?: 0L,
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
    orderIndex = (data["orderIndex"] as? Number)?.toLong() ?: (data["orderIndex"]?.toString()?.toLongOrNull()) ?: 0L,
    createdAt = data["createdAt"] as? Timestamp,
    updatedAt = data["updatedAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreQuestion(): FirestoreQuestion {
  val data = this.data ?: return FirestoreQuestion(id = this.id)
  return FirestoreQuestion(
    id = (data["id"] as? String)?.ifBlank { this.id } ?: this.id,
    courseId = data["courseId"] as? String ?: "",
    topicId = (data["topicId"] as? String)?.ifBlank { "T_GEN" } ?: "T_GEN",
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
    isPastExam = (data["isPastExam"] as? Boolean) ?: (data["isPastExam"]?.toString()?.toBooleanStrictOrNull()) ?: false,
    examYear = data["examYear"] as? String ?: "",
    examType = data["examType"] as? String ?: "",
    isVerified = (data["isVerified"] as? Boolean) ?: (data["isVerified"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    isPublished = (data["isPublished"] as? Boolean) ?: (data["isPublished"]?.toString()?.toBooleanStrictOrNull()) ?: true,
    createdAt = data["createdAt"] as? Timestamp
  )
}

private fun DocumentSnapshot.toFirestoreNote(): FirestoreNote {
  val data = this.data ?: return FirestoreNote(id = this.id)
  val parsedChapter = (data["chapterNumber"] as? Number)?.toLong()
    ?: (data["chapter"] as? Number)?.toLong()
    ?: data["chapterNumber"]?.toString()?.toLongOrNull()
    ?: data["chapter"]?.toString()?.toLongOrNull()
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
