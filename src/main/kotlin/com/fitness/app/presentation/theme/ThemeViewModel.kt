package com.fitness.app.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.repository.ThemeManager
import com.fitness.app.domain.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the app-wide theme state.
 * Exposes the current theme as a StateFlow and provides a function to change it.
 */
class ThemeViewModel(
    private val themeManager: ThemeManager
) : ViewModel() {

    val themeState: StateFlow<AppTheme> = themeManager.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeManager.setTheme(theme)
        }
    }
}
