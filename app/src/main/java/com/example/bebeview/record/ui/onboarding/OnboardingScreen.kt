package com.example.bebeview.record.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.ChildProfile
import com.example.bebeview.record.ui.LocalNav
import com.example.bebeview.record.ui.Route
import com.example.bebeview.ui.components.BottomButton
import com.example.bebeview.ui.components.Field
import com.example.bebeview.ui.components.PageHeader
import com.example.bebeview.ui.components.StepBar
import com.example.bebeview.ui.components.TextBox
import com.example.bebeview.ui.components.strokeIcon
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import java.util.Locale
import java.util.UUID

// growthLens src/screens/onboarding/index.tsx 포팅
// 흐름: 우리 아이 정보("다음") → 출생 정보("완료")
// (Figma는 출생 정보가 먼저였지만 디자인팀 요청으로 순서를 바꿈, 2026-11-20)

private enum class Sheet { Premature, Weeks, Region, Sibling, Gender, BirthDate, Weight, Height }

private const val UNKNOWN = "잘 모르겠어요"

private fun formatDate(iso: String): String {
    val (y, m, d) = iso.split("-")
    return "${y}년 ${m.toInt()}월 ${d.toInt()}일"
}

@Composable
fun OnboardingScreen() {
    val nav = LocalNav.current

    var step by rememberSaveable { mutableIntStateOf(1) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    // 2단계: 출생 정보
    var premature by remember { mutableStateOf<Boolean?>(null) }
    var weeks by remember { mutableStateOf<Measure>(Measure.Unset) }
    var region by remember { mutableStateOf<Pair<String, String>?>(null) }
    var siblingOrder by remember { mutableStateOf<Int?>(null) }

    // 1단계: 우리 아이 정보
    var name by rememberSaveable { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var birthDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf<Measure>(Measure.Unset) }
    var height by remember { mutableStateOf<Measure>(Measure.Unset) }

    val birthValid = premature != null &&
        (premature != true || weeks != Measure.Unset) &&
        region != null &&
        siblingOrder != null

    val childValid = name.isNotBlank() &&
        gender != null &&
        birthDate.isNotEmpty() &&
        weight != Measure.Unset &&
        height != Measure.Unset

    val close = { sheet = null }

    fun handleSubmit() {
        val g = gender ?: return
        val r = region ?: return
        if (!birthValid || !childValid) return
        AppData.setProfile(
            ChildProfile(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                birthDate = birthDate,
                gender = g,
                premature = premature,
                gestationalWeeks = if (premature == true) (weeks as? Measure.Value)?.v?.toInt() else null,
                region = r.first,
                district = r.second,
                siblingOrder = siblingOrder,
                weightKg = (weight as? Measure.Value)?.v,
                heightCm = (height as? Measure.Value)?.v,
                createdAt = System.currentTimeMillis(),
            )
        )
        // 회원가입 마지막 단계: 이용약관
        nav.go(Route.Terms)
    }

    // 2단계에서 뒤로 가기 = 1단계로 (웹의 헤더 뒤로 가기와 같게)
    BackHandler(enabled = step == 2) { step = 1 }

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        if (step == 2) {
            PageHeader("출생 정보") { step = 1 }
            StepBar(step = 3)
            FormColumn {
                Field("조산 여부") {
                    SelectBox(
                        value = when (premature) { null -> ""; true -> "조산"; false -> "만삭 출산" },
                        placeholder = "선택해주세요",
                        active = sheet == Sheet.Premature,
                    ) { sheet = Sheet.Premature }
                }

                // 조산을 골랐을 때만 보이는 칸. 시트는 조산을 고르는 순간 자동으로 열려요
                if (premature == true) {
                    Field("출생 시 임신 주수") {
                        SelectBox(
                            value = when (val w = weeks) {
                                Measure.Unset -> ""
                                Measure.Unknown -> UNKNOWN
                                is Measure.Value -> "${w.v.toInt()}주"
                            },
                            placeholder = "주수를 선택해주세요",
                            active = sheet == Sheet.Weeks,
                        ) { sheet = Sheet.Weeks }
                    }
                }

                Field("거주지역") {
                    SelectBox(
                        value = region?.let { "${it.first} ${it.second}" } ?: "",
                        placeholder = "지역을 선택해주세요",
                        active = sheet == Sheet.Region,
                    ) { sheet = Sheet.Region }
                }

                Field("출생 순서") {
                    SelectBox(
                        value = siblingOrder?.let { SIBLING_LABEL[it] } ?: "",
                        placeholder = "선택해주세요",
                        active = sheet == Sheet.Sibling,
                    ) { sheet = Sheet.Sibling }
                }
            }
            BottomButton("완료", onClick = ::handleSubmit, enabled = birthValid)
        } else {
            PageHeader("우리 아이 정보", onBack = nav::back)
            StepBar(step = 2)
            FormColumn {
                Field("이름") {
                    TextBox(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "이름을 입력해주세요",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }

                Field("성별") {
                    SelectBox(
                        value = when (gender) { null -> ""; "male" -> "남아"; else -> "여아" },
                        placeholder = "선택해주세요",
                        active = sheet == Sheet.Gender,
                    ) { sheet = Sheet.Gender }
                }

                Field("생년월일") {
                    SelectBox(
                        value = if (birthDate.isNotEmpty()) formatDate(birthDate) else "",
                        placeholder = "날짜를 선택해주세요",
                        calendar = true,
                        active = sheet == Sheet.BirthDate,
                    ) { sheet = Sheet.BirthDate }
                }

                Field("몸무게") {
                    SelectBox(
                        value = when (val w = weight) {
                            Measure.Unset -> ""
                            Measure.Unknown -> UNKNOWN
                            is Measure.Value -> String.format(Locale.US, "%.1fkg", w.v)
                        },
                        placeholder = "몸무게를 선택해주세요",
                        active = sheet == Sheet.Weight,
                    ) { sheet = Sheet.Weight }
                }

                Field("키") {
                    SelectBox(
                        value = when (val h = height) {
                            Measure.Unset -> ""
                            Measure.Unknown -> UNKNOWN
                            is Measure.Value -> "${h.v.toInt()}cm"
                        },
                        placeholder = "키를 선택해주세요",
                        active = sheet == Sheet.Height,
                    ) { sheet = Sheet.Height }
                }
            }
            BottomButton("다음", onClick = { step = 2 }, enabled = childValid)
        }
    }

    // 선택 시트
    when (sheet) {
        Sheet.Premature -> PrematureSheet(premature, close) { v ->
            premature = v
            if (v) {
                // 조산을 고르면 바로 임신 주수 시트를 띄움
                sheet = Sheet.Weeks
            } else {
                weeks = Measure.Unset
                close()
            }
        }
        Sheet.Weeks -> WeeksSheet(weeks, close) { weeks = it; close() }
        Sheet.Region -> RegionSheet(region, close) { region = it; close() }
        Sheet.Sibling -> SiblingSheet(siblingOrder, close) { siblingOrder = it; close() }
        Sheet.Gender -> GenderSheet(gender, close) { gender = it; close() }
        Sheet.BirthDate -> BirthDateSheet(birthDate, close) { birthDate = it; close() }
        Sheet.Weight -> WeightSheet(weight, close) { weight = it; close() }
        Sheet.Height -> HeightSheet(height, close) { height = it; close() }
        null -> Unit
    }
}

/** 입력 칸 묶음 (flex-1 px-6 pt-6 space-y-[22px]) */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FormColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) { content() }
}

// ─── SelectBox (FormField.tsx) ───────────────────────────

private val ChevronDown = strokeIcon("chevronDown", 20f, 20f, 1.67f, Color(0xFF555555), "M5 7.5l5 5 5-5")
private val CalendarSmall = strokeIcon(
    "calendar20", 20f, 20f, 1.67f, Color(0xFF555555),
    "M5 4h10a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z",
    "M3 8.5h14M7 2.5v3M13 2.5v3",
)

/** 누르면 선택 시트가 열리는 칸 (h-51, 둥근 10, 테두리 #BBB → 열려 있으면 #555) */
@Composable
private fun SelectBox(
    value: String,
    placeholder: String,
    active: Boolean,
    calendar: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(51.dp)
            .background(Color.White, shape)
            .border(1.dp, if (active) Bebe.InkSub else Bebe.Line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value.ifEmpty { placeholder },
            style = bebeText(16.sp, if (value.isNotEmpty()) Bebe.Ink else Bebe.InkHint, lineHeight = 24.sp),
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            if (calendar) CalendarSmall else ChevronDown,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
    }
}
