package com.example.bebeview.record.data

/** 로컬(기기 내)에 저장하는 과제 수행 기록 한 건. growthLens의 SessionRecord에 대응 */
data class MeasurementRecord(
    val id: String,
    val taskType: String,     // TaskType.apiName
    val recordedAt: Long,     // Unix ms
    val result: AnalysisResult,
)
