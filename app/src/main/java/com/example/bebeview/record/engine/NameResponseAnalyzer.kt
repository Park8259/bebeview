package com.example.bebeview.record.engine

import com.example.bebeview.record.data.AnalysisResult
import com.example.bebeview.record.data.FaceTrace
import com.example.bebeview.record.data.Metric
import com.example.bebeview.record.data.TraceFrame
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 호명반응 과제 분석. growthLens의 src/engine/metrics/nameResponse.ts 알고리즘을 그대로 포팅.
 * stimulusTimestampMs: 녹화 시작 기준, 신호음이 울린 시각(ms)
 */
object NameResponseAnalyzer {
    private const val WINDOW_SEC = 10.0
    private const val YAW_DELTA_DEG = 25.0
    private const val YAW_VEL_DEG_S = 30.0
    private const val RETRY_SEC = 5.0
    private const val MAX_ATTEMPTS = 3
    private const val MIN_DETECT_RATIO = 0.7

    /**
     * 분석 결과 + 결과 화면 "AI가 본 장면"용 좌표(trace). growthLens의 toTrace 에 대응.
     * 판정 로직은 analyze() 그대로 쓰고, 반응 시점만 결과에서 되짚어 계산해요.
     */
    fun analyzeWithTrace(track: FaceTrack, stimulusTimestampMs: Long): Pair<AnalysisResult, FaceTrace?> {
        val result = analyze(track, stimulusTimestampMs)
        if (track.frames.isEmpty() || result.metric == null) return result to null
        val stimulusSec = stimulusTimestampMs / 1000.0
        val responded = result.detail?.get("responded") == true
        val attempts = (result.detail?.get("attempts") as? Int) ?: 1
        val responseSec = if (responded) stimulusSec + (attempts - 1) * RETRY_SEC + result.metric.value else null
        val trace = FaceTrace(
            frames = track.frames.map { TraceFrame(it.t, it.yaw, it.pts) },
            stimulusSec = stimulusSec,
            responseSec = responseSec,
        )
        return result to trace
    }

    fun analyze(track: FaceTrack, stimulusTimestampMs: Long): AnalysisResult {
        val frames = track.frames
        if (frames.isEmpty()) {
            return AnalysisResult(status = "retry_needed", retryReason = "영상에서 프레임을 읽지 못했어요")
        }

        val totalDetected = frames.count { it.detected }
        if (totalDetected.toDouble() / frames.size < MIN_DETECT_RATIO) {
            val start = RetryReason.longestMissingStart(frames.map { it.t }, frames.map { it.detected })
            val reason = RetryReason.sentence("아이 얼굴이", "잘 보이지 않았어요", start, "아이 얼굴이 화면에 충분히 보이지 않았어요")
            return AnalysisResult(status = "retry_needed", retryReason = reason)
        }

        val t0BaseSec = stimulusTimestampMs / 1000.0

        for (attempt in 0 until MAX_ATTEMPTS) {
            val windowStart = t0BaseSec + attempt * RETRY_SEC
            val windowEnd = windowStart + WINDOW_SEC

            val windowFrames = frames.filter { it.t >= windowStart && it.t < windowEnd && it.yaw != null }
            if (windowFrames.isEmpty()) continue

            val detectedInWindow = windowFrames.count { it.detected }
            if (detectedInWindow.toDouble() / windowFrames.size < MIN_DETECT_RATIO) continue

            val refYaw = windowFrames[0].yaw!!

            for (i in 1 until windowFrames.size) {
                val cur = windowFrames[i]
                val prev = windowFrames[i - 1]
                val curYaw = cur.yaw ?: continue
                val prevYaw = prev.yaw ?: continue

                val delta = abs(curYaw - refYaw)
                val dt = cur.t - prev.t
                val vel = if (dt > 0) abs(curYaw - prevYaw) / dt else 0.0

                if (delta >= YAW_DELTA_DEG && vel >= YAW_VEL_DEG_S) {
                    val latencySec = cur.t - windowStart
                    return AnalysisResult(
                        status = "success",
                        metric = Metric(
                            value = (latencySec * 100).roundToInt() / 100.0,
                            unit = "seconds",
                            label = "호명 후 반응 지연",
                        ),
                        detail = mapOf(
                            "responded" to true,
                            "attempts" to (attempt + 1),
                            "yawDeltaDeg" to (delta * 10).roundToInt() / 10.0,
                        ),
                    )
                }
            }
        }

        return AnalysisResult(
            status = "success",
            metric = Metric(
                value = RETRY_SEC * MAX_ATTEMPTS,
                unit = "seconds",
                label = "호명 후 반응 지연",
            ),
            detail = mapOf("responded" to false, "attempts" to MAX_ATTEMPTS),
        )
    }
}
