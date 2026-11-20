package com.example.bebeview.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText

// growthLens 공용 컴포넌트 포팅: ParentNav · Pill · TaskIcon · TaskIllust · LogoReset · BottomSheet · Splash

// ─────────────────────────────────────────────────────────────────────────────
/** 하단 탭 종류 (Figma 부모 버전: 과제 · 기록 · 변화 · 진료 요약) */
enum class ParentTab(val label: String) { Tasks("과제"), Timeline("기록"), Charts("변화"), Summary("진료 요약") }

/** 회색 둥근 바 위에 선택된 탭만 파란 알약 (ParentNav.tsx) */
@Composable
fun ParentNav(active: ParentTab?, onSelect: (ParentTab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomSpace(18.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(Bebe.PCard, RoundedCornerShape(35.5.dp))
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParentTab.entries.forEach { tab ->
                val on = tab == active
                val icon = when (tab) {
                    ParentTab.Tasks -> BebeIcons.NavTask
                    ParentTab.Timeline -> BebeIcons.NavRecord
                    ParentTab.Charts -> BebeIcons.NavChange
                    ParentTab.Summary -> BebeIcons.NavSummary
                }
                Column(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .background(if (on) Bebe.Brand else Color.Transparent, RoundedCornerShape(27.dp))
                        .clickable(role = Role.Tab) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Icon(icon, contentDescription = null, tint = if (on) Color.White else Color(0xFF444444), modifier = Modifier.size(24.dp))
                    Text(
                        tab.label,
                        style = bebeText(11.sp, if (on) Color.White else Color(0xFF494949), bold = on, lineHeight = 13.sp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
enum class PillTone { Blue, Gray, Warn }

/** 칩 (과제 이름, 월령 등). h-26 px-8 12sp bold */
@Composable
fun Pill(text: String, tone: PillTone = PillTone.Blue, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        PillTone.Blue -> Bebe.BrandChip to Bebe.BrandDeep
        PillTone.Gray -> Bebe.PCard to Bebe.PBody
        PillTone.Warn -> Bebe.WarnBg to Bebe.WarnText
    }
    Box(
        modifier.height(26.dp).background(bg, CircleShape).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = bebeText(12.sp, fg, bold = true, lineHeight = 16.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 과제별 아이콘: 공 주고받기 = 공, 호명 반응 = 스피커, 동작 모방 = 손 */
fun taskIcon(task: TaskType): ImageVector = when (task) {
    TaskType.BALL_PLAY -> BebeIcons.Ball
    TaskType.NAME_RESPONSE -> BebeIcons.Speaker
    TaskType.IMITATION -> BebeIcons.Hand
}

@Composable
fun TaskIcon(task: TaskType, size: Dp = 24.dp, tint: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Icon(taskIcon(task), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/**
 * Figma 과제 일러스트 (흰 둥근 네모 + 그림). 그림 일부가 네모 밖으로 살짝 나오므로
 * 70px 네모 기준 위치를 size 에 맞춰 늘리고 줄여요 (TaskIllust.tsx).
 */
@Composable
fun TaskIllust(task: TaskType, size: Dp = 43.dp, modifier: Modifier = Modifier) {
    data class Art(val res: Int, val left: Float, val top: Float, val w: Float, val h: Float)
    val a = when (task) {
        TaskType.BALL_PLAY -> Art(R.drawable.task_ball_play, 0f, 0f, 70f, 70f)
        TaskType.NAME_RESPONSE -> Art(R.drawable.task_name_response, -1f, -7f, 81f, 83f)
        TaskType.IMITATION -> Art(R.drawable.task_imitation, 0f, -14f, 70f, 87f)
    }
    val k = size.value / 70f
    Box(modifier.size(size)) {
        Image(
            painterResource(a.res),
            contentDescription = null,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset((a.left * k).dp, (a.top * k).dp)
                .requiredSize((a.w * k).dp, (a.h * k).dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 베베뷰 로고 (마크 + 글자) */
@Composable
fun Logo(mark: Dp = 31.dp, word: Dp = 36.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.height(mark))
        Image(painterResource(R.drawable.logo_wordmark), contentDescription = "베베뷰", modifier = Modifier.height(word))
    }
}

/**
 * 베베뷰 로고. 누르면 아이 정보를 지우고 처음(회원가입·로그인) 화면으로 돌아가요.
 * (데모 때 정보입력부터 다시 보여주기 위한 숨은 기능. 기록은 지우지 않아요) — LogoReset.tsx
 */
@Composable
fun LogoReset(hasProfile: Boolean, onReset: () -> Unit, mark: Dp = 31.dp, word: Dp = 36.dp) {
    var confirm by remember { mutableStateOf(false) }
    Logo(mark, word, Modifier.clickable(role = Role.Button) { if (hasProfile) confirm = true else onReset() })
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text("아이 정보를 지우고 처음부터 다시 입력할까요?", style = bebeText(15.sp, Bebe.Ink)) },
            confirmButton = { TextButton(onClick = { confirm = false; onReset() }) { Text("확인", style = bebeText(15.sp, Bebe.BrandDeep, bold = true)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소", style = bebeText(15.sp, Bebe.InkSub)) } },
            containerColor = Color.White,
        )
    }
}

/** 앱을 처음 열 때 잠깐 보이는 로고 화면 (splash/index.tsx) */
@Composable
fun Splash() {
    Column(
        Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.height(92.dp))
        Image(
            painterResource(R.drawable.logo_wordmark),
            contentDescription = "베베뷰",
            modifier = Modifier.height(71.dp).offset(y = (-4).dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 아래에서 올라오는 시트 (BottomSheet.tsx). 둥근 24, 손잡이, 제목 20sp, 부제 14sp */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BebeBottomSheet(
    onClose: () -> Unit,
    title: String = "",
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(Color(0xFFD0D0D0), CircleShape),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            if (title.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(title, style = bebeText(20.sp, Bebe.Ink, bold = true, lineHeight = 24.sp))
            }
            if (subtitle != null) {
                Spacer(Modifier.height(10.dp))
                Text(subtitle, style = bebeText(14.sp, Bebe.InkSub, lineHeight = 20.sp))
            }
            Spacer(Modifier.height(if (title.isNotEmpty()) 20.dp else 24.dp))
            content()
        }
    }
}

/** 시트 하단 버튼: 취소 + 확인 */
@Composable
fun SheetActions(onCancel: () -> Unit, onConfirm: () -> Unit, confirmLabel: String = "확인") {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetButton("취소", onCancel, Modifier.width(128.dp), Bebe.FillCancel, Bebe.Ink)
        SheetButton(confirmLabel, onConfirm, Modifier.weight(1f), Bebe.Brand, Color.White)
    }
}

/** 시트 하단 단일 버튼: 선택 완료 */
@Composable
fun SheetDone(onClick: () -> Unit, enabled: Boolean = true) {
    SheetButton(
        "선택 완료", onClick, Modifier.fillMaxWidth(),
        if (enabled) Bebe.Brand else Bebe.FillDisabled, if (enabled) Color.White else Bebe.InkHint,
        enabled = enabled, fontSize = 17f,
    )
}

@Composable
fun SheetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    bg: Color,
    fg: Color,
    enabled: Boolean = true,
    fontSize: Float = 16f,
) {
    Box(
        modifier.height(54.dp).background(bg, RoundedCornerShape(14.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = bebeText(fontSize.sp, fg, bold = true)) }
}

/** 하늘색으로 선택 표시되는 카드 (조산여부·출생 순서·성별) — OptionCard */
@Composable
fun OptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(contentColor: Color) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .background(if (selected) Bebe.BrandLight else Color.White, shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Bebe.Brand else Bebe.LineSoft, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content(if (selected) Bebe.Brand else Bebe.InkSub) }
}

/** 화면 영역 위에 쓰는 가로 줄 (border-b 등) */
@Composable
fun Divider(color: Color = Bebe.LineDivider, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

