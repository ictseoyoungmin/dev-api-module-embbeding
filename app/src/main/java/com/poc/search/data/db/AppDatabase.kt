package com.poc.search.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocalImageEntity::class, LocalInstanceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): LocalImageDao
    abstract fun instanceDao(): LocalInstanceDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "search_poc.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
