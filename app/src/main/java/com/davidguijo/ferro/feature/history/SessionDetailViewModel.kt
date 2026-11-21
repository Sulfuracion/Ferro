package com.davidguijo.ferro.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionDetailUiState(val workoutId: String = "")

@HiltViewModel
class SessionDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val route = savedStateHandle.toRoute<SessionDetailRoute>()
    private val _uiState = MutableStateFlow(SessionDetailUiState(workoutId = route.workoutId))
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()
}
