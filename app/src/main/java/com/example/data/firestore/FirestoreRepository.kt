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
  val hasAdminClaim: Boolean = false,
  val canReadCoursesFromServer: Boolean = false,
  val canPassAdminReadProbe: Boolean = false,
  val hasWriteReadyAuth: Boolean = false,
  val writeTestAttempted: Boolean = false,
  val writeTestPassed: Boolean = false,
  val writeTestError: String? = null,
  val diagnosticMessage: String = ""
) {
  fun toCopyableReport(): String = buildString {
    appendLine("TTS Academy Cloud Admin Diagnostic")
    appendLine("Email: ${signedInEmail ?: "Not signed in"}")
    appendLine("Email Verified: $isEmailVerified")
    appendLine("Firebase UID: ${signedInUid ?: "none"}")
    appendLine("Database ID: $databaseId")
    appendLine("User Doc Exists: $userDocExists | Role: $userDocRole")
    appendLine("Admin Claim: $hasAdminClaim | Bootstrapped Admin: $isBootstrappedEmail | Invited Admin: $isInvitedEmail")
    appendLine("Server Read Courses: $canReadCoursesFromServer | Admin Probe: $canPassAdminReadProbe")
    if (writeTestAttempted) {
      appendLine("Live Write Test: ${if (writeTestPassed) "PASSED (Create + Server Read + Delete)" else "FAILED"}")
      if (!writeTestError.isNullOrBlank()) {
        appendLine("Write Test Error: $writeTestError")
      }
    }
    appendLine("Summary: $diagnosticMessage")
  }
}

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

  private val ALLOWED_USER_PROFILE_KEYS = setOf(
    "userId", "email", "name", "role", "department", "studentId",
    "isPremium", "activationCode", "questionsAnsweredCount",
    "correctAnswersCount", "enrolledCourses", "createdAt", "updatedAt"
  )

  private val DEFAULT_ENROLLED_COURSES = listOf("MATH_101", "PHYS_101", "CHEM_101", "CS_101")

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  private fun isBootstrappedAdminEmail(email: String): Boolean {
    val clean = email.trim().lowercase()
    return clean == "spikenarrd@gmail.com" || clean == "abebewottee@gmail.com"
  }

  private fun hasAdminCustomClaim(claims: Map<String, Any>): Boolean {
    val roleClaim = (claims["role"] as? String)?.trim()?.lowercase()
    val adminFlag = claims["admin"] as? Boolean
    return roleClaim == "admin" || roleClaim == "super_admin" || adminFlag == true
  }

  private fun normalizeDocId(rawId: String, fallbackPrefix: String): String {
    val cleaned = rawId.trim().replace("/", "_").replace("\\", "_").take(128).trim()
    return if (cleaned.isNotBlank()) {
      cleaned
    } else {
      "${fallbackPrefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    }
  }

  private suspend fun isCurrentUserEmailVerified(fbUser: com.google.firebase.auth.FirebaseUser?): Boolean {
    if (fbUser == null) return false
    if (fbUser.isEmailVerified) return true
    val claimVerified = try {
      fbUser.getIdToken(false).await().claims["email_verified"] as? Boolean
    } catch (_: Exception) {
      null
    }
    if (claimVerified == true) return true
    return try {
      fbUser.reload().await()
      fbUser.isEmailVerified
    } catch (_: Exception) {
      false
    }
  }

  private suspend fun refreshAdminAuthAndProfile() {
    val fbUser = auth.currentUser ?: return
    try {
      fbUser.reload().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w("FirestoreRepo", "fbUser.reload() failed during admin refresh: ${e.message}")
    }
    try {
      fbUser.getIdToken(true).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w("FirestoreRepo", "fbUser.getIdToken(true) failed during admin refresh: ${e.message}")
    }
    val email = fbUser.email?.trim().orEmpty()
    val name = fbUser.displayName?.trim().orEmpty().ifBlank { "TTS Admin" }
    if (email.isNotBlank()) {
      try {
        getOrCreateUserProfile(
          userId = fbUser.uid,
          email = email,
          name = name,
          role = if (isBootstrappedAdminEmail(email)) "ADMIN" else "STUDENT"
        ).onFailure { err ->
          Log.w("FirestoreRepo", "Admin profile repair failed during refresh: ${err.message}")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w("FirestoreRepo", "Unexpected error repairing admin profile: ${e.message}")
      }
    }
  }

  private suspend fun <T> executeAdminWrite(
    operationType: OperationType,
    path: String,
    block: suspend () -> T
  ): Result<T> = withContext(Dispatchers.IO) {
    try {
      Result.success(block())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val firestoreEx = findFirestoreException(e)
      val isAuthOrPermError = firestoreEx != null && (
        firestoreEx.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
        firestoreEx.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
      )
      if (isAuthOrPermError && auth.currentUser != null) {
        Log.w("FirestoreRepo", "Retrying ${operationType.value} on $path after refreshing Firebase Auth token & admin profile (initial=${firestoreEx?.code})")
        refreshAdminAuthAndProfile()
        try {
          return@withContext Result.success(block())
        } catch (retryEx: CancellationException) {
          throw retryEx
        } catch (retryEx: Exception) {
          return@withContext Result.failure(formatCloudError(retryEx, operationType, path))
        }
      }
      Result.failure(formatCloudError(e, operationType, path))
    }
  }

  private suspend fun fetchCollectionServerFirst(
    collectionRef: com.google.firebase.firestore.CollectionReference
  ): com.google.firebase.firestore.QuerySnapshot {
    return try {
      collectionRef.get(Source.SERVER).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        throw e
      }
      collectionRef.get(Source.DEFAULT).await()
    } catch (_: Exception) {
      collectionRef.get(Source.DEFAULT).await()
    }
  }

  private fun formatCloudError(
    e: Exception,
    operationType: OperationType,
    path: String
  ): Exception {
    handleFirestoreError(e, operationType, path)
    val firestoreEx = findFirestoreException(e)
    val fbUser = auth.currentUser
    val email = fbUser?.email ?: "not signed in"
    val uid = fbUser?.uid ?: "none"
    val verified = fbUser?.isEmailVerified == true

    if (firestoreEx != null) {
      val codeName = firestoreEx.code.name
      val serverDetail = firestoreEx.message?.take(360) ?: "no server detail"
      val reason = when (firestoreEx.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> when {
          fbUser == null -> "Firebase session is missing. Sign out and sign in with your Google Admin account."
          else -> "Firestore [$codeName] on ${operationType.value} '$path' for $email (uid=$uid, verified=$verified, db=$configuredDatabaseId): $serverDetail"
        }
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
          "Auth token rejected [$codeName] on '$path' for $email (db=$configuredDatabaseId): $serverDetail. Sign out and sign in again."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
          "Cloud Firestore temporarily unreachable [$codeName] on '$path' (db=$configuredDatabaseId). Check internet connection and retry."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
          "Firestore quota or write limit exceeded [$codeName] on '$path' (db=$configuredDatabaseId): $serverDetail"
        else ->
          "Firestore error [$codeName] on ${operationType.value} '$path' (email=$email, db=$configuredDatabaseId): $serverDetail"
      }
      val prefix = if (firestoreEx.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        "Missing or insufficient permissions: "
      } else {
        "Cloud write failed: "
      }
      return IllegalStateException("$prefix$reason", e)
    }
    return IllegalStateException(
      "Cloud ${operationType.value} failed on '$path' (email=$email, db=$configuredDatabaseId): ${e.message ?: e::class.java.simpleName}",
      e
    )
  }

  private suspend fun fetchServerFirstSnapshot(
    docRef: com.google.firebase.firestore.DocumentReference
  ): DocumentSnapshot? {
    return try {
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
  }

  private suspend fun prepareTimestamps(
    docRef: com.google.firebase.firestore.DocumentReference,
    payload: MutableMap<String, Any>,
    isNewDocument: Boolean = false
  ): DocumentSnapshot? {
    val snapshot = if (isNewDocument) {
      null
    } else {
      try {
        docRef.get(Source.SERVER).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w("FirestoreRepo", "Pre-write timestamp lookup skipped for ${docRef.path}: ${e.message}")
        try {
          docRef.get(Source.DEFAULT).await()
        } catch (_: Exception) {
          null
        }
      }
    }
    val existingCreatedAt = if (snapshot != null && snapshot.exists()) {
      snapshot.get("createdAt") as? Timestamp
    } else {
      null
    }
    payload["createdAt"] = existingCreatedAt ?: FieldValue.serverTimestamp()
    payload["updatedAt"] = FieldValue.serverTimestamp()
    return snapshot
  }

  private fun buildCleanUserPayload(
    snapshot: DocumentSnapshot?,
    uid: String,
    fallbackEmail: String,
    fallbackName: String,
    role: String,
    isPremium: Boolean,
    fallbackDepartment: String = "General Freshman Studies",
    overrideDepartment: String? = null,
    overrideStudentId: String? = null,
    overrideActivationCode: String? = null,
    overrideQuestionsAnsweredCount: Long? = null,
    overrideCorrectAnswersCount: Long? = null
  ): HashMap<String, Any> {
    val existingEmail = snapshot?.getString("email")?.trim()?.takeIf { it.length > 3 }
    val resolvedEmail = (existingEmail ?: fallbackEmail.trim().ifBlank { "student@ttsacademy.edu" }).take(150)

    val existingName = snapshot?.getString("name")?.trim()?.takeIf { it.isNotBlank() }
    val resolvedName = (existingName ?: fallbackName.trim().ifBlank { "TTS Student" }).take(100)

    val normalizedRole = if (role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
    val defaultCode = if (normalizedRole == "ADMIN") "ADM-" + uid.take(6).uppercase() else "STU-" + uid.take(6).uppercase()

    val resolvedDepartment = (overrideDepartment ?: snapshot?.getString("department") ?: fallbackDepartment)
      .trim()
      .take(150)
    val resolvedStudentId = (overrideStudentId?.takeIf { it.isNotBlank() }
      ?: snapshot?.getString("studentId")?.takeIf { it.isNotBlank() }
      ?: defaultCode)
      .trim()
      .take(50)
    val resolvedActivationCode = (overrideActivationCode ?: snapshot?.getString("activationCode") ?: "")
      .trim()
      .take(20)
    val resolvedQCount = (overrideQuestionsAnsweredCount
      ?: (snapshot?.get("questionsAnsweredCount") as? Number)?.toLong()
      ?: 0L)
      .coerceIn(0L, 1_000_000L)
    val resolvedCCount = (overrideCorrectAnswersCount
      ?: (snapshot?.get("correctAnswersCount") as? Number)?.toLong()
      ?: 0L)
      .coerceIn(0L, 1_000_000L)
    val resolvedCourses = (snapshot?.get("enrolledCourses") as? List<*>)
      ?.filterIsInstance<String>()
      ?.take(100)
      ?: DEFAULT_ENROLLED_COURSES
    val existingCreatedAt = if (snapshot != null && snapshot.exists()) {
      snapshot.get("createdAt") as? Timestamp
    } else {
      null
    }

    return hashMapOf(
      "userId" to uid,
      "email" to resolvedEmail,
      "name" to resolvedName,
      "role" to normalizedRole,
      "department" to resolvedDepartment,
      "studentId" to resolvedStudentId,
      "isPremium" to (isPremium || normalizedRole == "ADMIN"),
      "activationCode" to resolvedActivationCode,
      "questionsAnsweredCount" to resolvedQCount,
      "correctAnswersCount" to resolvedCCount,
      "enrolledCourses" to resolvedCourses,
      "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
      "updatedAt" to FieldValue.serverTimestamp()
    )
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
    val tokenClaims = try {
      fbUser.getIdToken(false).await().claims
    } catch (_: Exception) {
      emptyMap<String, Any>()
    }
    val tokenEmail = (tokenClaims["email"] as? String)?.trim().orEmpty()
    val email = tokenEmail.ifBlank { fbUser.email?.trim() ?: "" }
    val tokenVerified = tokenClaims["email_verified"] as? Boolean
    val verified = tokenVerified == true || isCurrentUserEmailVerified(fbUser)
    val hasClaimAdmin = hasAdminCustomClaim(tokenClaims)
    val bootstrapped = isBootstrappedAdminEmail(email)
    val invited = if (!bootstrapped && email.isNotBlank()) isEmailInvitedAdmin(email) else false

    var userDocExists = false
    var userDocRole = "NONE"
    var canReadCourses = false
    var canPassAdminRule = false
    var serverProbeError: String? = null

    try {
      val userSnap = db.collection("users").document(uid).get(Source.SERVER).await()
      userDocExists = userSnap.exists()
      userDocRole = userSnap.getString("role") ?: if (userDocExists) "MISSING_ROLE" else "NONE"
    } catch (e: Exception) {
      val code = findFirestoreException(e)?.code?.name ?: "ERR"
      serverProbeError = "[$code] users/$uid: ${e.message}"
    }

    try {
      db.collection("courses").limit(1).get(Source.SERVER).await()
      canReadCourses = true
    } catch (e: Exception) {
      val code = findFirestoreException(e)?.code?.name ?: "ERR"
      if (serverProbeError == null) serverProbeError = "[$code] courses: ${e.message}"
    }

    // Live server-side isAdmin() probe (users collection list requires isAdmin() in firestore.rules)
    try {
      db.collection("users").limit(1).get(Source.SERVER).await()
      canPassAdminRule = true
    } catch (e: Exception) {
      val code = findFirestoreException(e)?.code?.name ?: "ERR"
      serverProbeError = "[$code] admin probe failed: ${e.message}"
    }

    val writeReady = canPassAdminRule
    val msg = when {
      canPassAdminRule -> "Cloud admin verified ($email • DB role=$userDocRole${if (hasClaimAdmin) " • Admin Claim" else ""}${if (bootstrapped) " • Bootstrapped" else if (invited) " • Invited" else ""}${if (!verified) " • email_verified=false" else ""})."
      !verified && (bootstrapped || invited) -> "Google email ($email) is not verified in Firebase token (claim=$tokenVerified). Tap Refresh Permissions or re-sign in with Google."
      serverProbeError != null -> "Cloud admin check failed ($email, db=$configuredDatabaseId): $serverProbeError"
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
      hasAdminClaim = hasClaimAdmin,
      canReadCoursesFromServer = canReadCourses,
      canPassAdminReadProbe = canPassAdminRule,
      hasWriteReadyAuth = writeReady,
      diagnosticMessage = msg
    )
  }

  suspend fun runAdminWriteSelfTest(): CloudAdminDiagnostic = withContext(Dispatchers.IO) {
    refreshAdminAuthAndProfile()
    val base = diagnoseAdminAccess()
    val fbUser = auth.currentUser ?: return@withContext base.copy(
      writeTestAttempted = true,
      writeTestPassed = false,
      writeTestError = "Not signed into Firebase Auth."
    )

    val probeId = "diag_${normalizeDocId(fbUser.uid.take(16), "admin")}"
    val docRef = db.collection("announcements").document(probeId)
    val writeResult = executeAdminWrite(OperationType.CREATE, docRef.path) {
      val payload = hashMapOf<String, Any>(
        "id" to probeId,
        "title" to "Admin Cloud Write Diagnostic",
        "content" to "Temporary self-test probe verified and removed automatically.",
        "message" to "Temporary self-test probe verified and removed automatically.",
        "category" to "SYSTEM",
        "isUrgent" to false,
        "isPublished" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      val verifiedSnap = docRef.get(Source.SERVER).await()
      if (!verifiedSnap.exists()) {
        throw IllegalStateException("Write succeeded locally but document was not readable from Firestore server.")
      }
      docRef.delete().await()
      Unit
    }

    if (writeResult.isSuccess) {
      base.copy(
        hasWriteReadyAuth = true,
        writeTestAttempted = true,
        writeTestPassed = true,
        writeTestError = null,
        diagnosticMessage = "Live Cloud Write Test PASSED for ${base.signedInEmail} (Create + Server Read + Delete verified on ${base.databaseId})."
      )
    } else {
      val errText = writeResult.exceptionOrNull()?.message ?: "Unknown cloud write test failure."
      base.copy(
        hasWriteReadyAuth = false,
        writeTestAttempted = true,
        writeTestPassed = false,
        writeTestError = errText,
        diagnosticMessage = "Live Cloud Write Test FAILED: $errText"
      )
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
    val currentFbUser = auth.currentUser
    val uid = normalizeDocId(currentFbUser?.uid ?: userId, "user")
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

      val tokenClaims = try {
        currentFbUser?.getIdToken(false)?.await()?.claims ?: emptyMap()
      } catch (_: Exception) {
        emptyMap()
      }
      val hasClaimAdmin = hasAdminCustomClaim(tokenClaims)
      val isEmailVerified = isCurrentUserEmailVerified(currentFbUser)
      val isVerifiedBootstrapped = isBootstrappedAdminEmail(cleanEmail) && (currentFbUser == null || isEmailVerified)
      val isInvited = if (!isVerifiedBootstrapped && !hasClaimAdmin) isEmailInvitedAdmin(cleanEmail) else false
      val shouldBeAdmin = hasClaimAdmin || isVerifiedBootstrapped || isInvited || role.equals("ADMIN", ignoreCase = true)

      if (snapshot.exists()) {
        val existingProfile = snapshot.toFirestoreUserProfile()
        val existingCreatedAt = snapshot.get("createdAt") as? Timestamp
        val hasLegacyUnknownFields = snapshot.data?.keys?.any { it !in ALLOWED_USER_PROFILE_KEYS } == true
        val effectiveRole = if (shouldBeAdmin || existingProfile.role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
        val effectivePremium = existingProfile.isPremium || effectiveRole == "ADMIN"
        val missingRequiredFields =
          snapshot.getString("userId").isNullOrBlank() ||
          snapshot.getString("email").isNullOrBlank() ||
          snapshot.getString("name").isNullOrBlank() ||
          snapshot.getString("role").isNullOrBlank() ||
          snapshot.getBoolean("isPremium") == null ||
          existingCreatedAt == null

        val needsRolePromotion = effectiveRole == "ADMIN" &&
          (!existingProfile.role.equals("ADMIN", ignoreCase = true) || !existingProfile.isPremium)

        if (needsRolePromotion || missingRequiredFields || hasLegacyUnknownFields) {
          val cleanPayload = buildCleanUserPayload(
            snapshot = snapshot,
            uid = uid,
            fallbackEmail = cleanEmail,
            fallbackName = cleanName,
            role = effectiveRole,
            isPremium = effectivePremium,
            fallbackDepartment = cleanDept
          )
          docRef.set(cleanPayload).await()
          Result.success(
            existingProfile.copy(
              userId = uid,
              email = cleanPayload["email"] as? String ?: cleanEmail,
              name = cleanPayload["name"] as? String ?: cleanName,
              role = effectiveRole,
              department = cleanPayload["department"] as? String ?: cleanDept,
              studentId = cleanPayload["studentId"] as? String ?: existingProfile.studentId,
              isPremium = effectivePremium,
              enrolledCourses = (cleanPayload["enrolledCourses"] as? List<*>)?.filterIsInstance<String>() ?: existingProfile.enrolledCourses
            )
          )
        } else {
          Result.success(existingProfile)
        }
      } else {
        val effectiveRole = if (shouldBeAdmin) "ADMIN" else "STUDENT"
        val effectivePremium = shouldBeAdmin
        val studentCode = if (shouldBeAdmin) "ADM-" + uid.take(6).uppercase() else "STU-" + uid.take(6).uppercase()
        val newProfile = buildCleanUserPayload(
          snapshot = null,
          uid = uid,
          fallbackEmail = cleanEmail,
          fallbackName = cleanName,
          role = effectiveRole,
          isPremium = effectivePremium,
          fallbackDepartment = cleanDept,
          overrideStudentId = studentCode
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
            enrolledCourses = DEFAULT_ENROLLED_COURSES
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
    val uid = normalizeDocId(user.id, "user")
    val docRef = db.collection("users").document(uid)
    try {
      val snapshot = fetchServerFirstSnapshot(docRef)
      val normalizedRole = if (user.role.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
      val payload = buildCleanUserPayload(
        snapshot = snapshot,
        uid = uid,
        fallbackEmail = user.email,
        fallbackName = user.name,
        role = normalizedRole,
        isPremium = user.isPremium || normalizedRole == "ADMIN",
        fallbackDepartment = user.department,
        overrideDepartment = user.department.trim().take(150),
        overrideStudentId = user.studentId.trim().take(50),
        overrideActivationCode = (user.activationCode ?: "").trim().take(20),
        overrideQuestionsAnsweredCount = user.questionsAnsweredCount.toLong().coerceIn(0L, 1_000_000L),
        overrideCorrectAnswersCount = user.correctAnswersCount.toLong().coerceIn(0L, 1_000_000L)
      )
      docRef.set(payload).await()
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
      var snapshot = docRef.get(Source.SERVER).await()
      if (!snapshot.exists()) {
        val email = auth.currentUser?.email ?: "student@ttsacademy.edu"
        val name = auth.currentUser?.displayName ?: "TTS Student"
        getOrCreateUserProfile(userId, email, name).getOrThrow()
        snapshot = docRef.get(Source.SERVER).await()
      }
      val existingCreatedAt = snapshot.get("createdAt") as? Timestamp
      val hasLegacyUnknownFields = snapshot.data?.keys?.any { it !in ALLOWED_USER_PROFILE_KEYS } == true
      if (existingCreatedAt == null || hasLegacyUnknownFields) {
        val prevQ = (snapshot.get("questionsAnsweredCount") as? Number)?.toLong() ?: 0L
        val prevC = (snapshot.get("correctAnswersCount") as? Number)?.toLong() ?: 0L
        val cleanPayload = buildCleanUserPayload(
          snapshot = snapshot,
          uid = userId,
          fallbackEmail = auth.currentUser?.email ?: "student@ttsacademy.edu",
          fallbackName = auth.currentUser?.displayName ?: "TTS Student",
          role = snapshot.getString("role") ?: "STUDENT",
          isPremium = snapshot.getBoolean("isPremium") ?: false,
          overrideQuestionsAnsweredCount = (prevQ + questionsDelta).coerceIn(0L, 1_000_000L),
          overrideCorrectAnswersCount = (prevC + correctDelta).coerceIn(0L, 1_000_000L)
        )
        docRef.set(cleanPayload).await()
      } else {
        val payload = hashMapOf<String, Any>(
          "questionsAnsweredCount" to FieldValue.increment(questionsDelta.toLong()),
          "correctAnswersCount" to FieldValue.increment(correctDelta.toLong()),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.UPDATE, docRef.path))
    }
  }

  suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> {
    val docRef = db.collection("users").document(userId)
    return executeAdminWrite(OperationType.UPDATE, docRef.path) {
      val normalizedRole = if (newRole.equals("ADMIN", ignoreCase = true)) "ADMIN" else "STUDENT"
      val snapshot = docRef.get(Source.SERVER).await()
      if (!snapshot.exists()) {
        throw IllegalStateException("User profile '$userId' does not exist in cloud database yet.")
      }
      val hasActivePass = !snapshot.getString("activationCode").isNullOrBlank()
      val effectivePremium = normalizedRole == "ADMIN" || hasActivePass
      val cleanPayload = buildCleanUserPayload(
        snapshot = snapshot,
        uid = userId,
        fallbackEmail = "student@ttsacademy.edu",
        fallbackName = "TTS Member",
        role = normalizedRole,
        isPremium = effectivePremium
      )
      docRef.set(cleanPayload).await()
      Unit
    }
  }

  suspend fun saveAdminInvite(email: String, department: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase().take(150)
    val docRef = db.collection("admin_invites").document(cleanEmail)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val invitedBy = normalizeDocId(requireUserId(), "admin")
      val payload = hashMapOf<String, Any>(
        "email" to cleanEmail,
        "department" to department.trim().ifBlank { "Academic Directorate" }.take(150),
        "invitedBy" to invitedBy
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deleteAdminInvite(email: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase().take(150)
    val docRef = db.collection("admin_invites").document(cleanEmail)
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
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
      val cSnap = db.collection("courses").limit(1).get(Source.SERVER).await()
      val nSnap = db.collection("notes").limit(1).get(Source.SERVER).await()
      val qSnap = db.collection("questions").limit(1).get(Source.SERVER).await()
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
        close(error)
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
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreAnnouncement() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveAnnouncement(announcement: AnnouncementEntity, isNewDocument: Boolean = false): Result<Unit> {
    val docId = normalizeDocId(announcement.id, "ann")
    val docRef = db.collection("announcements").document(docId)
    return executeAdminWrite(if (isNewDocument) OperationType.CREATE else OperationType.WRITE, docRef.path) {
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
      prepareTimestamps(docRef, payload, isNewDocument = isNewDocument)
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deleteAnnouncement(id: String): Result<Unit> {
    val docRef = db.collection("announcements").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Courses Sync ---

  fun observeCourses(): Flow<List<FirestoreCourse>> = callbackFlow {
    val colRef = db.collection("courses")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreCourse() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveCourse(course: CourseEntity): Result<Unit> {
    val docId = normalizeDocId(course.id, "C")
    val docRef = db.collection("courses").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
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
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun updateCourseCounts(courseId: String, totalQuestions: Int, totalNotes: Int): Result<Unit> {
    val docId = normalizeDocId(courseId, "C")
    val docRef = db.collection("courses").document(docId)
    return executeAdminWrite(OperationType.UPDATE, docRef.path) {
      val snapshot = fetchServerFirstSnapshot(docRef)
      if (snapshot != null && snapshot.exists() && snapshot.get("createdAt") is Timestamp) {
        docRef.update(
          mapOf(
            "totalQuestions" to totalQuestions.toLong().coerceIn(0L, 100_000L),
            "totalNotes" to totalNotes.toLong().coerceIn(0L, 100_000L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Unit
    }
  }

  suspend fun deleteCourse(id: String): Result<Unit> {
    val docRef = db.collection("courses").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Sections / Topics Sync ---

  fun observeTopics(): Flow<List<FirestoreTopic>> = callbackFlow {
    val colRef = db.collection("topics")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
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
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreTopic() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveTopic(topic: TopicEntity): Result<Unit> {
    val docId = normalizeDocId(topic.id, "T")
    val docRef = db.collection("topics").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to normalizeDocId(topic.courseId, "C"),
        "title" to topic.title.trim().ifBlank { "General Section" }.take(200),
        "description" to topic.description.trim().take(2000),
        "orderIndex" to topic.orderIndex.toLong().coerceIn(0L, 10_000L)
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deleteTopic(id: String): Result<Unit> {
    val docRef = db.collection("topics").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Questions Sync ---

  fun observeQuestions(): Flow<List<FirestoreQuestion>> = callbackFlow {
    val colRef = db.collection("questions")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreQuestion() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveQuestion(q: QuestionEntity): Result<Unit> {
    val docId = normalizeDocId(q.id, "Q")
    val docRef = db.collection("questions").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val normalizedAnswer = q.correctAnswer.trim().uppercase().takeIf { it in listOf("A", "B", "C", "D") } ?: "A"
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to normalizeDocId(q.courseId, "C"),
        "topicId" to normalizeDocId(q.topicId.ifBlank { "T_GEN" }, "T"),
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
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deleteQuestion(id: String): Result<Unit> {
    val docRef = db.collection("questions").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Revision Notes Sync ---

  fun observeNotes(): Flow<List<FirestoreNote>> = callbackFlow {
    val colRef = db.collection("notes")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreNote() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveNote(note: NoteEntity): Result<Unit> {
    val docId = normalizeDocId(note.id, "NOTE")
    val docRef = db.collection("notes").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val safeMarkdown = note.contentMarkdown.trim().take(100_000)
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to normalizeDocId(note.courseId, "C"),
        "topicId" to normalizeDocId(note.topicId.ifBlank { "T_GEN" }, "T"),
        "title" to note.title.trim().ifBlank { "Revision Note" }.take(200),
        "chapterNumber" to note.chapterNumber.coerceIn(1, 500).toLong(),
        "summary" to note.summary.trim().take(5000),
        "keyFormulas" to note.keyFormulas.trim().take(10_000),
        "contentMarkdown" to safeMarkdown,
        "content" to safeMarkdown,
        "isPublished" to note.isPublished
      )
      prepareTimestamps(docRef, payload)
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deleteNote(id: String): Result<Unit> {
    val docRef = db.collection("notes").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Past Exams Sync ---

  fun observePastExams(): Flow<List<FirestorePastExam>> = callbackFlow {
    val colRef = db.collection("past_exams")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestorePastExam() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun savePastExam(exam: PastExamEntity): Result<Unit> {
    val docId = normalizeDocId(exam.id, "EXAM")
    val docRef = db.collection("past_exams").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val payload = hashMapOf<String, Any>(
        "id" to docId,
        "courseId" to normalizeDocId(exam.courseId, "C"),
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
      docRef.set(payload).await()
      Unit
    }
  }

  suspend fun deletePastExam(id: String): Result<Unit> {
    val docRef = db.collection("past_exams").document(id.trim())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
    }
  }

  // --- Access Codes Sync ---

  fun observeAccessCodes(): Flow<List<FirestoreAccessCode>> = callbackFlow {
    val colRef = db.collection("access_codes")
    val registration = colRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        close(error)
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val list = snapshot.documents.map { it.toFirestoreAccessCode() }
        trySend(list)
      }
    }
    awaitClose { registration.remove() }
  }

  suspend fun saveAccessCode(code: AccessCodeEntity): Result<Unit> {
    val docId = normalizeDocId(code.code.uppercase(), "PASS")
    val docRef = db.collection("access_codes").document(docId)
    return executeAdminWrite(OperationType.WRITE, docRef.path) {
      val adminCreatorId = normalizeDocId(
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
      if (code.redeemedAt != null) {
        payload["redeemedAt"] = Timestamp(java.util.Date(code.redeemedAt))
      }
      prepareTimestamps(docRef, payload)
      docRef.set(payload).await()
      Unit
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

        val userExistingCreatedAt = userSnap.get("createdAt") as? Timestamp
        val hasLegacyUserFields = userSnap.data?.keys?.any { it !in ALLOWED_USER_PROFILE_KEYS } == true
        if (userExistingCreatedAt == null || hasLegacyUserFields) {
          val cleanUserPayload = buildCleanUserPayload(
            snapshot = userSnap,
            uid = effectiveUid,
            fallbackEmail = userEmail.ifBlank { auth.currentUser?.email ?: "student@ttsacademy.edu" },
            fallbackName = auth.currentUser?.displayName ?: "TTS Student",
            role = userSnap.getString("role") ?: "STUDENT",
            isPremium = true,
            overrideActivationCode = clean
          )
          transaction.set(userDocRef, cleanUserPayload)
        } else {
          val userUpdates = hashMapOf<String, Any>(
            "isPremium" to true,
            "activationCode" to clean,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          transaction.update(userDocRef, userUpdates)
        }
        true
      }.await()

      Result.success(true)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.UPDATE, codeDocRef.path))
    }
  }

  suspend fun deleteAccessCode(code: String): Result<Unit> {
    val docRef = db.collection("access_codes").document(code.trim().uppercase())
    return executeAdminWrite(OperationType.DELETE, docRef.path) {
      docRef.delete().await()
      Unit
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
        close(error)
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
    val colRef = db.collection("courses")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreCourse() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestQuestions(): Result<List<FirestoreQuestion>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("questions")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreQuestion() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestNotes(): Result<List<FirestoreNote>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("notes")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreNote() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestPastExams(): Result<List<FirestorePastExam>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("past_exams")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestorePastExam() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestTopics(): Result<List<FirestoreTopic>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("topics")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreTopic() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestAnnouncements(): Result<List<FirestoreAnnouncement>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("announcements")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreAnnouncement() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
    }
  }

  suspend fun fetchLatestUsers(): Result<List<FirestoreUserProfile>> = withContext(Dispatchers.IO) {
    val colRef = db.collection("users")
    try {
      val snap = fetchCollectionServerFirst(colRef)
      Result.success(snap.documents.map { it.toFirestoreUserProfile() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(formatCloudError(e, OperationType.LIST, colRef.path))
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
