package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtilsAr
import com.example.data.local.CategoryEntity
import com.example.data.local.TaskEntity
import com.example.domain.ConflictCheckResult
import com.example.domain.ConflictEngine
import com.example.domain.FreeGap
import com.example.domain.RecurrenceEngine
import com.example.domain.RecurrenceType
import com.example.domain.TaskOccurrence
import com.example.service.ToneItem
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConflictAlertDialog
import java.time.DayOfWeek
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    initialEpochDay: Long,
    editingTask: TaskEntity? = null,
    editingOccurrence: TaskOccurrence? = null,
    categories: List<CategoryEntity>,
    availableTones: List<ToneItem>,
    existingOccurrencesOnDay: List<TaskOccurrence>,
    allowConflictOverride: Boolean = false,
    is24Hour: Boolean = true,
    onBack: () -> Unit,
    onSaveTask: (
        task: TaskEntity,
        subtasks: List<String>,
        updateFuture: Boolean
    ) -> Unit,
    onPlayTonePreview: (String) -> Unit,
    onStopTonePreview: () -> Unit
) {
    var epochDay by remember { mutableStateOf(initialEpochDay) }
    var title by remember { mutableStateOf(editingOccurrence?.title ?: editingTask?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(editingOccurrence?.categoryId ?: editingTask?.categoryId) }
    var note by remember { mutableStateOf(editingOccurrence?.note ?: editingTask?.note ?: "") }

    var startMinute by remember {
        mutableIntStateOf(editingOccurrence?.startMinute ?: editingTask?.startMinute ?: DateUtilsAr.currentMinuteOfDay().coerceAtMost(1380))
    }
    var endMinute by remember {
        mutableIntStateOf(editingOccurrence?.endMinute ?: editingTask?.endMinute ?: (startMinute + 60).coerceAtMost(1440))
    }

    var recurrenceType by remember {
        mutableStateOf(editingTask?.recurrenceType?.let { RecurrenceType.fromCode(it) } ?: RecurrenceType.ONCE)
    }
    var recurrenceDaysMask by remember { mutableIntStateOf(editingTask?.recurrenceDaysMask ?: 0) }

    var alarmStartEnabled by remember { mutableStateOf(editingOccurrence?.alarmStartEnabled ?: editingTask?.alarmStartEnabled ?: true) }
    var alarmEndEnabled by remember { mutableStateOf(editingOccurrence?.alarmEndEnabled ?: editingTask?.alarmEndEnabled ?: true) }
    var selectedToneId by remember { mutableStateOf(editingOccurrence?.toneType ?: editingTask?.toneType ?: "tone_default") }

    val subtaskTitles = remember { mutableStateListOf<String>() }
    var newSubtaskInput by remember { mutableStateOf("") }

    var showAdvancedOptions by remember { mutableStateOf(editingTask != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var conflictResult by remember { mutableStateOf<ConflictCheckResult?>(null) }
    var showRecurringEditScopeDialog by remember { mutableStateOf(false) }
    var showEndOfDayMoveDialog by remember { mutableStateOf(false) }

    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Duration calculation
    val durationMinutes = (endMinute - startMinute).coerceAtLeast(0)

    val todayEpoch = DateUtilsAr.todayEpochDay()
    val isToday = (epochDay == todayEpoch)
    val currentMinuteNow = DateUtilsAr.currentMinuteOfDay()
    val isPastTime = isToday && startMinute < currentMinuteNow

    fun performValidationAndSave(updateFuture: Boolean = false) {
        if (title.trim().isEmpty()) {
            errorMessage = "يرجى إدخال اسم المهمة."
            return
        }
        if (endMinute <= startMinute) {
            errorMessage = "وقت نهاية المهمة يجب أن يكون بعد وقت البداية."
            return
        }
        if (startMinute >= 1440 || endMinute > 1440) {
            showEndOfDayMoveDialog = true
            return
        }

        // Check conflicts
        val conflict = ConflictEngine.checkConflict(
            start = startMinute,
            end = endMinute,
            existingOccurrences = existingOccurrencesOnDay,
            excludeTaskId = editingTask?.id
        )

        if (conflict.hasConflict && conflictResult == null) {
            conflictResult = conflict
            return
        }

        // If editing recurring task and scope not asked yet
        if (editingTask != null && editingTask.recurrenceType != "once" && !showRecurringEditScopeDialog && !updateFuture) {
            showRecurringEditScopeDialog = true
            return
        }

        val taskToSave = TaskEntity(
            id = editingTask?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            categoryId = selectedCategoryId,
            note = note.trim().ifEmpty { null },
            startMinute = startMinute,
            endMinute = endMinute,
            durationMinutes = durationMinutes,
            startEpochDay = epochDay,
            recurrenceType = recurrenceType.code,
            recurrenceDaysMask = recurrenceDaysMask,
            alarmStartEnabled = alarmStartEnabled,
            alarmEndEnabled = alarmEndEnabled,
            toneType = selectedToneId,
            createdAt = editingTask?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        onSaveTask(taskToSave, subtaskTitles.toList(), updateFuture)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (editingTask != null) "تعديل المهمة" else "إضافة مهمة جديدة",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Error Message Banner
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Past Time Warning Banner
            if (isPastTime && editingTask == null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "⚠️ هذا الوقت مضى اليوم. ستُحفظ المهمة بدون تنبيه لما فات.",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Task Title (Required)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 80) {
                        title = it
                        errorMessage = null
                    }
                },
                label = { Text("اسم المهمة *") },
                placeholder = { Text("مثال: قراءة ورد القرآن، مراجعة الرياضيات") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text("${title.length}/80", fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories Selector Chips
            Text("التصنيف:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (cat in categories) {
                    val isSelected = (selectedCategoryId == cat.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryId = if (isSelected) null else cat.id
                        },
                        label = { Text("${cat.emoji} ${cat.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cat.colorValue).copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Selector Card (Start, End, and Duration)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الوقت والمدة (نظام 24 ساعة):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Time Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("البداية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Quick advance by 30 min if needed
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DateUtilsAr.formatTime(startMinute, is24Hour),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = { startMinute = (startMinute - 15).coerceAtLeast(0) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = {
                                                startMinute = (startMinute + 15).coerceAtMost(1425)
                                                if (endMinute <= startMinute) endMinute = (startMinute + 30).coerceAtMost(1440)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // End Time Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("النهاية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DateUtilsAr.formatTime(endMinute, is24Hour),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                endMinute = (endMinute - 15).coerceAtLeast(startMinute + 1)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = { endMinute = (endMinute + 15).coerceAtMost(1440) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-calculated duration label
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "المدة المحسوبة: ${DateUtilsAr.formatDurationArabic(durationMinutes)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable Advanced Options (Recurrence, Alarms, Notes, Subtasks)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedOptions = !showAdvancedOptions }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خيارات إضافية (التكرار، التنبيهات، المهام الفرعية)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = showAdvancedOptions) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Recurrence Selector
                    Text("التكرار:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (rType in RecurrenceType.entries) {
                            FilterChip(
                                selected = recurrenceType == rType,
                                onClick = { recurrenceType = rType },
                                label = { Text(rType.titleAr) }
                            )
                        }
                    }

                    // Days of week selector if DAYS_OF_WEEK selected
                    if (recurrenceType == RecurrenceType.DAYS_OF_WEEK) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("اختر أيام الأسبوع:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        val daysMap = listOf(
                            Pair("السبت", 1),
                            Pair("الأحد", 2),
                            Pair("الإثنين", 4),
                            Pair("الثلاثاء", 8),
                            Pair("الأربعاء", 16),
                            Pair("الخميس", 32),
                            Pair("الجمعة", 64)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for ((dayLabel, maskVal) in daysMap) {
                                val isChecked = (recurrenceDaysMask and maskVal) != 0
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        recurrenceDaysMask = if (isChecked) {
                                            recurrenceDaysMask and maskVal.inv()
                                        } else {
                                            recurrenceDaysMask or maskVal
                                        }
                                    },
                                    label = { Text(dayLabel) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm switches & Tone selector
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("إعدادات المنبه:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تنبيه عند وقت البداية 🔔", fontSize = 13.sp)
                                Switch(
                                    checked = alarmStartEnabled,
                                    onCheckedChange = { alarmStartEnabled = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تنبيه عند وقت النهاية ⏰", fontSize = 13.sp)
                                Switch(
                                    checked = alarmEndEnabled,
                                    onCheckedChange = { alarmEndEnabled = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("نغمة التنبيه:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (tone in availableTones) {
                                    val isToneSelected = (selectedToneId == tone.id)
                                    FilterChip(
                                        selected = isToneSelected,
                                        onClick = {
                                            selectedToneId = tone.id
                                            if (isPreviewPlaying) {
                                                onStopTonePreview()
                                                isPreviewPlaying = false
                                            } else {
                                                onPlayTonePreview(tone.id)
                                                isPreviewPlaying = true
                                            }
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(tone.titleAr)
                                                if (isToneSelected) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = "معاينة",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtasks checklist
                    Text("المهام الفرعية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskInput,
                            onValueChange = { newSubtaskInput = it },
                            placeholder = { Text("أضف مهمة فرعية...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSubtaskInput.trim().isNotEmpty()) {
                                    subtaskTitles.add(newSubtaskInput.trim())
                                    newSubtaskInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة")
                        }
                    }

                    if (subtaskTitles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            subtaskTitles.forEachIndexed { index, subTitle ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• $subTitle", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { subtaskTitles.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("ملاحظات إضافية (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Primary Save Button
            Button(
                onClick = { performValidationAndSave(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (editingTask != null) "حفظ التعديلات" else "جدولة المهمة الآن 💪",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Conflict Dialog
    conflictResult?.let { res ->
        ConflictAlertDialog(
            conflictResult = res,
            allowConflictOverride = allowConflictOverride,
            onDismiss = { conflictResult = null },
            onChangeTime = {
                conflictResult = null
                // User keeps editing the time fields
            },
            onShortenDuration = { newEnd ->
                endMinute = newEnd
                conflictResult = null
            },
            onSelectAlternativeGap = { gap ->
                startMinute = gap.startMinute
                endMinute = (gap.startMinute + durationMinutes).coerceAtMost(gap.endMinute)
                conflictResult = null
            },
            onSaveAnyway = {
                conflictResult = null
                performValidationAndSave(false)
            }
        )
    }

    // Recurring Task Edit Scope Dialog (Section 7)
    if (showRecurringEditScopeDialog && editingTask != null) {
        AlertDialog(
            onDismissRequest = { showRecurringEditScopeDialog = false },
            title = { Text("تعديل مهمة متكررة", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد تطبيق هذا التعديل على هذه المهمة فقط أم على جميع المهام المستقبلية أيضاً؟")
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showRecurringEditScopeDialog = false
                            // Single occurrence edit only
                            onSaveTask(
                                editingTask.copy(
                                    title = title.trim(),
                                    startMinute = startMinute,
                                    endMinute = endMinute,
                                    durationMinutes = durationMinutes,
                                    note = note.trim().ifEmpty { null }
                                ),
                                subtaskTitles.toList(),
                                false
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("هذه المهمة فقط")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            showRecurringEditScopeDialog = false
                            // Update this and all future
                            onSaveTask(
                                editingTask.copy(
                                    title = title.trim(),
                                    categoryId = selectedCategoryId,
                                    startMinute = startMinute,
                                    endMinute = endMinute,
                                    durationMinutes = durationMinutes,
                                    startEpochDay = epochDay,
                                    note = note.trim().ifEmpty { null },
                                    alarmStartEnabled = alarmStartEnabled,
                                    alarmEndEnabled = alarmEndEnabled,
                                    toneType = selectedToneId
                                ),
                                subtaskTitles.toList(),
                                true
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("هذه المهمة وجميع المستقبلية")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showRecurringEditScopeDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إلغاء")
                    }
                }
            },
            dismissButton = null
        )
    }

    // End of Day Dialog (Section 12.8)
    if (showEndOfDayMoveDialog) {
        AlertDialog(
            onDismissRequest = { showEndOfDayMoveDialog = false },
            title = { Text("انتهى وقت هذا اليوم", fontWeight = FontWeight.Bold) },
            text = {
                Text("الوقت المحدد يتجاوز منتصف الليل (23:59). هل تريد نقل هذه المهمة إلى الغد؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        epochDay += 1
                        startMinute = 0
                        endMinute = durationMinutes.coerceIn(15, 1440)
                        showEndOfDayMoveDialog = false
                    }
                ) {
                    Text("نقل إلى الغد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndOfDayMoveDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
