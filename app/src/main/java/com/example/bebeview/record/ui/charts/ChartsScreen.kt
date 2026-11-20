package com.example.bebeview.record.ui.charts

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.TaskType
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

// growthLens src/screens/charts/index.tsx 포팅
// Figma 부모 버전 "변화". 기록이 3회 미만이면 칸 3개 진행 막대, 3회 이상이면 꺾은선 그래프

private const val NEED = 3

private fun median(arr: List<Double>): Double {
    val s = arr.sorted()
    val m = s.size / 2
    return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2
}

/** 최근 3개 중앙값으로 흔들림을 줄임 */
private fun rolling(v: List<Double>) = v.indices.map { i -> median(v.subList(maxOf(0, i - 2), i + 1)) }

@Composable
fun ChartsScreen() {
    val nav = LocalNav.current
    val sorted = AppData.records.sortedBy { it.recordedAt }

    val ambientRecs = sorted.filter { it.result.ambient != null }
    val commons = listOf(
        Triple("보호자 지향 비율", "보호자를 바라보거나 다가간 시간", ambientRecs.map { ambientRatio(it, orient = true) }),
        Triple("활동량 비율", "촬영 중 움직인 시간", ambientRecs.map { ambientRatio(it, orient = false) }),
    )

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(64.dp), bottom = 24.dp),
        ) {
            Text("변화", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))

            if (ambientRecs.size < NEED) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(103.dp)
                        .background(Bebe.BrandSoft, RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(BebeIcons.Trend, contentDescription = null, tint = Bebe.BrandDeep, modifier = Modifier.size(30.dp))
                    Column {
                        Text(
                            if (ambientRecs.isEmpty()) "첫 기록을 남겨보세요!" else "첫 기록을 남겼어요!",
                            style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 24.sp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "일주일에 한 번씩 찍으면 3주 뒤부터\n변화 그래프를 볼 수 있어요",
                            style = bebeText(13.sp, Bebe.PBody, lineHeight = 20.sp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("1군 · 공통 지표")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                commons.forEach { (label, desc, values) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Bebe.PCard, RoundedCornerShape(16.dp))
                            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
                    ) {
                        Text(label, style = bebeText(15.sp, Bebe.PTitle, bold = true))
                        Spacer(Modifier.height(6.dp))
                        Text(desc, style = bebeText(12.sp, Bebe.PGray, lineHeight = 16.sp))
                        if (values.size < NEED) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(NEED) { i ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .background(if (i < values.size) Bebe.Brand else Color(0xFFDADDE1), CircleShape),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                buildAnnotatedString {
                                    append("기록이 3회 쌓이면 변화를 보여드려요 · ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Bebe.BrandDeep)) { append("현재 ${values.size}회") }
                                },
                                style = bebeText(13.sp, Bebe.PBody),
                            )
                        } else {
                            LineChart(rolling(values))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("2군 · 과제 지표")
            Spacer(Modifier.height(10.dp))
            Divider(Bebe.PLine)
            TaskType.ORDER.forEach { task ->
                val recs = sorted.filter { it.taskType == task.apiName && it.result.isSuccess && it.result.metric != null }
                val last = recs.lastOrNull()
                val sameDay = if (last != null) recs.filter { fmtDay(it.recordedAt) == fmtDay(last.recordedAt) } else emptyList()
                Row(Modifier.fillMaxWidth().height(65.dp), verticalAlignment = Alignment.CenterVertically) {
                    TaskIllust(task, 43.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(task.label, style = bebeText(15.sp, Bebe.PTitle, bold = true))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (last != null) {
                                "${fmtDay(last.recordedAt)} · " + sameDay.joinToString(", ") { r ->
                                    val m = r.result.metric!!
                                    "${jsNum(m.value)}${unitOf(m.unit)}"
                                }
                            } else "기록 없음",
                            style = bebeText(12.sp, Bebe.PGray, lineHeight = 16.sp),
                        )
                    }
                    if (last == null) {
                        Box(
                            Modifier
                                .height(34.dp)
                                .background(Bebe.PCard, CircleShape)
                                .clickable(role = Role.Button) { nav.go(Route.TaskGuide(task)) }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("찍으러 가기", style = bebeText(13.sp, Bebe.PBody, bold = true)) }
                    }
                }
                Divider(Bebe.PLine)
            }
        }
        ParentNav(ParentTab.Charts) { nav.openTab(it) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 24.sp), modifier = Modifier.padding(horizontal = 10.dp))
}

/** 꺾은선 그래프. 웹 SVG(viewBox 320×72, 높이 72, 가로 가득)와 같은 비율로 가운데 맞춰 그림 */
@Composable
private fun LineChart(values: List<Double>) {
    val w = 320f
    val h = 72f
    val pad = 8f
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it != 0.0 } ?: 1.0
    val pts = values.mapIndexed { i, v ->
        Offset(
            pad + (i.toFloat() / (values.size - 1)) * (w - pad * 2),
            (h - pad - ((v - min) / range) * (h - pad * 2)).toFloat(),
        )
    }
    val last = values.last()
    Column(Modifier.padding(top = 12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            // preserveAspectRatio="xMidYMid meet" 와 같게
            val s = minOf(size.width / w, size.height / h)
            val ox = (size.width - w * s) / 2
            val oy = (size.height - h * s) / 2
            fun p(o: Offset) = Offset(ox + o.x * s, oy + o.y * s)
            val path = Path().apply {
                pts.forEachIndexed { i, o -> val q = p(o); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) }
            }
            drawPath(path, Bebe.Brand, style = Stroke(2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            pts.forEach { o ->
                drawCircle(Color.White, 3.5f * s, p(o))
                drawCircle(Bebe.Brand, 3.5f * s, p(o), style = Stroke(2f * s))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                append("최근 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Bebe.BrandDeep)) { append("${jsRound(last)}%") }
                append(" · 기록 ${values.size}회")
            },
            style = bebeText(13.sp, Bebe.PBody),
        )
    }
}

