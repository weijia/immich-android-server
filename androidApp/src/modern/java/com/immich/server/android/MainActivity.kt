package com.immich.server.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.immich.server.android.ui.theme.ImmichServerTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity onCreate")

        val app = ImmichServerApplication.instance
        Log.d(TAG, "Server isRunning=${app.server.isRunning()}")

        setContent {
            ImmichServerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerControlScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MainActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "MainActivity onPause")
    }
}

@Composable
fun ServerControlScreen() {
    val app = ImmichServerApplication.instance
    var serverStatus by remember { mutableStateOf(if (app.server.isRunning()) "Running" else "Stopped") }
    var serverUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Immich Server",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Status: $serverStatus",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Port: 2283",
            style = MaterialTheme.typography.bodyMedium
        )

        if (serverUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Server URL: $serverUrl",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Log.i("MainActivity", "Button clicked, current status: $serverStatus")
                if (app.server.isRunning()) {
                    Log.i("MainActivity", "Stopping server...")
                    app.server.stop()
                    serverStatus = "Stopped"
                    serverUrl = ""
                    Log.i("MainActivity", "Server stopped")
                } else {
                    Log.i("MainActivity", "Starting server...")
                    app.server.start()
                    serverStatus = "Running"
                    serverUrl = app.server.getServerUrl()
                    Log.i("MainActivity", "Server started, URL: $serverUrl")
                }
            }
        ) {
            Text(if (app.server.isRunning()) "Stop Server" else "Start Server")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (serverUrl.isNotEmpty()) {
                "Connect with Immich app at $serverUrl"
            } else {
                "Start server to see connection URL"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Or use auto-discovery (UDP broadcast on port 2284)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
