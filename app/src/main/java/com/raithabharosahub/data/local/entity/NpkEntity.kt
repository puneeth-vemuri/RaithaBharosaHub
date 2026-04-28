package com.raithabharosahub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * NpkEntity stores soil test results for Nitrogen, Phosphorus, Potassium.
 * Personal data in this table is encrypted using SQLCipher.
 */
@Entity(
    tableName = "npk",
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
        Index(value = ["plot_id", "test_date"], unique = false)
    ]
)
data class NpkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "plot_id", index = true)
    val plotId: Long,

    @ColumnInfo(name = "nitrogen")
    val nitrogen: Float, // Nitrogen content in kg/ha

    @ColumnInfo(name = "phosphorus")
    val phosphorus: Float, // Phosphorus content in kg/ha

    @ColumnInfo(name = "potassium")
    val potassium: Float, // Potassium content in kg/ha

    @ColumnInfo(name = "test_date")
    val testDate: Date,

    @ColumnInfo(name = "lab_name")
    val labName: String
) {
    companion object {
        const val TABLE_NAME = "npk"
    }
}