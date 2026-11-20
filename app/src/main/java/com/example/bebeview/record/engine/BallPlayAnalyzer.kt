package com.example.bebeview.record.engine

import com.example.bebeview.record.data.AnalysisResult
import com.example.bebeview.record.data.Metric
import kotlin.math.roundToInt

// growthLens src/engine/metrics/ballPlay.ts 포팅 — 공 주고받기
// 공 상자와 각 사람 상자의 겹침(IoU)이 큰 쪽을 그 순간의 공 주인으로 보고,
// 아이 → 보호자로 주인이 바뀐 횟수를 세요.
object BallPlayAnalyzer {
    private const val MIN_BALL_DETECTED_RATIO = 0.5

    private enum class Owner { CHILD, CAREGIVER, NONE }

    private fun iou(a: Box, b: Box): Float {
        val ix1 = maxOf(a.x1, b.x1)
        val iy1 = maxOf(a.y1, b.y1)
        val ix2 = minOf(a.x2, b.x2)
        val iy2 = minOf(a.y2, b.y2)
        val inter = maxOf(0f, ix2 - ix1) * maxOf(0f, iy2 - iy1)
        if (inter == 0f) return 0f
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (areaA + areaB - inter)
    }

    /** track 은 PoseAnalyzer.extract(withBall = true) 로 만든 것 */
    fun analyze(track: PoseTrack): AnalysisResult {
        val frames = track.frames
        val detected = frames.count { it.ball != null }
        val ballDetectedRatio = detected.toDouble() / maxOf(1, frames.size)
        if (ballDetectedRatio < MIN_BALL_DETECTED_RATIO) {
            val start = RetryReason.longestMissingStart(frames.map { it.t }, frames.map { it.ball != null })
            val reason = RetryReason.sentence("공이", "화면 밖으로 나갔어요", start, "공이 화면에 충분히 보이지 않았어요")
            return AnalysisResult(status = "retry_needed", retryReason = reason)
        }

        // 프레임별 공 주인
        val owners = frames.map { f ->
            val ball = f.ball ?: return@map Owner.NONE
            val (child, caregiver) = PoseAnalyzer.identifyPersons(f)
            val childIoU = child?.let(PoseAnalyzer::personBox)?.let { iou(ball, it) } ?: 0f
            val careIoU = caregiver?.let(PoseAnalyzer::personBox)?.let { iou(ball, it) } ?: 0f
            when {
                childIoU == 0f && careIoU == 0f -> Owner.NONE
                childIoU >= careIoU -> Owner.CHILD
                else -> Owner.CAREGIVER
            }
        }

        // 아이 → 보호자 전환 (유효 상호작용)
        var transferCount = 0
        var totalTransferSec = 0.0
        var childStart: Double? = null
        var prev = Owner.NONE
        owners.forEachIndexed { i, owner ->
            if (owner == Owner.CHILD && prev != Owner.CHILD) {
                childStart = frames[i].t
            } else if (owner == Owner.CAREGIVER && prev == Owner.CHILD && childStart != null) {
                transferCount++
                totalTransferSec += frames[i].t - childStart!!
                childStart = null
            }
            prev = owner
        }

        val detail = buildMap<String, Any?> {
            put("transferred", transferCount > 0)
            if (transferCount > 0) put("transferTimeSec", (totalTransferSec / transferCount * 100).roundToInt() / 100.0)
            put("transferCount", transferCount)
            put("ballDetectedRatio", (ballDetectedRatio * 100).roundToInt() / 100.0)
        }

        return AnalysisResult(
            status = "success",
            metric = Metric(transferCount.toDouble(), "count", "주고받은 횟수"),
            ambient = AmbientAnalyzer.measure(track),
            detail = detail,
        )
    }
}
