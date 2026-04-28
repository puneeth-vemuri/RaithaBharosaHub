package com.raithabharosahub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * SeasonEntity represents a cropping season for a specific plot.
 * Tracks sowing date, harvest date, yield, and notes.
 */
@Entity(
    tableName = "season",
    foreignKeys = [
        ForeignKey(
            entity = PlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["plot_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plot_id", "sow_date"], unique = false)
    ]
)
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "plot_id", index = true)
    val plotId: Long,

    @ColumnInfo(name = "crop")
    val crop: String, // e.g., "Paddy", "Ragi", "Sugarcane"

    @ColumnInfo(name = "sow_date")
    val sowDate: Date,

    @ColumnInfo(name = "harvest_date")
    val harvestDate: Date? = null, // Nullable for ongoing seasons

    @ColumnInfo(name = "yield_kg")
    val yieldKg: Float? = null, // Yield in kilograms, nullable if not harvested yet

    @ColumnInfo(name = "notes")
    val notes: String? = null // Optional notes
) {
    companion object {
        const val TABLE_NAME = "season"
    }
}