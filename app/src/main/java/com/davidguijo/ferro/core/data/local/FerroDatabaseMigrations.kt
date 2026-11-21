package com.davidguijo.ferro.core.data.local

import androidx.room3.migration.Migration

/**
 * Registro central de migraciones manuales de [FerroDatabase].
 *
 * Cada incremento de [FerroDatabase.SCHEMA_VERSION] debe añadir aquí su objeto [Migration]
 * (nunca `fallbackToDestructiveMigration`) y su correspondiente prueba en `FerroDatabaseMigrationTest`.
 */
object FerroDatabaseMigrations {
    val ALL: Array<Migration> = emptyArray()
}
