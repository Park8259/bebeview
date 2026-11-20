package com.example.bebeview.record.data

/** growthLens의 AnalysisResult를 그대로 옮긴 모델 */
data class Metric(
    val value: Double,
    val unit: String,       // "seconds" | "count"
    val label: String,
)

/** 1군 공통 지표 (과제와 무관하게 모든 영상에서 계산) */
data class Ambient(
    val bothInFrameSec: Double,
    val caregiverOrientSec: Double,
    val activitySec: Double,
    val contactSec: Double,
)

data class AnalysisResult(
    val status: String,             // "success" | "retry_needed"
    val retryReason: String? = null,
    val metric: Metric? = null,
    val ambient: Ambient? = null,
    val detail: Map<String, Any?>? = null,
) {
    val isSuccess: Boolean get() = status == "success"
}

/** 호명 반응: 프레임별 얼굴 윤곽과 고개 각도 (결과 화면 "AI가 본 장면"용, 저장하지 않음) */
data class FaceTrace(
    val frames: List<TraceFrame>,
    /** 이름을 부른 시점(초) */
    val stimulusSec: Double,
    /** 고개를 돌린 것으로 잡힌 시점(초). 반응이 없으면 null */
    val responseSec: Double?,
)

data class TraceFrame(
    val t: Double,
    val yaw: Double?,
    /** 얼굴 윤곽 + 코끝 정규화 좌표 [x0, y0, x1, y1, …] (0~1), 미검출이면 null */
    val pts: FloatArray?,
)
