package com.example.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar

@Composable
fun ReliabilityScreen(
    onBack: () -> Unit,
    onTestAlarm: () -> Unit
) {
    val context = LocalContext.current
    var testAlarmScheduled by remember { mutableStateOf(false) }

    // Check notification status
    val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    // Check exact alarm status
    val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.canScheduleExactAlarms()
    } else {
        true
    }

    // Check battery optimization exemption status
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "موثوقية التنبيهات 🛡️",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "لضمان عمل المنبهات ورنينها حتى لو كان التطبيق مغلقاً تماماً أو في وضع السكون:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Notification Permission
            ReliabilityItemCard(
                title = "إذن الإشعارات",
                isOk = notificationGranted,
                description = if (notificationGranted) "مفعل: يمكن للتطبيق عرض تنبيهات المهام" else "مطلوب للسماح برنين المنبهات وعرض الشاشة",
                onFix = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            // 2. Exact Alarms Permission
            ReliabilityItemCard(
                title = "إذن المنبهات الدقيقة (Exact Alarms)",
                isOk = exactAlarmGranted,
                description = if (exactAlarmGranted) "مفعل: الجدولة تتم في الثانية المحددة تماماً" else "مطلوب لإطلاق التنبيه في موعده بدقة متناهية",
                onFix = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                }
            )

            // 3. Battery Optimization
            ReliabilityItemCard(
                title = "استثناء تحسين البطارية",
                isOk = isIgnoringBatteryOptimizations,
                description = if (isIgnoringBatteryOptimizations) "مستثنى: النظام لن يقوم بإيقاف المنبهات لتوفير الطاقة" else "يُفضّل الاستثناء حتى لا يوقف النظام التنبيه أثناء نوم الهاتف",
                onFix = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            // 4. Test Alarm Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختبار المنبه الحقيقي 🔔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اضغط الزر أدناه لتفعيل منبه حقيقي بعد 10 ثوانٍ للتأكد من رنين الصوت والاهتزاز وشاشة الرنين.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onTestAlarm()
                            testAlarmScheduled = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_alarm_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختبار التنبيه بعد 10 ثوانٍ", fontWeight = FontWeight.Bold)
                    }

                    if (testAlarmScheduled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تمت الجدولة! يمكنك الآن إغلاق التطبيق أو قفل الشاشة لرؤية النتيجة بعد 10 ثوانٍ.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            // 5. Vendor Specific Tips (Xiaomi, Samsung, Huawei, Oppo)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نصائح لأجهزة محددة (أندرويد مخصص):", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• شاومي (Xiaomi/POCO): فعّل «التشغيل التلقائي» (Autostart) من إعدادات إدارة التطبيقات.\n" +
                                "• سامسونج (Samsung): أضف التطبيق إلى «التطبيقات التي لا تنام أبداً» (Never sleeping apps).\n" +
                                "• هواوي / أوبو (Huawei/Oppo): تأكد من إعطاء إذن «التشغيل في الخلفية وعرض النوافذ المنبثقة».",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ReliabilityItemCard(
    title: String,
    isOk: Boolean,
    description: String,
    onFix: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOk) Color(0xFFA7F3D0) else Color(0xFFFECACA)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOk) Color(0xFF059669) else Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOk) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                }

                if (!isOk) {
                    Button(
                        onClick = onFix,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("إصلاح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (isOk) Color(0xFF047857) else Color(0xFFB91C1C)
            )
        }
    }
}
