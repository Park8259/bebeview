package com.example.bebeview.record.ui.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.Divider
import com.example.bebeview.ui.components.Pill
import com.example.bebeview.ui.components.TaskIllust
import com.example.bebeview.ui.components.bottomSpace
import com.example.bebeview.ui.components.strokeIcon
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText

// growthLens src/screens/taskGuide/index.tsx 포팅
// Figma 최종본(Page 3) "호명반응" 안내

/** 촬영 예시 이미지가 있는 과제 */
private val GUIDE_IMAGE: Map<TaskType, Int> = mapOf(TaskType.NAME_RESPONSE to R.drawable.guide_name_response)

/** 소리 끔 (Figma "아이콘_3": 스피커 + 사선) */
private val MuteIcon = strokeIcon(
    "mute", 20f, 20f, 1.54f, Color(0xFF767676),
    "M0.77 7.95H5.9L12.05 2.82V17.18L5.9 12.05H0.77V7.95Z",
    "M15.64 6.92C17.35 8.97 17.35 11.03 15.64 13.08",
    "M0.77 19.23L19.23 0.77",
)

private val CHECKLIST = listOf(
    BebeIcons.Phone to "폰을 세워 두세요",
    BebeIcons.Ruler to "아이 전신이 보이도록 2~3m 떨어뜨려 주세요",
    MuteIcon to "TV · 음악을 끄고 조용한 곳에서 찍어 주세요",
    BebeIcons.Pin to "매번 같은 장소에서 찍으면 더 정확해요",
)

@Composable
fun TaskGuideScreen(task: TaskType) {
    val nav = LocalNav.current
    // 새 기록: 선생님이 마지막으로 메모를 남긴 날 이후 이 과제 기록 수 (메모가 없으면 전체)
    val lastNote = AppData.notes.filter { it.isShared }.maxOfOrNull { it.date }
    val count = AppData.records.count { r ->
        r.taskType == task.apiName && (lastNote == null || dateOf(r.recordedAt) > lastNote)
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // 상단: 뒤로 + 제목 (h-100, 아래 정렬)
        Row(
            Modifier
                .fillMaxWidth()
                .height(topSpace(100.dp, minExtra = 40.dp))
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                BebeIcons.Back, contentDescription = "뒤로 가기", tint = Bebe.PTitle,
                modifier = Modifier.size(24.dp).clickable(role = Role.Button, onClick = nav::back).padding(bottom = 0.dp),
            )
            Text(task.label, style = bebeText(18.sp, Bebe.PTitle, bold = true, lineHeight = 28.sp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            // 촬영 예시 이미지. 아직 그림이 없는 과제는 자리만 보여줘요
            val img = GUIDE_IMAGE[task]
            if (img != null) {
                Image(
                    painterResource(img),
                    contentDescription = "아이와 2~3m 떨어져서 휴대폰으로 찍는 모습",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                )
            } else {
                Column(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .height(147.dp)
                        .background(Bebe.BrandPhoto, RoundedCornerShape(24.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    Icon(BebeIcons.ImagePlaceholder, contentDescription = null, tint = Bebe.BrandIcon, modifier = Modifier.size(20.dp))
                    Text("촬영 예시 이미지", style = bebeText(11.sp, Bebe.BrandIcon))
                }
            }

            // Figma: "새 기록 3" · "11/20 진료" 칩. 진료일은 아직 저장하는 곳이 없어 새 기록 칩만 보여줘요
            if (count > 0) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Pill("새 기록 $count")
                }
            }

            Spacer(Modifier.height(if (count > 0) 22.dp else 14.dp))
            Text(
                task.label, style = bebeText(26.sp, Bebe.PTitle, bold = true, lineHeight = 31.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(11.dp))
            Text(
                task.guide, style = bebeText(15.sp, Bebe.PBody, lineHeight = 18.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // 준비물: Figma "호명반응" 안내처럼 과제 그림 + 한 줄
            if (task == TaskType.BALL_PLAY || task == TaskType.NAME_RESPONSE) {
                Spacer(Modifier.height(32.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Bebe.BrandSoft, RoundedCornerShape(16.dp))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TaskIllust(task, size = 48.dp)
                    Column {
                        Text("준비물", style = bebeText(13.sp, Bebe.BrandDeep, bold = true))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (task == TaskType.BALL_PLAY) "밝은 단색 공 (지름 10~20cm)" else "별도 준비물 없이 아이 이름만 불러요",
                            style = bebeText(15.sp, Bebe.PTitle, bold = true),
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "찍기 전에 확인해요", style = bebeText(18.sp, Bebe.PTitle, bold = true, lineHeight = 21.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(18.dp))
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
                CHECKLIST.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = Bebe.PGray, modifier = Modifier.size(20.dp))
                        Text(text, style = bebeText(15.sp, Bebe.PTitle, lineHeight = 18.sp))
                    }
                }
            }
        }

        // 하단 버튼
        Divider(Bebe.PLine)
        Box(Modifier.fillMaxWidth().background(Color.White).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomSpace(32.dp))) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Bebe.Brand, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { nav.go(Route.Recording(task)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(BebeIcons.PlayFill, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("촬영 시작", style = bebeText(16.sp, Color.White, bold = true))
            }
        }
    }
}

private fun dateOf(ts: Long): String =
    java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
