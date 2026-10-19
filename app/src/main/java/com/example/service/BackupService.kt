package com.example.service

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.BacklogEntity
import com.example.data.local.OccurrenceStateEntity
import com.example.data.local.SubtaskEntity
import com.example.data.local.SubtaskStateEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskExceptionEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek

object BackupService {

    suspend fun exportToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context).preferencesFlow.value

        val root = JSONObject()
        root.put("app", "PowerOfNow")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // Preferences
        val prefsObj = JSONObject().apply {
            put("defaultTone", prefs.defaultTone)
            put("ringDurationSeconds", prefs.ringDurationSeconds)
            put("vibrationEnabled", prefs.vibrationEnabled)
            put("allowConflictOverride", prefs.allowConflictOverride)
            put("weekStartDay", prefs.weekStartDay.name)
            put("is24HourFormat", prefs.is24HourFormat)
            put("quietHoursEnabled", prefs.quietHoursEnabled)
            put("quietHoursStartMinute", prefs.quietHoursStartMinute)
            put("quietHoursEndMinute", prefs.quietHoursEndMinute)
        }
        root.put("preferences", prefsObj)

        // Tasks
        val tasksArray = JSONArray()
        for (task in db.taskDao().getAllTasks()) {
            val taskObj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("categoryId", task.categoryId)
                put("note", task.note)
                put("startMinute", task.startMinute)
                put("endMinute", task.endMinute)
                put("durationMinutes", task.durationMinutes)
                put("startEpochDay", task.startEpochDay)
                put("recurrenceType", task.recurrenceType)
                put("recurrenceDaysMask", task.recurrenceDaysMask)
                put("recurrenceEndEpochDay", task.recurrenceEndEpochDay ?: JSONObject.NULL)
                put("alarmStartEnabled", task.alarmStartEnabled)
                put("alarmEndEnabled", task.alarmEndEnabled)
                put("toneType", task.toneType)
                put("toneRef", task.toneRef)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
            }
            tasksArray.put(taskObj)
        }
        root.put("tasks", tasksArray)

        // Exceptions
        val exceptionsArray = JSONArray()
        for (ex in db.taskExceptionDao().getAllExceptions()) {
            val exObj = JSONObject().apply {
                put("id", ex.id)
                put("taskId", ex.taskId)
                put("originalEpochDay", ex.originalEpochDay)
                put("type", ex.type)
                put("overrideTitle", ex.overrideTitle)
                put("overrideStart", ex.overrideStart ?: JSONObject.NULL)
                put("overrideEnd", ex.overrideEnd ?: JSONObject.NULL)
                put("overrideEpochDay", ex.overrideEpochDay ?: JSONObject.NULL)
                put("overrideNote", ex.overrideNote)
                put("overrideAlarmStart", ex.overrideAlarmStart ?: JSONObject.NULL)
                put("overrideAlarmEnd", ex.overrideAlarmEnd ?: JSONObject.NULL)
                put("overrideTone", ex.overrideTone)
            }
            exceptionsArray.put(exObj)
        }
        root.put("taskExceptions", exceptionsArray)

        // Occurrence states
        val statesArray = JSONArray()
        for (st in db.occurrenceDao().getAllOccurrenceStates()) {
            val stObj = JSONObject().apply {
                put("taskId", st.taskId)
                put("epochDay", st.epochDay)
                put("status", st.status)
                put("completedAt", st.completedAt ?: JSONObject.NULL)
            }
            statesArray.put(stObj)
        }
        root.put("occurrenceStates", statesArray)

        // Subtasks
        val subtasksArray = JSONArray()
        for (sub in db.subtaskDao().getAllSubtasks()) {
            val subObj = JSONObject().apply {
                put("id", sub.id)
                put("taskId", sub.taskId)
                put("title", sub.title)
                put("sortOrder", sub.sortOrder)
            }
            subtasksArray.put(subObj)
        }
        root.put("subtasks", subtasksArray)

        // Subtask states
        val subtaskStatesArray = JSONArray()
        for (sst in db.subtaskDao().getAllSubtaskStates()) {
            val sstObj = JSONObject().apply {
                put("subtaskId", sst.subtaskId)
                put("epochDay", sst.epochDay)
                put("done", sst.done)
            }
            subtaskStatesArray.put(sstObj)
        }
        root.put("subtaskStates", subtaskStatesArray)

        // Backlog
        val backlogArray = JSONArray()
        for (bl in db.backlogDao().getAllBacklog()) {
            val blObj = JSONObject().apply {
                put("id", bl.id)
                put("title", bl.title)
                put("durationMinutes", bl.durationMinutes ?: JSONObject.NULL)
                put("note", bl.note)
                put("categoryId", bl.categoryId)
                put("createdAt", bl.createdAt)
            }
            backlogArray.put(blObj)
        }
        root.put("backlog", backlogArray)

        root.toString(2)
    }

    suspend fun importFromJson(context: Context, jsonString: String, replaceAll: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val root = JSONObject(jsonString)
            val db = AppDatabase.getInstance(context)

            if (replaceAll) {
                db.taskDao().clearAll()
                db.taskExceptionDao().clearAll()
                db.occurrenceDao().clearAll()
                db.subtaskDao().clearAll()
                db.subtaskDao().clearAllStates()
                db.backlogDao().clearAll()
            }

            // Restore tasks
            if (root.has("tasks")) {
                val tasksArray = root.getJSONArray("tasks")
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    val endDay = if (obj.isNull("recurrenceEndEpochDay")) null else obj.getLong("recurrenceEndEpochDay")
                    val task = TaskEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        categoryId = obj.optString("categoryId").takeIf { it.isNotEmpty() && it != "null" },
                        note = obj.optString("note").takeIf { it.isNotEmpty() && it != "null" },
                        startMinute = obj.getInt("startMinute"),
                        endMinute = obj.getInt("endMinute"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        startEpochDay = obj.getLong("startEpochDay"),
                        recurrenceType = obj.optString("recurrenceType", "once"),
                        recurrenceDaysMask = obj.optInt("recurrenceDaysMask", 0),
                        recurrenceEndEpochDay = endDay,
                        alarmStartEnabled = obj.optBoolean("alarmStartEnabled", true),
                        alarmEndEnabled = obj.optBoolean("alarmEndEnabled", true),
                        toneType = obj.optString("toneType", "defaultTone"),
                        toneRef = obj.optString("toneRef").takeIf { it.isNotEmpty() && it != "null" },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    db.taskDao().insertTask(task)
                }
            }

            // Restore exceptions
            if (root.has("taskExceptions")) {
                val exArray = root.getJSONArray("taskExceptions")
                for (i in 0 until exArray.length()) {
                    val obj = exArray.getJSONObject(i)
                    val ex = TaskExceptionEntity(
                        id = obj.getString("id"),
                        taskId = obj.getString("taskId"),
                        originalEpochDay = obj.getLong("originalEpochDay"),
                        type = obj.getString("type"),
                        overrideTitle = obj.optString("overrideTitle").takeIf { it.isNotEmpty() && it != "null" },
                        overrideStart = if (obj.isNull("overrideStart")) null else obj.getInt("overrideStart"),
                        overrideEnd = if (obj.isNull("overrideEnd")) null else obj.getInt("overrideEnd"),
                        overrideEpochDay = if (obj.isNull("overrideEpochDay")) null else obj.getLong("overrideEpochDay"),
                        overrideNote = obj.optString("overrideNote").takeIf { it.isNotEmpty() && it != "null" },
                        overrideAlarmStart = if (obj.isNull("overrideAlarmStart")) null else obj.getBoolean("overrideAlarmStart"),
                        overrideAlarmEnd = if (obj.isNull("overrideAlarmEnd")) null else obj.getBoolean("overrideAlarmEnd"),
                        overrideTone = obj.optString("overrideTone").takeIf { it.isNotEmpty() && it != "null" }
                    )
                    db.taskExceptionDao().insertException(ex)
                }
            }

            // Restore occurrences
            if (root.has("occurrenceStates")) {
                val stArray = root.getJSONArray("occurrenceStates")
                for (i in 0 until stArray.length()) {
                    val obj = stArray.getJSONObject(i)
                    val st = OccurrenceStateEntity(
                        taskId = obj.getString("taskId"),
                        epochDay = obj.getLong("epochDay"),
                        status = obj.optString("status", "pending"),
                        completedAt = if (obj.isNull("completedAt")) null else obj.getLong("completedAt")
                    )
                    db.occurrenceDao().setOccurrenceState(st)
                }
            }

            // Restore subtasks
            if (root.has("subtasks")) {
                val subArray = root.getJSONArray("subtasks")
                for (i in 0 until subArray.length()) {
                    val obj = subArray.getJSONObject(i)
                    val sub = SubtaskEntity(
                        id = obj.getString("id"),
                        taskId = obj.getString("taskId"),
                        title = obj.getString("title"),
                        sortOrder = obj.optInt("sortOrder", i)
                    )
                    db.subtaskDao().insertSubtask(sub)
                }
            }

            // Restore backlog
            if (root.has("backlog")) {
                val blArray = root.getJSONArray("backlog")
                for (i in 0 until blArray.length()) {
                    val obj = blArray.getJSONObject(i)
                    val item = BacklogEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        durationMinutes = if (obj.isNull("durationMinutes")) null else obj.getInt("durationMinutes"),
                        note = obj.optString("note").takeIf { it.isNotEmpty() && it != "null" },
                        categoryId = obj.optString("categoryId").takeIf { it.isNotEmpty() && it != "null" },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    db.backlogDao().insertBacklog(item)
                }
            }

            // Reschedule alarms after successful import
            AlarmScheduler.rescheduleRollingWindow(context)
            true
        } catch (_: Exception) {
            false
        }
    }
}
