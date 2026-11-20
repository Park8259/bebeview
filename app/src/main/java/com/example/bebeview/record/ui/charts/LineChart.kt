package com.example.bebeview.record.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.ui.components.Divider
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlin.math.ceil

// Figma "변화" 그래프 카드: 측정값(파랑)과 또래 지표(노랑) 꺾은선 + 아래로 옅은 그라데이션

internal const val MAX_POINTS = 6

internal object ChartColors {
    val Measured = Color(0xFF009BD1)
    val Peer = Color(0xFFFEDC00)
    val Dot = Color(0xFF069FCF)
    val Grid = Color(0xFFDBE3E9)
    val Axis = Color(0xFF9AA6AF)
    val Border = Color(0xFFE0E5E9)
    val Footer = Color(0xFF8D969F)
}

/** values: 그릴 측정값, peer: 같은 시점의 또래 기준(없으면 null), firstIndex: 첫 점이 몇 번째 기록인지(1부터) */
internal class ChartData(val values: List<Double>, val peer: List<Double?>, val total: Int, val firstIndex: Int)

/** 0 부터 6칸으로 나눌 보기 좋은 간격 */
private fun niceStep(max: Double): Double {
    val raw = max / 6
    val steps = listOf(0.5, 1.0, 2.0, 2.5, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0)
    return steps.firstOrNull { it >= raw } ?: ceil(raw / 100) * 100
}

@Composable
internal fun ChartCard(task: TaskType, d: ChartData) {
    val measurer = rememberTextMeasurer()
    val maxV = (d.values + d.peer.filterNotNull()).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val step = niceStep(maxV)
    val top = step * 6
    val axisStyle = TextStyle(fontSize = 10.8.sp, color = ChartColors.Axis)

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, ChartColors.Border, RoundedCornerShape(20.dp))
            .padding(start = 18.dp, end = 16.dp, top = 22.dp, bottom = 22.dp),
    ) {
        Text(
            task.label,
            style = bebeText(18.sp, Color(0xFF17202A), bold = true, lineHeight = 21.sp),
            modifier = Modifier.padding(start = 2.dp),
        )
        Spacer(Modifier.height(36.dp))
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val plotL = 27.dp.toPx()
            val plotT = 6.dp.toPx()
            val plotB = plotT + 142.dp.toPx()
            val plotR = size.width
            val inset = 13.dp.toPx()
            val n = d.values.size
            fun x(i: Int) = if (n == 1) (plotL + plotR) / 2 else plotL + inset + i * (plotR - plotL - inset * 2) / (n - 1)
            fun y(v: Double) = (plotB - (v / top) * (plotB - plotT)).toFloat()

            // 가로 눈금선 + y 라벨
            for (k in 0..6) {
                val yy = y(step * k)
                drawLine(ChartColors.Grid, Offset(plotL, yy), Offset(plotR, yy), 1.dp.toPx())
                val lbl = measurer.measure(jsNum(step * k), axisStyle)
                drawText(lbl, topLeft = Offset(plotL - 9.dp.toPx() - lbl.size.width, yy - lbl.size.height / 2f))
            }
            // x 라벨 (1회, 2회 …)
            for (i in 0 until n) {
                val lbl = measurer.measure("${d.firstIndex + i}회", axisStyle)
                drawText(lbl, topLeft = Offset(x(i) - lbl.size.width / 2f, plotB + 12.dp.toPx()))
            }

            val measured = d.values.indices.map { Offset(x(it), y(d.values[it])) }
            // 또래 지표는 값이 있는 구간끼리만 이어요
            val peerRuns = mutableListOf<MutableList<Offset>>()
            d.peer.forEachIndexed { i, v ->
                if (v == null) {
                    if (peerRuns.lastOrNull()?.isNotEmpty() == true) peerRuns.add(mutableListOf())
                } else {
                    if (peerRuns.isEmpty()) peerRuns.add(mutableListOf())
                    peerRuns.last().add(Offset(x(i), y(v)))
                }
            }
            val peers = peerRuns.filter { it.isNotEmpty() }

            area(measured, plotB, Brush.verticalGradient(
                listOf(Color(0xFF78E2FF).copy(alpha = 0.6f), Color(0xFFECF9FD).copy(alpha = 0.08f)),
                startY = measured.minOf { it.y }, endY = plotB,
            ))
            peers.forEach { run ->
                area(run, plotB, Brush.verticalGradient(
                    listOf(Color(0xFFFFEE7C), Color(0xFFECF9FD).copy(alpha = 0.08f)),
                    startY = run.minOf { it.y }, endY = plotB,
                ))
            }
            peers.forEach { line(it, ChartColors.Peer) }
            line(measured, ChartColors.Dot)
            measured.forEach { dot(it, ChartColors.Dot) }
            peers.forEach { run -> run.forEach { dot(it, ChartColors.Peer) } }
        }
        Spacer(Modifier.height(10.dp))
        Divider(Color(0xFFE2E7EB))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text("총 ${d.total}회 · ${d.values.size}시점 표시", style = bebeText(11.sp, ChartColors.Footer, lineHeight = 13.sp))
        }
    }
}

private fun DrawScope.line(pts: List<Offset>, color: Color) {
    if (pts.size < 2) return
    val path = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
    drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.area(pts: List<Offset>, baseY: Float, brush: Brush) {
    if (pts.size < 2) return
    val path = Path().apply {
        moveTo(pts.first().x, baseY)
        pts.forEach { lineTo(it.x, it.y) }
        lineTo(pts.last().x, baseY)
        close()
    }
    drawPath(path, brush)
}

private fun DrawScope.dot(o: Offset, color: Color) {
    drawCircle(Color.White, 6.5.dp.toPx(), o)
    drawCircle(color, 5.5.dp.toPx(), o)
}
