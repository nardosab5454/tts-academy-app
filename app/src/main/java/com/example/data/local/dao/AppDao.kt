package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.BookmarkEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity
import com.example.data.local.model.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  suspend fun getUserById(userId: String): UserEntity?

  @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
  suspend fun getUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  fun getAllUsersFlow(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY createdAt DESC")
  suspend fun getAllUsers(): List<UserEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Update
  suspend fun updateUser(user: UserEntity)

  @Query("UPDATE users SET questionsAnsweredCount = questionsAnsweredCount + :questions, correctAnswersCount = correctAnswersCount + :correct WHERE id = :userId")
  suspend fun incrementUserStats(userId: String, questions: Int, correct: Int)

  @Query("UPDATE users SET role = :newRole WHERE id = :userId")
  suspend fun updateUserRole(userId: String, newRole: String)

  @Query("UPDATE users SET isPremium = :isPremium, activationCode = :code, activatedAt = :activatedAt WHERE id = :userId")
  suspend fun updateUserPremium(userId: String, isPremium: Boolean, code: String, activatedAt: Long)

  @Query("UPDATE users SET department = :department, studyGoalMinutesPerDay = :dailyGoalMinutes WHERE id = :userId")
  suspend fun updateUserDepartmentAndGoal(userId: String, department: String, dailyGoalMinutes: Int)

  @Query("DELETE FROM users WHERE id = :userId")
  suspend fun deleteUserById(userId: String)
}

@Dao
interface CourseDao {
  @Query("SELECT * FROM courses WHERE isPublished = 1 ORDER BY orderIndex ASC")
  fun getPublishedCoursesFlow(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses ORDER BY orderIndex ASC")
  fun getAllCoursesFlow(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
  suspend fun getCourseById(courseId: String): CourseEntity?

  @Query("SELECT * FROM courses")
  suspend fun getAllCourses(): List<CourseEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourse(course: CourseEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourses(courses: List<CourseEntity>)

  @Update
  suspend fun updateCourse(course: CourseEntity)

  @Query("DELETE FROM courses WHERE id = :courseId")
  suspend fun deleteCourse(courseId: String)

  @Query("DELETE FROM courses WHERE id NOT IN (:validIds)")
  suspend fun deleteCoursesNotIn(validIds: List<String>)

  @Query("UPDATE courses SET totalQuestions = (SELECT COUNT(*) FROM questions WHERE courseId = :courseId), totalNotes = (SELECT COUNT(*) FROM notes WHERE courseId = :courseId) WHERE id = :courseId")
  suspend fun recalculateCounts(courseId: String)

  @Query("SELECT COUNT(*) FROM courses")
  suspend fun getCount(): Int
}

@Dao
interface TopicDao {
  @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY orderIndex ASC")
  fun getTopicsForCourseFlow(courseId: String): Flow<List<TopicEntity>>

  @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY orderIndex ASC")
  suspend fun getTopicsForCourse(courseId: String): List<TopicEntity>

  @Query("SELECT * FROM topics")
  suspend fun getAllTopics(): List<TopicEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopics(topics: List<TopicEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopic(topic: TopicEntity)

  @Query("DELETE FROM topics WHERE id = :topicId")
  suspend fun deleteTopic(topicId: String)

  @Query("DELETE FROM topics WHERE courseId = :courseId")
  suspend fun deleteTopicsForCourse(courseId: String)

  @Query("DELETE FROM topics WHERE id NOT IN (:validIds)")
  suspend fun deleteTopicsNotIn(validIds: List<String>)

  @Query("SELECT * FROM topics ORDER BY orderIndex ASC")
  fun getAllTopicsFlow(): Flow<List<TopicEntity>>

  @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
  suspend fun getTopicById(topicId: String): TopicEntity?

  @Update
  suspend fun updateTopic(topic: TopicEntity)
}

@Dao
interface QuestionDao {
  @Query("SELECT * FROM questions WHERE topicId = :topicId AND isPublished = 1")
  suspend fun getQuestionsForTopic(topicId: String): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE courseId = :courseId AND isPublished = 1")
  suspend fun getQuestionsForCourse(courseId: String): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE courseId = :courseId AND isPastExam = 1 AND isPublished = 1")
  suspend fun getPastExamQuestionsForCourse(courseId: String): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE courseId = :courseId AND examYear = :year AND isPublished = 1")
  suspend fun getQuestionsForExamYear(courseId: String, year: String): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
  suspend fun getQuestionById(id: String): QuestionEntity?

  @Query("SELECT * FROM questions ORDER BY courseId ASC")
  fun getAllQuestionsFlow(): Flow<List<QuestionEntity>>

  @Query("SELECT * FROM questions")
  suspend fun getAllQuestions(): List<QuestionEntity>

  @Query("SELECT * FROM questions WHERE questionText LIKE '%' || :query || '%' OR explanationGeneral LIKE '%' || :query || '%'")
  suspend fun searchQuestions(query: String): List<QuestionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestions(questions: List<QuestionEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestion(question: QuestionEntity)

  @Update
  suspend fun updateQuestion(question: QuestionEntity)

  @Query("DELETE FROM questions WHERE id = :questionId")
  suspend fun deleteQuestion(questionId: String)

  @Query("DELETE FROM questions WHERE id NOT IN (:validIds)")
  suspend fun deleteQuestionsNotIn(validIds: List<String>)

  @Query("DELETE FROM questions WHERE courseId = :courseId")
  suspend fun deleteQuestionsForCourse(courseId: String)

  @Query("SELECT COUNT(*) FROM questions")
  suspend fun getCount(): Int

  @Query("SELECT COUNT(*) FROM questions WHERE courseId = :courseId")
  suspend fun getCountForCourse(courseId: String): Int
}

@Dao
interface NoteDao {
  @Query("SELECT * FROM notes WHERE courseId = :courseId AND isPublished = 1 ORDER BY createdAt ASC")
  fun getNotesForCourseFlow(courseId: String): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE isPublished = 1 ORDER BY createdAt DESC")
  fun getAllPublishedNotesFlow(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes ORDER BY createdAt DESC")
  fun getAllNotesFlow(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  suspend fun getNoteById(id: String): NoteEntity?

  @Query("SELECT * FROM notes WHERE courseId = :courseId")
  suspend fun getNotesForCourse(courseId: String): List<NoteEntity>

  @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%'")
  suspend fun searchNotes(query: String): List<NoteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotes(notes: List<NoteEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity)

  @Update
  suspend fun updateNote(note: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :noteId")
  suspend fun deleteNote(noteId: String)

  @Query("DELETE FROM notes WHERE id NOT IN (:validIds)")
  suspend fun deleteNotesNotIn(validIds: List<String>)

  @Query("DELETE FROM notes WHERE courseId = :courseId")
  suspend fun deleteNotesForCourse(courseId: String)

  @Query("SELECT COUNT(*) FROM notes")
  suspend fun getCount(): Int

  @Query("SELECT COUNT(*) FROM notes WHERE courseId = :courseId")
  suspend fun getCountForCourse(courseId: String): Int
}

@Dao
interface PastExamDao {
  @Query("SELECT * FROM past_exams WHERE courseId = :courseId AND isPublished = 1 ORDER BY academicYear DESC, examType ASC")
  fun getPastExamsForCourseFlow(courseId: String): Flow<List<PastExamEntity>>

  @Query("SELECT * FROM past_exams WHERE isPublished = 1 ORDER BY academicYear DESC")
  fun getAllPastExamsFlow(): Flow<List<PastExamEntity>>

  @Query("SELECT * FROM past_exams ORDER BY academicYear DESC")
  fun getAllAdminPastExamsFlow(): Flow<List<PastExamEntity>>

  @Query("SELECT * FROM past_exams WHERE id = :id LIMIT 1")
  suspend fun getPastExamById(id: String): PastExamEntity?

  @Query("SELECT * FROM past_exams WHERE courseId = :courseId")
  suspend fun getPastExamsForCourse(courseId: String): List<PastExamEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPastExams(exams: List<PastExamEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPastExam(exam: PastExamEntity)

  @Update
  suspend fun updatePastExam(exam: PastExamEntity)

  @Query("DELETE FROM past_exams WHERE id = :examId")
  suspend fun deletePastExam(examId: String)

  @Query("DELETE FROM past_exams WHERE id NOT IN (:validIds)")
  suspend fun deletePastExamsNotIn(validIds: List<String>)

  @Query("DELETE FROM past_exams WHERE courseId = :courseId")
  suspend fun deletePastExamsForCourse(courseId: String)

  @Query("SELECT COUNT(*) FROM past_exams")
  suspend fun getCount(): Int
}

@Dao
interface QuizAttemptDao {
  @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY timestamp DESC")
  fun getAttemptsForUserFlow(userId: String): Flow<List<QuizAttemptEntity>>

  @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY timestamp DESC")
  suspend fun getAttemptsForUser(userId: String): List<QuizAttemptEntity>

  @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND courseId = :courseId ORDER BY timestamp DESC")
  suspend fun getAttemptsForUserAndCourse(userId: String, courseId: String): List<QuizAttemptEntity>

  @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC LIMIT 20")
  fun getRecentGlobalAttemptsFlow(): Flow<List<QuizAttemptEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttempt(attempt: QuizAttemptEntity)
}

@Dao
interface BookmarkDao {
  @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY timestamp DESC")
  fun getBookmarksForUserFlow(userId: String): Flow<List<BookmarkEntity>>

  @Query("SELECT COUNT(*) FROM bookmarks WHERE userId = :userId AND itemId = :itemId")
  suspend fun isBookmarked(userId: String, itemId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: BookmarkEntity)

  @Query("DELETE FROM bookmarks WHERE userId = :userId AND itemId = :itemId")
  suspend fun deleteBookmark(userId: String, itemId: String)

  @Query("DELETE FROM bookmarks WHERE id = :id")
  suspend fun deleteBookmarkById(id: String)
}

@Dao
interface AnnouncementDao {
  @Query("SELECT * FROM announcements WHERE isPublished = 1 ORDER BY isUrgent DESC, createdAt DESC")
  fun getPublishedAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

  @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
  fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnnouncement(announcement: AnnouncementEntity)

  @Query("DELETE FROM announcements WHERE id = :id")
  suspend fun deleteAnnouncement(id: String)

  @Query("DELETE FROM announcements WHERE id NOT IN (:validIds)")
  suspend fun deleteAnnouncementsNotIn(validIds: List<String>)
}

@Dao
interface UserPreferenceDao {
  @Query("SELECT value FROM user_preferences WHERE `key` = :key LIMIT 1")
  suspend fun getPreference(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setPreference(pref: UserPreferenceEntity)

  @Query("SELECT `key` FROM user_preferences WHERE `key` LIKE 'dl_' || :userId || '_%' AND value = 'true'")
  fun getDownloadedKeysFlow(userId: String): Flow<List<String>>

  @Query("SELECT `key` FROM user_preferences WHERE `key` LIKE 'dl_%' AND value = 'true'")
  fun getAllDownloadedKeysFlow(): Flow<List<String>>

  @Query("DELETE FROM user_preferences WHERE `key` = :key")
  suspend fun deletePreference(key: String)
}

@Dao
interface AccessCodeDao {
  @Query("SELECT * FROM access_codes ORDER BY createdAt DESC")
  fun getAllAccessCodesFlow(): Flow<List<AccessCodeEntity>>

  @Query("SELECT * FROM access_codes WHERE code = :code LIMIT 1")
  suspend fun getAccessCode(code: String): AccessCodeEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccessCode(accessCode: AccessCodeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccessCodes(accessCodes: List<AccessCodeEntity>)

  @Update
  suspend fun updateAccessCode(accessCode: AccessCodeEntity)

  @Query("DELETE FROM access_codes WHERE code = :code")
  suspend fun deleteAccessCode(code: String)

  @Query("DELETE FROM access_codes WHERE code NOT IN (:validCodes)")
  suspend fun deleteAccessCodesNotIn(validCodes: List<String>)
}

