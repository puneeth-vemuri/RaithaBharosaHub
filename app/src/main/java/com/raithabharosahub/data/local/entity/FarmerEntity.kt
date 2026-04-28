package com.raithabharosahub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

/**
 * FarmerEntity represents a farmer profile in the database.
 * Personal data in this table is encrypted using SQLCipher.
 */
@Entity(
    tableName = "farmer",
    foreignKeys = []
)
data class FarmerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mobile")
    val mobile: String,

    @ColumnInfo(name = "primary_crop")
    val primaryCrop: String,

    @ColumnInfo(name = "district")
    val district: String,

    @ColumnInfo(name = "language_pref")
    val languagePref: String = "kn" // Default to Kannada
) {
    companion object {
        const val TABLE_NAME = "farmer"
    }
}