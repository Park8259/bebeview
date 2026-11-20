package com.example.bebeview.record.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.data.MockDoctor
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.correctedAgeMonths
import com.example.bebeview.record.data.nameWithI
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.record.ui.AppNav
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.record.ui.resetToStart
import com.example.bebeview.ui.components.BebeBottomSheet
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.LogoReset
import com.example.bebeview.ui.components.ParentNav
import com.example.bebeview.ui.components.ParentTab
import com.example.bebeview.ui.components.Pill
import com.example.bebeview.ui.components.PillTone
import com.example.bebeview.ui.components.SheetButton
import com.example.bebeview.ui.components.TaskIllust
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// growthLens src/screens/todayTasks/index.tsx 포팅
// Figma 부모 버전: "오늘의과제 4"(시작 전), "오늘의과제 3"(1개 완료 + 1개 다시 찍기)

private enum class TaskState { Todo, Done, Retry }

private fun dayOf(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

/** 오늘 이 과제의 마지막 기록 */
private fun lastToday(records: List<MeasurementRecord>, task: TaskType, today: LocalDate) =
    records.filter { it.taskType == task.apiName && dayOf(it.recordedAt) == today }.maxByOrNull { it.recordedAt }

/** 숫자를 웹(JS)처럼 표시: 2.0 → "2", 3.6 → "3.6" */
private fun fmtValue(v: Double): String = if (v == kotlin.math.floor(v)) v.toLong().toString() else v.toString()

/** 하단 탭 → 화면 */
private fun goTab(nav: AppNav, tab: ParentTab) = nav.tab(
    when (tab) {
        ParentTab.Tasks -> Route.Tasks
        ParentTab.Timeline -> Route.Timeline
        ParentTab.Charts -> Route.Charts
        ParentTab.Summary -> Route.ClinicSummary
    },
)

/** "아이 얼굴이 충분히 보이지 않았습니다" → "화면 밖으로 나갔어요" 같은 짧은 말 */
private fun shortReason(reason: String?): String = when {
    reason.isNullOrEmpty() -> "다시 찍어주세요"
    reason.contains("공") -> "공이 잘 안 보였어요"
    else -> "화면 밖으로 나갔어요"
}

@Composable
fun TodayTasksScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val profile = AppData.profile.value
    val records = AppData.records
    var memoOpen by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    // 아이 정보가 없으면 처음 화면으로 (웹: navigate("/"))
    LaunchedEffect(profile) { if (profile == null) nav.reset(Route.Start) }
    if (profile == null) return

    val chronological = correctedAgeMonths(profile.birthDate, null)
    val corrected = correctedAgeMonths(profile.birthDate, profile.gestationalWeeks)

    val tasks = TaskType.ORDER.map { task ->
        val last = lastToday(records, task, today)
        val state = when {
            last == null -> TaskState.Todo
            last.result.isSuccess -> TaskState.Done
            else -> TaskState.Retry
        }
        Triple(task, state, last)
    }
    val doneCount = tasks.count { it.second == TaskState.Done }
    val next = tasks.firstOrNull { it.second != TaskState.Done }

    val memo = AppData.notes.filter { it.isShared }.maxByOrNull { it.date }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topSpace(56.dp), bottom = 24.dp),
        ) {
            // 로고 + 알림
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LogoReset(hasProfile = true, onReset = { resetToStart(context, nav) })
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .semantics { contentDescription = "알림" }
                        .clickable(role = Role.Button) { if (memo != null) memoOpen = true },
                ) {
                    Icon(BebeIcons.Bell, contentDescription = null, tint = Bebe.PTitle, modifier = Modifier.size(24.dp))
                    if (memo != null) {
                        Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(Bebe.WarnDot, CircleShape))
                    }
                }
            }

            // 제목
            Spacer(Modifier.height(16.dp))
            Text(
                "${nameWithI(profile.name)}, 오늘은\n" + (next?.let { "${it.first.label} 차례예요" } ?: "과제를 모두 마쳤어요"),
                style = bebeText(26.sp, Bebe.PTitle, bold = true, lineHeight = 36.sp),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("${chronological}개월", PillTone.Gray)
                if (corrected != chronological) Pill("교정 ${corrected}개월")
            }

            // 진행 상황
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Bebe.PCard, RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${TaskType.ORDER.size}개 중 ${doneCount}개 완료", style = bebeText(15.sp, Bebe.PTitle, bold = true))
                    Spacer(Modifier.weight(1f))
                    Text("전부 해도 2분이면 끝나요", style = bebeText(13.sp, Bebe.PGray))
                }
                Spacer(Modifier.height(12.dp))
                val ratio = maxOf(doneCount.toFloat() / TaskType.ORDER.size, if (doneCount > 0) 0f else 0.04f)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White)) {
                    Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().background(Bebe.Brand, CircleShape))
                }
            }

            // 과제 카드
            Spacer(Modifier.height(17.dp))
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                tasks.forEach { (task, state, last) ->
                    val metric = last?.result?.metric
                    val sub = when {
                        state == TaskState.Done && metric != null -> "오늘 기록 ${fmtValue(metric.value)}${unitOf(metric.unit)}"
                        state == TaskState.Retry -> "${task.meta.split(" · ")[0]} · ${shortReason(last?.result?.retryReason)}"
                        else -> task.meta
                    }
                    TaskCard(task, state, sub, onStart = { nav.go(Route.TaskGuide(task)) })
                }
            }

            // 선생님 메모
            if (memo != null) {
                Spacer(Modifier.height(17.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Bebe.BrandSoft, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { memoOpen = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(BebeIcons.Memo, contentDescription = null, tint = Bebe.BrandDeep, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${MockDoctor.NAME} 선생님 메모가 도착했어요", style = bebeText(14.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            memo.text.replace("\n", " "), style = bebeText(13.sp, Bebe.PBody),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(BebeIcons.ChevronRight, contentDescription = null, tint = Bebe.BrandDeep, modifier = Modifier.size(18.dp))
                }
            }
        }

        ParentNav(ParentTab.Tasks) { goTab(nav, it) }
    }

    if (memoOpen && memo != null) {
        BebeBottomSheet(
            onClose = { memoOpen = false },
            title = "${MockDoctor.NAME} 선생님 메모",
            subtitle = memo.date.replace("-", ". "),
        ) {
            Text(memo.text, style = bebeText(15.sp, Bebe.PBody, lineHeight = 24.sp))
            Spacer(Modifier.height(24.dp))
            SheetButton("확인", { memoOpen = false }, Modifier.fillMaxWidth(), Bebe.Brand, Color.White)
        }
    }
}

@Composable
private fun TaskCard(task: TaskType, state: TaskState, sub: String, onStart: () -> Unit) {
    // Figma "오늘의과제 4/5": 할 과제는 파란 카드, 끝낸·다시 찍을 과제는 흰 카드. 과제 이름만 크게
    val filled = state == TaskState.Todo
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(102.dp)
            .background(if (filled) Bebe.Brand else Color.White, shape)
            .then(if (filled) Modifier else Modifier.border(1.dp, Bebe.PLine, shape))
            .padding(horizontal = 16.dp)
            // 화면에는 숨기고 읽어 주기에만 남겨요 (웹의 sr-only)
            .semantics(mergeDescendants = true) { contentDescription = "${task.label}, ${task.short} · $sub" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskIllust(task, size = 70.dp)
        Text(
            task.label,
            style = bebeText(20.sp, if (filled) Color.White else Color(0xFF494949), bold = true, lineHeight = 24.sp),
            modifier = Modifier.padding(start = 26.dp).weight(1f),
        )
        if (state == TaskState.Done) {
            Box(
                Modifier.height(35.dp).background(Bebe.PCard, CircleShape).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("완료", style = bebeText(13.sp, Color(0xFF494949), bold = true)) }
        } else {
            val retry = state == TaskState.Retry
            Box(
                Modifier
                    .height(35.dp)
                    .background(if (retry) Bebe.WarnBg else Color.White, CircleShape)
                    .clickable(role = Role.Button, onClick = onStart)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (retry) "다시 찍기" else "시작하기",
                    style = bebeText(13.sp, if (retry) Bebe.WarnText else Bebe.BrandIcon, bold = true),
                )
            }
        }
    }
}
