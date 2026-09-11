package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mneme.app.settings.AppSetupStatus

@Composable
fun SettingsPane(
    setup: AppSetupStatus,
    mediaBusy: Boolean,
    showBanner: Boolean,
    onDismissBanner: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenNotifListener: () -> Unit,
    onRequestMedia: () -> Unit,
    onOpenAllFiles: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestMic: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onScanWhatsApp: () -> Unit,
    onDemoSample: () -> Unit,
    onOpenAppDetails: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showBanner && setup.missingCritical) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(16.dp),
            ) {
                Text(
                    text = "Finish setup",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Turn on notification access and alerts so Retenqo can save important chats and remind you about dues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDismissBanner) {
                    Text("Got it")
                }
            }
        }

        Text(
            text = "Permissions",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Everything stays on this phone. Grant only what you use.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SetupRow(
            title = "Notification access",
            subtitle = "Read notifications from apps you allow, including WhatsApp, Gmail, SMS, Calendar, and more",
            ok = setup.notifListener,
            actionLabel = if (setup.notifListener) "Manage" else "Enable",
            onAction = onOpenNotifListener,
        )
        SetupRow(
            title = "Due reminders",
            subtitle = "Allow Retenqo to post morning due notifications",
            ok = setup.postNotifications,
            actionLabel = if (setup.postNotifications) "On" else "Allow",
            onAction = onRequestPostNotifications,
            enabled = !setup.postNotifications,
        )
        SetupRow(
            title = "Photos & media",
            subtitle = "Scan WhatsApp images and documents into memory",
            ok = setup.canReadMedia,
            actionLabel = if (setup.canReadMedia) "On" else "Allow",
            onAction = onRequestMedia,
            enabled = !setup.canReadMedia,
        )
        SetupRow(
            title = "Exact alarms",
            subtitle = "Fire due reminders on time (optional but recommended)",
            ok = setup.exactAlarms,
            actionLabel = if (setup.exactAlarms) "Manage" else "Allow",
            onAction = onOpenExactAlarm,
        )
        SetupRow(
            title = "Microphone",
            subtitle = "Voice Ask (optional — text Ask works without this)",
            ok = setup.microphone,
            actionLabel = if (setup.microphone) "On" else "Allow",
            onAction = onRequestMic,
            enabled = !setup.microphone,
        )
        SetupRow(
            title = "All files access",
            subtitle = "Only if WhatsApp media scan still fails after Photos",
            ok = setup.allFilesAccess,
            actionLabel = if (setup.allFilesAccess) "Manage" else "Open",
            onAction = onOpenAllFiles,
        )
        SetupRow(
            title = "App context access",
            subtitle = "Optional: let Retenqo understand the foreground app",
            ok = setup.accessibilityService,
            actionLabel = if (setup.accessibilityService) "Manage" else "Enable",
            onAction = onOpenAccessibility,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Actions", style = MaterialTheme.typography.titleLarge)
        SetupRow(
            title = "Scan WhatsApp media",
            subtitle = "OCR recent WA images / docs into notes now",
            ok = false,
            actionLabel = if (mediaBusy) "Scanning…" else "Scan",
            onAction = onScanWhatsApp,
            enabled = !mediaBusy,
            showStatus = false,
        )
        SetupRow(
            title = "Demo sample card",
            subtitle = "Save a sample “pay ₹500” chat without WhatsApp (airplane-mode demo)",
            ok = false,
            actionLabel = if (mediaBusy) "Working…" else "Save",
            onAction = onDemoSample,
            enabled = !mediaBusy,
            showStatus = false,
        )
        SetupRow(
            title = "App system settings",
            subtitle = "Battery, permissions, and notification channels",
            ok = false,
            actionLabel = "Open",
            onAction = onOpenAppDetails,
            enabled = true,
            showStatus = false,
        )
    }
}

@Composable
private fun SetupRow(
    title: String,
    subtitle: String,
    ok: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean = true,
    showStatus: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (showStatus) {
                Text(
                    text = if (ok) "Ready" else "Needed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
            }
            TextButton(
                onClick = onAction,
                enabled = enabled,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
