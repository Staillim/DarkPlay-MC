package com.darkplaymc.app.presentation.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberArtworkBackgroundColor(artworkUri: Any?): Color {
    val context = LocalContext.current
    var backgroundColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(artworkUri) {
        backgroundColor = if (artworkUri == null) {
            Color.Black
        } else {
            extractArtworkBackgroundColor(context, artworkUri) ?: Color.Black
        }
    }

    return backgroundColor
}

private suspend fun extractArtworkBackgroundColor(
    context: Context,
    artworkUri: Any
): Color? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(artworkUri)
        .allowHardware(false)
        .size(64)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return@withContext null
    val bitmap = result.drawable.toBitmap(
        width = 64,
        height = 64,
        config = Bitmap.Config.ARGB_8888
    )

    bitmap.toIntenseDarkColor()
}

private fun Bitmap.toIntenseDarkColor(): Color? {
    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var sampleCount = 0
    val hsl = FloatArray(3)
    val step = maxOf(1, minOf(width, height) / 24)

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            if (AndroidColor.alpha(pixel) >= 128) {
                val red = AndroidColor.red(pixel)
                val green = AndroidColor.green(pixel)
                val blue = AndroidColor.blue(pixel)
                ColorUtils.RGBToHSL(red, green, blue, hsl)

                if (hsl[1] >= 0.08f && hsl[2] in 0.05f..0.92f) {
                    redSum += red
                    greenSum += green
                    blueSum += blue
                    sampleCount++
                }
            }
            x += step
        }
        y += step
    }

    if (sampleCount == 0) return null

    ColorUtils.RGBToHSL(
        (redSum / sampleCount).toInt(),
        (greenSum / sampleCount).toInt(),
        (blueSum / sampleCount).toInt(),
        hsl
    )
    hsl[1] = hsl[1].coerceIn(0.35f, 0.75f)
    hsl[2] = hsl[2].coerceIn(0.18f, 0.22f)

    return Color(ColorUtils.HSLToColor(hsl))
}
