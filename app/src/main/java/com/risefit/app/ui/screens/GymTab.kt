package com.risefit.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.risefit.app.MainViewModel
import com.risefit.app.data.GymExercise
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.LocalImage
import com.risefit.app.ui.components.NumberField
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.LogTypes
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.fa
import java.time.LocalDate

private fun weightText(w: Float): String =
    (if (w % 1f == 0f) w.toInt().toString() else w.toString()).fa()

private fun exerciseSummary(e: GymExercise): String {
    val parts = ArrayList<String>()
    parts.add("${e.sets.fa()} ست × ${e.reps.fa()} تکرار")
    if (e.weightKg > 0f) parts.add("${weightText(e.weightKg)} کیلو")
    parts.add("استراحت ${e.restSec.fa()} ثانیه")
    return parts.joinToString(" • ")
}

private fun isVideo(context: Context, uri: String): Boolean =
    try {
        context.contentResolver.getType(Uri.parse(uri))?.startsWith("video") == true
    } catch (e: Exception) {
        false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTab(vm: MainViewModel) {
    val exercises by vm.gym.collectAsState()
    val logs by vm.logs.collectAsState()
    var day by remember { mutableStateOf(dayIndexOf(LocalDate.now())) }
    var editing by remember { mutableStateOf<GymExercise?>(null) }
    var deleting by remember { mutableStateOf<GymExercise?>(null) }
    var logging by remember { mutableStateOf(false) }

    val dayExercises = exercises.filter { it.dayIndex == day }
    val gymLogs = logs.filter { it.type == LogTypes.GYM }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Jalali.weekOrder.forEach { d ->
                    val has = exercises.any { it.dayIndex == d }
                    FilterChip(
                        selected = day == d, onClick = { day = d },
                        label = { Text(Jalali.weekdayNames[d] + if (has) " •" else "") }
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { editing = GymExercise(dayIndex = day, name = "") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" افزودن حرکت")
                }
                Button(
                    onClick = { logging = true },
                    enabled = dayExercises.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text(" ثبت تمرین")
                }
            }
        }

        if (dayExercises.isEmpty()) {
            item { EmptyHint("برای ${Jalali.weekdayNames[day]} حرکتی ثبت نشده؛ می‌تونه روز استراحت باشه.") }
        } else {
            items(dayExercises, key = { it.id }) { e ->
                ExerciseCard(e, onEdit = { editing = e }, onDelete = { deleting = e })
            }
        }

        item { SectionTitle("سابقه باشگاه", Modifier.padding(top = 8.dp)) }
        if (gymLogs.isEmpty()) {
            item { EmptyHint("هنوز جلسه‌ای ثبت نشده.") }
        } else {
            items(gymLogs, key = { it.id }) { l -> LogRow(l, onDelete = { vm.deleteLog(l) }) }
        }
    }

    editing?.let { initial ->
        GymEditDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveGym(saved)
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { e ->
        ConfirmDialog(
            title = "حذف حرکت", text = "«${e.name}» حذف شود؟",
            onConfirm = { vm.deleteGym(e); deleting = null },
            onDismiss = { deleting = null }
        )
    }

    if (logging) {
        val notes = dayExercises.joinToString("\n") { e ->
            e.name + ": " + exerciseSummary(e)
        }
        SessionLogDialog(
            title = "ثبت تمرین باشگاه",
            defaultNotes = notes,
            defaultDuration = 60,
            onConfirm = { duration, text ->
                logging = false
                vm.addLog(LogTypes.GYM, "باشگاه – " + Jalali.weekdayNames[day], duration, text)
            },
            onDismiss = { logging = false }
        )
    }
}

@Composable
private fun ExerciseCard(e: GymExercise, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    GlowCard(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.name, fontWeight = FontWeight.Bold, color = RF.TextPrimary, style = MaterialTheme.typography.titleMedium)
                DimText(exerciseSummary(e))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", tint = RF.TextDim)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = RF.Danger)
            }
        }
        if (expanded) {
            if (e.guide.isNotBlank()) {
                Text("راهنمای اجرا", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                Text(e.guide, color = RF.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            } else {
                DimText("برای این حرکت راهنمایی ننوشتی؛ با دکمه ویرایش اضافه کن.")
            }
            val media = e.mediaUri
            if (media != null) {
                if (isVideo(context, media)) {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(media), "video/*")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(" پخش ویدئوی آموزشی")
                    }
                } else {
                    LocalImage(
                        media,
                        Modifier.padding(top = 8.dp).fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GymEditDialog(initial: GymExercise, onSave: (GymExercise) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var dayIdx by remember { mutableStateOf(initial.dayIndex) }
    var sets by remember { mutableStateOf(initial.sets.toString()) }
    var reps by remember { mutableStateOf(initial.reps.toString()) }
    var weight by remember { mutableStateOf(if (initial.weightKg > 0f) initial.weightKg.toString().removeSuffix(".0") else "") }
    var rest by remember { mutableStateOf(initial.restSec.toString()) }
    var guide by remember { mutableStateOf(initial.guide) }
    var media by remember { mutableStateOf(initial.mediaUri) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            media = uri.toString()
        }
    }

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
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) },
                    label = { Text("نام حرکت") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("روز تمرین", color = RF.TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Jalali.weekOrder.forEach { d ->
                        FilterChip(selected = dayIdx == d, onClick = { dayIdx = d }, label = { Text(Jalali.weekdayNames[d]) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("ست", sets, { sets = it }, Modifier.weight(1f))
                    NumberField("تکرار", reps, { reps = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("وزنه (کیلو)", weight, { weight = it }, Modifier.weight(1f), decimal = true)
                    NumberField("استراحت (ثانیه)", rest, { rest = it }, Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = guide, onValueChange = { guide = it.take(600) },
                    label = { Text("راهنمای اجرای درست") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        try {
                            picker.launch(arrayOf("image/*", "video/*"))
                        } catch (_: Exception) {
                        }
                    }) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null)
                        Text(if (media == null) " عکس یا ویدئوی آموزشی" else " تغییر فایل")
                    }
                    if (media != null) {
                        TextButton(onClick = { media = null }) { Text("حذف فایل", color = RF.Danger) }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("لغو") }
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name.trim(), dayIndex = dayIdx,
                                    sets = (sets.toIntOrNull() ?: 3).coerceIn(1, 50),
                                    reps = (reps.toIntOrNull() ?: 10).coerceIn(1, 500),
                                    weightKg = weight.toFloatOrNull() ?: 0f,
                                    restSec = (rest.toIntOrNull() ?: 60).coerceIn(0, 900),
                                    guide = guide.trim(), mediaUri = media
                                )
                            )
                        }
                    ) { Text("ذخیره") }
                }
            }
        }
    }
}
