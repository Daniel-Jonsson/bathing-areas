package se.miun.dajo1903.dt031g.bathingsites

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Unique column was learnt from:
 * https://stackoverflow.com/questions/48962106/add-unique-constraint-in-room-database-to-multiple-column
 *
 * The BathingSite is a data class representing an entity in the room database. It contains a number
 * of columns that represent data in this entity. The entity also provides functionality to make the
 * long and latitude unique.
 *
 * @author Daniel Jönsson
 * @see Entity
 */
@Entity(indices = [Index(value = ["latitude", "longitude"], unique = true)])
data class BathingSite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description", defaultValue = "Not Provided") val desc: String?,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "latitude") var latitude: String,
    @ColumnInfo(name = "longitude") var longitude: String,
    @ColumnInfo(name = "grade", defaultValue = "Not provided") val grade: String,
    @ColumnInfo(name = "water_temp", defaultValue = "Not provided") val waterTemp: String?,
    @ColumnInfo(name = "date_for_temp", defaultValue = "Not provided") val dateForTemp: String?
)

