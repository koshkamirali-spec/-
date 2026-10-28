package com.risefit.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.risefit.app.PlusViewModel
import com.risefit.app.data.CustomReminder
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DayChips
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.components.rememberNotificationPermissionRequest
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.dayIndexOf
import com.risefit.app.util.describeDays
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import com.risefit.app.util.maskToggle
import java.time.LocalDate
import java.time.LocalTime

private const val ALL_DAYS = 0b1111111

private fun isOnce(r: CustomReminder): Boolean = (r.daysMask and ALL_DAYS) == 0

private fun appliesTo(r: CustomReminder, dayIndex: Int): Boolean =
    if (isOnce(r)) {
        r.onceEpochDay >= 0 && dayIndexOf(LocalDate.ofEpochDay(r.onceEpochDay)) == dayIndex
    } else {
        maskHas(r.daysMask, dayIndex)
    }

@Composable
fun RemindersScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val list by vm.reminders.collectAsState()
    val askNotifications = rememberNotificationPermissionRequest()
    var filterDay by remember { mutableStateOf(-1) } // -1 = all, otherwise day index (Monday = 0)
    var editing by remember { mutableStateOf<CustomReminder?>(null) }
    var deleting by remember { mutableStateOf<CustomReminder?>(null) }

    val shown = if (filterDay < 0) list else list.filter { appliesTo(it, filterDay) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "یادآورها و برنامه هفتگی",
                subtitle = "هر یادآور یک کلید روشن/خاموش دارد",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        editing = CustomReminder(
                            title = "", hour = (LocalTime.now().hour + 1) % 24, minute = 0
                        )
                    }) {
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
        if (shown.isEmpty()) {
            item {
                EmptyHint(
                    if (list.isEmpty()) "هنوز یادآوری نساخته‌ای. با دکمه + اولین را بساز."
                    else "برای این روز یادآوری نیست."
                )
            }
        } else {
            items(shown, key = { it.id }) { r ->
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = r }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.title, fontWeight = FontWeight.Bold,
                                color = if (r.enabled) RF.TextPrimary else RF.TextDim
                            )
                            val whenText = if (isOnce(r)) {
                                "یک‌بار • " + if (r.onceEpochDay >= 0) Jalali.format(LocalDate.ofEpochDay(r.onceEpochDay)) else ""
                            } else {
                                describeDays(r.daysMask)
                            }
                            DimText("${formatTime(r.hour, r.minute)} • $whenText")
                        }
                        Switch(
                            checked = r.enabled,
                            onCheckedChange = {
                                vm.toggleReminder(r, it)
                                if (it) askNotifications()
                            }
                        )
                    }
                }
            }
        }
        item {
            DimText(
                "یادآورها بدون اینترنت کار می‌کنند. اگر گوشی‌ات (مثلاً شیائومی یا سامسونگ) اعلان‌ها را دیر نشان می‌دهد، در «آلارم ← تست» محدودیت باتری را بردار.",
                Modifier.fillMaxWidth()
            )
        }
    }

    editing?.let { initial ->
        ReminderDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveReminder(saved)
                askNotifications()
            },
            onDelete = if (initial.id != 0) ({
                editing = null
                deleting = initial
            }) else null,
            onDismiss = { editing = null }
        )
    }

    deleting?.let { r ->
        ConfirmDialog(
            title = "حذف یادآور",
            text = "«${r.title}» حذف شود؟",
            onConfirm = {
                vm.deleteReminder(r)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ReminderDialog(
    initial: CustomReminder,
    onSave: (CustomReminder) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var mask by remember { mutableStateOf(initial.daysMask and ALL_DAYS) }

    FormDialog(if (initial.id == 0) "یادآور جدید" else "ویرایش یادآور", onDismiss) {
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(60) },
            label = { Text("متن یادآوری") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        TimeField("ساعت", hour, minute, { h, m -> hour = h; minute = m }, Modifier.fillMaxWidth())
        Text("تکرار", color = RF.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mask == 0, onClick = { mask = 0 }, label = { Text("یک‌بار") })
            FilterChip(selected = mask == ALL_DAYS, onClick = { mask = ALL_DAYS }, label = { Text("هر روز") })
        }
        DayChips(mask, { mask = maskToggle(mask, it) })
        DimText(
            if (mask == 0) "یک‌بار، در نزدیک‌ترین زمانی که ساعت به این وقت برسد."
            else "هر هفته در روزهای انتخاب‌شده."
        )
        DialogActions(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    initial.copy(
                        title = title.trim(), hour = hour, minute = minute,
                        daysMask = mask and ALL_DAYS, enabled = true
                    )
                )
            },
            canSave = title.isNotBlank(),
            onDelete = onDelete
        )
    }
}
