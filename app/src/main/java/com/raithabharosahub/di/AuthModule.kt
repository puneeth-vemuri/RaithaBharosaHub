package com.raithabharosahub.di

import com.google.firebase.auth.FirebaseAuth
import com.raithabharosahub.data.repository.AuthRepository
import com.raithabharosahub.data.repository.FirebaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase Authentication dependencies:
 *  - [FirebaseAuth] singleton instance
 *  - [AuthRepository] bound to [FirebaseAuthRepository]
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * Binds [FirebaseAuthRepository] as the concrete [AuthRepository] implementation.
     * Hilt will inject this wherever AuthRepository is required.
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    companion object {
        /**
         * Provides the FirebaseAuth singleton.
         * FirebaseAuth.getInstance() is itself a process-level singleton so
         * wrapping it here is safe and idempotent.
         */
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        /**
         * Provides the FirebaseFirestore singleton.
         */
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): com.google.firebase.firestore.FirebaseFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    }
}
