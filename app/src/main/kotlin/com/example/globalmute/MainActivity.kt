package com.example.globalmute

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.globalmute.data.MuteRepository
import com.example.globalmute.service.GlobalMuteService
import com.example.globalmute.ui.theme.GlobalMuteTheme
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.globalmute.widget.MuteWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        setContent {
            GlobalMuteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlobalMuteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MuteRepository(context) }
    val isMuted by repo.isMuted.collectAsStateWithLifecycle(initialValue = false)

    var notificationAccessGranted by remember { mutableStateOf(false) }
    var dndAccessGranted by remember { mutableStateOf(false) }
    var batteryOptExempt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notificationAccessGranted = isNotificationListenerEnabled(context)
        dndAccessGranted = isDndAccessGranted(context)
        batteryOptExempt = isBatteryOptimizationExempt(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "GlobalMute",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        PermissionCard(
            title = "Notification Access",
            description = "Required to cancel notifications when muted",
            granted = notificationAccessGranted,
            onRefresh = { notificationAccessGranted = isNotificationListenerEnabled(context) },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
        )

        PermissionCard(
            title = "Do Not Disturb Access",
            description = "Required to enable Total Silence DND mode",
            granted = dndAccessGranted,
            onRefresh = { dndAccessGranted = isDndAccessGranted(context) },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            },
        )

        PermissionCard(
            title = "Battery Optimization Exempt",
            description = "Keeps mute active when the screen is off",
            granted = batteryOptExempt,
            onRefresh = { batteryOptExempt = isBatteryOptimizationExempt(context) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            },
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Global Mute",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (isMuted) "Active – all sounds silenced" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isMuted,
                    onCheckedChange = { newMuted ->
                        scope.launch {
                            repo.setMuted(newMuted)
                            val serviceIntent = Intent(context, GlobalMuteService::class.java).apply {
                                action = if (newMuted) GlobalMuteService.ACTION_MUTE
                                else GlobalMuteService.ACTION_UNMUTE
                            }
                            if (newMuted) {
                                ContextCompat.startForegroundService(context, serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            GlanceAppWidgetManager(context)
                                .getGlanceIds(MuteWidget::class.java)
                                .forEach { id -> MuteWidget().update(context, id) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    color = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!granted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenSettings) { Text("Open Settings") }
                    Button(onClick = onRefresh) { Text("Refresh") }
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    val cn = ComponentName(context, com.example.globalmute.service.MuteNotificationListener::class.java)
    return flat.contains(cn.flattenToString())
}

private fun isDndAccessGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}

private fun isBatteryOptimizationExempt(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
