package se.miun.dajo1903.dt031g.bathingsites

import android.content.Context
import androidx.room.Room

/**
 * Singleton object which creates an instance of the app database. If the instance is currently null
 * it calls DatabaseBuilder::buildRoomDB with a provided context to build the database. If the room
 *
 *
 * @see Room
 */
object DatabaseBuilder {
    private var INSTANCE: AppDatabase? = null
    fun getInstance(context: Context) : AppDatabase {
        INSTANCE = INSTANCE ?: buildRoomDB(context)
        return INSTANCE as AppDatabase
    }

    private fun buildRoomDB(context: Context) =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "application-database"
            ).build()
}