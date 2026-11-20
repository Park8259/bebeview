package com.example.bebeview.record.data

import kotlin.math.roundToLong
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

// growthLens의 zustand 저장소(child · records · notes · questions)를 옮긴 것.
// 웹처럼 기기 안(파일)에만 저장해요. 서버로는 지표 숫자와 공유 코드만 오가요.

/** 아이 정보 (growthLens types/child.ts) */
data class ChildProfile(
    val id: String,
    val name: String,
    val birthDate: String,          // YYYY-MM-DD
    val gender: String,             // "male" | "female"
    val gestationalWeeks: Int? = null, // null = 만삭 또는 조산인데 주수 모름; <37 = 조산
    val premature: Boolean? = null,
    val region: String? = null,     // 시/도
    val district: String? = null,   // 시/군/구
    val siblingOrder: Int? = null,  // 1=첫째, 2=둘째, 3=셋째 이상
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val createdAt: Long,
)

/** 의사 메모 (growthLens store/notes.ts). 보호자 앱은 공개된 것만 읽어요 */
data class DoctorNote(
    val id: String,
    val patientId: String,
    val date: String,               // YYYY-MM-DD
    val text: String,
    val isShared: Boolean,
    val nextTasks: List<String>? = null,
)

/** 의사 앱 데모 데이터 (growthLens data/mockDoctor.ts 의 의사 정보) */
object MockDoctor {
    const val NAME = "김민준"
    const val SPECIALTY = "소아청소년과"
    const val HOSPITAL = "서울어린이발달센터"
}

object AppData {
    private val gson = Gson()
    private lateinit var dir: File

    val profile = mutableStateOf<ChildProfile?>(null)
    val records = mutableStateListOf<MeasurementRecord>()
    val notes = mutableStateListOf<DoctorNote>()
    val questions = mutableStateListOf<String>()

    fun init(context: Context) {
        if (::dir.isInitialized) return
        dir = context.filesDir
        profile.value = read("child.json", object : TypeToken<ChildProfile?>() {})
        // 저장된 기록이 없으면 예시 기록으로 시작 (웹과 같음)
        records.addAll(read("records.json", object : TypeToken<List<MeasurementRecord>>() {})?.takeIf { it.isNotEmpty() } ?: seedRecords())
        notes.addAll(read("notes.json", object : TypeToken<List<DoctorNote>>() {}) ?: SEED_NOTES)
        questions.addAll(read("questions.json", object : TypeToken<List<String>>() {}) ?: SEED_QUESTIONS)
    }

    // ── 아이 정보 ──
    fun setProfile(p: ChildProfile) {
        profile.value = p
        write("child.json", p)
    }

    fun clearProfile() {
        profile.value = null
        File(dir, "child.json").delete()
    }

    // ── 기록 ──
    fun addRecord(taskType: TaskType, recordedAt: Long, result: AnalysisResult): MeasurementRecord {
        val r = MeasurementRecord(UUID.randomUUID().toString(), taskType.apiName, recordedAt, result)
        records.add(r)
        write("records.json", records.toList())
        return r
    }

    fun deleteRecord(id: String) {
        records.removeAll { it.id == id }
        write("records.json", records.toList())
    }

    fun record(id: String): MeasurementRecord? = records.firstOrNull { it.id == id }

    // ── 선생님께 물어볼 것 ──
    fun addQuestion(q: String) {
        questions.add(q)
        write("questions.json", questions.toList())
    }

    fun removeQuestion(index: Int) {
        if (index in questions.indices) questions.removeAt(index)
        write("questions.json", questions.toList())
    }

    // ── 파일 ──
    private fun <T> read(name: String, type: TypeToken<T>): T? = try {
        val f = File(dir, name)
        if (f.exists()) gson.fromJson<T>(f.readText(), type.type) else null
    } catch (_: Exception) {
        null
    }

    private fun write(name: String, value: Any) {
        File(dir, name).writeText(gson.toJson(value))
    }

    // ── 예시 데이터 (growthLens store/records.ts · notes.ts · questions.ts) ──
    private const val DAY = 24 * 60 * 60 * 1000L

    private fun seedRecords(): List<MeasurementRecord> {
        val now = System.currentTimeMillis()
        fun ambient(orient: Double, activity: Double) = Ambient(52.0, orient, activity, 2.8)
        fun ok(task: TaskType, daysAgo: Int, value: Double, unit: String, label: String, amb: Ambient?) =
            MeasurementRecord(
                UUID.randomUUID().toString(), task.apiName, now - daysAgo * DAY,
                AnalysisResult("success", metric = Metric(value, unit, label), ambient = amb),
            )
        return listOf(
            // 3주 전
            ok(TaskType.NAME_RESPONSE, 21, 4.2, "seconds", "호명 후 반응 지연", null),
            ok(TaskType.IMITATION, 20, 5.1, "seconds", "모방까지의 지연", ambient(21.0, 14.0)),
            ok(TaskType.BALL_PLAY, 19, 2.0, "count", "주고받은 횟수", ambient(23.0, 16.0)),
            // 2주 전
            ok(TaskType.NAME_RESPONSE, 14, 3.6, "seconds", "호명 후 반응 지연", null),
            ok(TaskType.IMITATION, 13, 4.0, "seconds", "모방까지의 지연", ambient(27.0, 20.0)),
            ok(TaskType.BALL_PLAY, 12, 3.0, "count", "주고받은 횟수", ambient(29.0, 22.0)),
            // 1주 전
            ok(TaskType.NAME_RESPONSE, 7, 2.8, "seconds", "호명 후 반응 지연", null),
            ok(TaskType.IMITATION, 6, 3.2, "seconds", "모방까지의 지연", ambient(32.0, 25.0)),
            ok(TaskType.BALL_PLAY, 5, 4.0, "count", "주고받은 횟수", ambient(35.0, 27.0)),
        )
    }

    private val SEED_NOTES = listOf(
        DoctorNote("seed-n1", "p1", "2026-11-11", "대근육 발달 양호. 좌우 균형 개선됨. 다음 세션에서 소근육 집중 권장.", false),
        DoctorNote("seed-n2", "p1", "2026-11-04", "지난 달보다 보호자 지향 시간이 늘었습니다.\n집에서 눈 맞춤 놀이 꾸준히 해주세요.", true),
        DoctorNote("seed-n3", "p1", "2026-10-10", "초진. 월령 대비 발달 지표 전반적으로 양호. 사회성 반응 시간 모니터링 시작.", false),
    )

    private val SEED_QUESTIONS = listOf("공을 주고받다가 자꾸 던지기만 해요.\n이런 행동도 괜찮은 걸까요?")
}

/** 교정 월령 (growthLens utils/ageCalc.ts). gestationalWeeks < 37이면 조산 보정 */
fun correctedAgeMonths(birthDate: String, gestationalWeeks: Int?): Int {
    val birth = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return 0
    val days = ChronoUnit.DAYS.between(birth, LocalDate.now())
    val raw = kotlin.math.floor(days / (365.25 / 12)).toInt()
    if (gestationalWeeks != null && gestationalWeeks < 37) {
        val correction = kotlin.math.floor((40 - gestationalWeeks) / 4.33).toInt()
        return maxOf(0, raw - correction)
    }
    return raw
}

/** 또래 참고값 (growthLens data/mockNorms.ts) — 그래프 아래에 글자로만 보여줘요 */
val PEER_REFERENCE: Map<TaskType, String> = mapOf(
    TaskType.NAME_RESPONSE to "참고 평균 3.62초 (전근아·김휘영 2025, 18~48개월)",
)

/**
 * 또래 지표 (변화 그래프의 노란 선). 월령(개월) → 값, 사이 월령은 선형 보간.
 * - 호명 반응 18개월 이상: 전근아·김휘영(2025) TD군 평균 3.62초
 * - 그 밖의 값(호명 반응 18개월 미만, 동작 모방, 공 주고받기)은 논문 값이 없어 넣은 **데모용 임시값**이에요.
 *   실제 참조 데이터가 생기면 이 표만 바꾸면 돼요.
 */
private val PEER_TABLE: Map<TaskType, List<Pair<Int, Double>>> = mapOf(
    TaskType.NAME_RESPONSE to listOf(6 to 5.5, 12 to 4.6, 18 to 3.62, 48 to 3.62), // 초 (짧을수록 빠름)
    TaskType.IMITATION to listOf(6 to 7.0, 9 to 6.0, 12 to 5.0, 18 to 4.0, 24 to 3.2, 36 to 2.6, 48 to 2.4), // 초
    TaskType.BALL_PLAY to listOf(6 to 0.0, 9 to 1.0, 12 to 2.0, 18 to 3.0, 24 to 4.0, 36 to 6.0, 48 to 7.0), // 회
)

fun peerNorm(task: TaskType, ageMonths: Int): Double? {
    val table = PEER_TABLE[task] ?: return null
    if (ageMonths <= table.first().first) return table.first().second
    if (ageMonths >= table.last().first) return table.last().second
    val (a, b) = table.zipWithNext().first { (lo, hi) -> ageMonths in lo.first..hi.first }
    val t = (ageMonths - a.first).toDouble() / (b.first - a.first)
    return ((a.second + (b.second - a.second) * t) * 100).roundToLong() / 100.0
}
