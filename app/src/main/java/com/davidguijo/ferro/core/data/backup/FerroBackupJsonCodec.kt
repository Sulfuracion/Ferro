package com.davidguijo.ferro.core.data.backup

import com.davidguijo.ferro.core.data.local.FerroDatabase
import com.davidguijo.ferro.core.domain.WeightUnit
import com.davidguijo.ferro.core.domain.model.AppLanguage
import com.davidguijo.ferro.core.domain.model.BackupSnapshot
import com.davidguijo.ferro.core.domain.model.BodyMeasurement
import com.davidguijo.ferro.core.domain.model.Equipment
import com.davidguijo.ferro.core.domain.model.Exercise
import com.davidguijo.ferro.core.domain.model.Muscle
import com.davidguijo.ferro.core.domain.model.PersonalRecord
import com.davidguijo.ferro.core.domain.model.ProgressPhoto
import com.davidguijo.ferro.core.domain.model.RecordType
import com.davidguijo.ferro.core.domain.model.Routine
import com.davidguijo.ferro.core.domain.model.RoutineExercise
import com.davidguijo.ferro.core.domain.model.RoutineFolder
import com.davidguijo.ferro.core.domain.model.RoutineSet
import com.davidguijo.ferro.core.domain.model.SetType
import com.davidguijo.ferro.core.domain.model.Settings
import com.davidguijo.ferro.core.domain.model.ThemeMode
import com.davidguijo.ferro.core.domain.model.TrackingType
import com.davidguijo.ferro.core.domain.model.Workout
import com.davidguijo.ferro.core.domain.model.WorkoutExercise
import com.davidguijo.ferro.core.domain.model.WorkoutSet
import com.davidguijo.ferro.core.domain.model.WorkoutStatus
import com.davidguijo.ferro.core.domain.repository.BackupException
import com.davidguijo.ferro.core.domain.repository.BackupFailureReason
import java.time.DayOfWeek
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
internal data class FerroBackupDocumentDto(
    val schemaVersion: Int = FerroDatabase.SCHEMA_VERSION,
    val appVersion: String = DEFAULT_APP_VERSION,
    val exportedAtMillis: Long = 0L,
    val settings: BackupSettingsDto = BackupSettingsDto(),
    val exercises: List<BackupExerciseDto> = emptyList(),
    val routineFolders: List<BackupRoutineFolderDto> = emptyList(),
    val routines: List<BackupRoutineDto> = emptyList(),
    val routineExercises: List<BackupRoutineExerciseDto> = emptyList(),
    val routineSets: List<BackupRoutineSetDto> = emptyList(),
    val workouts: List<BackupWorkoutDto> = emptyList(),
    val workoutExercises: List<BackupWorkoutExerciseDto> = emptyList(),
    val workoutSets: List<BackupWorkoutSetDto> = emptyList(),
    val personalRecords: List<BackupPersonalRecordDto> = emptyList(),
    val bodyMeasurements: List<BackupBodyMeasurementDto> = emptyList(),
    val progressPhotos: List<BackupProgressPhotoDto> = emptyList()
) {
    companion object {
        const val DEFAULT_APP_VERSION = "0.1.0"
    }
}

@Serializable
internal data class BackupSettingsDto(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultRestSeconds: Int = Settings.DEFAULT_REST_SECONDS,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val timerSoundEnabled: Boolean = true,
    val timerVibrationEnabled: Boolean = true
)

@Serializable
internal data class BackupExerciseDto(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: Equipment,
    val trackingType: TrackingType,
    val instructionsEs: String = "",
    val instructionsEn: String = "",
    val isCustom: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupRoutineFolderDto(
    val id: String,
    val name: String,
    val position: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupRoutineDto(
    val id: String,
    val folderId: String? = null,
    val name: String,
    val notes: String = "",
    val position: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupRoutineExerciseDto(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val position: Int = 0,
    val supersetGroup: String? = null,
    val restSeconds: Int? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupRoutineSetDto(
    val id: String,
    val routineExerciseId: String,
    val position: Int = 0,
    val setType: SetType = SetType.NORMAL,
    val targetWeightKg: Double? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationS: Int? = null,
    val targetDistanceM: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupWorkoutDto(
    val id: String,
    val name: String,
    val routineId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val notes: String = "",
    val status: WorkoutStatus = WorkoutStatus.COMPLETED,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupWorkoutExerciseDto(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val position: Int = 0,
    val supersetGroup: String? = null,
    val restSeconds: Int? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupWorkoutSetDto(
    val id: String,
    val workoutExerciseId: String,
    val position: Int = 0,
    val setType: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,
    val durationS: Int? = null,
    val distanceM: Double? = null,
    val completed: Boolean = true,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupPersonalRecordDto(
    val id: String,
    val exerciseId: String,
    val recordType: RecordType,
    val value: Double,
    val workoutSetId: String,
    val achievedAt: Long,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupBodyMeasurementDto(
    val id: String,
    val measuredAt: Long,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val armLCm: Double? = null,
    val armRCm: Double? = null,
    val thighLCm: Double? = null,
    val thighRCm: Double? = null,
    val calfCm: Double? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
internal data class BackupProgressPhotoDto(
    val id: String,
    val takenAt: Long,
    val fileName: String,
    val pose: String,
    val measurementId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

internal data class DecodedBackupPayload(val snapshot: BackupSnapshot, val originalSchemaVersion: Int)

/**
 * Codifica, valida y migra documentos JSON de copia de seguridad de Ferro.
 */
internal object FerroBackupJsonCodec {
    val json: Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun encode(snapshot: BackupSnapshot): String =
        json.encodeToString(FerroBackupDocumentDto.serializer(), snapshot.toDto())

    fun decodeAndValidate(rawJson: String): Result<DecodedBackupPayload> = runCatching {
        if (rawJson.isBlank()) {
            throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Backup JSON is empty")
        }
        val rootElement =
            try {
                json.parseToJsonElement(rawJson)
            } catch (e: SerializationException) {
                throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Malformed JSON", e)
            } catch (e: IllegalArgumentException) {
                throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Malformed JSON", e)
            }
        val rootObject =
            (rootElement as? JsonObject)
                ?: throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Root must be a JSON object")

        val rawVersion = extractOriginalSchemaVersion(rootObject)
        if (rawVersion > FerroDatabase.SCHEMA_VERSION) {
            throw BackupException(
                BackupFailureReason.UNSUPPORTED_FUTURE_SCHEMA,
                "Backup schema version $rawVersion is newer than supported ${FerroDatabase.SCHEMA_VERSION}"
            )
        }

        val migratedObject =
            if (rawVersion < FerroDatabase.SCHEMA_VERSION) {
                migrateToCurrentSchema(rootObject, fromVersion = rawVersion)
            } else {
                validateRequiredEnvelope(rootObject)
                rootObject
            }

        val dto =
            try {
                json.decodeFromJsonElement<FerroBackupDocumentDto>(migratedObject)
            } catch (e: SerializationException) {
                throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Invalid backup structure", e)
            } catch (e: IllegalArgumentException) {
                throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Invalid backup values", e)
            }

        validateSemanticIntegrity(dto)

        DecodedBackupPayload(
            snapshot = dto.toDomain(),
            originalSchemaVersion = rawVersion
        )
    }

    private fun extractOriginalSchemaVersion(root: JsonObject): Int {
        val versionPrimitive =
            (root["schemaVersion"] ?: root["version"] ?: root["schema_version"]) as? JsonPrimitive
                ?: throw BackupException(
                    BackupFailureReason.CORRUPTED_JSON,
                    "Missing schemaVersion field"
                )
        val parsed =
            versionPrimitive.intOrNull
                ?: throw BackupException(
                    BackupFailureReason.CORRUPTED_JSON,
                    "schemaVersion must be an integer"
                )
        if (parsed < 0) {
            throw BackupException(
                BackupFailureReason.CORRUPTED_JSON,
                "schemaVersion must be >= 0"
            )
        }
        return parsed
    }

    private fun validateRequiredEnvelope(root: JsonObject) {
        val hasAnyKnownSection =
            root.containsKey("exportedAtMillis") ||
                root.containsKey("settings") ||
                root.containsKey("exercises") ||
                root.containsKey("workouts") ||
                root.containsKey("routines") ||
                root.containsKey("bodyMeasurements")
        if (!hasAnyKnownSection) {
            throw BackupException(
                BackupFailureReason.CORRUPTED_JSON,
                "JSON object does not contain Ferro backup sections"
            )
        }
    }

    /**
     * Pipeline de migración paso a paso desde versiones de esquema antiguas (< [FerroDatabase.SCHEMA_VERSION])
     * hasta el esquema actual (`1`).
     *
     * Soporta archivos legacy (versión 0) donde:
     * - La versión podía venir en `version` o `schema_version` y la fecha en `exportedAt` / `createdAt`.
     * - Los ejercicios podían tener un único `name` / `instructions` en vez de `nameEs`/`nameEn`.
     * - Las tablas en serpiente (`routine_folders`, `routine_exercises`, `routine_sets`, `workout_exercises`,
     *   `workout_sets`, `personal_records`, `body_measurements`, `progress_photos`) o anidadas
     *   se normalizan al formato plano v1.
     */
    private fun migrateToCurrentSchema(root: JsonObject, fromVersion: Int): JsonObject {
        var current = root
        var version = fromVersion
        while (version < FerroDatabase.SCHEMA_VERSION) {
            current =
                when (version) {
                    0 -> migrateV0ToV1(current)
                    else -> current
                }
            version += 1
        }
        return current
    }

    private fun migrateV0ToV1(old: JsonObject): JsonObject {
        val exportedAt =
            (old["exportedAtMillis"] ?: old["exportedAt"] ?: old["timestamp"])
                ?.let { (it as? JsonPrimitive)?.longOrNull }
                ?: 0L
        val appVersion =
            (old["appVersion"] ?: old["app_version"])
                ?.let { (it as? JsonPrimitive)?.contentOrNull }
                ?.takeIf { it.isNotBlank() }
                ?: FerroBackupDocumentDto.DEFAULT_APP_VERSION

        val migratedExercises =
            (old["exercises"] as? JsonArray)?.map { element ->
                val obj =
                    element as? JsonObject
                        ?: throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Invalid exercise item")
                val fallbackName = (obj["name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val nameEs = (obj["nameEs"] as? JsonPrimitive)?.contentOrNull ?: fallbackName
                val nameEn = (obj["nameEn"] as? JsonPrimitive)?.contentOrNull ?: nameEs
                val fallbackInstr = (obj["instructions"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val instrEs = (obj["instructionsEs"] as? JsonPrimitive)?.contentOrNull ?: fallbackInstr
                val instrEn = (obj["instructionsEn"] as? JsonPrimitive)?.contentOrNull ?: instrEs
                val created = (obj["createdAt"] as? JsonPrimitive)?.longOrNull ?: exportedAt
                val updated = (obj["updatedAt"] as? JsonPrimitive)?.longOrNull ?: created
                JsonObject(
                    obj.toMutableMap().apply {
                        put("nameEs", JsonPrimitive(nameEs))
                        put("nameEn", JsonPrimitive(nameEn))
                        put("instructionsEs", JsonPrimitive(instrEs))
                        put("instructionsEn", JsonPrimitive(instrEn))
                        put("createdAt", JsonPrimitive(created))
                        put("updatedAt", JsonPrimitive(updated))
                        if (!containsKey("primaryMuscle") && containsKey("muscle")) {
                            put("primaryMuscle", getValue("muscle"))
                        }
                        if (!containsKey("trackingType")) {
                            put("trackingType", JsonPrimitive(TrackingType.WEIGHT_REPS.name))
                        }
                        if (!containsKey("equipment")) {
                            put("equipment", JsonPrimitive(Equipment.BARBELL.name))
                        }
                    }
                )
            } ?: emptyList()

        val migratedWorkouts =
            (old["workouts"] as? JsonArray)?.map { element ->
                val obj =
                    element as? JsonObject
                        ?: throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Invalid workout item")
                val startedAt =
                    ((obj["startedAt"] ?: obj["date"]) as? JsonPrimitive)?.longOrNull
                        ?: throw BackupException(BackupFailureReason.CORRUPTED_JSON, "Workout missing startedAt")
                val endedAt = (obj["endedAt"] as? JsonPrimitive)?.longOrNull
                val created = (obj["createdAt"] as? JsonPrimitive)?.longOrNull ?: startedAt
                val updated = (obj["updatedAt"] as? JsonPrimitive)?.longOrNull ?: (endedAt ?: startedAt)
                val statusStr =
                    (obj["status"] as? JsonPrimitive)?.contentOrNull
                        ?: if (obj["completed"]?.jsonPrimitive?.booleanOrNull == false) {
                            WorkoutStatus.IN_PROGRESS.name
                        } else {
                            WorkoutStatus.COMPLETED.name
                        }
                JsonObject(
                    obj.toMutableMap().apply {
                        put("startedAt", JsonPrimitive(startedAt))
                        put("createdAt", JsonPrimitive(created))
                        put("updatedAt", JsonPrimitive(updated))
                        put("status", JsonPrimitive(statusStr))
                    }
                )
            } ?: emptyList()

        val map = old.toMutableMap()
        map["schemaVersion"] = JsonPrimitive(1)
        map["appVersion"] = JsonPrimitive(appVersion)
        map["exportedAtMillis"] = JsonPrimitive(exportedAt)
        map["exercises"] = JsonArray(migratedExercises)
        map["workouts"] = JsonArray(migratedWorkouts)

        renameKeyIfNeeded(map, "routine_folders", "routineFolders")
        renameKeyIfNeeded(map, "routine_exercises", "routineExercises")
        renameKeyIfNeeded(map, "routine_sets", "routineSets")
        renameKeyIfNeeded(map, "workout_exercises", "workoutExercises")
        renameKeyIfNeeded(map, "workout_sets", "workoutSets")
        renameKeyIfNeeded(map, "personal_records", "personalRecords")
        renameKeyIfNeeded(map, "body_measurements", "bodyMeasurements")
        renameKeyIfNeeded(map, "progress_photos", "progressPhotos")

        return JsonObject(map)
    }

    private fun renameKeyIfNeeded(map: MutableMap<String, JsonElement>, oldKey: String, newKey: String) {
        if (!map.containsKey(newKey) && map.containsKey(oldKey)) {
            val value = map.remove(oldKey)
            if (value != null && value !is JsonNull) {
                map[newKey] = value
            }
        }
    }

    private fun validateSemanticIntegrity(dto: FerroBackupDocumentDto) {
        if (dto.exercises.any { it.id.isBlank() || (it.nameEs.isBlank() && it.nameEn.isBlank()) }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Exercise with blank id or name")
        }
        if (dto.routineFolders.any { it.id.isBlank() || it.name.isBlank() }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Routine folder with blank id or name")
        }
        if (dto.routines.any { it.id.isBlank() || it.name.isBlank() }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Routine with blank id or name")
        }
        if (dto.workouts.any { it.id.isBlank() || it.name.isBlank() }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Workout with blank id or name")
        }
        if (dto.bodyMeasurements.any { it.id.isBlank() }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Body measurement with blank id")
        }
        if (dto.progressPhotos.any { it.id.isBlank() || it.fileName.isBlank() }) {
            throw BackupException(BackupFailureReason.INVALID_DATA, "Progress photo with blank id or fileName")
        }
    }
}

private fun BackupSnapshot.toDto(): FerroBackupDocumentDto = FerroBackupDocumentDto(
    schemaVersion = schemaVersion,
    appVersion = appVersion,
    exportedAtMillis = exportedAtMillis,
    settings =
        BackupSettingsDto(
            language = settings.language,
            weightUnit = settings.weightUnit,
            themeMode = settings.themeMode,
            defaultRestSeconds = settings.defaultRestSeconds,
            firstDayOfWeek = settings.firstDayOfWeek,
            timerSoundEnabled = settings.timerSoundEnabled,
            timerVibrationEnabled = settings.timerVibrationEnabled
        ),
    exercises =
        exercises.map {
            BackupExerciseDto(
                id = it.id,
                nameEs = it.nameEs,
                nameEn = it.nameEn,
                primaryMuscle = it.primaryMuscle,
                secondaryMuscles = it.secondaryMuscles,
                equipment = it.equipment,
                trackingType = it.trackingType,
                instructionsEs = it.instructionsEs,
                instructionsEn = it.instructionsEn,
                isCustom = it.isCustom,
                archived = it.archived,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineFolders =
        routineFolders.map {
            BackupRoutineFolderDto(
                id = it.id,
                name = it.name,
                position = it.position,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routines =
        routines.map {
            BackupRoutineDto(
                id = it.id,
                folderId = it.folderId,
                name = it.name,
                notes = it.notes,
                position = it.position,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineExercises =
        routineExercises.map {
            BackupRoutineExerciseDto(
                id = it.id,
                routineId = it.routineId,
                exerciseId = it.exerciseId,
                position = it.position,
                supersetGroup = it.supersetGroup,
                restSeconds = it.restSeconds,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineSets =
        routineSets.map {
            BackupRoutineSetDto(
                id = it.id,
                routineExerciseId = it.routineExerciseId,
                position = it.position,
                setType = it.setType,
                targetWeightKg = it.targetWeightKg,
                targetRepsMin = it.targetRepsMin,
                targetRepsMax = it.targetRepsMax,
                targetDurationS = it.targetDurationS,
                targetDistanceM = it.targetDistanceM,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workouts =
        workouts.map {
            BackupWorkoutDto(
                id = it.id,
                name = it.name,
                routineId = it.routineId,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                notes = it.notes,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workoutExercises =
        workoutExercises.map {
            BackupWorkoutExerciseDto(
                id = it.id,
                workoutId = it.workoutId,
                exerciseId = it.exerciseId,
                position = it.position,
                supersetGroup = it.supersetGroup,
                restSeconds = it.restSeconds,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workoutSets =
        workoutSets.map {
            BackupWorkoutSetDto(
                id = it.id,
                workoutExerciseId = it.workoutExerciseId,
                position = it.position,
                setType = it.setType,
                weightKg = it.weightKg,
                reps = it.reps,
                rpe = it.rpe,
                durationS = it.durationS,
                distanceM = it.distanceM,
                completed = it.completed,
                completedAt = it.completedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    personalRecords =
        personalRecords.map {
            BackupPersonalRecordDto(
                id = it.id,
                exerciseId = it.exerciseId,
                recordType = it.recordType,
                value = it.value,
                workoutSetId = it.workoutSetId,
                achievedAt = it.achievedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    bodyMeasurements =
        bodyMeasurements.map {
            BackupBodyMeasurementDto(
                id = it.id,
                measuredAt = it.measuredAt,
                weightKg = it.weightKg,
                bodyFatPct = it.bodyFatPct,
                neckCm = it.neckCm,
                chestCm = it.chestCm,
                waistCm = it.waistCm,
                hipsCm = it.hipsCm,
                armLCm = it.armLCm,
                armRCm = it.armRCm,
                thighLCm = it.thighLCm,
                thighRCm = it.thighRCm,
                calfCm = it.calfCm,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    progressPhotos =
        progressPhotos.map {
            BackupProgressPhotoDto(
                id = it.id,
                takenAt = it.takenAt,
                fileName = it.fileName,
                pose = it.pose,
                measurementId = it.measurementId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
)

private fun FerroBackupDocumentDto.toDomain(): BackupSnapshot = BackupSnapshot(
    schemaVersion = schemaVersion,
    appVersion = appVersion,
    exportedAtMillis = exportedAtMillis,
    settings =
        Settings(
            language = settings.language,
            weightUnit = settings.weightUnit,
            themeMode = settings.themeMode,
            defaultRestSeconds =
                settings.defaultRestSeconds.coerceIn(Settings.MIN_REST_SECONDS, Settings.MAX_REST_SECONDS),
            firstDayOfWeek = settings.firstDayOfWeek,
            timerSoundEnabled = settings.timerSoundEnabled,
            timerVibrationEnabled = settings.timerVibrationEnabled
        ),
    exercises =
        exercises.map {
            Exercise(
                id = it.id,
                nameEs = it.nameEs.ifBlank { it.nameEn },
                nameEn = it.nameEn.ifBlank { it.nameEs },
                primaryMuscle = it.primaryMuscle,
                secondaryMuscles = it.secondaryMuscles,
                equipment = it.equipment,
                trackingType = it.trackingType,
                instructionsEs = it.instructionsEs,
                instructionsEn = it.instructionsEn,
                isCustom = it.isCustom,
                archived = it.archived,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineFolders =
        routineFolders.map {
            RoutineFolder(
                id = it.id,
                name = it.name,
                position = it.position,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routines =
        routines.map {
            Routine(
                id = it.id,
                folderId = it.folderId,
                name = it.name,
                notes = it.notes,
                position = it.position,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineExercises =
        routineExercises.map {
            RoutineExercise(
                id = it.id,
                routineId = it.routineId,
                exerciseId = it.exerciseId,
                position = it.position,
                supersetGroup = it.supersetGroup,
                restSeconds = it.restSeconds,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    routineSets =
        routineSets.map {
            RoutineSet(
                id = it.id,
                routineExerciseId = it.routineExerciseId,
                position = it.position,
                setType = it.setType,
                targetWeightKg = it.targetWeightKg,
                targetRepsMin = it.targetRepsMin,
                targetRepsMax = it.targetRepsMax,
                targetDurationS = it.targetDurationS,
                targetDistanceM = it.targetDistanceM,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workouts =
        workouts.map {
            Workout(
                id = it.id,
                name = it.name,
                routineId = it.routineId,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                notes = it.notes,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workoutExercises =
        workoutExercises.map {
            WorkoutExercise(
                id = it.id,
                workoutId = it.workoutId,
                exerciseId = it.exerciseId,
                position = it.position,
                supersetGroup = it.supersetGroup,
                restSeconds = it.restSeconds,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    workoutSets =
        workoutSets.map {
            WorkoutSet(
                id = it.id,
                workoutExerciseId = it.workoutExerciseId,
                position = it.position,
                setType = it.setType,
                weightKg = it.weightKg,
                reps = it.reps,
                rpe = it.rpe,
                durationS = it.durationS,
                distanceM = it.distanceM,
                completed = it.completed,
                completedAt = it.completedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    personalRecords =
        personalRecords.map {
            PersonalRecord(
                id = it.id,
                exerciseId = it.exerciseId,
                recordType = it.recordType,
                value = it.value,
                workoutSetId = it.workoutSetId,
                achievedAt = it.achievedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    bodyMeasurements =
        bodyMeasurements.map {
            BodyMeasurement(
                id = it.id,
                measuredAt = it.measuredAt,
                weightKg = it.weightKg,
                bodyFatPct = it.bodyFatPct,
                neckCm = it.neckCm,
                chestCm = it.chestCm,
                waistCm = it.waistCm,
                hipsCm = it.hipsCm,
                armLCm = it.armLCm,
                armRCm = it.armRCm,
                thighLCm = it.thighLCm,
                thighRCm = it.thighRCm,
                calfCm = it.calfCm,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        },
    progressPhotos =
        progressPhotos.map {
            ProgressPhoto(
                id = it.id,
                takenAt = it.takenAt,
                fileName = it.fileName,
                pose = it.pose,
                measurementId = it.measurementId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
)
