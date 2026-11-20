package com.example.bebeview.record.ui.auth

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.R
import com.example.bebeview.record.api.ApiClient
import com.example.bebeview.record.api.AuthTokenHolder
import com.example.bebeview.record.api.LoginRequest
import com.example.bebeview.record.api.RegisterRequest
import com.example.bebeview.record.api.SendCodeRequest
import com.example.bebeview.record.api.VerifyCodeRequest
import com.example.bebeview.record.api.errorMessageOf
import com.example.bebeview.record.data.ParentSession
import com.example.bebeview.ui.components.BigButton
import com.example.bebeview.ui.components.BottomButton
import com.example.bebeview.ui.components.Field
import com.example.bebeview.ui.components.PageHeader
import com.example.bebeview.ui.components.PasswordBox
import com.example.bebeview.ui.components.SideButton
import com.example.bebeview.ui.components.SideTone
import com.example.bebeview.ui.components.StepBar
import com.example.bebeview.ui.components.SvgIcons
import com.example.bebeview.ui.components.TextBox
import com.example.bebeview.ui.components.bottomSpace
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// growthLens src/screens/auth/index.tsx 포팅
// Figma 와이어프레임 > 정보입력: 01 시작 · 02/03 회원가입 계정 · 04 아이 정보 입력 안내 · 07 이용약관 · 08 로그인

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val PASSWORD_RE = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}$") // 영문, 숫자 포함 8자 이상
private const val CODE_SEC = 5 * 60

/** 화면 바탕: 흰색, 키보드가 올라오면 그만큼 줄어듦 */
@Composable
private fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).imePadding(), content = content)
}

/** 서버 오류 안내 (웹에는 서버가 없어서 없던 부분) */
@Composable
private fun ErrorText(msg: String, modifier: Modifier = Modifier) {
    if (msg.isNotEmpty()) Text(msg, style = bebeText(12.5.sp, Bebe.Error), modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
/** 01 시작 */
@Composable
fun StartScreen(onSignup: () -> Unit, onLogin: () -> Unit) {
    Screen {
        Column(
            Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 24.dp, top = topSpace(162.dp)),
        ) {
            Text(
                "아이의 일상을 기록하고\n진료에 연결해요",
                style = bebeText(28.sp, Bebe.Title, bold = true, lineHeight = 41.sp),
            )
            Spacer(Modifier.height(10.dp))
            Text("이메일로 간편하게 가입하고 시작해보세요", style = bebeText(15.sp, Bebe.Desc))
        }
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = bottomSpace(38.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BigButton("회원가입", onSignup)
            BigButton("로그인", onLogin, container = Bebe.GrayButton, content = Bebe.Title)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 02·03 회원가입 ① 계정 (이메일 인증) */
@Composable
fun SignupScreen(onDone: (ParentSession) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var sentAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var verified by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    // 파일럿 서버는 메일 대신 응답으로 인증번호를 돌려줘요. 있을 때만 화면에 보여줘요
    var devCode by remember { mutableStateOf<String?>(null) }

    // 인증번호 남은 시간
    LaunchedEffect(sentAt, verified) {
        if (sentAt == null || verified) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val left = sentAt?.let { maxOf(0, CODE_SEC - ((now - it) / 1000).toInt()) } ?: 0

    val emailOk = EMAIL_RE.matches(email.trim())
    val passwordOk = PASSWORD_RE.matches(password)
    val match = confirm.isNotEmpty() && confirm == password
    val canNext = name.isNotBlank() && verified && passwordOk && match

    fun sendCode() {
        errorMsg = ""
        scope.launch {
            try {
                devCode = ApiClient.api.sendEmailCode(SendCodeRequest(email.trim())).devCode
                sentAt = System.currentTimeMillis()
                now = sentAt!!
                code = ""
            } catch (e: Exception) {
                errorMsg = errorMessageOf(e)
            }
        }
    }

    fun checkCode() {
        errorMsg = ""
        scope.launch {
            try {
                ApiClient.api.verifyEmailCode(VerifyCodeRequest(email.trim(), code))
                verified = true
            } catch (e: Exception) {
                errorMsg = errorMessageOf(e)
            }
        }
    }

    fun next() {
        errorMsg = ""
        loading = true
        scope.launch {
            try {
                val res = ApiClient.api.register(RegisterRequest(name.trim(), email.trim(), password))
                AuthTokenHolder.token = res.token
                onDone(ParentSession(res.parentId, res.name, res.email, res.token))
            } catch (e: Exception) {
                errorMsg = errorMessageOf(e)
            } finally {
                loading = false
            }
        }
    }

    Screen {
        PageHeader("회원가입", onBack)
        StepBar(step = 1)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 19.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Field("보호자 이름") {
                TextBox(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "이름을 입력해주세요",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }

            Field("이메일") {
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    TextBox(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "example@email.com",
                        enabled = !verified,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    SideButton(
                        label = if (sentAt == null) "인증번호 받기" else "재전송",
                        tone = if (sentAt == null) SideTone.Brand else SideTone.Gray,
                        enabled = emailOk && !verified,
                        onClick = ::sendCode,
                    )
                }
            }

            Column {
                Field("인증번호") {
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        TextBox(
                            value = if (verified) "●●●●●●" else code,
                            onValueChange = { v -> code = v.filter(Char::isDigit).take(6) },
                            placeholder = "인증번호 6자리",
                            enabled = sentAt != null && !verified,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                            trailing = if (sentAt != null && !verified) {
                                {
                                    Text(
                                        "%d:%02d".format(left / 60, left % 60),
                                        style = bebeText(13.sp, Bebe.Error, bold = true)
                                            .copy(fontFeatureSettings = "tnum"),
                                    )
                                }
                            } else null,
                        )
                        SideButton(
                            label = if (verified) "인증 완료" else "확인",
                            tone = if (verified) SideTone.Done else SideTone.Gray,
                            enabled = !verified && code.length == 6 && left > 0,
                            onClick = ::checkCode,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val (help, helpColor, helpBold) = when {
                    verified -> Triple("✓ 이메일 인증이 완료됐어요", Bebe.Link, true)
                    sentAt != null && left == 0 -> Triple("인증 시간이 지났어요. 재전송을 눌러주세요", Bebe.Error, false)
                    sentAt != null && devCode != null -> Triple("테스트용 인증번호: $devCode (메일 발송 전 파일럿 단계)", Bebe.Help, false)
                    else -> Triple("입력한 이메일로 인증번호를 보내드려요", Bebe.Help, false)
                }
                Text(help, style = bebeText(12.5.sp, helpColor, bold = helpBold))
            }

            Field("비밀번호") {
                PasswordBox(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "영문, 숫자 포함 8자 이상",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                )
            }
            if (password.isNotEmpty() && !passwordOk) {
                Text("영문과 숫자를 섞어 8자 이상 입력해주세요", style = bebeText(12.5.sp, Bebe.Error))
            }

            Column {
                Field("비밀번호 확인") {
                    PasswordBox(
                        value = confirm,
                        onValueChange = { confirm = it },
                        placeholder = "비밀번호를 한 번 더 입력해주세요",
                        highlight = match,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                }
                if (confirm.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (match) "✓ 비밀번호가 일치해요" else "비밀번호가 일치하지 않아요",
                        style = bebeText(12.5.sp, if (match) Bebe.Link else Bebe.Error, bold = true),
                    )
                }
            }

            ErrorText(errorMsg)
            Spacer(Modifier.height(8.dp))
        }

        BottomButton("다음", onClick = ::next, enabled = canNext, loading = loading)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 04 회원가입 · 아이 정보 입력 안내 */
@Composable
fun SignupDoneScreen(onNext: () -> Unit) {
    Screen {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(71.dp).background(Bebe.Brand, CircleShape), contentAlignment = Alignment.Center) {
                Image(SvgIcons.BigCheck, contentDescription = null)
            }
            Spacer(Modifier.height(24.dp))
            Text("계정이 만들어졌어요", style = bebeText(15.sp, Bebe.Link, bold = true))
            Spacer(Modifier.height(16.dp))
            Text(
                "이제 우리 아이에 대해\n알려주세요",
                style = bebeText(26.sp, Bebe.Title, bold = true, lineHeight = 31.sp),
                textAlign = TextAlign.Center,
            )
        }
        BottomButton("아이 정보 입력하기", onClick = onNext)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 07 회원가입 ④ 이용약관 (마지막) */
private data class TermsSection(val title: String, val items: List<String>)

private val TERMS = listOf(
    TermsSection(
        "1. 서비스 개요 및 목적",
        listOf(
            "'베베뷰'는 보호자가 영유아 자녀의 일상 행동을 영상으로 기록하고, 진료에 활용할 수 있는 관찰 자료로 확인·공유할 수 있도록 지원하는 모바일 앱입니다.",
            "관찰 과제 안내, 영상 기반 기록, 시점별 변화 확인, 진료 요약 공유 기능을 제공합니다.",
            "의료 진단이나 전문 치료 목적이 아닌, 관찰 기록 및 참고용 서비스입니다.",
        ),
    ),
    TermsSection(
        "2. 회원가입 및 이용 조건",
        listOf(
            "만 14세 이상만 회원가입이 가능하며, 부모나 보호자만 자녀 정보를 입력할 수 있습니다.",
            "이메일 인증을 완료해야 가입할 수 있으며, 본인 인증 및 계정 보안을 책임져야 합니다.",
            "아이 1명당 1개의 관찰 기록을 생성할 수 있습니다.",
            "부정행위(타인 정보 도용, 다중 계정 생성 등)가 확인되면 계정이 제한될 수 있습니다.",
        ),
    ),
    TermsSection(
        "3. 주요 기능 및 제공 범위",
        listOf(
            "관찰 과제 안내: 공 주고받기, 호명 반응, 동작 모방 등 촬영 방법을 안내합니다.",
            "영상 기록: 촬영한 영상은 이 기기 안에서만 분석되며, 기록과 변화를 확인할 수 있습니다.",
            "진료 요약 공유: 보호자가 허용한 기록만 의사에게 공유되며, 영상은 공유되지 않습니다.",
            "제공되는 수치는 진단이 아닌 관찰 기록이며, 판단은 의료진 상담을 통해 이루어집니다.",
        ),
    ),
)

@Composable
fun TermsScreen(onFinish: () -> Unit, onBack: () -> Unit) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    val body = bebeText(14.sp, Bebe.Title, bold = true, lineHeight = 22.sp)

    Screen {
        PageHeader("베베뷰 이용약관", onBack)
        StepBar(step = 4)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 24.dp),
        ) {
            Text(
                "'베베뷰' 앱을 이용하시기 전, 아래 약관 내용을 꼭 확인해주세요. 본 요약은 전체 이용약관의 주요 내용을 담고 있으며, 전체 약관은 앱 내의 [이용약관]에서 다시 확인하실 수 있습니다.",
                style = body,
            )
            TERMS.forEach { sec ->
                Spacer(Modifier.height(32.dp))
                Text(sec.title, style = bebeText(17.sp, Bebe.Title, bold = true, lineHeight = 20.sp))
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    sec.items.forEach { t ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("-", style = body)
                            Text(t, style = body)
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Bebe.LineDivider))
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = bottomSpace(38.dp)),
        ) {
            Row(
                Modifier.clickable(role = Role.Checkbox) { agreed = !agreed },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val boxShape = RoundedCornerShape(6.dp)
                Box(
                    Modifier
                        .size(26.dp)
                        .background(if (agreed) Bebe.Brand else Color.White, boxShape)
                        .border(2.dp, if (agreed) Bebe.Brand else Bebe.CheckOff, boxShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (agreed) Image(SvgIcons.BoxCheck, contentDescription = null)
                }
                Text("(필수) 베베뷰 이용약관에 동의합니다.", style = bebeText(15.sp, Bebe.Title, bold = true))
            }
            Spacer(Modifier.height(18.dp))
            BigButton("가입 완료", onFinish, enabled = agreed)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
/** 08 로그인 */
@Composable
fun LoginScreen(onDone: (ParentSession) -> Unit, onBack: () -> Unit, onSignup: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val canLogin = EMAIL_RE.matches(email.trim()) && password.isNotEmpty()

    fun login() {
        if (!canLogin || loading) return
        errorMsg = ""
        loading = true
        scope.launch {
            try {
                val res = ApiClient.api.login(LoginRequest(email.trim(), password))
                AuthTokenHolder.token = res.token
                onDone(ParentSession(res.parentId, res.name, res.email, res.token))
            } catch (e: Exception) {
                errorMsg = errorMessageOf(e)
            } finally {
                loading = false
            }
        }
    }

    Screen {
        PageHeader("로그인", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 65.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.height(31.dp))
                Image(painterResource(R.drawable.logo_wordmark), contentDescription = "베베뷰", modifier = Modifier.height(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("다시 만나서 반가워요", style = bebeText(26.sp, Bebe.Title, bold = true, lineHeight = 31.sp))
            Spacer(Modifier.height(16.dp))
            Text("가입한 이메일로 로그인해주세요", style = bebeText(15.sp, Bebe.Desc))

            Spacer(Modifier.height(42.dp))
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Field("이메일") {
                    TextBox(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "example@email.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                }
                Field("비밀번호") {
                    PasswordBox(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "비밀번호를 입력해주세요",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { login() }),
                    )
                }
                ErrorText(errorMsg)
            }
            Spacer(Modifier.height(if (errorMsg.isEmpty()) 44.dp else 31.dp))
            BigButton("로그인", ::login, enabled = canLogin, loading = loading)

            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = bottomSpace(24.dp)),
                horizontalArrangement = Arrangement.spacedBy(38.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 서버 연결: 비밀번호 찾기 화면 (웹에도 아직 없음)
                Text("비밀번호 찾기", style = bebeText(13.5.sp, Bebe.Desc))
                Box(Modifier.width(1.dp).height(16.dp).background(Bebe.LinkDivider))
                Text(
                    "회원가입",
                    style = bebeText(13.5.sp, Bebe.Link).copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onSignup),
                )
            }
        }
    }
}
