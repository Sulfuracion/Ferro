package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.BackupImportMode
import com.davidguijo.ferro.core.domain.model.BackupImportResult
import com.davidguijo.ferro.core.domain.model.BackupPreview
import com.davidguijo.ferro.core.domain.model.BackupSnapshot
import java.io.File
import java.time.ZoneId
import java.util.Locale

/** Error tipado al validar o importar una copia de seguridad JSON. */
enum class BackupFailureReason {
    CORRUPTED_JSON,
    UNSUPPORTED_FUTURE_SCHEMA,
    INVALID_DATA
}

class BackupException(val reason: BackupFailureReason, message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

interface BackupRepository {
    /** Exporta todas las tablas de Room y los ajustes en JSON y registra la fecha de la última copia. */
    suspend fun exportJson(): String

    /** Toma una instantánea completa en memoria del estado actual (tablas + ajustes). */
    suspend fun captureSnapshot(): BackupSnapshot

    /** Valida un JSON de copia (migrándolo si es de una versión anterior) y devuelve su vista previa. */
    suspend fun validateAndPreviewJson(rawJson: String): Result<BackupPreview>

    /**
     * Guarda primero una copia automática de los datos actuales en `filesDir/backups` y luego
     * importa [rawJson] en una sola transacción de Room según [mode] (reemplazar todo o fusionar por ID).
     */
    suspend fun importJson(rawJson: String, mode: BackupImportMode): Result<BackupImportResult>

    /** Genera el CSV de todas las series completadas de entrenos completados en la unidad [weightUnit]. */
    suspend fun exportSeriesCsv(
        weightUnit: WeightUnit,
        zoneId: ZoneId,
        locale: Locale,
        useEnglishExerciseNames: Boolean
    ): String

    /** Devuelve el último archivo de copia automática pre-importación guardado en `filesDir/backups`, si existe. */
    fun latestSafetyBackupFile(): File?
}
