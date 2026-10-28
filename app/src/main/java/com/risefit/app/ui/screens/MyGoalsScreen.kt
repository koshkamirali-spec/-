package com.risefit.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.PlusViewModel
import com.risefit.app.data.GoalItem
import com.risefit.app.data.GoalStep
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.NumberField
import com.risefit.app.ui.components.PercentBar
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.GoalStats
import com.risefit.app.util.Jalali
import com.risefit.app.util.fa
import com.risefit.app.util.todayEpochDay
import java.time.LocalDate

@Composable
fun MyGoalsScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val goals by vm.goals.collectAsState()
    val steps by vm.steps.collectAsState()
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<GoalItem?>(null) }
    var deleting by remember { mutableStateOf<GoalItem?>(null) }
    val today = todayEpochDay()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "هدف‌های من",
                subtitle = "هدف ۳۰، ۶۰ یا ۱۰۰ روزه با قدم‌های کوچک",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        editing = GoalItem(title = "", startEpochDay = today, deadlineEpochDay = today + 30)
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        if (goals.isEmpty()) {
            item { EmptyHint("هنوز هدفی نساخته‌ای. با دکمه + اولین هدفت را بنویس.") }
        } else {
            items(goals, key = { it.id }) { g ->
                GoalCard(
                    vm = vm,
                    goal = g,
                    goalSteps = steps.filter { it.goalId == g.id },
                    today = today,
                    expanded = expandedId == g.id,
                    onToggle = { expandedId = if (expandedId == g.id) null else g.id },
                    onEdit = { editing = g },
                    onDelete = { deleting = g }
                )
            }
        }
    }

    editing?.let { initial ->
        GoalDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveGoal(saved)
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { g ->
        ConfirmDialog(
            title = "حذف هدف",
            text = "«${g.title}» و قدم‌هایش حذف شود؟",
            onConfirm = {
                vm.deleteGoal(g)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun GoalCard(
    vm: PlusViewModel,
    goal: GoalItem,
    goalSteps: List<GoalStep>,
    today: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val percent = GoalStats.percent(goal, goalSteps)
    val accent = if (goal.status == "DONE") RF.Success else MaterialTheme.colorScheme.primary
    val total = GoalStats.totalDays(goal)
    val left = GoalStats.daysLeft(goal, today)

    GlowCard(Modifier.fillMaxWidth(), onClick = onToggle, accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                DimText(
                    "${GoalStats.statusLabel(goal.status)} • ${total.fa()} روزه • " +
                        if (left >= 0) "${left.fa()} روز مانده" else "مهلت گذشته"
                )
            }
            Text("${percent.fa()}٪", fontWeight = FontWeight.Black, color = accent)
        }
        PercentBar(percent / 100f, Modifier.padding(top = 10.dp), color = accent)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GoalStats.milestones.forEach { m -> MilestoneChip(m, percent >= m, accent) }
        }
        DimText(GoalStats.message(goal, percent, today), Modifier.padding(top = 8.dp))

        if (expanded) {
            Text(
                "قدم‌ها", fontWeight = FontWeight.Bold, color = RF.TextPrimary,
                modifier = Modifier.padding(top = 14.dp)
            )
            if (goalSteps.isEmpty()) {
                DimText("قدمی ننوشته‌ای. می‌توانی پیشرفت را دستی تنظیم کنی یا قدم اضافه کنی تا درصد خودکار حساب شود.")
                var slider by remember(goal.id, goal.progress) { mutableStateOf(goal.progress.toFloat()) }
                Slider(
                    value = slider,
                    onValueChange = { slider = it },
                    onValueChangeFinished = { vm.setGoalProgress(goal, slider.toInt()) },
                    valueRange = 0f..100f
                )
            } else {
                goalSteps.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = s.done, onCheckedChange = { vm.setStepDone(s, it) })
                        Text(
                            s.text, Modifier.weight(1f),
                            color = if (s.done) RF.TextDim else RF.TextPrimary,
                            textDecoration = if (s.done) TextDecoration.LineThrough else null
                        )
                        IconButton(onClick = { vm.deleteStep(s) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "حذف قدم", tint = RF.TextDim)
                        }
                    }
                }
            }
            var newStep by remember { mutableStateOf("") }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newStep, onValueChange = { newStep = it.take(80) },
                    label = { Text("قدم جدید") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                IconButton(
                    enabled = newStep.isNotBlank(),
                    onClick = {
                        vm.addStep(goal.id, newStep)
                        newStep = ""
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "افزودن قدم", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
                when (goal.status) {
                    "ACTIVE" -> {
                        TextButton(onClick = { vm.setGoalStatus(goal, "DONE") }) { Text("تکمیل شد") }
                        TextButton(onClick = { vm.setGoalStatus(goal, "PAUSED") }) { Text("متوقف کن") }
                    }
                    "PAUSED" -> TextButton(onClick = { vm.setGoalStatus(goal, "ACTIVE") }) { Text("ادامه بده") }
                    else -> TextButton(onClick = { vm.setGoalStatus(goal, "ACTIVE") }) { Text("بازگشایی") }
                }
                TextButton(onClick = onDelete) { Text("حذف", color = RF.Danger) }
            }
        }
    }
}

@Composable
private fun MilestoneChip(value: Int, reached: Boolean, accent: Color) {
    Text(
        "${value.fa()}٪",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (reached) Color(0xFF12001F) else RF.TextDim,
        modifier = Modifier
            .background(if (reached) accent else RF.CardHigh, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun GoalDialog(initial: GoalItem, onSave: (GoalItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var days by remember { mutableStateOf(GoalStats.totalDays(initial).toString()) }
    val dayCount = days.toIntOrNull() ?: 0
    val valid = title.isNotBlank() && dayCount in 1..365
    val deadline = initial.startEpochDay + dayCount

    FormDialog(if (initial.id == 0) "هدف جدید" else "ویرایش هدف", onDismiss) {
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(80) },
            label = { Text("هدف من") }, modifier = Modifier.fillMaxWidth()
        )
        Text("مدت هدف", color = RF.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 60, 100).forEach { d ->
                FilterChip(
                    selected = dayCount == d, onClick = { days = d.toString() },
                    label = { Text("${d.fa()} روز") }
                )
            }
        }
        NumberField("یا تعداد روز دلخواه", days, { days = it }, Modifier.fillMaxWidth())
        if (dayCount in 1..365) {
            DimText("مهلت: ${Jalali.format(LocalDate.ofEpochDay(deadline))}")
        } else {
            Text("تعداد روز باید بین ۱ تا ۳۶۵ باشد.", color = RF.Warning, style = MaterialTheme.typography.bodySmall)
        }
        DialogActions(
            onDismiss = onDismiss,
            onSave = { onSave(initial.copy(title = title.trim(), deadlineEpochDay = deadline)) },
            canSave = valid
        )
    }
}
