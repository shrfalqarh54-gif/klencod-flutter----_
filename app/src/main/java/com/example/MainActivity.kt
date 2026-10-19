package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.core.DateUtilsAr
import com.example.data.local.BacklogEntity
import com.example.data.local.TaskEntity
import com.example.domain.ConflictEngine
import com.example.domain.FreeGap
import com.example.domain.TaskOccurrence
import com.example.ui.components.SmartRescheduleDialog
import com.example.ui.screens.AddEditTaskScreen
import com.example.ui.screens.BacklogScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NowScreen
import com.example.ui.screens.ReliabilityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScheduleScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data object Now : Screen()
    data class TodaySchedule(val epochDay: Long) : Screen()
    data object Calendar : Screen()
    data class AddEditTask(
        val epochDay: Long,
        val editingTask: TaskEntity? = null,
        val editingOccurrence: TaskOccurrence? = null,
        val prefilledStartMinute: Int? = null,
        val prefilledEndMinute: Int? = null,
        val prefilledTitle: String? = null
    ) : Screen()
    data object Backlog : Screen()
    data object Settings : Screen()
    data object Reliability : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel, intent = intent)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel, intent: Intent) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val preferences by viewModel.preferences.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val backlogItems by viewModel.backlog.collectAsState()
    val selectedEpochDay by viewModel.selectedEpochDay.collectAsState()
    val occurrences by viewModel.occurrences.collectAsState()
    val nowAnalysis by viewModel.nowMomentAnalysis.collectAsState()
    val availableTones by viewModel.availableTones.collectAsState()

    var activeRescheduleOccurrence by remember { mutableStateOf<TaskOccurrence?>(null) }

    // SAF Audio Picker for Custom Tones
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "custom_tone.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.saveCustomTone(uri, fileName)
            Toast.makeText(context, "تم حفظ النغمة محلياً: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Check notifications / exact alarms for warning banner
    val showReliabilityBanner = remember {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            !am.canScheduleExactAlarms()
        } else {
            false
        }
    }

    // Check if opened from intent with reschedule request
    LaunchedEffect(intent) {
        val rescheduleTaskId = intent.getStringExtra("open_reschedule_task_id")
        val rescheduleEpochDay = intent.getLongExtra("open_reschedule_epoch_day", 0L)
        if (!rescheduleTaskId.isNullOrEmpty() && rescheduleEpochDay > 0) {
            val targetOcc = occurrences.firstOrNull { it.taskId == rescheduleTaskId && it.epochDay == rescheduleEpochDay }
            if (targetOcc != null) {
                activeRescheduleOccurrence = targetOcc
            }
        }
    }

    // Handle Back Press navigation
    BackHandler(enabled = currentScreen !is Screen.Home) {
        currentScreen = Screen.Home
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    analysis = nowAnalysis,
                    is24Hour = preferences.is24HourFormat,
                    showReliabilityBanner = showReliabilityBanner,
                    onNavigateToNow = { currentScreen = Screen.Now },
                    onNavigateToTodaySchedule = {
                        viewModel.selectDay(DateUtilsAr.todayEpochDay())
                        currentScreen = Screen.TodaySchedule(DateUtilsAr.todayEpochDay())
                    },
                    onNavigateToTomorrowPlan = {
                        val tomorrow = DateUtilsAr.todayEpochDay() + 1
                        viewModel.selectDay(tomorrow)
                        currentScreen = Screen.TodaySchedule(tomorrow)
                    },
                    onNavigateToAddTask = { startMin ->
                        val start = startMin ?: DateUtilsAr.currentMinuteOfDay()
                        val end = (start + 60).coerceAtMost(1440)
                        currentScreen = Screen.AddEditTask(
                            epochDay = DateUtilsAr.todayEpochDay(),
                            prefilledStartMinute = start,
                            prefilledEndMinute = end
                        )
                    },
                    onNavigateToCalendar = { currentScreen = Screen.Calendar },
                    onNavigateToBacklog = { currentScreen = Screen.Backlog },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToReliability = { currentScreen = Screen.Reliability },
                    onMarkTaskDone = { occ -> viewModel.markDone(occ) },
                    onRescheduleMissed = {
                        val missedOcc = nowAnalysis.todayTasks.firstOrNull { it.status == com.example.domain.TaskStatus.MISSED }
                        if (missedOcc != null) {
                            activeRescheduleOccurrence = missedOcc
                        } else {
                            currentScreen = Screen.TodaySchedule(DateUtilsAr.todayEpochDay())
                        }
                    }
                )
            }

            is Screen.Now -> {
                NowScreen(
                    analysis = nowAnalysis,
                    is24Hour = preferences.is24HourFormat,
                    onBack = { currentScreen = Screen.Home },
                    onMarkTaskDone = { occ -> viewModel.markDone(occ) },
                    onAddTaskAt = { start, end ->
                        currentScreen = Screen.AddEditTask(
                            epochDay = DateUtilsAr.todayEpochDay(),
                            prefilledStartMinute = start,
                            prefilledEndMinute = end
                        )
                    },
                    onNavigateToTodaySchedule = {
                        viewModel.selectDay(DateUtilsAr.todayEpochDay())
                        currentScreen = Screen.TodaySchedule(DateUtilsAr.todayEpochDay())
                    },
                    onNavigateToTomorrowPlan = {
                        val tomorrow = DateUtilsAr.todayEpochDay() + 1
                        viewModel.selectDay(tomorrow)
                        currentScreen = Screen.TodaySchedule(tomorrow)
                    }
                )
            }

            is Screen.TodaySchedule -> {
                TodayScheduleScreen(
                    epochDay = screen.epochDay,
                    occurrences = occurrences,
                    categories = categories,
                    is24Hour = preferences.is24HourFormat,
                    onBack = { currentScreen = Screen.Home },
                    onDayChanged = { newEpoch ->
                        viewModel.selectDay(newEpoch)
                        currentScreen = Screen.TodaySchedule(newEpoch)
                    },
                    onNavigateToCalendar = { currentScreen = Screen.Calendar },
                    onAddTaskAt = { day, start, end ->
                        currentScreen = Screen.AddEditTask(
                            epochDay = day,
                            prefilledStartMinute = start,
                            prefilledEndMinute = end
                        )
                    },
                    onEditTask = { occ ->
                        coroutineScope.launch {
                            val entity = viewModel.repository.getTaskById(occ.taskId)
                            currentScreen = Screen.AddEditTask(
                                epochDay = occ.epochDay,
                                editingTask = entity,
                                editingOccurrence = occ
                            )
                        }
                    },
                    onDeleteOccurrence = { occ, future ->
                        viewModel.deleteOccurrence(occ, future)
                    },
                    onMarkDone = { occ -> viewModel.markDone(occ) },
                    onReschedule = { occ -> activeRescheduleOccurrence = occ }
                )
            }

            is Screen.Calendar -> {
                CalendarScreen(
                    currentSelectedEpochDay = selectedEpochDay,
                    onBack = { currentScreen = Screen.Home },
                    onSelectDayAndOpenSchedule = { day ->
                        viewModel.selectDay(day)
                        currentScreen = Screen.TodaySchedule(day)
                    }
                )
            }

            is Screen.AddEditTask -> {
                AddEditTaskScreen(
                    initialEpochDay = screen.epochDay,
                    editingTask = screen.editingTask,
                    editingOccurrence = screen.editingOccurrence,
                    categories = categories,
                    availableTones = availableTones,
                    existingOccurrencesOnDay = occurrences,
                    allowConflictOverride = preferences.allowConflictOverride,
                    is24Hour = preferences.is24HourFormat,
                    onBack = {
                        viewModel.stopTonePreview()
                        currentScreen = Screen.TodaySchedule(screen.epochDay)
                    },
                    onSaveTask = { task, subs, updateFuture ->
                        viewModel.stopTonePreview()
                        viewModel.saveTask(task, subs, updateFuture)
                        currentScreen = Screen.TodaySchedule(screen.epochDay)
                    },
                    onPlayTonePreview = { toneId -> viewModel.playTonePreview(toneId) },
                    onStopTonePreview = { viewModel.stopTonePreview() }
                )
            }

            is Screen.Backlog -> {
                BacklogScreen(
                    backlogItems = backlogItems,
                    onBack = { currentScreen = Screen.Home },
                    onAddBacklogItem = { title, dur, n -> viewModel.addBacklogItem(title, dur, n) },
                    onDeleteBacklogItem = { id -> viewModel.deleteBacklogItem(id) },
                    onScheduleItemNow = { item ->
                        val now = DateUtilsAr.currentMinuteOfDay()
                        val dur = item.durationMinutes ?: 60
                        currentScreen = Screen.AddEditTask(
                            epochDay = DateUtilsAr.todayEpochDay(),
                            prefilledTitle = item.title,
                            prefilledStartMinute = now,
                            prefilledEndMinute = (now + dur).coerceAtMost(1440)
                        )
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    preferences = preferences,
                    tones = availableTones,
                    onBack = { currentScreen = Screen.Home },
                    onUpdatePreferences = { update -> viewModel.updatePreferences(update) },
                    onNavigateToReliability = { currentScreen = Screen.Reliability },
                    onPickCustomTone = { audioPickerLauncher.launch("audio/*") },
                    onExportBackup = {
                        coroutineScope.launch {
                            val json = viewModel.exportBackupJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PowerOfNowBackup", json))
                            Toast.makeText(context, "تم نسخ ملف JSON للنسخة الاحتياطية إلى الحافظة بنجاح ✅", Toast.LENGTH_LONG).show()
                        }
                    },
                    onImportBackup = { json, replace ->
                        coroutineScope.launch {
                            val success = viewModel.importBackupJson(json, replace)
                            if (success) {
                                Toast.makeText(context, "تم استيراد النسخة الاحتياطية بنجاح ✅", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "فشل الاستيراد: صيغة JSON غير صالحة ⚠️", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onWipeAllData = {
                        viewModel.wipeAllData()
                        Toast.makeText(context, "تم مسح جميع البيانات والمنبهات بنجاح.", Toast.LENGTH_SHORT).show()
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Reliability -> {
                ReliabilityScreen(
                    onBack = { currentScreen = Screen.Settings },
                    onTestAlarm = { viewModel.testAlarm() }
                )
            }
        }

        // Active Smart Reschedule Dialog
        activeRescheduleOccurrence?.let { occ ->
            val gapsToday = remember(occurrences) {
                ConflictEngine.suggestAlternativeGaps(occ.durationMinutes, occurrences, occ.taskId)
            }

            SmartRescheduleDialog(
                taskTitle = occ.title,
                suggestedGaps = gapsToday,
                onDismiss = { activeRescheduleOccurrence = null },
                onMoveToGapToday = { gap ->
                    viewModel.rescheduleOccurrence(occ, gap.startMinute, gap.startMinute + occ.durationMinutes, occ.epochDay)
                    activeRescheduleOccurrence = null
                },
                onMoveToTomorrow = {
                    val tomorrow = occ.epochDay + 1
                    viewModel.rescheduleOccurrence(occ, occ.startMinute, occ.endMinute, tomorrow)
                    activeRescheduleOccurrence = null
                    Toast.makeText(context, "تم نقل المهمة إلى الغد", Toast.LENGTH_SHORT).show()
                },
                onMoveToBacklog = {
                    viewModel.moveOccurrenceToBacklog(occ)
                    activeRescheduleOccurrence = null
                    Toast.makeText(context, "تم وضع المهمة في قائمة «لاحقاً»", Toast.LENGTH_SHORT).show()
                },
                onMarkDone = {
                    viewModel.markDone(occ)
                    activeRescheduleOccurrence = null
                },
                onDelete = {
                    viewModel.deleteOccurrence(occ, false)
                    activeRescheduleOccurrence = null
                }
            )
        }
    }
}
