package com.risefit.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.risefit.app.MainViewModel
import com.risefit.app.alarm.AlarmRingActivity
import com.risefit.app.alarm.AlarmScheduler
import com.risefit.app.alarm.AlarmService
import com.risefit.app.data.AlarmEntity
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DayChips
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.components.rememberNotificationPermissionRequest
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.describeDays
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskToggle

@Composable
fun AlarmsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val alarms by vm.alarms.collectAsState()
    var editing by remember { mutableStateOf<AlarmEntity?>(null) }
    var deleting by remember { mutableStateOf<AlarmEntity?>(null) }
    val askNotifications = rememberNotificationPermissionRequest()
    var tick by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val notificationsGranted = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val fullScreenAllowed = remember(tick) {
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
        } else true
    }

    val batteryOk = remember(tick) {
        try {
            context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (e: Exception) {
            true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "آلارم صبحگاهی",
                subtitle = "بیدار شو و روزت را با انرژی شروع کن",
                actions = {
                    IconButton(onClick = { editing = AlarmEntity(hour = 6, minute = 30) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "آلارم جدید", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (!notificationsGranted) {
            item {
                GlowCard(Modifier.fillMaxWidth(), accent = RF.Warning) {
                    Text("مجوز اعلان لازم است", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    DimText("برای نمایش آلارم روی صفحه و یادآوری‌ها اجازه نمایش اعلان را بده.")
                    TextButton(onClick = { askNotifications() }) { Text("دادن مجوز") }
                }
            }
        }
        if (!fullScreenAllowed) {
            item {
                GlowCard(Modifier.fillMaxWidth(), accent = RF.Warning) {
                    Text("نمایش روی صفحه قفل", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                    DimText("برای اینکه صفحه آلارم روی قفل‌صفحه باز شود، این مجوز را در تنظیمات فعال کن.")
                    TextButton(onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }) { Text("باز کردن تنظیمات") }
                }
            }
        }

        item {
            GlowCard(Modifier.fillMaxWidth(), accent = if (batteryOk) MaterialTheme.colorScheme.primary else RF.Warning) {
                Text("آلارم زنگ نمی‌زند؟ تست کن", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                DimText("اول «تست الان» را بزن. بعد «تست ۱۰ ثانیه‌ای» را بزن و صفحه را قفل کن؛ دقیقاً مثل آلارم واقعی رفتار می‌کند.")
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            AlarmService.startRinging(context, AlarmScheduler.TEST_ID)
                            try {
                                context.startActivity(
                                    Intent(context, AlarmRingActivity::class.java)
                                        .putExtra(AlarmScheduler.EXTRA_ALARM_ID, AlarmScheduler.TEST_ID)
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("تست الان") }
                    OutlinedButton(
                        onClick = {
                            askNotifications()
                            AlarmScheduler.scheduleTest(context, 10)
                            Toast.makeText(context, "۱۰ ثانیه دیگر زنگ می‌خورد؛ صفحه را قفل کن", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("تست ۱۰ ثانیه‌ای") }
                }
                if (!batteryOk) {
                    DimText("محدودیت باتری برای RiseFit فعال است و ممکن است آلارم را ساکت کند.", Modifier.padding(top = 8.dp))
                    TextButton(onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: Exception) {
                            }
                        }
                    }) { Text("برداشتن محدودیت باتری") }
                }
                TextButton(onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        )
                    } catch (_: Exception) {
                    }
                }) { Text("باز کردن تنظیمات برنامه") }
                DimText("روی شیائومی: «شروع خودکار» را روشن کن، باتری را روی «بدون محدودیت» بگذار و «نمایش روی قفل‌صفحه» و «پنجره‌های پاپ‌آپ در پس‌زمینه» را اجازه بده. روی سامسونگ: برنامه را از «Sleeping apps» دربیاور.")
            }
        }

        if (alarms.isEmpty()) {
            item { EmptyHint("هنوز آلارمی نساختی. با دکمه + یک آلارم بساز.") }
        } else {
            items(alarms, key = { it.id }) { a ->
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = a }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatTime(a.hour, a.minute), fontSize = 38.sp, fontWeight = FontWeight.Black,
                                color = if (a.enabled) RF.TextPrimary else RF.TextDim
                            )
                            if (a.label.isNotBlank()) Text(a.label, color = RF.TextPrimary)
                            DimText(
                                describeDays(a.daysMask) + " • چرت ${a.snoozeMinutes.fa()} دقیقه" +
                                    if (a.vibrate) " • لرزش" else ""
                            )
                        }
                        Switch(
                            checked = a.enabled,
                            onCheckedChange = {
                                vm.toggleAlarm(a, it)
                                if (it) askNotifications()
                            }
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { deleting = a }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = RF.Danger)
                        }
                    }
                }
            }
        }
        item {
            DimText(
                "نکته: روی بعضی گوشی‌ها (مثل شیائومی، سامسونگ و هوآوی) باید RiseFit را از «بهینه‌سازی باتری» مستثنا کنی تا آلارم همیشه به‌موقع به صدا دربیاید.",
                Modifier.padding(top = 8.dp)
            )
        }
    }

    editing?.let { initial ->
        AlarmEditDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                askNotifications()
                vm.saveAlarm(saved)
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { a ->
        ConfirmDialog(
            title = "حذف آلارم",
            text = "آلارم ساعت ${formatTime(a.hour, a.minute)} حذف شود؟",
            onConfirm = {
                vm.deleteAlarm(a)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Suppress("DEPRECATION")
private fun Intent.pickedRingtone(): Uri? =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }

private fun soundTitle(context: Context, uriString: String?): String {
    if (uriString.isNullOrBlank()) return "پیش‌فرض سیستم"
    return try {
        RingtoneManager.getRingtone(context, Uri.parse(uriString))?.getTitle(context) ?: "صدای انتخاب‌شده"
    } catch (e: Exception) {
        "صدای انتخاب‌شده"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditDialog(initial: AlarmEntity, onSave: (AlarmEntity) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var mask by remember { mutableStateOf(initial.daysMask) }
    var label by remember { mutableStateOf(initial.label) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }
    var snooze by remember { mutableStateOf(initial.snoozeMinutes) }
    var sound by remember { mutableStateOf(initial.soundUri) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            sound = result.data?.pickedRingtone()?.toString()
        }
    }

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
                    if (initial.id == 0) "آلارم جدید" else "ویرایش آلارم",
                    style = MaterialTheme.typography.titleLarge, color = RF.TextPrimary
                )
                TimeField("ساعت", hour, minute, { h, m -> hour = h; minute = m }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(40) },
                    label = { Text("برچسب (اختیاری)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("روزهای تکرار", color = RF.TextPrimary)
                DayChips(mask, { mask = maskToggle(mask, it) })
                DimText(if ((mask and 0b1111111) == 0) "هیچ روزی انتخاب نشده: آلارم فقط یک‌بار به صدا درمی‌آید." else describeDays(mask))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("لرزش", Modifier.weight(1f), color = RF.TextPrimary)
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Text("مدت چرت", color = RF.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { m ->
                        FilterChip(
                            selected = snooze == m, onClick = { snooze = m },
                            label = { Text("${m.fa()} دقیقه") }
                        )
                    }
                }

                Text("صدای آلارم", color = RF.TextPrimary)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای آلارم")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            val existing: Uri? = sound?.let { Uri.parse(it) }
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                        }
                        try {
                            picker.launch(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null)
                    Text(" " + soundTitle(context, sound))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("لغو") }
                    Button(onClick = {
                        onSave(
                            initial.copy(
                                hour = hour, minute = minute, daysMask = mask and 0b1111111,
                                label = label.trim(), enabled = true, vibrate = vibrate,
                                snoozeMinutes = snooze, soundUri = sound
                            )
                        )
                    }) { Text("ذخیره") }
                }
            }
        }
    }
}
