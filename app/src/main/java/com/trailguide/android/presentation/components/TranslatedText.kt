package com.trailguide.android.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.presentation.viewmodel.TranslationViewModel

/**
 * A Text composable that automatically translates its content
 * based on the user's language preference.
 */
@Composable
fun TranslatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    sourceLanguage: String? = "en",
    translationViewModel: TranslationViewModel = hiltViewModel()
) {
    var translatedText by remember(text) { mutableStateOf(text) }
    val currentLanguage by translationViewModel.currentLanguage.collectAsState()
    val uiState by translationViewModel.uiState.collectAsState()
    
    // Translate when language or text changes
    LaunchedEffect(text, currentLanguage) {
        if (currentLanguage != "en" && text.isNotBlank() && uiState.apiConfigured) {
            translationViewModel.translateTrailDescription(text) { translated ->
                translatedText = translated
            }
        } else {
            translatedText = text
        }
    }
    
    Text(
        text = translatedText,
        modifier = modifier,
        style = style,
        color = color
    )
}

