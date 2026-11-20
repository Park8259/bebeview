package com.example.bebeview.web

import android.content.res.AssetManager
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/** 앱 안의 웹 화면이 열리는 주소. https라서 카메라·WASM 등 보안 컨텍스트 기능이 동작함 */
const val APP_HOST = WebViewAssetLoader.DEFAULT_DOMAIN // appassets.androidplatform.net
const val APP_ORIGIN = "https://$APP_HOST"

/**
 * assets/www (웹 빌드 결과)를 https://appassets.androidplatform.net/ 루트로 제공.
 * 웹 코드가 "/images/..", "/models/.." 처럼 절대 경로를 쓰므로 루트("/")에 붙임.
 * React Router(BrowserRouter) 경로(/tasks, /doctor/patients/3 등)는 index.html로 돌려줌.
 */
class SpaAssetPathHandler(private val assets: AssetManager) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse {
        val clean = path.trim('/')
        if (clean.split('/').any { it == ".." }) return notFound()

        val target = clean.ifEmpty { "index.html" }
        open(target)?.let { return it }

        // 확장자가 없는 경로 = 화면 경로 → SPA 진입점
        val isRoute = !target.substringAfterLast('/').contains('.')
        if (isRoute) open("index.html")?.let { return it }
        return notFound()
    }

    private fun open(relative: String): WebResourceResponse? = try {
        val stream = assets.open("$ROOT/$relative")
        val mime = mimeTypeOf(relative)
        val encoding = if (mime.startsWith("text/") || mime.endsWith("json")) "utf-8" else null
        WebResourceResponse(mime, encoding, stream)
    } catch (_: FileNotFoundException) {
        null
    }

    private fun notFound() =
        WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(ByteArray(0)))

    companion object {
        private const val ROOT = "www"

        private val MIME = mapOf(
            "html" to "text/html",
            "js" to "text/javascript",
            "mjs" to "text/javascript",
            "css" to "text/css",
            "json" to "application/json",
            "wasm" to "application/wasm", // WebAssembly.instantiateStreaming 에 필요
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            "woff" to "font/woff",
            "woff2" to "font/woff2",
            "ttf" to "font/ttf",
            "mp3" to "audio/mpeg",
            "wav" to "audio/wav",
            "mp4" to "video/mp4",
            "webm" to "video/webm",
        )

        fun mimeTypeOf(path: String): String =
            MIME[path.substringAfterLast('.', "").lowercase()] ?: "application/octet-stream"
    }
}

fun createAppAssetLoader(assets: AssetManager): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .setDomain(APP_HOST)
        .addPathHandler("/", SpaAssetPathHandler(assets))
        .build()
