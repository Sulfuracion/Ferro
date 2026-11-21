package com.davidguijo.ferro.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import java.time.DayOfWeek

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.syncSystemLocale()
    }

    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onLanguageSelected = viewModel::onLanguageSelected,
        onWeightUnitSelected = viewModel::onWeightUnitSelected,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onDecreaseDefaultRest = viewModel::onDecreaseDefaultRest,
        onIncreaseDefaultRest = viewModel::onIncreaseDefaultRest,
        onFirstDayOfWeekSelected = viewModel::onFirstDayOfWeekSelected,
        onTimerSoundChanged = viewModel::onTimerSoundChanged,
        onTimerVibrationChanged = viewModel::onTimerVibrationChanged,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDecreaseDefaultRest: () -> Unit,
    onIncreaseDefaultRest: () -> Unit,
    onFirstDayOfWeekSelected: (DayOfWeek) -> Unit,
    onTimerSoundChanged: (Boolean) -> Unit,
    onTimerVibrationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val colors = FerroTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                navigationIcon = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
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
                    .padding(horizontal = FerroTheme.spacing.space4)
                    .padding(bottom = FerroTheme.spacing.space6)
        ) {
            SectionTitle(text = stringResource(R.string.settings_section_general))
            FerroCard {
                ChipSettingBlock(
                    title = stringResource(R.string.settings_language_title),
                    options =
                        listOf(
                            AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                            AppLanguage.ES to stringResource(R.string.settings_language_es),
                            AppLanguage.EN to stringResource(R.string.settings_language_en)
                        ),
                    selected = settings.language,
                    onSelect = onLanguageSelected
                )

                FerroDivider(modifier = Modifier.padding(vertical = FerroTheme.spacing.space3))

                ChipSettingBlock(
                    title = stringResource(R.string.settings_theme_title),
                    options =
                        listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                            ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                        ),
                    selected = settings.themeMode,
                    onSelect = onThemeModeSelected
                )
            }

            Spacer(modifier = Modifier.height(FerroTheme.spacing.space5))

            SectionTitle(text = stringResource(R.string.settings_section_training))
            FerroCard {
                ChipSettingBlock(
                    title = stringResource(R.string.settings_unit_title),
                    options =
                        listOf(
                            WeightUnit.KG to stringResource(R.string.settings_unit_kg),
                            WeightUnit.LB to stringResource(R.string.settings_unit_lb)
                        ),
                    selected = settings.weightUnit,
                    onSelect = onWeightUnitSelected
                )

                FerroDivider(modifier = Modifier.padding(vertical = FerroTheme.spacing.space3))

                DefaultRestRow(
                    restSeconds = settings.defaultRestSeconds,
                    onDecrease = onDecreaseDefaultRest,
                    onIncrease = onIncreaseDefaultRest
                )

                FerroDivider(modifier = Modifier.padding(vertical = FerroTheme.spacing.space3))

                ChipSettingBlock(
                    title = stringResource(R.string.settings_first_day_title),
                    options =
                        listOf(
                            DayOfWeek.MONDAY to stringResource(R.string.settings_day_monday),
                            DayOfWeek.SUNDAY to stringResource(R.string.settings_day_sunday),
                            DayOfWeek.SATURDAY to stringResource(R.string.settings_day_saturday)
                        ),
                    selected = settings.firstDayOfWeek,
                    onSelect = onFirstDayOfWeekSelected
                )
            }

            Spacer(modifier = Modifier.height(FerroTheme.spacing.space5))

            SectionTitle(text = stringResource(R.string.settings_section_timer))
            FerroCard(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = FerroTheme.spacing.space1)
            ) {
                ToggleSettingRow(
                    title = stringResource(R.string.settings_timer_sound_title),
                    subtitle = stringResource(R.string.settings_timer_sound_subtitle),
                    checked = settings.timerSoundEnabled,
                    onCheckedChange = onTimerSoundChanged
                )
                FerroDivider()
                ToggleSettingRow(
                    title = stringResource(R.string.settings_timer_vibration_title),
                    subtitle = stringResource(R.string.settings_timer_vibration_subtitle),
                    checked = settings.timerVibrationEnabled,
                    onCheckedChange = onTimerVibrationChanged
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = FerroTheme.typography.label,
        color = FerroTheme.colors.inkMuted,
        modifier = Modifier.padding(bottom = FerroTheme.spacing.space2)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipSettingBlock(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = FerroTheme.typography.bodyStrong,
            color = FerroTheme.colors.ink
        )
        Spacer(modifier = Modifier.height(FerroTheme.spacing.space2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space1)
        ) {
            options.forEach { (value, label) ->
                FerroChip(
                    label = label,
                    selected = value == selected,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun DefaultRestRow(restSeconds: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val minutes = restSeconds / 60
    val seconds = restSeconds % 60
    val formatted = stringResource(R.string.settings_rest_format_min_sec, minutes, seconds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_rest_title),
                style = FerroTheme.typography.bodyStrong,
                color = FerroTheme.colors.ink
            )
            Text(
                text = pluralStringResource(R.plurals.rest_seconds, restSeconds, restSeconds),
                style = FerroTheme.typography.caption,
                color = FerroTheme.colors.inkMuted
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space1)
        ) {
            FerroIconButton(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.settings_rest_decrease_cd),
                enabled = restSeconds > Settings.MIN_REST_SECONDS,
                containerColor = FerroTheme.colors.surfaceRaised,
                onClick = onDecrease
            )
            Text(
                text = formatted,
                style = FerroTheme.typography.number,
                color = FerroTheme.colors.ink,
                modifier = Modifier.padding(horizontal = FerroTheme.spacing.space2)
            )
            FerroIconButton(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.settings_rest_increase_cd),
                enabled = restSeconds < Settings.MAX_REST_SECONDS,
                containerColor = FerroTheme.colors.surfaceRaised,
                onClick = onIncrease
            )
        }
    }
}

@Composable
private fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = FerroTheme.colors
    FerroListItem(
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = colors.brasaText,
                        checkedTrackColor = colors.brasaSoft,
                        checkedBorderColor = colors.brasaText,
                        uncheckedThumbColor = colors.inkMuted,
                        uncheckedTrackColor = colors.surfaceRaised,
                        uncheckedBorderColor = colors.lineStrong
                    )
            )
        }
    )
}

@FerroPreviews
@Composable
private fun SettingsScreenPreview() {
    FerroTheme {
        SettingsContent(
            uiState = SettingsUiState(),
            onBack = {},
            onLanguageSelected = {},
            onWeightUnitSelected = {},
            onThemeModeSelected = {},
            onDecreaseDefaultRest = {},
            onIncreaseDefaultRest = {},
            onFirstDayOfWeekSelected = {},
            onTimerSoundChanged = {},
            onTimerVibrationChanged = {}
        )
    }
}
