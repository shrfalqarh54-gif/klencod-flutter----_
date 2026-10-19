package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtilsAr
import com.example.domain.NowMomentAnalysis
import com.example.domain.NowStateKind
import com.example.domain.TaskOccurrence
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    analysis: NowMomentAnalysis,
    is24Hour: Boolean = true,
    showReliabilityBanner: Boolean = false,
    onNavigateToNow: () -> Unit,
    onNavigateToTodaySchedule: () -> Unit,
    onNavigateToTomorrowPlan: () -> Unit,
    onNavigateToAddTask: (initialStartMinute: Int?) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBacklog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReliability: () -> Unit,
    onMarkTaskDone: (TaskOccurrence) -> Unit,
    onRescheduleMissed: () -> Unit
) {
    // 30-second live clock ticker to refresh current minute while screen is visible
    var currentMinute by remember { mutableIntStateOf(DateUtilsAr.currentMinuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentMinute = DateUtilsAr.currentMinuteOfDay()
        }
    }

    val todayEpoch = DateUtilsAr.todayEpochDay()
    val fullDateString = DateUtilsAr.formatFullDateArabic(todayEpoch)
    val clockTimeString = DateUtilsAr.formatTime(currentMinute, is24Hour)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "قوة الآن",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("💪", fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToBacklog, modifier = Modifier.testTag("home_backlog_button")) {
                        Icon(Icons.Default.ListAlt, contentDescription = "قائمة لاحقاً")
                    }
                    IconButton(onClick = onNavigateToCalendar, modifier = Modifier.testTag("home_calendar_button")) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "التقويم")
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("home_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Live Date & Time Header Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = fullDateString,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مدير اليوم الذكي (00:00 - 23:59)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Huge Live Clock
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = clockTimeString,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reliability Banner (if permissions missing)
            if (showReliabilityBanner) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReliability() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "التنبيهات تحتاج إذنًا للعمل بدقة والتطبيق مغلق",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                        Text("إصلاح", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Missed Tasks Banner
            if (analysis.missedTasksCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRescheduleMissed() }
                        .testTag("home_missed_tasks_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لديك ${analysis.missedTasksCount} مهام لم تنجزها في موعدها",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        Text("مراجعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // HERO CARD: Current Task / Current Moment
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (analysis.kind) {
                        NowStateKind.ACTIVE_TASK -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNow() }
                    .testTag("home_hero_now_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (analysis.kind == NowStateKind.ACTIVE_TASK)
                                Color(0xFFF59E0B)
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "الآن",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (analysis.kind == NowStateKind.ACTIVE_TASK) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (analysis.kind == NowStateKind.ACTIVE_TASK && analysis.remainingMinutesInCurrent != null) {
                            Text(
                                text = "متبقي: ${DateUtilsAr.formatDurationArabic(analysis.remainingMinutesInCurrent)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCD34D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (analysis.kind) {
                        NowStateKind.ACTIVE_TASK -> {
                            val cur = analysis.currentTask!!
                            Text(
                                text = cur.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtilsAr.formatTimeRange(cur.startMinute, cur.endMinute, is24Hour),
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { onMarkTaskDone(cur) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("home_hero_done_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تم الإنجاز ✅", fontWeight = FontWeight.Bold)
                            }
                        }

                        NowStateKind.FREE_TIME_WITH_NEXT -> {
                            Text(
                                text = "لا توجد مهمة جارية الآن",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val freeMin = analysis.freeMinutesUntilNext ?: 0
                            Text(
                                text = "لديك وقت فارغ لمدة ${DateUtilsAr.formatDurationArabic(freeMin)} حتى المهمة القادمة",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigateToAddTask(currentMinute) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استغل الوقت وأضف مهمة")
                            }
                        }

                        NowStateKind.NO_MORE_TASKS_TODAY -> {
                            Text(
                                text = "لا توجد مهام متبقية اليوم",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أنجزت ${analysis.completedTasksCount} من أصل ${analysis.totalTasksCount} مهمة اليوم",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        NowStateKind.EMPTY_DAY -> {
                            Text(
                                text = "يومك فارغ من المهام",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "قسّم يومك بحكمة من 00:00 حتى 23:59",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // NEXT TASK CARD
            if (analysis.nextTask != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTodaySchedule() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⏳", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("التالي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                analysis.nextTask.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                DateUtilsAr.formatTimeRange(analysis.nextTask.startMinute, analysis.nextTask.endMinute, is24Hour),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // THE 4 MAIN ACTION BUTTONS per specification 12.1
            Text(
                text = "الإجراءات الرئيسية",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. «+ إضافة مهمة»
                Button(
                    onClick = { onNavigateToAddTask(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_add_task_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ إضافة مهمة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. «ماذا أفعل الآن؟»
                Button(
                    onClick = onNavigateToNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_now_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💪", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ماذا أفعل الآن؟", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. «جدول اليوم»
                OutlinedButton(
                    onClick = onNavigateToTodaySchedule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_today_schedule_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جدول اليوم (24 ساعة)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 4. «خطة الغد»
                OutlinedButton(
                    onClick = onNavigateToTomorrowPlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_tomorrow_plan_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("خطة الغد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
