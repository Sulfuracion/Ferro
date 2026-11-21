package com.davidguijo.ferro.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroBottomSheet
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroListItem
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.component.FerroSetType
import com.davidguijo.ferro.core.designsystem.component.NumberField
import com.davidguijo.ferro.core.designsystem.component.SetTypeBadge
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.designsystem.theme.FerroThemeMode
import com.davidguijo.ferro.core.designsystem.theme.LocalFerroThemeController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignCatalogScreen(modifier: Modifier = Modifier) {
    val themeController = LocalFerroThemeController.current
    var weightText by remember { mutableStateOf("102,5") }
    var repsText by remember { mutableStateOf("") }
    var selectedChip by remember { mutableIntStateOf(0) }
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FerroTheme.colors.bg
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(FerroTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space6)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space3)) {
                Text(
                    text = stringResource(R.string.catalog_title),
                    style = FerroTheme.typography.title,
                    color = FerroTheme.colors.ink
                )
                Row(horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
                    FerroChip(
                        label = stringResource(R.string.catalog_theme_system),
                        selected = themeController.mode == FerroThemeMode.System,
                        onClick = { themeController.mode = FerroThemeMode.System }
                    )
                    FerroChip(
                        label = stringResource(R.string.catalog_theme_light),
                        selected = themeController.mode == FerroThemeMode.Light,
                        onClick = { themeController.mode = FerroThemeMode.Light }
                    )
                    FerroChip(
                        label = stringResource(R.string.catalog_theme_dark),
                        selected = themeController.mode == FerroThemeMode.Dark,
                        onClick = { themeController.mode = FerroThemeMode.Dark }
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_colors)) {
                ColorSwatches()
            }

            CatalogSection(title = stringResource(R.string.catalog_section_typography)) {
                TypographySamples()
            }

            CatalogSection(title = stringResource(R.string.catalog_section_buttons)) {
                Column(verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
                    FerroButton(
                        text = stringResource(R.string.catalog_btn_primary),
                        onClick = {},
                        variant = FerroButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FerroButton(
                        text = stringResource(R.string.catalog_btn_secondary),
                        onClick = {},
                        variant = FerroButtonVariant.Secondary,
                        leadingIcon = painterResource(R.drawable.ic_add),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
                    ) {
                        FerroButton(
                            text = stringResource(R.string.catalog_btn_ghost),
                            onClick = {},
                            variant = FerroButtonVariant.Ghost,
                            leadingIcon = painterResource(R.drawable.ic_add)
                        )
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_timer),
                            contentDescription = stringResource(R.string.catalog_section_icons),
                            tint = FerroTheme.colors.brasaText,
                            onClick = {}
                        )
                    }
                    FerroButton(
                        text = stringResource(R.string.catalog_btn_danger),
                        onClick = {},
                        variant = FerroButtonVariant.Danger,
                        leadingIcon = painterResource(R.drawable.ic_delete)
                    )
                    FerroButton(
                        text = stringResource(R.string.catalog_btn_disabled),
                        onClick = {},
                        enabled = false
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_inputs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
                ) {
                    NumberField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        placeholder = stringResource(R.string.catalog_sample_placeholder_reps),
                        allowDecimal = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(FerroTheme.spacing.space3))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
                    FerroChip(
                        label = stringResource(R.string.catalog_sample_chip_1),
                        selected = selectedChip == 0,
                        onClick = { selectedChip = 0 },
                        leadingIcon =
                            if (selectedChip == 0) {
                                painterResource(R.drawable.ic_check)
                            } else {
                                null
                            }
                    )
                    FerroChip(
                        label = stringResource(R.string.catalog_sample_chip_2),
                        selected = selectedChip == 1,
                        onClick = { selectedChip = 1 }
                    )
                    FerroChip(
                        label = stringResource(R.string.catalog_sample_chip_3),
                        selected = selectedChip == 2,
                        onClick = { selectedChip = 2 }
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_badges)) {
                Row(horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
                    SetTypeBadge(type = FerroSetType.Warmup, onClick = {})
                    SetTypeBadge(type = FerroSetType.Normal, setNumber = 1, onClick = {})
                    SetTypeBadge(type = FerroSetType.Normal, setNumber = 2, onClick = {})
                    SetTypeBadge(type = FerroSetType.Failure, onClick = {})
                    SetTypeBadge(type = FerroSetType.Drop, onClick = {})
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_cards_lists)) {
                FerroCard {
                    FerroListItem(
                        title = stringResource(R.string.catalog_sample_exercise),
                        subtitle = stringResource(R.string.catalog_sample_meta),
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_fitness_center),
                                contentDescription = null,
                                modifier = Modifier.size(FerroTheme.sizes.iconMd),
                                tint = FerroTheme.colors.inkMuted
                            )
                        },
                        trailingContent = {
                            Text(
                                text = "100 kg",
                                style = FerroTheme.typography.number,
                                color = FerroTheme.colors.ink
                            )
                        },
                        onClick = {}
                    )
                    FerroDivider()
                    FerroListItem(
                        title = stringResource(R.string.catalog_sample_exercise),
                        onClick = {}
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_modals)) {
                Row(horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
                    FerroButton(
                        text = stringResource(R.string.catalog_open_sheet),
                        onClick = { showSheet = true },
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    FerroButton(
                        text = stringResource(R.string.catalog_open_dialog),
                        onClick = { showDialog = true },
                        variant = FerroButtonVariant.Danger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_empty)) {
                FerroCard {
                    EmptyState(
                        title = stringResource(R.string.catalog_empty_title),
                        description = stringResource(R.string.catalog_empty_desc),
                        icon = painterResource(R.drawable.ic_fitness_center),
                        actionLabel = stringResource(R.string.catalog_btn_primary),
                        onActionClick = {}
                    )
                }
            }

            CatalogSection(title = stringResource(R.string.catalog_section_icons)) {
                val iconIds =
                    listOf(
                        R.drawable.ic_fitness_center,
                        R.drawable.ic_timer,
                        R.drawable.ic_trophy,
                        R.drawable.ic_check,
                        R.drawable.ic_add,
                        R.drawable.ic_calendar_month,
                        R.drawable.ic_bar_chart,
                        R.drawable.ic_person
                    )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space3)
                ) {
                    iconIds.forEach { resId ->
                        Icon(
                            painter = painterResource(resId),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconLg),
                            tint = FerroTheme.colors.ink
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        FerroBottomSheet(
            onDismissRequest = { showSheet = false },
            title = stringResource(R.string.catalog_sheet_title)
        ) {
            FerroListItem(
                title = stringResource(R.string.set_type_warmup_cd),
                onClick = { showSheet = false }
            )
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.set_type_failure_cd),
                onClick = { showSheet = false }
            )
            FerroDivider()
            FerroListItem(
                title = stringResource(R.string.set_type_drop_cd),
                onClick = { showSheet = false }
            )
        }
    }

    if (showDialog) {
        ConfirmDialog(
            title = stringResource(R.string.catalog_dialog_title),
            message = stringResource(R.string.catalog_dialog_message),
            confirmLabel = stringResource(R.string.catalog_dialog_confirm),
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
        Text(
            text = title.uppercase(),
            style = FerroTheme.typography.label,
            color = FerroTheme.colors.inkMuted
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches() {
    val colors = FerroTheme.colors
    val swatches =
        listOf(
            "bg" to colors.bg,
            "surface" to colors.surface,
            "raised" to colors.surfaceRaised,
            "line" to colors.line,
            "lineStr" to colors.lineStrong,
            "ink" to colors.ink,
            "muted" to colors.inkMuted,
            "subtle" to colors.inkSubtle,
            "brasa" to colors.brasa,
            "pressed" to colors.brasaPressed,
            "onBrasa" to colors.onBrasa,
            "brasaTxt" to colors.brasaText,
            "brasaSoft" to colors.brasaSoft,
            "iron" to colors.iron,
            "success" to colors.success,
            "warning" to colors.warning,
            "danger" to colors.danger,
            "warmup" to colors.setWarmup,
            "failure" to colors.setFailure,
            "drop" to colors.setDrop,
            "focus" to colors.focus
        )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)
    ) {
        swatches.forEach { (name, color) ->
            ColorSwatch(name = name, color = color)
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(color, RoundedCornerShape(FerroTheme.radius.sm))
                    .border(
                        width = 1.dp,
                        color = FerroTheme.colors.lineStrong,
                        shape = RoundedCornerShape(FerroTheme.radius.sm)
                    )
        )
        Text(
            text = name,
            style = FerroTheme.typography.caption,
            color = FerroTheme.colors.inkMuted
        )
    }
}

@Composable
private fun TypographySamples() {
    Column(verticalArrangement = Arrangement.spacedBy(FerroTheme.spacing.space2)) {
        Text("PUSH · DÍA 1", style = FerroTheme.typography.display, color = FerroTheme.colors.ink)
        Text("Press banca", style = FerroTheme.typography.title, color = FerroTheme.colors.ink)
        Text("1:30", style = FerroTheme.typography.numberXl, color = FerroTheme.colors.ink)
        Text("102,5 × 8", style = FerroTheme.typography.number, color = FerroTheme.colors.ink)
        Text(
            stringResource(R.string.catalog_sample_exercise),
            style = FerroTheme.typography.heading,
            color = FerroTheme.colors.ink
        )
        Text(
            stringResource(R.string.home_tagline),
            style = FerroTheme.typography.body,
            color = FerroTheme.colors.ink
        )
        Text(
            stringResource(R.string.catalog_btn_primary),
            style = FerroTheme.typography.bodyStrong,
            color = FerroTheme.colors.ink
        )
        Text(
            stringResource(R.string.catalog_sample_meta),
            style = FerroTheme.typography.caption,
            color = FerroTheme.colors.inkMuted
        )
        Text(
            "SERIE  ANTERIOR  KG  REPS",
            style = FerroTheme.typography.label,
            color = FerroTheme.colors.inkMuted
        )
    }
}

@FerroPreviews
@Composable
private fun DesignCatalogScreenPreview() {
    FerroTheme {
        DesignCatalogScreen()
    }
}
