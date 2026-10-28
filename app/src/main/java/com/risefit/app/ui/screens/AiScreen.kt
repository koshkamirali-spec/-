package com.risefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.risefit.app.ChatMsg
import com.risefit.app.PlusViewModel
import com.risefit.app.ai.AiIntent
import com.risefit.app.ai.LocalAssistant
import com.risefit.app.ui.components.DialogActions
import com.risefit.app.ui.components.DimText
import com.risefit.app.ui.components.FormDialog
import com.risefit.app.ui.components.ScreenHeader
import com.risefit.app.ui.components.rememberNotificationPermissionRequest
import com.risefit.app.ui.theme.RF

private val quick = listOf("برنامه امروز", "عادت‌ها", "تمرکز", "هدف‌ها", "انگیزه", "راهنما")

@Composable
fun AiScreen(vm: PlusViewModel, onBack: () -> Unit) {
    val chat by vm.chat.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val aiUrl by vm.aiUrl.collectAsState()
    val aiToken by vm.aiToken.collectAsState()
    val askNotifications = rememberNotificationPermissionRequest()

    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val count = chat.size + if (thinking) 1 else 0
    LaunchedEffect(count) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        if (LocalAssistant.parse(t) is AiIntent.AddReminder) askNotifications()
        vm.ask(t)
        input = ""
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            title = "RiseAI",
            subtitle = if (aiUrl.isBlank()) "دستیار آفلاین روی گوشی تو" else "متصل به سرور خودت برای سؤال‌های آزاد",
            onBack = onBack,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
            actions = {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات RiseAI", tint = RF.TextPrimary)
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chat) { m -> Bubble(m) }
            if (thinking) {
                item { Bubble(ChatMsg(false, "در حال فکر کردن...")) }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quick.forEach { q ->
                FilterChip(selected = false, onClick = { send(q) }, label = { Text(q) })
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(500) },
                placeholder = { Text("بنویس...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
                modifier = Modifier.weight(1f)
            )
            IconButton(enabled = input.isNotBlank() && !thinking, onClick = { send(input) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send, contentDescription = "ارسال",
                    tint = if (input.isNotBlank() && !thinking) MaterialTheme.colorScheme.primary else RF.TextDim
                )
            }
        }
    }

    if (showSettings) {
        AiSettingsDialog(
            url = aiUrl,
            token = aiToken,
            onSave = { u, t ->
                showSettings = false
                vm.saveAiSettings(u, t)
            },
            onClear = {
                vm.clearChat()
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun Bubble(m: ChatMsg) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.Start else Arrangement.End
    ) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .background(if (m.fromUser) accent else RF.CardHigh, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                m.text,
                color = if (m.fromUser) Color(0xFF12001F) else RF.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AiSettingsDialog(
    url: String,
    token: String,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var u by remember { mutableStateOf(url) }
    var t by remember { mutableStateOf(token) }
    FormDialog("تنظیمات RiseAI", onDismiss) {
        DimText(
            "بدون این تنظیم، RiseAI فقط دستورهای داخلی را بدون اینترنت انجام می‌دهد. " +
                "برای جواب دادن به سؤال‌های آزاد، آدرس سرور واسط خودت را وارد کن. " +
                "کلید هوش مصنوعی هرگز داخل این برنامه نیست و روی همان سرور می‌ماند؛ روش ساختن سرور در راهنمای README آمده است."
        )
        OutlinedTextField(
            value = u, onValueChange = { u = it.trim().take(300) },
            label = { Text("آدرس سرور (https://...)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = t, onValueChange = { t = it.trim().take(100) },
            label = { Text("رمز دسترسی سرور (اختیاری)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        DimText("فقط سؤال‌های آزادی که می‌نویسی به سرور می‌رود. برنامه، عادت‌ها و دفترچه‌ات هیچ‌وقت فرستاده نمی‌شوند.")
        TextButton(onClick = onClear) { Text("پاک کردن گفتگو") }
        DialogActions(onDismiss = onDismiss, onSave = { onSave(u, t) }, canSave = true)
    }
}
