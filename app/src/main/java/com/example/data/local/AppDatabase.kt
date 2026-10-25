package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccessCodeDao
import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.CourseDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.PastExamDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.dao.QuizAttemptDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.UserPreferenceDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    UserEntity::class,
    CourseEntity::class,
    TopicEntity::class,
    QuestionEntity::class,
    NoteEntity::class,
    PastExamEntity::class,
    QuizAttemptEntity::class,
    BookmarkEntity::class,
    AnnouncementEntity::class,
    UserPreferenceEntity::class,
    AccessCodeEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun courseDao(): CourseDao
  abstract fun topicDao(): TopicDao
  abstract fun questionDao(): QuestionDao
  abstract fun noteDao(): NoteDao
  abstract fun pastExamDao(): PastExamDao
  abstract fun quizAttemptDao(): QuizAttemptDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun announcementDao(): AnnouncementDao
  abstract fun userPreferenceDao(): UserPreferenceDao
  abstract fun accessCodeDao(): AccessCodeDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "tts_academy_database"
        )
          .addCallback(DatabaseCallback(scope))
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateDatabase(database)
          }
        }
      }
    }

    suspend fun populateDatabase(database: AppDatabase) {
      // Seed default users
      val userDao = database.userDao()
      SeedData.getDefaultUsers().forEach { userDao.insertUser(it) }

      // Seed courses
      val courseDao = database.courseDao()
      courseDao.insertCourses(SeedData.getCourses())

      // Seed topics
      val topicDao = database.topicDao()
      topicDao.insertTopics(SeedData.getTopics())

      // Seed questions
      val questionDao = database.questionDao()
      questionDao.insertQuestions(SeedData.getQuestions())

      // Seed notes
      val noteDao = database.noteDao()
      noteDao.insertNotes(SeedData.getNotes())

      // Seed past exams
      val pastExamDao = database.pastExamDao()
      pastExamDao.insertPastExams(SeedData.getPastExams())

      // Seed announcements
      val announcementDao = database.announcementDao()
      announcementDao.insertAnnouncements(SeedData.getAnnouncements())

      // Seed access codes
      val accessCodeDao = database.accessCodeDao()
      accessCodeDao.insertAccessCodes(SeedData.getAccessCodes())

      // Seed preferences
      val prefDao = database.userPreferenceDao()
      prefDao.setPreference(UserPreferenceEntity("active_user_id", ""))
      prefDao.setPreference(UserPreferenceEntity("app_theme", "SYSTEM"))
    }
  }
}
