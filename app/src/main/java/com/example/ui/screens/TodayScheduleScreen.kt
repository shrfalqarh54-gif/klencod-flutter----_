package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtilsAr
import com.example.data.local.CategoryEntity
import com.example.domain.ConflictEngine
import com.example.domain.FreeGap
import com.example.domain.TaskOccurrence
import com.example.domain.TaskStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.TaskOccurrenceCard
sealed class TimelineBlock(val start: Int) {
    class TaskBlock(val occurrence: TaskOccurrence) : TimelineBlock(occurrence.startMinute)
    class GapBlock(val gap: FreeGap) : TimelineBlock(gap.startMinute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    epochDay: Long,
    occurrences: List<TaskOccurrence>,
    categories: List<CategoryEntity>,
    is24Hour: Boolean = true,
    onBack: () -> Unit,
    onDayChanged: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onAddTaskAt: (epochDay: Long, startMinute: Int, endMinute: Int) -> Unit,
    onEditTask: (TaskOccurrence) -> Unit,
    onDeleteOccurrence: (TaskOccurrence, deleteFuture: Boolean) -> Unit,
    onMarkDone: (TaskOccurrence) -> Unit,
    onReschedule: (TaskOccurrence) -> Unit
) {
    val todayEpoch = DateUtilsAr.todayEpochDay()
    val isToday = (epochDay == todayEpoch)
    val currentMinute = DateUtilsAr.currentMinuteOfDay()

    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    var selectedOccurrenceForOptions by remember { mutableStateOf<TaskOccurrence?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Calculate free gaps for 24 hours
    val freeGaps = remember(occurrences) {
        ConflictEngine.calculateFreeGaps(occurrences).filter { it.durationMinutes >= 10 }
    }

    // Merge tasks and free gaps into timeline blocks sorted by startMinute

    val timelineBlocks = remember(occurrences, freeGaps) {
        val list = mutableListOf<TimelineBlock>()
        occurrences.forEach { list.add(TimelineBlock.TaskBlock(it)) }
        freeGaps.forEach { list.add(TimelineBlock.GapBlock(it)) }
        list.sortedBy { it.start }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isToday) "جدول اليوم (24 ساعة)" else "جدول ${DateUtilsAr.epochDayToLocalDate(epochDay).dayOfMonth} ${DateUtilsAr.getMonthNameArabic(DateUtilsAr.epochDayToLocalDate(epochDay).monthValue)}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToCalendar, modifier = Modifier.testTag("schedule_calendar_button")) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "التقويم")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Day Switcher Bar (Previous / Date Title / Next)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDayChanged(epochDay - 1) },
                        modifier = Modifier.testTag("schedule_prev_day_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "اليوم السابق")
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateToCalendar() }
                    ) {
                        Text(
                            text = DateUtilsAr.formatRelativeDateArabic(epochDay),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isToday) {
                            Text(
                                text = "اضغط للعودة إلى اليوم",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDayChanged(epochDay + 1) },
                        modifier = Modifier.testTag("schedule_next_day_button")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "اليوم التالي")
                    }
                }
            }

            // Timeline List
            if (timelineBlocks.isEmpty() || (timelineBlocks.size == 1 && timelineBlocks.first() is TimelineBlock.GapBlock && (timelineBlocks.first() as TimelineBlock.GapBlock).gap.durationMinutes == 1440)) {
                // Completely empty day
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("لا توجد مهام في هذا اليوم", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("اليوم مقسّم من 00:00 حتى 23:59", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onAddTaskAt(epochDay, if (isToday) currentMinute else 540, if (isToday) (currentMinute + 60).coerceAtMost(1440) else 600) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ إضافة مهمة لهذا اليوم")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(timelineBlocks) { block ->
                        when (block) {
                            is TimelineBlock.TaskBlock -> {
                                val occ = block.occurrence
                                val category = categoriesMap[occ.categoryId]

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Vertical time line node
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(52.dp)
                                    ) {
                                        Text(
                                            text = DateUtilsAr.formatTime(occ.startMinute, is24Hour),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(50.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                        Text(
                                            text = DateUtilsAr.formatTime(occ.endMinute, is24Hour),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        TaskOccurrenceCard(
                                            occurrence = occ,
                                            category = category,
                                            is24Hour = is24Hour,
                                            onClick = { selectedOccurrenceForOptions = occ },
                                            onMarkDone = { onMarkDone(occ) },
                                            onReschedule = { onReschedule(occ) }
                                        )
                                    }
                                }
                            }

                            is TimelineBlock.GapBlock -> {
                                val gap = block.gap
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(52.dp)
                                    ) {
                                        Text(
                                            text = DateUtilsAr.formatTime(gap.startMinute, is24Hour),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Subtle Free Gap Card with quick add button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onAddTaskAt(epochDay, gap.startMinute, gap.endMinute) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "فراغ: ${DateUtilsAr.formatDurationArabic(gap.durationMinutes)} (${DateUtilsAr.formatTimeRange(gap.startMinute, gap.endMinute, is24Hour)})",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text("+ إضافة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Task Options Bottom Sheet / Dialog
    selectedOccurrenceForOptions?.let { occ ->
        AlertDialog(
            onDismissRequest = { selectedOccurrenceForOptions = null },
            title = {
                Text(occ.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    Text(
                        text = "${DateUtilsAr.formatTimeRange(occ.startMinute, occ.endMinute, is24Hour)}  •  ${DateUtilsAr.formatDurationArabic(occ.durationMinutes)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!occ.note.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ملاحظة: ${occ.note}", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            selectedOccurrenceForOptions = null
                            onEditTask(occ)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل المهمة")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            selectedOccurrenceForOptions = null
                            onReschedule(occ)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إعادة الجدولة الذكية")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            selectedOccurrenceForOptions = null
                            onMarkDone(occ)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تعليم كمنجزة ✅")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف المهمة")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { selectedOccurrenceForOptions = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إغلاق")
                    }
                }
            },
            dismissButton = null
        )
    }

    // Delete Confirmation (Single vs Future per Section 7)
    if (showDeleteConfirmDialog && selectedOccurrenceForOptions != null) {
        val occ = selectedOccurrenceForOptions!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف المهمة", fontWeight = FontWeight.Bold) },
            text = {
                if (occ.isRecurring) {
                    Text("هذه مهمة متكررة. هل تريد حذف هذه النسخة فقط أم جميع النسخ المستقبلية؟")
                } else {
                    Text("هل أنت متأكد من رغبتك في حذف مهمة «${occ.title}»؟")
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (occ.isRecurring) {
                        Button(
                            onClick = {
                                onDeleteOccurrence(occ, false)
                                showDeleteConfirmDialog = false
                                selectedOccurrenceForOptions = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("هذه المهمة فقط")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                onDeleteOccurrence(occ, true)
                                showDeleteConfirmDialog = false
                                selectedOccurrenceForOptions = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("هذه المهمة وجميع المستقبلية")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDeleteOccurrence(occ, false)
                                showDeleteConfirmDialog = false
                                selectedOccurrenceForOptions = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حذف")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إلغاء")
                    }
                }
            },
            dismissButton = null
        )
    }
}
