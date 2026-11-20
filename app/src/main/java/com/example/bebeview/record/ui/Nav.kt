package com.example.bebeview.record.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.bebeview.record.data.TaskType

/** 앱 화면 목록 (growthLens App.tsx 의 보호자 라우트) */
sealed interface Route {
    // 회원가입 · 로그인
    data object Start : Route                       // /start
    data object Signup : Route                      // /signup
    data object SignupDone : Route                  // /signup/done
    data object Terms : Route                       // /signup/terms
    data object Login : Route                       // /login

    // 보호자 앱
    data object Onboarding : Route                  // /onboarding
    data object Tasks : Route                       // /tasks (하단 탭: 과제)
    data class TaskGuide(val task: TaskType) : Route // /task-guide/:taskType
    data class Recording(val task: TaskType) : Route // /recording/:taskType
    /** /result/:taskType — 방금 저장한 기록 id 로 결과를 보여줘요 */
    data class Result(val task: TaskType, val recordId: String) : Route
    data object Timeline : Route                    // /timeline (하단 탭: 기록)
    data object Charts : Route                      // /charts (하단 탭: 변화)
    data object ClinicSummary : Route               // /clinic-summary (하단 탭: 진료 요약)
    data class Replay(val recordId: String) : Route // /replay/:sessionId (지난 기록 상세)
}

/** 화면 이동. 웹의 navigate() 에 대응 */
interface AppNav {
    /** navigate(path) — 새 화면을 위에 쌓음 */
    fun go(route: Route)
    /** navigate(path, { replace: true }) — 지금 화면을 바꿈 */
    fun replace(route: Route)
    /** 기록을 모두 지우고 이 화면 하나만 남김 (로그인 완료, 처음으로 등) */
    fun reset(route: Route)
    /** 하단 탭 이동: [과제, 선택한 탭] 만 남김 */
    fun tab(route: Route)
    /** navigate(-1) */
    fun back()
    /** 로그인 완료 / 가입 완료 처리 (세션 저장 후 첫 화면으로) */
    fun finishAuth()
}

val LocalNav = staticCompositionLocalOf<AppNav> { error("AppNav 가 없어요") }
