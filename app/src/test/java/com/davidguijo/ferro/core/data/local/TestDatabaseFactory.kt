package com.davidguijo.ferro.core.data.local

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.robolectric.RuntimeEnvironment

internal fun createInMemoryFerroDatabase(): FerroDatabase =
    Room.inMemoryDatabaseBuilder<FerroDatabase>(RuntimeEnvironment.getApplication())
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
