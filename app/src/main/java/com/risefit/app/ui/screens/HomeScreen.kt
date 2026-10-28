package com.risefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.MainViewModel
import com.risefit.app.alarm.AlarmScheduler
import com.risefit.app.data.ScheduleItem
import com.risefit.app.data.WorkoutLog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.LocalImage
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.components.categoryIcon
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Categories
import com.risefit.app.util.Jalali
import com.risefit.app.util.Progress
import com.risefit.app.util.Quotes
import com.risefit.app.util.describeDuration
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val tips = listOf(
    "امشب زودتر بخواب؛ رشد عضله در خواب اتفاق می‌افتد.",
    "امروز آب کافی بنوش، مخصوصاً قبل و بعد از تمرین.",
    "گرم‌کردن پنج‌دقیقه‌ای را فراموش نکن.",
    "فرم درست مهم‌تر از وزنه سنگین است.",
    "اگر خسته‌ای، تمرین را سبک‌تر کن؛ استراحت هم پیشرفت است.",
    "پروتئین و غذای کامل، سوخت بدن توست.",
    "کشش بعد از تمرین به ریکاوری کمک می‌کند."
)

private fun isDone(item: ScheduleItem, logsToday: List<WorkoutLog>, now: LocalDateTime): Boolean =
    when (item.category) {
        "GYM" -> logsToday.any { it.type == "GYM" }
        "HOME" -> logsToday.any { it.type == "ABS" || it.type == "HOME" }
        "FUTSAL" -> logsToday.any { it.type == "FUTSAL" }
        else -> LocalTime.of(item.hour, item.minute).isBefore(now.toLocalTime())
    }

@Composable
fun HomeScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val alarms by vm.alarms.collectAsState()
    val schedule by vm.schedule.collectAsState()
    val logs by vm.logs.collectAsState()
    val goals by vm.goals.collectAsState()

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L)
        }
    }

    val today = now.toLocalDate()
    val epochDay = today.toEpochDay()
    val dayIdx = dayIndexOf(today)
    val todayItems = schedule.filter { it.enabled && maskHas(it.daysMask, dayIdx) }
    val logsToday = logs.filter { it.epochDay == epochDay }
    val doneCount = todayItems.count { isDone(it, logsToday, now) }
    val hasTraining = todayItems.any { Categories.isTraining(it.category) }
    val stats = Progress.compute(logs, epochDay)
    val nextAlarm = AlarmScheduler.nextAmong(alarms)
    val mainGoal = goals.firstOrNull { it.stage == 0 }
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- title ----
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "RiseFit", fontSize = 30.sp, fontWeight = FontWeight.Black, color = accent
                    )
                    DimText(Quotes.SLOGAN)
                }
                IconButton(onClick = { onNavigate("settings") }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات", tint = RF.TextPrimary)
                }
            }
        }

        // ---- clock ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        formatTime(now.hour, now.minute), fontSize = 56.sp,
                        fontWeight = FontWeight.Black, color = RF.TextPrimary
                    )
                    Text(
                        now.second.toString().padStart(2, '0').fa(), fontSize = 20.sp,
                        color = accent, modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Text(Jalali.formatWithWeekday(today), color = RF.TextDim, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // ---- next alarm ----
        item {
            GlowCard(Modifier.fillMaxWidth(), onClick = { onNavigate("alarms") }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        if (nextAlarm == null) {
                            Text("آلارمی فعال نیست", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                            DimText("برای تنظیم آلارم صبحگاهی اینجا را بزن")
                        } else {
                            val (alarm, at) = nextAlarm
                            val minutes = ChronoUnit.MINUTES.between(now, at)
                            val dayLabel = when {
                                at.toLocalDate() == today -> "امروز"
                                at.toLocalDate() == today.plusDays(1) -> "فردا"
                                else -> Jalali.weekdayName(at.dayOfWeek)
                            }
                            Text(
                                "آلارم بعدی: $dayLabel ${formatTime(alarm.hour, alarm.minute)}",
                                fontWeight = FontWeight.Bold, color = RF.TextPrimary
                            )
                            DimText("${describeDuration(minutes)} تا بیدار شدن")
                        }
                    }
                }
            }
        }

        // ---- motivation ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Text(Quotes.today(), style = MaterialTheme.typography.titleMedium, color = RF.TextPrimary)
                Text(
                    Quotes.MAIN_SENTENCE, color = accent, modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ---- today's plan ----
        item { SectionTitle("برنامه امروز") }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                if (todayItems.isEmpty()) {
                    DimText("برای امروز برنامه‌ای ثبت نشده. از «برنامه روزانه» اضافه کن.")
                } else {
                    Text(
                        "${doneCount.fa()} از ${todayItems.size.fa()} مورد انجام شد",
                        color = RF.TextPrimary, fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / todayItems.size },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(6.dp).clip(CircleShape),
                        color = accent, trackColor = RF.CardHigh
                    )
                    todayItems.forEach { item ->
                        val done = isDone(item, logsToday, now)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) RF.Success else RF.TextDim
                            )
                            Icon(categoryIcon(item.category), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            Text(
                                item.title, Modifier.weight(1f),
                                color = if (done) RF.TextDim else RF.TextPrimary
                            )
                            Text(formatTime(item.hour, item.minute), color = RF.TextDim)
                        }
                    }
                    if (!hasTraining) {
                        Text(
                            "امروز روز استراحته. ریکاوری هم بخشی از پیشرفته.",
                            color = RF.Success, modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ---- shortcuts ----
        item { SectionTitle("میانبرها") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut(Icons.Rounded.FitnessCenter, "باشگاه", Modifier.weight(1f)) {
                        vm.workoutTab.value = 0; onNavigate("workout")
                    }
                    Shortcut(Icons.Rounded.DirectionsRun, "تمرین خانه", Modifier.weight(1f)) {
                        vm.workoutTab.value = 1; onNavigate("workout")
                    }
                    Shortcut(Icons.Rounded.SportsSoccer, "فوتسال", Modifier.weight(1f)) {
                        vm.workoutTab.value = 2; onNavigate("workout")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut(Icons.Rounded.CalendarMonth, "تقویم", Modifier.weight(1f)) { onNavigate("calendar") }
                    Shortcut(Icons.Rounded.BarChart, "پیشرفت", Modifier.weight(1f)) { onNavigate("progress") }
                    Shortcut(Icons.Rounded.Schedule, "برنامه من", Modifier.weight(1f)) { onNavigate("schedule") }
                }
            }
        }

        // ---- my tools ----
        item { SectionTitle("ابزارهای من") }
        item {
            GlowCard(Modifier.fillMaxWidth(), onClick = { onNavigate("ai") }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text("RiseAI", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                        DimText("دستیار فارسی؛ کار و یادآور اضافه می‌کند و وضعیتت را می‌گوید")
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut(Icons.Rounded.EditCalendar, "دفترچه برنامه", Modifier.weight(1f)) { onNavigate("planner") }
                    Shortcut(Icons.Rounded.NotificationsActive, "یادآورها", Modifier.weight(1f)) { onNavigate("reminders") }
                    Shortcut(Icons.Rounded.TaskAlt, "عادت‌ها", Modifier.weight(1f)) { onNavigate("habits") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut(Icons.Rounded.EmojiEvents, "هدف‌ها", Modifier.weight(1f)) { onNavigate("mygoals") }
                    Shortcut(Icons.Rounded.Timer, "تمرکز", Modifier.weight(1f)) { onNavigate("focus") }
                    Shortcut(Icons.Rounded.Book, "دفترچه شخصی", Modifier.weight(1f)) { onNavigate("journal") }
                }
            }
        }

        // ---- streak / points ----
        item {
            GlowCard(Modifier.fillMaxWidth(), onClick = { onNavigate("progress") }) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Stat(Icons.Rounded.LocalFireDepartment, stats.streak.fa(), "روز استمرار")
                    Stat(Icons.Rounded.Star, stats.totalPoints.fa(), "امتیاز")
                    Stat(Icons.Rounded.FitnessCenter, stats.activeDaysLast7.fa(), "روز فعال هفته")
                }
            }
        }

        // ---- goal picture ----
        if (mainGoal?.photoUri != null) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))
                ) {
                    LocalImage(mainGoal.photoUri, Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                        )
                    )
                    Text(
                        "هدف من", color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                    )
                }
            }
        }

        // ---- tip ----
        item {
            DimText(
                tips[Math.floorMod(epochDay, tips.size.toLong()).toInt()],
                Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    GlowCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(
                label, color = RF.TextPrimary, modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary)
        DimText(label)
    }
}
