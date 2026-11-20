package com.example.bebeview.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** 웹 SVG의 선(stroke) 아이콘을 그대로 옮기는 도구. path 문자열은 웹 코드에서 복사 */
fun strokeIcon(
    name: String,
    viewportWidth: Float,
    viewportHeight: Float,
    strokeWidth: Float,
    color: Color,
    vararg paths: String,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = viewportWidth.dp,
    defaultHeight = viewportHeight.dp,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
).apply {
    paths.forEach { d ->
        addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(color),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

object SvgIcons {
    /** PageHeader 뒤로 가기 (10×16) */
    val Back = strokeIcon("back", 10f, 16f, 2f, Color(0xFF222222), "M8 1L1.5 8 8 15")

    /** 가입 완료 원 안의 체크 (30×22) */
    val BigCheck = strokeIcon("bigCheck", 30f, 22f, 4.5f, Color.White, "M3 11l8 8L27 3")

    /** 약관 동의 체크박스 체크 (14×11) */
    val BoxCheck = strokeIcon("boxCheck", 14f, 11f, 2.4f, Color.White, "M1.5 5.5l3.5 3.5 7.5-7.5")

    private const val EYE = "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"
    private const val PUPIL = "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z"

    /** 비밀번호 보기/숨기기 (24 뷰포트, 20dp 로 그림) */
    val Eye = strokeIcon("eye", 24f, 24f, 1.8f, Color(0xFF9CA3AF), EYE, PUPIL)
    val EyeOff = strokeIcon("eyeOff", 24f, 24f, 1.8f, Color(0xFF9CA3AF), EYE, PUPIL, "M4 4l16 16")
}
