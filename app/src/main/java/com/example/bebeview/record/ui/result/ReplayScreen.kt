package com.example.bebeview.record.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe

// growthLens screens/replay/index.tsx 포팅
// 기록 탭에서 지난 기록을 눌렀을 때 — 촬영 직후 결과 화면과 같은 모양으로 그 기록을 보여줘요.
// (영상 위 표시 "측정 과정 다시 보기"는 영상 저장이 붙으면 다시 넣을 자리)

@Composable
fun ReplayScreen(recordId: String) {
    val nav = LocalNav.current
    val record = AppData.record(recordId)
    if (record == null) {
        NotFound("기록을 찾을 수 없어요", "기록으로 돌아가기") { nav.tab(Route.Timeline) }
        return
    }
    val task = TaskType.fromApi(record.taskType)
    // 이 기록보다 앞선 같은 과제의 성공 기록, 최근 것부터
    val earlier = AppData.records
        .filter { it.taskType == record.taskType && it.result.isSuccess && it.recordedAt < record.recordedAt }
        .sortedByDescending { it.recordedAt }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        BackTitleBar(task.label) { nav.back() }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
        ) {
            RecordDetail(
                task = task,
                title = "${fmtDay(record.recordedAt)} 기록이에요",
                result = record.result,
                recordedAt = record.recordedAt,
                earlier = earlier,
                currentLabel = fmtDay(record.recordedAt),
            )
        }

        BottomBar {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("기록 목록", Modifier.width(132.dp), primary = false) { nav.tab(Route.Timeline) }
                ActionButton("이 과제 다시 하기", Modifier.weight(1f), primary = true) { nav.go(Route.TaskGuide(task)) }
            }
        }
    }
}
