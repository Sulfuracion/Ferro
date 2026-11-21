package com.davidguijo.ferro.core.domain.model

/** Tipo de serie dentro de una rutina o un entreno. */
enum class SetType {
    WARMUP,
    NORMAL,
    FAILURE,
    DROP
}

/** Qué magnitudes se registran para un ejercicio. */
enum class TrackingType {
    WEIGHT_REPS,
    BODYWEIGHT_REPS,
    WEIGHTED_BODYWEIGHT,
    ASSISTED_BODYWEIGHT,
    DURATION,
    DISTANCE_DURATION
}

/** Grupo muscular principal o secundario de un ejercicio. */
enum class Muscle {
    CHEST,
    LATS,
    UPPER_BACK,
    LOWER_BACK,
    TRAPS,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    ADDUCTORS,
    ABDUCTORS,
    FULL_BODY,
    CARDIO
}

/** Equipamiento necesario para un ejercicio. */
enum class Equipment {
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    BAND,
    PLATE,
    EZ_BAR,
    TRAP_BAR,
    SMITH_MACHINE,
    BENCH,
    PULL_UP_BAR,
    CARDIO_MACHINE,
    OTHER
}

/** Tipo de récord personal registrado en una serie. */
enum class RecordType {
    MAX_WEIGHT,
    ESTIMATED_1RM,
    BEST_SET_VOLUME,
    MAX_REPS_AT_WEIGHT
}

/** Estado del ciclo de vida de un entrenamiento. */
enum class WorkoutStatus {
    IN_PROGRESS,
    COMPLETED,
    DISCARDED
}
