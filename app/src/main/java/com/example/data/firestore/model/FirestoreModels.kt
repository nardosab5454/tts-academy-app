package com.example.data.firestore.model

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
  val enrolledCourses: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

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
