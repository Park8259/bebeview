package com.example.bebeview.record.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// growthLens src/engine/pose.ts (+ ballPlay.ts 의 공 검출) 의 네이티브 버전.
// 웹은 Pose·공통 지표·공 검출을 위해 영상을 따로따로 2~3번 읽지만,
// 여기서는 한 번 읽으면서 같은 프레임에 Pose(최대 2인)와 공 검출을 함께 돌려요.

/** [x, y, z, visibility]  x·y: 0~1 비율, 왼쪽위=(0,0) — 웹의 Landmark 와 같음 */
typealias Landmark = FloatArray

/** 0~1 비율 좌표 상자 */
data class Box(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/** 프레임 하나. poses 는 검출된 사람들(최대 2), ball 은 공 상자(공 검출을 켠 경우만) */
data class PoseFrame(val t: Double, val poses: List<List<Landmark>>, val ball: Box? = null)

data class PoseTrack(val durationSec: Double, val fps: Int, val frames: List<PoseFrame>)

data class PersonPoses(val child: List<Landmark>?, val caregiver: List<Landmark>?)

/** 자주 쓰는 관절 번호 (MediaPipe Pose 기준) */
object LM {
    const val NOSE = 0
    const val L_EAR = 7
    const val R_EAR = 8
    const val L_SHOULDER = 11
    const val R_SHOULDER = 12
    const val L_ELBOW = 13
    const val R_ELBOW = 14
    const val L_WRIST = 15
    const val R_WRIST = 16
}

val Landmark.x get() = this[0]
val Landmark.y get() = this[1]
val Landmark.vis get() = this[3]

object PoseAnalyzer {
    const val DEFAULT_FPS = 15
    private const val POSE_MODEL = "models/pose_landmarker_full.task"
    private const val BALL_MODEL = "models/efficientdet_lite0.tflite"
    private const val BALL_CATEGORY = "sports ball"
    private const val MIN_BALL_SCORE = 0.3f

    /**
     * 녹화 영상 → 프레임별 관절 좌표 (최대 2인). withBall 이면 같은 프레임에서 공도 찾아요.
     * onProgress: 0~1
     */
    suspend fun extract(
        context: Context,
        videoFile: File,
        withBall: Boolean = false,
        fps: Int = DEFAULT_FPS,
        onProgress: ((Float) -> Unit)? = null,
    ): PoseTrack = withContext(Dispatchers.Default) {
        val pose = PoseLandmarker.createFromOptions(
            context,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(POSE_MODEL).build())
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(2)
                .build(),
        )
        val detector = if (withBall) ObjectDetector.createFromOptions(
            context,
            ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(BALL_MODEL).build())
                .setRunningMode(RunningMode.VIDEO)
                .setScoreThreshold(MIN_BALL_SCORE)
                .setCategoryAllowlist(listOf(BALL_CATEGORY))
                .build(),
        ) else null

        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<PoseFrame>()
        var durationSec = 0.0
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            durationSec = durationMs / 1000.0
            val stepUs = 1_000_000L / fps
            val totalUs = durationMs * 1000L
            var i = 0
            while (i * stepUs < totalUs) {
                val tUs = i * stepUs
                val t = tUs / 1_000_000.0
                val raw: Bitmap? = retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (raw == null) {
                    frames.add(PoseFrame(t, emptyList()))
                } else {
                    // MediaPipe 는 ARGB_8888 만 받아요
                    val bitmap = if (raw.config == Bitmap.Config.ARGB_8888) raw else raw.copy(Bitmap.Config.ARGB_8888, false)
                    val image = BitmapImageBuilder(bitmap).build()
                    val tsMs = tUs / 1000L

                    val poses = pose.detectForVideo(image, tsMs).landmarks().map { person ->
                        person.map { p -> floatArrayOf(p.x(), p.y(), p.z(), p.visibility().orElse(0f)) }
                    }

                    // 공: 점수가 가장 높은 것. MediaPipe 상자는 픽셀 단위라 0~1 비율로 바꿔요
                    // (웹 ballPlay.ts 는 픽셀 상자를 비율 좌표인 사람 상자와 그대로 비교하는 버그가 있음)
                    val ball = detector?.detectForVideo(image, tsMs)?.detections()
                        ?.filter { d -> d.categories().any { it.categoryName() == BALL_CATEGORY } }
                        ?.maxByOrNull { d -> d.categories().firstOrNull()?.score() ?: 0f }
                        ?.boundingBox()
                        ?.let { r ->
                            val w = bitmap.width.toFloat()
                            val h = bitmap.height.toFloat()
                            Box(r.left / w, r.top / h, r.right / w, r.bottom / h)
                        }

                    frames.add(PoseFrame(t, poses, ball))
                    if (bitmap !== raw) bitmap.recycle()
                    raw.recycle()
                }
                onProgress?.invoke(if (totalUs > 0) (tUs.toFloat() / totalUs).coerceIn(0f, 1f) else 1f)
                i++
            }
            onProgress?.invoke(1f)
        } finally {
            retriever.release()
            pose.close()
            detector?.close()
        }
        PoseTrack(durationSec, fps, frames)
    }

    /** visibility > 0.3 인 관절 기준 상자 세로 길이 */
    private fun bboxHeight(lm: List<Landmark>): Float {
        val ys = lm.filter { it.vis > 0.3f }.map { it.y }
        if (ys.isEmpty()) return 0f
        return ys.max() - ys.min()
    }

    /** 아이·보호자 구분: 상자 높이가 큰 쪽이 보호자(어른), 작은 쪽이 아이 (웹과 같음) */
    fun identifyPersons(frame: PoseFrame): PersonPoses = when (frame.poses.size) {
        0 -> PersonPoses(null, null)
        1 -> PersonPoses(frame.poses[0], null)
        else -> {
            val (a, b) = frame.poses
            if (bboxHeight(a) <= bboxHeight(b)) PersonPoses(a, b) else PersonPoses(b, a)
        }
    }

    /** visibility > 0.3 인 관절을 감싸는 상자 */
    fun personBox(lm: List<Landmark>): Box? {
        val v = lm.filter { it.vis > 0.3f }
        if (v.isEmpty()) return null
        return Box(v.minOf { it.x }, v.minOf { it.y }, v.maxOf { it.x }, v.maxOf { it.y })
    }
}
