package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "routine_folders")
internal data class RoutineFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long
)
