package com.risefit.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.risefit.app.MainViewModel
import com.risefit.app.PlusViewModel
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.components.TimeField
import com.risefit.app.ui.components.rememberNotificationPermissionRequest
import com.risefit.app.ui.theme.RF
import com.risefit.app.ui.theme.accentColors
import com.risefit.app.ui.theme.accentNames

@Composable
fun SettingsScreen(vm: MainViewModel, plus: PlusViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val askNotifications = rememberNotificationPermissionRequest()

    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmPhotos by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) confirmRestore = uri }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(title = "تنظیمات", subtitle = "همه چیز روی همین گوشی می‌ماند", onBack = onBack) }

        // ---- theme ----
        item { SectionTitle("رنگ نئونی برنامه") }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    accentColors.forEachIndexed { i, c ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(46.dp).background(c, CircleShape)
                                    .let { if (settings.accentIndex == i) it.border(3.dp, Color.White, CircleShape) else it }
                                    .clickable { vm.updateSettings(settings.copy(accentIndex = i)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.accentIndex == i) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF12001F))
                                }
                            }
                            DimText(accentNames[i].substringBefore(' '), Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        // ---- reminders ----
        item { SectionTitle("یادآوری‌ها") }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                SwitchRow("یادآوری برنامه‌های تمرین", settings.activityReminders) {
                    vm.updateSettings(settings.copy(activityReminders = it))
                    if (it) askNotifications()
                }
                SwitchRow("یادآوری نوشیدن آب", settings.waterReminders) {
                    vm.updateSettings(settings.copy(waterReminders = it))
                    if (it) askNotifications()
                }
                SwitchRow("یادآوری آرام‌سازی و خواب", settings.sleepReminder) {
                    vm.updateSettings(settings.copy(sleepReminder = it))
                    if (it) askNotifications()
                }
                if (settings.sleepReminder) {
                    TimeField(
                        "ساعت یادآوری خواب", settings.sleepHour, settings.sleepMinute,
                        { h, m -> vm.updateSettings(settings.copy(sleepHour = h, sleepMinute = m)) },
                        Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                DimText(
                    "ساعت‌های هر برنامه را از «برنامه روزانه من» عوض کن. یادآوری‌ها بدون نیاز به اینترنت کار می‌کنند و ممکن است چند دقیقه دیرتر برسند.",
                    Modifier.padding(top = 8.dp)
                )
            }
        }

        // ---- backup ----
        item { SectionTitle("پشتیبان‌گیری و بازیابی") }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                DimText("یک فایل پشتیبان از آلارم‌ها، برنامه‌ها، سابقه تمرین، عکس‌های هدف، دفترچه برنامه، عادت‌ها، هدف‌ها، یادآورها و یادداشت‌های شخصی می‌سازد. فایل رمزگذاری نیست؛ آن را جای امن نگه دار.")
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("risefit-backup.json") }, modifier = Modifier.weight(1f)) {
                        Text("پشتیبان‌گیری")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("بازیابی") }
                }
                DimText("ویدئو و عکس‌های آموزشی حرکت‌های باشگاه داخل پشتیبان نیستند، فقط پیوندشان ذخیره می‌شود.", Modifier.padding(top = 8.dp))
            }
        }

        // ---- delete ----
        item { SectionTitle("حذف اطلاعات", ) }
        item {
            GlowCard(Modifier.fillMaxWidth(), accent = RF.Danger) {
                OutlinedButton(onClick = { confirmPhotos = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("حذف عکس‌ها و هدف‌ها", color = RF.Danger)
                }
                OutlinedButton(
                    onClick = { confirmAll = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("حذف همه اطلاعات برنامه", color = RF.Danger) }
            }
        }

        // ---- privacy ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Text("حریم خصوصی", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                DimText("RiseFit حساب کاربری نمی‌خواهد و همه بخش‌ها بدون اینترنت کار می‌کنند. اطلاعات و عکس‌های تو فقط روی همین گوشی ذخیره می‌شوند. فقط اگر خودت در RiseAI آدرس سرور بگذاری، سؤال‌های آزادی که همان‌جا می‌نویسی به آن سرور فرستاده می‌شود؛ برنامه، عادت‌ها و دفترچه‌ات هیچ‌وقت فرستاده نمی‌شوند. مجوزها (اعلان، نمایش آلارم روی قفل‌صفحه) فقط وقتی لازم شوند پرسیده می‌شوند.")
            }
        }
        item { DimText("RiseFit نسخه ۱٫۱", Modifier.padding(bottom = 8.dp)) }
    }

    confirmRestore?.let { uri ->
        ConfirmDialog(
            title = "بازیابی اطلاعات",
            text = "با بازیابی، همه اطلاعات فعلی با محتوای فایل پشتیبان جایگزین می‌شود. ادامه می‌دهی؟",
            confirmLabel = "بازیابی",
            onConfirm = { vm.importBackup(uri); confirmRestore = null },
            onDismiss = { confirmRestore = null }
        )
    }
    if (confirmPhotos) {
        ConfirmDialog(
            title = "حذف عکس‌ها و هدف‌ها",
            text = "همه عکس‌ها، تصویر هدف و یادداشت‌های مرحله‌های ۳۰، ۶۰ و ۱۰۰ برای همیشه حذف می‌شوند.",
            onConfirm = { vm.wipePhotosAndGoals(); confirmPhotos = false },
            onDismiss = { confirmPhotos = false }
        )
    }
    if (confirmAll) {
        ConfirmDialog(
            title = "حذف همه اطلاعات",
            text = "آلارم‌ها، برنامه‌ها، سابقه تمرین، عکس‌ها، عادت‌ها، هدف‌ها، یادآورها، یادداشت‌ها و تنظیمات پاک می‌شوند. این کار قابل بازگشت نیست.",
            onConfirm = { vm.wipeEverything(); plus.resetPrefs(); confirmAll = false },
            onDismiss = { confirmAll = false }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = RF.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
