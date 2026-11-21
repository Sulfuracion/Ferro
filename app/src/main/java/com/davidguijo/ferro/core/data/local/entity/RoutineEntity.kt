package com.davidguijo.ferro.core.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = RoutineFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["folderId"])
    ]
)
internal data class RoutineEntity(
    @PrimaryKey val id: String,
    val folderId: String?,
    val name: String,
    val notes: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long
)
