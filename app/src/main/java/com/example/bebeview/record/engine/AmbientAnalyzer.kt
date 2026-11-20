package com.example.bebeview.record.engine

import com.example.bebeview.record.data.Ambient
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// growthLens src/engine/metrics/ambient.ts 포팅 — 1군 공통 지표 (동작 모방·공 주고받기에서 계산)
// bothInFrameSec: 두 사람이 모두 검출된 구간
// caregiverOrientSec: 아이 머리 방향이 보호자를 향한 누적 시간
// activitySec: 아이 움직임(중심 이동 or 자세 변화) 누적 시간
// contactSec: 보호자 손목이 아이 상자 안에 있는 누적 시간 (참고값)
object AmbientAnalyzer {
    private const val SMOOTH_SEC = 0.3
    private const val MIN_BOTH_RATIO = 0.7

    private data class Bbox(val cx: Float, val cy: Float, val w: Float, val h: Float)

    private fun childBbox(lm: List<Landmark>): Bbox? {
        val b = PoseAnalyzer.personBox(lm) ?: return null
        return Bbox((b.x1 + b.x2) / 2, (b.y1 + b.y2) / 2, b.x2 - b.x1, b.y2 - b.y1)
    }

    private fun dot2dNorm(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val aLen = sqrt(ax * ax + ay * ay).takeIf { it != 0f } ?: 1f
        val bLen = sqrt(bx * bx + by * by).takeIf { it != 0f } ?: 1f
        return (ax / aLen) * (bx / bLen) + (ay / aLen) * (by / bLen)
    }

    /** 앞뒤 halfWin 프레임 중 과반이 true 인 프레임 수 */
    private fun smoothCount(raw: BooleanArray, halfWin: Int): Int {
        var count = 0
        for (i in raw.indices) {
            val s = maxOf(0, i - halfWin)
            val e = minOf(raw.size, i + halfWin + 1)
            var on = 0
            for (j in s until e) if (raw[j]) on++
            if (on * 2 > e - s) count++
        }
        return count
    }

    /** minRun 프레임 이상 이어진 true 구간만 더한 프레임 수 */
    private fun continuousCount(raw: BooleanArray, minRun: Int): Int {
        var total = 0
        var run = 0
        for (v in raw) {
            if (v) run++ else { if (run >= minRun) total += run; run = 0 }
        }
        if (run >= minRun) total += run
        return total
    }

    /** 두 사람이 영상의 70% 이상 함께 보이지 않으면 null (웹의 undefined) */
    fun measure(track: PoseTrack): Ambient? {
        val fps = track.fps
        val frames = track.frames
        val n = frames.size
        val halfWin = (SMOOTH_SEC * fps / 2).roundToInt()
        val minRun = (SMOOTH_SEC * fps).roundToInt()

        val bothRaw = BooleanArray(n)
        val orientRaw = BooleanArray(n)
        val activeRaw = BooleanArray(n)
        val contactRaw = BooleanArray(n)
        var prev: Bbox? = null

        for (i in 0 until n) {
            val (child, caregiver) = PoseAnalyzer.identifyPersons(frames[i])
            if (child == null) { prev = null; continue }
            bothRaw[i] = caregiver != null

            // 보호자 지향: 아이 시선(귀 중간→코) 과 아이 코→보호자 코 방향의 내적 ≥ 0.3
            if (caregiver != null) {
                val nose = child[LM.NOSE]
                val lEar = child[LM.L_EAR]
                val rEar = child[LM.R_EAR]
                if (nose.vis > 0.5f && (lEar.vis > 0.5f || rEar.vis > 0.5f)) {
                    val earMidX = (lEar.x + rEar.x) / 2
                    val earMidY = (lEar.y + rEar.y) / 2
                    val cNose = caregiver[LM.NOSE]
                    orientRaw[i] = dot2dNorm(nose.x - earMidX, nose.y - earMidY, cNose.x - nose.x, cNose.y - nose.y) >= 0.3f
                }
            }

            // 활동: 상자 중심 이동 또는 가로세로비 변화
            val box = childBbox(child)
            if (box != null) {
                prev?.let { p ->
                    val diag = sqrt(p.w * p.w + p.h * p.h).takeIf { it != 0f } ?: 1f
                    val move = sqrt((box.cx - p.cx) * (box.cx - p.cx) + (box.cy - p.cy) * (box.cy - p.cy))
                    val dAR = abs(box.w / (if (box.h != 0f) box.h else 0.001f) - p.w / (if (p.h != 0f) p.h else 0.001f))
                    activeRaw[i] = move / diag > 0.02f || dAR > 0.05f
                }
                prev = box

                // 접촉: 보호자 손목이 아이 상자 안
                if (caregiver != null) {
                    fun inBox(w: Landmark) = w.vis > 0.5f && abs(w.x - box.cx) < box.w / 2 && abs(w.y - box.cy) < box.h / 2
                    contactRaw[i] = inBox(caregiver[LM.L_WRIST]) || inBox(caregiver[LM.R_WRIST])
                }
            } else {
                prev = null
            }
        }

        val bothInFrameSec = bothRaw.count { it }.toDouble() / fps
        if (bothInFrameSec == 0.0 || track.durationSec <= 0 || bothInFrameSec / track.durationSec < MIN_BOTH_RATIO) return null

        return Ambient(
            bothInFrameSec = bothInFrameSec,
            caregiverOrientSec = smoothCount(orientRaw, halfWin).toDouble() / fps,
            activitySec = smoothCount(activeRaw, halfWin).toDouble() / fps,
            contactSec = continuousCount(contactRaw, minRun).toDouble() / fps,
        )
    }
}
