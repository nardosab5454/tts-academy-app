package com.example.data.firestore.model

import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.UserEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val name: String = "",
  val role: String = "STUDENT",
  val department: String = "",
  val studentId: String = "",
  val isPremium: Boolean = false,
  val enrolledCourses: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toEntity(): UserEntity {
    val cleanId = if (userId.isNotBlank()) userId else id
    return UserEntity(
      id = cleanId,
      name = name.ifBlank { "TTS Student" },
      email = email,
      studentId = studentId.ifBlank { "STU-${cleanId.take(4).uppercase()}" },
      department = department.ifBlank { "General Freshman Studies" },
      role = role.ifBlank { "STUDENT" },
      isPremium = isPremium,
      createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
  }
}

@IgnoreExtraProperties
data class FirestoreAnnouncement(
  @DocumentId val id: String = "",
  val title: String = "",
  val content: String = "",
  val category: String = "GENERAL",
  val author: String = "TTS Directorate",
  val isUrgent: Boolean = false,
  val isPublished: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
) {
  fun toEntity(): AnnouncementEntity {
    return AnnouncementEntity(
      id = id,
      title = title,
      message = content,
      category = category.ifBlank { "GENERAL" },
      isUrgent = isUrgent,
      isPublished = isPublished,
      createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
  }
}

@IgnoreExtraProperties
data class FirestoreCourse(
  @DocumentId val id: String = "",
  val code: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = "STEM",
  val iconName: String = "calculate",
  val totalQuestions: Long = 0L,
  val totalNotes: Long = 0L,
  val isPublished: Boolean = true,
  val orderIndex: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toEntity(): CourseEntity {
    return CourseEntity(
      id = id,
      code = code,
      title = title,
      description = description,
      category = category.ifBlank { "STEM" },
      iconName = iconName.ifBlank { "calculate" },
      totalQuestions = totalQuestions.toInt(),
      totalNotes = totalNotes.toInt(),
      isPublished = isPublished,
      orderIndex = orderIndex.toInt()
    )
  }
}

@IgnoreExtraProperties
data class FirestoreQuestion(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val topicId: String = "T_GEN",
  val questionText: String = "",
  val optionA: String = "",
  val optionB: String = "",
  val optionC: String = "",
  val optionD: String = "",
  val correctAnswer: String = "A",
  val explanationGeneral: String = "",
  val explanationA: String = "",
  val explanationB: String = "",
  val explanationC: String = "",
  val explanationD: String = "",
  val difficulty: String = "MEDIUM",
  val isPastExam: Boolean = false,
  val examYear: String = "",
  val examType: String = "",
  val isVerified: Boolean = true,
  val isPublished: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
) {
  fun toEntity(): QuestionEntity {
    return QuestionEntity(
      id = id,
      courseId = courseId,
      topicId = topicId.ifBlank { "T_GEN" },
      questionText = questionText,
      optionA = optionA,
      optionB = optionB,
      optionC = optionC,
      optionD = optionD,
      correctAnswer = correctAnswer,
      explanationGeneral = explanationGeneral,
      explanationA = explanationA,
      explanationB = explanationB,
      explanationC = explanationC,
      explanationD = explanationD,
      difficulty = difficulty,
      isPastExam = isPastExam,
      examYear = examYear,
      examType = examType,
      isVerified = isVerified,
      isPublished = isPublished
    )
  }
}

@IgnoreExtraProperties
data class FirestoreNote(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val title: String = "",
  val summary: String = "",
  val keyFormulas: String = "",
  val contentMarkdown: String = "",
  val isPublished: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toEntity(): NoteEntity {
    return NoteEntity(
      id = id,
      courseId = courseId,
      topicId = "T_GEN",
      title = title,
      summary = summary,
      keyFormulas = keyFormulas,
      contentMarkdown = contentMarkdown,
      isPublished = isPublished
    )
  }
}

@IgnoreExtraProperties
data class FirestorePastExam(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val academicYear: String = "",
  val semester: String = "Semester 1",
  val examType: String = "FINAL",
  val title: String = "",
  val instructions: String = "",
  val totalQuestions: Long = 0L,
  val durationMinutes: Long = 60L,
  val isDigitized: Boolean = true,
  val isVerified: Boolean = true,
  val documentSummary: String = "",
  val isPublished: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null
) {
  fun toEntity(): PastExamEntity {
    return PastExamEntity(
      id = id,
      courseId = courseId,
      academicYear = academicYear,
      semester = semester,
      examType = examType,
      title = title,
      instructions = instructions,
      totalQuestions = totalQuestions.toInt(),
      durationMinutes = durationMinutes.toInt(),
      isDigitized = isDigitized,
      isVerified = isVerified,
      documentSummary = documentSummary,
      isPublished = isPublished
    )
  }
}

@IgnoreExtraProperties
data class FirestoreAccessCode(
  @DocumentId val code: String = "",
  val createdByAdminId: String = "admin_1",
  val isRedeemed: Boolean = false,
  val redeemedByUserId: String = "",
  val redeemedByEmail: String = "",
  val note: String = "Permanent Academic Pass",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val expiresAt: Timestamp? = null,
  @ServerTimestamp val redeemedAt: Timestamp? = null
) {
  fun toEntity(): AccessCodeEntity {
    return AccessCodeEntity(
      code = code,
      createdByAdminId = createdByAdminId,
      isRedeemed = isRedeemed,
      redeemedByUserId = redeemedByUserId.ifBlank { null },
      redeemedByEmail = redeemedByEmail.ifBlank { null },
      note = note,
      createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
  }
}

@IgnoreExtraProperties
data class FirestoreQuizAttempt(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val topicId: String = "",
  val score: Long = 0L,
  val totalQuestions: Long = 0L,
  val percentage: Long = 0L,
  @ServerTimestamp val completedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBookmark(
  @DocumentId val id: String = "",
  val userId: String = "",
  val questionId: String = "",
  val courseId: String = "",
  val topicId: String = "",
  @ServerTimestamp val savedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreStudyNote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val title: String = "",
  val content: String = "",
  val tags: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
