package com.example.bebeview.record.ui.summary

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.record.api.ApiClient
import com.example.bebeview.ui.components.BebeBottomSheet
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.Divider
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** 서버 만료 시각 → Unix ms. 시간대가 없으면 기기 시간대로 읽음 (웹 new Date() 와 같게) */
private fun parseExpiry(s: String): Long? =
    runCatching { Instant.parse(s).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(s).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()

/** "선생님께 보여주세요" — QR과 6자리 코드. 데모용 QR 이미지(Figma)를 써요. */
@Composable
internal fun ShareSheet(childId: String, hasQuestions: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf(false) }

    // 시트를 열면 서버에서 코드 발급
    LaunchedEffect(childId) {
        try {
            val res = ApiClient.api.createShareCode(childId)
            code = res.code
            expiresAt = parseExpiry(res.expiresAt)
        } catch (_: Exception) {
            error = true
        }
    }
    // 남은 시간은 30초마다 갱신
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val minutes = expiresAt?.let { maxOf(0L, Math.ceil((it - now) / 60000.0).toLong()) } ?: 0L
    // 영상(장면)은 보내지 않아요. 필요하면 보호자 휴대폰에서 직접 보여줘요
    val contents = listOfNotNull("지표", if (hasQuestions) "질문" else null).joinToString(" · ")

    fun sendLink() {
        val c = code ?: return
        // 웹은 navigator.share 로 링크를 보내요. 앱 안 주소는 받는 사람이 열 수 없어서 코드 문구만 보내요
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "베베뷰 진료 요약")
            putExtra(Intent.EXTRA_TEXT, "연결 코드 $c")
        }
        context.startActivity(Intent.createChooser(send, "베베뷰 진료 요약"))
    }

    BebeBottomSheet(onClose = onClose, title = "") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("선생님께 보여주세요", style = bebeText(22.sp, Bebe.PTitle, bold = true))
            Spacer(Modifier.height(8.dp))
            Text("QR을 찍거나 코드를 입력하면 진료 요약이 열려요", style = bebeText(13.sp, Bebe.PGray), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.size(152.dp).border(1.dp, Bebe.PLine, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.qr_sample), contentDescription = "연결 QR 코드", modifier = Modifier.size(128.dp))
            }
            Spacer(Modifier.height(12.dp))
            when {
                error -> Text(
                    "코드 발급에 실패했어요. 다시 시도해주세요",
                    style = bebeText(15.sp, Color(0xFFEF4444), bold = true),
                    textAlign = TextAlign.Center,
                )
                code != null -> {
                    Text(code!!, style = bebeText(26.sp, Bebe.PTitle, bold = true).copy(letterSpacing = 0.04.em))
                    Spacer(Modifier.height(4.dp))
                    Text("${minutes}분 뒤 만료돼요", style = bebeText(13.sp, Bebe.PGray))
                }
                else -> Text("코드 발급 중...", style = bebeText(15.sp, Bebe.PGray))
            }
        }

        Spacer(Modifier.height(20.dp))
        Divider(Bebe.PLine)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("보여줄 내용", style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp))
            Text(
                contents,
                style = bebeText(14.sp, Bebe.PTitle, bold = true, lineHeight = 20.sp),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(BebeIcons.ChevronRight, contentDescription = null, tint = Bebe.PLight, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.width(128.dp).height(54.dp).background(Bebe.PCard, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Text("공유 멈추기", style = bebeText(16.sp, Bebe.PBody, bold = true)) }
            val ready = code != null
            Box(
                Modifier.weight(1f).height(54.dp).alpha(if (ready) 1f else 0.4f)
                    .background(Bebe.Brand, RoundedCornerShape(14.dp))
                    .clickable(enabled = ready, role = Role.Button) { sendLink() },
                contentAlignment = Alignment.Center,
            ) { Text("링크 보내기", style = bebeText(16.sp, Color.White, bold = true)) }
        }
    }
}
