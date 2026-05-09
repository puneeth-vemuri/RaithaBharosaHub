package com.raithabharosahub.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raithabharosahub.data.local.dao.FarmerDao
import com.raithabharosahub.data.local.dao.PlotDao
import com.raithabharosahub.data.local.dao.NpkDao
import com.raithabharosahub.data.local.dao.SeasonDao
import com.raithabharosahub.data.repository.AuthRepository
import com.raithabharosahub.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents all possible states of the authentication UI.
 */
sealed class AuthUiState {
    /** No auth operation in progress. */
    object Idle : AuthUiState()

    /** Auth operation running — show CircularProgressIndicator. */
    object Loading : AuthUiState()

    /**
     * Auth succeeded.
     * @param user The authenticated Firebase user.
     * @param navigateToDashboard True → Dashboard, False → Onboarding.
     */
    data class Success(
        val user: FirebaseUser,
        val navigateToDashboard: Boolean
    ) : AuthUiState()

    /**
     * Auth failed.
     * @param message Human-readable error to display in a Snackbar.
     */
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel for [LoginScreen].
 *
 * Uses [AuthRepository] for all Firebase Auth calls and [FarmerDao] to decide
 * post-login destination: if a FarmerEntity already exists the user has completed
 * onboarding and goes straight to Dashboard; otherwise they go to Onboarding.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val farmerDao: FarmerDao,
    private val plotDao: PlotDao,
    private val npkDao: NpkDao,
    private val seasonDao: SeasonDao,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Reset state back to Idle (e.g. after consuming a Success or Error). */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signInWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithEmail(email.trim(), password)
                .onSuccess { user -> handleAuthSuccess(user) }
                .onFailure { e -> _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign-in failed") }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.registerWithEmail(email.trim(), password)
                .onSuccess { user -> handleAuthSuccess(user) }
                .onFailure { e -> _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Registration failed") }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user -> handleAuthSuccess(user) }
                .onFailure { e -> _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Google sign-in failed") }
        }
    }

    /**
     * After any successful auth, check whether the local Room DB already has a
     * FarmerEntity. If yes, onboarding is done → Dashboard. If no → Onboarding.
     *
     * NOTE: This uses count() because FarmerEntity has no uid column and this
     * app is single-farmer-per-device. A uid column + DB migration can be added
     * in a future release.
     */
    private suspend fun handleAuthSuccess(user: FirebaseUser) {
        try {
            val userData = firestoreRepository.loadUserData()
            if (userData != null && userData.farmer != null && userData.plot != null) {
                farmerDao.insert(userData.farmer)
                plotDao.insert(userData.plot)
                userData.npkHistory.forEach { npkDao.insert(it) }
                userData.seasonHistory.forEach { seasonDao.insert(it) }
            }
        } catch (e: Exception) {
            // Ignore error
        }

        val farmerExists = try {
            farmerDao.count() > 0
        } catch (e: Exception) {
            false
        }
        _uiState.value = AuthUiState.Success(
            user = user,
            navigateToDashboard = farmerExists
        )
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = AuthUiState.Error("Email cannot be empty")
                false
            }
            password.length < 6 -> {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}
