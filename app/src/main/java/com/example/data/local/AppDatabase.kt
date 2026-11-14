package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
  version = 5,
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

    private fun getExistingColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
      val cols = mutableSetOf<String>()
      db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
          if (nameIdx >= 0) cols.add(cursor.getString(nameIdx))
        }
      }
      return cols
    }

    private fun addColumnIfMissing(
      db: SupportSQLiteDatabase,
      tableName: String,
      existingCols: Set<String>,
      columnName: String,
      definition: String
    ) {
      if (columnName !in existingCols) {
        db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $definition")
      }
    }

    private fun migrateSchemaToV5(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `topics` (
          `id` TEXT NOT NULL,
          `courseId` TEXT NOT NULL,
          `title` TEXT NOT NULL,
          `description` TEXT NOT NULL DEFAULT '',
          `orderIndex` INTEGER NOT NULL DEFAULT 0,
          PRIMARY KEY(`id`)
        )
        """.trimIndent()
      )
      db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `access_codes` (
          `code` TEXT NOT NULL,
          `createdAt` INTEGER NOT NULL DEFAULT 0,
          `expiresAt` INTEGER NOT NULL DEFAULT 0,
          `createdByAdminId` TEXT NOT NULL DEFAULT 'admin_1',
          `isRedeemed` INTEGER NOT NULL DEFAULT 0,
          `redeemedByUserId` TEXT,
          `redeemedByEmail` TEXT,
          `redeemedAt` INTEGER,
          `note` TEXT NOT NULL DEFAULT 'Permanent Academic Access Pass',
          PRIMARY KEY(`code`)
        )
        """.trimIndent()
      )

      val userCols = getExistingColumns(db, "users")
      if (userCols.isNotEmpty()) {
        addColumnIfMissing(db, "users", userCols, "password", "TEXT NOT NULL DEFAULT 'student123'")
        addColumnIfMissing(db, "users", userCols, "studyGoalMinutesPerDay", "INTEGER NOT NULL DEFAULT 45")
        addColumnIfMissing(db, "users", userCols, "isPremium", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "users", userCols, "activationCode", "TEXT")
        addColumnIfMissing(db, "users", userCols, "activatedAt", "INTEGER")
        addColumnIfMissing(db, "users", userCols, "createdAt", "INTEGER NOT NULL DEFAULT 0")
      }

      val courseCols = getExistingColumns(db, "courses")
      if (courseCols.isNotEmpty()) {
        addColumnIfMissing(db, "courses", courseCols, "totalQuestions", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "courses", courseCols, "totalNotes", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "courses", courseCols, "isPublished", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "courses", courseCols, "orderIndex", "INTEGER NOT NULL DEFAULT 0")
      }

      val noteCols = getExistingColumns(db, "notes")
      if (noteCols.isNotEmpty()) {
        addColumnIfMissing(db, "notes", noteCols, "topicId", "TEXT NOT NULL DEFAULT 'T_GEN'")
        addColumnIfMissing(db, "notes", noteCols, "keyFormulas", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "notes", noteCols, "chapterNumber", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "notes", noteCols, "readTimeMinutes", "INTEGER NOT NULL DEFAULT 5")
        addColumnIfMissing(db, "notes", noteCols, "isPublished", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "notes", noteCols, "createdAt", "INTEGER NOT NULL DEFAULT 0")
      }

      val examCols = getExistingColumns(db, "past_exams")
      if (examCols.isNotEmpty()) {
        addColumnIfMissing(db, "past_exams", examCols, "university", "TEXT NOT NULL DEFAULT 'Addis Ababa University (AAU)'")
        addColumnIfMissing(db, "past_exams", examCols, "isDigitized", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "past_exams", examCols, "isVerified", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "past_exams", examCols, "documentSummary", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "past_exams", examCols, "isPublished", "INTEGER NOT NULL DEFAULT 1")
      }

      val questionCols = getExistingColumns(db, "questions")
      if (questionCols.isNotEmpty()) {
        addColumnIfMissing(db, "questions", questionCols, "topicId", "TEXT NOT NULL DEFAULT 'T_GEN'")
        addColumnIfMissing(db, "questions", questionCols, "difficulty", "TEXT NOT NULL DEFAULT 'MEDIUM'")
        addColumnIfMissing(db, "questions", questionCols, "isPastExam", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "questions", questionCols, "examYear", "TEXT")
        addColumnIfMissing(db, "questions", questionCols, "examType", "TEXT")
        addColumnIfMissing(db, "questions", questionCols, "isVerified", "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, "questions", questionCols, "isPublished", "INTEGER NOT NULL DEFAULT 1")
      }

      val annCols = getExistingColumns(db, "announcements")
      if (annCols.isNotEmpty()) {
        addColumnIfMissing(db, "announcements", annCols, "category", "TEXT NOT NULL DEFAULT 'GENERAL'")
        addColumnIfMissing(db, "announcements", annCols, "targetCourseId", "TEXT")
        addColumnIfMissing(db, "announcements", annCols, "targetCourseTitle", "TEXT")
        addColumnIfMissing(db, "announcements", annCols, "isUrgent", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "announcements", annCols, "isPublished", "INTEGER NOT NULL DEFAULT 1")
      }
    }

    val MIGRATION_1_5 = object : Migration(1, 5) {
      override fun migrate(db: SupportSQLiteDatabase) = migrateSchemaToV5(db)
    }
    val MIGRATION_2_5 = object : Migration(2, 5) {
      override fun migrate(db: SupportSQLiteDatabase) = migrateSchemaToV5(db)
    }
    val MIGRATION_3_5 = object : Migration(3, 5) {
      override fun migrate(db: SupportSQLiteDatabase) = migrateSchemaToV5(db)
    }
    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) = migrateSchemaToV5(db)
    }

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "tts_academy_database"
        )
          .addCallback(DatabaseCallback(scope))
          .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
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
