package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.example.data.preferences.UserPreferences
import com.example.service.ToneItem
import com.example.ui.components.AppTopBar
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    tones: List<ToneItem>,
    onBack: () -> Unit,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onNavigateToReliability: () -> Unit,
    onPickCustomTone: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (json: String, replace: Boolean) -> Unit,
    onWipeAllData: () -> Unit
) {
    var showWipeConfirm1 by remember { mutableStateOf(false) }
    var showWipeConfirm2 by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "الإعدادات ⚙️",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RELIABILITY ENTRY CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReliability() }
                    .testTag("settings_reliability_item")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("موثوقية التنبيهات واختبار الرنين", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("فحص الصلاحيات، تحسين البطارية، واختبار منبه حقيقي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // ALARM & SOUND SETTINGS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التنبيهات والصوت", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ring Duration
                    Text("مدة رنين المنبه:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 120).forEach { dur ->
                            FilterChip(
                                selected = preferences.ringDurationSeconds == dur,
                                onClick = { onUpdatePreferences { it.copy(ringDurationSeconds = dur) } },
                                label = { Text("$dur ثانية") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vibration Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الاهتزاز أثناء الرنين", fontSize = 14.sp)
                        }
                        Switch(
                            checked = preferences.vibrationEnabled,
                            onCheckedChange = { isChecked ->
                                onUpdatePreferences { it.copy(vibrationEnabled = isChecked) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Default Tone Selection
                    Text("النغمة الافتراضية:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tones.forEach { tone ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (preferences.defaultTone == tone.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdatePreferences { it.copy(defaultTone = tone.id) }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tone.titleAr,
                                        fontSize = 13.sp,
                                        fontWeight = if (preferences.defaultTone == tone.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (preferences.defaultTone == tone.id) {
                                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pick custom tone button
                    OutlinedButton(
                        onClick = onPickCustomTone,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار نغمة من ذاكرة الهاتف 📱")
                    }
                }
            }

            // CALENDAR & SCHEDULE PREFERENCES
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التقويم والجدولة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Week start day
                    Text("بداية الأسبوع:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY).forEach { day ->
                            FilterChip(
                                selected = preferences.weekStartDay == day,
                                onClick = { onUpdatePreferences { it.copy(weekStartDay = day) } },
                                label = { Text(DateUtilsAr.getDayOfWeekArabic(day)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 12/24 hour format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نظام الوقت 24 ساعة (مثال: 16:00)", fontSize = 14.sp)
                        Switch(
                            checked = preferences.is24HourFormat,
                            onCheckedChange = { isChecked ->
                                onUpdatePreferences { it.copy(is24HourFormat = isChecked) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Allow Conflict Override
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("السماح بحفظ المهام المتعارضة يدويًا", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إتاحة زر «حفظ رغم التعارض» مع وضع شارة ⚠️ في الجدول", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.allowConflictOverride,
                            onCheckedChange = { isChecked ->
                                onUpdatePreferences { it.copy(allowConflictOverride = isChecked) }
                            }
                        )
                    }
                }
            }

            // BACKUP & RESTORE
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("النسخ الاحتياطي (JSON محلي 100%)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "احفظ نسخة احتياطية كاملة من كل مهامك وتكراراتك وإعداداتك، أو استرجعها في أي وقت دون إنترنت.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تصدير JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("استيراد JSON")
                        }
                    }
                }
            }

            // DANGER ZONE & ABOUT
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = { showWipeConfirm1 = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح جميع البيانات والمنبهات (تأكيد مزدوج)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حول تطبيق «قوة الآن»")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Wipe Double Confirmation Dialogs
    if (showWipeConfirm1) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm1 = false },
            title = { Text("تحذير: مسح كل البيانات", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع المهام والتكرارات والمنبهات وقائمة لاحقاً؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm1 = false
                        showWipeConfirm2 = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، تابع للمرحلة الأخيرة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm1 = false }) { Text("إلغاء") }
            }
        )
    }

    if (showWipeConfirm2) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm2 = false },
            title = { Text("تأكيد أخير ونهائي", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("سيتم إلغاء كل المنبهات ومسح قاعدة البيانات تماماً. اضغط لتأكيد الحذف النهائي.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm2 = false
                        onWipeAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("مسح نهائي الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm2 = false }) { Text("تراجع") }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد نسخة احتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الصق نص JSON للنسخة الاحتياطية هنا:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(
                        onClick = {
                            if (importJsonText.trim().isNotEmpty()) {
                                onImportBackup(importJsonText.trim(), false)
                                showImportDialog = false
                            }
                        }
                    ) {
                        Text("دمج البيانات")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            if (importJsonText.trim().isNotEmpty()) {
                                onImportBackup(importJsonText.trim(), true)
                                showImportDialog = false
                            }
                        }
                    ) {
                        Text("استبدال كامل")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("قوة الآن 💪", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("الإصدار: 1.0.0", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "تطبيق لإدارة الوقت والمهام اليومية وفق مبدأ قوة اللحظة الحالية وتقسيم اليوم 24 ساعة (00:00 - 23:59).\n\n" +
                                "• يعمل بدون إنترنت 100% وبدون إعلانات\n" +
                                "• تنبيهات ومنبهات موثوقة تعمل والتطبيق مغلق تماماً\n" +
                                "• محرك ذكي لمنع التعارضات وحساب الفراغات\n" +
                                "• دعم كامل للغة العربية والاتجاه من اليمين لليسار (RTL)",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}
