package com.davidguijo.ferro.feature.body

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BodyMeasurementsUiState(val measurements: List<String> = emptyList())

@HiltViewModel
class BodyMeasurementsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(BodyMeasurementsUiState())
    val uiState: StateFlow<BodyMeasurementsUiState> = _uiState.asStateFlow()
}
