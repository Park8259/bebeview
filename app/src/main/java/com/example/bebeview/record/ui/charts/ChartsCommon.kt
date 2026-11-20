package com.example.bebeview.record.ui.charts

import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.ui.AppNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.ParentTab
import java.util.Calendar

// 변화 · 진료 요약 화면이 같이 쓰는 작은 도구

/** 하단 탭 → 화면 이동 */
internal fun AppNav.openTab(tab: ParentTab) = tab(
    when (tab) {
        ParentTab.Tasks -> Route.Tasks
        ParentTab.Timeline -> Route.Timeline
        ParentTab.Charts -> Route.Charts
        ParentTab.Summary -> Route.ClinicSummary
    },
)

/** 자바스크립트 숫자 표시와 같게: 2.0 → "2", 4.2 → "4.2" */
internal fun jsNum(v: Double): String =
    if (v == Math.floor(v) && !v.isInfinite() && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()

/** 자바스크립트 Math.round 와 같게 (0.5 는 올림) */
internal fun jsRound(v: Double): Long = Math.floor(v + 0.5).toLong()

internal fun cal(ts: Long): Calendar = Calendar.getInstance().apply { timeInMillis = ts }

/** "11월 21일" */
internal fun fmtDay(ts: Long): String = cal(ts).let { "${it.get(Calendar.MONTH) + 1}월 ${it.get(Calendar.DAY_OF_MONTH)}일" }

/** 공통 지표 비율(%) — 함께 보인 시간 대비 */
internal fun ambientRatio(r: MeasurementRecord, orient: Boolean): Double {
    val a = r.result.ambient ?: return 0.0
    val v = if (orient) a.caregiverOrientSec else a.activitySec
    return if (a.bothInFrameSec > 0) v / a.bothInFrameSec * 100 else 0.0
}
