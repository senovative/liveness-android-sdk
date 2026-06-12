package io.senovative.liveness.sdk.utils.analyzer

import io.senovative.liveness.sdk.utils.model.LivenessResult

interface LivenessDetectionListener {
    fun onFaceStatusChanged(faceStatus: FaceStatus)
    fun onStartDetection(detectionMode: DetectionMode)
    fun onLiveDetectionSuccess(livenessResult: LivenessResult)
    fun onLiveDetectionFailure(exception: Exception)
}