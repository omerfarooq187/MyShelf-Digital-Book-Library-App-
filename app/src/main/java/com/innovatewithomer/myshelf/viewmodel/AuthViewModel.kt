package com.innovatewithomer.myshelf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatewithomer.myshelf.data.remote.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _userState = MutableStateFlow<AuthState>(AuthState.Loading)
    val userState = _userState.asStateFlow()

    init {
        val user = authRepository.currentUser
        if (user != null) {
            _userState.value = AuthState.Authenticated(user.uid)
        } else {
            signInAnonymously()
        }
    }

    private fun signInAnonymously() {
        viewModelScope.launch {
            _userState.value = AuthState.Loading
            val result = authRepository.signInAnonymously()
            if (result.isSuccess) {
                _userState.value = AuthState.Authenticated(result.getOrThrow().uid)
            } else {
                _userState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}

sealed class AuthState {
    data object Loading: AuthState()
    data class Authenticated(val userId: String): AuthState()
    data class Error(val message: String): AuthState()
    data object unAuthenticated: AuthState()
}