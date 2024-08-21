package se.miun.dajo1903.dt031g.bathingsites

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The AppDatabase class is the applications room database. It contains an array of entities and also
 * an Dao to manipulate the data.
 */
@Database(entities = [BathingSite::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun BathingSiteDao(): BathingSiteDao
}