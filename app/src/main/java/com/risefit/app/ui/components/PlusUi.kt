package com.risefit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.todayEpochDay
import java.time.LocalDate

/** Scrollable form card in a dialog, same look as the other edit dialogs. */
@Composable
fun FormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp), color = RF.Card,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 640.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = RF.TextPrimary)
                content()
            }
        }
    }
}

/** Delete / cancel / save row for [FormDialog]. */
@Composable
fun DialogActions(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    onDelete: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onDelete != null) {
            TextButton(onClick = onDelete) { Text("حذف", color = RF.Danger) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("لغو") }
            Button(enabled = canSave, onClick = onSave) { Text("ذخیره") }
        }
    }
}

@Composable
fun PercentBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        color = color,
        trackColor = RF.CardHigh
    )
}

/** Previous / next day arrows with the Persian date in the middle. */
@Composable
fun DayNavigator(day: Long, onChange: (Long) -> Unit) {
    val date = LocalDate.ofEpochDay(day)
    val isToday = day == todayEpochDay()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(day - 1) }) {
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "روز قبل", tint = RF.TextPrimary)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Jalali.weekdayName(date.dayOfWeek) + if (isToday) " (امروز)" else "",
                fontWeight = FontWeight.Bold, color = RF.TextPrimary
            )
            DimText(Jalali.format(date))
        }
        if (!isToday) {
            TextButton(onClick = { onChange(todayEpochDay()) }) { Text("امروز") }
        }
        IconButton(onClick = { onChange(day + 1) }) {
            Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "روز بعد", tint = RF.TextPrimary)
        }
    }
}
