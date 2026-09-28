package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BonEntity
import com.example.data.model.CalendarDayEntity
import com.example.data.model.FixedPostEntity
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.QuinzaineEntity
import com.example.data.model.TransportEntity
import com.example.data.model.WorkerGroupEntity

@Database(
    entities = [
        QuinzaineEntity::class,
        WorkerGroupEntity::class,
        HourlyWorkerEntity::class,
        FixedPostEntity::class,
        CalendarDayEntity::class,
        TransportEntity::class,
        BonEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quinzaineDao(): QuinzaineDao
    abstract fun workerGroupDao(): WorkerGroupDao
    abstract fun hourlyWorkerDao(): HourlyWorkerDao
    abstract fun fixedPostDao(): FixedPostDao
    abstract fun calendarDayDao(): CalendarDayDao
    abstract fun transportDao(): TransportDao
    abstract fun bonDao(): BonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prevision_paie_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
