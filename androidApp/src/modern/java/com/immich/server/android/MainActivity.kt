package com.immich.server.android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.immich.server.android.ui.theme.ImmichServerTheme
import com.immich.server.platform.Logger
import kotlinx.coroutines.delay

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
                    MainScreen()
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
fun MainScreen() {
    var showLogs by remember { mutableStateOf(false) }

    if (showLogs) {
        LogViewerScreen(onBack = { showLogs = false })
    } else {
        ServerControlScreen(onViewLogs = { showLogs = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerControlScreen(onViewLogs: () -> Unit) {
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

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onViewLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Logs")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    var logEntries by remember { mutableStateOf(Logger.logs) }
    var autoScroll by remember { mutableStateOf(true) }
    var logCount by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-refresh every 1 second
    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                logEntries = Logger.logs
                logCount = logEntries.size
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
        onDispose { handler.removeCallbacks(runnable) }
    }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logCount) {
        if (autoScroll && logCount > 0) {
            delay(100)
            listState.animateScrollToItem(logCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Logs (${logCount})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Logger.clearLogs()
                        logEntries = emptyList()
                        logCount = 0
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                    IconButton(onClick = {
                        logEntries = Logger.logs
                        logCount = logEntries.size
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Auto-scroll toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { autoScroll = !autoScroll }) {
                    Text(
                        text = if (autoScroll) "Auto-scroll: ON" else "Auto-scroll: OFF",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                TextButton(onClick = {
                    val text = logEntries.joinToString("\n") { "${it.timestamp} [${it.level}] ${it.message}" }
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                    Toast.makeText(
                        ImmichServerApplication.instance,
                        "Logs copied to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("Copy All", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            if (logEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No logs yet.\nStart the server to see logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                ) {
                    items(logEntries, key = { "${it.timestamp}-${it.message}" }) { entry ->
                        val textColor = when (entry.level) {
                            "E" -> Color(0xFFFF6B6B)
                            "W" -> Color(0xFFFFD93D)
                            "I" -> Color(0xFF6BCB77)
                            "D" -> Color(0xFF4D96FF)
                            else -> Color(0xFFCCCCCC)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${entry.timestamp} ",
                                color = Color(0xFF888888),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "[${entry.level}] ",
                                color = textColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = entry.message,
                                color = Color(0xFFCCCCCC),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
