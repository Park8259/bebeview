package com.example.bebeview.record.ui.result

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bebeview.record.data.FaceTrace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// growthLens screens/result/TraceReplay.tsx 포팅
// "AI가 본 장면": 방금 찍은 영상 위에 분석 때 뽑은 얼굴 윤곽과 고개 방향을 다시 그려요.
// 좌표는 분석 때 이미 뽑은 것을 그대로 쓰고, AI를 다시 돌리지 않아요.

private const val OUTLINE_POINTS = 36 // pts 의 앞 36점이 얼굴 윤곽, 마지막 1점이 코끝
private const val EVENT_SEC = 1.5     // "이름을 불렀어요" 같은 알림을 띄워 두는 시간

private fun fmt(sec: Double): String {
    val s = max(0, sec.toInt())
    return "${s / 60}:%02d".format(s % 60)
}

@Composable
fun TraceReplay(video: File, trace: FaceTrace) {
    val frames = trace.frames
    val stimulusSec = trace.stimulusSec
    val responseSec = trace.responseSec
    val step = if (frames.size > 1) frames[1].t - frames[0].t else 1.0 / 15

    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var playing by remember { mutableStateOf(false) }
    var time by remember { mutableDoubleStateOf(0.0) }
    var duration by remember { mutableDoubleStateOf(0.0) }
    // 영상이 실제로 놓인 위치 (VideoView 는 비율을 지켜 가운데 놓여요 = object-contain)
    var rect by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f, 0f)) }

    fun frameAt(t: Double) = frames.getOrNull(
        ((t - (frames.firstOrNull()?.t ?: 0.0)) / step).roundToInt().coerceIn(0, maxOf(0, frames.size - 1)),
    )

    fun toggle() {
        val v = videoView ?: return
        if (v.isPlaying) { v.pause(); playing = false } else { v.start(); playing = true }
    }

    fun seek(sec: Double, play: Boolean = false) {
        val v = videoView ?: return
        val target = sec.coerceIn(0.0, maxOf(0.0, (if (duration > 0) duration else sec) - 0.05))
        v.seekTo((target * 1000).toInt())
        time = target
        if (play) { v.start(); playing = true }
    }

    // 재생 중에는 매 화면마다 현재 시각을 읽어 다시 그림
    LaunchedEffect(playing) {
        while (playing) {
            withFrameNanos { }
            videoView?.let { time = it.currentPosition / 1000.0 }
        }
    }
    DisposableEffect(Unit) { onDispose { videoView?.stopPlayback() } }

    val event = when {
        responseSec != null && time >= responseSec && time < responseSec + EVENT_SEC ->
            "고개를 돌렸어요 · ${fmtNum(((responseSec - stimulusSec) * 10).roundToInt() / 10.0)}초"
        time >= stimulusSec && time < stimulusSec + EVENT_SEC -> "이름을 불렀어요"
        else -> null
    }

    Column(Modifier.padding(top = 34.dp)) {
        Text("AI가 본 장면", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 24.sp))
        Spacer(Modifier.height(6.dp))
        Text("얼굴 윤곽과 고개 방향을 영상 위에 표시했어요. 영상은 이 휴대폰에만 있어요.", style = bebeText(13.sp, Bebe.PGray))

        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Bebe.PDark),
        ) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        val v = VideoView(ctx)
                        addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
                        v.setVideoPath(video.absolutePath)
                        v.setOnPreparedListener { mp ->
                            duration = mp.duration / 1000.0
                            v.seekTo(1) // 첫 화면 보이기
                        }
                        v.setOnCompletionListener { playing = false }
                        v.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
                            rect = floatArrayOf(l.toFloat(), t.toFloat(), (r - l).toFloat(), (b - t).toFloat())
                        }
                        videoView = v
                    }
                },
                modifier = Modifier.fillMaxSize().clickable(role = Role.Button) { toggle() },
            )

            // 얼굴 윤곽 + 고개 방향 화살표
            Canvas(Modifier.fillMaxSize()) {
                val (ox, oy, w, h) = rect.let { listOf(it[0], it[1], it[2], it[3]) }
                if (w <= 0f || h <= 0f) return@Canvas
                val f = frameAt(time) ?: return@Canvas
                val pts = f.pts ?: return@Canvas
                if (pts.size < (OUTLINE_POINTS + 1) * 2) return@Canvas
                fun px(k: Int) = ox + pts[k * 2] * w
                fun py(k: Int) = oy + pts[k * 2 + 1] * h
                val responding = responseSec != null && time >= responseSec && time < responseSec + EVENT_SEC
                val color = if (responding) Bebe.BrandDeep else Bebe.Brand
                val d = density

                val outline = Path().apply {
                    moveTo(px(0), py(0))
                    for (k in 1 until OUTLINE_POINTS) lineTo(px(k), py(k))
                    close()
                }
                drawPath(outline, color, style = Stroke((if (responding) 4f else 2.5f) * d, join = StrokeJoin.Round))

                var cx = 0f
                var cy = 0f
                for (k in 0 until OUTLINE_POINTS) { cx += px(k); cy += py(k) }
                cx /= OUTLINE_POINTS
                cy /= OUTLINE_POINTS
                val nx = px(OUTLINE_POINTS)
                val ny = py(OUTLINE_POINTS)
                val dx = nx - cx
                val dy = ny - cy
                val len = hypot(dx, dy)
                if (len > 1f) {
                    val reach = max(28f * d, len * 3)
                    val ex = nx + dx / len * reach
                    val ey = ny + dy / len * reach
                    drawLine(color, Offset(nx, ny), Offset(ex, ey), 3f * d, cap = StrokeCap.Butt)
                    val a = atan2(ey - ny, ex - nx)
                    val head = 11f * d
                    drawPath(
                        Path().apply {
                            moveTo(ex, ey)
                            lineTo(ex - head * cos(a - 0.5f), ey - head * sin(a - 0.5f))
                            lineTo(ex - head * cos(a + 0.5f), ey - head * sin(a + 0.5f))
                            close()
                        },
                        color,
                    )
                }
                drawCircle(Color.White, 4f * d, Offset(nx, ny))
            }

            // 위: 지금 고개 각도
            val yaw = frameAt(time)?.yaw
            Box(
                Modifier.padding(12.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    if (yaw != null) "고개 각도 ${abs(yaw).roundToInt()}°" else "얼굴을 찾는 중",
                    style = bebeText(12.sp, Color.White, bold = true, lineHeight = 16.sp).copy(fontFeatureSettings = "tnum"),
                )
            }
            // 아래: 이름 부름 / 돌아봄 알림
            if (event != null) {
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                        .shadow(3.dp, CircleShape).background(Color.White, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(event, style = bebeText(14.sp, Bebe.BrandDeep, bold = true, lineHeight = 20.sp)) }
            }
            if (!playing) {
                Box(
                    Modifier.align(Alignment.Center).size(56.dp).background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable(role = Role.Button) { toggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(22.dp)) {
                        val s = size.width / 22f
                        drawPath(
                            Path().apply { moveTo(8f * s, 5f * s); lineTo(18f * s, 11f * s); lineTo(8f * s, 17f * s); close() },
                            Color(0xFF111111),
                        )
                    }
                }
            }
        }

        // 시간 막대: 이름 부름·반응 시점 표시, 누르면 그 위치로
        fun frac(sec: Double) = if (duration > 0) (sec / duration).coerceIn(0.0, 1.0).toFloat() else 0f
        BoxWithConstraints(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(14.dp)
                .pointerInput(duration) {
                    detectTapGestures { o -> if (duration > 0) seek(o.x / size.width * duration) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val full = maxWidth
            Box(Modifier.fillMaxWidth().height(6.dp).background(Bebe.PLine, CircleShape)) {
                Box(Modifier.fillMaxHeight().width(full * frac(time)).background(Bebe.Brand, CircleShape))
            }
            Marker(Bebe.PGray, full * frac(stimulusSec))
            if (responseSec != null) Marker(Bebe.BrandDeep, full * frac(responseSec))
        }
        Row(
            Modifier.padding(top = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(Bebe.PGray, "이름 부름")
                if (responseSec != null) Legend(Bebe.BrandDeep, "돌아봄")
            }
            Text("${fmt(time)} / ${fmt(duration)}", style = bebeText(12.sp, Bebe.PGray, lineHeight = 16.sp).copy(fontFeatureSettings = "tnum"))
        }

        Box(
            Modifier.padding(top = 14.dp).fillMaxWidth().height(44.dp)
                .background(Bebe.BrandSoft, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { seek(stimulusSec - 0.5, play = true) },
            contentAlignment = Alignment.Center,
        ) { Text("이름 부른 순간부터 보기", style = bebeText(14.sp, Bebe.BrandDeep, bold = true, lineHeight = 20.sp)) }
    }
}

@Composable
private fun Marker(color: Color, x: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.offset(x = x - 7.dp).size(14.dp)
            .background(color, CircleShape).border(2.dp, Color.White, CircleShape),
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = bebeText(12.sp, Bebe.PGray, lineHeight = 16.sp))
    }
}
