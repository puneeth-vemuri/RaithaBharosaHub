package com.raithabharosahub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * WeatherEntity stores weather data for a specific plot.
 * Each entry corresponds to a date/time snapshot.
 */
@Entity(
    tableName = "weather",
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
        Index(value = ["plot_id", "date"], unique = true)
    ]
)
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "plot_id", index = true)
    val plotId: Long,

    @ColumnInfo(name = "date")
    val date: Date,

    @ColumnInfo(name = "rain_mm")
    val rainMm: Float, // Rainfall in mm

    @ColumnInfo(name = "temp_max")
    val tempMax: Float, // Maximum temperature in °C

    @ColumnInfo(name = "humidity")
    val humidity: Float, // Humidity percentage

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Date // When this data was fetched from API/mock
) {
    companion object {
        const val TABLE_NAME = "weather"
    }
}