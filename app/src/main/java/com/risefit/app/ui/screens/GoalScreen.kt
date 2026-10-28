package com.risefit.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.risefit.app.MainViewModel
import com.risefit.app.data.GoalStage
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.LocalImage
import com.risefit.app.ui.components.PRESET_COUNT
import com.risefit.app.ui.components.PresetImage
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.fa

private val stageNumbers = listOf(30, 60, 100)

@Composable
fun GoalScreen(vm: MainViewModel) {
    val goals by vm.goals.collectAsState()
    fun stageData(s: Int): GoalStage = goals.firstOrNull { it.stage == s } ?: GoalStage(stage = s)

    // which stage the gallery picker is currently serving, and the photo waiting for confirmation
    var pickTarget by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<Pair<Int, Uri>?>(null) }
    var showPresets by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pending = pickTarget to uri
    }
    fun pickPhoto(stage: Int) {
        pickTarget = stage
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val main = stageData(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(title = "هدف من", subtitle = "تصویر هدفت را ببین و قدم‌به‌قدم جلو برو") }

        // ---- main picture ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)).background(RF.CardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (main.photoUri != null) {
                        LocalImage(main.photoUri, Modifier.fillMaxSize())
                    } else {
                        Text("هنوز تصویری انتخاب نکردی", color = RF.TextDim)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickPhoto(0) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        Text(" گالری")
                    }
                    OutlinedButton(onClick = { showPresets = true }, modifier = Modifier.weight(1f)) {
                        Text("تصویر انگیزشی")
                    }
                }
                if (main.photoUri != null) {
                    TextButton(onClick = { clearing = 0 }) { Text("حذف تصویر هدف", color = RF.Danger) }
                }
                DimText("تصویرهای انگیزشی فقط الهام‌بخش هستند و پیش‌بینی قطعی تغییر بدن نیستند. هیچ تغییری روی عکس‌های تو اعمال نمی‌شود.")
            }
        }

        // ---- stages ----
        item { SectionTitle("مسیر ۳۰، ۶۰ و ۱۰۰ روزه") }
        stageNumbers.forEach { s ->
            item(key = "stage$s") {
                StageCard(
                    stage = s, data = stageData(s),
                    onPickPhoto = { pickPhoto(s) },
                    onSave = { goal, note, progress -> vm.saveStageText(s, goal, note, progress) },
                    onClear = { clearing = s }
                )
            }
        }

        // ---- comparison ----
        item { SectionTitle("مقایسه شخصی و خصوصی") }
        item {
            val withPhotos = stageNumbers.filter { stageData(it).photoUri != null }
            GlowCard(Modifier.fillMaxWidth()) {
                if (withPhotos.isEmpty()) {
                    EmptyHint("وقتی عکس مرحله‌ها را ثبت کنی، اینجا کنار هم دیده می‌شوند.")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        withPhotos.forEach { s ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                LocalImage(
                                    stageData(s).photoUri,
                                    Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                                )
                                DimText("روز ${s.fa()}")
                            }
                        }
                    }
                }
                DimText("عکس‌ها فقط روی همین گوشی ذخیره می‌شوند و جایی ارسال نمی‌شوند.")
            }
        }
    }

    // ---- confirm a picked photo before saving it ----
    pending?.let { (stage, uri) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (stage == 0) "این عکس هدف من باشد؟" else "این عکس برای روز ${stage.fa()} ثبت شود؟") },
            text = {
                LocalImage(
                    uri.toString(),
                    Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    vm.setStagePhoto(stage, uri)
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("انصراف") } }
        )
    }

    if (showPresets) {
        AlertDialog(
            onDismissRequest = { showPresets = false },
            title = { Text("یک تصویر انگیزشی انتخاب کن") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 0 until PRESET_COUNT) {
                        PresetImage(
                            i,
                            Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(14.dp)).clickable {
                                showPresets = false
                                vm.setStagePreset(0, i)
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPresets = false }) { Text("بستن") } }
        )
    }

    clearing?.let { s ->
        ConfirmDialog(
            title = "حذف اطلاعات",
            text = if (s == 0) "تصویر هدف حذف شود؟" else "عکس و یادداشت‌های روز ${s.fa()} برای همیشه حذف شوند؟",
            onConfirm = { vm.clearStage(s); clearing = null },
            onDismiss = { clearing = null }
        )
    }
}

@Composable
private fun StageCard(
    stage: Int,
    data: GoalStage,
    onPickPhoto: () -> Unit,
    onSave: (String, String, Int) -> Unit,
    onClear: () -> Unit
) {
    var goal by remember(data.goalText) { mutableStateOf(data.goalText) }
    var note by remember(data.note) { mutableStateOf(data.note) }
    var progress by remember(data.progress) { mutableStateOf(data.progress.toFloat()) }

    GlowCard(Modifier.fillMaxWidth()) {
        Text("روز ${stage.fa()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = RF.TextPrimary)
        Box(
            Modifier.padding(top = 8.dp).fillMaxWidth().height(170.dp)
                .clip(RoundedCornerShape(14.dp)).background(RF.CardHigh),
            contentAlignment = Alignment.Center
        ) {
            if (data.photoUri != null) {
                LocalImage(data.photoUri, Modifier.fillMaxSize())
            } else {
                Text("عکسی ثبت نشده", color = RF.TextDim)
            }
        }
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
            Text(if (data.photoUri == null) " ثبت عکس واقعی" else " تغییر عکس")
        }
        OutlinedTextField(
            value = goal, onValueChange = { goal = it.take(120) },
            label = { Text("هدف این مرحله") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = note, onValueChange = { note = it.take(400) },
            label = { Text("یادداشت") }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text("پیشرفت: ${progress.toInt().fa()}٪", color = RF.TextPrimary, modifier = Modifier.padding(top = 8.dp))
        Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(goal.trim(), note.trim(), progress.toInt()) }, modifier = Modifier.weight(1f)) {
                Text("ذخیره")
            }
            TextButton(onClick = onClear) { Text("حذف اطلاعات", color = RF.Danger) }
        }
    }
}
