package com.raithabharosahub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * WeatherEntity stores a single 3-hourly forecast snapshot for one plot.
 *
 * Schema notes:
 *  - `plot_id` is a FK → PlotEntity(id), CASCADE delete/update.
 *  - `(plot_id, date)` is a UNIQUE index so REPLACE conflict-strategy on insert
 *    acts as an upsert rather than creating duplicate rows.
 *  - `fetched_at` records when the API (or mock) call completed — used by the
 *    repository to skip redundant network requests within a short window.
 *  - `last_updated_at` is the canonical "data freshness" timestamp exposed to the UI
 *    so screens can display "Last updated 5 min ago" style staleness indicators.
 *    Distinct from `fetched_at`: fetched_at is set per-entity, last_updated_at is
 *    the same value across all entities written in one refresh batch.
 *
 * DB version: 2  (added last_updated_at — see AppDatabase migration)
 */
@Entity(
    tableName = "weather",
    foreignKeys = [
        ForeignKey(
            entity = PlotEntity::class,
            parentColumns = ["id"],
            childColumns  = ["plot_id"],
            onDelete      = ForeignKey.CASCADE,
            onUpdate      = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plot_id", "date"], unique = true)
    ]
)
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** FK to PlotEntity — which farmland plot this forecast belongs to. */
    @ColumnInfo(name = "plot_id", index = true)
    val plotId: Long,

    /** UTC timestamp of the 3-hourly forecast slot (from OWM `dt` field × 1000). */
    @ColumnInfo(name = "date")
    val date: Date,

    /** Rainfall accumulation over the 3-hour slot, in mm. 0f when dry. */
    @ColumnInfo(name = "rain_mm")
    val rainMm: Float,

    /** Maximum temperature in the slot, in °C. */
    @ColumnInfo(name = "temp_max")
    val tempMax: Float,

    /** Relative humidity percentage (0–100). */
    @ColumnInfo(name = "humidity")
    val humidity: Float,

    /**
     * Timestamp of the individual API/mock call that produced this row.
     * Set to Date() at the moment the repository writes to Room.
     */
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Date,

    /**
     * "Batch freshness" timestamp — the same value is written to every entity
     * in one refresh cycle. The UI uses this to show "Last updated X ago".
     * Added in DB schema version 2.
     */
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Date
) {
    companion object {
        const val TABLE_NAME = "weather"
    }
}