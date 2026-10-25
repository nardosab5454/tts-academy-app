package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val name: String,
  val email: String,
  val password: String = "student123",
  val studentId: String,
  val department: String,
  val role: String, // "STUDENT" or "ADMIN"
  val avatarUrl: String = "avatar_1",
  val streakDays: Int = 3,
  val lastStudyDate: String = "",
  val questionsAnsweredCount: Int = 0,
  val correctAnswersCount: Int = 0,
  val isNotificationsEnabled: Boolean = true,
  val studyGoalMinutesPerDay: Int = 45,
  val isPremium: Boolean = false,
  val activationCode: String? = null,
  val activatedAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "courses")
data class CourseEntity(
  @PrimaryKey val id: String,
  val code: String,
  val title: String,
  val description: String,
  val category: String, // "STEM", "Computing", "Humanities", "Engineering", "Social Sciences"
  val iconName: String,
  val totalQuestions: Int = 0,
  val totalNotes: Int = 0,
  val isPublished: Boolean = true,
  val orderIndex: Int = 0
)

@Entity(tableName = "topics")
data class TopicEntity(
  @PrimaryKey val id: String,
  val courseId: String,
  val title: String,
  val description: String = "",
  val orderIndex: Int = 0
)

@Entity(tableName = "questions")
data class QuestionEntity(
  @PrimaryKey val id: String,
  val courseId: String,
  val topicId: String,
  val questionText: String,
  val optionA: String,
  val optionB: String,
  val optionC: String,
  val optionD: String,
  val correctAnswer: String, // "A", "B", "C", "D"
  val explanationGeneral: String,
  val explanationA: String,
  val explanationB: String,
  val explanationC: String,
  val explanationD: String,
  val difficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
  val isPastExam: Boolean = false,
  val examYear: String? = null,
  val examType: String? = null,
  val isVerified: Boolean = true,
  val isPublished: Boolean = true
)

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey val id: String,
  val courseId: String,
  val topicId: String,
  val title: String,
  val summary: String,
  val contentMarkdown: String,
  val keyFormulas: String = "",
  val readTimeMinutes: Int = 5,
  val isPublished: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "past_exams")
data class PastExamEntity(
  @PrimaryKey val id: String,
  val courseId: String,
  val university: String = "Addis Ababa University (AAU)",
  val academicYear: String,
  val semester: String,
  val examType: String, // "MIDTERM", "FINAL"
  val title: String,
  val instructions: String,
  val totalQuestions: Int = 10,
  val durationMinutes: Int = 60,
  val isDigitized: Boolean = true,
  val isVerified: Boolean = true,
  val documentSummary: String = "",
  val isPublished: Boolean = true
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val courseId: String,
  val courseTitle: String,
  val topicId: String?,
  val topicTitle: String?,
  val examId: String?,
  val isExamMode: Boolean,
  val totalQuestions: Int,
  val correctCount: Int,
  val incorrectCount: Int,
  val scorePercent: Int,
  val timeSpentSeconds: Int,
  val timestamp: Long = System.currentTimeMillis(),
  val answersJson: String = "" // Serialized answer log: questionId|selected|correct|isCorrect
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val itemType: String, // "QUESTION", "NOTE"
  val itemId: String,
  val courseId: String,
  val courseTitle: String,
  val title: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
  @PrimaryKey val id: String,
  val title: String,
  val message: String,
  val category: String = "GENERAL", // "EXAM", "ALERT", "NOTE", "GENERAL"
  val targetCourseId: String? = null,
  val targetCourseTitle: String? = null,
  val isUrgent: Boolean = false,
  val isPublished: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @PrimaryKey val key: String,
  val value: String
)

@Entity(tableName = "access_codes")
data class AccessCodeEntity(
  @PrimaryKey val code: String, // 6 characters, e.g. "739104", "8K#92X"
  val createdAt: Long = System.currentTimeMillis(),
  val expiresAt: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000L),
  val createdByAdminId: String = "admin_1",
  val isRedeemed: Boolean = false,
  val redeemedByUserId: String? = null,
  val redeemedByEmail: String? = null,
  val redeemedAt: Long? = null,
  val note: String = "Permanent Academic Access Pass"
)

