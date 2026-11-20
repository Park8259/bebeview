package com.example.bebeview.record.ui.summary

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.bebeview.record.data.ChildProfile
import com.example.bebeview.record.data.unitOf
import com.example.bebeview.record.ui.charts.jsNum

// 웹의 "PDF 저장"(window.print)에 대응: 진료 요약을 A4 한 장 HTML 로 만들어 안드로이드 인쇄(→ PDF로 저장)로 넘겨요.

/** 인쇄가 끝날 때까지 WebView 가 사라지지 않게 잡아 둠 */
private var printingView: WebView? = null

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")

@SuppressLint("SetJavaScriptEnabled")
internal fun printSummary(context: Context, profile: ChildProfile, age: Int, d: SummaryData, questions: List<String>) {
    val (by, bm, bd) = profile.birthDate.split("-").let { Triple(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }.trimStart('0'), it.getOrElse(2) { "" }.trimStart('0')) }
    val period = if (d.first != null && d.last != null)
        "<p class='muted'>기록 기간 ${fmtYmd(d.first.recordedAt)} ~ ${fmtMd(d.last.recordedAt)} (${d.weeks}주)</p>" else ""
    val taskRows = if (d.taskRows.isEmpty()) "<tr><td class='muted'>아직 기록이 없어요</td></tr>" else
        d.taskRows.joinToString("") { (task, f, l) ->
            "<tr><td><b>${task.label}</b></td><td class='muted'>${jsNum(f.value)}${unitOf(f.unit)} →</td>" +
                "<td><b>${jsNum(l.value)}${unitOf(l.unit)}</b></td><td class='trend'>${trendWord(f.value, l.value, l.unit)}</td></tr>"
        }
    val ambient = if (d.a0 != null && d.a1 != null) """
        <section><h2>공통 지표</h2><table><tr>
        <td><span class='muted'>보호자 지향 비율</span><br>${ambientPct(d.a0, true)}% → <b>${ambientPct(d.a1, true)}%</b></td>
        <td><span class='muted'>활동량 비율</span><br>${ambientPct(d.a0, false)}% → <b>${ambientPct(d.a1, false)}%</b></td>
        </tr></table></section>""" else ""
    val qs = questions.joinToString("") { "<li>${esc(it)}</li>" }

    val html = """
        <html><head><meta charset='utf-8'><style>
        @page { size: A4; margin: 10mm; }
        body { font-family: sans-serif; color: #111; font-size: 13px; }
        h1 { font-size: 22px; } h2 { font-size: 15px; margin: 0 0 10px; }
        section { border: 1px solid #E3E5E8; border-radius: 12px; padding: 14px 16px; margin-top: 12px; }
        table { width: 100%; border-collapse: collapse; } td { padding: 6px 0; }
        .muted { color: #767676; } .trend { color: #0E8DBA; font-weight: bold; text-align: right; }
        li { background: #F3F4F6; border-radius: 10px; padding: 10px 14px; margin-bottom: 6px; list-style: none; }
        ul { padding: 0; }
        </style></head><body>
        <h1>진료 요약</h1>
        <p><b>${esc(profile.name)} (${age}개월)</b> · ${by}년 ${bm}월 ${bd}일생</p>$period
        <p class='muted'>보호자가 집에서 찍은 영상을 바탕으로 한 관찰 기록이에요. 진단이나 발달 판정이 아니에요.</p>
        <section><h2>과제 지표 <span class='muted' style='font-weight:normal;font-size:12px'>첫 기록 → 최근</span></h2><table>$taskRows</table></section>
        $ambient
        <section><h2>수행 이력</h2><table><tr>
        <td>완료한 과제 <b>${d.doneCount}회</b></td><td>기록한 날 <b>${d.dayCount}일</b></td><td>재촬영 <b>${d.retryCount}회</b></td>
        </tr></table></section>
        ${if (qs.isNotEmpty()) "<section><h2>선생님께 물어볼 것</h2><ul>$qs</ul></section>" else ""}
        </body></html>
    """.trimIndent()

    val webView = WebView(context)
    printingView = webView
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val name = "베베뷰 진료 요약 - ${profile.name}"
            pm.print(name, view.createPrintDocumentAdapter(name), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}
