package com.sunitha.fittrack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunitha.fittrack.data.db.dao.AiFoodCacheDao
import com.sunitha.fittrack.data.db.dao.AiInsightDao
import com.sunitha.fittrack.data.db.dao.FoodDao
import com.sunitha.fittrack.data.db.dao.PeriodDao
import com.sunitha.fittrack.data.db.dao.RestDayDao
import com.sunitha.fittrack.data.db.dao.StepDao
import com.sunitha.fittrack.data.db.dao.WeightDao
import com.sunitha.fittrack.data.db.dao.WorkoutDao
import com.sunitha.fittrack.data.db.entities.AiFoodCacheEntry
import com.sunitha.fittrack.data.db.entities.AiInsight
import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.db.entities.PeriodEntry
import com.sunitha.fittrack.data.db.entities.RestDayEntry
import com.sunitha.fittrack.data.db.entities.StepEntry
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet

@Database(
    entities = [
        WorkoutSession::class,
        WorkoutSet::class,
        FoodEntry::class,
        WeightEntry::class,
        AiInsight::class,
        StepEntry::class,
        RestDayEntry::class,
        AiFoodCacheEntry::class,
        PeriodEntry::class
    ],
    version = 8,
    exportSchema = true
)
abstract class FitTrackDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun foodDao(): FoodDao
    abstract fun weightDao(): WeightDao
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun stepDao(): StepDao
    abstract fun restDayDao(): RestDayDao
    abstract fun aiFoodCacheDao(): AiFoodCacheDao
    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile private var INSTANCE: FitTrackDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `step_entries` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`dateMillis` INTEGER NOT NULL, `steps` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rest_day_entries` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`dateMillis` INTEGER NOT NULL, `note` TEXT NOT NULL DEFAULT '')"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_food_cache` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`foodNameKey` TEXT NOT NULL, `name` TEXT NOT NULL, `serving` TEXT NOT NULL, " +
                    "`calories` INTEGER NOT NULL, `proteinG` REAL NOT NULL, " +
                    "`carbsG` REAL NOT NULL, `fatG` REAL NOT NULL, `category` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_food_cache_foodNameKey` " +
                    "ON `ai_food_cache` (`foodNameKey`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `period_entries` " +
                    "(`dateMillis` INTEGER PRIMARY KEY NOT NULL, " +
                    "`note` TEXT NOT NULL DEFAULT '')"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE step_entries ADD COLUMN isWalkDay INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_entries ADD COLUMN fiberG REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ai_food_cache ADD COLUMN fiberG REAL NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_entries ADD COLUMN servings REAL NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): FitTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
