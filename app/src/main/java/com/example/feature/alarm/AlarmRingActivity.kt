package com.example.feature.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.OccurrenceStateEntity
import com.example.service.AlarmRingtoneService
import com.example.service.AlarmScheduler
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        turnScreenOnAndKeyguard()

        val taskId = intent.getStringExtra("taskId") ?: ""
        val epochDay = intent.getLongExtra("epochDay", 0L)
        val title = intent.getStringExtra("title") ?: "المهمة المجدولة"
        val kind = intent.getStringExtra("kind") ?: "start"
        val timeRange = intent.getStringExtra("timeRange") ?: ""

        setContent {
            MyApplicationTheme {
                AlarmRingContent(
                    title = title,
                    kind = kind,
                    timeRange = timeRange,
                    onStop = {
                        AlarmRingtoneService.stopService(this)
                        finish()
                    },
                    onMarkDone = {
                        markStatus(taskId, epochDay, "done")
                        AlarmRingtoneService.stopService(this)
                        finish()
                    },
                    onMarkMissed = {
                        markStatus(taskId, epochDay, "missed")
                        AlarmRingtoneService.stopService(this)
                        // Open main activity with reschedule intent
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("open_reschedule_task_id", taskId)
                            putExtra("open_reschedule_epoch_day", epochDay)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun markStatus(taskId: String, epochDay: Long, status: String) {
        if (taskId.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(applicationContext)
                db.occurrenceDao().setOccurrenceState(
                    OccurrenceStateEntity(
                        taskId = taskId,
                        epochDay = epochDay,
                        status = status,
                        completedAt = if (status == "done") System.currentTimeMillis() else null
                    )
                )
                AlarmScheduler.rescheduleRollingWindow(applicationContext)
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}

@Composable
fun AlarmRingContent(
    title: String,
    kind: String,
    timeRange: String,
    onStop: () -> Unit,
    onMarkDone: () -> Unit,
    onMarkMissed: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B),
            Color(0xFF312E81)
        )
    )

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Icon
                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale),
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (kind == "start") Icons.Default.NotificationsActive else Icons.Default.Alarm,
                            contentDescription = "أيقونة المنبه",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Kind Badge
                Surface(
                    color = if (kind == "start") Color(0xFF1E293B) else Color(0xFF334155),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Text(
                        text = if (kind == "start") "🔔 الآن وقت البدء" else "⏰ انتهت مدة المهمة",
                        color = Color(0xFFFCD34D),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Huge Task Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (timeRange.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = timeRange,
                        color = Color(0xFFCBD5E1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action Buttons
                if (kind == "start") {
                    Button(
                        onClick = onStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("alarm_ring_stop_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF1E1B4B)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("إيقاف المنبه والبدء الآن", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // End Alarm: "تم ✅" and "لم أنجزها"
                    Button(
                        onClick = onMarkDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("alarm_ring_done_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("تم الإنجاز بنجاح ✅", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onMarkMissed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("alarm_ring_missed_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF87171))
                    ) {
                        Text("لم أنجزها (إعادة جدولة)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
