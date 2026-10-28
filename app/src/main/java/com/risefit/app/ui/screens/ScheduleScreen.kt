package com.risefit.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.risefit.app.data.ScheduleItem
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DayChips
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.components.categoryIcon
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Categories
import com.risefit.app.util.Jalali
import com.risefit.app.util.describeDays
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskCount
import com.risefit.app.util.maskHas
import com.risefit.app.util.maskToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.schedule.collectAsState()
    var filterDay by remember { mutableStateOf(-1) } // -1 = all days, else day index (Monday = 0)
    var editing by remember { mutableStateOf<ScheduleItem?>(null) }
    var deleting by remember { mutableStateOf<ScheduleItem?>(null) }

    val shown = if (filterDay < 0) items else items.filter { maskHas(it.daysMask, filterDay) }
    val restDay = filterDay >= 0 && shown.none { Categories.isTraining(it.category) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "برنامه روزانه من",
                subtitle = "ساعت‌ها را هر طور دوست داری تغییر بده",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { editing = ScheduleItem(title = "", hour = 18, minute = 0) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "افزودن", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterDay < 0, onClick = { filterDay = -1 }, label = { Text("همه") })
                Jalali.weekOrder.forEach { d ->
                    FilterChip(
                        selected = filterDay == d, onClick = { filterDay = d },
                        label = { Text(Jalali.weekdayNames[d]) }
                    )
                }
            }
        }
        if (restDay) {
            item {
                GlowCard(Modifier.fillMaxWidth(), accent = RF.Success) {
                    Text("این روز، روز استراحت است", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    DimText("تمرین سنگین برنامه‌ریزی نشده. استراحت و خواب کافی را جدی بگیر.")
                }
            }
        }
        if (shown.isEmpty()) {
            item { EmptyHint("موردی ثبت نشده. با دکمه + اضافه کن.") }
        } else {
            items(shown, key = { it.id }) { s ->
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = s }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(categoryIcon(s.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold, color = if (s.enabled) RF.TextPrimary else RF.TextDim)
                            DimText("${formatTime(s.hour, s.minute)} • ${describeDays(s.daysMask)} • ${Categories.label(s.category)}")
                        }
                        Switch(checked = s.enabled, onCheckedChange = { vm.saveScheduleItem(s.copy(enabled = it)) })
                    }
                }
            }
        }
    }

    editing?.let { initial ->
        ScheduleEditDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveScheduleItem(saved)
            },
            onDelete = if (initial.id != 0) ({
                editing = null
                deleting = initial
            }) else null,
            onDismiss = { editing = null }
        )
    }

    deleting?.let { s ->
        ConfirmDialog(
            title = "حذف از برنامه",
            text = "«${s.title}» از برنامه حذف شود؟",
            onConfirm = {
                vm.deleteScheduleItem(s)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditDialog(
    initial: ScheduleItem,
    onSave: (ScheduleItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var category by remember { mutableStateOf(initial.category) }
    var mask by remember { mutableStateOf(initial.daysMask) }

    val canSave = title.isNotBlank() && maskCount(mask) > 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp), color = RF.Card,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 640.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (initial.id == 0) "مورد جدید" else "ویرایش مورد",
                    style = MaterialTheme.typography.titleLarge, color = RF.TextPrimary
                )
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(60) },
                    label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                TimeField("ساعت", hour, minute, { h, m -> hour = h; minute = m }, Modifier.fillMaxWidth())

                Text("دسته", color = RF.TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categories.all.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(Categories.label(c)) })
                    }
                }

                Text("روزها", color = RF.TextPrimary)
                DayChips(mask, { mask = maskToggle(mask, it) })
                if (maskCount(mask) == 0) {
                    Text("حداقل یک روز را انتخاب کن.", color = RF.Warning, style = MaterialTheme.typography.bodySmall)
                } else if (Categories.isTraining(category) && maskCount(mask) == 7) {
                    Text(
                        "تمرین هر روز هفته؟ حداقل یک روز استراحت بگذار تا بدنت ریکاوری کند.",
                        color = RF.Warning, style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("حذف", color = RF.Danger) }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("لغو") }
                        Button(
                            enabled = canSave,
                            onClick = {
                                onSave(
                                    initial.copy(
                                        title = title.trim(), hour = hour, minute = minute,
                                        category = category, daysMask = mask and 0b1111111
                                    )
                                )
                            }
                        ) { Text("ذخیره") }
                    }
                }
            }
        }
    }
}
