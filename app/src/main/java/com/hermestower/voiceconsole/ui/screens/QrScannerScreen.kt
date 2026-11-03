package com.hermestower.voiceconsole.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.pairing.QrPairingScanFlow
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

/**
 * GRAPH 4 CP-G4-010 wave 2 (G4-C09 wave 2): in-app QR pairing scanner.
 *
 * CameraX capture pipeline + ML Kit BUNDLED on-device barcode scanning (no
 * Play Services round-trip, frames never leave the process, nothing is
 * persisted). Every policy decision (duplicate-frame suppression, strict
 * parse, typed rejections) lives in [QrPairingScanFlow] — this screen is the
 * thin Android adapter: permission flow, preview surface, analyzer wiring.
 *
 * Lifecycle rules (camera analogue of the §10 honesty contract):
 *  - the CAMERA permission is requested ONLY from this screen, in context;
 *  - use cases bind to the screen lifecycle via bindToLifecycle — the camera
 *    hardware is released whenever the screen leaves STARTED;
 *  - onDispose releases the use cases AND the ML Kit scanner;
 *  - every failure is a typed, visible state (permission denied,
 *    CAMERA_UNAVAILABLE) — no silent fallback, no pretend scanning.
 *
 * Pairing truth: a parsed payload pairs through the deterministic MOCK client
 * only; the result is labeled SIMULATED in the UI. Real pairing is Tower
 * Graph 1.7 authority — no endpoint is contacted from here.
 */
@Composable
fun QrScannerScreen(graph: AppGraph, onBack: () -> Unit) {
    val c = hermesColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    val scanFlow = remember { QrPairingScanFlow(graph.pairingPayloadParser) }
    var scanState by remember { mutableStateOf<QrPairingScanFlow.ScanState>(QrPairingScanFlow.ScanState.Idle) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var pairingLine by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(com.hermestower.voiceconsole.R.string.qr_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(com.hermestower.voiceconsole.R.string.qr_intro),
            style = HermesType.bodySecondary, color = c.inkSecondary,
        )

        when {
            !hasPermission -> SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.qr_permission_card)) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.qr_permission_note),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                if (permissionDenied) {
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.qr_permission_refused),
                        style = HermesType.body, color = c.energy,
                    )
                }
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(com.hermestower.voiceconsole.R.string.qr_grant_button))
                }
            }

            // capture the delegated value once: smart cast is impossible on a
            // delegated property, and the format arg must be non-null Any.
            val cameraFailure = cameraError
            cameraFailure != null -> SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.qr_unavailable_card)) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.qr_unavailable_note, cameraFailure),
                    style = HermesType.body, color = c.energy,
                )
            }

            else -> {
                // ---- Live preview + analysis; lifecycle-bound, released on dispose ----
                val previewView = remember { PreviewView(context) }
                var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

                LaunchedEffect(hasPermission) {
                    val provider = kotlin.runCatching {
                        kotlinx.coroutines.suspendCancellableCoroutine<ProcessCameraProvider?> { cont ->
                            val future = ProcessCameraProvider.getInstance(context)
                            future.addListener({
                                try {
                                    cont.resume(future.get())
                                } catch (t: Throwable) {
                                    cameraError = "CAMERA_UNAVAILABLE: ${t.javaClass.simpleName}"
                                    cont.resume(null)
                                }
                            }, context.mainExecutor)
                        }
                    }.getOrNull()
                    cameraProvider = provider
                }

                DisposableEffect(cameraProvider, lifecycleOwner) {
                    val provider = cameraProvider
                    val analyzer = QrBarcodeAnalyzer { raw -> scanState = scanFlow.onRawScan(raw) }
                    if (provider != null) {
                        try {
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { it.setAnalyzer(context.mainExecutor, analyzer) }
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        } catch (t: Throwable) {
                            cameraError = "CAMERA_UNAVAILABLE: ${t.javaClass.simpleName}"
                        }
                    }
                    onDispose {
                        runCatching { provider?.unbindAll() }
                        analyzer.close()
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                ) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                }

                SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.qr_result_card)) {
                    when (val s = scanState) {
                        is QrPairingScanFlow.ScanState.Idle, is QrPairingScanFlow.ScanState.Scanning ->
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_looking_fmt, scanFlow.framesAnalyzed),
                                style = HermesType.bodySecondary, color = c.inkSecondary,
                            )

                        is QrPairingScanFlow.ScanState.Rejected -> {
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_refused_fmt, s.reject.kind, s.reject.detail),
                                style = HermesType.body, color = c.energy,
                            )
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_refused_note),
                                style = HermesType.bodySecondary, color = c.inkSecondary,
                            )
                            TextButton(onClick = {
                                scanFlow.reset()
                                scanState = QrPairingScanFlow.ScanState.Idle
                            }) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_scan_again)) }
                        }

                        is QrPairingScanFlow.ScanState.Detected -> {
                            Text(stringResource(com.hermestower.voiceconsole.R.string.qr_accepted), style = HermesType.body, color = c.ink)
                            Text(s.summary, style = HermesType.bodySecondary, color = c.inkSecondary)
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_mock_note),
                                style = HermesType.bodySecondary, color = c.inkSecondary,
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        pairingLine = context.getString(com.hermestower.voiceconsole.R.string.qr_pairing_progress)
                                        val phrase = "OWNER-CONSOLE"
                                        pairingLine = when (val r = graph.mockTowerPairingClient.pair(s.payload, phrase)) {
                                            is com.hermestower.voiceconsole.pairing.PairingResult.Success ->
                                                context.getString(com.hermestower.voiceconsole.R.string.qr_result_ok_fmt, r.sessionId, r.matchPhrase)
                                            is com.hermestower.voiceconsole.pairing.PairingResult.Failure ->
                                                context.getString(com.hermestower.voiceconsole.R.string.qr_result_fail_fmt, r.kind, r.detail)
                                        }
                                    }
                                },
                            ) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_pair_button)) }
                            pairingLine?.let { Text(it, style = HermesType.body, color = c.energy) }
                        }
                    }
                }
            }
        }

        TextButton(onClick = onBack) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_back)) }
        Text(
            stringResource(com.hermestower.voiceconsole.R.string.qr_replay_note),
            style = HermesType.bodySecondary, color = c.inkSecondary,
        )
    }
}

/** Thin ML Kit adapter: QR-only, per-frame, proxy always closed (no frame leak). */
private class QrBarcodeAnalyzer(private val onQr: (String?) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes -> onQr(codes.firstOrNull()?.rawValue) }
            .addOnFailureListener { onQr(null) }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        scanner.close()
    }
}
