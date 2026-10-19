package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface TaskExceptionDao {
    @Query("SELECT * FROM task_exceptions WHERE taskId = :taskId")
    suspend fun getExceptionsForTask(taskId: String): List<TaskExceptionEntity>

    @Query("SELECT * FROM task_exceptions")
    suspend fun getAllExceptions(): List<TaskExceptionEntity>

    @Query("SELECT * FROM task_exceptions")
    fun getAllExceptionsFlow(): Flow<List<TaskExceptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(exception: TaskExceptionEntity)

    @Query("DELETE FROM task_exceptions WHERE taskId = :taskId")
    suspend fun deleteExceptionsForTask(taskId: String)

    @Query("DELETE FROM task_exceptions")
    suspend fun clearAll()
}

@Dao
interface OccurrenceDao {
    @Query("SELECT * FROM occurrence_state WHERE taskId = :taskId AND epochDay = :epochDay")
    suspend fun getOccurrenceState(taskId: String, epochDay: Long): OccurrenceStateEntity?

    @Query("SELECT * FROM occurrence_state WHERE epochDay = :epochDay")
    suspend fun getOccurrencesForDay(epochDay: Long): List<OccurrenceStateEntity>

    @Query("SELECT * FROM occurrence_state WHERE epochDay = :epochDay")
    fun getOccurrencesForDayFlow(epochDay: Long): Flow<List<OccurrenceStateEntity>>

    @Query("SELECT * FROM occurrence_state")
    suspend fun getAllOccurrenceStates(): List<OccurrenceStateEntity>

    @Query("SELECT * FROM occurrence_state")
    fun getAllOccurrenceStatesFlow(): Flow<List<OccurrenceStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOccurrenceState(state: OccurrenceStateEntity)

    @Query("DELETE FROM occurrence_state WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("DELETE FROM occurrence_state")
    suspend fun clearAll()
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun getSubtasksForTaskFlow(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasks(): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)

    @Query("SELECT * FROM subtask_state WHERE subtaskId = :subtaskId AND epochDay = :epochDay")
    suspend fun getSubtaskState(subtaskId: String, epochDay: Long): SubtaskStateEntity?

    @Query("SELECT * FROM subtask_state WHERE epochDay = :epochDay")
    suspend fun getSubtaskStatesForDay(epochDay: Long): List<SubtaskStateEntity>

    @Query("SELECT * FROM subtask_state WHERE epochDay = :epochDay")
    fun getSubtaskStatesForDayFlow(epochDay: Long): Flow<List<SubtaskStateEntity>>

    @Query("SELECT * FROM subtask_state")
    suspend fun getAllSubtaskStates(): List<SubtaskStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSubtaskState(state: SubtaskStateEntity)

    @Query("DELETE FROM subtasks")
    suspend fun clearAll()

    @Query("DELETE FROM subtask_state")
    suspend fun clearAllStates()
}

@Dao
interface BacklogDao {
    @Query("SELECT * FROM backlog ORDER BY createdAt DESC")
    fun getAllBacklogFlow(): Flow<List<BacklogEntity>>

    @Query("SELECT * FROM backlog ORDER BY createdAt DESC")
    suspend fun getAllBacklog(): List<BacklogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBacklog(item: BacklogEntity)

    @Query("DELETE FROM backlog WHERE id = :id")
    suspend fun deleteBacklog(id: String)

    @Query("DELETE FROM backlog")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM scheduled_alarms")
    suspend fun getAllScheduledAlarms(): List<ScheduledAlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE requestCode = :requestCode")
    suspend fun deleteAlarm(requestCode: Int)

    @Query("DELETE FROM scheduled_alarms WHERE taskId = :taskId")
    suspend fun deleteAlarmsForTask(taskId: String)

    @Query("DELETE FROM scheduled_alarms")
    suspend fun clearAll()
}
