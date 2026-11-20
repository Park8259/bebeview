package com.example.bebeview.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** 웹 앱 전체를 띄우는 화면. 카메라 권한, 뒤로 가기, 외부 링크를 네이티브로 처리 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    val pendingRequest = remember { mutableStateOf<PermissionRequest?>(null) }

    // 웹이 카메라를 요청 → 안드로이드 권한 요청 → 결과대로 웹에 허용/거부
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val request = pendingRequest.value ?: return@rememberLauncherForActivityResult
        pendingRequest.value = null
        val granted = request.resources.filter { res ->
            androidPermissionFor(res)?.let { isGranted(context, it) } == true
        }
        if (granted.isEmpty()) request.deny() else request.grant(granted.toTypedArray())
    }

    // 웹의 <input type="file"> → 안드로이드 파일 선택 창 → 고른 파일을 웹에 전달
    val pendingFileCallback = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingFileCallback.value?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        pendingFileCallback.value = null
    }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val assetLoader = createAppAssetLoader(ctx.assets)
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.WHITE)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true // localStorage (zustand persist)
                    mediaPlaybackRequiresUserGesture = false // 카메라 미리보기·신호음 자동 재생
                    allowFileAccess = false
                    allowContentAccess = false
                    textZoom = 100 // 시스템 글꼴 크기로 디자인이 깨지지 않게
                }
                if (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    WebView.setWebContentsDebuggingEnabled(true) // chrome://inspect 로 디버깅
                }

                addJavascriptInterface(NativeBridge(ctx), NativeBridge.JS_NAME)
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(this, NativeBridge.SHARE_POLYFILL, setOf(APP_ORIGIN))
                }

                webViewClient = AppWebViewClient(assetLoader) { canGoBack = it }
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        if (request.origin.host != APP_HOST) {
                            request.deny(); return
                        }
                        val supported = request.resources.filter { androidPermissionFor(it) != null }
                        if (supported.isEmpty()) {
                            request.deny(); return
                        }
                        val missing = supported.mapNotNull(::androidPermissionFor)
                            .filterNot { isGranted(context, it) }
                        if (missing.isEmpty()) {
                            request.grant(supported.toTypedArray())
                        } else {
                            pendingRequest.value?.deny()
                            pendingRequest.value = request
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest) {
                        if (pendingRequest.value == request) pendingRequest.value = null
                    }

                    override fun onShowFileChooser(
                        view: WebView,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        params: FileChooserParams,
                    ): Boolean {
                        // 이전 요청이 남아 있으면 취소 처리해야 다음 선택이 동작함
                        pendingFileCallback.value?.onReceiveValue(null)
                        pendingFileCallback.value = filePathCallback
                        return try {
                            fileChooserLauncher.launch(params.createIntent()) // accept="video/*" 반영됨
                            true
                        } catch (_: ActivityNotFoundException) {
                            pendingFileCallback.value = null
                            false
                        }
                    }
                }

                loadUrl("$APP_ORIGIN/")
                webView = this
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}

private class AppWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onHistoryChanged: (canGoBack: Boolean) -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        assetLoader.shouldInterceptRequest(request.url)

    /** 앱 내부 주소는 WebView 안에서, 그 밖의 링크(논문 등)는 브라우저로 */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == APP_HOST) return false
        try {
            view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        } catch (_: ActivityNotFoundException) {
        }
        return true
    }

    // React Router의 pushState 이동도 여기로 들어옴
    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        onHistoryChanged(view.canGoBack())
    }
}

private fun androidPermissionFor(resource: String): String? = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
    else -> null
}

private fun isGranted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
