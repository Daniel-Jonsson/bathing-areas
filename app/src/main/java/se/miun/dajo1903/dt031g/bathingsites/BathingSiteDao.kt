package se.miun.dajo1903.dt031g.bathingsites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The BathingSiteDao is a Data-access-object in which data can be manipulated/fetched from the room
 * database.
 */
@Dao
interface BathingSiteDao {

    /**
     * Gets all the bathingsites from the database and returns it as a list.
     */
    @Query("SELECT * FROM BathingSite")
    suspend fun getBathingSites(): List<BathingSite>

    /**
     * Insert a single bathing site, the onConflict specifies that the process should be aborted
     * if a conflict occurs. Such as not unique long/lat coordinates.
     *
     * @param bathingSite The bathing site to insert
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOne(bathingSite: BathingSite)

    /**
     * Insets a list of bathing sites, the onConflict specifies that the bathing site that caused the
     * conflict should be ignored and not inserted.
     *
     * @param bathingSites The list of bathing sites to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMany(bathingSites: List<BathingSite>)

}