package com.davidguijo.ferro.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroEmptyScaffold

@Composable
fun ProgressScreen(modifier: Modifier = Modifier, viewModel: ProgressViewModel = hiltViewModel()) {
    @Suppress("UNUSED_VARIABLE")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FerroEmptyScaffold(
        title = stringResource(R.string.progress_title),
        emptyTitle = stringResource(R.string.progress_empty_title),
        emptyDescription = stringResource(R.string.progress_empty_description),
        emptyIcon = painterResource(R.drawable.ic_bar_chart),
        modifier = modifier
    )
}
