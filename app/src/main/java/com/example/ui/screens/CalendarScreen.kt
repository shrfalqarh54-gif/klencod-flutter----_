package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtilsAr
import com.example.domain.TaskOccurrence
import com.example.ui.components.AppTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    currentSelectedEpochDay: Long,
    allTasksCountMap: Map<Long, Int> = emptyMap(),
    onBack: () -> Unit,
    onSelectDayAndOpenSchedule: (Long) -> Unit
) {
    var selectedEpochDay by remember { mutableStateOf(currentSelectedEpochDay) }
    val initialDate = DateUtilsAr.epochDayToLocalDate(selectedEpochDay)
    var currentYearMonth by remember { mutableStateOf(YearMonth.of(initialDate.year, initialDate.month)) }

    val todayEpoch = DateUtilsAr.todayEpochDay()

    val quickShortcuts = listOf(
        Pair("اليوم", todayEpoch),
        Pair("غداً", todayEpoch + 1),
        Pair("بعد غد", todayEpoch + 2),
        Pair("بعد أسبوع", todayEpoch + 7),
        Pair("بعد شهر", todayEpoch + 30)
    )

    // Week days header with Saturday first
    val weekDaysArabic = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")

    Scaffold(
        topBar = {
            AppTopBar(
                title = "التقويم وخطة الأيام",
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
            // Quick Shortcuts Row
            Text(
                text = "اختصارات سريعة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickShortcuts) { (label, epoch) ->
                    val isSelected = (epoch == selectedEpochDay)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedEpochDay = epoch
                            val d = DateUtilsAr.epochDayToLocalDate(epoch)
                            currentYearMonth = YearMonth.of(d.year, d.month)
                        },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Month Navigation Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month & Year Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "الشهر السابق")
                        }

                        Text(
                            text = "${DateUtilsAr.getMonthNameArabic(currentYearMonth.monthValue)} ${currentYearMonth.year}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "الشهر التالي")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of Week Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (wDay in weekDaysArabic) {
                            Text(
                                text = wDay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Grid Calculations (Saturday first)
                    val firstDayOfMonth = currentYearMonth.atDay(1)
                    val daysInMonth = currentYearMonth.lengthOfMonth()

                    // Convert DayOfWeek so Saturday = 0, Sunday = 1, ..., Friday = 6
                    val firstDayOffset = when (firstDayOfMonth.dayOfWeek) {
                        DayOfWeek.SATURDAY -> 0
                        DayOfWeek.SUNDAY -> 1
                        DayOfWeek.MONDAY -> 2
                        DayOfWeek.TUESDAY -> 3
                        DayOfWeek.WEDNESDAY -> 4
                        DayOfWeek.THURSDAY -> 5
                        DayOfWeek.FRIDAY -> 6
                    }

                    val totalCells = firstDayOffset + daysInMonth
                    val rowsCount = (totalCells + 6) / 7

                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOffset + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNumber)
                                    val cellEpoch = cellDate.toEpochDay()
                                    val isCellSelected = (cellEpoch == selectedEpochDay)
                                    val isToday = (cellEpoch == todayEpoch)
                                    val taskCount = allTasksCountMap[cellEpoch] ?: 0

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCellSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedEpochDay = cellEpoch },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNumber",
                                                fontSize = 14.sp,
                                                fontWeight = if (isCellSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isCellSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            // Task dot if day has scheduled tasks
                                            if (taskCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isCellSelected) Color.White else Color(0xFFF59E0B)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action for the selected day
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "اليوم المحدد:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateUtilsAr.formatRelativeDateArabic(selectedEpochDay),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectDayAndOpenSchedule(selectedEpochDay) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("calendar_open_day_schedule_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح جدول هذا اليوم والتخطيط له", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
