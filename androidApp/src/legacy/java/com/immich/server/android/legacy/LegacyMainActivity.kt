package com.immich.server.android.legacy

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.immich.server.android.ImmichServerApplication
import com.immich.server.android.R

/**
 * Legacy Activity for Android 4.1+ (API 16-20)
 * Uses XML layout instead of Jetpack Compose
 */
class LegacyMainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var urlText: TextView
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        urlText = findViewById(R.id.urlText)
        toggleButton = findViewById(R.id.toggleButton)

        updateStatus()

        toggleButton.setOnClickListener {
            val app = application as ImmichServerApplication
            if (app.server.isRunning()) {
                app.server.stop()
                stopLegacyService()
            } else {
                app.server.start()
                startLegacyService()
            }
            updateStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val app = application as ImmichServerApplication
        val isRunning = app.server.isRunning()
        statusText.text = "Status: ${if (isRunning) "Running" else "Stopped"}"
        toggleButton.text = if (isRunning) "Stop Server" else "Start Server"

        if (isRunning) {
            val url = app.server.getServerUrl()
            urlText.text = "URL: $url\nAuto-discovery: UDP port 2284"
        } else {
            urlText.text = "Start server to see connection URL"
        }
    }

    private fun startLegacyService() {
        val intent = Intent(this, LegacyServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopLegacyService() {
        val intent = Intent(this, LegacyServerService::class.java)
        stopService(intent)
    }
}
