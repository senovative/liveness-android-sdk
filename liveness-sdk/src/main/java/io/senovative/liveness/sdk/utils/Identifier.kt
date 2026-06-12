package io.senovative.liveness.sdk.utils

import android.content.Context
import android.content.Intent
import io.senovative.liveness.sdk.utils.Constants.Companion.EXTRA_RESULT
import io.senovative.liveness.sdk.utils.analyzer.DetectionMode
import io.senovative.liveness.sdk.utils.model.LivenessResult
// Removed Activity import

object Identifier {
    private var attempt = 0
    internal var lowMemoryThreshold: Int? = null

    internal var detectionMode = listOf(
        DetectionMode.HOLD_STILL,
        DetectionMode.OPEN_MOUTH,
        DetectionMode.BLINK,
        DetectionMode.SHAKE_HEAD,
        DetectionMode.SMILE
    )

    @JvmStatic
    fun setDetectionModeSequence(shuffle: Boolean, detectionMode: List<DetectionMode>){
        Identifier.detectionMode = detectionMode.run {
            if (shuffle) shuffled() else this
        }
    }

    @JvmStatic
    fun setLowMemoryThreshold(threshold: Int){
        lowMemoryThreshold = threshold
    }

    @JvmStatic
    fun incrementAttempt() {
        attempt++
    }

    @JvmStatic
    fun trackAttempt(result: LivenessResult): LivenessResult {
        result.attempt = attempt
        if (result.isSuccess) attempt = 0
        return result
    }

}
