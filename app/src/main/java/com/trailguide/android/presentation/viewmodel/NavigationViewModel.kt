package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.trailguide.android.data.model.NavigationState
import com.trailguide.android.data.repository.NavigationRepository
import com.trailguide.android.data.repository.NavigationStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    navigationStateHolder: NavigationStateHolder,
    private val navigationRepository: NavigationRepository
) : ViewModel() {

    val navigationState: StateFlow<NavigationState> = navigationStateHolder.state

    fun getActiveSessions() = navigationRepository.getActiveSessions()
}

