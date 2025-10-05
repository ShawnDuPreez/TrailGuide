package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication (login/register)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    fun setName(value: String) {
        _name.value = value
        _errorMessage.value = null
    }
    
    fun setEmail(value: String) {
        _email.value = value
        _errorMessage.value = null
    }
    
    fun setPassword(value: String) {
        _password.value = value
        _errorMessage.value = null
    }
    
    fun setConfirmPassword(value: String) {
        _confirmPassword.value = value
        _errorMessage.value = null
    }
    
    fun login() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            authRepository.signInWithEmail(_email.value, _password.value).collect { result ->
                when (result) {
                    is com.trailguide.android.data.remote.NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                    is com.trailguide.android.data.remote.NetworkResult.Success -> {
                        _isAuthenticated.value = true
                        _isLoading.value = false
                    }
                    is com.trailguide.android.data.remote.NetworkResult.Error -> {
                        _errorMessage.value = result.message
                        _isLoading.value = false
                    }
                }
            }
        }
    }
    
    fun register() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            if (_password.value != _confirmPassword.value) {
                _errorMessage.value = "Passwords don't match"
                _isLoading.value = false
                return@launch
            }
            
            authRepository.registerWithEmail(_email.value, _password.value, _name.value).collect { result ->
                when (result) {
                    is com.trailguide.android.data.remote.NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                    is com.trailguide.android.data.remote.NetworkResult.Success -> {
                        _isAuthenticated.value = true
                        _isLoading.value = false
                    }
                    is com.trailguide.android.data.remote.NetworkResult.Error -> {
                        _errorMessage.value = result.message
                        _isLoading.value = false
                    }
                }
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

