package com.davidguijo.ferro.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidguijo.ferro.core.domain.BackupCalculations
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.BackupImportMode
import com.davidguijo.ferro.core.domain.model.BackupPreview
import com.davidguijo.ferro.core.domain.repository.BackupException
import com.davidguijo.ferro.core.domain.repository.BackupFailureReason
import com.davidguijo.ferro.core.domain.repository.BackupRepository
import com.davidguijo.ferro.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BackupFeedback {
    data object ExportJsonSuccess : BackupFeedback

    data object ExportCsvSuccess : BackupFeedback

    data class ImportSuccess(val safetyBackupFileName: String) : BackupFeedback

    data object ErrorCorrupted : BackupFeedback

    data object ErrorFutureSchema : BackupFeedback

    data object ErrorIo : BackupFeedback
}

data class PendingImportCandidate(
    val rawJson: String,
    val preview: BackupPreview,
    val selectedMode: BackupImportMode = BackupImportMode.MERGE_BY_ID
)

data class BackupUiState(
    val isBusy: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val lastBackupAtMillis: Long? = null,
    val isBackupStale: Boolean = false,
    val pendingImport: PendingImportCandidate? = null,
    val feedback: BackupFeedback? = null
)

@HiltViewModel
class BackupViewModel
@Inject
constructor(
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                val now = clock.millis()
                val stale =
                    settings.lastBackupAtMillis != null &&
                        BackupCalculations.isBackupReminderDue(
                            lastBackupAtMillis = settings.lastBackupAtMillis,
                            nowMillis = now,
                            hasCompletedWorkouts = true
                        )
                _uiState.update { state ->
                    state.copy(
                        weightUnit = settings.weightUnit,
                        appLanguage = settings.language,
                        lastBackupAtMillis = settings.lastBackupAtMillis,
                        isBackupStale = stale
                    )
                }
            }
        }
    }

    fun suggestedJsonFileName(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateStr =
            Instant.ofEpochMilli(clock.millis())
                .atZone(zoneId)
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "ferro-backup-$dateStr.json"
    }

    fun suggestedCsvFileName(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateStr =
            Instant.ofEpochMilli(clock.millis())
                .atZone(zoneId)
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "ferro-sets-$dateStr.csv"
    }

    /** Exporta el JSON completo y lo pasa a [writeContent] para guardarlo en el URI elegido. */
    fun exportJsonTo(writeContent: suspend (String) -> Boolean) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, feedback = null) }
        viewModelScope.launch {
            val feedback =
                try {
                    val jsonText = backupRepository.exportJson()
                    if (writeContent(jsonText)) {
                        BackupFeedback.ExportJsonSuccess
                    } else {
                        BackupFeedback.ErrorIo
                    }
                } catch (_: Exception) {
                    BackupFeedback.ErrorIo
                }
            _uiState.update { it.copy(isBusy = false, feedback = feedback) }
        }
    }

    /** Exporta el CSV de series completadas en la unidad elegida y lo escribe con [writeContent]. */
    fun exportCsvTo(zoneId: ZoneId, locale: Locale, writeContent: suspend (String) -> Boolean) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, feedback = null) }
        viewModelScope.launch {
            val state = _uiState.value
            val useEnglish =
                when (state.appLanguage) {
                    AppLanguage.EN -> true
                    AppLanguage.ES -> false
                    AppLanguage.SYSTEM -> locale.language.equals("en", ignoreCase = true)
                }
            val feedback =
                try {
                    val csvText =
                        backupRepository.exportSeriesCsv(
                            weightUnit = state.weightUnit,
                            zoneId = zoneId,
                            locale = locale,
                            useEnglishExerciseNames = useEnglish
                        )
                    if (writeContent(csvText)) {
                        BackupFeedback.ExportCsvSuccess
                    } else {
                        BackupFeedback.ErrorIo
                    }
                } catch (_: Exception) {
                    BackupFeedback.ErrorIo
                }
            _uiState.update { it.copy(isBusy = false, feedback = feedback) }
        }
    }

    /** Lee y valida el contenido de un archivo JSON seleccionado con `OpenDocument` para mostrar su vista previa. */
    fun onImportFileLoaded(rawJson: String?) {
        if (rawJson == null) {
            _uiState.update { it.copy(feedback = BackupFeedback.ErrorIo) }
            return
        }
        _uiState.update { it.copy(isBusy = true, feedback = null, pendingImport = null) }
        viewModelScope.launch {
            val result = backupRepository.validateAndPreviewJson(rawJson)
            result.fold(
                onSuccess = { preview ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImport =
                                PendingImportCandidate(
                                    rawJson = rawJson,
                                    preview = preview,
                                    selectedMode = BackupImportMode.MERGE_BY_ID
                                )
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            feedback = error.toBackupFeedback()
                        )
                    }
                }
            )
        }
    }

    fun onSelectImportMode(mode: BackupImportMode) {
        _uiState.update { state ->
            val current = state.pendingImport ?: return@update state
            state.copy(pendingImport = current.copy(selectedMode = mode))
        }
    }

    fun onCancelPendingImport() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    fun onConfirmImport() {
        val candidate = _uiState.value.pendingImport ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, feedback = null) }
        viewModelScope.launch {
            val result = backupRepository.importJson(candidate.rawJson, candidate.selectedMode)
            result.fold(
                onSuccess = { importResult ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImport = null,
                            feedback = BackupFeedback.ImportSuccess(importResult.safetyBackupFileName)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImport = null,
                            feedback = error.toBackupFeedback()
                        )
                    }
                }
            )
        }
    }

    fun onDismissFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    private fun Throwable.toBackupFeedback(): BackupFeedback = when ((this as? BackupException)?.reason) {
        BackupFailureReason.UNSUPPORTED_FUTURE_SCHEMA -> BackupFeedback.ErrorFutureSchema

        BackupFailureReason.CORRUPTED_JSON,
        BackupFailureReason.INVALID_DATA,
        null -> BackupFeedback.ErrorCorrupted
    }
}
