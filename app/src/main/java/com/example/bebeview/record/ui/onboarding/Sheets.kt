package com.example.bebeview.record.ui.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.ui.components.BebeBottomSheet
import com.example.bebeview.ui.components.OptionCard
import com.example.bebeview.ui.components.SheetActions
import com.example.bebeview.ui.components.SheetDone
import com.example.bebeview.ui.components.strokeIcon
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// growthLens src/screens/onboarding/sheets.tsx 포팅
// 각 시트는 열릴 때마다 새로 그려져요. 그래서 고르는 중인 값(draft)은 처음 값에서 시작하고,
// 확인/선택 완료를 눌러야 바깥에 반영돼요. 취소하거나 바깥을 누르면 그대로 닫혀요.

/** 몸무게·키·임신 주수: 아직 안 고름 / 잘 모르겠어요 / 값 */
sealed interface Measure {
    data object Unset : Measure
    data object Unknown : Measure
    data class Value(val v: Double) : Measure
}

private fun range(from: Int, to: Int): List<Int> = if (to < from) listOf(from) else (from..to).toList()

private fun pad(n: Int) = n.toString().padStart(2, '0')

/** "잘 모르겠어요" 링크 */
@Composable
private fun UnknownLink(onClick: () -> Unit) {
    Text(
        "잘 모르겠어요",
        style = bebeText(14.sp, Bebe.InkSub, bold = true, lineHeight = 20.sp),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** 시트 안 휠 아래 안내 줄 (mt-5 mb-6) */
@Composable
private fun InfoRow(content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
    Spacer(Modifier.height(24.dp))
}

/** 휠 옆 단위 글자 (kg, cm, 주) */
@Composable
private fun UnitLabel(text: String, width: Dp?, size: Int = 20) {
    val m = if (width != null) Modifier.width(width) else Modifier
    Text(text, style = bebeText(size.sp, Bebe.Ink, bold = true), modifier = m)
}

// ─── 성별 ────────────────────────────────────────────────

@Composable
fun GenderSheet(value: String?, onClose: () -> Unit, onSelect: (String) -> Unit) {
    var draft by remember { mutableStateOf(value) }
    BebeBottomSheet(onClose = onClose, title = "성별을 선택해주세요") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("male", "female").forEach { g ->
                OptionCard(
                    selected = draft == g,
                    onClick = { draft = g },
                    modifier = Modifier.weight(1f).height(149.dp),
                ) { color ->
                    BabyIcon(g, color)
                    Spacer(Modifier.height(4.dp))
                    Text(if (g == "male") "남아" else "여아", style = bebeText(17.sp, color, bold = true))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SheetDone(enabled = draft != null, onClick = { draft?.let(onSelect) })
    }
}

/**
 * 아기 얼굴 아이콘. PNG 모양에 글자 색을 칠해서,
 * 카드를 선택하면 글씨와 함께 하늘색으로 바뀌어요.
 */
@Composable
private fun BabyIcon(gender: String, color: Color) {
    val (res, w, h) = if (gender == "male") Triple(R.drawable.icon_baby_boy, 64.dp, 64.dp)
    else Triple(R.drawable.icon_baby_girl, 91.dp, 68.dp)
    Image(
        painterResource(res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color, BlendMode.SrcIn),
        modifier = Modifier.size(w, h),
    )
}

// ─── 생년월일 ────────────────────────────────────────────

/** 고를 수 있는 가장 이른 생일: 오늘부터 4년(48개월) 전. 엔진 참고 연구가 18~48개월 대상 */
private const val MAX_AGE_YEARS = 4

private fun monthsSince(y: Int, m: Int, d: Int, now: LocalDate): Int {
    var months = (now.year - y) * 12 + (now.monthValue - m)
    if (now.dayOfMonth < d) months -= 1
    return maxOf(0, months)
}

/** 달력 아이콘 (18×18, 시트 안 "달력으로 보기") */
private val SmallCalendar = strokeIcon(
    "smallCalendar", 18f, 18f, 1.5f, Color(0xFF555555),
    "M5 3.5h8a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
    "M3 7.5h12M6.5 2v3M11.5 2v3",
)

@Composable
fun BirthDateSheet(value: String, onClose: () -> Unit, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val now = remember { LocalDate.now() }
    val maxY = now.year
    val maxM = now.monthValue
    val maxD = now.dayOfMonth
    val minY = maxY - MAX_AGE_YEARS

    // 처음 열 때 값이 없으면 12개월 전 날짜에서 시작
    val init = remember {
        if (value.isNotEmpty()) value.split("-").map { it.toInt() }
        else now.minusYears(1).let { listOf(it.year, it.monthValue, it.dayOfMonth) }
    }
    var y by remember { mutableIntStateOf(init[0].coerceIn(minY, maxY)) }
    var m by remember { mutableIntStateOf(init[1]) }
    var d by remember { mutableIntStateOf(init[2]) }

    val years = remember { range(minY, maxY) }
    val months = range(if (y == minY) maxM else 1, if (y == maxY) maxM else 12)
    // 연·월이 바뀌어 고른 월/일이 범위를 넘으면 가장 가까운 값으로 맞춤
    val month = m.coerceIn(months.first(), months.last())
    val last = YearMonth.of(y, month).lengthOfMonth()
    val firstDay = if (y == minY && month == maxM) minOf(maxD, last) else 1
    val days = range(firstDay, if (y == maxY && month == maxM) minOf(last, maxD) else last)
    val day = d.coerceIn(days.first(), days.last())
    val iso = "$y-${pad(month)}-${pad(day)}"

    BebeBottomSheet(onClose = onClose, title = "생년월일") {
        WheelGroup(highlight = false) {
            WheelPicker(years, y, { y = it }) { "${it}년" }
            WheelPicker(months, month, { m = it }) { "${it}월" }
            WheelPicker(days, day, { d = it }) { "${it}일" }
        }

        InfoRow {
            Text("현재 ${monthsSince(y, month, day, now)}개월", style = bebeText(14.sp, Bebe.InkSub, lineHeight = 20.sp))
            Row(
                Modifier.clickable(role = Role.Button) {
                    // 웹의 <input type="date"> 달력 → 안드로이드 달력 대화상자
                    val dialog = DatePickerDialog(context, { _, ny, nm, nd ->
                        y = ny; m = nm + 1; d = nd
                    }, y, month - 1, day)
                    val zone = ZoneId.systemDefault()
                    dialog.datePicker.minDate = LocalDate.of(minY, maxM, maxD).atStartOfDay(zone).toInstant().toEpochMilli()
                    dialog.datePicker.maxDate = now.atStartOfDay(zone).toInstant().toEpochMilli()
                    dialog.show()
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(SmallCalendar, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                Text("달력으로 보기", style = bebeText(14.sp, Bebe.InkSub, bold = true, lineHeight = 20.sp))
            }
        }

        SheetActions(onCancel = onClose, onConfirm = { onSelect(iso) })
    }
}

// ─── 몸무게 / 키 ─────────────────────────────────────────

@Composable
fun WeightSheet(value: Measure, onClose: () -> Unit, onSelect: (Measure) -> Unit) {
    val start = (value as? Measure.Value)?.v ?: 9.0
    var whole by remember { mutableIntStateOf(kotlin.math.floor(start).toInt()) }
    var tenth by remember { mutableIntStateOf(Math.round((start - kotlin.math.floor(start)) * 10).toInt()) }

    BebeBottomSheet(onClose = onClose, title = "몸무게", subtitle = "가장 최근에 잰 몸무게를 선택해주세요") {
        WheelGroup {
            Spacer(Modifier.width(56.dp))
            WheelPicker(range(2, 25), whole, { whole = it })
            UnitLabel(".", null)
            WheelPicker(range(0, 9), tenth, { tenth = it })
            UnitLabel("kg", 56.dp)
        }
        InfoRow {
            Text("0.1kg 단위로 선택할 수 있어요", style = bebeText(14.sp, Bebe.InkSub, lineHeight = 20.sp))
            UnknownLink { onSelect(Measure.Unknown) }
        }
        SheetActions(onCancel = onClose, onConfirm = { onSelect(Measure.Value(whole + tenth / 10.0)) })
    }
}

@Composable
fun HeightSheet(value: Measure, onClose: () -> Unit, onSelect: (Measure) -> Unit) {
    var cm by remember { mutableIntStateOf(((value as? Measure.Value)?.v ?: 75.0).toInt()) }
    BebeBottomSheet(onClose = onClose, title = "키", subtitle = "가장 최근에 잰 키를 선택해주세요") {
        WheelGroup {
            Spacer(Modifier.width(96.dp))
            WheelPicker(range(45, 130), cm, { cm = it })
            UnitLabel("cm", 96.dp)
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            UnknownLink { onSelect(Measure.Unknown) }
        }
        Spacer(Modifier.height(24.dp))
        SheetActions(onCancel = onClose, onConfirm = { onSelect(Measure.Value(cm.toDouble())) })
    }
}

// ─── 조산 여부 / 출생 시 임신 주수 ──────────────────────

@Composable
fun PrematureSheet(value: Boolean?, onClose: () -> Unit, onSelect: (Boolean) -> Unit) {
    var draft by remember { mutableStateOf(value) }
    val options = listOf(Triple(true, "조산", "임신 37주 미만"), Triple(false, "만삭 출산", "임신 37주 이상"))
    BebeBottomSheet(onClose = onClose, title = "조산 여부를 선택해주세요") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            options.forEach { (v, label, sub) ->
                val on = draft == v
                OptionCard(
                    selected = on,
                    onClick = { draft = v },
                    modifier = Modifier.weight(1f).height(112.dp),
                ) { color ->
                    Text(label, style = bebeText(17.sp, color, bold = on))
                    Spacer(Modifier.height(8.dp))
                    Text(sub, style = bebeText(11.sp, if (on) color else Color(0xFF888888)))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SheetDone(enabled = draft != null, onClick = { draft?.let(onSelect) })
    }
}

@Composable
fun WeeksSheet(value: Measure, onClose: () -> Unit, onSelect: (Measure) -> Unit) {
    var weeks by remember { mutableIntStateOf(((value as? Measure.Value)?.v ?: 34.0).toInt()) }
    BebeBottomSheet(onClose = onClose, title = "출생 시 임신 주수") {
        WheelGroup {
            Spacer(Modifier.width(96.dp))
            WheelPicker(range(23, 36), weeks, { weeks = it })
            UnitLabel("주", 96.dp, size = 15)
        }
        InfoRow {
            Text("임신 37주 미만이면 조산이에요", style = bebeText(12.sp, Color(0xFF777777), lineHeight = 16.sp))
            UnknownLink { onSelect(Measure.Unknown) }
        }
        SheetActions(onCancel = onClose, onConfirm = { onSelect(Measure.Value(weeks.toDouble())) })
    }
}

// ─── 거주지역 ────────────────────────────────────────────

@Composable
fun RegionSheet(value: Pair<String, String>?, onClose: () -> Unit, onSelect: (Pair<String, String>) -> Unit) {
    var province by remember { mutableStateOf(value?.first ?: PROVINCES[0]) }
    val districts = REGIONS.getValue(province)
    var picked by remember { mutableStateOf(value?.second ?: districts[0]) }
    // 시·도를 바꾸면 그 시·도의 첫 시·군·구로
    val district = if (picked in districts) picked else districts[0]

    BebeBottomSheet(onClose = onClose, title = "거주지역", subtitle = "현재 살고 있는 지역을 선택해주세요") {
        WheelGroup {
            WheelPicker(PROVINCES, province, { province = it })
            key(province) {
                WheelPicker(districts, district, { picked = it })
            }
        }
        Spacer(Modifier.height(24.dp))
        SheetActions(onCancel = onClose, onConfirm = { onSelect(province to district) })
    }
}

// ─── 출생 순서 ───────────────────────────────────────────

@Composable
fun SiblingSheet(value: Int?, onClose: () -> Unit, onSelect: (Int) -> Unit) {
    var draft by remember { mutableStateOf(value) }
    BebeBottomSheet(onClose = onClose, title = "출생 순서를 선택해주세요") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(1, 2, 3).forEach { n ->
                val on = draft == n
                OptionCard(
                    selected = on,
                    onClick = { draft = n },
                    modifier = Modifier.weight(1f).height(97.dp),
                ) { color ->
                    Text(SIBLING_LABEL.getValue(n), style = bebeText(17.sp, color, bold = on))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SheetDone(enabled = draft != null, onClick = { draft?.let(onSelect) })
    }
}
