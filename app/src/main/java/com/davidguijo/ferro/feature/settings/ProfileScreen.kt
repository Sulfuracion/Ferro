package com.davidguijo.ferro.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
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
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        uiState = uiState,
        onOpenExerciseLibrary = onOpenExerciseLibrary,
        onOpenHistory = onOpenHistory,
        onOpenCalendar = onOpenCalendar,
        onOpenBodyMeasurements = onOpenBodyMeasurements,
        onOpenBackup = onOpenBackup,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
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
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: ProfileUiState = ProfileUiState()
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
            if (uiState.showBackupReminder) {
                BackupReminderBanner(
                    daysSinceLastBackup = uiState.daysSinceLastBackup,
                    onOpenBackup = onOpenBackup
                )
                Spacer(modifier = Modifier.height(spacing.space5))
            }

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
                FerroDivider()
                ProfileNavItem(
                    title = stringResource(R.string.profile_about_title),
                    subtitle = stringResource(R.string.profile_about_subtitle),
                    onClick = onOpenAbout
                )
            }

            Spacer(modifier = Modifier.height(spacing.space6))
        }
    }
}

@Composable
private fun BackupReminderBanner(daysSinceLastBackup: Int?, onOpenBackup: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val message =
        if (daysSinceLastBackup != null) {
            stringResource(R.string.profile_backup_reminder_stale, daysSinceLastBackup)
        } else {
            stringResource(R.string.profile_backup_reminder_never)
        }

    FerroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_backup_reminder_title),
                    style = FerroTheme.typography.bodyStrong,
                    color = colors.warning
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = message,
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }
            FerroButton(
                text = stringResource(R.string.profile_backup_reminder_action),
                onClick = onOpenBackup,
                variant = FerroButtonVariant.Secondary
            )
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
            uiState = ProfileUiState(showBackupReminder = true, daysSinceLastBackup = 42),
            onOpenExerciseLibrary = {},
            onOpenHistory = {},
            onOpenCalendar = {},
            onOpenBodyMeasurements = {},
            onOpenBackup = {},
            onOpenSettings = {},
            onOpenAbout = {}
        )
    }
}
