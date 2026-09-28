package com.lockaltime.feature.home.impl

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.lockaltime.core.designsystem.icon.LockalTimeIcons
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * A full-screen camera view that reports the first QR code it reads. Decoding runs in the app with
 * ZXing, so it works on phones without Google Play services. The caller must hold the camera
 * permission before showing it.
 *
 * @param onScanned the raw QR content, which may not be an invite.
 * @param onFailed the camera couldn't start, e.g. the phone has none.
 */
@Composable
internal fun InviteScannerDialog(onScanned: (String) -> Unit, onFailed: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            QrCameraPreview(onScanned = onScanned, onFailed = onFailed, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .border(3.dp, Color.White, RoundedCornerShape(24.dp)),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
            ) {
                Icon(LockalTimeIcons.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
            }
            Text(
                stringResource(R.string.scan_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(32.dp),
            )
        }
    }
}

@Composable
private fun QrCameraPreview(onScanned: (String) -> Unit, onFailed: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnFailed by rememberUpdatedState(onFailed)
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            // The default analysis size is too small for invites with many apps, which make dense codes.
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build()
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        // Frames already queued can decode after the first result; only report once.
        var finished = false
        fun finish(report: () -> Unit) {
            if (!finished) {
                finished = true
                report()
            }
        }
        controller.setImageAnalysisAnalyzer(
            analysisExecutor,
            QrCodeAnalyzer { code -> mainExecutor.execute { finish { currentOnScanned(code) } } },
        )
        controller.bindToLifecycle(lifecycleOwner)
        controller.initializationFuture.addListener({
            try {
                controller.initializationFuture.get()
            } catch (e: ExecutionException) {
                finish { currentOnFailed() }
                return@addListener
            }
            // Tablets and Chromebooks may only have a front camera.
            when {
                controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> Unit
                controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                    controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                else -> finish { currentOnFailed() }
            }
        }, mainExecutor)
        onDispose {
            finished = true
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            analysisExecutor.shutdown()
        }
    }
    AndroidView(
        factory = { PreviewView(it).apply { this.controller = controller } },
        modifier = modifier,
    )
}

/**
 * Looks for a QR code in each frame's luminance (Y) plane, which is all ZXing needs. Rotation
 * doesn't matter: QR codes read at any angle.
 */
private class QrCodeAnalyzer(private val onDecoded: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)
    private var luminance = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = image.planes[0]
            val buffer = plane.buffer
            if (luminance.size != buffer.remaining()) luminance = ByteArray(buffer.remaining())
            buffer.get(luminance)
            val source = PlanarYUVLuminanceSource(
                luminance,
                plane.rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false,
            )
            try {
                onDecoded(reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text)
            } catch (e: ReaderException) {
                // No readable QR code in this frame.
            } finally {
                reader.reset()
            }
        }
    }
}
