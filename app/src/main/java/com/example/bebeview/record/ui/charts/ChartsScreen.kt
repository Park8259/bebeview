package com.example.bebeview.record.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.peerNorm
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.Divider
import com.example.bebeview.ui.components.ParentNav
import com.example.bebeview.ui.components.ParentTab
import com.example.bebeview.ui.components.TaskIllust
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Figma 해커톤 Page 3 "변화"
// 과제마다 기록이 3회 미만이면 칸 3개 진행 막대 카드, 3회 이상이면 측정값·또래 지표 꺾은선 그래프 카드

private const val NEED = 3

/** 변화 카드에 쓰는 과제 설명 (Figma 문구 그대로) */
private fun TaskType.changeDesc() = when (this) {
    TaskType.NAME_RESPONSE -> "보호자를 바라보거나 다가간 시간"
    TaskType.IMITATION -> "촬영 중 움직인 시간"
    TaskType.BALL_PLAY -> "보호자와 공을 주고받은 횟수"
}

/** 기록 당시의 교정 월령 (AppData.correctedAgeMonths 와 같은 계산, 기준일만 기록 날짜) */
private fun ageMonthsAt(ts: Long): Int? {
    val p = AppData.profile.value ?: return null
    val birth = runCatching { LocalDate.parse(p.birthDate) }.getOrNull() ?: return null
    val at = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
    val raw = kotlin.math.floor(ChronoUnit.DAYS.between(birth, at) / (365.25 / 12)).toInt()
    val w = p.gestationalWeeks
    return if (w != null && w < 37) maxOf(0, raw - kotlin.math.floor((40 - w) / 4.33).toInt()) else raw
}

@Composable
fun ChartsScreen() {
    val nav = LocalNav.current
    val sorted = AppData.records.sortedBy { it.recordedAt }
    val byTask: Map<TaskType, List<MeasurementRecord>> = TaskType.ORDER.associateWith { task ->
        sorted.filter { it.taskType == task.apiName && it.result.isSuccess && it.result.metric != null }
    }
    val charts = TaskType.ORDER.filter { byTask.getValue(it).size >= NEED }
        .associateWith { task -> chartData(task, byTask.getValue(task)) }
    val anyPeer = charts.values.any { d -> d.peer.any { it != null } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(63.dp), bottom = 24.dp),
        ) {
            Text("변화", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 29.sp), modifier = Modifier.padding(start = 3.dp))

            if (charts.isEmpty()) {
                // 첫 기록 안내 + 과제 지표 (모두 3회 미만)
                Spacer(Modifier.height(19.dp))
                FirstRecordCard(hasAny = sorted.isNotEmpty())
                Spacer(Modifier.height(23.dp))
                Text("과제 지표", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 19.sp), modifier = Modifier.padding(start = 5.dp))
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskType.ORDER.forEach { ProgressCard(it, byTask.getValue(it).size) }
                }
            } else {
                // 범례 + 과제별 그래프 (3회 미만인 과제는 진행 막대 카드)
                Spacer(Modifier.height(37.dp))
                Legend(anyPeer)
                Spacer(Modifier.height(19.dp))
                Column(verticalArrangement = Arrangement.spacedBy(25.dp)) {
                    TaskType.ORDER.forEach { task ->
                        val d = charts[task]
                        if (d != null) ChartCard(task, d) else ProgressCard(task, byTask.getValue(task).size)
                    }
                }
            }

            Spacer(Modifier.height(if (charts.isEmpty()) 20.dp else 45.dp))
            TaskType.ORDER.forEachIndexed { i, task ->
                if (i > 0) Divider(Bebe.PLine)
                TaskRow(task, byTask.getValue(task)) { nav.go(Route.TaskGuide(task)) }
            }
        }
        ParentNav(ParentTab.Charts) { nav.openTab(it) }
    }
}

@Composable
private fun FirstRecordCard(hasAny: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(103.dp)
            .background(Bebe.BrandSoft, RoundedCornerShape(16.dp))
            .padding(start = 28.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        Icon(BebeIcons.Trend, contentDescription = null, tint = Bebe.BrandDeep, modifier = Modifier.size(30.dp))
        Column {
            Text(
                if (hasAny) "첫 기록을 남겼어요!" else "첫 기록을 남겨보세요!",
                style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 19.sp),
            )
            Spacer(Modifier.height(10.dp))
            Text("일주일에 한 번씩 찍으면 3주 뒤부터\n변화 그래프를 볼 수 있어요", style = bebeText(13.sp, Bebe.PBody, lineHeight = 16.sp))
        }
    }
}

@Composable
private fun ProgressCard(task: TaskType, count: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Bebe.PCard, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 17.dp),
    ) {
        Text(task.label, style = bebeText(15.sp, Bebe.PTitle, bold = true, lineHeight = 18.sp))
        Spacer(Modifier.height(5.dp))
        Text(task.changeDesc(), style = bebeText(12.sp, Bebe.PGray, lineHeight = 14.sp))
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(NEED) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(if (i < count) Bebe.Brand else Color(0xFFDADDE1), CircleShape),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("기록이 3회 쌓이면 변화를 보여드려요", style = bebeText(13.sp, Bebe.PBody, lineHeight = 16.sp))
    }
}

@Composable
private fun Legend(showPeer: Boolean) {
    Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LegendItem(ChartColors.Measured, "측정값")
        if (showPeer) LegendItem(ChartColors.Peer, "또래 지표")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(22.dp).height(4.dp).background(color, CircleShape))
        Spacer(Modifier.width(11.dp))
        Text(label, style = bebeText(10.sp, Color(0xFF647487), lineHeight = 15.sp))
    }
}

@Composable
private fun TaskRow(task: TaskType, recs: List<MeasurementRecord>, onGo: () -> Unit) {
    val last = recs.lastOrNull()
    val sameDay = if (last != null) recs.filter { fmtDay(it.recordedAt) == fmtDay(last.recordedAt) } else emptyList()
    val doneToday = last != null && fmtDay(last.recordedAt) == fmtDay(System.currentTimeMillis()) &&
        cal(last.recordedAt).get(java.util.Calendar.YEAR) == cal(System.currentTimeMillis()).get(java.util.Calendar.YEAR)
    Row(Modifier.fillMaxWidth().height(65.dp), verticalAlignment = Alignment.CenterVertically) {
        TaskIllust(task, 43.dp, Modifier.padding(start = 4.dp))
        Column(Modifier.weight(1f).padding(start = 11.dp)) {
            Text(task.label, style = bebeText(15.sp, Bebe.PTitle, bold = true, lineHeight = 18.sp))
            Spacer(Modifier.height(5.dp))
            Text(
                if (last != null) {
                    "${fmtDay(last.recordedAt)} · " + sameDay.joinToString(", ") { r ->
                        val m = r.result.metric!!
                        "${jsNum(m.value)}${unitOf(m.unit)}"
                    }
                } else "기록 없음",
                style = bebeText(12.sp, Bebe.PGray, lineHeight = 14.sp),
            )
        }
        if (doneToday) {
            Box(
                Modifier.height(35.dp).background(Bebe.PCard, CircleShape).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("완료", style = bebeText(13.sp, Color(0xFF494949), bold = true, lineHeight = 16.sp)) }
        } else {
            Box(
                Modifier
                    .padding(end = 2.dp)
                    .height(34.dp)
                    .width(92.dp)
                    .background(Bebe.PCard, RoundedCornerShape(38.dp))
                    .clickable(role = Role.Button, onClick = onGo),
                contentAlignment = Alignment.Center,
            ) { Text("찍으러 가기", style = bebeText(13.sp, Bebe.PBody, bold = true, lineHeight = 16.sp)) }
        }
    }
}

/** 그래프에 그릴 최근 기록 (최대 MAX_POINTS 개) */
private fun chartData(task: TaskType, recs: List<MeasurementRecord>): ChartData {
    val shown = recs.takeLast(MAX_POINTS)
    return ChartData(
        values = shown.map { it.result.metric!!.value },
        peer = shown.map { r -> ageMonthsAt(r.recordedAt)?.let { peerNorm(task, it) } },
        total = recs.size,
        firstIndex = recs.size - shown.size + 1,
    )
}
