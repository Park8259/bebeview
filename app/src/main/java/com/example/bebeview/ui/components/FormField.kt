package com.example.bebeview.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText

// growthLens src/components/FormField.tsx 포팅

/**
 * 웹(Figma)의 위·아래 여백은 화면 맨 끝에서 잰 값이에요 (상태바·내비게이션 바 영역 포함).
 * 그래서 시스템 바 높이를 더하지 않고, 디자인 값과 "시스템 바 + 최소 여백" 중 큰 쪽을 써요.
 */
@Composable
fun topSpace(design: Dp, minExtra: Dp = 16.dp): Dp =
    maxOf(design, WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + minExtra)

@Composable
fun bottomSpace(design: Dp, minExtra: Dp = 12.dp): Dp =
    maxOf(design, WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + minExtra)

/** 입력 항목 라벨 + 칸 */
@Composable
fun Field(label: String?, content: @Composable () -> Unit) {
    Column {
        if (label != null) {
            Text(label, style = bebeText(14.sp, Bebe.InkSub, bold = true, lineHeight = 20.sp))
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** 글자를 직접 입력하는 칸 (h-51, 둥근 10, 테두리 #BBB → 포커스 #555) */
@Composable
fun TextBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = bebeText(16.sp, Bebe.Ink, lineHeight = 24.sp),
        cursorBrush = SolidColor(Bebe.Ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(51.dp)
            .background(Color.White, shape)
            .border(1.dp, if (focused) Bebe.InkSub else Bebe.Line, shape),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = bebeText(16.sp, Bebe.InkHint, lineHeight = 24.sp), maxLines = 1)
                }
                Box(Modifier.padding(end = if (trailing != null) 32.dp else 0.dp)) { inner() }
                if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
            }
        },
    )
}

/** 비밀번호 칸 + 보기/숨기기 눈 아이콘. highlight 면 바깥에 브랜드색 2px 링 */
@Composable
fun PasswordBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    highlight: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var show by remember { mutableStateOf(false) }
    TextBox(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = if (highlight) Modifier.ring(Bebe.Brand, 10.dp) else Modifier,
        trailing = if (value.isEmpty()) null else {
            {
                Image(
                    imageVector = if (show) SvgIcons.EyeOff else SvgIcons.Eye,
                    contentDescription = if (show) "비밀번호 숨기기" else "비밀번호 보기",
                    modifier = Modifier
                        .clickable(role = Role.Button) { show = !show }
                        .padding(4.dp)
                        .size(20.dp),
                )
            }
        },
    )
}

/** CSS ring-2: 테두리 바깥쪽에 2dp 링 */
fun Modifier.ring(color: Color, radius: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp = 2.dp) =
    drawBehind {
        val w = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(-w / 2, -w / 2),
            size = Size(size.width + w, size.height + w),
            cornerRadius = CornerRadius(radius.toPx() + w / 2),
            style = Stroke(w),
        )
    }

/** 화면 상단: 뒤로 가기 + 가운데 제목 + 구분선 (h-90, 상태바 영역 포함) */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(topSpace(90.dp, minExtra = 48.dp))) {
            Image(
                imageVector = SvgIcons.Back,
                contentDescription = "뒤로 가기",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp)
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(8.dp),
            )
            Text(
                title,
                style = bebeText(18.sp, Bebe.Ink, bold = true, lineHeight = 28.sp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Bebe.LineDivider))
    }
}

/** 회원가입 진행 막대 (계정 → 우리 아이 정보 → 출생 정보 → 이용약관) */
@Composable
fun StepBar(step: Int, total: Int = 4) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(if (i < step) Bebe.Brand else Bebe.StepOff, RoundedCornerShape(50)),
            )
        }
    }
}

/** 큰 버튼 (h-56, 둥근 14). 비활성: 회색 바탕 + 흐린 글자 */
@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fontSize: Float = 16f,
    container: Color = Bebe.Brand,
    content: Color = Color.White,
) {
    val active = enabled && !loading
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (active || loading) container else Bebe.FillDisabled, shape)
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(label, style = bebeText(fontSize.sp, if (active) content else Bebe.InkHint, bold = true))
        }
    }
}

/** 화면 하단 큰 버튼 (다음 / 완료). px-24 pt-16 pb-50, 17sp */
@Composable
fun BottomButton(label: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = bottomSpace(50.dp)),
    ) {
        BigButton(label, onClick, enabled = enabled, loading = loading, fontSize = 17f)
    }
}

/** 입력칸 오른쪽 작은 버튼 (인증번호 받기 / 재전송 / 확인 / 인증 완료) */
enum class SideTone { Brand, Gray, Done }

@Composable
fun SideButton(label: String, tone: SideTone, enabled: Boolean, onClick: () -> Unit) {
    val (bg, fg) = when (tone) {
        SideTone.Brand -> if (enabled) Bebe.Brand to Color.White else Bebe.FillDisabled to Bebe.InkHint
        SideTone.Gray -> Bebe.GrayButton to (if (enabled) Bebe.Title else Bebe.InkHint)
        SideTone.Done -> Bebe.DoneButton to Bebe.Link
    }
    Box(
        Modifier
            .width(100.dp)
            .height(51.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = bebeText(13.sp, fg, bold = true))
    }
}
