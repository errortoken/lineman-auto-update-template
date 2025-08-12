package com.errortoken.linemanwebview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
  private lateinit var webView: WebView
  private var downloadId: Long = -1L

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    WebView.setWebContentsDebuggingEnabled(true)

    webView = findViewById(R.id.webview)
    with(webView.settings) {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      setSupportZoom(true)
      useWideViewPort = true
      loadWithOverviewMode = true
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    webView.webViewClient = object : WebViewClient() {
      override fun onReceivedError(
        view: WebView, request: WebResourceRequest, error: WebResourceError
      ) {
        view.loadData(
          "<html><body style='font-family:sans-serif;color:#fff;background:#121212;padding:24px'>"+
          "<h3>Load failed</h3><p>${error.description}</p></body></html>",
          "text/html","utf-8"
        )
      }
    }
    webView.webChromeClient = WebChromeClient()

    val startUrl = getString(R.string.start_url)
    webView.loadUrl(startUrl)

    // ตรวจอัปเดต (ไม่ทำงานบนเธรดหลัก)
    Thread {
      try {
        val (latestCode, apkUrl) = fetchLatestFromGitHub("errortoken","lineman-auto-update-template")
        val current = packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        if (latestCode > current && apkUrl.isNotEmpty()) {
          runOnUiThread {
            MaterialAlertDialogBuilder(this)
              .setTitle("Update available")
              .setMessage("A newer version is available. Install now?")
              .setPositiveButton("Install") { _, _ -> downloadAndInstall(apkUrl) }
              .setNegativeButton("Later", null)
              .show()
          }
        }
      } catch (_: Exception) {}
    }.start()

    registerReceiver(object: BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
        if (id == downloadId) onDownloadComplete(id)
      }
    }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
  }

  private fun fetchLatestFromGitHub(owner: String, repo: String): Pair<Int,String> {
    val api = "https://api.github.com/repos/$owner/$repo/releases/latest"
    val conn = URL(api).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000; conn.readTimeout = 8000
    conn.setRequestProperty("Accept", "application/vnd.github+json")
    conn.inputStream.use { ins ->
      val text = BufferedReader(InputStreamReader(ins)).readText()
      val json = JSONObject(text)
      val tag = json.optString("tag_name","v0")
      val code = tag.trim().removePrefix("v").toIntOrNull() ?: 0
      var apk = ""
      val assets = json.optJSONArray("assets")
      if (assets != null) {
        for (i in 0 until assets.length()) {
          val a = assets.getJSONObject(i)
          if (a.optString("name","").endsWith(".apk")) {
            apk = a.optString("browser_download_url",""); break
          }
        }
      }
      return Pair(code, apk)
    }
  }

  private fun downloadAndInstall(url: String) {
    try {
      val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
      val req = DownloadManager.Request(Uri.parse(url))
        .setTitle("Downloading update")
        .setDescription("Please wait...")
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true).setAllowedOverRoaming(true)
      downloadId = dm.enqueue(req)
    } catch (_: Exception) { }
  }

  private fun onDownloadComplete(id: Long) {
    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    val c: Cursor = dm.query(DownloadManager.Query().setFilterById(id))
    if (c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
      val uri = dm.getUriForDownloadedFile(id)
      val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      startActivity(install)
    }
    c.close()
  }
}
