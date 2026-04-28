package com.raithabharosahub.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.raithabharosahub.domain.calculator.SowingIndexCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies (DataStore, generators, etc.)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "raitha_bharosa_preferences")

    /**
     * Provides Application Context for injection.
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    /**
     * Provides DataStore<Preferences> for storing app preferences.
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    /**
     * Provides SowingIndexCalculator for calculating sowing indices.
     */
    @Provides
    @Singleton
    fun provideSowingIndexCalculator(): SowingIndexCalculator {
        return SowingIndexCalculator()
    }
}