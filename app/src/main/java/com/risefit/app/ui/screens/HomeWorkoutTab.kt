package com.risefit.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.risefit.app.MainViewModel
import com.risefit.app.data.HomeExercise
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.NumberField
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.LogTypes
import com.risefit.app.util.fa
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

private val intensityNames = listOf("سبک", "متوسط", "سنگین")
private val intensityFactors = listOf(0.7f, 1.0f, 1.3f)

private fun scaled(n: Int, factor: Float): Int = if (n <= 0) 0 else max(1, (n * factor).roundToInt())

private fun homeSummary(e: HomeExercise, factor: Float): String {
    val parts = ArrayList<String>()
    if (e.durationSec > 0) parts.add("${scaled(e.durationSec, factor).fa()} ثانیه")
    if (e.reps > 0) parts.add("${scaled(e.reps, factor).fa()} تکرار")
    parts.add("استراحت ${e.restSec.fa()} ثانیه")
    return parts.joinToString(" • ")
}

@Suppress("DEPRECATION")
private fun buzz(context: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        v?.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
    }
}

/** Countdown for an exercise or a rest period. */
@Composable
fun CountdownDialog(title: String, totalSeconds: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val total = totalSeconds.coerceAtLeast(1)
    var remaining by remember { mutableStateOf(total) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000L)
            remaining -= 1
        }
        if (running && remaining <= 0) {
            buzz(context)
            running = false
        }
    }

    val minutes = remaining / 60
    val seconds = remaining % 60
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { remaining.toFloat() / total },
                    modifier = Modifier.size(150.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = RF.CardHigh,
                    strokeWidth = 8.dp
                )
                Text(
                    "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}".fa(),
                    fontSize = 32.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary
                )
            }
        },
        confirmButton = {
            Row {
                if (remaining > 0) {
                    TextButton(onClick = { running = !running }) { Text(if (running) "توقف" else "ادامه") }
                }
                TextButton(onClick = { remaining = total; running = true }) { Text("از اول") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWorkoutTab(vm: MainViewModel) {
    val home by vm.home.collectAsState()
    val done by vm.homeDone.collectAsState()
    val logs by vm.logs.collectAsState()

    var group by remember { mutableStateOf("ABS") }
    var intensity by remember { mutableStateOf(1) }
    var editing by remember { mutableStateOf<HomeExercise?>(null) }
    var deleting by remember { mutableStateOf<HomeExercise?>(null) }
    var timer by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var logging by remember { mutableStateOf(false) }

    val today = LocalDate.now().toEpochDay()
    val doneIds = done.filter { it.epochDay == today }.map { it.exerciseId }.toSet()
    val list = home.filter { it.group == group }
    val doneCount = list.count { it.id in doneIds }
    val factor = intensityFactors[intensity.coerceIn(0, 2)]
    val tiredWarning = (1..3).all { d ->
        logs.any { it.epochDay == today - d && (it.type == LogTypes.ABS || it.type == LogTypes.HOME) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = group == "ABS", onClick = { group = "ABS" }, label = { Text("تمرین شکم") })
                FilterChip(selected = group == "BODY", onClick = { group = "BODY" }, label = { Text("تمرین کل بدن") })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DimText("شدت متناسب با توان تو (تعداد و زمان را کم و زیاد می‌کند)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intensityNames.forEachIndexed { i, n ->
                        FilterChip(selected = intensity == i, onClick = { intensity = i }, label = { Text(n) })
                    }
                }
            }
        }
        if (tiredWarning) {
            item {
                GlowCard(Modifier.fillMaxWidth(), accent = RF.Warning) {
                    Text("سه روز پشت سر هم تمرین کردی", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    DimText("آفرین! امروز استراحت یا یک تمرین سبک انتخاب خوبیه؛ عضله موقع ریکاوری قوی می‌شود.")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { editing = HomeExercise(group = group, name = "", howTo = "", reps = 10) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" افزودن حرکت")
                }
                Button(onClick = { logging = true }, enabled = doneCount > 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text(" ثبت جلسه")
                }
            }
        }
        item {
            DimText("امروز ${doneCount.fa()} از ${list.size.fa()} حرکت انجام شده. هفته‌ای ۲ تا ۳ جلسه شکم کافی است؛ استراحت هم بخشی از برنامه است.")
        }

        if (list.isEmpty()) {
            item { EmptyHint("حرکتی در این بخش نیست. با «افزودن حرکت» بساز.") }
        } else {
            items(list, key = { it.id }) { e ->
                HomeExerciseCard(
                    e = e, factor = factor, checked = e.id in doneIds,
                    onChecked = { vm.setHomeDone(e, it) },
                    onTimer = { timer = e.name to scaled(e.durationSec, factor) },
                    onRest = { timer = "استراحت" to e.restSec },
                    onEdit = { editing = e },
                    onDelete = { deleting = e }
                )
            }
        }
    }

    editing?.let { initial ->
        HomeEditDialog(initial, onSave = { editing = null; vm.saveHome(it) }, onDismiss = { editing = null })
    }
    deleting?.let { e ->
        ConfirmDialog(
            title = "حذف حرکت", text = "«${e.name}» حذف شود؟",
            onConfirm = { vm.deleteHome(e); deleting = null },
            onDismiss = { deleting = null }
        )
    }
    timer?.let { (title, seconds) ->
        CountdownDialog(title = title, totalSeconds = seconds, onDismiss = { timer = null })
    }
    if (logging) {
        val doneNames = list.filter { it.id in doneIds }.joinToString("، ") { it.name }
        SessionLogDialog(
            title = "ثبت جلسه",
            defaultNotes = doneNames,
            defaultDuration = 20,
            onConfirm = { duration, notes ->
                logging = false
                if (group == "ABS") vm.addLog(LogTypes.ABS, "تمرین شکم", duration, notes)
                else vm.addLog(LogTypes.HOME, "تمرین کل بدن", duration, notes)
            },
            onDismiss = { logging = false }
        )
    }
}

@Composable
private fun HomeExerciseCard(
    e: HomeExercise,
    factor: Float,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onTimer: () -> Unit,
    onRest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GlowCard(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Column(Modifier.weight(1f)) {
                Text(
                    e.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                    color = if (checked) RF.Success else RF.TextPrimary
                )
                DimText(homeSummary(e, factor))
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = RF.TextDim) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = RF.Danger) }
        }
        if (expanded) {
            if (e.howTo.isNotBlank()) {
                Text(e.howTo, color = RF.TextPrimary, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (e.durationSec > 0) {
                    OutlinedButton(onClick = onTimer) {
                        Icon(Icons.Rounded.Timer, contentDescription = null)
                        Text(" تایمر حرکت")
                    }
                }
                OutlinedButton(onClick = onRest) { Text("استراحت ${e.restSec.fa()} ثانیه") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeEditDialog(initial: HomeExercise, onSave: (HomeExercise) -> Unit, onDismiss: () -> Unit) {
    var group by remember { mutableStateOf(initial.group) }
    var name by remember { mutableStateOf(initial.name) }
    var howTo by remember { mutableStateOf(initial.howTo) }
    var reps by remember { mutableStateOf(if (initial.reps > 0) initial.reps.toString() else "") }
    var duration by remember { mutableStateOf(if (initial.durationSec > 0) initial.durationSec.toString() else "") }
    var rest by remember { mutableStateOf(initial.restSec.toString()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp), color = RF.Card,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 680.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial.id == 0) "حرکت جدید" else "ویرایش حرکت",
                    style = MaterialTheme.typography.titleLarge, color = RF.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = group == "ABS", onClick = { group = "ABS" }, label = { Text("شکم") })
                    FilterChip(selected = group == "BODY", onClick = { group = "BODY" }, label = { Text("کل بدن") })
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) },
                    label = { Text("نام حرکت") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = howTo, onValueChange = { howTo = it.take(600) },
                    label = { Text("روش اجرا") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("تکرار", reps, { reps = it }, Modifier.weight(1f))
                    NumberField("مدت (ثانیه)", duration, { duration = it }, Modifier.weight(1f))
                }
                NumberField("استراحت (ثانیه)", rest, { rest = it }, Modifier.fillMaxWidth())
                DimText("اگر حرکت زمان‌دار است (مثل پلانک) فقط مدت را پر کن؛ اگر تعدادی است فقط تکرار را.")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("لغو") }
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            var r = (reps.toIntOrNull() ?: 0).coerceIn(0, 500)
                            val d = (duration.toIntOrNull() ?: 0).coerceIn(0, 3600)
                            if (r == 0 && d == 0) r = 10
                            onSave(
                                initial.copy(
                                    group = group, name = name.trim(), howTo = howTo.trim(),
                                    reps = r, durationSec = d,
                                    restSec = (rest.toIntOrNull() ?: 45).coerceIn(0, 900)
                                )
                            )
                        }
                    ) { Text("ذخیره") }
                }
            }
        }
    }
}
