package com.example.bebeview.record.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.Ambient
import com.example.bebeview.record.data.AnalysisResult
import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// growthLens screens/result/RecordDetail.tsx 포팅
// 기록 한 건의 결과 본문. 촬영 직후 결과 화면과 기록 탭의 지난 기록 화면이 함께 써요.

private fun cal(ts: Long) = Calendar.getInstance().apply { timeInMillis = ts }

fun fmtDateTime(ts: Long): String {
    val c = cal(ts)
    val h = c.get(Calendar.HOUR_OF_DAY)
    return "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일 ${if (h < 12) "오전" else "오후"} " +
        "${if (h % 12 == 0) 12 else h % 12}:%02d".format(c.get(Calendar.MINUTE))
}

fun fmtDay(ts: Long): String {
    val c = cal(ts)
    return "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일"
}

/** 자바스크립트 숫자처럼 표시 (2.0 → "2", 3.62 → "3.62") */
fun fmtNum(v: Double): String = if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

private fun ratio(a: Ambient, v: Double) = if (a.bothInFrameSec > 0) (v / a.bothInFrameSec * 100).roundToInt() else 0

/** 과제 이름 칩 (Pill 을 h-7 px-4 13px 로 키운 것) */
@Composable
fun TaskPill(task: TaskType) {
    Box(
        Modifier.height(28.dp).background(Bebe.BrandChip, CircleShape).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(task.label, style = bebeText(13.sp, Bebe.BrandDeep, bold = true)) }
}

/**
 * @param recordedAt 이 기록의 시각 (없으면 날짜 줄을 숨김)
 * @param earlier 이 기록보다 앞선 성공 기록, 최근 것부터
 * @param currentLabel 비교 상자에서 이 기록을 부르는 이름 ("오늘", "11월 18일")
 * @param extra 측정값 카드 바로 아래에 넣을 내용 (결과 화면의 "AI가 본 장면")
 */
@Composable
fun RecordDetail(
    task: TaskType,
    title: String,
    result: AnalysisResult,
    recordedAt: Long?,
    earlier: List<MeasurementRecord>,
    currentLabel: String,
    extra: (@Composable () -> Unit)? = null,
) {
    val prev = earlier.firstOrNull()
    val metric = result.metric
    val unit = metric?.let { unitOf(it.unit) } ?: ""
    val prevAmbient = earlier.firstOrNull { it.result.ambient != null }?.result?.ambient

    Column {
        Text(title, style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))

        // 이번 기록
        Spacer(Modifier.height(11.dp))
        Row(
            Modifier.fillMaxWidth().height(115.dp).background(Bebe.Brand, RoundedCornerShape(18.dp)).padding(start = 28.dp, end = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                if (recordedAt != null) {
                    Text(fmtDateTime(recordedAt), style = bebeText(15.sp, Color.White, bold = true, lineHeight = 18.sp))
                    Spacer(Modifier.height(6.dp))
                }
                Text(metric?.label ?: "", style = bebeText(14.sp, Color.White, lineHeight = 20.sp))
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    metric?.let { fmtNum(it.value) } ?: "—",
                    style = bebeText(52.sp, Color.White, bold = true, lineHeight = 62.sp).copy(fontFeatureSettings = "tnum"),
                )
                Text(unit, style = bebeText(18.sp, Color.White, bold = true, lineHeight = 28.sp), modifier = Modifier.padding(bottom = 10.dp))
            }
        }

        extra?.invoke()

        // 지난 기록과 비교
        val prevMetric = prev?.result?.metric
        if (prev != null && prevMetric != null && metric != null) {
            Spacer(Modifier.height(34.dp))
            Text("지난 기록과 비교", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 24.sp))
            Spacer(Modifier.height(4.dp))
            Text(compareText(prevMetric.value, metric.value, metric.unit), style = bebeText(14.sp, Bebe.BrandDeep, bold = true, lineHeight = 20.sp))
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    Modifier.weight(1f).height(78.dp).background(Bebe.PCard, RoundedCornerShape(16.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(fmtDay(prev.recordedAt), style = bebeText(12.sp, Bebe.PGray, lineHeight = 14.sp))
                    Spacer(Modifier.height(6.dp))
                    Text("${fmtNum(prevMetric.value)}$unit", style = bebeText(20.sp, Bebe.PGray, bold = true, lineHeight = 28.sp))
                }
                Icon(BebeIcons.ChevronRight, null, tint = Bebe.BrandIcon, modifier = Modifier.size(20.dp))
                Column(
                    Modifier.weight(1f).height(78.dp).border(2.dp, Bebe.Brand, RoundedCornerShape(16.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(currentLabel, style = bebeText(12.sp, Bebe.BrandDeep, bold = true, lineHeight = 14.sp))
                    Spacer(Modifier.height(6.dp))
                    Text("${fmtNum(metric.value)}$unit", style = bebeText(20.sp, Bebe.PTitle, bold = true, lineHeight = 28.sp))
                }
            }
        }

        // 공통 지표
        result.ambient?.let { a ->
            Spacer(Modifier.height(38.dp))
            Text("함께 기록된 공통 지표", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp))
            Spacer(Modifier.height(4.dp))
            Text("모든 과제에서 측정", style = bebeText(13.sp, Bebe.PGray, lineHeight = 16.sp))
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmbientBox("보호자 지향 비율", ratio(a, a.caregiverOrientSec), prevAmbient?.let { ratio(it, it.caregiverOrientSec) }, Modifier.weight(1f))
                AmbientBox("활동량 비율", ratio(a, a.activitySec), prevAmbient?.let { ratio(it, it.activitySec) }, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(BebeIcons.Info, null, tint = Bebe.PLight, modifier = Modifier.size(16.dp))
            Text(
                "진단이 아닌 관찰 기록이에요. 궁금한 점은 진료 때 기록을\n보여주며 상담해 보세요.",
                style = bebeText(13.sp, Bebe.PGray, lineHeight = 16.sp),
            )
        }
    }
}

/** 지난번 대비 문구. 시간은 짧을수록, 횟수는 많을수록 좋은 방향 */
private fun compareText(prev: Double, now: Double, unit: String): String {
    val diff = (abs(now - prev) * 10).roundToInt() / 10.0
    if (diff == 0.0) return "지난번과 같아요"
    return if (unit == "seconds") "지난번보다 ${fmtNum(diff)}초 ${if (now < prev) "짧아졌어요" else "길어졌어요"}"
    else "지난번보다 ${fmtNum(diff)}회 ${if (now > prev) "늘었어요" else "줄었어요"}"
}

@Composable
private fun AmbientBox(label: String, now: Int, prev: Int?, modifier: Modifier) {
    Column(
        modifier.height(105.dp).border(1.dp, Bebe.PLine, RoundedCornerShape(16.dp)).padding(start = 22.dp, end = 14.dp, top = 16.dp),
    ) {
        Text(label, style = bebeText(13.sp, Bebe.PGray, lineHeight = 16.sp))
        Spacer(Modifier.height(9.dp))
        Text("$now%", style = bebeText(22.sp, Bebe.PTitle, bold = true, lineHeight = 26.sp))
        if (prev != null) {
            Spacer(Modifier.height(8.dp))
            Text("지난번 $prev%", style = bebeText(12.sp, Bebe.PGray, lineHeight = 14.sp))
        }
    }
}
