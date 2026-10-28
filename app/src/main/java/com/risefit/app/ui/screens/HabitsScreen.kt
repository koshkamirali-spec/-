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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.risefit.app.PlusViewModel
import com.risefit.app.data.Habit
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.PercentBar
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.HabitStats
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import com.risefit.app.util.todayEpochDay
import java.time.LocalDate

private val weekLetters = listOf("د", "س", "چ", "پ", "ج", "ش", "ی")
private val suggestions = listOf("آب خوردن", "ورزش", "مطالعه", "مرور درس", "خواب به‌موقع", "کشش و نرمش")

@Composable
fun HabitsScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val habits by vm.habits.collectAsState()
    val logs by vm.habitLogs.collectAsState()
    var editing by remember { mutableStateOf<Habit?>(null) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    val today = todayEpochDay()
    val monthStart = HabitStats.monthStart(today)
    val daysThisMonth = (today - monthStart + 1).toInt().coerceAtLeast(1)
    val byHabit = remember(logs) {
        logs.groupBy({ it.habitId }, { it.epochDay }).mapValues { entry -> entry.value.toSet() }
    }
    val doneToday = habits.count { today in (byHabit[it.id] ?: emptySet()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "عادت‌ها",
                subtitle = "کوچک و پیوسته؛ یک روز جا افتادن مهم نیست",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { editing = Habit(name = "") }) {
                        Icon(Icons.Rounded.Add, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        if (habits.isEmpty()) {
            item {
                GlowCard(Modifier.fillMaxWidth()) {
                    Text("اولین عادتت را بساز", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    DimText("یکی از پیشنهادها را بزن یا با دکمه + عادت خودت را بنویس.")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { s ->
                            FilterChip(selected = false, onClick = { vm.saveHabit(Habit(name = s)) }, label = { Text(s) })
                        }
                    }
                }
            }
        } else {
            item {
                GlowCard(Modifier.fillMaxWidth()) {
                    Text(
                        "امروز ${doneToday.fa()} از ${habits.size.fa()} عادت انجام شد",
                        fontWeight = FontWeight.Bold, color = RF.TextPrimary
                    )
                    PercentBar(doneToday.toFloat() / habits.size, Modifier.padding(top = 10.dp))
                }
            }
            items(habits, key = { it.id }) { h ->
                val days = byHabit[h.id] ?: emptySet()
                val streak = HabitStats.streak(days, today)
                val isDoneToday = today in days
                val week = HabitStats.countIn(days, today - 6, today)
                val month = HabitStats.countIn(days, monthStart, today)
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = h }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { vm.setHabitDay(h.id, today, !isDoneToday) }) {
                            Icon(
                                if (isDoneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "انجام شد",
                                tint = if (isDoneToday) RF.Success else RF.TextDim,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(h.name, fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                            DimText(HabitStats.encouragement(streak, isDoneToday))
                        }
                        if (streak > 0) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(streak.fa(), fontWeight = FontWeight.Black, color = RF.TextPrimary)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (back in 6 downTo 0) {
                            val d = today - back
                            DayDot(
                                letter = weekLetters[dayIndexOf(LocalDate.ofEpochDay(d))],
                                done = d in days,
                                onClick = { vm.setHabitDay(h.id, d, d !in days) }
                            )
                        }
                    }
                    DimText(
                        "هفته اخیر: ${week.fa()} از ${7.fa()} روز  •  این ماه: ${month.fa()} از ${daysThisMonth.fa()} روز",
                        Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    editing?.let { initial ->
        HabitDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveHabit(saved)
            },
            onDelete = if (initial.id != 0) ({
                editing = null
                deleting = initial
            }) else null,
            onDismiss = { editing = null }
        )
    }

    deleting?.let { h ->
        ConfirmDialog(
            title = "حذف عادت",
            text = "«${h.name}» و سابقه‌اش حذف شود؟",
            onConfirm = {
                vm.deleteHabit(h)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun DayDot(letter: String, done: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(34.dp)
            .then(
                if (done) Modifier.background(accent, CircleShape)
                else Modifier.border(1.dp, RF.Outline, CircleShape)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            color = if (done) Color(0xFF12001F) else RF.TextDim,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HabitDialog(
    initial: Habit,
    onSave: (Habit) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    FormDialog(if (initial.id == 0) "عادت جدید" else "ویرایش عادت", onDismiss) {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) },
            label = { Text("اسم عادت") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        DialogActions(
            onDismiss = onDismiss,
            onSave = { onSave(initial.copy(name = name.trim())) },
            canSave = name.isNotBlank(),
            onDelete = onDelete
        )
    }
}
