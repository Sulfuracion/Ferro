package com.davidguijo.ferro.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroEmptyScaffold
import kotlinx.serialization.Serializable

/** Detalle de una sesión pasada del historial. */
@Serializable
data class SessionDetailRoute(val workoutId: String)

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FerroEmptyScaffold(
        title = stringResource(R.string.session_detail_title),
        emptyTitle = stringResource(R.string.session_detail_empty_title),
        emptyDescription = stringResource(R.string.session_detail_empty_description),
        emptyIcon = painterResource(R.drawable.ic_calendar_month),
        onBack = onBack,
        modifier = modifier
    )
}
