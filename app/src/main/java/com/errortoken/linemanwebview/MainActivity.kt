package com.errortoken.linemanwebview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var ghOwner: String
    private lateinit var ghRepo: String

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        ghOwner = getString(R.string.github_owner)
        ghRepo  = getString(R.string.github_repo)

        val ws: WebSettings = web.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                // เปิดลิงก์ภายนอกนอกเว็บแอป
                val host = uri.host ?: ""
                if (!host.contains("github.io") && !host.contains("yourdomain.com")) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return false
            }
        }

        // โหลดเว็บของคุณ
        web.loadUrl("https://errortoken.github.io/test/")

        // เช็คอัพเดตแบบเบื้องหลัง (ไม่บังคับ)
        lifecycleScope.launchWhenStarted {
            runCatching { fetchLatest(ghOwner, ghRepo) }.onSuccess { latest ->
                // คุณจะเลือกทำอะไรกับข้อมูล release ก็ได้
                // เช่น แสดง snackbar หรือเปิดหน้า release
                // ตัวอย่าง (คอมเมนต์ไว้):
                // startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latest.htmlUrl)))
            }
        }
    }

    data class ReleaseInfo(val tag: String, val htmlUrl: String)

    private suspend fun fetchLatest(owner: String, repo: String): ReleaseInfo = withContext(Dispatchers.IO) {
        val api = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        conn.inputStream.use { ins ->
            val body = ins.bufferedReader().readText()
            val o = JSONObject(body)
            ReleaseInfo(
                tag = o.optString("tag_name"),
                htmlUrl = o.optString("html_url")
            )
        }
    }

    override fun onBackPressed() {
        if (this::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
