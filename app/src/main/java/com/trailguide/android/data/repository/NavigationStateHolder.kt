package com.trailguide.android.data.repository

import com.trailguide.android.data.model.NavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val state: StateFlow<NavigationState> = _state

    fun update(state: NavigationState) {
        _state.value = state
    }
}

