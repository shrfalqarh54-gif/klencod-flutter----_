package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtilsAr
import com.example.data.local.AppDatabase
import com.example.data.local.BacklogEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TaskEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.data.repository.TaskRepository
import com.example.domain.ConflictEngine
import com.example.domain.FreeGap
import com.example.domain.NowMomentAnalysis
import com.example.domain.RecurrenceEngine
import com.example.domain.SmartEngine
import com.example.domain.TaskOccurrence
import com.example.service.AlarmScheduler
import com.example.service.BackupService
import com.example.service.ToneItem
import com.example.service.ToneManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = TaskRepository(db)
    val preferencesManager = PreferencesManager(application)
    val toneManager = ToneManager(application)

    val preferences: StateFlow<UserPreferences> = preferencesManager.preferencesFlow

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backlog: StateFlow<List<BacklogEntity>> = repository.allBacklogFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEpochDay = MutableStateFlow(DateUtilsAr.todayEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    val occurrences: StateFlow<List<TaskOccurrence>> = _selectedEpochDay.flatMapLatest { epoch ->
        repository.getDayOccurrencesFlow(epoch)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Now Analysis for Today
    val nowMomentAnalysis: StateFlow<NowMomentAnalysis> = repository.getDayOccurrencesFlow(DateUtilsAr.todayEpochDay())
        .map { todayTasks ->
            SmartEngine.analyzeNowMoment(todayTasks, DateUtilsAr.currentMinuteOfDay())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SmartEngine.analyzeNowMoment(emptyList(), DateUtilsAr.currentMinuteOfDay())
        )

    private val _availableTones = MutableStateFlow(toneManager.getAllTones())
    val availableTones: StateFlow<List<ToneItem>> = _availableTones.asStateFlow()

    fun selectDay(epochDay: Long) {
        _selectedEpochDay.value = epochDay
    }

    fun saveTask(task: TaskEntity, subtaskTitles: List<String>, updateFuture: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getTaskById(task.id)
            if (existing != null) {
                if (updateFuture && existing.recurrenceType != "once") {
                    repository.updateThisAndFutureOccurrences(
                        oldTask = existing,
                        fromEpochDay = task.startEpochDay,
                        newTitle = task.title,
                        newStartMinute = task.startMinute,
                        newEndMinute = task.endMinute,
                        newNote = task.note,
                        newCategoryId = task.categoryId,
                        newAlarmStart = task.alarmStartEnabled,
                        newAlarmEnd = task.alarmEndEnabled,
                        newToneType = task.toneType
                    )
                } else {
                    repository.updateTask(task, subtaskTitles)
                }
            } else {
                repository.insertTask(task, subtaskTitles)
            }
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun markDone(occurrence: TaskOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setOccurrenceStatus(occurrence.taskId, occurrence.epochDay, "done")
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun markMissed(occurrence: TaskOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setOccurrenceStatus(occurrence.taskId, occurrence.epochDay, "missed")
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun deleteOccurrence(occurrence: TaskOccurrence, deleteFuture: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (occurrence.isRecurring) {
                if (deleteFuture) {
                    val task = repository.getTaskById(occurrence.taskId)
                    if (task != null) {
                        repository.deleteThisAndFutureOccurrences(task, occurrence.epochDay)
                    }
                } else {
                    repository.deleteSingleOccurrence(occurrence.taskId, occurrence.epochDay)
                }
            } else {
                repository.deleteTask(occurrence.taskId)
            }
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun rescheduleOccurrence(
        occurrence: TaskOccurrence,
        newStartMinute: Int,
        newEndMinute: Int,
        newEpochDay: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel occurrence on current day and schedule on new day
            repository.setOccurrenceStatus(occurrence.taskId, occurrence.epochDay, "moved")
            val newTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = occurrence.title,
                categoryId = occurrence.categoryId,
                note = occurrence.note,
                startMinute = newStartMinute,
                endMinute = newEndMinute,
                durationMinutes = newEndMinute - newStartMinute,
                startEpochDay = newEpochDay,
                recurrenceType = "once",
                alarmStartEnabled = occurrence.alarmStartEnabled,
                alarmEndEnabled = occurrence.alarmEndEnabled,
                toneType = occurrence.toneType,
                toneRef = occurrence.toneRef
            )
            repository.insertTask(newTask)
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun moveOccurrenceToBacklog(occurrence: TaskOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveOccurrenceToBacklog(occurrence)
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    fun addBacklogItem(title: String, durationMinutes: Int?, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBacklogItem(title, durationMinutes, note, null)
        }
    }

    fun deleteBacklogItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBacklogItem(id)
        }
    }

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        preferencesManager.updatePreferences(update)
    }

    fun saveCustomTone(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = toneManager.saveCustomToneFromUri(uri, displayName)
            if (item != null) {
                _availableTones.value = toneManager.getAllTones()
            }
        }
    }

    fun playTonePreview(toneId: String) {
        toneManager.playPreview(toneId)
    }

    fun stopTonePreview() {
        toneManager.stopPreview()
    }

    fun testAlarm() {
        AlarmScheduler.scheduleTestAlarm(getApplication(), 10)
    }

    fun wipeAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            AlarmScheduler.rescheduleRollingWindow(getApplication())
        }
    }

    suspend fun exportBackupJson(): String {
        return BackupService.exportToJson(getApplication())
    }

    suspend fun importBackupJson(json: String, replace: Boolean): Boolean {
        return BackupService.importFromJson(getApplication(), json, replace)
    }

    override fun onCleared() {
        super.onCleared()
        toneManager.stopPreview()
    }
}
