package com.davidguijo.ferro.core.domain.model

data class PersonalRecord(
    val id: String,
    val exerciseId: String,
    val recordType: RecordType,
    val value: Double,
    val workoutSetId: String,
    val achievedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)
