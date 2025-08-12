package com.errortoken.linemanwebview

import android.content.Intent
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OfflineActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_offline)

    findViewById<Button>(R.id.btnRetry).setOnClickListener {
      if (isOnline()) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
      }
    }
  }
  private fun isOnline(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java)
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
