package com.example.bebeview.record.ui.summary

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.MeasurementRecord
import com.example.bebeview.record.data.Metric
import com.example.bebeview.record.data.ParentSessionStore
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.correctedAgeMonths
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.charts.ambientRatio
import com.example.bebeview.record.ui.charts.cal
import com.example.bebeview.record.ui.charts.jsNum
import com.example.bebeview.record.ui.charts.jsRound
import com.example.bebeview.record.ui.charts.openTab
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.Divider
import com.example.bebeview.ui.components.ParentNav
import com.example.bebeview.ui.components.ParentTab
import com.example.bebeview.ui.components.TaskIllust
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.util.Calendar

// growthLens src/screens/clinicSummary/index.tsx 포팅
// Figma 부모 버전 "진료요약" + "진료코드"(공유 시트 → ShareSheet.kt)

internal fun fmtYmd(ts: Long) = cal(ts).let { "${it.get(Calendar.YEAR)}. ${it.get(Calendar.MONTH) + 1}. ${it.get(Calendar.DAY_OF_MONTH)}" }
internal fun fmtMd(ts: Long) = cal(ts).let { "${it.get(Calendar.MONTH) + 1}. ${it.get(Calendar.DAY_OF_MONTH)}" }
private fun dayKey(ts: Long) = cal(ts).let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.DAY_OF_YEAR)}" }

/** 변화 방향만 사실대로 (좋다/나쁘다로 읽히는 말은 쓰지 않음) */
internal fun trendWord(first: Double, last: Double, unit: String): String = when {
    first == last -> "같음"
    unit == "seconds" -> if (last < first) "빨라짐" else "느려짐"
    else -> if (last > first) "늘어남" else "줄어듦"
}

internal fun ambientPct(r: MeasurementRecord, orient: Boolean): Long = jsRound(ambientRatio(r, orient))

/** 요약 화면과 PDF 가 같이 쓰는 값 */
internal class SummaryData(records: List<MeasurementRecord>) {
    val sorted = records.sortedBy { it.recordedAt }
    val successes = sorted.filter { it.result.isSuccess && it.result.metric != null }
    val first = sorted.firstOrNull()
    val last = sorted.lastOrNull()
    val weeks = if (first != null && last != null)
        maxOf(1L, jsRound((last.recordedAt - first.recordedAt) / (7 * 864e5))) else 0L
    val taskRows: List<Triple<TaskType, Metric, Metric>> = TaskType.ORDER.mapNotNull { task ->
        val recs = successes.filter { it.taskType == task.apiName }
        val f = recs.firstOrNull()?.result?.metric
        val l = recs.lastOrNull()?.result?.metric
        if (f != null && l != null) Triple(task, f, l) else null
    }
    private val ambient = sorted.filter { it.result.ambient != null }
    val a0 = ambient.firstOrNull()
    val a1 = ambient.lastOrNull()
    val doneCount = successes.size
    val dayCount = sorted.map { dayKey(it.recordedAt) }.toSet().size
    val retryCount = sorted.count { it.result.status == "retry_needed" }
}

@Composable
fun ClinicSummaryScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val profile = AppData.profile.value
    val questions = AppData.questions
    var shareOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    // 아이 정보가 없으면 웹처럼 비워 둠 (탭만 남김)
    if (profile == null) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Spacer(Modifier.weight(1f))
            ParentNav(ParentTab.Summary) { nav.openTab(it) }
        }
        return
    }

    val d = SummaryData(AppData.records)
    val age = correctedAgeMonths(profile.birthDate, profile.gestationalWeeks)
    val (by, bm, bd) = profile.birthDate.split("-").map { it.toIntOrNull() ?: 0 }.let { Triple(it.getOrElse(0) { 0 }, it.getOrElse(1) { 0 }, it.getOrElse(2) { 0 }) }

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = topSpace(59.dp), bottom = 0.dp)) {
            Text("진료 요약이 도착했어요", style = bebeText(24.sp, Bebe.PTitle, bold = true, lineHeight = 32.sp))

            // 아이 정보
            Spacer(Modifier.height(37.dp))
            Row(
                Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(27.dp),
            ) {
                Image(
                    painterResource(R.drawable.child_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(110.dp, 113.dp).clip(CircleShape),
                )
                Column {
                    Text("${profile.name} (${age}개월)", style = bebeText(16.sp, Color(0xFF1E2A44), bold = true, lineHeight = 20.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("${by}년 ${bm}월 ${bd}일생", style = bebeText(14.sp, Bebe.InkSub, lineHeight = 18.sp))
                    if (d.first != null && d.last != null) {
                        Spacer(Modifier.height(11.dp))
                        Text(
                            "기록 기간\n${fmtYmd(d.first.recordedAt)} ~ ${fmtMd(d.last.recordedAt)} (${d.weeks}주)",
                            style = bebeText(13.sp, Color(0xFF777777), lineHeight = 19.sp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(46.dp))

            // 과제 지표
            Card {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("과제 지표", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp), modifier = Modifier.alignByBaseline())
                    Text("첫 기록 → 최근", style = bebeText(12.sp, Bebe.PGray, lineHeight = 16.sp), modifier = Modifier.alignByBaseline())
                }
                Spacer(Modifier.height(8.dp))
                if (d.taskRows.isEmpty()) {
                    Text("아직 기록이 없어요", style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp), modifier = Modifier.padding(vertical = 12.dp))
                }
                d.taskRows.forEachIndexed { i, (task, f, l) ->
                    if (i > 0) Divider(Bebe.PLine)
                    val num = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp).copy(fontFeatureSettings = "tnum")
                    Row(
                        Modifier.height(59.dp).padding(start = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TaskIllust(task, 42.dp)
                        Text(
                            task.label,
                            style = bebeText(14.sp, Bebe.PTitle, bold = true, lineHeight = 17.sp),
                            modifier = Modifier.weight(1f).padding(start = 13.dp),
                        )
                        Text("${jsNum(f.value)}${unitOf(f.unit)} →", style = num)
                        Text(
                            "${jsNum(l.value)}${unitOf(l.unit)}",
                            style = bebeText(18.sp, Bebe.PTitle, bold = true, lineHeight = 22.sp).copy(fontFeatureSettings = "tnum"),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(55.dp),
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            trendWord(f.value, l.value, l.unit),
                            style = bebeText(12.sp, Bebe.BrandDeep, bold = true, lineHeight = 14.sp),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(52.dp),
                        )
                    }
                }
            }

            // 수행 이력
            Card(top = 29.dp) {
                Text("수행 이력", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp))
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    listOf(
                        Triple(SummaryIcons.DoneTask, "${d.doneCount}회", "완료한 과제"),
                        Triple(SummaryIcons.RecordDay, "${d.dayCount}일", "기록한 날"),
                        Triple(SummaryIcons.Retake, "${d.retryCount}회", "재촬영"),
                    ).forEach { (icon, v, l) ->
                        Column(
                            Modifier.weight(1f).height(85.dp).background(Bebe.Brand, RoundedCornerShape(8.dp)).padding(top = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(v, style = bebeText(14.sp, Color.White, bold = true, lineHeight = 17.sp))
                            Spacer(Modifier.height(3.dp))
                            Text(l, style = bebeText(11.sp, Color.White, lineHeight = 13.sp))
                        }
                    }
                }
            }

            // 선생님께 물어볼 것 (테두리 카드)
            Card(top = 22.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("선생님께 물어볼 것", style = bebeText(16.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp), modifier = Modifier.weight(1f))
                    Text(
                        "+ 추가",
                        style = bebeText(13.sp, Bebe.BrandDeep, bold = true),
                        modifier = Modifier.clickable(role = Role.Button) { adding = true },
                    )
                }
                Spacer(Modifier.height(17.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    questions.forEachIndexed { i, q ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Bebe.PCard, RoundedCornerShape(12.dp))
                                .padding(horizontal = 25.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(BebeIcons.Memo, contentDescription = null, tint = Bebe.PGray, modifier = Modifier.padding(top = 1.dp).size(18.dp))
                            Text(q, style = bebeText(14.sp, Bebe.PBody, lineHeight = 17.sp), modifier = Modifier.weight(1f))
                            Text(
                                "×",
                                style = bebeText(16.sp, Bebe.PLight, lineHeight = 24.sp),
                                modifier = Modifier.clickable(role = Role.Button) { AppData.removeQuestion(i) },
                            )
                        }
                    }
                    if (adding) {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        Column(Modifier.fillMaxWidth().background(Bebe.PCard, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            val style = bebeText(14.sp, Bebe.PBody, lineHeight = 20.sp)
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                textStyle = style,
                                cursorBrush = SolidColor(Bebe.PBody),
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                                decorationBox = { inner ->
                                    Box {
                                        if (draft.isEmpty()) Text("진료 때 물어볼 것을 적어주세요", style = style.copy(color = Bebe.PLight))
                                        inner()
                                    }
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                                Box(
                                    Modifier.height(32.dp).clip(RoundedCornerShape(8.dp))
                                        .clickable(role = Role.Button) { adding = false; draft = "" }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text("취소", style = bebeText(13.sp, Bebe.PGray)) }
                                val ok = draft.isNotBlank()
                                Box(
                                    Modifier.height(32.dp).alpha(if (ok) 1f else 0.5f)
                                        .background(Bebe.Brand, RoundedCornerShape(8.dp))
                                        .clickable(enabled = ok, role = Role.Button) {
                                            AppData.addQuestion(draft.trim()); draft = ""; adding = false
                                        }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text("저장", style = bebeText(13.sp, Color.White, bold = true)) }
                            }
                        }
                    }
                }
            }
        }


        // 하단 버튼 (웹: 고정이 아니라 내용 끝에 붙어 함께 스크롤돼요. 하단 탭만 고정)
        Spacer(Modifier.height(53.dp))
        Divider(Bebe.PLine)
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(start = 19.dp, end = 22.dp, top = 13.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                Modifier.width(130.dp).height(43.dp)
                    .border(1.dp, Bebe.PLine, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                    printSummary(context, profile, age, d, questions.toList())
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(BebeIcons.Pdf, contentDescription = null, tint = Bebe.PTitle, modifier = Modifier.size(18.dp))
                Text("PDF 저장", style = bebeText(16.sp, Bebe.PTitle, bold = true))
            }
            Box(
                Modifier.weight(1f).height(43.dp).background(Bebe.Brand, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { shareOpen = true },
                contentAlignment = Alignment.Center,
            ) { Text("의사에게 공유하기", style = bebeText(16.sp, Color.White, bold = true)) }
        }
        }
        ParentNav(ParentTab.Summary) { nav.openTab(it) }
    }

    if (shareOpen) {
        // 웹은 profile.id 로 코드를 발급해요. 없을 일은 없지만 로그인 세션 id 를 대신 써요
        val childId = profile.id.ifBlank { ParentSessionStore.load(context)?.parentId ?: "" }
        ShareSheet(childId = childId, hasQuestions = questions.isNotEmpty(), onClose = { shareOpen = false })
    }
}

/** 테두리 카드 (Figma: 둥근 16, 안쪽 여백 16~19) */
@Composable
private fun Card(top: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(top))
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Bebe.PLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 17.dp, vertical = 18.dp),
        content = content,
    )
}
