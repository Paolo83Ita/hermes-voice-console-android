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
import androidx.compose.material3.OutlinedTextField
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
import com.hermestower.voiceconsole.pairing.NumericPairingCode
import com.hermestower.voiceconsole.pairing.TowerQrV2ScanFlow
import com.hermestower.voiceconsole.towerpair.TowerPairingState
import com.hermestower.voiceconsole.towerpair.toTowerPairingState
import com.hermestower.voiceconsole.locale.StateLabels
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import com.hermestower.voiceconsole.ui.components.HermesFilledButton

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
 * GRAPH A4R C1-C4 (CP-A4R-C1-008): the scanner is the PRODUCTION Tower
 * pairing entry — it consumes the frozen voice-console/v2 contract through
 * [TowerQrV2ScanFlow] + [TowerPairingController] (the B authority lane). The
 * SIMULATED v1 candidate-scheme path is gone from production navigation: a
 * parsed QR is claimed with the Tower's six-digit code, the Owner confirms
 * the three-word phrase on the Tower console, and the pairing state is
 * committed atomically by the repaired AndroidKeyStore-backed store.
 *
 * Typed states are EXACTLY the mission section 8 list; camera permission
 * denial (including permanently-denied) and camera unavailability are typed,
 * never silent. No stack trace ever reaches this screen.
 */
@Composable
fun QrScannerScreen(graph: AppGraph, onBack: () -> Unit, onPaired: () -> Unit = {}) {
    val c = hermesColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    // Mission §8 typed camera states (not booleans): DENIED vs PERMANENTLY_DENIED
    // (the OS stopped showing the rationale = "don't ask again"), rendered via
    // the §23 localized StateLabels table.
    var permissionDeniedPermanent by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? android.app.Activity
            val showsRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } == true
            // After a refusal: rationale visible => plain denial; rationale gone
            // => the OS marked it permanent ("never ask again").
            permissionDeniedPermanent = !showsRationale
        }
    }

    val scanFlow = remember { TowerQrV2ScanFlow(graph.towerQrParser) }
    var scanState by remember { mutableStateOf<TowerQrV2ScanFlow.ScanState>(TowerQrV2ScanFlow.ScanState.Idle) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    // ANDROID REPAIR CP-005: the raw QR text is retained so the CANONICAL session
    // machine (envelope v2+v3, multi-candidate resolver) can re-parse and drive
    // the full flow — the old single-endpoint resolution is retired.
    var lastRawQr by remember { mutableStateOf<String?>(null) }
    // ---- production pairing state (Tower v2/v3 contract, session machine) ----
    var manualEndpointInput by remember { mutableStateOf("") }
    var manualEndpointVisible by remember { mutableStateOf(false) }
    var numericCodeInput by remember { mutableStateOf("") }
    var numericCodeInvalid by remember { mutableStateOf(false) }
    var pairingBusy by remember { mutableStateOf(false) }
    var sessionSnap by remember { mutableStateOf<com.hermestower.voiceconsole.pairing.PairingSessionController.Snapshot?>(null) }

    val localeTag = com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context)
    fun typedStateLine(state: TowerPairingState): String = context.getString(
        com.hermestower.voiceconsole.R.string.qr_typed_state_fmt,
        StateLabels.label(state.name, localeTag),
    )


    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(com.hermestower.voiceconsole.R.string.qr_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(com.hermestower.voiceconsole.R.string.qr_intro),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )

        // capture the delegated value once: smart cast is impossible on a delegated
        // property, and a val declaration may not precede when-branches.
        val cameraFailure = cameraError
        when {
            !hasPermission -> SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.qr_permission_card)) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.qr_permission_note),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
                // Mission §8: BOTH denial states are typed and surfaced.
                Text(
                    typedStateLine(
                        if (permissionDeniedPermanent) TowerPairingState.CAMERA_PERMISSION_PERMANENTLY_DENIED
                        else TowerPairingState.CAMERA_PERMISSION_DENIED,
                    ),
                    style = HermesType.body, color = c.textPrimary,
                )
                if (permissionDeniedPermanent) {
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.qr_v2_perm_permanent_note),
                        style = HermesType.bodySecondary, color = c.textSecondary,
                    )
                }
                HermesFilledButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(com.hermestower.voiceconsole.R.string.qr_grant_button))
                }
            }

            cameraFailure != null -> SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.qr_unavailable_card)) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.qr_unavailable_note, cameraFailure),
                    style = HermesType.body, color = c.textInfo,
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
                    val analyzer = QrBarcodeAnalyzer { raw ->
                        if (!raw.isNullOrBlank()) lastRawQr = raw
                        scanState = scanFlow.onRawScan(raw)
                    }
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
                        is TowerQrV2ScanFlow.ScanState.Idle, is TowerQrV2ScanFlow.ScanState.Scanning ->
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_v2_looking_fmt, scanFlow.framesAnalyzed),
                                style = HermesType.bodySecondary, color = c.textSecondary,
                            )

                        is TowerQrV2ScanFlow.ScanState.Rejected -> {
                            val missionState = s.reject.kind.toTowerPairingState()
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_refused_fmt, s.reject.kind, s.reject.detail),
                                style = HermesType.body, color = c.textInfo,
                            )
                            Text(
                                typedStateLine(missionState),
                                style = HermesType.body, color = c.textPrimary,
                            )
                            Text(
                                stringResource(com.hermestower.voiceconsole.R.string.qr_refused_note),
                                style = HermesType.bodySecondary, color = c.textSecondary,
                            )
                            TextButton(onClick = {
                                scanFlow.reset()
                                scanState = TowerQrV2ScanFlow.ScanState.Idle
                            }) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_scan_again)) }
                        }

                        is TowerQrV2ScanFlow.ScanState.Detected -> {
                            // ANDROID REPAIR CP-005: the CANONICAL session machine drives
                            // the pairing (envelope v2+v3 -> multi-candidate resolver ->
                            // claim/attest -> phrase gate -> ack -> commit). The old
                            // single-endpoint dead-end (tower.local on 4G) is retired:
                            // every candidate is classified, LAN is skipped honestly on
                            // cellular, manual fallback is the same pipeline (no bypass).
                            val rawQr = lastRawQr
                            val snap = sessionSnap
                            when {
                                rawQr == null -> {
                                    Text(
                                        stringResource(com.hermestower.voiceconsole.R.string.qr_refused_note),
                                        style = HermesType.bodySecondary, color = c.textSecondary,
                                    )
                                    TextButton(onClick = {
                                        scanFlow.reset()
                                        scanState = TowerQrV2ScanFlow.ScanState.Idle
                                    }) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_scan_again)) }
                                }
                                snap != null -> {
                                    PairingSessionStateView(
                                        graph = graph,
                                        snap = snap,
                                        onPaired = onPaired,
                                        onRestartScan = {
                                            sessionSnap = null
                                            scanFlow.reset()
                                            scanState = TowerQrV2ScanFlow.ScanState.Idle
                                        },
                                    )
                                }
                                else -> {
                                    if (!manualEndpointVisible) {
                                        TextButton(onClick = { manualEndpointVisible = true }) {
                                            Text(stringResource(com.hermestower.voiceconsole.R.string.qr_v2_manual_endpoint_action))
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = manualEndpointInput,
                                            onValueChange = { manualEndpointInput = it },
                                            label = { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_v2_manual_endpoint_label)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    OutlinedTextField(
                                        value = numericCodeInput,
                                        onValueChange = { if (it.length <= NumericPairingCode.REQUIRED_LENGTH) numericCodeInput = it },
                                        label = { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_v2_code_label)) },
                                        isError = numericCodeInvalid,
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (numericCodeInvalid) {
                                        Text(
                                            stringResource(com.hermestower.voiceconsole.R.string.qr_v2_code_invalid),
                                            style = HermesType.body, color = c.textInfo,
                                        )
                                    }
                                    if (pairingBusy) {
                                        Text(
                                            stringResource(com.hermestower.voiceconsole.R.string.qr_v2_progress),
                                            style = HermesType.bodySecondary, color = c.textSecondary,
                                        )
                                    }
                                    HermesFilledButton(
                                        enabled = !pairingBusy,
                                        onClick = {
                                            val normalized = NumericPairingCode.normalize(numericCodeInput)
                                            if (normalized == null) {
                                                numericCodeInvalid = true
                                            } else {
                                                numericCodeInvalid = false
                                                pairingBusy = true
                                                scope.launch {
                                                    try {
                                                        val manualCandidates = manualEndpointInput.trim()
                                                            .takeIf { it.isNotEmpty() }
                                                            ?.let {
                                                                com.hermestower.voiceconsole.pairing.ManualEndpointParser.parse(it).candidate
                                                            }
                                                            ?.let { listOf(it) } ?: emptyList()
                                                        sessionSnap = graph.pairingSessionController.startFromEnvelope(
                                                            qrText = rawQr,
                                                            claimCode = normalized,
                                                            label = "Console vocale Android",
                                                            manualCandidates = manualCandidates,
                                                        )
                                                    } finally {
                                                        pairingBusy = false
                                                    }
                                                }
                                            }
                                        },
                                    ) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_v2_pair_button)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        TextButton(onClick = onBack) { Text(stringResource(com.hermestower.voiceconsole.R.string.qr_back)) }
        Text(
            stringResource(com.hermestower.voiceconsole.R.string.qr_replay_note),
            style = HermesType.bodySecondary, color = c.textSecondary,
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
