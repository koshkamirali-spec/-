package com.risefit.app.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.risefit.app.PlusViewModel
import com.risefit.app.data.NoteItem
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.EmptyHint
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import java.time.LocalDate

private val moods = listOf("😄", "🙂", "😐", "😔", "😡")

@Suppress("DEPRECATION")
@Composable
fun JournalScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lockOn by vm.journalLock.collectAsState()
    val unlocked by vm.journalUnlocked.collectAsState()
    val notes by vm.notes.collectAsState()

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<NoteItem?>(null) }
    var deleting by remember { mutableStateOf<NoteItem?>(null) }

    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.unlockJournal()
    }
    fun authenticate() {
        val intent = if (keyguard.isDeviceSecure) {
            keyguard.createConfirmDeviceCredentialIntent("دفترچه RiseFit", "برای باز کردن دفترچه، قفل گوشی را وارد کن")
        } else {
            null
        }
        if (intent == null) vm.unlockJournal() else authLauncher.launch(intent)
    }

    // ---- locked ----
    if (lockOn && !unlocked) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Lock, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)
            )
            Text(
                "دفترچه قفل است", style = MaterialTheme.typography.titleLarge,
                color = RF.TextPrimary, modifier = Modifier.padding(top = 16.dp)
            )
            DimText(
                if (keyguard.isDeviceSecure) "با PIN، الگو یا اثر انگشت گوشی‌ات باز می‌شود."
                else "برای گوشی‌ات قفل صفحه تنظیم نشده؛ برای امنیت بیشتر یک قفل بگذار.",
                Modifier.padding(top = 8.dp)
            )
            Button(onClick = { authenticate() }, modifier = Modifier.padding(top = 20.dp)) { Text("باز کردن دفترچه") }
            androidx.compose.material3.TextButton(onClick = onBack) { Text("بازگشت") }
        }
        return
    }

    val q = query.trim()
    val shown = if (q.isEmpty()) notes else notes.filter {
        it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "دفترچه شخصی",
                subtitle = if (lockOn) "قفل فعال است" else "فقط روی همین گوشی",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        if (lockOn) {
                            vm.setJournalLock(false)
                        } else if (keyguard.isDeviceSecure) {
                            vm.setJournalLock(true)
                        } else {
                            Toast.makeText(
                                context,
                                "اول در تنظیمات گوشی یک قفل صفحه (PIN، الگو یا اثر انگشت) تنظیم کن",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Icon(
                            if (lockOn) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "قفل دفترچه", tint = RF.TextPrimary
                        )
                    }
                    IconButton(onClick = {
                        editing = NoteItem(epochDay = LocalDate.now().toEpochDay(), createdAt = 0L, title = "", body = "")
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "یادداشت جدید", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(60) },
                label = { Text("جست‌وجو در یادداشت‌ها") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        if (shown.isEmpty()) {
            item {
                EmptyHint(if (notes.isEmpty()) "هنوز چیزی ننوشته‌ای. با دکمه + اولین یادداشتت را بنویس." else "نتیجه‌ای پیدا نشد.")
            }
        } else {
            items(shown, key = { it.id }) { n ->
                GlowCard(Modifier.fillMaxWidth(), onClick = { editing = n }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (n.mood.isNotEmpty()) Text(n.mood, style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.title.ifBlank { n.body.lineSequence().firstOrNull().orEmpty() },
                                fontWeight = FontWeight.Bold, color = RF.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            DimText(Jalali.formatWithWeekday(LocalDate.ofEpochDay(n.epochDay)))
                        }
                    }
                    if (n.title.isNotBlank() && n.body.isNotBlank()) {
                        Text(
                            n.body, color = RF.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
        item {
            DimText(
                "یادداشت‌ها فقط روی همین گوشی ذخیره می‌شوند و هرگز برای RiseAI فرستاده نمی‌شوند. قفل فقط جلوی باز شدن دفترچه داخل برنامه را می‌گیرد؛ فایل پشتیبان رمزگذاری نیست، پس آن را جای امن نگه دار.",
                Modifier.fillMaxWidth()
            )
        }
    }

    editing?.let { initial ->
        NoteDialog(
            initial = initial,
            onSave = { saved ->
                editing = null
                vm.saveNote(saved)
            },
            onDelete = if (initial.id != 0) ({
                editing = null
                deleting = initial
            }) else null,
            onDismiss = { editing = null }
        )
    }

    deleting?.let { n ->
        ConfirmDialog(
            title = "حذف یادداشت",
            text = "این یادداشت برای همیشه حذف شود؟",
            onConfirm = {
                vm.deleteNote(n)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun NoteDialog(
    initial: NoteItem,
    onSave: (NoteItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }
    var mood by remember { mutableStateOf(initial.mood) }

    FormDialog(if (initial.id == 0) "یادداشت جدید" else "ویرایش یادداشت", onDismiss) {
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(80) },
            label = { Text("عنوان (اختیاری)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body, onValueChange = { body = it.take(5000) },
            label = { Text("امروز چه خبر بود؟") }, minLines = 5, modifier = Modifier.fillMaxWidth()
        )
        Text("حالت امروز", color = RF.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { m ->
                FilterChip(
                    selected = mood == m,
                    onClick = { mood = if (mood == m) "" else m },
                    label = { Text(m) }
                )
            }
        }
        DialogActions(
            onDismiss = onDismiss,
            onSave = { onSave(initial.copy(title = title.trim(), body = body.trim(), mood = mood)) },
            canSave = title.isNotBlank() || body.isNotBlank(),
            onDelete = onDelete
        )
    }
}
