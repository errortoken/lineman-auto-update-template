package com.errortoken.linemanwebview

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)

    val online = isOnline()
    startActivity(Intent(this, if (online) MainActivity::class.java else OfflineActivity::class.java))
    finish()
  }

  private fun isOnline(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java)
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
