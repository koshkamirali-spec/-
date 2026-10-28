package com.risefit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.risefit.app.ui.theme.RF

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت", tint = RF.TextPrimary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = RF.TextPrimary
            )
            if (subtitle != null) DimText(subtitle)
        }
        actions()
    }
}

fun categoryIcon(category: String): ImageVector = when (category) {
    "WAKE" -> Icons.Rounded.WbSunny
    "SCHOOL" -> Icons.Rounded.School
    "GYM" -> Icons.Rounded.FitnessCenter
    "HOME" -> Icons.Rounded.DirectionsRun
    "FUTSAL" -> Icons.Rounded.SportsSoccer
    "MEAL" -> Icons.Rounded.Restaurant
    "SLEEP" -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.Event
}

fun logTypeIcon(type: String): ImageVector = when (type) {
    "GYM" -> Icons.Rounded.FitnessCenter
    "FUTSAL" -> Icons.Rounded.SportsSoccer
    else -> Icons.Rounded.DirectionsRun
}
