package io.senovative.liveness.sample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.senovative.liveness.sdk.view.LivenessDetectionRoute
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.senovative.liveness.sdk.utils.Identifier
import io.senovative.liveness.sdk.utils.theme.MyExploreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: LivenessHomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyExploreTheme {
                val context = LocalContext.current
                var showLiveness by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        Identifier.incrementAttempt()
                        showLiveness = true
                    } else {
                        viewModel.onCameraPermissionDenied()
                    }
                }

                if (showLiveness) {
                    LivenessDetectionRoute(
                        onResult = { result ->
                            viewModel.onLivenessResult(Identifier.trackAttempt(result))
                            showLiveness = false
                        },
                        onBack = {
                            showLiveness = false
                        }
                    )
                } else {
                    LivenessHomeScreen(
                        state = viewModel.uiState,
                        onStartLiveness = {
                            val isCameraGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isCameraGranted) {
                                Identifier.incrementAttempt()
                                showLiveness = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivenessHomeScreen(
    state: LivenessHomeUiState,
    onStartLiveness: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Senovative Liveness",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onStartLiveness,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        text = "Start Liveness",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (state.lastAttempt != null || state.detections.isNotEmpty() || state.message != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ResultSummary(state = state)

                        val shouldShowMessage = state.message != null &&
                            (state.detections.isEmpty() || state.message != "Liveness verified")
                        if (shouldShowMessage) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(state: LivenessHomeUiState) {
    if (state.lastAttempt == null && state.detections.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        state.lastAttempt?.let {
            Text(
                text = "Attempt: $it",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.detections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.detections) { detection ->
                    DetectionResultItem(detection)
                }
            }
        }
    }
}

@Composable
private fun DetectionResultItem(detection: DetectionSummary) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(
        initialValue = null,
        detection.imageUri,
        detection.imagePath
    ) {
        value = loadDetectionBitmap(context, detection)
    }.value

    Column(
        modifier = Modifier.width(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = detection.name,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${detection.timeMillis ?: 0}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = detection.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

private suspend fun loadDetectionBitmap(
    context: Context,
    detection: DetectionSummary
): Bitmap? = withContext(Dispatchers.IO) {
    detection.imageUri?.let { uri ->
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } ?: detection.imagePath?.let { path ->
        BitmapFactory.decodeFile(path)
    }
}

@Preview(showBackground = true)
@Composable
private fun LivenessHomePreview() {
    MyExploreTheme {
        LivenessHomeScreen(
            state = LivenessHomeUiState(
                lastAttempt = 1,
                totalTimeMillis = 3500,
                detections = listOf(
                    DetectionSummary("HOLD STILL", 1200),
                    DetectionSummary("BLINK", 700)
                ),
                message = "Liveness verified"
            ),
            onStartLiveness = {},
        )
    }
}
