package com.davidguijo.ferro.feature.body

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.ConfirmDialog
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroChartEntry
import com.davidguijo.ferro.core.designsystem.component.FerroChip
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroLineChart
import com.davidguijo.ferro.core.designsystem.component.FerroLoadingState
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.BodyChartPoint
import com.davidguijo.ferro.core.domain.BodyMeasurementCalculations
import com.davidguijo.ferro.core.domain.BodyMetric
import com.davidguijo.ferro.core.domain.BodyMetricSummary
import com.davidguijo.ferro.core.domain.LengthUnit
import com.davidguijo.ferro.core.domain.PhotoDateGroup
import com.davidguijo.ferro.core.domain.PhotoPose
import com.davidguijo.ferro.core.domain.Units
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.model.ProgressTimeRange
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class BodyTab(@StringRes val labelRes: Int) {
    SUMMARY(R.string.body_tab_summary),
    CHARTS(R.string.body_tab_charts),
    PHOTOS(R.string.body_tab_photos)
}

@Composable
fun BodyMeasurementsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyMeasurementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val pickMediaLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.onPhotoPickedFromGallery(uri)
            }
        }
    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val tempFile = pendingCameraFile
            pendingCameraFile = null
            if (tempFile != null) {
                viewModel.onCameraPhotoCaptured(tempFile, success)
            }
        }

    BodyMeasurementsContent(
        uiState = uiState,
        onBack = onBack,
        onOpenAddForm = viewModel::onOpenAddForm,
        onOpenEditForm = viewModel::onOpenEditForm,
        onDismissForm = viewModel::onDismissForm,
        onUpdateFormDate = viewModel::onUpdateFormDate,
        onUpdateFormField = viewModel::onUpdateFormField,
        onUpdateFormNotes = viewModel::onUpdateFormNotes,
        onSaveForm = viewModel::onSaveForm,
        onRequestDeleteMeasurement = viewModel::onRequestDeleteMeasurement,
        onDismissDeleteMeasurement = viewModel::onDismissDeleteMeasurement,
        onConfirmDeleteMeasurement = viewModel::onConfirmDeleteMeasurement,
        onSelectMetric = viewModel::onSelectMetric,
        onSelectRange = viewModel::onSelectRange,
        onPickPhoto = {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onTakePhoto = {
            val pair = viewModel.createTempCameraUri()
            if (pair != null) {
                pendingCameraFile = pair.second
                takePictureLauncher.launch(pair.first)
            }
        },
        onSelectPendingPhotoPose = viewModel::onSelectPendingPhotoPose,
        onSavePendingPhoto = viewModel::onSavePendingPhoto,
        onCancelPendingPhoto = viewModel::onCancelPendingPhoto,
        onDismissPhotoError = viewModel::onDismissPhotoError,
        onRequestDeletePhoto = viewModel::onRequestDeletePhoto,
        onDismissDeletePhoto = viewModel::onDismissDeletePhoto,
        onConfirmDeletePhoto = viewModel::onConfirmDeletePhoto,
        onToggleCompareMode = viewModel::onToggleCompareMode,
        onSelectCompareSlot = viewModel::onSelectCompareSlot,
        onPickPhotoForCompare = viewModel::onPickPhotoForCompare,
        resolvePhotoFile = viewModel::resolvePhotoFile,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyMeasurementsContent(
    uiState: BodyMeasurementsUiState,
    onBack: () -> Unit,
    onOpenAddForm: () -> Unit,
    onOpenEditForm: (BodyMeasurement) -> Unit,
    onDismissForm: () -> Unit,
    onUpdateFormDate: (Long) -> Unit,
    onUpdateFormField: (BodyMetric, String) -> Unit,
    onUpdateFormNotes: (String) -> Unit,
    onSaveForm: () -> Unit,
    onRequestDeleteMeasurement: (String) -> Unit,
    onDismissDeleteMeasurement: () -> Unit,
    onConfirmDeleteMeasurement: () -> Unit,
    onSelectMetric: (BodyMetric) -> Unit,
    onSelectRange: (ProgressTimeRange) -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectPendingPhotoPose: (PhotoPose) -> Unit,
    onSavePendingPhoto: () -> Unit,
    onCancelPendingPhoto: () -> Unit,
    onDismissPhotoError: () -> Unit,
    onRequestDeletePhoto: (ProgressPhoto) -> Unit,
    onDismissDeletePhoto: () -> Unit,
    onConfirmDeletePhoto: () -> Unit,
    onToggleCompareMode: () -> Unit,
    onSelectCompareSlot: (CompareSlot) -> Unit,
    onPickPhotoForCompare: (String) -> Unit,
    resolvePhotoFile: (String) -> File?,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.body_measurements_title),
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
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = colors.bg,
                contentColor = colors.ink,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = colors.brasaText
                    )
                },
                divider = { HorizontalDivider(color = colors.line) }
            ) {
                BodyTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    Tab(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = FerroTheme.typography.bodyStrong,
                                color = if (selected) colors.ink else colors.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (BodyTab.entries[selectedTabIndex]) {
                BodyTab.SUMMARY ->
                    BodySummaryTab(
                        uiState = uiState,
                        onAddRecord = onOpenAddForm,
                        onEditRecord = onOpenEditForm,
                        onDeleteRecord = onRequestDeleteMeasurement,
                        onOpenMetricChart = { metric ->
                            onSelectMetric(metric)
                            selectedTabIndex = BodyTab.CHARTS.ordinal
                        }
                    )

                BodyTab.CHARTS ->
                    BodyChartsTab(
                        selectedMetric = uiState.selectedMetric,
                        selectedRange = uiState.selectedRange,
                        weightUnit = uiState.weightUnit,
                        lengthUnit = uiState.lengthUnit,
                        chartPoints = uiState.chartPoints,
                        onSelectMetric = onSelectMetric,
                        onSelectRange = onSelectRange
                    )

                BodyTab.PHOTOS ->
                    BodyPhotosTab(
                        uiState = uiState,
                        onPickPhoto = onPickPhoto,
                        onTakePhoto = onTakePhoto,
                        onToggleCompareMode = onToggleCompareMode,
                        onSelectCompareSlot = onSelectCompareSlot,
                        onPickPhotoForCompare = onPickPhotoForCompare,
                        onRequestDeletePhoto = onRequestDeletePhoto,
                        onDismissPhotoError = onDismissPhotoError,
                        resolvePhotoFile = resolvePhotoFile
                    )
            }
        }
    }

    val formDraft = uiState.formDraft
    if (formDraft != null) {
        BodyMeasurementFormSheet(
            draft = formDraft,
            weightUnit = uiState.weightUnit,
            lengthUnit = uiState.lengthUnit,
            onDismiss = onDismissForm,
            onUpdateDate = onUpdateFormDate,
            onUpdateField = onUpdateFormField,
            onUpdateNotes = onUpdateFormNotes,
            onSave = onSaveForm
        )
    }

    if (uiState.deleteMeasurementConfirmId != null) {
        ConfirmDialog(
            title = stringResource(R.string.body_delete_title),
            message = stringResource(R.string.body_delete_message),
            confirmLabel = stringResource(R.string.body_delete_confirm),
            onConfirm = onConfirmDeleteMeasurement,
            onDismiss = onDismissDeleteMeasurement
        )
    }

    val pendingPhoto = uiState.pendingPhotoDraft
    if (pendingPhoto != null) {
        PendingPhotoPoseDialog(
            pending = pendingPhoto,
            photoFile = resolvePhotoFile(pendingPhoto.fileName),
            onSelectPose = onSelectPendingPhotoPose,
            onConfirm = onSavePendingPhoto,
            onDismiss = onCancelPendingPhoto
        )
    }

    if (uiState.deletePhotoConfirm != null) {
        ConfirmDialog(
            title = stringResource(R.string.body_photos_delete_title),
            message = stringResource(R.string.body_photos_delete_message),
            confirmLabel = stringResource(R.string.body_delete_confirm),
            onConfirm = onConfirmDeletePhoto,
            onDismiss = onDismissDeletePhoto
        )
    }
}

@Composable
private fun BodySummaryTab(
    uiState: BodyMeasurementsUiState,
    onAddRecord: () -> Unit,
    onEditRecord: (BodyMeasurement) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onOpenMetricChart: (BodyMetric) -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        item {
            FerroButton(
                text = stringResource(R.string.body_add_record),
                onClick = onAddRecord,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!uiState.isLoaded) {
            item(key = "loading") {
                FerroLoadingState(modifier = Modifier.height(240.dp))
            }
            return@LazyColumn
        }

        if (uiState.measurements.isEmpty()) {
            item {
                FerroCard {
                    EmptyState(
                        title = stringResource(R.string.body_measurements_empty_title),
                        description = stringResource(R.string.body_measurements_empty_description),
                        icon = painterResource(R.drawable.ic_person)
                    )
                }
            }
            return@LazyColumn
        }

        item {
            Text(
                text = stringResource(R.string.body_section_latest).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
        }

        item {
            FerroCard(contentPadding = PaddingValues(vertical = spacing.space1)) {
                uiState.metricSummaries.forEachIndexed { index, summary ->
                    MetricSummaryRow(
                        summary = summary,
                        weightUnit = uiState.weightUnit,
                        lengthUnit = uiState.lengthUnit,
                        locale = locale,
                        onClick = { onOpenMetricChart(summary.metric) }
                    )
                    if (index < uiState.metricSummaries.lastIndex) {
                        FerroDivider()
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.body_section_history).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
        }

        items(uiState.measurements, key = { it.id }) { measurement ->
            MeasurementHistoryCard(
                measurement = measurement,
                weightUnit = uiState.weightUnit,
                lengthUnit = uiState.lengthUnit,
                locale = locale,
                dateFormatter = dateFormatter,
                zoneId = zoneId,
                onEdit = { onEditRecord(measurement) },
                onDelete = { onDeleteRecord(measurement.id) }
            )
        }
    }
}

@Composable
private fun MetricSummaryRow(
    summary: BodyMetricSummary,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    locale: Locale,
    onClick: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    val valueText =
        summary.latestValue?.let {
            formatMetricValue(summary.metric, it, weightUnit, lengthUnit, locale)
        } ?: stringResource(R.string.body_no_value)

    val deltaText =
        summary.delta?.let {
            formatMetricDelta(summary.metric, it, weightUnit, lengthUnit, locale)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.space4, vertical = spacing.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(summary.metric.labelRes()),
            style = FerroTheme.typography.body,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = valueText,
                style = FerroTheme.typography.number,
                color = if (summary.latestValue != null) colors.ink else colors.inkSubtle
            )
            if (deltaText != null) {
                Text(
                    text = deltaText,
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }
        }
    }
}

@Composable
private fun MeasurementHistoryCard(
    measurement: BodyMeasurement,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    locale: Locale,
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val dateText =
        Instant.ofEpochMilli(measurement.measuredAt)
            .atZone(zoneId)
            .toLocalDate()
            .format(dateFormatter)

    val chips =
        BodyMetric.entries.mapNotNull { metric ->
            val value = BodyMeasurementCalculations.valueFor(measurement, metric) ?: return@mapNotNull null
            val formatted = formatMetricValue(metric, value, weightUnit, lengthUnit, locale)
            metric to formatted
        }

    FerroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateText,
                style = FerroTheme.typography.bodyStrong,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Row {
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.body_edit_cd),
                    onClick = onEdit
                )
                FerroIconButton(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.body_delete_cd),
                    onClick = onDelete,
                    tint = colors.danger
                )
            }
        }
        if (chips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.space2))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                chips.forEach { (metric, valueText) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(metric.labelRes()),
                            style = FerroTheme.typography.caption,
                            color = colors.inkMuted
                        )
                        Text(
                            text = valueText,
                            style = FerroTheme.typography.body,
                            color = colors.ink
                        )
                    }
                }
            }
        }
        if (measurement.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text = measurement.notes,
                style = FerroTheme.typography.caption,
                color = colors.inkMuted
            )
        }
    }
}

@Composable
private fun BodyChartsTab(
    selectedMetric: BodyMetric,
    selectedRange: ProgressTimeRange,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    chartPoints: List<BodyChartPoint>,
    onSelectMetric: (BodyMetric) -> Unit,
    onSelectRange: (ProgressTimeRange) -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val shortDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val fullDateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    val unitSuffix =
        when (selectedMetric) {
            BodyMetric.WEIGHT ->
                stringResource(
                    if (weightUnit ==
                        WeightUnit.KG
                    ) {
                        R.string.settings_unit_kg
                    } else {
                        R.string.settings_unit_lb
                    }
                )

            BodyMetric.BODY_FAT -> stringResource(R.string.body_unit_pct)

            else ->
                stringResource(if (lengthUnit == LengthUnit.CM) R.string.body_unit_cm else R.string.body_unit_in)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            items(BodyMetric.entries, key = { it.name }) { metric ->
                FerroChip(
                    label = stringResource(metric.labelRes()),
                    selected = metric == selectedMetric,
                    onClick = { onSelectMetric(metric) }
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            items(ProgressTimeRange.entries, key = { it.name }) { range ->
                FerroChip(
                    label = stringResource(timeRangeLabelRes(range)),
                    selected = range == selectedRange,
                    onClick = { onSelectRange(range) }
                )
            }
        }

        if (chartPoints.isEmpty()) {
            FerroCard {
                EmptyState(
                    title = stringResource(R.string.body_chart_empty_title),
                    description = stringResource(R.string.body_chart_empty_description),
                    icon = painterResource(R.drawable.ic_bar_chart)
                )
            }
            return
        }

        val chartEntries =
            remember(chartPoints, selectedMetric, weightUnit, lengthUnit, locale) {
                chartPoints.map { point ->
                    val localDate = Instant.ofEpochMilli(point.timestampMillis).atZone(zoneId).toLocalDate()
                    val displayVal = convertForDisplay(selectedMetric, point.value, weightUnit, lengthUnit)
                    FerroChartEntry(
                        xLabel = localDate.format(shortDateFormatter),
                        tooltipTitle = localDate.format(fullDateFormatter),
                        value = displayVal,
                        formattedValue = formatMetricValue(selectedMetric, point.value, weightUnit, lengthUnit, locale)
                    )
                }
            }

        val latestPoint = chartPoints.last()
        FerroCard {
            Text(
                text = stringResource(selectedMetric.labelRes()).uppercase(),
                style = FerroTheme.typography.label,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = formatMetricValue(selectedMetric, latestPoint.value, weightUnit, lengthUnit, locale),
                style = FerroTheme.typography.title,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(spacing.space3))

            FerroLineChart(
                entries = chartEntries,
                yAxisValueFormatter = { axisVal ->
                    "${formatAxisValue(axisVal, locale)} $unitSuffix"
                }
            )
        }
    }
}

@Composable
private fun BodyPhotosTab(
    uiState: BodyMeasurementsUiState,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onToggleCompareMode: () -> Unit,
    onSelectCompareSlot: (CompareSlot) -> Unit,
    onPickPhotoForCompare: (String) -> Unit,
    onRequestDeletePhoto: (ProgressPhoto) -> Unit,
    onDismissPhotoError: () -> Unit,
    resolvePhotoFile: (String) -> File?
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val dateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val photosById = remember(uiState.allPhotos) { uiState.allPhotos.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                FerroButton(
                    text = stringResource(R.string.body_photos_pick),
                    onClick = onPickPhoto,
                    variant = FerroButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                FerroButton(
                    text = stringResource(R.string.body_photos_take),
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (uiState.photoError) {
            item {
                FerroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.body_photos_import_error),
                            style = FerroTheme.typography.caption,
                            color = colors.danger,
                            modifier = Modifier.weight(1f)
                        )
                        FerroButton(
                            text = stringResource(R.string.dialog_cancel),
                            onClick = onDismissPhotoError,
                            variant = FerroButtonVariant.Ghost
                        )
                    }
                }
            }
        }

        if (uiState.allPhotos.size >= 2) {
            item {
                FerroButton(
                    text =
                        stringResource(
                            if (uiState.isCompareMode) {
                                R.string.body_photos_close_compare
                            } else {
                                R.string.body_photos_compare
                            }
                        ),
                    onClick = onToggleCompareMode,
                    variant = FerroButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.isCompareMode) {
            item {
                PhotoCompareCard(
                    leftPhoto = uiState.compareLeftPhotoId?.let { photosById[it] },
                    rightPhoto = uiState.compareRightPhotoId?.let { photosById[it] },
                    activeSlot = uiState.activeCompareSlot,
                    onSelectSlot = onSelectCompareSlot,
                    resolvePhotoFile = resolvePhotoFile,
                    dateFormatter = dateFormatter
                )
            }
        }

        if (uiState.isLoaded && uiState.photoGroups.isEmpty()) {
            item {
                FerroCard {
                    EmptyState(
                        title = stringResource(R.string.body_photos_empty_title),
                        description = stringResource(R.string.body_photos_empty_description),
                        icon = painterResource(R.drawable.ic_person)
                    )
                }
            }
            return@LazyColumn
        }

        uiState.photoGroups.forEach { group ->
            item(key = "header_${group.date}") {
                Text(
                    text = group.date.format(dateFormatter).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted
                )
            }
            item(key = "photos_${group.date}") {
                PhotoGroupRow(
                    group = group,
                    isCompareMode = uiState.isCompareMode,
                    leftSelectedId = uiState.compareLeftPhotoId,
                    rightSelectedId = uiState.compareRightPhotoId,
                    onPickPhotoForCompare = onPickPhotoForCompare,
                    onRequestDeletePhoto = onRequestDeletePhoto,
                    resolvePhotoFile = resolvePhotoFile
                )
            }
        }
    }
}

@Composable
private fun PhotoCompareCard(
    leftPhoto: ProgressPhoto?,
    rightPhoto: ProgressPhoto?,
    activeSlot: CompareSlot,
    onSelectSlot: (CompareSlot) -> Unit,
    resolvePhotoFile: (String) -> File?,
    dateFormatter: DateTimeFormatter
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val zoneId = remember { ZoneId.systemDefault() }

    FerroCard {
        Text(
            text = stringResource(R.string.body_photos_compare_title),
            style = FerroTheme.typography.heading,
            color = colors.ink
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = stringResource(R.string.body_photos_compare_pick_hint),
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space3))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            CompareSlotPane(
                label = stringResource(R.string.body_photos_compare_left),
                photo = leftPhoto,
                selected = activeSlot == CompareSlot.LEFT,
                onClick = { onSelectSlot(CompareSlot.LEFT) },
                resolvePhotoFile = resolvePhotoFile,
                dateFormatter = dateFormatter,
                zoneId = zoneId,
                modifier = Modifier.weight(1f)
            )
            CompareSlotPane(
                label = stringResource(R.string.body_photos_compare_right),
                photo = rightPhoto,
                selected = activeSlot == CompareSlot.RIGHT,
                onClick = { onSelectSlot(CompareSlot.RIGHT) },
                resolvePhotoFile = resolvePhotoFile,
                dateFormatter = dateFormatter,
                zoneId = zoneId,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompareSlotPane(
    label: String,
    photo: ProgressPhoto?,
    selected: Boolean,
    onClick: () -> Unit,
    resolvePhotoFile: (String) -> File?,
    dateFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val shape = RoundedCornerShape(FerroTheme.radius.md)
    val borderColor = if (selected) colors.brasa else colors.lineStrong

    Column(
        modifier =
            modifier
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space1)
    ) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = if (selected) colors.brasaText else colors.inkMuted
        )
        if (photo != null) {
            AsyncImage(
                model = resolvePhotoFile(photo.fileName),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(FerroTheme.radius.sm))
                        .background(colors.surfaceRaised)
            )
            val dateText =
                Instant.ofEpochMilli(photo.takenAt).atZone(zoneId).toLocalDate().format(dateFormatter)
            Text(
                text = "$dateText · ${stringResource(PhotoPose.fromKey(photo.pose).labelRes())}",
                style = FerroTheme.typography.caption,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(colors.surfaceRaised, RoundedCornerShape(FerroTheme.radius.sm)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = colors.inkSubtle,
                    modifier = Modifier.size(FerroTheme.sizes.iconLg)
                )
            }
        }
    }
}

@Composable
private fun PhotoGroupRow(
    group: PhotoDateGroup,
    isCompareMode: Boolean,
    leftSelectedId: String?,
    rightSelectedId: String?,
    onPickPhotoForCompare: (String) -> Unit,
    onRequestDeletePhoto: (ProgressPhoto) -> Unit,
    resolvePhotoFile: (String) -> File?
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
        items(group.photos, key = { it.id }) { photo ->
            val isSelectedInCompare =
                isCompareMode && (photo.id == leftSelectedId || photo.id == rightSelectedId)
            val shape = RoundedCornerShape(FerroTheme.radius.md)
            val pose = PhotoPose.fromKey(photo.pose)

            Column(
                modifier =
                    Modifier
                        .size(width = 144.dp, height = 216.dp)
                        .background(colors.surface, shape)
                        .border(
                            width = if (isSelectedInCompare) 2.dp else 1.dp,
                            color = if (isSelectedInCompare) colors.brasa else colors.line,
                            shape = shape
                        )
                        .clip(shape)
                        .then(
                            if (isCompareMode) {
                                Modifier.clickable { onPickPhotoForCompare(photo.id) }
                            } else {
                                Modifier
                            }
                        )
            ) {
                AsyncImage(
                    model = resolvePhotoFile(photo.fileName),
                    contentDescription = stringResource(pose.labelRes()),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(colors.surfaceRaised)
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.space2, vertical = spacing.space1),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(pose.labelRes()),
                        style = FerroTheme.typography.caption,
                        color = colors.ink
                    )
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.body_photos_delete_cd),
                        onClick = { onRequestDeletePhoto(photo) },
                        tint = colors.danger
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingPhotoPoseDialog(
    pending: PendingPhotoDraft,
    photoFile: File?,
    onSelectPose: (PhotoPose) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(FerroTheme.radius.lg),
            color = colors.surface,
            contentColor = colors.ink,
            shadowElevation = FerroTheme.elevation.large
        ) {
            Column(
                modifier = Modifier.padding(spacing.space5),
                verticalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                Text(
                    text = stringResource(R.string.body_photos_pose_title),
                    style = FerroTheme.typography.heading,
                    color = colors.ink
                )
                AsyncImage(
                    model = photoFile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(FerroTheme.radius.md))
                            .background(colors.surfaceRaised)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    PhotoPose.entries.forEach { pose ->
                        FerroChip(
                            label = stringResource(pose.labelRes()),
                            selected = pending.pose == pose,
                            onClick = { onSelectPose(pose) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    FerroButton(
                        text = stringResource(R.string.body_form_cancel),
                        onClick = onDismiss,
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    FerroButton(
                        text = stringResource(R.string.body_photos_confirm_save),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@StringRes
private fun PhotoPose.labelRes(): Int = when (this) {
    PhotoPose.FRONT -> R.string.body_photos_pose_front
    PhotoPose.SIDE -> R.string.body_photos_pose_side
    PhotoPose.BACK -> R.string.body_photos_pose_back
}

@StringRes
private fun timeRangeLabelRes(range: ProgressTimeRange): Int = when (range) {
    ProgressTimeRange.ONE_MONTH -> R.string.exercise_chart_range_1m
    ProgressTimeRange.THREE_MONTHS -> R.string.exercise_chart_range_3m
    ProgressTimeRange.ONE_YEAR -> R.string.exercise_chart_range_1y
    ProgressTimeRange.ALL -> R.string.exercise_chart_range_all
}

private fun convertForDisplay(
    metric: BodyMetric,
    canonicalValue: Double,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit
): Double = when (metric) {
    BodyMetric.WEIGHT -> Units.fromKg(canonicalValue, weightUnit)
    BodyMetric.BODY_FAT -> canonicalValue
    else -> Units.fromCm(canonicalValue, lengthUnit)
}

private fun formatMetricValue(
    metric: BodyMetric,
    canonicalValue: Double,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    locale: Locale
): String = when (metric) {
    BodyMetric.WEIGHT -> Units.formatWeight(canonicalValue, weightUnit, locale = locale, includeUnit = true)
    BodyMetric.BODY_FAT -> Units.formatBodyFat(canonicalValue, locale = locale, includeUnit = true)
    else -> Units.formatLength(canonicalValue, lengthUnit, locale = locale, includeUnit = true)
}

private fun formatMetricDelta(
    metric: BodyMetric,
    canonicalDelta: Double,
    weightUnit: WeightUnit,
    lengthUnit: LengthUnit,
    locale: Locale
): String {
    val displayDelta = convertForDisplay(metric, canonicalDelta, weightUnit, lengthUnit)
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val pattern = if (metric == BodyMetric.BODY_FAT) "0.#" else "0.##"
    val formatter =
        DecimalFormat(pattern, symbols).apply {
            isGroupingUsed = false
        }
    val prefix =
        when {
            displayDelta > 0.0 -> "+"
            else -> ""
        }
    val unitSuffix =
        when (metric) {
            BodyMetric.WEIGHT -> if (weightUnit == WeightUnit.KG) "kg" else "lb"
            BodyMetric.BODY_FAT -> "%"
            else -> if (lengthUnit == LengthUnit.CM) "cm" else "in"
        }
    val normalized = if (displayDelta == 0.0) 0.0 else displayDelta
    return "$prefix${formatter.format(normalized)} $unitSuffix"
}

private fun formatAxisValue(value: Double, locale: Locale): String {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val formatter =
        DecimalFormat("0.#", symbols).apply {
            isGroupingUsed = false
        }
    val normalized = if (value == 0.0) 0.0 else value
    return formatter.format(normalized)
}
