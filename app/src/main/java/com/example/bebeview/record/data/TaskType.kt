package com.example.bebeview.record.data

// growthLens src/data/tasks.ts 포팅 — 과제 정보 한곳 모음 (Figma 부모 버전 문구 기준)

/** 측정 과제 종류. (growthLens의 TaskType과 1:1 대응) */
enum class TaskType(
    val apiName: String,
    val label: String,
    /** 오늘의 과제 카드 한 줄 설명 */
    val short: String,
    val meta: String,
    /** 과제 안내 화면 설명 (2줄) */
    val guide: String,
    /** 촬영 준비 화면 하단 제목 */
    val prepTitle: String,
    /** 분석 화면 두 번째 단계 */
    val analyzeStep: String,
    /** 재촬영 화면 팁 3개 */
    val retryTips: List<String>,
    /** 측정 시간(초) */
    val durationSec: Int,
) {
    NAME_RESPONSE(
        apiName = "name_response",
        label = "호명 반응",
        short = "부르면 얼마나 빨리 돌아보는지",
        meta = "약 20초",
        guide = "아이가 다른 곳을 볼 때 이름을 불러요.\n소리가 나면 바로 불러주세요.",
        prepTitle = "아이 옆에 편하게 앉아주세요",
        analyzeStep = "고개를 돌리는 순간",
        retryTips = listOf(
            "아이 얼굴이 화면 가운데 오게 해주세요",
            "폰을 조금 더 뒤로, 아이 전신이 보이게 해주세요",
            "아이 시야 밖에서 이름을 불러주세요",
        ),
        durationSec = 20,
    ),
    IMITATION(
        apiName = "imitation",
        label = "동작 모방",
        short = "보호자 동작을 따라 하는지",
        meta = "약 30초",
        guide = "아이가 보는 앞에서 손 들기나 박수를 보여줘요.\n소리가 나면 바로 시작해주세요.",
        prepTitle = "아이와 마주 앉아주세요",
        analyzeStep = "동작을 따라 하는 순간",
        retryTips = listOf(
            "아이와 마주 앉아서 시작해 주세요",
            "폰을 더 뒤로, 아이와 보호자가 함께 보이게 해주세요",
            "동작은 아이가 보는 앞에서 크게 해주세요",
        ),
        durationSec = 30,
    ),
    BALL_PLAY(
        apiName = "ball_play",
        label = "공 주고받기",
        short = "공을 보호자와 주고받는지",
        meta = "약 1분 · 준비물 단색 공",
        guide = "공을 아이와 보호자 사이에 두고 주고받아요.\n공이 화면에 계속 보이게 해주세요.",
        prepTitle = "공을 화면 안에 놓아주세요",
        analyzeStep = "아이가 다시 던지는 순간",
        retryTips = listOf(
            "아이와 마주 앉아서 시작해 주세요",
            "폰을 조금 더 뒤로, 아이 전신과 공이 보이게 해주세요",
            "공은 아이와 보호자 사이에서만 굴려 주세요",
        ),
        durationSec = 60,
    );

    /** 신호음(이름 부르기·동작 시범 시점)이 필요한 과제 */
    val needsStimulus: Boolean get() = this != BALL_PLAY

    companion object {
        /** 과제를 보여주는 순서 (오늘의 과제, 변화, 진료 요약 공통). 호명 반응이 맨 위 */
        val ORDER = listOf(NAME_RESPONSE, BALL_PLAY, IMITATION)

        fun fromApi(name: String): TaskType = entries.firstOrNull { it.apiName == name } ?: NAME_RESPONSE
    }
}

fun unitOf(unit: String) = if (unit == "seconds") "초" else "회"

/**
 * 아이를 부르는 이름. 성을 떼고, 받침이 있으면 "이"를 붙여요.
 * "김하온" → "하온이", "김철수" → "철수", "남궁민준" → "민준이", "하온" → "하온이"
 */
fun nameWithI(fullName: String): String {
    val name = fullName.trim()
    val given = when (name.length) {
        3 -> name.substring(1)
        4 -> name.substring(2)
        else -> name
    }
    if (given.isEmpty()) return given
    val last = given.last().code
    if (last < 0xAC00 || last > 0xD7A3) return given
    return if ((last - 0xAC00) % 28 == 0) given else "${given}이"
}
