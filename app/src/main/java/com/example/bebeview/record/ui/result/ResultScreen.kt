package com.example.bebeview.record.ui.result

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.record.ui.recording.LastReplayHolder
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.bottomSpace
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText

// growthLens screens/result/index.tsx 포팅
// Figma 부모 버전: "기록 저장"(성공), "재촬영"(retry_needed)

@Composable
fun ResultScreen(task: TaskType, recordId: String) {
    val nav = LocalNav.current
    val record = AppData.record(recordId)

    if (record == null) {
        NotFound("결과를 불러올 수 없어요", "과제로 돌아가기") { nav.tab(Route.Tasks) }
        return
    }
    val result = record.result
    if (!result.isSuccess) {
        RetryView(task, result.retryReason)
        return
    }

    // 방금 저장된 기록(가장 최근) + 그 전 성공 기록
    val mine = AppData.records
        .filter { it.taskType == task.apiName && it.result.isSuccess }
        .sortedByDescending { it.recordedAt }
    // 방금 분석한 영상과 좌표 (메모리에만 있음, 앱을 다시 켜면 없음)
    val replay = LastReplayHolder.get(task)

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(56.dp), bottom = 24.dp),
        ) {
            RecordDetail(
                task = task,
                title = "기록이 저장됐어요",
                result = result,
                recordedAt = mine.firstOrNull()?.recordedAt,
                earlier = mine.drop(1),
                currentLabel = "오늘",
                extra = replay?.let { r -> { TraceReplay(r.video, r.trace) } },
            )
        }
        BottomBar {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("기록 보기", Modifier.width(132.dp), primary = false) { nav.tab(Route.Timeline) }
                ActionButton("다음 과제 하기", Modifier.weight(1f), primary = true) { nav.tab(Route.Tasks) }
            }
        }
    }
}

@Composable
private fun RetryView(task: TaskType, reason: String?) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(56.dp), bottom = 24.dp),
        ) {
            TaskPill(task)
            Spacer(Modifier.height(15.dp))
            Text("다시 한번 찍어볼까요?", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))
            Spacer(Modifier.height(12.dp))
            Text(
                "기록이 잘못된 게 아니라, 화면에서 아이${if (task == TaskType.BALL_PLAY) "나 공이" else "가"} 잘 안보여서 정확히 잴 수 없었어요.",
                style = bebeText(15.sp, Bebe.PGray, lineHeight = 22.sp),
            )

            Spacer(Modifier.height(22.dp))
            val shape = RoundedCornerShape(16.dp)
            Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, Bebe.PLine, shape)) {
                Box(Modifier.fillMaxWidth().height(168.dp).background(Bebe.BrandPhoto), contentAlignment = Alignment.Center) {
                    Icon(BebeIcons.ImagePlaceholder, null, tint = Bebe.BrandIcon, modifier = Modifier.size(22.dp))
                }
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(BebeIcons.Question, null, tint = Bebe.BrandDeep, modifier = Modifier.size(18.dp))
                    Text(reason ?: "아이가 화면에 충분히 보이지 않았어요", style = bebeText(14.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp))
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("이렇게 하면 잘 찍혀요", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 24.sp))
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                task.retryTips.forEachIndexed { i, tip -> Text("${i + 1}. $tip", style = bebeText(15.sp, Bebe.PBody)) }
            }
        }
        BottomBar {
            ActionButton("다시 찍기", Modifier.fillMaxWidth(), primary = true) { nav.replace(Route.Recording(task)) }
            Spacer(Modifier.height(16.dp))
            Text(
                "나중에 할게요",
                style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { nav.tab(Route.Tasks) },
            )
        }
    }
}

/** 아래 고정 버튼 영역 (sticky bottom-0 px-5 pt-3 pb-8) */
@Composable
fun BottomBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomSpace(32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** h-54 둥근 14 버튼. primary = 파랑, 아니면 회색 */
@Composable
fun ActionButton(label: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier.height(54.dp)
            .background(if (primary) Bebe.Brand else Bebe.PCard, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = bebeText(16.sp, if (primary) Color.White else Bebe.PBody, bold = true)) }
}

/** 기록을 못 찾을 때 */
@Composable
fun NotFound(message: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(message, style = bebeText(15.sp, Bebe.PGray))
        Box(
            Modifier.height(54.dp).background(Bebe.PCard, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) { Text(button, style = bebeText(16.sp, Bebe.PBody, bold = true)) }
    }
}
