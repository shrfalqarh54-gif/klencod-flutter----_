package com.example.data.repository

import com.example.core.DateUtilsAr
import com.example.data.local.AppDatabase
import com.example.data.local.BacklogEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.OccurrenceStateEntity
import com.example.data.local.SubtaskEntity
import com.example.data.local.SubtaskStateEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskExceptionEntity
import com.example.domain.ConflictEngine
import com.example.domain.RecurrenceEngine
import com.example.domain.TaskOccurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class TaskRepository(private val db: AppDatabase) {

    val allTasksFlow: Flow<List<TaskEntity>> = db.taskDao().getAllTasksFlow()
    val allCategoriesFlow: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategoriesFlow()
    val allBacklogFlow: Flow<List<BacklogEntity>> = db.backlogDao().getAllBacklogFlow()

    fun getDayOccurrencesFlow(epochDay: Long): Flow<List<TaskOccurrence>> {
        return combine(
            db.taskDao().getAllTasksFlow(),
            db.taskExceptionDao().getAllExceptionsFlow(),
            db.occurrenceDao().getAllOccurrenceStatesFlow(),
            db.subtaskDao().getSubtaskStatesForDayFlow(epochDay)
        ) { tasks, exceptions, states, subtaskStates ->
            val allSubtasks = db.subtaskDao().getAllSubtasks()
            val occurrences = RecurrenceEngine.expandOccurrencesForDay(
                epochDay = epochDay,
                tasks = tasks,
                exceptions = exceptions,
                occurrenceStates = states,
                subtasks = allSubtasks,
                subtaskStates = subtaskStates,
                currentMinute = DateUtilsAr.currentMinuteOfDay(),
                isToday = (epochDay == DateUtilsAr.todayEpochDay())
            )
            ConflictEngine.markAllConflictsInDay(occurrences)
        }
    }

    suspend fun getDayOccurrences(epochDay: Long): List<TaskOccurrence> {
        val tasks = db.taskDao().getAllTasks()
        val exceptions = db.taskExceptionDao().getAllExceptions()
        val states = db.occurrenceDao().getAllOccurrenceStates()
        val subtasks = db.subtaskDao().getAllSubtasks()
        val subtaskStates = db.subtaskDao().getSubtaskStatesForDay(epochDay)

        val occurrences = RecurrenceEngine.expandOccurrencesForDay(
            epochDay = epochDay,
            tasks = tasks,
            exceptions = exceptions,
            occurrenceStates = states,
            subtasks = subtasks,
            subtaskStates = subtaskStates,
            currentMinute = DateUtilsAr.currentMinuteOfDay(),
            isToday = (epochDay == DateUtilsAr.todayEpochDay())
        )
        return ConflictEngine.markAllConflictsInDay(occurrences)
    }

    suspend fun insertTask(task: TaskEntity, subtaskTitles: List<String> = emptyList()) {
        db.taskDao().insertTask(task)
        if (subtaskTitles.isNotEmpty()) {
            val subtaskEntities = subtaskTitles.mapIndexed { index, title ->
                SubtaskEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    title = title,
                    sortOrder = index
                )
            }
            db.subtaskDao().insertSubtasks(subtaskEntities)
        }
    }

    suspend fun updateTask(task: TaskEntity, subtaskTitles: List<String>? = null) {
        db.taskDao().updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        if (subtaskTitles != null) {
            db.subtaskDao().deleteSubtasksForTask(task.id)
            val subtaskEntities = subtaskTitles.mapIndexed { index, title ->
                SubtaskEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    title = title,
                    sortOrder = index
                )
            }
            db.subtaskDao().insertSubtasks(subtaskEntities)
        }
    }

    suspend fun updateSingleOccurrence(
        taskId: String,
        epochDay: Long,
        title: String,
        startMinute: Int,
        endMinute: Int,
        note: String?
    ) {
        val exception = TaskExceptionEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            originalEpochDay = epochDay,
            type = "override",
            overrideTitle = title,
            overrideStart = startMinute,
            overrideEnd = endMinute,
            overrideNote = note
        )
        db.taskExceptionDao().insertException(exception)
    }

    suspend fun updateThisAndFutureOccurrences(
        oldTask: TaskEntity,
        fromEpochDay: Long,
        newTitle: String,
        newStartMinute: Int,
        newEndMinute: Int,
        newNote: String?,
        newCategoryId: String?,
        newAlarmStart: Boolean,
        newAlarmEnd: Boolean,
        newToneType: String
    ) {
        // End the previous series at the day before fromEpochDay
        val previousDay = fromEpochDay - 1
        if (previousDay >= oldTask.startEpochDay) {
            db.taskDao().updateTask(oldTask.copy(recurrenceEndEpochDay = previousDay))
        } else {
            // Replaced right at the beginning
            db.taskDao().deleteTaskById(oldTask.id)
        }

        // Start new recurring or single task from fromEpochDay
        val newTask = oldTask.copy(
            id = UUID.randomUUID().toString(),
            title = newTitle,
            startMinute = newStartMinute,
            endMinute = newEndMinute,
            durationMinutes = newEndMinute - newStartMinute,
            startEpochDay = fromEpochDay,
            note = newNote,
            categoryId = newCategoryId,
            alarmStartEnabled = newAlarmStart,
            alarmEndEnabled = newAlarmEnd,
            toneType = newToneType,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.taskDao().insertTask(newTask)
    }

    suspend fun deleteTask(taskId: String) {
        db.taskDao().deleteTaskById(taskId)
        db.taskExceptionDao().deleteExceptionsForTask(taskId)
        db.occurrenceDao().deleteForTask(taskId)
        db.subtaskDao().deleteSubtasksForTask(taskId)
        db.alarmDao().deleteAlarmsForTask(taskId)
    }

    suspend fun deleteSingleOccurrence(taskId: String, epochDay: Long) {
        val exception = TaskExceptionEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            originalEpochDay = epochDay,
            type = "cancelled"
        )
        db.taskExceptionDao().insertException(exception)
    }

    suspend fun deleteThisAndFutureOccurrences(task: TaskEntity, fromEpochDay: Long) {
        val previousDay = fromEpochDay - 1
        if (previousDay >= task.startEpochDay) {
            db.taskDao().updateTask(task.copy(recurrenceEndEpochDay = previousDay))
        } else {
            deleteTask(task.id)
        }
    }

    suspend fun setOccurrenceStatus(taskId: String, epochDay: Long, status: String) {
        val completedAt = if (status == "done") System.currentTimeMillis() else null
        db.occurrenceDao().setOccurrenceState(
            OccurrenceStateEntity(
                taskId = taskId,
                epochDay = epochDay,
                status = status,
                completedAt = completedAt
            )
        )
    }

    suspend fun toggleSubtaskState(subtaskId: String, epochDay: Long, done: Boolean) {
        db.subtaskDao().setSubtaskState(
            SubtaskStateEntity(
                subtaskId = subtaskId,
                epochDay = epochDay,
                done = done
            )
        )
    }

    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity> {
        return db.subtaskDao().getSubtasksForTask(taskId)
    }

    suspend fun getTaskById(taskId: String): TaskEntity? {
        return db.taskDao().getTaskById(taskId)
    }

    suspend fun insertBacklogItem(title: String, durationMinutes: Int?, note: String?, categoryId: String?) {
        val item = BacklogEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            durationMinutes = durationMinutes,
            note = note,
            categoryId = categoryId
        )
        db.backlogDao().insertBacklog(item)
    }

    suspend fun deleteBacklogItem(id: String) {
        db.backlogDao().deleteBacklog(id)
    }

    suspend fun moveOccurrenceToBacklog(occurrence: TaskOccurrence) {
        insertBacklogItem(
            title = occurrence.title,
            durationMinutes = occurrence.durationMinutes,
            note = occurrence.note,
            categoryId = occurrence.categoryId
        )
        setOccurrenceStatus(occurrence.taskId, occurrence.epochDay, "moved")
    }

    suspend fun clearAllData() {
        db.taskDao().clearAll()
        db.taskExceptionDao().clearAll()
        db.occurrenceDao().clearAll()
        db.subtaskDao().clearAll()
        db.subtaskDao().clearAllStates()
        db.backlogDao().clearAll()
        db.alarmDao().clearAll()
    }
}
