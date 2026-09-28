package com.lockaltime.feature.home.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.floor

/**
 * Always black on white with a white border (the "quiet zone"), whatever the theme: scanners
 * expect dark modules on a light background.
 */
@Composable
internal fun QrCode(content: String, contentDescription: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        // Size 0 gives one pixel per module; the Canvas scales it up.
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 0,
            ),
        )
    }
    Canvas(
        modifier
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        // Whole pixels per module, so neighbouring modules don't leave hairline gaps.
        val moduleSize = floor(size.minDimension / matrix.width).coerceAtLeast(1f)
        val origin = (size.minDimension - moduleSize * matrix.width) / 2
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(origin + x * moduleSize, origin + y * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}
