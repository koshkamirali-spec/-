package com.risefit.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.risefit.app.PlusViewModel
import com.risefit.app.data.TaskCats
import com.risefit.app.data.TaskItem
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DayNavigator
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.PercentBar
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.PlannerStats
import com.risefit.app.util.fa
import com.risefit.app.util.todayEpochDay
import java.time.LocalTime

@Composable
fun PlannerScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val all by vm.tasks.collectAsState()
    var day by remember { mutableStateOf(todayEpochDay()) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    var deleting by remember { mutableStateOf<TaskItem?>(null) }

    val list = all.filter { it.epochDay == day }
    val doneCount = list.count { it.done }
    val percent = PlannerStats.percent(list)
    val openCount = list.size - doneCount

    fun newTask(): TaskItem {
        val start = ((LocalTime.now().hour + 1).coerceAtMost(22)) * 60
        return TaskItem(epochDay = day, title = "", startMin = start, endMin = start + 60)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "دفترچه برنامه",
                subtitle = "کارهای هر روز را بنویس و تیک بزن",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { editing = newTask() }) {
                        Icon(Icons.Rounded.Add, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        item { DayNavigator(day) { day = it } }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("پیشرفت این روز", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    Text("${percent.fa()}٪", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                PercentBar(percent / 100f, Modifier.padding(vertical = 10.dp))
                DimText(
                    if (list.isEmpty()) "هنوز کاری برای این روز نوشته نشده"
                    else "${doneCount.fa()} از ${list.size.fa()} کار انجام شد"
                )
                if (openCount > 0 && day <= todayEpochDay()) {
                    TextButton(onClick = { vm.moveUnfinishedToNextDay(day) }) {
                        Text("انتقال کارهای انجام‌نشده به روز بعد")
                    }
                }
            }
        }
        if (list.isEmpty()) {
            item { EmptyHint("با دکمه + اولین کار این روز را اضافه کن.") }
        } else {
            items(list, key = { it.id }) { t ->
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = t }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = t.done, onCheckedChange = { vm.setTaskDone(t, it) })
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title, fontWeight = FontWeight.Bold,
                                color = if (t.done) RF.TextDim else RF.TextPrimary,
                                textDecoration = if (t.done) TextDecoration.LineThrough else null
                            )
                            DimText(
                                "${PlannerStats.time(t.startMin)} تا ${PlannerStats.time(t.endMin)} • ${TaskCats.label(t.category)}"
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { initial ->
        TaskDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveTask(saved)
            },
            onDelete = if (initial.id != 0) ({
                editing = null
                deleting = initial
            }) else null,
            onDismiss = { editing = null }
        )
    }

    deleting?.let { t ->
        ConfirmDialog(
            title = "حذف کار",
            text = "«${t.title}» حذف شود؟",
            onConfirm = {
                vm.deleteTask(t)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun TaskDialog(
    initial: TaskItem,
    onSave: (TaskItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var startMin by remember { mutableStateOf(initial.startMin) }
    var endMin by remember { mutableStateOf(initial.endMin) }
    var category by remember { mutableStateOf(initial.category) }

    val timeOk = endMin > startMin
    FormDialog(if (initial.id == 0) "کار جدید" else "ویرایش کار", onDismiss) {
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(60) },
            label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(
                "شروع", startMin / 60, startMin % 60,
                { h, m ->
                    startMin = h * 60 + m
                    if (endMin <= startMin) endMin = (startMin + 30).coerceAtMost(23 * 60 + 59)
                },
                Modifier.weight(1f)
            )
            TimeField("پایان", endMin / 60, endMin % 60, { h, m -> endMin = h * 60 + m }, Modifier.weight(1f))
        }
        if (!timeOk) {
            Text("ساعت پایان باید بعد از شروع باشد.", color = RF.Warning, style = MaterialTheme.typography.bodySmall)
        }
        Text("دسته", color = RF.TextPrimary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskCats.all.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(TaskCats.label(c)) })
            }
        }
        DialogActions(
            onDismiss = onDismiss,
            onSave = {
                onSave(initial.copy(title = title.trim(), startMin = startMin, endMin = endMin, category = category))
            },
            canSave = title.isNotBlank() && timeOk,
            onDelete = onDelete
        )
    }
}
