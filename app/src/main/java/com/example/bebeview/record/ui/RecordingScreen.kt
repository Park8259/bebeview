package com.example.bebeview.record.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.bebeview.record.api.ApiClient
import com.example.bebeview.record.api.MetricUploadRequest
import com.example.bebeview.record.data.AnalysisResult
import com.example.bebeview.record.data.AppData
import com.example.bebeview.record.data.FaceTrace
import com.example.bebeview.record.data.ParentSessionStore
import com.example.bebeview.record.data.TaskType
import com.example.bebeview.record.data.correctedAgeMonths
import com.example.bebeview.record.engine.BallPlayAnalyzer
import com.example.bebeview.record.engine.FaceAnalyzer
import com.example.bebeview.record.engine.ImitationAnalyzer
import com.example.bebeview.record.engine.PoseAnalyzer
import com.example.bebeview.record.engine.NameResponseAnalyzer
import com.example.bebeview.record.ui.recording.AnalyzingView
import com.example.bebeview.record.ui.recording.CheckRow
import com.example.bebeview.record.ui.recording.FilePickButton
import com.example.bebeview.record.ui.recording.LastReplay
import com.example.bebeview.record.ui.recording.LastReplayHolder
import com.example.bebeview.record.ui.recording.detectCallTime
import com.example.bebeview.ui.components.BebeIcons
import com.example.bebeview.ui.components.TaskIcon
import com.example.bebeview.ui.components.bottomSpace
import com.example.bebeview.ui.components.strokeIcon
import com.example.bebeview.ui.components.topSpace
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

// growthLens screens/recording/index.tsx 포팅
// Figma 부모 버전: "촬영"(준비) → "촬영 다음"(녹화 중) → "영상 분석"
// 카메라는 CameraX, 분석은 기기 안 MediaPipe(FaceAnalyzer) 로 해요.

private enum class Phase { INIT, PREP, COUNTDOWN, RECORDING, ANALYZING, ERROR }

private const val COUNTDOWN_SEC = 5
private val BEEP_AT = listOf(5, 10, 15) // 녹화 시작 후 신호음 시각(초)

/** 녹화 중인 CameraX Recording. 시작 시각(ms)과 완료(Finalize) 때 최종 파일을 돌려주는 핸들 */
private class ActiveRecording(val recording: Recording, val outputFile: File) {
    val finalized = CompletableDeferred<File?>()
    var startMs = SystemClock.elapsedRealtime()
    /** 그만 찍기 / 처음부터: 결과를 버림 */
    var discard = false
}

private val CloseIcon = strokeIcon("close", 12f, 12f, 1.83f, Color.White, "M1 1l10 10M11 1L1 11")

private fun mmss(sec: Int) = "%02d:%02d".format(sec / 60, sec % 60)

@Composable
fun RecordingScreen(taskType: TaskType) {
    val task = taskType
    val nav = LocalNav.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val duration = task.durationSec

    var phase by remember { mutableStateOf(Phase.INIT) }
    var countdown by remember { mutableIntStateOf(COUNTDOWN_SEC) }
    var elapsed by remember { mutableIntStateOf(0) }
    var beeps by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf(false) }
    var bright by remember { mutableStateOf<Boolean?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    var cameraGranted by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val active = remember { mutableStateOf<ActiveRecording?>(null) }
    val stimulusMs = remember { mutableStateOf<Long?>(null) }
    val previewView = remember { PreviewView(context) }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    /** 분석 끝: 기록 저장 + 서버 업로드, 영상과 좌표는 결과 화면 다시 보기용으로 메모리에만 둠 */
    fun finish(video: File, result: AnalysisResult, trace: FaceTrace?) {
        LastReplayHolder.set(trace?.let { LastReplay(task, video, it) })
        val record = AppData.addRecord(task, System.currentTimeMillis(), result)
        uploadSessionRecord(context, task, result)
        nav.replace(Route.Result(task, record.id))
    }

    /** 영상 하나 분석 (녹화한 영상, 데모 파일 공통) */
    suspend fun analyze(video: File, stimulus: Long?, info: String) {
        try {
            val (result, trace) = withContext(Dispatchers.Default) {
                val stimulusMs = stimulus ?: (BEEP_AT[0] * 1000L)
                when (task) {
                    TaskType.NAME_RESPONSE ->
                        NameResponseAnalyzer.analyzeWithTrace(FaceAnalyzer.extractFaceTrack(context, video), stimulusMs)
                    TaskType.IMITATION ->
                        ImitationAnalyzer.analyze(PoseAnalyzer.extract(context, video), stimulusMs) to null
                    TaskType.BALL_PLAY ->
                        BallPlayAnalyzer.analyze(PoseAnalyzer.extract(context, video, withBall = true)) to null
                }
            }
            finish(video, result, trace)
        } catch (e: Exception) {
            errorMsg = "분석 중 문제가 생겼어요: ${e.message ?: e.javaClass.simpleName}\n[$info]"
            phase = Phase.ERROR
        }
    }

    fun stopAndAnalyze() {
        val a = active.value ?: return
        phase = Phase.ANALYZING
        a.recording.stop()
        scope.launch {
            val file = a.finalized.await()
            stopCamera()
            if (file == null) {
                errorMsg = "녹화된 영상을 찾지 못했어요"
                phase = Phase.ERROR
                return@launch
            }
            val mb = "%.1f".format(file.length() / 1024.0 / 1024.0)
            analyze(file, stimulusMs.value, "녹화: ${mb}MB, video/mp4")
        }
    }

    /** 그만 찍기 / 닫기: 저장하지 않고 나감 */
    fun handleQuit() {
        active.value?.let { it.discard = true; it.recording.stop() }
        active.value = null
        nav.back()
    }

    /** 처음부터: 지금까지 찍은 것은 버리고 준비 화면으로 */
    fun handleRestart() {
        active.value?.let { it.discard = true; it.recording.stop() }
        active.value = null
        elapsed = 0
        beeps = 0
        countdown = COUNTDOWN_SEC
        phase = Phase.PREP
    }

    // 데모용: 촬영 대신 영상 파일로 분석. 이름을 부른 시점은 영상 소리에서 찾고, 못 찾으면 첫 신호음(5초)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        stopCamera()
        phase = Phase.ANALYZING
        scope.launch {
            val file = withContext(Dispatchers.IO) { copyToCache(context, uri) }
            if (file == null) {
                errorMsg = "영상 파일을 읽지 못했어요"
                phase = Phase.ERROR
                return@launch
            }
            val stimulus = if (task.needsStimulus) {
                withContext(Dispatchers.Default) { detectCallTime(file) } ?: (BEEP_AT[0] * 1000L)
            } else null
            analyze(file, stimulus, "파일: ${file.name}")
        }
    }
    val pickFile = { filePicker.launch("video/*") }

    // 카메라 권한 (웹처럼 촬영 화면에 들어올 때 요청)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.CAMERA] == true || isGranted(context, Manifest.permission.CAMERA)) {
            cameraGranted = true
        } else {
            errorMsg = "카메라를 쓸 수 없어요. 설정에서 카메라를 허용해 주세요."
            phase = Phase.ERROR
        }
    }
    LaunchedEffect(Unit) {
        val missing = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).filterNot { isGranted(context, it) }
        if (missing.isEmpty()) cameraGranted = true else permissionLauncher.launch(missing.toTypedArray())
    }

    // 카메라 켜기
    LaunchedEffect(cameraGranted) {
        if (!cameraGranted) return@LaunchedEffect
        try {
            val provider = suspendCancellableCoroutine { cont ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
            }
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
            val capture = VideoCapture.withOutput(recorder)
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            cameraProvider = provider
            videoCapture = capture
            if (phase == Phase.INIT) phase = Phase.PREP
        } catch (e: Exception) {
            // 권한은 있는데 카메라 연결에 실패한 경우 (다른 앱이 사용 중, 기기 오류 등)
            errorMsg = "카메라를 켜지 못했어요. 다른 앱이 카메라를 쓰고 있다면 닫고 다시 시도해 주세요."
            phase = Phase.ERROR
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            active.value?.let { it.discard = true; it.recording.stop() }
            cameraProvider?.unbindAll()
        }
    }

    // 준비 화면: 1초마다 화면 밝기 확인 (평균 밝기 60/255 이상이면 충분)
    LaunchedEffect(phase) {
        if (phase != Phase.PREP && phase != Phase.COUNTDOWN) return@LaunchedEffect
        while (true) {
            delay(1000)
            previewView.bitmap?.let { bright = averageLuma(it) >= 60 }
        }
    }

    // 카운트다운 (시작을 누른 뒤 5초) → 녹화 시작
    LaunchedEffect(phase) {
        if (phase != Phase.COUNTDOWN) return@LaunchedEffect
        countdown = COUNTDOWN_SEC
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        val rec = startRecording(context, videoCapture)
        if (rec == null) {
            errorMsg = "녹화를 시작하지 못했어요"
            phase = Phase.ERROR
            return@LaunchedEffect
        }
        active.value = rec
        stimulusMs.value = null
        elapsed = 0
        beeps = 0
        phase = Phase.RECORDING
    }

    // 녹화 타이머: 1초마다 초를 올리고, 신호음 시각이면 소리, 끝나면 분석
    LaunchedEffect(phase) {
        if (phase != Phase.RECORDING) return@LaunchedEffect
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        try {
            var sec = 0
            while (sec < duration) {
                delay(1000)
                sec++
                elapsed = sec
                if (task.needsStimulus && sec in BEEP_AT) {
                    if (stimulusMs.value == null) {
                        stimulusMs.value = SystemClock.elapsedRealtime() - (active.value?.startMs ?: SystemClock.elapsedRealtime())
                    }
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    beeps++
                    flash = true
                    scope.launch { delay(400); flash = false }
                }
            }
            // 끝났다는 소리 (호명 반응은 이름 부르는 신호와 헷갈리지 않게 생략)
            if (task != TaskType.NAME_RESPONSE) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                delay(200)
            }
        } finally {
            tone.release()
        }
        stopAndAnalyze()
    }

    BackHandler(enabled = phase == Phase.RECORDING) { handleQuit() }

    // ── 화면 ──
    when (phase) {
        Phase.ERROR -> {
            Column(
                Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Text(errorMsg, style = bebeText(15.sp, Bebe.PBody), textAlign = TextAlign.Center)
                Box(
                    Modifier.height(54.dp).background(Bebe.PCard, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { nav.back() }.padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("돌아가기", style = bebeText(16.sp, Bebe.PBody, bold = true)) }
                FilePickButton(pickFile)
            }
            return
        }
        Phase.ANALYZING -> {
            AnalyzingView(task)
            return
        }
        else -> Unit
    }

    val live = phase == Phase.RECORDING
    val overlay by animateColorAsState(
        if (flash) Bebe.Brand.copy(alpha = 0.4f) else Bebe.PDark.copy(alpha = 0.4f),
        tween(200), label = "flash",
    )
    val headerTop = topSpace(58.dp, minExtra = 8.dp)

    Box(Modifier.fillMaxSize().background(Bebe.PDark)) {
        // 카메라 화면
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(overlay))

        if (live) {
            // 상단: 닫기 · 과제 이름 · 녹화 시간
            Row(
                Modifier.fillMaxWidth().padding(top = topSpace(57.dp, minExtra = 8.dp), start = 23.dp, end = 23.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .clickable(role = Role.Button) { handleQuit() },
                    contentAlignment = Alignment.Center,
                ) { Icon(CloseIcon, "닫기", tint = Color.White, modifier = Modifier.size(12.dp)) }
                Box(
                    Modifier.height(36.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(task.label, style = bebeText(14.sp, Color.White, bold = true, lineHeight = 20.sp)) }
                Row(
                    Modifier.height(36.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(8.dp).background(Color(0xFFFF4D4D), CircleShape))
                    Text(
                        mmss(elapsed),
                        style = bebeText(14.sp, Color.White, bold = true, lineHeight = 20.sp).copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
            // 가운데: 신호 횟수 / 남은 시간
            Column(
                Modifier.fillMaxWidth().padding(top = 426.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.height(53.dp).background(Color(0xFF263035), CircleShape).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TaskIcon(task, size = 20.dp, tint = Color.White)
                    Text(
                        if (task.needsStimulus) "신호 ${beeps}번" else "남은 시간 ${mmss(maxOf(0, duration - elapsed))}",
                        style = bebeText(20.sp, Color.White, bold = true, lineHeight = 28.sp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("아이 전신이 화면에 들어오게 해주세요", style = bebeText(13.sp, Color.White))
            }
        } else {
            // 상단: 뒤로 + "폰 세우기"
            Box(Modifier.fillMaxWidth().padding(top = headerTop).height(40.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(40.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .clickable(role = Role.Button) { handleQuit() },
                    contentAlignment = Alignment.Center,
                ) { Icon(BebeIcons.Back, "뒤로 가기", tint = Color.White, modifier = Modifier.size(22.dp)) }
                Text("폰 세우기", style = bebeText(16.sp, Color.White, bold = true, lineHeight = 24.sp))
            }
            // 점검 목록
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = headerTop + 58.dp, start = 24.dp, end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CheckRow(bright == true, if (bright == false) "조금 더 밝은 곳으로 옮겨주세요" else "밝기가 충분해요")
                CheckRow(false, "아이 전신이 보이게 해주세요")
                if (task == TaskType.BALL_PLAY) CheckRow(false, "공이 화면에 보이게 해주세요")
            }
        }

        // 하단 시트
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = bottomSpace(40.dp)),
        ) {
            if (live) {
                Text(
                    "${if (task.needsStimulus) "소리가 나면 바로 시작해주세요" else "아이를 보며 편하게 놀아주세요"} · 끝나면 소리로 알려드려요",
                    style = bebeText(14.sp, Bebe.PGray, lineHeight = 20.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterHorizontally)) {
                    RoundButton("그만 찍기") { handleQuit() }
                    RoundButton("처음부터") { handleRestart() }
                }
            } else {
                Text(task.prepTitle, style = bebeText(20.sp, Bebe.PTitle, bold = true, lineHeight = 28.sp), modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "시작을 누르고 아이 앞에 앉으면 ${COUNTDOWN_SEC}초 뒤 촬영이 시작되고, " +
                        "${if (duration == 60) "1분" else "${duration}초"} 뒤 " +
                        "${if (task == TaskType.NAME_RESPONSE) "자동으로 끝나요" else "소리로 알려드려요"}.",
                    style = bebeText(14.sp, Bebe.PGray, lineHeight = 24.sp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(20.dp))
                if (phase == Phase.COUNTDOWN) {
                    Box(
                        Modifier.fillMaxWidth().height(58.dp).background(Bebe.PCard, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${countdown}초 뒤 촬영 시작", style = bebeText(16.sp, Bebe.PBody, bold = true)) }
                } else {
                    val ready = phase == Phase.PREP
                    Box(
                        Modifier.fillMaxWidth().height(58.dp)
                            .background(if (ready) Bebe.Brand else Bebe.PCard, RoundedCornerShape(14.dp))
                            .clickable(enabled = ready, role = Role.Button) { phase = Phase.COUNTDOWN },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (phase == Phase.INIT) "카메라 준비 중..." else "시작",
                            style = bebeText(16.sp, if (ready) Color.White else Bebe.PLight, bold = true),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FilePickButton(pickFile) }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.width(140.dp).height(52.dp).background(Bebe.Brand, CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = bebeText(16.sp, Color.White, bold = true)) }
}

private fun isGranted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** 32×18 로 줄여서 평균 밝기(0~255) */
private fun averageLuma(src: Bitmap): Double {
    val small = Bitmap.createScaledBitmap(src, 32, 18, true)
    val px = IntArray(32 * 18)
    small.getPixels(px, 0, 32, 0, 0, 32, 18)
    var sum = 0.0
    for (c in px) {
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        sum += 0.299 * r + 0.587 * g + 0.114 * b
    }
    return sum / px.size
}

private fun startRecording(context: Context, videoCapture: VideoCapture<Recorder>?): ActiveRecording? {
    val capture = videoCapture ?: return null
    val outputFile = File(context.cacheDir, "task_${System.currentTimeMillis()}.mp4")
    val options = FileOutputOptions.Builder(outputFile).build()
    var holder: ActiveRecording? = null
    val pending = capture.output.prepareRecording(context, options).let {
        // 마이크 권한이 있으면 소리도 같이 녹음 (분석에는 안 쓰지만 데모 파일과 같은 형식)
        if (isGranted(context, Manifest.permission.RECORD_AUDIO)) it.withAudioEnabled() else it
    }
    val recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
        val h = holder ?: return@start
        when (event) {
            is VideoRecordEvent.Start -> h.startMs = SystemClock.elapsedRealtime()
            is VideoRecordEvent.Finalize -> {
                val ok = !event.hasError() || event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
                if (h.discard) {
                    outputFile.delete()
                    h.finalized.complete(null)
                } else {
                    h.finalized.complete(if (ok && outputFile.exists()) outputFile else null)
                }
            }
        }
    }
    holder = ActiveRecording(recording, outputFile)
    return holder
}

/** 고른 영상을 앱 캐시로 복사 (분석·다시 보기용) */
private fun copyToCache(context: Context, uri: Uri): File? = try {
    val out = File(context.cacheDir, "demo_${System.currentTimeMillis()}.mp4")
    val copied = context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
    if (copied == null) null else out
} catch (_: Exception) {
    null
}

/**
 * 과제 결과를 서버에 업로드 (growthLens api/client.ts uploadSessionRecord).
 * 메인 지표 + 공통(ambient) 지표를 각각 하나씩. 아이 생년월일은 보내지 않고 이 순간의 교정 월령만 보냄.
 * 실패해도 기기 기록은 이미 저장돼 있으므로 조용히 무시. 화면이 바뀌어도 끝까지 보내도록 별도 스코프에서.
 */
private fun uploadSessionRecord(context: Context, task: TaskType, result: AnalysisResult) {
    val profile = AppData.profile.value
    val childId = profile?.id ?: ParentSessionStore.load(context)?.parentId ?: return
    val ageMonths = profile?.let { correctedAgeMonths(it.birthDate, it.gestationalWeeks).toDouble() }
    val calls = buildList {
        result.metric?.let { add(MetricUploadRequest(childId, task.apiName, "${task.apiName}_metric", it.value, ageMonths)) }
        result.ambient?.let { a ->
            add(MetricUploadRequest(childId, "ambient", "bothInFrameSec", a.bothInFrameSec, ageMonths))
            add(MetricUploadRequest(childId, "ambient", "caregiverOrientSec", a.caregiverOrientSec, ageMonths))
            add(MetricUploadRequest(childId, "ambient", "activitySec", a.activitySec, ageMonths))
            add(MetricUploadRequest(childId, "ambient", "contactSec", a.contactSec, ageMonths))
        }
    }
    if (calls.isEmpty()) return
    CoroutineScope(Dispatchers.IO).launch {
        calls.forEach { runCatching { ApiClient.api.uploadMetric(it) } }
    }
}
