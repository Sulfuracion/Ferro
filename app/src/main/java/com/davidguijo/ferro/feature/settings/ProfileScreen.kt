package com.davidguijo.ferro.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import kotlinx.serialization.Serializable

/** Pestaña «Perfil». */
@Serializable
data object ProfileRoute

@Composable
fun ProfileScreen(
    onOpenExerciseLibrary: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBodyMeasurements: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        onOpenExerciseLibrary = onOpenExerciseLibrary,
        onOpenHistory = onOpenHistory,
        onOpenCalendar = onOpenCalendar,
        onOpenBodyMeasurements = onOpenBodyMeasurements,
        onOpenBackup = onOpenBackup,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileContent(
    onOpenExerciseLibrary: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBodyMeasurements: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space4)
        ) {
            Text(
                text = stringResource(R.string.profile_section_data).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = spacing.space2)
            )
            FerroCard(contentPadding = PaddingValues(vertical = spacing.space1)) {
                ProfileNavItem(
                    title = stringResource(R.string.profile_exercises_title),
                    subtitle = stringResource(R.string.profile_exercises_subtitle),
                    onClick = onOpenExerciseLibrary
                )
                FerroDivider()
                ProfileNavItem(
                    title = stringResource(R.string.profile_history_title),
                    subtitle = stringResource(R.string.profile_history_subtitle),
                    onClick = onOpenHistory
                )
                FerroDivider()
                ProfileNavItem(
                    title = stringResource(R.string.profile_calendar_title),
                    subtitle = stringResource(R.string.profile_calendar_subtitle),
                    onClick = onOpenCalendar
                )
                FerroDivider()
                ProfileNavItem(
                    title = stringResource(R.string.profile_body_title),
                    subtitle = stringResource(R.string.profile_body_subtitle),
                    onClick = onOpenBodyMeasurements
                )
            }

            Spacer(modifier = Modifier.height(spacing.space6))

            Text(
                text = stringResource(R.string.profile_section_preferences).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = spacing.space2)
            )
            FerroCard(contentPadding = PaddingValues(vertical = spacing.space1)) {
                ProfileNavItem(
                    title = stringResource(R.string.profile_backup_title),
                    subtitle = stringResource(R.string.profile_backup_subtitle),
                    onClick = onOpenBackup
                )
                FerroDivider()
                ProfileNavItem(
                    title = stringResource(R.string.profile_settings_title),
                    subtitle = stringResource(R.string.profile_settings_subtitle),
                    onClick = onOpenSettings
                )
            }

            Spacer(modifier = Modifier.height(spacing.space6))
        }
    }
}

@Composable
private fun ProfileNavItem(title: String, subtitle: String, onClick: () -> Unit) {
    FerroListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(FerroTheme.sizes.iconMd),
                tint = FerroTheme.colors.inkMuted
            )
        }
    )
}

@FerroPreviews
@Composable
private fun ProfileScreenPreview() {
    FerroTheme {
        ProfileContent(
            onOpenExerciseLibrary = {},
            onOpenHistory = {},
            onOpenCalendar = {},
            onOpenBodyMeasurements = {},
            onOpenBackup = {},
            onOpenSettings = {}
        )
    }
}
