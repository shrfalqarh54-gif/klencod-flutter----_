package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtilsAr
import com.example.data.local.CategoryEntity
import com.example.domain.ConflictCheckResult
import com.example.domain.FreeGap
import com.example.domain.TaskOccurrence
import com.example.domain.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun TaskOccurrenceCard(
    occurrence: TaskOccurrence,
    category: CategoryEntity?,
    is24Hour: Boolean = true,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onReschedule: () -> Unit
) {
    val statusColor = when (occurrence.status) {
        TaskStatus.RUNNING -> Color(0xFFF59E0B)
        TaskStatus.DONE -> Color(0xFF10B981)
        TaskStatus.MISSED -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("task_occurrence_${occurrence.taskId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (occurrence.status == TaskStatus.RUNNING)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category emoji avatar
                Surface(
                    shape = CircleShape,
                    color = category?.let { Color(it.colorValue).copy(alpha = 0.2f) }
                        ?: MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = category?.emoji ?: "⭐",
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = occurrence.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (occurrence.isRecurring) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔁", fontSize = 12.sp)
                        }

                        if (occurrence.hasConflict) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚠️", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${DateUtilsAr.formatTimeRange(occurrence.startMinute, occurrence.endMinute, is24Hour)}  •  ${DateUtilsAr.formatDurationArabic(occurrence.durationMinutes)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip / action button
                when (occurrence.status) {
                    TaskStatus.DONE -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "منجزة ✅",
                                color = Color(0xFF059669),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    TaskStatus.RUNNING -> {
                        Button(
                            onClick = onMarkDone,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تم ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TaskStatus.MISSED -> {
                        OutlinedButton(
                            onClick = onReschedule,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text("لم تُنجز ⚠️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        IconButton(
                            onClick = onMarkDone,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تعليم كمنجزة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Subtasks progress bar if any
            if (occurrence.totalSubtasks > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المهام الفرعية: ${occurrence.completedSubtasks}/${occurrence.totalSubtasks}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        val progressFraction = occurrence.completedSubtasks.toFloat() / occurrence.totalSubtasks
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(5.dp)
                                .background(Color(0xFF10B981))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Required Conflict Dialog per Section 6
 */
@Composable
fun ConflictAlertDialog(
    conflictResult: ConflictCheckResult,
    allowConflictOverride: Boolean,
    onDismiss: () -> Unit,
    onChangeTime: () -> Unit,
    onShortenDuration: (newEnd: Int) -> Unit,
    onSelectAlternativeGap: (FreeGap) -> Unit,
    onSaveAnyway: () -> Unit
) {
    val firstConflicting = conflictResult.conflictingOccurrences.firstOrNull()
    val conflictingText = if (firstConflicting != null) {
        "لديك مهمة أخرى «${firstConflicting.title}» من ${DateUtilsAr.formatTime(firstConflicting.startMinute)} إلى ${DateUtilsAr.formatTime(firstConflicting.endMinute)}"
    } else {
        "هذا الوقت يتعارض مع مهمة أخرى مجدولة."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("⚠️ هذا الوقت محجوز", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(conflictingText, fontSize = 14.sp)

                if (conflictResult.suggestedGaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "أوقات فراغ بديلة مقترحة اليوم:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (gap in conflictResult.suggestedGaps) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAlternativeGap(gap) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        DateUtilsAr.formatTimeRange(gap.startMinute, gap.endMinute),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "فراغ: ${DateUtilsAr.formatDurationArabic(gap.durationMinutes)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onChangeTime,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تغيير وقت المهمة")
                }

                if (conflictResult.proposedShortenedEnd != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onShortenDuration(conflictResult.proposedShortenedEnd) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تقصير مدة المهمة إلى ${DateUtilsAr.formatTime(conflictResult.proposedShortenedEnd)}")
                    }
                }

                if (allowConflictOverride) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onSaveAnyway,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ رغم التعارض ⚠️")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("إلغاء")
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Smart Reschedule Sheet Dialog per Section 12.7
 */
@Composable
fun SmartRescheduleDialog(
    taskTitle: String,
    suggestedGaps: List<FreeGap>,
    onDismiss: () -> Unit,
    onMoveToGapToday: (FreeGap) -> Unit,
    onMoveToTomorrow: () -> Unit,
    onMoveToBacklog: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إعادة الجدولة الذكية",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    "لم تنجز مهمة «$taskTitle». ماذا تريد أن تفعل؟",
                    fontSize = 14.sp
                )

                if (suggestedGaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "فترات فارغة متاحة اليوم:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    for (gap in suggestedGaps.take(3)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMoveToGapToday(gap) }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    DateUtilsAr.formatTimeRange(gap.startMinute, gap.endMinute),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "فراغ: ${DateUtilsAr.formatDurationArabic(gap.durationMinutes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onMoveToTomorrow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نقل إلى الغد")
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onMoveToBacklog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("وضعها في قائمة «لاحقاً»")
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onMarkDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تمت بالفعل ✅")
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حذف المهمة")
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("إلغاء")
                }
            }
        },
        dismissButton = null
    )
}
