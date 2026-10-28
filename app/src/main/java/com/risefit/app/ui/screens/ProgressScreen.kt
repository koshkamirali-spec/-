package com.risefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.MainViewModel
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.GlowCard
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.SectionTitle
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.LogTypes
import com.risefit.app.util.Progress
import com.risefit.app.util.fa
import java.time.LocalDate

@Composable
fun ProgressScreen(vm: MainViewModel, onBack: () -> Unit) {
    val logs by vm.logs.collectAsState()
    val goals by vm.goals.collectAsState()
    val accent = MaterialTheme.colorScheme.primary

    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    val stats = Progress.compute(logs, todayDay)
    val badges = Progress.badges(stats, logs, goals)

    val currentWeekStart = todayDay - Jalali.weekColumn(today)
    val weeks = (7 downTo 0).map { i ->
        val start = currentWeekStart - 7L * i
        val count = logs.count { it.epochDay in start..(start + 6) }
        start to count
    }
    val maxCount = (weeks.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(title = "پیشرفت", subtitle = "هر جلسه یک قدم رو به جلو است", onBack = onBack) }

        // ---- level & points ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("سطح ${stats.level.fa()}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = accent)
                        DimText("${stats.totalPoints.fa()} امتیاز • ${stats.totalSessions.fa()} جلسه")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${stats.streak.fa()} روز", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                        DimText("استمرار سالم")
                    }
                }
                LinearProgressIndicator(
                    progress = { stats.pointsIntoLevel.toFloat() / Progress.POINTS_PER_LEVEL },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(CircleShape),
                    color = accent, trackColor = RF.CardHigh
                )
                DimText(
                    "${(Progress.POINTS_PER_LEVEL - stats.pointsIntoLevel).fa()} امتیاز تا سطح بعدی",
                    Modifier.padding(top = 6.dp)
                )
                DimText("استمرار سالم یعنی یک روز استراحت بین تمرین‌ها زنجیره را قطع نمی‌کند.", Modifier.padding(top = 4.dp))
            }
        }

        // ---- chart ----
        item { SectionTitle("تعداد تمرین در ۸ هفته اخیر") }
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().height(170.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeks.forEach { (start, count) ->
                        val j = Jalali.fromLocalDate(LocalDate.ofEpochDay(start))
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(count.fa(), color = RF.TextDim, style = MaterialTheme.typography.labelSmall)
                            Box(
                                Modifier.fillMaxWidth(0.6f)
                                    .height((110f * count / maxCount).dp.coerceAtLeast(3.dp))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (count > 0) accent else RF.CardHigh)
                            )
                            Text(
                                j.day.fa(), color = RF.TextDim, style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                DimText("عدد زیر هر ستون، روزِ شروع آن هفته است.", Modifier.padding(top = 8.dp))
            }
        }

        // ---- days & types ----
        item {
            GlowCard(Modifier.fillMaxWidth()) {
                Text("روزهای فعال", fontWeight = FontWeight.Bold, color = RF.TextPrimary)
                DimText("۷ روز اخیر: ${stats.activeDaysLast7.fa()} روز • این ماه: ${stats.activeDaysThisMonth.fa()} روز")
                LogTypes.all.forEach { t ->
                    val n = logs.count { it.type == t }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(LogTypes.label(t), Modifier.weight(0.35f), color = RF.TextPrimary)
                        LinearProgressIndicator(
                            progress = { if (logs.isEmpty()) 0f else n.toFloat() / logs.size },
                            modifier = Modifier.weight(0.5f).height(6.dp).clip(CircleShape),
                            color = accent, trackColor = RF.CardHigh
                        )
                        Text(n.fa(), Modifier.weight(0.15f), color = RF.TextDim, textAlign = TextAlign.End)
                    }
                }
            }
        }

        // ---- badges ----
        item { SectionTitle("نشان‌ها") }
        badges.forEach { b ->
            item(key = b.title) {
                GlowCard(Modifier.fillMaxWidth(), accent = if (b.unlocked) accent else RF.Outline) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (b.unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (b.unlocked) RF.Warning else RF.TextDim,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                b.title, fontWeight = FontWeight.Bold,
                                color = if (b.unlocked) RF.TextPrimary else RF.TextDim
                            )
                            DimText(b.description)
                        }
                    }
                }
            }
        }
        item {
            DimText(
                "اگر یک روز نتوانستی تمرین کنی مهم نیست؛ فردا دوباره شروع می‌کنی. استمرار یعنی برگشتن، نه بی‌نقص بودن.",
                Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
