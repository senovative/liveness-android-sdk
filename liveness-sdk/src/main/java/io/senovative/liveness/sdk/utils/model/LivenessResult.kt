package io.senovative.liveness.sdk.utils.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import io.senovative.liveness.sdk.common.ResultErrorType
import io.senovative.liveness.sdk.utils.BitmapUtils
import io.senovative.liveness.sdk.utils.analyzer.DetectionMode
import kotlinx.parcelize.Parcelize

@Parcelize
data class LivenessResult(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val errorType: ResultErrorType? = null,
    val totalTimeMilis: Long? = null,
    val detectionResult: List<DetectionResult>? = null,
    var attempt: Int = 0
): Parcelable{

    @Parcelize
    data class DetectionResult(
        val detectionMode: DetectionMode,
        val image: Uri?,
        val imagePath: String?,
        val timeMilis: Long?): Parcelable

    fun getBitmap(
        context: Context,
        detectionMode: DetectionMode,
        onError: (String, ResultErrorType) -> Unit
    ): Bitmap? {
        return detectionResult?.find {
            it.detectionMode == detectionMode
        }?.let {
            it.image?.let { uri ->
                BitmapUtils.getBitmapFromContentUri(context.contentResolver, uri, onError = onError)
            }
        }
    }
}