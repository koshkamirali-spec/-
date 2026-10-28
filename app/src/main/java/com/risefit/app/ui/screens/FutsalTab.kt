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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.risefit.app.MainViewModel
import com.risefit.app.data.FutsalPlan
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.NumberField
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.LogTypes
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import java.time.LocalDate

private val focusTypes = listOf("تکنیک", "پاس", "شوت", "سرعت", "آمادگی جسمانی")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutsalTab(vm: MainViewModel) {
    val plans by vm.futsal.collectAsState()
    val gym by vm.gym.collectAsState()
    val logs by vm.logs.collectAsState()

    var editing by remember { mutableStateOf<FutsalPlan?>(null) }
    var deleting by remember { mutableStateOf<FutsalPlan?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var duration by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }

    val gymDays = gym.map { it.dayIndex }.toSet()
    val futsalLogs = logs.filter { it.type == LogTypes.FUTSAL }.take(10)
    val sortedPlans = plans.sortedWith(
        compareBy({ Jalali.weekOrder.indexOf(it.dayIndex) }, { it.hour }, { it.minute })
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("برنامه هفتگی فوتسال", Modifier.weight(1f))
                IconButton(onClick = {
                    editing = FutsalPlan(dayIndex = dayIndexOf(LocalDate.now()), hour = 17, minute = 30)
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (sortedPlans.isEmpty()) {
            item { EmptyHint("هنوز روزی برای فوتسال ثبت نکردی.") }
        } else {
            items(sortedPlans, key = { it.id }) { p ->
                GlowCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Jalali.weekdayNames[p.dayIndex], fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                            DimText("ساعت ${formatTime(p.hour, p.minute)} • ${p.durationMin.fa()} دقیقه")
                        }
                        IconButton(onClick = { editing = p }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = RF.TextDim)
                        }
                        IconButton(onClick = { deleting = p }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = RF.Danger)
                        }
                    }
                    if (p.dayIndex in gymDays) {
                        Text(
                            "این روز باشگاه هم داری؛ اگر خسته‌ای یکی را سبک‌تر کن.",
                            color = RF.Warning, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item { SectionTitle("ثبت تمرین امروز", Modifier.padding(top = 8.dp)) }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                DimText("روی چه چیزی کار کردی؟")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    focusTypes.forEach { t ->
                        FilterChip(
                            selected = t in selected,
                            onClick = { selected = if (t in selected) selected - t else selected + t },
                            label = { Text(t) }
                        )
                    }
                }
                NumberField("مدت (دقیقه)", duration, { duration = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it.take(400) },
                    label = { Text("یادداشت عملکرد") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        val title = if (selected.isEmpty()) "فوتسال"
                        else "فوتسال – " + focusTypes.filter { it in selected }.joinToString("، ")
                        vm.addLog(LogTypes.FUTSAL, title, duration.toIntOrNull() ?: 0, notes.trim())
                        selected = emptySet()
                        notes = ""
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) { Text("ثبت جلسه فوتسال") }
            }
        }

        item { SectionTitle("سابقه فوتسال", Modifier.padding(top = 8.dp)) }
        if (futsalLogs.isEmpty()) {
            item { EmptyHint("هنوز جلسه‌ای ثبت نشده.") }
        } else {
            items(futsalLogs, key = { it.id }) { l -> LogRow(l, onDelete = { vm.deleteLog(l) }) }
        }
    }

    editing?.let { initial ->
        FutsalEditDialog(initial, onSave = { editing = null; vm.saveFutsal(it) }, onDismiss = { editing = null })
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "حذف از برنامه", text = "فوتسال ${Jalali.weekdayNames[p.dayIndex]} حذف شود؟",
            onConfirm = { vm.deleteFutsal(p); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FutsalEditDialog(initial: FutsalPlan, onSave: (FutsalPlan) -> Unit, onDismiss: () -> Unit) {
    var day by remember { mutableStateOf(initial.dayIndex) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var duration by remember { mutableStateOf(initial.durationMin.toString()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(24.dp), color = RF.Card, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (initial.id == 0) "روز فوتسال جدید" else "ویرایش فوتسال",
                    style = MaterialTheme.typography.titleLarge, color = RF.TextPrimary
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Jalali.weekOrder.forEach { d ->
                        FilterChip(selected = day == d, onClick = { day = d }, label = { Text(Jalali.weekdayNames[d]) })
                    }
                }
                TimeField("ساعت", hour, minute, { h, m -> hour = h; minute = m }, Modifier.fillMaxWidth())
                NumberField("مدت (دقیقه)", duration, { duration = it }, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("لغو") }
                    Button(onClick = {
                        onSave(
                            initial.copy(
                                dayIndex = day, hour = hour, minute = minute,
                                durationMin = (duration.toIntOrNull() ?: 60).coerceIn(5, 600)
                            )
                        )
                    }) { Text("ذخیره") }
                }
            }
        }
    }
}
