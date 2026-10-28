package com.risefit.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.risefit.app.ui.theme.RF
import com.risefit.app.util.Jalali
import com.risefit.app.util.fa
import com.risefit.app.util.formatTime
import com.risefit.app.util.maskHas
import com.risefit.app.util.toAsciiDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = RF.Card)
    val border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    val shaped = modifier.shadow(
        elevation = 8.dp, shape = shape,
        ambientColor = accent.copy(alpha = 0.30f), spotColor = accent.copy(alpha = 0.45f)
    )
    if (onClick != null) {
        Card(onClick = onClick, modifier = shaped, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = shaped, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun DimText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = RF.TextDim)
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = RF.TextDim, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "حذف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) { Text(confirmLabel, color = RF.Danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** Seven round day toggles in Persian week order (Saturday first). */
@Composable
fun DayChips(mask: Int, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val short = listOf("د", "س", "چ", "پ", "ج", "ش", "ی")
    val accent = MaterialTheme.colorScheme.primary
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (dayIndex in Jalali.weekOrder) {
            val on = maskHas(mask, dayIndex)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip38(on, accent)
                    .clickable { onToggle(dayIndex) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    short[dayIndex],
                    color = if (on) Color(0xFF12001F) else RF.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun Modifier.clip38(on: Boolean, accent: Color): Modifier =
    this.then(
        if (on) Modifier.background(accent, CircleShape)
        else Modifier.border(1.dp, RF.Outline, CircleShape)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogRF(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour, initialMinute, true)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(24.dp), color = RF.Card, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("انتخاب ساعت", style = MaterialTheme.typography.titleMedium, color = RF.TextPrimary)
                Box(Modifier.padding(vertical = 16.dp)) { TimePicker(state = state) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("لغو") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("تأیید") }
                }
            }
        }
    }
}

/** Button that shows a time and opens the time picker when tapped. */
@Composable
fun TimeField(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text("$label: ${formatTime(hour, minute)}")
    }
    if (open) {
        TimePickerDialogRF(
            initialHour = hour, initialMinute = minute,
            onConfirm = { h, m -> open = false; onChange(h, m) },
            onDismiss = { open = false }
        )
    }
}

private object PersianDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(AnnotatedString(text.text.fa()), OffsetMapping.Identity)
}

/** Numeric field: accepts Persian or Latin digits, keeps ASCII internally and shows Persian digits. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val ascii = raw.toAsciiDigits().replace('٫', '.').replace(',', '.')
            val cleaned = ascii.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(cleaned.take(6))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        visualTransformation = PersianDigitsTransformation,
        modifier = modifier
    )
}

/** Returns a function that asks for the notification permission (Android 13+) only when called. */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val request: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return request
}
