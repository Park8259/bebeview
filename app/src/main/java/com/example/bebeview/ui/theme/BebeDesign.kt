package com.example.bebeview.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.bebeview.R

/**
 * growthLens 웹(tailwind.config.js)의 디자인 값을 그대로 옮긴 것.
 * 웹의 px 값은 모바일에서 dp/sp 와 1:1 로 대응해요.
 */
object Bebe {
    // brand
    val Brand = Color(0xFF14C4FF)
    val BrandLight = Color(0xFFDFF7FF)
    val BrandSoft = Color(0xFFE6F8FF)
    val BrandDeep = Color(0xFF0E8DBA)
    val BrandChip = Color(0xFFDAF6FF)   // 과제 이름 칩
    val BrandPhoto = Color(0xFFDCEFF6)  // 사진·영상 자리
    val BrandIcon = Color(0xFF009ED3)   // 아이콘

    // 부모 앱 화면 공통 글자/면 색 (tailwind p-*)
    val PTitle = Color(0xFF111111)
    val PBody = Color(0xFF333333)
    val PGray = Color(0xFF767676)
    val PLight = Color(0xFFA8A8A8)
    val PFaint = Color(0xFFC8CBD0)
    val PLine = Color(0xFFE3E5E8)
    val PCard = Color(0xFFF3F4F6)
    val PDark = Color(0xFF34444C)       // 촬영 화면 배경

    // warn-*
    val WarnBg = Color(0xFFFFF8CA)
    val WarnText = Color(0xFFC28514)
    val WarnDot = Color(0xFFE8590C)

    // ink (글자)
    val Ink = Color(0xFF222222)
    val InkStrong = Color(0xFF17202B)
    val InkSub = Color(0xFF555555)
    val InkMuted = Color(0xFF5B6573)
    val InkHint = Color(0xFF999999)
    val InkFaint = Color(0xFFCCCCCC)

    // line (테두리)
    val Line = Color(0xFFBBBBBB)
    val LineSoft = Color(0xFFD9D9D9)
    val LineDivider = Color(0xFFE5E5E5)

    // fill (면)
    val FillDisabled = Color(0xFFE4E4E4)
    val FillCancel = Color(0xFFEBEBEB)
    val FillWheel = Color(0xFFF2F2F2)
    val FillCard = Color(0xFFEEEEEE)

    // 회원가입·로그인 화면(auth/index.tsx)에서 직접 쓰는 색
    val Title = Color(0xFF1A2230)
    val Desc = Color(0xFF6B7280)
    val Help = Color(0xFF9CA3AF)
    val Error = Color(0xFFFF5A5F)
    val Link = Color(0xFF0A8FBF)
    val GrayButton = Color(0xFFEDEDED)
    val DoneButton = Color(0xFFE3F6FF)
    val StepOff = Color(0xFFE5E7EB)
    val CheckOff = Color(0xFFC4C4C4)
    val LinkDivider = Color(0xFFD1D5DB)

    val Pretendard = FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_bold, FontWeight.Bold),
    )
}

/*
 * Tailwind → Compose 대응표 (포팅할 때 참고)
 *  글자: text-xs 12/16 · text-sm 14/20 · text-base 16/24 · text-lg 18/28 · text-xl 20/28 · text-2xl 24/32
 *        text-[Npx] 처럼 줄 높이 없는 크기는 N × 1.5 (웹 기본 줄 높이)
 *  간격: p-1 = 4dp (숫자 × 4), gap·space-y 도 같음
 *  둥글기: rounded 4 · md 6 · lg 8 · xl 12 · 2xl 16 · 3xl 24 · full = CircleShape/50%
 *  font-bold = FontWeight.Bold (Pretendard Bold), font-semibold 도 Bold 로 처리
 */

/**
 * 웹과 같은 글자 모양. 줄 높이를 따로 주지 않으면 웹 기본값(글자 크기 × 1.5)을 따라요.
 * CSS처럼 위아래 여백을 반씩 나눠 가운데에 놓음.
 */
fun bebeText(
    size: TextUnit,
    color: Color = Bebe.Ink,
    bold: Boolean = false,
    lineHeight: TextUnit = (size.value * 1.5f).sp,
) = TextStyle(
    fontFamily = Bebe.Pretendard,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontSize = size,
    lineHeight = lineHeight,
    color = color,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)
