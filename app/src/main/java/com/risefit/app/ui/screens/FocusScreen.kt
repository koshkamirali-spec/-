package com.risefit.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.FocusPhase
import com.risefit.app.PlusViewModel
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.components.rememberNotificationPermissionRequest
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.fa
import com.risefit.app.util.todayEpochDay
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun FocusScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val s by vm.focusUi.collectAsState()
    val sessions by vm.focusSessions.collectAsState()
    val askNotifications = rememberNotificationPermissionRequest()

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.running, s.endAtMillis) {
        while (s.running) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    // keep the screen awake while a session runs so the countdown stays in front of you
    val view = LocalView.current
    DisposableEffect(s.running) {
        view.keepScreenOn = s.running
        onDispose { view.keepScreenOn = false }
    }

    val remainingMs = if (s.running) (s.endAtMillis - now).coerceAtLeast(0L) else s.remainingMs
    val seconds = ((remainingMs + 999L) / 1000L).toInt()
    val fraction = if (s.phaseMs <= 0L) 0f else 1f - remainingMs.toFloat() / s.phaseMs.toFloat()
    val clock = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}".fa()
    val isFocus = s.phase == FocusPhase.FOCUS
    val elapsedMs = s.phaseMs - remainingMs

    val today = todayEpochDay()
    val todays = sessions.filter { it.epochDay == today }
    val weekMinutes = sessions.filter { it.epochDay in (today - 6)..today }.sumOf { it.minutes }
    val accent = if (isFocus) MaterialTheme.colorScheme.primary else RF.Success

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(title = "تایمر تمرکز", subtitle = "تمرکز کامل، بعد استراحت", onBack = onBack)
        }
        item {
            GlowCard(Modifier.fillMaxWidth(), accent = accent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = s.focusMin == 25, enabled = !s.running,
                        onClick = { vm.setFocusLength(25, 5) }, label = { Text("${25.fa()} دقیقه") }
                    )
                    FilterChip(
                        selected = s.focusMin == 45, enabled = !s.running,
                        onClick = { vm.setFocusLength(45, 10) }, label = { Text("${45.fa()} دقیقه") }
                    )
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 10.dp,
                        color = accent,
                        trackColor = RF.CardHigh
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(clock, fontSize = 48.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary)
                        DimText(if (isFocus) "زمان تمرکز" else "زمان استراحت")
                    }
                }
                if (isFocus) {
                    OutlinedTextField(
                        value = s.label, onValueChange = { vm.setFocusLabel(it) },
                        label = { Text("روی چه چیزی تمرکز می‌کنی؟ (اختیاری)") },
                        singleLine = true, enabled = !s.running, modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (s.running) {
                        Button(onClick = { vm.pauseFocus() }, modifier = Modifier.weight(1f)) { Text("توقف") }
                    } else {
                        Button(
                            onClick = {
                                askNotifications()
                                vm.startFocus()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (s.remainingMs < s.phaseMs) "ادامه" else "شروع") }
                    }
                    OutlinedButton(onClick = { vm.resetFocus() }, modifier = Modifier.weight(1f)) { Text("از اول") }
                }
                if (isFocus && elapsedMs >= 60_000L) {
                    TextButton(onClick = { vm.finishFocusEarly() }) { Text("همین‌جا تمام کن و زمانی که گذشته را ثبت کن") }
                }
                if (!isFocus) {
                    TextButton(onClick = { vm.resetFocus() }) { Text("استراحت لازم نیست، جلسه بعد") }
                }
            }
        }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(todays.size.fa(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary)
                        DimText("جلسه امروز")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            todays.sumOf { it.minutes }.fa(), fontSize = 24.sp,
                            fontWeight = FontWeight.Black, color = RF.TextPrimary
                        )
                        DimText("دقیقه امروز")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(weekMinutes.fa(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = RF.TextPrimary)
                        DimText("دقیقه در ۷ روز")
                    }
                }
            }
        }
        if (sessions.isNotEmpty()) {
            item { SectionTitle("جلسه‌های اخیر") }
            items(sessions.take(8), key = { it.id }) { f ->
                GlowCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (f.label.isBlank()) "جلسه تمرکز" else f.label,
                                fontWeight = FontWeight.Bold, color = RF.TextPrimary
                            )
                            DimText("${f.minutes.fa()} دقیقه • ${Jalali.format(LocalDate.ofEpochDay(f.epochDay))}")
                        }
                        IconButton(onClick = { vm.deleteFocusSession(f) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "حذف", tint = RF.TextDim)
                        }
                    }
                }
            }
        }
    }
}
