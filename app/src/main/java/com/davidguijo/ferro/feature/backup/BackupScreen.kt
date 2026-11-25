package com.davidguijo.ferro.feature.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroButton
import com.davidguijo.ferro.core.designsystem.component.FerroButtonVariant
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BackupImportMode
import com.davidguijo.ferro.core.domain.model.BackupPreview
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIME_APPLICATION_JSON = "application/json"
private const val MIME_TEXT_CSV = "text/csv"

@Composable
fun BackupScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BackupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val coroutineScope = rememberCoroutineScope()

    val createJsonLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(MIME_APPLICATION_JSON)
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.exportJsonTo { content ->
                    writeTextToUri(context, uri, content)
                }
            }
        }

    val openJsonLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val rawJson = readTextFromUri(context, uri)
                    viewModel.onImportFileLoaded(rawJson)
                }
            }
        }

    val createCsvLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(MIME_TEXT_CSV)
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.exportCsvTo(zoneId = zoneId, locale = locale) { content ->
                    writeTextToUri(context, uri, content)
                }
            }
        }

    BackupContent(
        uiState = uiState,
        onBack = onBack,
        onExportJsonClick = {
            createJsonLauncher.launch(viewModel.suggestedJsonFileName(zoneId))
        },
        onImportJsonClick = {
            openJsonLauncher.launch(arrayOf(MIME_APPLICATION_JSON, "application/octet-stream", "text/plain", "*/*"))
        },
        onExportCsvClick = {
            createCsvLauncher.launch(viewModel.suggestedCsvFileName(zoneId))
        },
        onSelectImportMode = viewModel::onSelectImportMode,
        onConfirmImport = viewModel::onConfirmImport,
        onCancelImport = viewModel::onCancelPendingImport,
        onDismissFeedback = viewModel::onDismissFeedback,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupContent(
    uiState: BackupUiState,
    onBack: () -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onSelectImportMode: (BackupImportMode) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }
    val dateTimeFormatter =
        remember(locale) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        }

    val lastBackupText =
        uiState.lastBackupAtMillis?.let { millis ->
            Instant.ofEpochMilli(millis).atZone(zoneId).format(dateTimeFormatter)
        } ?: stringResource(R.string.backup_last_never)

    val unitLabel =
        stringResource(
            if (uiState.weightUnit == WeightUnit.KG) R.string.settings_unit_kg else R.string.settings_unit_lb
        )

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.backup_title),
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
                    .padding(horizontal = spacing.space4)
                    .padding(bottom = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space5)
        ) {
            // Estado de la última copia y aviso sobre fotos
            Column {
                SectionHeader(text = stringResource(R.string.backup_last_section))
                FerroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.backup_last_date_label),
                            style = FerroTheme.typography.body,
                            color = colors.inkMuted
                        )
                        Text(
                            text = lastBackupText,
                            style = FerroTheme.typography.bodyStrong,
                            color = if (uiState.isBackupStale) colors.warning else colors.ink
                        )
                    }
                    if (uiState.isBackupStale) {
                        Spacer(modifier = Modifier.height(spacing.space2))
                        Text(
                            text = stringResource(R.string.backup_stale_warning),
                            style = FerroTheme.typography.caption,
                            color = colors.warning
                        )
                    } else if (uiState.lastBackupAtMillis == null) {
                        Spacer(modifier = Modifier.height(spacing.space2))
                        Text(
                            text = stringResource(R.string.backup_never_warning),
                            style = FerroTheme.typography.caption,
                            color = colors.inkMuted
                        )
                    }
                    FerroDivider(modifier = Modifier.padding(vertical = spacing.space3))
                    Text(
                        text = stringResource(R.string.backup_photos_notice),
                        style = FerroTheme.typography.caption,
                        color = colors.inkMuted
                    )
                }
            }

            // Mensaje de éxito o error
            val feedback = uiState.feedback
            if (feedback != null) {
                BackupFeedbackBanner(
                    feedback = feedback,
                    onDismiss = onDismissFeedback
                )
            }

            // Vista previa de importación JSON (cuando el usuario ha elegido un archivo válido)
            val pendingImport = uiState.pendingImport
            if (pendingImport != null) {
                ImportPreviewCard(
                    candidate = pendingImport,
                    dateTimeFormatter = dateTimeFormatter,
                    zoneId = zoneId,
                    enabled = !uiState.isBusy,
                    onSelectMode = onSelectImportMode,
                    onConfirm = onConfirmImport,
                    onCancel = onCancelImport
                )
            }

            // Sección JSON (Exportar / Importar)
            Column {
                SectionHeader(text = stringResource(R.string.backup_section_json))
                FerroCard {
                    Text(
                        text = stringResource(R.string.backup_json_description),
                        style = FerroTheme.typography.body,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(spacing.space2))
                    Text(
                        text = stringResource(R.string.backup_photos_notice),
                        style = FerroTheme.typography.caption,
                        color = colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(spacing.space4))
                    FerroButton(
                        text = stringResource(R.string.backup_export_json_action),
                        onClick = onExportJsonClick,
                        enabled = !uiState.isBusy,
                        variant =
                            if (pendingImport == null) {
                                FerroButtonVariant.Primary
                            } else {
                                FerroButtonVariant.Secondary
                            },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(spacing.space2))
                    FerroButton(
                        text = stringResource(R.string.backup_import_json_action),
                        onClick = onImportJsonClick,
                        enabled = !uiState.isBusy,
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sección CSV
            Column {
                SectionHeader(text = stringResource(R.string.backup_section_csv))
                FerroCard {
                    Text(
                        text = stringResource(R.string.backup_csv_description, unitLabel),
                        style = FerroTheme.typography.body,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(spacing.space4))
                    FerroButton(
                        text = stringResource(R.string.backup_export_csv_action),
                        onClick = onExportCsvClick,
                        enabled = !uiState.isBusy,
                        variant = FerroButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = FerroTheme.typography.label,
        color = FerroTheme.colors.inkMuted,
        modifier = Modifier.padding(bottom = FerroTheme.spacing.space2)
    )
}

@Composable
private fun BackupFeedbackBanner(feedback: BackupFeedback, onDismiss: () -> Unit) {
    val colors = FerroTheme.colors
    val isError =
        when (feedback) {
            BackupFeedback.ErrorCorrupted,
            BackupFeedback.ErrorFutureSchema,
            BackupFeedback.ErrorIo -> true

            BackupFeedback.ExportJsonSuccess,
            BackupFeedback.ExportCsvSuccess,
            is BackupFeedback.ImportSuccess -> false
        }

    val message =
        when (feedback) {
            BackupFeedback.ExportJsonSuccess -> stringResource(R.string.backup_status_export_json_ok)

            BackupFeedback.ExportCsvSuccess -> stringResource(R.string.backup_status_export_csv_ok)

            is BackupFeedback.ImportSuccess ->
                stringResource(R.string.backup_status_import_ok, feedback.safetyBackupFileName)

            BackupFeedback.ErrorCorrupted -> stringResource(R.string.backup_error_corrupted)

            BackupFeedback.ErrorFutureSchema -> stringResource(R.string.backup_error_future_schema)

            BackupFeedback.ErrorIo -> stringResource(R.string.backup_error_io)
        }

    FerroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = FerroTheme.typography.bodyStrong,
                color = if (isError) colors.danger else colors.success,
                modifier = Modifier.weight(1f)
            )
            FerroButton(
                text = stringResource(R.string.dialog_cancel),
                onClick = onDismiss,
                variant = FerroButtonVariant.Ghost
            )
        }
    }
}

@Composable
private fun ImportPreviewCard(
    candidate: PendingImportCandidate,
    dateTimeFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    enabled: Boolean,
    onSelectMode: (BackupImportMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val preview = candidate.preview
    val dateText =
        if (preview.exportedAtMillis > 0L) {
            Instant.ofEpochMilli(preview.exportedAtMillis).atZone(zoneId).format(dateTimeFormatter)
        } else {
            stringResource(R.string.backup_last_never)
        }

    FerroCard {
        Text(
            text = stringResource(R.string.backup_preview_title),
            style = FerroTheme.typography.heading,
            color = colors.ink
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text =
                stringResource(
                    R.string.backup_preview_meta_format,
                    preview.schemaVersion,
                    preview.appVersion,
                    dateText
                ),
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )

        if (preview.migratedFromSchemaVersion != null) {
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text =
                    stringResource(
                        R.string.backup_preview_migrated_badge,
                        preview.migratedFromSchemaVersion,
                        preview.schemaVersion
                    ),
                style = FerroTheme.typography.caption,
                color = colors.warning
            )
        }

        Spacer(modifier = Modifier.height(spacing.space4))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            PreviewStatItem(
                label = stringResource(R.string.backup_preview_workouts_label),
                count = preview.workoutsCount,
                modifier = Modifier.weight(1f)
            )
            PreviewStatItem(
                label = stringResource(R.string.backup_preview_routines_label),
                count = preview.routinesCount,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(spacing.space3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            PreviewStatItem(
                label = stringResource(R.string.backup_preview_exercises_label),
                count = preview.exercisesCount,
                modifier = Modifier.weight(1f)
            )
            PreviewStatItem(
                label = stringResource(R.string.backup_preview_measurements_label),
                count = preview.measurementsCount,
                modifier = Modifier.weight(1f)
            )
        }

        FerroDivider(modifier = Modifier.padding(vertical = spacing.space4))

        Text(
            text = stringResource(R.string.backup_import_mode_label).uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkMuted
        )
        Spacer(modifier = Modifier.height(spacing.space2))

        ImportModeOptionRow(
            title = stringResource(R.string.backup_import_mode_merge_title),
            description = stringResource(R.string.backup_import_mode_merge_desc),
            selected = candidate.selectedMode == BackupImportMode.MERGE_BY_ID,
            onClick = { onSelectMode(BackupImportMode.MERGE_BY_ID) }
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        ImportModeOptionRow(
            title = stringResource(R.string.backup_import_mode_replace_title),
            description = stringResource(R.string.backup_import_mode_replace_desc),
            selected = candidate.selectedMode == BackupImportMode.REPLACE_ALL,
            onClick = { onSelectMode(BackupImportMode.REPLACE_ALL) }
        )

        Spacer(modifier = Modifier.height(spacing.space3))
        Text(
            text = stringResource(R.string.backup_import_safety_note),
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )

        Spacer(modifier = Modifier.height(spacing.space4))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            FerroButton(
                text = stringResource(R.string.dialog_cancel),
                onClick = onCancel,
                enabled = enabled,
                variant = FerroButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
            FerroButton(
                text = stringResource(R.string.backup_import_confirm_action),
                onClick = onConfirm,
                enabled = enabled,
                variant = FerroButtonVariant.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewStatItem(label: String, count: Int, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    Column(modifier = modifier) {
        Text(
            text = count.toString(),
            style = FerroTheme.typography.number,
            color = colors.ink
        )
        Text(
            text = label,
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )
    }
}

@Composable
private fun ImportModeOptionRow(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing
    val shape = RoundedCornerShape(FerroTheme.radius.md)
    val borderColor = if (selected) colors.brasaText else colors.lineStrong

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
                .clickable(onClick = onClick)
                .padding(spacing.space3)
    ) {
        Text(
            text = title,
            style = FerroTheme.typography.bodyStrong,
            color = if (selected) colors.brasaText else colors.ink
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = description,
            style = FerroTheme.typography.caption,
            color = colors.inkMuted
        )
    }
}

private suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri)
            ?: return@withContext false
        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(content)
        }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

@FerroPreviews
@Composable
private fun BackupScreenPreview() {
    FerroTheme {
        BackupContent(
            uiState =
                BackupUiState(
                    lastBackupAtMillis = 1_700_000_000_000L,
                    isBackupStale = true,
                    pendingImport =
                        PendingImportCandidate(
                            rawJson = "{}",
                            preview =
                                BackupPreview(
                                    schemaVersion = 1,
                                    migratedFromSchemaVersion = 0,
                                    appVersion = "0.1.0",
                                    exportedAtMillis = 1_730_000_000_000L,
                                    workoutsCount = 24,
                                    routinesCount = 4,
                                    exercisesCount = 128,
                                    measurementsCount = 12
                                ),
                            selectedMode = BackupImportMode.MERGE_BY_ID
                        )
                ),
            onBack = {},
            onExportJsonClick = {},
            onImportJsonClick = {},
            onExportCsvClick = {},
            onSelectImportMode = {},
            onConfirmImport = {},
            onCancelImport = {},
            onDismissFeedback = {}
        )
    }
}
