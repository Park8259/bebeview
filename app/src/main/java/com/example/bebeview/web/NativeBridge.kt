package com.example.bebeview.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat

/**
 * 웹에서 window.BebeviewNative 로 부르는 네이티브 기능.
 * WebView에는 navigator.share 가 없어서, 아래 SHARE_POLYFILL 이 이 객체로 연결해 줌.
 */
class NativeBridge(private val context: Context) {

    @JavascriptInterface
    fun share(title: String?, text: String?, url: String?) {
        // 앱 내부 주소(appassets...)는 받는 사람이 열 수 없으므로 빼고 보냄
        val sharableUrl = url?.takeIf { it.isNotBlank() && Uri.parse(it).host != APP_HOST }
        val body = listOfNotNull(text?.takeIf { it.isNotBlank() }, sharableUrl).joinToString("\n")
        if (body.isEmpty()) return

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        ContextCompat.getMainExecutor(context).execute {
            context.startActivity(Intent.createChooser(send, title?.ifBlank { null }))
        }
    }

    companion object {
        const val JS_NAME = "BebeviewNative"

        /** 페이지 시작 시 주입: navigator.share → 안드로이드 공유 시트 */
        val SHARE_POLYFILL = """
            (function () {
              if (!window.$JS_NAME) return;
              navigator.share = function (data) {
                data = data || {};
                try {
                  window.$JS_NAME.share(String(data.title || ""), String(data.text || ""), String(data.url || ""));
                  return Promise.resolve();
                } catch (e) {
                  return Promise.reject(e);
                }
              };
              navigator.canShare = function () { return true; };
            })();
        """.trimIndent()
    }
}
