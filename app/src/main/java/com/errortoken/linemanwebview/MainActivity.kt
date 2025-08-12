package com.errortoken.linemanwebview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.WindowInsetsController
import android.webkit.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
  private lateinit var webView: WebView
  private lateinit var refresh: SwipeRefreshLayout
  private var downloadId: Long = -1L

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    window.insetsController?.systemBarsBehavior =
      WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    WebView.setWebContentsDebuggingEnabled(true)

    refresh = findViewById(R.id.refresh)
    webView = findViewById(R.id.webview)

    with(webView.settings) {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      useWideViewPort = true
      loadWithOverviewMode = true
      setSupportZoom(true)
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
        refresh.isRefreshing = false
      }
      override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
        refresh.isRefreshing = false
      }
    }
    webView.webChromeClient = WebChromeClient()

    refresh.setOnRefreshListener { webView.reload() }

    val startUrl = getString(R.string.start_url)
    webView.loadUrl(startUrl)

    // เช็คอัปเดตจาก GitHub Releases
    Thread {
      try {
        val (latestCode, apkUrl) = fetchLatest(getString(R.string.github_owner), getString(R.string.github_repo))
        val current = packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        if (latestCode > current && apkUrl.isNotEmpty()) {
          runOnUiThread { downloadAndInstall(apkUrl) }
        }
      } catch (_: Exception) {}
    }.start()

    // รับการแจ้งเตือนดาวน์โหลดเสร็จ
    registerReceiver(object: BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
        if (id == downloadId) onDownloadComplete(id)
      }
    }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
  }

  private fun fetchLatest(getString(R.string.github_owner), getString(R.string.github_repo)): Pair<Int,String> {
    val api = "https://api.github.com/repos/$owner/$repo/releases/latest"
    val conn = URL(api).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000; conn.readTimeout = 8000
    conn.setRequestProperty("Accept", "application/vnd.github+json")
    conn.inputStream.use {
      val text = BufferedReader(InputStreamReader(it)).readText()
      val json = JSONObject(text)
      val tag = json.optString("tag_name","v0")
      val code = tag.removePrefix("v").toIntOrNull() ?: 0
      var apk = ""
      val assets = json.optJSONArray("assets")
      if (assets != null) for (i in 0 until assets.length()) {
        val a = assets.getJSONObject(i)
        if (a.optString("name","").endsWith(".apk")) {
          apk = a.optString("browser_download_url",""); break
        }
      }
      return Pair(code, apk)
    }
  }

  private fun downloadAndInstall(url: String) {
    try {
      val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
      val req = DownloadManager.Request(Uri.parse(url))
        .setTitle("Downloading update") .setDescription("Please wait…")
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "lineman_update.apk")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      downloadId = dm.enqueue(req)
    } catch (_: Exception) { }
  }

  private fun onDownloadComplete(id: Long) {
    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    val c: Cursor = dm.query(DownloadManager.Query().setFilterById(id))
    if (c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
      val uri = dm.getUriForDownloadedFile(id)
      val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      startActivity(intent)
    }
    c.close()
  }

  override fun onBackPressed() {
    if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
  }
}
