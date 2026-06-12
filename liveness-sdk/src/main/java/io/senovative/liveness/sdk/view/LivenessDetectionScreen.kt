package io.senovative.liveness.sdk.view

import android.app.ActivityManager
import android.content.Context
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.senovative.liveness.sdk.R
import io.senovative.liveness.sdk.utils.Identifier
import io.senovative.liveness.sdk.utils.MemoryUsageMonitor
import io.senovative.liveness.sdk.utils.analyzer.DetectionMode
import io.senovative.liveness.sdk.utils.analyzer.FaceStatus
import io.senovative.liveness.sdk.utils.analyzer.LivenessDetectionAnalyzer
import io.senovative.liveness.sdk.utils.analyzer.LivenessDetectionListener
import io.senovative.liveness.sdk.utils.model.LivenessResult
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun LivenessDetectionRoute(
    onResult: (LivenessResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var instructionText by remember { mutableStateOf(context.getString(R.string.lbl_put_face_to_the_frame)) }
    var timerText by remember { mutableIntStateOf(20) }
    var countdownTime by remember { mutableIntStateOf(20) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentDetection by remember { mutableStateOf<DetectionMode?>(null) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var graphicOverlay by remember { mutableStateOf<GraphicOverlay?>(null) }
    var faceBounds by remember { mutableStateOf<Rect?>(null) }
    var cameraStarted by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                val locale = Locale.getDefault()
                textToSpeech?.voices?.firstOrNull { it.locale == locale }?.also {
                    textToSpeech?.voice = it
                }
            }
        }
        textToSpeech = tts
        val activity = context as? android.app.Activity ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
        activity?.let {
            val monitor = MemoryUsageMonitor(
                it,
                it.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager,
                lowMemoryThreshold = Identifier.lowMemoryThreshold
            )
            monitor.checkMemory()
        }

        onDispose {
            tts.shutdown()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(isTimerRunning, countdownTime) {
        if (isTimerRunning) {
            if (countdownTime > 0) {
                delay(1000)
                countdownTime--
                timerText = countdownTime
            } else {
                onResult(LivenessResult(false, "User Timeout"))
            }
        }
    }

    var faceStatus by remember { mutableStateOf(FaceStatus.NOT_FOUND) }

    val frameColor = when {
        currentDetection != null -> MaterialTheme.colorScheme.primary
        faceStatus == FaceStatus.TOO_FAR || faceStatus == FaceStatus.TOO_CLOSE -> Color.Red
        faceStatus == FaceStatus.READY -> Color.Green
        else -> Color.White
    }

    val listener = remember(context, onResult) {
        object : LivenessDetectionListener {
            override fun onFaceStatusChanged(newFaceStatus: FaceStatus) {
                textToSpeech?.stop()
                faceStatus = newFaceStatus
                instructionText = when (newFaceStatus) {
                    FaceStatus.NOT_FOUND -> context.getString(R.string.lbl_put_face_to_the_frame)
                    FaceStatus.TOO_FAR -> context.getString(R.string.lbl_too_far)
                    FaceStatus.TOO_CLOSE -> context.getString(R.string.lbl_too_close)
                    else -> context.getString(R.string.lbl_put_face_to_the_frame)
                }
            }

            override fun onStartDetection(detectionMode: DetectionMode) {
                if (currentDetection != detectionMode) {
                    currentDetection = detectionMode
                    countdownTime = 20
                    timerText = 20
                }
                val instruction = when (detectionMode) {
                    DetectionMode.HOLD_STILL -> context.getString(R.string.lbl_hold_still_instruction)
                    DetectionMode.BLINK -> context.getString(R.string.liveness_please_blink)
                    DetectionMode.OPEN_MOUTH -> context.getString(R.string.liveness_please_open_mouth)
                    DetectionMode.SHAKE_HEAD -> context.getString(R.string.liveness_please_shake_head)
                    DetectionMode.SMILE -> context.getString(R.string.liveness_please_smile)
                }
                instructionText = instruction
                textToSpeech?.speak(
                    instruction,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "${instruction.hashCode()} ${System.currentTimeMillis()}"
                )
            }

            override fun onLiveDetectionSuccess(livenessResult: LivenessResult) {
                onResult(livenessResult)
            }

            override fun onLiveDetectionFailure(exception: Exception) {
                onResult(LivenessResult(false, exception.message))
            }
        }
    }

    LaunchedEffect(previewView, graphicOverlay, faceBounds) {
        val preview = previewView ?: return@LaunchedEffect
        val overlay = graphicOverlay ?: return@LaunchedEffect
        val bounds = faceBounds ?: return@LaunchedEffect
        if (bounds.width() <= 0 || bounds.height() <= 0 || cameraStarted) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val offset = 50
            val analysisUseCase = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(
                    cameraExecutor,
                    LivenessDetectionAnalyzer(
                        context,
                        Identifier.detectionMode,
                        Rect(
                            bounds.left - offset * 2,
                            bounds.top - offset,
                            bounds.right + offset * 2,
                            bounds.bottom + offset
                        ),
                        overlay,
                        false,
                        listener
                    )
                )
            }

            val previewUseCase = Preview.Builder().build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, analysisUseCase)
                previewUseCase.setSurfaceProvider(preview.surfaceProvider)
                cameraStarted = true
                isTimerRunning = true
            } catch (exc: Exception) {
                Log.e("Liveness", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    LivenessDetectionScreenContent(
        instruction = instructionText,
        timer = timerText,
        frameColor = frameColor,
        onBack = onBack,
        onPreviewReady = { previewView = it },
        onOverlayReady = { graphicOverlay = it },
        onFaceFramePositioned = { faceBounds = it }
    )
}

@Composable
private fun LivenessDetectionScreenContent(
    instruction: String,
    timer: Int,
    frameColor: Color,
    onBack: () -> Unit,
    onPreviewReady: (PreviewView) -> Unit,
    onOverlayReady: (GraphicOverlay) -> Unit,
    onFaceFramePositioned: (Rect) -> Unit
) {
    var faceBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).also(onPreviewReady)
            },
            modifier = Modifier.fillMaxSize()
        )
        AndroidView(
            factory = { context ->
                GraphicOverlay(context, null).also(onOverlayReady)
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Dummy Box to calculate face bounds for the analyzer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .padding(horizontal = 48.dp)
                .padding(top = 80.dp)
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt()
                    ).also {
                        faceBounds = it
                        onFaceFramePositioned(it)
                    }
                }
        )

        FaceMask(faceBounds = faceBounds, frameColor = frameColor)
        TopBar(onBack = onBack)
        BottomInstructionPanel(timer = timer, instruction = instruction)
    }
}

@Composable
private fun BoxScope.TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "X",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoxScope.BottomInstructionPanel(timer: Int, instruction: String) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = instruction,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { timer / 20f },
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Text(
                    text = timer.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FaceMask(faceBounds: Rect?, frameColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val bounds = faceBounds ?: return@Canvas

        val rect = androidx.compose.ui.geometry.Rect(
            left = bounds.left.toFloat(),
            top = bounds.top.toFloat(),
            right = bounds.right.toFloat(),
            bottom = bounds.bottom.toFloat()
        )

        val path = Path().apply {
            addOval(rect)
        }

        with(drawContext.canvas) {
            val fullScreenRect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)
            val checkPoint = saveLayer(fullScreenRect, Paint())
            
            // Draw dark overlay covering the entire screen
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                size = size
            )
            
            // Clear the oval cutout
            drawPath(
                path = path,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
            restore()
        }

        // Draw colored stroke around the oval
        drawPath(
            path = path,
            color = frameColor,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
