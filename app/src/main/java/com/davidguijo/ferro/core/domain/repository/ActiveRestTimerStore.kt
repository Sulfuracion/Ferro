package com.davidguijo.ferro.core.domain.repository

import com.davidguijo.ferro.core.domain.RestTimerState

/** Persistencia ligera del temporizador de descanso activo para sobrevivir a la muerte del proceso. */
interface ActiveRestTimerStore {
    suspend fun load(): RestTimerState?

    suspend fun save(state: RestTimerState?)
}
