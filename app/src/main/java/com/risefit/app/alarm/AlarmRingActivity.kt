package com.risefit.app.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.MainActivity
import com.risefit.app.data.AlarmEntity
import com.risefit.app.data.AppDatabase
import com.risefit.app.data.AppSettings
import com.risefit.app.data.ScheduleItem
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.theme.RF
import com.risefit.app.ui.theme.RiseFitTheme
import com.risefit.app.util.Jalali
import com.risefit.app.util.Quotes
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Full-screen alarm UI: dismiss or snooze, then a motivational start-of-day screen. */
class AlarmRingActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val accentIndex = AppSettings(this).load().accentIndex

        setContent {
            RiseFitTheme(accentIndex) {
                RingScreen(
                    alarmId = alarmId,
                    onStop = {
                        Notifier.cancelFallback(this)
                        startService(AlarmService.stopIntent(this))
                    },
                    onSnooze = {
                        Notifier.cancelFallback(this)
                        startService(AlarmService.snoozeIntent(this))
                        finish()
                    },
                    onStartDay = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNothingRinging = { finish() }
                )
            }
        }
    }
}

@Composable
private fun RingScreen(
    alarmId: Int,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
    onStartDay: () -> Unit,
    onNothingRinging: () -> Unit
) {
    val context = LocalContext.current
    var dismissed by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    val alarm by produceState<AlarmEntity?>(null, alarmId) {
        value = if (alarmId < 0) null else withContext(Dispatchers.IO) {
            AppDatabase.get(context).alarmDao().getById(alarmId)
        }
    }
    val firstActivity by produceState<ScheduleItem?>(null, alarm) {
        val a = alarm
        value = withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val items = AppDatabase.get(context).scheduleDao().getAll()
                .filter { it.enabled && maskHas(it.daysMask, dayIndexOf(today)) }
            val afterAlarm = if (a != null) {
                items.firstOrNull { it.hour * 60 + it.minute >= a.hour * 60 + a.minute }
            } else null
            afterAlarm ?: items.firstOrNull()
        }
    }

    // If the service stops (notification action, timeout) while we are still ringing, close the screen.
    LaunchedEffect(Unit) {
        AlarmState.ringingId.collect { id ->
            if (id == -1 && !dismissed) onNothingRinging()
        }
    }

    BackHandler(enabled = !dismissed) { }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )

    Box(Modifier.fillMaxSize().background(RF.Bg), contentAlignment = Alignment.Center) {
        if (!dismissed) {
            Box(Modifier.size(300.dp).scale(scale).background(accent.copy(alpha = 0.14f), CircleShape))
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (alarm != null) formatTime(alarm!!.hour, alarm!!.minute) else "بیدار شو!",
                    fontSize = 72.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary
                )
                val label = if (alarmId == AlarmScheduler.TEST_ID) "تست آلارم" else alarm?.label.orEmpty()
                Text(
                    label.ifBlank { "وقت بیدار شدن است" },
                    color = accent, fontSize = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                )
                Button(
                    onClick = {
                        dismissed = true
                        onStop()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) { Text("خاموش", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp)
                ) { Text("چرت ${(alarm?.snoozeMinutes ?: 5).fa()} دقیقه‌ای", fontSize = 16.sp) }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("صبح بخیر!", fontSize = 34.sp, fontWeight = FontWeight.Black, color = accent)
                DimText(Jalali.formatWithWeekday(LocalDate.now()), Modifier.padding(top = 4.dp))
                GlowCard(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(Quotes.MAIN_SENTENCE, color = RF.TextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
                    DimText(Quotes.today(), Modifier.padding(top = 10.dp))
                }
                val first = firstActivity
                if (first != null) {
                    GlowCard(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        DimText("اولین برنامه امروز")
                        Text(first.title, color = RF.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        DimText("ساعت ${formatTime(first.hour, first.minute)}")
                    }
                }
                Button(
                    onClick = onStartDay,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(56.dp)
                ) { Text("شروع روز", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
