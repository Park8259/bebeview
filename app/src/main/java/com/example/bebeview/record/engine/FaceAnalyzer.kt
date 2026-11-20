package com.example.bebeview.record.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/** 영상 한 프레임의 얼굴 검출 결과. growthLens의 FaceFrame에 대응 */
data class FaceFrame(
    val t: Double,        // 영상 내 시각(초)
    val yaw: Double?,     // 머리 좌우 각도 추정치(도). 얼굴 미검출 시 null
    val detected: Boolean,
    /** 얼굴 윤곽(FACE_OVAL 36점) + 코끝 1점의 정규화 좌표 [x, y, …]. 결과 화면 다시 보기용 */
    val pts: FloatArray? = null,
)

data class FaceTrack(
    val durationSec: Double,
    val fps: Int,
    val frames: List<FaceFrame>,
)

/**
 * 녹화된 영상을 fps 간격으로 읽어가며 MediaPipe FaceLandmarker로 얼굴을 검출하고
 * 고개 좌우 각도(yaw)를 추정합니다. (growthLens의 face.ts 에 대응하는 네이티브 버전)
 *
 * 주의: 웹(growthLens)은 FacialTransformationMatrix에서 Euler yaw를 뽑아 쓰는데,
 * 여기서는 코끝(1)·양 볼(234/454) 랜드마크의 상대 위치로 yaw를 근사합니다.
 * 정확히 같은 척도는 아니라서, NameResponseAnalyzer의 임계값은 실기기 테스트 후
 * 보정이 필요할 수 있습니다.
 */
object FaceAnalyzer {
    const val DEFAULT_FPS = 15

    private const val NOSE_TIP = 1
    private const val LEFT_CHEEK = 234
    private const val RIGHT_CHEEK = 454
    private const val MODEL_ASSET_PATH = "models/face_landmarker.task"

    // MediaPipe Face Mesh의 얼굴 윤곽 순서 (FACEMESH_FACE_OVAL), 끝에 코끝(1) — growthLens face.ts 와 같음
    private val OUTLINE_IDX = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377,
        152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109, 1,
    )

    suspend fun extractFaceTrack(
        context: Context,
        videoFile: File,
        fps: Int = DEFAULT_FPS,
    ): FaceTrack = withContext(Dispatchers.Default) {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .build()
        val landmarker = FaceLandmarker.createFromOptions(context, options)
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<FaceFrame>()
        var durationSec = 0.0

        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationSec = durationMs / 1000.0

            val stepUs = 1_000_000L / fps
            var i = 0
            while (i * stepUs < durationMs * 1000L) {
                val tUs = i * stepUs
                val bitmap: Bitmap? = retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    val result = landmarker.detectForVideo(mpImage, tUs / 1000L)
                    val faceLandmarks = result.faceLandmarks()
                    if (faceLandmarks.isNotEmpty()) {
                        val lm = faceLandmarks[0]
                        val nose = lm[NOSE_TIP]
                        val left = lm[LEFT_CHEEK]
                        val right = lm[RIGHT_CHEEK]
                        val width = right.x() - left.x()
                        val yaw = if (abs(width) > 1e-4f) {
                            val ratio = (nose.x() - left.x()) / width // 0=왼쪽 볼 쪽, 1=오른쪽 볼 쪽
                            ((ratio - 0.5) * 180.0).toDouble()
                        } else null
                        val pts = FloatArray(OUTLINE_IDX.size * 2)
                        OUTLINE_IDX.forEachIndexed { k, idx ->
                            pts[k * 2] = lm[idx].x()
                            pts[k * 2 + 1] = lm[idx].y()
                        }
                        frames.add(FaceFrame(t = tUs / 1_000_000.0, yaw = yaw, detected = true, pts = pts))
                    } else {
                        frames.add(FaceFrame(t = tUs / 1_000_000.0, yaw = null, detected = false))
                    }
                } else {
                    frames.add(FaceFrame(t = tUs / 1_000_000.0, yaw = null, detected = false))
                }
                i++
            }
        } finally {
            retriever.release()
            landmarker.close()
        }

        FaceTrack(durationSec = durationSec, fps = fps, frames = frames)
    }
}
