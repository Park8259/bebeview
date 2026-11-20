package com.example.bebeview.record.ui.recording

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.bottomSpace
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlinx.coroutines.delay

// growthLens recording/index.tsx 의 작은 부품: 점검 줄 · 데모 파일 버튼 · 분석 화면

/** 점검 목록 한 줄 (밝기, 전신, 공) */
@Composable
fun CheckRow(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(22.dp).background(if (ok) Bebe.Brand else Bebe.PLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(BebeIcons.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
        Text(text, style = bebeText(15.sp, Color.White))
    }
}

/** 데모용 "영상 파일로 분석" 버튼 (작게, 눈에 덜 띄게) */
@Composable
fun FilePickButton(onClick: () -> Unit) {
    Text(
        "영상 파일로 분석 (데모)",
        style = bebeText(13.sp, Bebe.PLight).merge(TextStyle(textDecoration = TextDecoration.Underline)),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** 영상 분석 화면. 단계 표시는 시간에 맞춰 넘어가고, 마지막 단계는 결과가 나올 때까지 진행 중으로 둬요 */
@Composable
fun AnalyzingView(task: TaskType) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(2500); step = 1
        delay(3000); step = 2
    }
    val steps = listOf("아이 찾기", task.analyzeStep, "시간 계산하기")
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(topSpace(214.dp)))
            Text("영상을 보고 있어요", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))
            Spacer(Modifier.height(8.dp))
            Text("10초 정도 걸려요", style = bebeText(16.sp, Bebe.PGray, lineHeight = 24.sp))
            Spacer(Modifier.height(50.dp))
            // 96×96, 반지름 40, 굵기 8: 연한 원 + 파란 호
            Canvas(Modifier.size(96.dp).rotate(spin)) {
                val s = size.width / 96f
                val r = 40f * s
                val stroke = 8f * s
                val topLeft = Offset(center.x - r, center.y - r)
                drawCircle(Bebe.BrandSoft, radius = r, style = Stroke(stroke))
                // 웹 경로 "M48 8 a40 40 0 0 1 38 28" ≈ 12시에서 시계방향 72도
                drawArc(Bebe.Brand, -90f, 72f, false, topLeft, Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(67.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                steps.forEachIndexed { i, label ->
                    val done = i < step
                    val current = i == step
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Box(
                            Modifier.size(22.dp).background(
                                when {
                                    done -> Bebe.Brand
                                    current -> Bebe.Brand.copy(alpha = 0.6f)
                                    else -> Bebe.PFaint
                                },
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) { Icon(BebeIcons.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                        Text(
                            label,
                            style = bebeText(16.sp, if (done || current) Bebe.PTitle else Bebe.PFaint, bold = done || current, lineHeight = 24.sp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = bottomSpace(45.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(BebeIcons.Lock, null, tint = Bebe.PLight, modifier = Modifier.size(16.dp))
            Text("영상은 이 폰 안에서만 분석돼요", style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp))
        }
    }
}
