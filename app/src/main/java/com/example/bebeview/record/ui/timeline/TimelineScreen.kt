package com.example.bebeview.record.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.record.ui.AppNav
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.ParentNav
import com.example.bebeview.ui.components.ParentTab
import com.example.bebeview.ui.components.TaskIllust
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// growthLens src/screens/timeline/index.tsx 포팅 — Figma 부모 버전 "기록"

/** 필터: null = 전체 */
private val FILTERS: List<TaskType?> = listOf(null, TaskType.NAME_RESPONSE, TaskType.IMITATION, TaskType.BALL_PLAY)
private val WEEKDAY = listOf("월", "화", "수", "목", "금", "토", "일") // DayOfWeek.value 1=월 … 7=일

private fun dateTime(ts: Long): LocalDateTime = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDateTime()

private fun dayLabel(ts: Long, today: LocalDate): String {
    val d = dateTime(ts).toLocalDate()
    val base = "${d.monthValue}월 ${d.dayOfMonth}일 (${WEEKDAY[d.dayOfWeek.value - 1]})"
    return if (d == today) "$base · 오늘" else base
}

private fun timeLabel(ts: Long): String {
    val d = dateTime(ts)
    val h = d.hour
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "${if (h < 12) "오전" else "오후"} $h12:${"%02d".format(d.minute)}"
}

/** 숫자를 웹(JS)처럼 표시: 2.0 → "2", 3.6 → "3.6" */
private fun fmtValue(v: Double): String = if (v == kotlin.math.floor(v)) v.toLong().toString() else v.toString()

private fun goTab(nav: AppNav, tab: ParentTab) = nav.tab(
    when (tab) {
        ParentTab.Tasks -> Route.Tasks
        ParentTab.Timeline -> Route.Timeline
        ParentTab.Charts -> Route.Charts
        ParentTab.Summary -> Route.ClinicSummary
    },
)

@Composable
fun TimelineScreen() {
    val nav = LocalNav.current
    val records = AppData.records
    var filterIdx by rememberSaveable { mutableStateOf(0) }
    val filter = FILTERS[filterIdx]
    val today = remember { LocalDate.now() }

    val shown = records
        .filter { filter == null || it.taskType == filter.apiName }
        .sortedByDescending { it.recordedAt }
    // 날짜별로 묶기 (최신 날짜가 위)
    val groups = shown.groupBy { dateTime(it.recordedAt).toLocalDate() }.toList()

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(64.dp), bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("기록", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))
                Spacer(Modifier.weight(1f))
                Text("총 ${records.size}회", style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp))
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FILTERS.forEachIndexed { i, f ->
                    val on = i == filterIdx
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        Modifier
                            .height(37.dp)
                            .background(if (on) Bebe.Brand else Color.White, shape)
                            .then(if (on) Modifier else Modifier.border(1.dp, Bebe.PLine, shape))
                            .clickable(role = Role.Tab) { filterIdx = i }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(f?.label ?: "전체", style = bebeText(14.sp, if (on) Color.White else Bebe.PBody, bold = on, lineHeight = 20.sp))
                    }
                }
            }

            if (groups.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("아직 기록이 없어요", style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp))
                    Box(
                        Modifier
                            .height(46.dp)
                            .background(Bebe.Brand, RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { nav.tab(Route.Tasks) }
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("과제 하러 가기", style = bebeText(14.sp, Color.White, bold = true, lineHeight = 20.sp)) }
                }
            } else {
                groups.forEach { (_, items) ->
                    Spacer(Modifier.height(18.dp))
                    Text(dayLabel(items.first().recordedAt, today), style = bebeText(13.sp, Bebe.PGray, bold = true))
                    Spacer(Modifier.height(21.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(27.dp)) {
                        items.forEach { r -> RecordRow(r, records, nav) }
                    }
                }
            }
        }
        ParentNav(ParentTab.Timeline) { goTab(nav, it) }
    }
}

@Composable
private fun RecordRow(r: MeasurementRecord, all: List<MeasurementRecord>, nav: AppNav) {
    val task = TaskType.fromApi(r.taskType)
    val ok = r.result.isSuccess
    // 같은 과제를 이 기록 뒤에 다시 찍었으면 재촬영은 끝난 것
    val retaken = !ok && all.any { it.taskType == r.taskType && it.recordedAt > r.recordedAt }
    val metric = r.result.metric
    Row(
        Modifier
            .fillMaxWidth()
            // 재촬영 필요한 기록은 그 과제를 바로 다시 시작. 이미 다시 찍었으면 누를 수 없음
            .clickable(enabled = !retaken, role = Role.Button) {
                nav.go(if (ok) Route.Replay(r.id) else Route.TaskGuide(task))
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskIllust(task, size = 43.dp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(task.label, style = bebeText(15.sp, Bebe.PTitle, bold = true))
            Spacer(Modifier.height(4.dp))
            Text(timeLabel(r.recordedAt), style = bebeText(12.sp, Bebe.PLight, lineHeight = 16.sp))
        }
        when {
            ok && metric != null -> Text(
                "${fmtValue(metric.value)}${unitOf(metric.unit)}",
                style = bebeText(17.sp, Bebe.PTitle, bold = true).copy(fontFeatureSettings = "tnum"),
            )
            retaken -> StatusChip("다시 찍었어요", Bebe.PCard, Bebe.PLight)
            else -> StatusChip("재촬영 필요", Bebe.WarnBg, Bebe.WarnText)
        }
    }
}

@Composable
private fun StatusChip(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.height(34.dp).background(bg, CircleShape).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = bebeText(14.sp, fg, bold = true, lineHeight = 20.sp)) }
}
