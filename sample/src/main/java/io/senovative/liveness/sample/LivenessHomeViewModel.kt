package io.senovative.liveness.sample

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.senovative.liveness.sdk.utils.model.LivenessResult

data class DetectionSummary(
    val name: String,
    val timeMillis: Long?,
    val imageUri: Uri? = null,
    val imagePath: String? = null
)

data class LivenessHomeUiState(
    val lastAttempt: Int? = null,
    val totalTimeMillis: Long? = null,
    val detections: List<DetectionSummary> = emptyList(),
    val message: String? = null
)

class LivenessHomeViewModel : ViewModel() {
    var uiState by mutableStateOf(LivenessHomeUiState())
        private set

    fun onLivenessResult(result: LivenessResult?) {
        uiState = when {
            result == null -> uiState.copy(message = "No liveness result returned")
            result.isSuccess -> LivenessHomeUiState(
                lastAttempt = result.attempt,
                totalTimeMillis = result.totalTimeMilis,
                detections = result.detectionResult.orEmpty().map {
                    DetectionSummary(
                        name = it.detectionMode.name.replace('_', ' '),
                        timeMillis = it.timeMilis,
                        imageUri = it.image,
                        imagePath = it.imagePath
                    )
                },
                message = "Liveness verified"
            )
            else -> LivenessHomeUiState(
                message = result.errorMessage ?: result.errorType?.name ?: "Liveness failed"
            )
        }
    }

    fun onCameraPermissionDenied() {
        uiState = uiState.copy(message = "Camera permission is required")
    }
}
