package com.risefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.risefit.app.MainViewModel
import com.risefit.app.data.FutsalPlan
import com.risefit.app.data.GymExercise
import com.risefit.app.data.ScheduleItem
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.LogTypes
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import java.time.LocalDate

private val colGym = Color(0xFFB26BFF)
private val colHome = Color(0xFF4DA3FF)
private val colFutsal = Color(0xFF39E58C)

private fun typeColor(t: String): Color = when (t) {
    "GYM" -> colGym
    "FUTSAL" -> colFutsal
    else -> colHome
}

private fun typeName(t: String): String = when (t) {
    "GYM" -> "باشگاه"
    "FUTSAL" -> "فوتسال"
    else -> "تمرین خانه"
}

/** Which training types are planned on this weekday, from the schedule, gym plan and futsal plan. */
private fun plannedTypes(
    date: LocalDate,
    schedule: List<ScheduleItem>,
    gym: List<GymExercise>,
    futsal: List<FutsalPlan>
): List<String> {
    val di = dayIndexOf(date)
    val result = LinkedHashSet<String>()
    for (s in schedule) {
        if (!s.enabled || !maskHas(s.daysMask, di)) continue
        if (s.category == "GYM") result.add("GYM")
        if (s.category == "HOME") result.add("HOME")
        if (s.category == "FUTSAL") result.add("FUTSAL")
    }
    if (gym.any { it.dayIndex == di }) result.add("GYM")
    if (futsal.any { it.dayIndex == di }) result.add("FUTSAL")
    return listOf("GYM", "HOME", "FUTSAL").filter { it in result }
}

private fun doneTypesOf(logs: List<com.risefit.app.data.WorkoutLog>, date: LocalDate): Set<String> {
    val day = date.toEpochDay()
    return logs.filter { it.epochDay == day }.map { if (it.type == "ABS") "HOME" else it.type }.toSet()
}

@Composable
private fun Dots(planned: List<String>, done: Set<String>) {
    val all = (planned + done.filter { it !in planned })
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        all.forEach { t ->
            val c = typeColor(t)
            val filled = t in done
            Box(
                Modifier.size(7.dp).let {
                    if (filled) it.background(c, CircleShape) else it.border(1.dp, c, CircleShape)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: MainViewModel) {
    val schedule by vm.schedule.collectAsState()
    val gym by vm.gym.collectAsState()
    val futsal by vm.futsal.collectAsState()
    val logs by vm.logs.collectAsState()

    val todayDate = LocalDate.now()
    var mode by remember { mutableStateOf(1) } // 0 weekly, 1 monthly
    var selected by remember { mutableStateOf(todayDate) }
    var adding by remember { mutableStateOf<String?>(null) }

    val accent = MaterialTheme.colorScheme.primary
    val jSel = Jalali.fromLocalDate(selected)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(title = "تقویم", subtitle = "برنامه، تمرین‌های انجام‌شده و روزهای استراحت") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("هفتگی") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("ماهانه") })
            }
        }

        // ---- navigation ----
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    selected = if (mode == 1) {
                        val pm = if (jSel.month == 1) 12 else jSel.month - 1
                        val py = if (jSel.month == 1) jSel.year - 1 else jSel.year
                        Jalali.toLocalDate(py, pm, 1)
                    } else selected.minusDays(7)
                }) { Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "قبلی", tint = RF.TextPrimary) }
                Text(
                    "${Jalali.monthNames[jSel.month - 1]} ${jSel.year.fa()}",
                    Modifier.weight(1f), fontWeight = FontWeight.Bold, color = RF.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = {
                    selected = if (mode == 1) {
                        val nm = if (jSel.month == 12) 1 else jSel.month + 1
                        val ny = if (jSel.month == 12) jSel.year + 1 else jSel.year
                        Jalali.toLocalDate(ny, nm, 1)
                    } else selected.plusDays(7)
                }) { Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "بعدی", tint = RF.TextPrimary) }
            }
        }

        // ---- grid / week ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                if (mode == 1) {
                    val first = Jalali.toLocalDate(jSel.year, jSel.month, 1)
                    val offset = Jalali.weekColumn(first)
                    val days = Jalali.daysInMonth(jSel.year, jSel.month)
                    val rows = (offset + days + 6) / 7
                    Row(Modifier.fillMaxWidth()) {
                        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                            Text(
                                it, Modifier.weight(1f), color = RF.TextDim,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            for (c in 0 until 7) {
                                val dayNum = r * 7 + c - offset + 1
                                if (dayNum < 1 || dayNum > days) {
                                    Box(Modifier.weight(1f).height(48.dp))
                                } else {
                                    val date = Jalali.toLocalDate(jSel.year, jSel.month, dayNum)
                                    val isSel = date == selected
                                    val isToday = date == todayDate
                                    Column(
                                        Modifier.weight(1f).height(48.dp).padding(2.dp)
                                            .let { if (isSel) it.border(1.5.dp, accent, RoundedCornerShape(10.dp)) else it }
                                            .clickable { selected = date },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            dayNum.fa(),
                                            color = if (isToday) accent else RF.TextPrimary,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal
                                        )
                                        Dots(plannedTypes(date, schedule, gym, futsal), doneTypesOf(logs, date))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val weekStart = selected.minusDays(Jalali.weekColumn(selected).toLong())
                    for (i in 0 until 7) {
                        val date = weekStart.plusDays(i.toLong())
                        val planned = plannedTypes(date, schedule, gym, futsal)
                        val isSel = date == selected
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .let { if (isSel) it.border(1.5.dp, accent, RoundedCornerShape(12.dp)) else it }
                                .clickable { selected = date }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Jalali.weekdayName(date.dayOfWeek),
                                    color = if (date == todayDate) accent else RF.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                val j = Jalali.fromLocalDate(date)
                                DimText("${j.day.fa()} ${Jalali.monthNames[j.month - 1]}")
                            }
                            if (planned.isEmpty()) {
                                Text("استراحت", color = RF.Success, style = MaterialTheme.typography.bodySmall)
                            } else {
                                Dots(planned, doneTypesOf(logs, date))
                            }
                        }
                    }
                }
                Row(
                    Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    listOf("GYM", "HOME", "FUTSAL").forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(8.dp).background(typeColor(t), CircleShape))
                            DimText(typeName(t))
                        }
                    }
                    DimText("توپر = انجام شده")
                }
            }
        }

        // ---- selected day ----
        item {
            SectionTitle(Jalali.formatWithWeekday(selected))
        }
        item {
            val planned = plannedTypes(selected, schedule, gym, futsal)
            val daySchedule = schedule.filter { it.enabled && maskHas(it.daysMask, dayIndexOf(selected)) }
            GlowCard(Modifier.fillMaxWidth()) {
                if (planned.isEmpty()) {
                    Text("روز استراحت", fontWeight = FontWeight.Bold, color = RF.Success)
                    DimText("برای این روز تمرین سنگینی برنامه‌ریزی نشده. ریکاوری هم بخشی از پیشرفته.")
                } else {
                    Text(
                        "برنامه: " + planned.joinToString("، ") { typeName(it) },
                        fontWeight = FontWeight.Bold, color = RF.TextPrimary
                    )
                }
                daySchedule.forEach { s ->
                    DimText("${formatTime(s.hour, s.minute)}  ${s.title}", Modifier.padding(top = 3.dp))
                }
                Row(
                    Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogTypes.all.forEach { t ->
                        OutlinedButton(onClick = { adding = t }) { Text("ثبت ${LogTypes.label(t)}") }
                    }
                }
            }
        }
        val dayLogs = logs.filter { it.epochDay == selected.toEpochDay() }
        if (dayLogs.isEmpty()) {
            item { DimText("برای این روز جلسه‌ای ثبت نشده.") }
        } else {
            items(dayLogs, key = { it.id }) { l -> LogRow(l, onDelete = { vm.deleteLog(l) }) }
        }
    }

    adding?.let { type ->
        SessionLogDialog(
            title = "ثبت ${LogTypes.label(type)} – ${Jalali.format(selected)}",
            defaultNotes = "",
            defaultDuration = 0,
            onConfirm = { duration, notes ->
                adding = null
                vm.addLog(type, LogTypes.label(type), duration, notes, selected.toEpochDay())
            },
            onDismiss = { adding = null }
        )
    }
}
