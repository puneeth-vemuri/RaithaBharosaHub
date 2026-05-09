package com.raithabharosahub.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Repository interface for Firebase Authentication operations.
 */
interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    fun signOut()
    fun getCurrentUser(): FirebaseUser?
    fun isLoggedIn(): Boolean
}

/**
 * Firebase implementation of [AuthRepository].
 *
 * NOTE: For Google Sign-In to work you must:
 *   1. Add the app's SHA-1 fingerprint in Firebase Console → Project Settings → Android App.
 *   2. Enable Google as a sign-in provider in Firebase Console → Authentication → Sign-in method.
 *   3. Re-download google-services.json and replace the existing file.
 *   4. Set FIREBASE_WEB_CLIENT_ID in local.properties (from Firebase Console → OAuth 2.0 Web client).
 */
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user ?: error("Sign-in succeeded but user is null")
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user ?: error("Registration succeeded but user is null")
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        result.user ?: error("Google sign-in succeeded but user is null")
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
