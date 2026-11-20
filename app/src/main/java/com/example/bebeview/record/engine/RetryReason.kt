package com.example.bebeview.record.engine

import kotlin.math.roundToInt

/**
 * 재촬영 화면에 보여줄 구체적인 이유 ("공이 9초쯤 화면 밖으로 나갔어요").
 * 프레임별로 "잘 보였는지"를 받아, 가장 길게 안 보인 구간이 시작된 시각을 문장으로 만들어요.
 */
internal object RetryReason {
    /** 가장 길게 이어진 false 구간의 시작 시각(초). false 가 없으면 null */
    fun longestMissingStart(times: List<Double>, visible: List<Boolean>): Double? {
        var bestStart = -1
        var bestLen = 0
        var runStart = -1
        for (i in visible.indices) {
            if (!visible[i]) {
                if (runStart < 0) runStart = i
                val len = i - runStart + 1
                if (len > bestLen) { bestLen = len; bestStart = runStart }
            } else {
                runStart = -1
            }
        }
        return if (bestStart < 0) null else times[bestStart]
    }

    /**
     * @param subject "공이", "아이 몸이" 처럼 조사까지 붙인 주어
     * @param what "화면 밖으로 나갔어요" 처럼 뒤에 붙일 말
     */
    fun sentence(subject: String, what: String, startSec: Double?, fallback: String): String {
        if (startSec == null) return fallback
        val s = startSec.roundToInt()
        return if (s <= 0) "$subject 처음부터 화면에 잘 보이지 않았어요" else "$subject ${s}초쯤 $what"
    }
}
