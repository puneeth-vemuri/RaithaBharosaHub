package com.raithabharosahub.di

import android.content.Context
import androidx.room.Room
import com.raithabharosahub.BuildConfig
import com.raithabharosahub.data.local.AppDatabase
import com.raithabharosahub.data.local.dao.FarmerDao
import com.raithabharosahub.data.local.dao.NpkDao
import com.raithabharosahub.data.local.dao.PlotDao
import com.raithabharosahub.data.local.dao.SeasonDao
import com.raithabharosahub.data.local.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing database and DAO dependencies.
 * Personal tables (farmer, npk) are encrypted using SQLCipher.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides SQLCipher passphrase for database encryption.
     */
    @Provides
    @Singleton
    @Named("databasePassphrase")
    fun provideEncryptionPassphrase(): String {
        val configuredPassphrase = BuildConfig.DATABASE_PASSPHRASE
        if (configuredPassphrase.isNotBlank()) {
            return configuredPassphrase
        }

        check(BuildConfig.DEBUG) {
            "DATABASE_PASSPHRASE must be configured for release builds"
        }

        return "dev_encryption_key_change_in_production"
    }

    /**
     * Provides SQLCipher SupportFactory for Room database encryption.
     */
    @Provides
    @Singleton
    fun provideSupportFactory(@Named("databasePassphrase") passphrase: String): SupportFactory {
        val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
        return SupportFactory(passphraseBytes)
    }

    /**
     * Provides the Room AppDatabase instance with SQLCipher encryption.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        factory: SupportFactory
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "raitha_bharosa.db"
        ).openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    /**
     * Provides FarmerDao from the database.
     */
    @Provides
    @Singleton
    fun provideFarmerDao(database: AppDatabase): FarmerDao = database.farmerDao()

    /**
     * Provides PlotDao from the database.
     */
    @Provides
    @Singleton
    fun providePlotDao(database: AppDatabase): PlotDao = database.plotDao()

    /**
     * Provides WeatherDao from the database.
     */
    @Provides
    @Singleton
    fun provideWeatherDao(database: AppDatabase): WeatherDao = database.weatherDao()

    /**
     * Provides NpkDao from the database.
     */
    @Provides
    @Singleton
    fun provideNpkDao(database: AppDatabase): NpkDao = database.npkDao()

    /**
     * Provides SeasonDao from the database.
     */
    @Provides
    @Singleton
    fun provideSeasonDao(database: AppDatabase): SeasonDao = database.seasonDao()
}