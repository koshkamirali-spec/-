package com.risefit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import com.risefit.app.MainViewModel
import com.risefit.app.data.WorkoutLog
import com.risefit.app.ui.components.ConfirmDialog
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.NumberField
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.logTypeIcon
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.LogTypes
import com.risefit.app.util.fa
import java.time.LocalDate

@Composable
fun WorkoutScreen(vm: MainViewModel) {
    val tab by vm.workoutTab.collectAsState()
    val titles = listOf("باشگاه", "شکم و خانه", "فوتسال")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "تمرین",
            subtitle = "برنامه‌ات را انجام بده، ثبت کن، استراحت را هم فراموش نکن",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        TabRow(
            selectedTabIndex = tab.coerceIn(0, 2),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { vm.workoutTab.value = i }, text = { Text(t) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> GymTab(vm)
                1 -> HomeWorkoutTab(vm)
                else -> FutsalTab(vm)
            }
        }
    }
}

/** Dialog used by every tab to save a finished session into the history. */
@Composable
fun SessionLogDialog(
    title: String,
    defaultNotes: String,
    defaultDuration: Int,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var duration by remember { mutableStateOf(if (defaultDuration > 0) defaultDuration.toString() else "") }
    var notes by remember { mutableStateOf(defaultNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("مدت (دقیقه)", duration, { duration = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it.take(500) },
                    label = { Text("یادداشت") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration.toIntOrNull() ?: 0, notes.trim()) }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}

/** One history row with delete confirmation. */
@Composable
fun LogRow(log: WorkoutLog, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    GlowCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                logTypeIcon(log.type), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    log.title.ifBlank { LogTypes.label(log.type) },
                    fontWeight = FontWeight.Bold, color = RF.TextPrimary
                )
                val duration = if (log.durationMin > 0) " • ${log.durationMin.fa()} دقیقه" else ""
                DimText(Jalali.format(LocalDate.ofEpochDay(log.epochDay)) + duration)
            }
            IconButton(onClick = { confirm = true }) {
                Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = RF.Danger)
            }
        }
        if (log.notes.isNotBlank()) {
            Text(log.notes, color = RF.TextDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "حذف از سابقه", text = "این جلسه از سابقه حذف شود؟",
            onConfirm = { confirm = false; onDelete() },
            onDismiss = { confirm = false }
        )
    }
}
