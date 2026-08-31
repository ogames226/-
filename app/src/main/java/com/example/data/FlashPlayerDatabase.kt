package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FlashGameEntity::class], version = 1, exportSchema = false)
abstract class FlashPlayerDatabase : RoomDatabase() {
    abstract fun flashGameDao(): FlashGameDao

    companion object {
        @Volatile
        private var INSTANCE: FlashPlayerDatabase? = null

        fun getDatabase(context: Context): FlashPlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlashPlayerDatabase::class.java,
                    "flash_player_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
