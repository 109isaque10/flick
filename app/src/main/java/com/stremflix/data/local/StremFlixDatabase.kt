package com.stremflix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        ExternalIdEntity::class,
        MetadataCacheEntity::class,
        MediaMetadataEntity::class,
        WatchProgressEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class StremFlixDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun cacheDao(): CacheDao
    abstract fun mediaMetadataDao(): MediaMetadataDao
    abstract fun watchProgressDao(): WatchProgressDao

    companion object {
        @Volatile
        private var INSTANCE: StremFlixDatabase? = null

        fun getDatabase(context: Context): StremFlixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StremFlixDatabase::class.java,
                    "stremflix_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

