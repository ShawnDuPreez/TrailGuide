package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.repository.TranslationRepository
import com.trailguide.android.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing real-time translations across the app.
 */
@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()
    
    // Current selected language from preferences
    val currentLanguage = preferencesRepository.userPreferencesFlow
        .map { it.language.code }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "en"
        )
    
    init {
        checkApiConfiguration()
    }
    
    /**
     * Check if Google Translate API is configured.
     */
    private fun checkApiConfiguration() {
        val isConfigured = translationRepository.isConfigured()
        _uiState.update { it.copy(apiConfigured = isConfigured) }
    }
    
    /**
     * Translate a single text to the current user's language.
     */
    fun translateToUserLanguage(
        text: String,
        sourceLanguage: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, error = null) }
            
            val targetLanguage = currentLanguage.value
            
            translationRepository.translateText(text, targetLanguage, sourceLanguage)
                .onSuccess { translated ->
                    _uiState.update { 
                        it.copy(
                            isTranslating = false,
                            lastTranslation = translated
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isTranslating = false,
                            error = error.message ?: "Translation failed"
                        ) 
                    }
                }
        }
    }
    
    /**
     * Translate batch of texts.
     */
    fun translateBatch(
        texts: List<String>,
        targetLanguage: String? = null,
        sourceLanguage: String? = null,
        onComplete: (List<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, error = null) }
            
            val target = targetLanguage ?: currentLanguage.value
            
            translationRepository.translateBatch(texts, target, sourceLanguage)
                .onSuccess { translations ->
                    _uiState.update { it.copy(isTranslating = false) }
                    onComplete(translations)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isTranslating = false,
                            error = error.message ?: "Batch translation failed"
                        ) 
                    }
                }
        }
    }
    
    /**
     * Detect language of text.
     */
    fun detectLanguage(text: String, onDetected: (String) -> Unit) {
        viewModelScope.launch {
            translationRepository.detectLanguage(text)
                .onSuccess { language ->
                    onDetected(language)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = "Language detection failed: ${error.message}") 
                    }
                }
        }
    }
    
    /**
     * Translate trail description.
     */
    fun translateTrailDescription(
        description: String,
        onTranslated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val targetLanguage = currentLanguage.value
            
            // Skip if already in English or same language
            if (targetLanguage == "en") {
                onTranslated(description)
                return@launch
            }
            
            translationRepository.translateText(description, targetLanguage, "en")
                .onSuccess { translated ->
                    onTranslated(translated)
                }
                .onFailure {
                    // Fallback to original if translation fails
                    onTranslated(description)
                }
        }
    }
    
    /**
     * Clear translation cache.
     */
    fun clearCache() {
        translationRepository.clearCache()
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for translation operations.
 */
data class TranslationUiState(
    val isTranslating: Boolean = false,
    val lastTranslation: String? = null,
    val error: String? = null,
    val apiConfigured: Boolean = false
)

