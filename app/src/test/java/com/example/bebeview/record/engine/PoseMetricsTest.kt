package com.example.bebeview.record.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 동작 모방 · 공 주고받기 · 공통 지표 판정 규칙 테스트.
 * MediaPipe 없이, 가짜 관절 좌표로 growthLens 웹 규칙과 같은 결과가 나오는지 확인해요.
 */
class PoseMetricsTest {
    private val fps = 15

    /** 33개 관절. 기본은 "팔 내린 채 서 있는 사람" (cx 중심, 세로 길이 h) */
    private fun person(cx: Float, top: Float, h: Float, armUp: Boolean = false, handsTogether: Boolean = false): List<Landmark> {
        val lm = MutableList(33) { floatArrayOf(cx, top + h / 2, 0f, 0.9f) }
        fun set(i: Int, x: Float, y: Float) { lm[i] = floatArrayOf(x, y, 0f, 0.9f) }
        val sw = h * 0.25f // 어깨 너비
        val shoulderY = top + h * 0.2f
        set(LM.NOSE, cx, top + h * 0.05f)
        set(LM.L_EAR, cx - sw * 0.2f, top + h * 0.05f)
        set(LM.R_EAR, cx + sw * 0.2f, top + h * 0.05f)
        set(LM.L_SHOULDER, cx - sw / 2, shoulderY)
        set(LM.R_SHOULDER, cx + sw / 2, shoulderY)
        set(0 + 1, cx, top) // 머리 위 (상자 높이용)
        set(27, cx - sw / 3, top + h) // 발목 (상자 높이용)
        set(28, cx + sw / 3, top + h)
        if (armUp) {
            // 팔꿈치는 어깨 옆 바깥·약간 위, 손목은 그보다 위
            set(LM.L_ELBOW, cx - sw, shoulderY - 0.01f)
            set(LM.L_WRIST, cx - sw, shoulderY - h * 0.15f)
            set(LM.R_ELBOW, cx + sw / 2, shoulderY + h * 0.15f)
            set(LM.R_WRIST, cx + sw / 2, shoulderY + h * 0.3f)
        } else {
            set(LM.L_ELBOW, cx - sw / 2, shoulderY + h * 0.15f)
            set(LM.R_ELBOW, cx + sw / 2, shoulderY + h * 0.15f)
            val spread = if (handsTogether) 0.01f else sw
            set(LM.L_WRIST, cx - spread / 2, shoulderY + h * 0.25f)
            set(LM.R_WRIST, cx + spread / 2, shoulderY + h * 0.25f)
        }
        return lm
    }

    private fun track(seconds: Double, frame: (t: Double) -> PoseFrame): PoseTrack {
        val n = (seconds * fps).toInt()
        return PoseTrack(seconds, fps, (0 until n).map { frame(it.toDouble() / fps) })
    }

    // 아이(작음, 왼쪽) + 보호자(큼, 오른쪽)
    private fun child(t: Double, armUp: Boolean = false, handsTogether: Boolean = false) =
        person(0.3f, 0.5f, 0.3f, armUp, handsTogether)
    private val caregiver = person(0.75f, 0.1f, 0.8f)

    @Test
    fun imitation_armRaise_latency() {
        // 신호 5초, 아이가 8초부터 팔을 들고 유지 → 지연 = 8 - 5 = 3초
        val tr = track(30.0) { t -> PoseFrame(t, listOf(child(t, armUp = t >= 8.0), caregiver)) }
        val r = ImitationAnalyzer.analyze(tr, 5000)
        assertEquals("success", r.status)
        assertEquals(3.0, r.metric!!.value, 0.1)
        assertEquals("arm_raise", r.detail!!["actionType"])
    }

    @Test
    fun imitation_clap_detected() {
        // 10초부터 0.2초 간격으로 손을 모았다 벌림 → 박수
        val tr = track(30.0) { t ->
            val clapping = t >= 10.0 && ((t - 10.0) * 5).toInt() % 2 == 0
            PoseFrame(t, listOf(child(t, handsTogether = clapping), caregiver))
        }
        val r = ImitationAnalyzer.analyze(tr, 5000)
        assertEquals("success", r.status)
        assertEquals("clap", r.detail!!["actionType"])
        // 모으는 순간 10.0·10.4초 → 직전 1초 창에 2번 들어오는 첫 프레임 10.47초
        // 지연 = 10.47 - 1(창) - 5(신호) = 4.47초 (웹 imitation.ts 와 같은 계산)
        assertEquals(4.47, r.metric!!.value, 0.01)
    }

    @Test
    fun imitation_noAction_returnsWindow() {
        val tr = track(30.0) { t -> PoseFrame(t, listOf(child(t), caregiver)) }
        val r = ImitationAnalyzer.analyze(tr, 5000)
        assertEquals("success", r.status)
        assertEquals(15.0, r.metric!!.value, 0.0)
        assertEquals(false, r.detail!!["imitated"])
    }

    @Test
    fun imitation_noChild_retry() {
        val tr = track(30.0) { t -> PoseFrame(t, emptyList()) }
        assertEquals("retry_needed", ImitationAnalyzer.analyze(tr, 5000).status)
    }

    @Test
    fun ballPlay_childToCaregiver_counts() {
        // 공이 아이 몸 위 → 보호자 몸 위로 3초마다 왕복 → 아이→보호자 전환 횟수 셈
        val childBall = Box(0.25f, 0.6f, 0.35f, 0.7f)
        val careBall = Box(0.7f, 0.5f, 0.8f, 0.6f)
        val tr = track(60.0) { t ->
            val atChild = (t / 3).toInt() % 2 == 0
            PoseFrame(t, listOf(child(t), caregiver), ball = if (atChild) childBall else careBall)
        }
        val r = BallPlayAnalyzer.analyze(tr)
        assertEquals("success", r.status)
        assertEquals(10.0, r.metric!!.value, 0.0) // 60초 / 6초 주기 = 10번
        assertEquals(3.0, r.detail!!["transferTimeSec"] as Double, 0.1)
    }

    @Test
    fun ballPlay_noBall_retry() {
        val tr = track(60.0) { t -> PoseFrame(t, listOf(child(t), caregiver), ball = null) }
        assertEquals("retry_needed", BallPlayAnalyzer.analyze(tr).status)
    }

    @Test
    fun ambient_bothInFrame_and_missing() {
        val both = track(30.0) { t -> PoseFrame(t, listOf(child(t), caregiver)) }
        val a = AmbientAnalyzer.measure(both)
        assertNotNull(a)
        assertEquals(30.0, a!!.bothInFrameSec, 0.1)

        // 보호자가 절반만 보이면 (70% 미만) 공통 지표 없음
        val half = track(30.0) { t -> PoseFrame(t, if (t < 15) listOf(child(t), caregiver) else listOf(child(t))) }
        assertNull(AmbientAnalyzer.measure(half))
    }
}
