package com.example.bebeview.record.engine

import com.example.bebeview.record.data.AnalysisResult
import com.example.bebeview.record.data.Metric
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

// growthLens src/engine/metrics/imitation.ts 포팅 — 동작 모방
// Pose 관절로 팔 들기·박수를 찾고, 신호음(stimulus) 이후 아이가 따라 하기까지의 지연 시간을 구해요.
object ImitationAnalyzer {
    private const val WINDOW_SEC = 15.0      // 감시 창
    private const val HOLD_SEC = 0.3         // 팔 들기: 연속 유지 최소 시간
    private const val CLAP_WINDOW_SEC = 1.0  // 박수: 1초 안에 모았다 벌리기 2회 이상
    private const val MIN_VIS = 0.5f         // 관절 최소 visibility
    private const val LABEL = "모방까지의 지연"

    /** 어깨 너비. 한쪽이라도 잘 안 보이면 null */
    private fun shoulderWidth(lm: List<Landmark>): Float? {
        val ls = lm[LM.L_SHOULDER]
        val rs = lm[LM.R_SHOULDER]
        if (ls.vis < MIN_VIS || rs.vis < MIN_VIS) return null
        return abs(rs.x - ls.x)
    }

    /** 팔 들기 (한쪽이라도 만족하면 true) */
    private fun isArmRaised(lm: List<Landmark>): Boolean {
        fun check(shoulder: Landmark, elbow: Landmark, wrist: Landmark): Boolean {
            if (shoulder.vis < MIN_VIS || elbow.vis < MIN_VIS || wrist.vis < MIN_VIS) return false
            // 이미지 y는 아래 방향. 손목이 팔꿈치보다 위, 팔꿈치가 어깨 근처 이상
            val wristAboveElbow = wrist.y < elbow.y - 0.02f
            val elbowNearShoulder = elbow.y < shoulder.y + 0.05f
            // 팔꿈치-어깨 벡터와 수직(y축) 사이 각도 ≥ 30도
            val dx = elbow.x - shoulder.x
            val dy = elbow.y - shoulder.y
            val angleDeg = Math.toDegrees(atan2(abs(dx), abs(dy)).toDouble())
            return wristAboveElbow && elbowNearShoulder && angleDeg >= 30
        }
        return check(lm[LM.L_SHOULDER], lm[LM.L_ELBOW], lm[LM.L_WRIST]) ||
            check(lm[LM.R_SHOULDER], lm[LM.R_ELBOW], lm[LM.R_WRIST])
    }

    /** 두 손목 거리 / 어깨 너비 < 0.35 → 손을 모은 상태 */
    private fun handsClose(lm: List<Landmark>): Boolean {
        val lw = lm[LM.L_WRIST]
        val rw = lm[LM.R_WRIST]
        if (lw.vis < MIN_VIS || rw.vis < MIN_VIS) return false
        val sw = shoulderWidth(lm) ?: 0.3f
        val dist = sqrt((lw.x - rw.x) * (lw.x - rw.x) + (lw.y - rw.y) * (lw.y - rw.y))
        return dist / sw < 0.35f
    }

    /** 상반신 평균 visibility (무효 판정용) */
    private fun torsoVisibility(lm: List<Landmark>): Float {
        val idxs = intArrayOf(LM.L_SHOULDER, LM.R_SHOULDER, LM.L_ELBOW, LM.R_ELBOW, LM.L_WRIST, LM.R_WRIST)
        return idxs.sumOf { lm[it].vis.toDouble() }.toFloat() / idxs.size
    }

    private fun round2(v: Double) = (v * 100).roundToInt() / 100.0

    private fun success(latencySec: Double, track: PoseTrack, detail: Map<String, Any?>) = AnalysisResult(
        status = "success",
        metric = Metric(round2(maxOf(0.0, latencySec)), "seconds", LABEL),
        ambient = AmbientAnalyzer.measure(track),
        detail = detail,
    )

    /** stimulusTimestampMs: 녹화 시작 기준 신호음 시각(ms) */
    fun analyze(track: PoseTrack, stimulusTimestampMs: Long): AnalysisResult {
        val fps = track.fps
        val windowStart = stimulusTimestampMs / 1000.0
        val windowEnd = windowStart + WINDOW_SEC

        val windowFrames = track.frames
            .filter { it.t >= windowStart && it.t < windowEnd }
            .map { it.t to PoseAnalyzer.identifyPersons(it).child }

        // 무효: 아이 상반신이 잘 안 보임
        val seen = windowFrames.map { (_, lm) -> lm != null && torsoVisibility(lm) >= MIN_VIS }
        val valid = seen.count { it }
        if (valid.toDouble() / maxOf(1, windowFrames.size) < 0.5) {
            val start = RetryReason.longestMissingStart(windowFrames.map { it.first }, seen)
            val reason = RetryReason.sentence("아이 몸이", "화면 밖으로 나갔어요", start, "아이 몸이 화면에 충분히 보이지 않았어요")
            return AnalysisResult(status = "retry_needed", retryReason = reason)
        }

        val holdFrames = (HOLD_SEC * fps).roundToInt()

        // 팔 들기 — holdFrames 이상 연속 유지
        var armRun = 0
        for ((t, lm) in windowFrames) {
            if (lm != null && isArmRaised(lm)) {
                armRun++
                if (armRun >= holdFrames) {
                    val latency = t - holdFrames.toDouble() / fps - windowStart
                    return success(latency, track, mapOf("imitated" to true, "attempts" to 1, "actionType" to "arm_raise"))
                }
            } else {
                armRun = 0
            }
        }

        // 박수 — 1초 창 안에서 벌림→모음 전환 2회 이상
        val clapWin = (CLAP_WINDOW_SEC * fps).roundToInt()
        val closeSeq = windowFrames.map { (_, lm) -> lm != null && handsClose(lm) }
        for (i in clapWin until closeSeq.size) {
            val slice = closeSeq.subList(i - clapWin, i)
            var transitions = 0
            for (j in 1 until slice.size) if (!slice[j - 1] && slice[j]) transitions++
            if (transitions >= 2) {
                val latency = windowFrames[i].first - CLAP_WINDOW_SEC - windowStart
                return success(latency, track, mapOf("imitated" to true, "attempts" to 1, "actionType" to "clap"))
            }
        }

        // 따라 하지 않음 — 감시 창 전체 시간
        return AnalysisResult(
            status = "success",
            metric = Metric(WINDOW_SEC, "seconds", LABEL),
            ambient = AmbientAnalyzer.measure(track),
            detail = mapOf("imitated" to false, "attempts" to 1),
        )
    }
}
