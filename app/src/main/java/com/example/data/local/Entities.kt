package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val note: String? = null,
    val startMinute: Int, // 0..1439
    val endMinute: Int,   // 1..1440
    val durationMinutes: Int,
    val startEpochDay: Long,
    val recurrenceType: String = "once", // "once", "daily", "weekly", "daysOfWeek", "monthly"
    val recurrenceDaysMask: Int = 0,    // bitmask: Sat=1, Sun=2, Mon=4, Tue=8, Wed=16, Thu=32, Fri=64
    val recurrenceEndEpochDay: Long? = null,
    val alarmStartEnabled: Boolean = true,
    val alarmEndEnabled: Boolean = true,
    val toneType: String = "defaultTone", // "defaultTone", "appTone", "customTone"
    val toneRef: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_exceptions",
    indices = [Index(value = ["taskId", "originalEpochDay"])]
)
data class TaskExceptionEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val originalEpochDay: Long,
    val type: String, // "cancelled", "override"
    val overrideTitle: String? = null,
    val overrideStart: Int? = null,
    val overrideEnd: Int? = null,
    val overrideEpochDay: Long? = null,
    val overrideNote: String? = null,
    val overrideAlarmStart: Boolean? = null,
    val overrideAlarmEnd: Boolean? = null,
    val overrideTone: String? = null
)

@Entity(
    tableName = "occurrence_state",
    primaryKeys = ["taskId", "epochDay"]
)
data class OccurrenceStateEntity(
    val taskId: String,
    val epochDay: Long,
    val status: String = "pending", // "pending", "done", "missed", "skipped", "moved"
    val completedAt: Long? = null
)

@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val title: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "subtask_state",
    primaryKeys = ["subtaskId", "epochDay"]
)
data class SubtaskStateEntity(
    val subtaskId: String,
    val epochDay: Long,
    val done: Boolean = false
)

@Entity(tableName = "backlog")
data class BacklogEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val durationMinutes: Int? = null,
    val note: String? = null,
    val categoryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val emoji: String,
    val colorValue: Long,
    val isDefault: Boolean = false
)

@Entity(tableName = "scheduled_alarms")
data class ScheduledAlarmEntity(
    @PrimaryKey
    val requestCode: Int,
    val taskId: String,
    val epochDay: Long,
    val kind: String, // "start", "end"
    val triggerAtMillis: Long
)
