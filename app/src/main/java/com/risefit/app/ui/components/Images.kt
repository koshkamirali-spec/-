package com.risefit.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risefit.app.ui.theme.RF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max

private class Preset(val colors: List<Color>, val icon: ImageVector, val text: String)

private val presets = listOf(
    Preset(listOf(Color(0xFF3B0A6B), Color(0xFFB26BFF)), Icons.Rounded.EmojiEvents, "قهرمان شدن از همین لحظه شروع می‌شه"),
    Preset(listOf(Color(0xFF062B4F), Color(0xFF4DA3FF)), Icons.Rounded.Bolt, "قدم به قدم، قوی‌تر از دیروز"),
    Preset(listOf(Color(0xFF053B22), Color(0xFF39E58C)), Icons.Rounded.FitnessCenter, "استمرار، راز اصلی پیشرفت است"),
    Preset(listOf(Color(0xFF4A0A2B), Color(0xFFFF5FA2)), Icons.Rounded.LocalFireDepartment, "نسخه بهتر خودت منتظرته")
)

const val PRESET_COUNT = 4

/** Built-in inspirational picture (no photo needed). Purely motivational, not a prediction. */
@Composable
fun PresetImage(index: Int, modifier: Modifier = Modifier) {
    val p = presets[index.coerceIn(0, presets.lastIndex)]
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF0A0A0F), p.colors[0], p.colors[1]))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(p.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            Text(
                p.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Shows a private photo (file path or content uri) or a built-in "preset:N" picture. */
@Composable
fun LocalImage(source: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    if (source != null && source.startsWith("preset:")) {
        PresetImage(source.removePrefix("preset:").toIntOrNull() ?: 0, modifier)
    } else {
        val context = LocalContext.current
        val bitmap: State<ImageBitmap?> = produceState<ImageBitmap?>(null, source) {
            value = if (source == null) null else withContext(Dispatchers.IO) {
                decodeSampled(context, source, 1280)?.asImageBitmap()
            }
        }
        val bmp = bitmap.value
        if (bmp != null) {
            Image(bitmap = bmp, contentDescription = null, modifier = modifier, contentScale = contentScale)
        } else {
            Box(modifier.background(RF.CardHigh))
        }
    }
}

private fun openSource(context: Context, source: String): InputStream? =
    try {
        if (source.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(source))
        } else {
            FileInputStream(source)
        }
    } catch (e: Exception) {
        null
    }

/** Decodes a downscaled bitmap and applies the EXIF rotation. The file itself is never modified. */
fun decodeSampled(context: Context, source: String, maxSide: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openSource(context, source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = openSource(context, source)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val orientation = try {
            openSource(context, source)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
