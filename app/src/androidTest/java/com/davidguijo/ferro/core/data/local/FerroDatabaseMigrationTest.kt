package com.davidguijo.ferro.core.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Infraestructura de pruebas de migración de [FerroDatabase] con [MigrationTestHelper] de Room 3.
 *
 * El plugin `androidx.room3` expone automáticamente `app/schemas` como assets de `androidTest`.
 * Cuando se incremente [FerroDatabase.SCHEMA_VERSION] a 2, añade aquí `migrate1To2()`
 * verificando los datos y el esquema tras ejecutar `FerroDatabaseMigrations.MIGRATION_1_2`.
 */
@RunWith(AndroidJUnit4::class)
class FerroDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = instrumentation.targetContext.getDatabasePath(TEST_DB_NAME),
            driver = AndroidSQLiteDriver(),
            databaseClass = FerroDatabase::class
        )

    @Test
    fun createVersion1AndValidateCurrentSchema() = runTest {
        helper.createDatabase(version = 1).close()

        helper
            .runMigrationsAndValidate(
                version = FerroDatabase.SCHEMA_VERSION,
                migrations = FerroDatabaseMigrations.ALL.toList()
            )
            .close()
    }

    private companion object {
        const val TEST_DB_NAME = "ferro-migration-test.db"
    }
}
