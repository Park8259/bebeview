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
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText

// growthLens src/screens/taskGuide/index.tsx 포팅
// Figma 부모 버전 "공 주고받기" — 상단 제목줄과 하단 버튼은 프레임 밖 부품을 조립함

/** 촬영 예시 이미지가 있는 과제 */
private val GUIDE_IMAGE: Map<TaskType, Int> = mapOf(TaskType.NAME_RESPONSE to R.drawable.guide_name_response)

private val CHECKLIST = listOf(
    BebeIcons.Phone to "폰을 세워 두세요",
    BebeIcons.Ruler to "아이 전신이 보이도록 2~3m 떨어뜨려 주세요",
    BebeIcons.Sun to "밝은 곳에서 찍어 주세요",
    BebeIcons.Pin to "매번 같은 장소에서 찍으면 더 정확해요",
)

@Composable
fun TaskGuideScreen(task: TaskType) {
    val nav = LocalNav.current
    val count = AppData.records.count { it.taskType == task.apiName }

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

            // 진료일 칩은 뺐어요: 보호자가 실제로 잡은 진료가 아니라 데모 데이터라서
            if (count > 0) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("기록 ${count}회")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                task.label, style = bebeText(26.sp, Bebe.PTitle, bold = true, lineHeight = 31.sp),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                task.guide, style = bebeText(16.sp, Bebe.PBody, lineHeight = 24.sp),
                modifier = Modifier.padding(horizontal = 10.dp),
            )

            // 준비물: Figma "호명반응" 안내처럼 과제 그림 + 한 줄
            if (task == TaskType.BALL_PLAY || task == TaskType.NAME_RESPONSE) {
                Spacer(Modifier.height(20.dp))
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

            Spacer(Modifier.height(32.dp))
            Text(
                "찍기 전에 확인해요", style = bebeText(18.sp, Bebe.PTitle, bold = true, lineHeight = 28.sp),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                CHECKLIST.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = Bebe.PGray, modifier = Modifier.size(20.dp))
                        Text(text, style = bebeText(15.sp, Bebe.PTitle))
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
                Icon(BebeIcons.Video, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("촬영 시작", style = bebeText(16.sp, Color.White, bold = true))
            }
        }
    }
}
