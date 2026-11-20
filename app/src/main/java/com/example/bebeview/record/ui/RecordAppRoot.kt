package com.example.bebeview.record.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.ParentSession
import com.example.bebeview.record.data.ParentSessionStore
import com.example.bebeview.record.ui.auth.LoginScreen
import com.example.bebeview.record.ui.auth.SignupDoneScreen
import com.example.bebeview.record.ui.auth.SignupScreen
import com.example.bebeview.record.ui.auth.StartScreen
import com.example.bebeview.record.ui.auth.TermsScreen
import com.example.bebeview.record.ui.charts.ChartsScreen
import com.example.bebeview.record.ui.onboarding.OnboardingScreen
import com.example.bebeview.record.ui.result.ReplayScreen
import com.example.bebeview.record.ui.result.ResultScreen
import com.example.bebeview.record.ui.summary.ClinicSummaryScreen
import com.example.bebeview.record.ui.tasks.TaskGuideScreen
import com.example.bebeview.record.ui.tasks.TodayTasksScreen
import com.example.bebeview.record.ui.timeline.TimelineScreen
import com.example.bebeview.ui.components.Splash
import kotlinx.coroutines.delay

private const val SPLASH_MS = 1200L

/**
 * 앱 진입점 (growthLens App.tsx).
 * 로그인 세션이 없으면 시작(회원가입·로그인), 있으면 아이 정보가 있을 때 오늘의 과제, 없으면 아이 정보 입력.
 */
@Composable
fun RecordAppRoot() {
    val context = LocalContext.current
    remember { AppData.init(context) }

    var session by remember { mutableStateOf(ParentSessionStore.load(context)) }
    // 회원가입으로 계정은 만들었지만 약관 동의 전인 세션 (가입 완료 때 저장)
    var pendingSession by remember { mutableStateOf<ParentSession?>(null) }

    fun firstRoute(): Route = when {
        session == null && pendingSession == null -> Route.Start
        AppData.profile.value != null -> Route.Tasks
        else -> Route.Onboarding
    }

    val stack = remember { mutableStateListOf(firstRoute()) }
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SPLASH_MS)
        splash = false
    }

    val nav = remember {
        object : AppNav {
            override fun go(route: Route) { stack.add(route) }
            override fun replace(route: Route) { stack[stack.lastIndex] = route }
            override fun reset(route: Route) { stack.clear(); stack.add(route) }
            override fun tab(route: Route) {
                stack.clear()
                stack.add(Route.Tasks)
                if (route != Route.Tasks) stack.add(route)
            }
            override fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
            override fun finishAuth() {
                pendingSession?.let { ParentSessionStore.save(context, it); session = it }
                pendingSession = null
                reset(Route.Tasks)
            }
        }
    }

    // 안드로이드 뒤로 가기 = 웹의 navigate(-1)
    BackHandler(enabled = stack.size > 1) { nav.back() }

    CompositionLocalProvider(LocalNav provides nav) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            when (val r = stack.last()) {
                Route.Start -> StartScreen(
                    onSignup = { nav.go(Route.Signup) },
                    onLogin = { nav.go(Route.Login) },
                )
                Route.Signup -> SignupScreen(
                    onDone = { s -> pendingSession = s; nav.go(Route.SignupDone) },
                    onBack = nav::back,
                )
                Route.SignupDone -> SignupDoneScreen(onNext = { nav.replace(Route.Onboarding) })
                Route.Terms -> TermsScreen(onFinish = nav::finishAuth, onBack = nav::back)
                Route.Login -> LoginScreen(
                    onDone = { s ->
                        ParentSessionStore.save(context, s)
                        session = s
                        nav.reset(if (AppData.profile.value != null) Route.Tasks else Route.Onboarding)
                    },
                    onBack = nav::back,
                    onSignup = { nav.go(Route.Signup) },
                )
                Route.Onboarding -> OnboardingScreen()
                Route.Tasks -> TodayTasksScreen()
                is Route.TaskGuide -> TaskGuideScreen(r.task)
                is Route.Recording -> RecordingScreen(r.task)
                is Route.Result -> ResultScreen(r.task, r.recordId)
                Route.Timeline -> TimelineScreen()
                Route.Charts -> ChartsScreen()
                Route.ClinicSummary -> ClinicSummaryScreen()
                is Route.Replay -> ReplayScreen(r.recordId)
            }
            // 스크롤한 내용이 상태바 뒤로 비치지 않게 흰 바탕을 깔아요 (웹에는 상태바가 없음).
            // 촬영 화면은 카메라가 화면 끝까지 보이도록 제외
            if (stack.last() !is Route.Recording) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color.White),
                )
            }
            if (splash) Splash()
        }
    }
}

/** 처음으로 (LogoReset): 아이 정보와 로그인을 지우고 시작 화면으로. 기록은 남겨요 */
fun resetToStart(context: android.content.Context, nav: AppNav) {
    AppData.clearProfile()
    ParentSessionStore.clear(context)
    nav.reset(Route.Start)
}
