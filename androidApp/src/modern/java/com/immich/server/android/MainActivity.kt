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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import java.util.concurrent.TimeUnit

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
    
    // Battery state
    var batteryState by remember { mutableStateOf(app.batteryMonitor.batteryState.value) }
    
    // Storage state
    var storageState by remember { mutableStateOf(app.storageMonitor.storageState.value) }
    
    // Update states periodically
    LaunchedEffect(Unit) {
        while (true) {
            batteryState = app.batteryMonitor.batteryState.value
            storageState = app.storageMonitor.storageState.value
            delay(1000) // Update every second
        }
    }
    
    // Update storage state when server status changes
    LaunchedEffect(serverStatus) {
        app.storageMonitor.update()
        storageState = app.storageMonitor.storageState.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Immich Server",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = BuildInfo.display,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Battery Status Card
        BatteryStatusCard(batteryState, app.batteryMonitor)

        Spacer(modifier = Modifier.height(8.dp))

        // Storage Status Card
        StorageStatusCard(storageState, app.storageMonitor)

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

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

@Composable
fun BatteryStatusCard(
    batteryState: BatteryMonitor.BatteryState,
    batteryMonitor: BatteryMonitor
) {
    // Determine card color based on overcharging status
    val cardColor = when {
        batteryState.isOvercharging && TimeUnit.MILLISECONDS.toHours(batteryState.timeSinceFullMillis) >= 4 ->
            Color(0xFFFFEBEE) // Red background for severe overcharge
        batteryState.isOvercharging ->
            Color(0xFFFFF8E1) // Yellow background for warning
        batteryState.isFull && batteryState.isCharging ->
            Color(0xFFE8F5E9) // Light green for full and charging
        else ->
            MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconColor = when {
        batteryState.isOvercharging && TimeUnit.MILLISECONDS.toHours(batteryState.timeSinceFullMillis) >= 4 ->
            Color(0xFFF44336) // Red
        batteryState.isOvercharging ->
            Color(0xFFFF9800) // Orange
        batteryState.isFull && batteryState.isCharging ->
            Color(0xFF4CAF50) // Green
        batteryState.isCharging ->
            Color(0xFF2196F3) // Blue
        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with icon and level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            batteryState.isOvercharging -> Icons.Default.Warning
                            batteryState.isFull -> Icons.Default.CheckCircle
                            batteryState.isCharging -> Icons.Default.Info
                            else -> Icons.Default.Info
                        },
                        contentDescription = "Battery",
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "${batteryState.level}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    
                    if (batteryState.isCharging) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡",
                            style = MaterialTheme.typography.titleMedium,
                            color = iconColor
                        )
                    }
                }
                
                // Temperature
                Text(
                    text = "${batteryState.temperature}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (batteryState.temperature > 40) Color(0xFFF44336) 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Charging type
            if (batteryState.isCharging) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "充电方式: ${batteryState.chargeType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Time since full charge - the key feature
            if (batteryState.isFull && batteryState.isCharging && batteryState.timeSinceFullMillis > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val timeSinceFull = batteryMonitor.formatDuration(batteryState.timeSinceFullMillis)
                val warningMessage = batteryMonitor.getOverchargeWarning(batteryState.timeSinceFullMillis)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已充满时间: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = timeSinceFull,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
                
                if (warningMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = warningMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (TimeUnit.MILLISECONDS.toHours(batteryState.timeSinceFullMillis) >= 4) 
                                Color(0xFFF44336) 
                                else Color(0xFFFF9800)
                    )
                }
            }
            
            // Battery health warning
            if (batteryState.health != "Good" && batteryState.health != "Unknown") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "电池状态: ${batteryState.health}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun StorageStatusCard(
    storageState: StorageMonitor.StorageState,
    storageMonitor: StorageMonitor
) {
    // Determine card color based on storage status
    val cardColor = when {
        storageState.isCriticalSpace ->
            Color(0xFFFFEBEE) // Red background for critical
        storageState.isLowSpace ->
            Color(0xFFFFF8E1) // Yellow background for warning
        else ->
            MaterialTheme.colorScheme.surfaceVariant
    }
    
    val progressColor = when {
        storageState.usedPercentage >= 95 ->
            Color(0xFFF44336) // Red
        storageState.usedPercentage >= 90 ->
            Color(0xFFFF9800) // Orange
        storageState.usedPercentage >= 80 ->
            Color(0xFF4CAF50) // Green (still okay)
        else ->
            Color(0xFF2196F3) // Blue (good)
    }
    
    val iconColor = when {
        storageState.isCriticalSpace -> Color(0xFFF44336)
        storageState.isLowSpace -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (storageState.isCriticalSpace || storageState.isLowSpace) 
                            Icons.Default.Warning 
                            else Icons.Default.SdCard,
                        contentDescription = "Storage",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "存储空间",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${storageState.usedPercentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { storageState.usedPercentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = Color(0xFFE0E0E0),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Storage details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "已用: ${storageMonitor.formatSize(storageState.usedSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "可用: ${storageMonitor.formatSize(storageState.freeSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "总计: ${storageMonitor.formatSize(storageState.totalSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Immich: ${storageMonitor.formatSize(storageState.immichUsedSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Estimated capacity
            if (storageState.freeSpace > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Capacity",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = storageMonitor.getStorageRecommendation(storageState),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Warning message
            val warningMessage = storageMonitor.getSpaceWarning(storageState)
            if (warningMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (storageState.isCriticalSpace) Color(0xFFF44336) else Color(0xFFFF9800)
                )
            }
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

    // Auto-refresh every 2 seconds (reduced from 1 second to prevent UI lag)
    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // Only update if logs have changed
                val newLogs = Logger.logs
                if (newLogs.size != logEntries.size) {
                    logEntries = newLogs
                    logCount = newLogs.size
                }
                handler.postDelayed(this, 2000)  // Changed from 1000 to 2000
            }
        }
        handler.post(runnable)
        onDispose { handler.removeCallbacks(runnable) }
    }

    // Auto-scroll to bottom when new logs arrive (only if near bottom)
    LaunchedEffect(logCount) {
        if (autoScroll && logCount > 0) {
            // Check if we're near the bottom before auto-scrolling
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = lastVisibleItem >= logCount - 5
            if (isNearBottom) {
                delay(50)  // Reduced delay
                listState.scrollToItem(logCount - 1)  // Use scrollToItem instead of animateScrollToItem
            }
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
                    // Use index as stable key instead of timestamp-message
                    items(logEntries.size, key = { index -> index }) { index ->
                        val entry = logEntries[index]
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
                                maxLines = 3,  // Reduced from 5 to prevent long messages
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
