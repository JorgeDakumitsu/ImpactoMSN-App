@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.pages

import com.impactomsn.app.calls.CallRingtonePrefs

import com.impactomsn.app.R

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impactomsn.app.ConversationNotificationSettingsActivity
import com.impactomsn.app.appContainer
import com.impactomsn.app.messengerPlusPlainText
import com.impactomsn.app.notifications.InAppNotificationOptions
import com.impactomsn.app.notifications.InAppNotificationPrefs
import com.impactomsn.app.notifications.InAppNotificationType
import com.impactomsn.app.notifications.notificationKey
import com.impactomsn.app.pages.components.ExpandableSettingsGroup
import com.impactomsn.app.pages.components.NotificationCustomizationRow
import com.impactomsn.app.pages.components.PageContentList
import androidx.compose.ui.res.stringResource

@Composable
internal fun NotificationSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    onInAppNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val settings by remember(context) { InAppNotificationPrefs.state(context) }.collectAsState()
    var ringtoneLabel by remember(context) {
        mutableStateOf(CallRingtonePrefs.getUri(context)?.let { uri ->
            runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
        } ?: "System ringtone")
    }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            CallRingtonePrefs.setUri(context, uri)
            ringtoneLabel = uri?.let { runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull() }
                ?: "System ringtone"
        }
    }
    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "incoming-call-ringtone") {
            SegmentedListItem(
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Incoming call ringtone")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, CallRingtonePrefs.getUri(context)
                            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))
                    }
                    ringtonePicker.launch(intent)
                },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().testTag("incoming_call_ringtone"),
                supportingContent = { Text(ringtoneLabel) },
                content = { Text(stringResource(R.string.call_incoming_ringtone)) },
            )
        }
        item(key = "silence-push-when-not-online") {
            ExpandableSettingsGroup(
                expanded = settings.push.silenceWhenNotOnline,
                master = { shapes ->
                    SegmentedListItem(
                        checked = settings.push.silenceWhenNotOnline,
                        onCheckedChange = { enabled ->
                            InAppNotificationPrefs.setSilencePushWhenNotOnline(context, enabled)
                        },
                        shapes = shapes,
                        modifier = Modifier.fillMaxWidth().testTag("silence_push_when_not_online"),
                        trailingContent = {
                            Switch(checked = settings.push.silenceWhenNotOnline, onCheckedChange = null)
                        },
                        supportingContent = {
                            Text(stringResource(R.string.push_silence_nonchat_statuses))
                        },
                        content = { Text(stringResource(R.string.push_silence_not_online)) },
                    )
                },
            ) { shapes, colors ->
                SegmentedListItem(
                    checked = settings.push.includeChatsWhenNotOnline,
                    onCheckedChange = { enabled ->
                        InAppNotificationPrefs.setIncludeChatsWhenNotOnline(context, enabled)
                    },
                    shapes = shapes,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().testTag("include_chat_push_when_not_online"),
                    trailingContent = {
                        Checkbox(
                            checked = settings.push.includeChatsWhenNotOnline,
                            onCheckedChange = null,
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.push_silence_contacts_status))
                    },
                    content = { Text(stringResource(R.string.push_include_contact_group)) },
                )
            }
        }
        settingsSectionSpacer()
        item(key = "in-app-notifications") {
            SegmentedListItem(
                onClick = onInAppNotifications,
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                modifier = Modifier.fillMaxWidth().testTag("in_app_notification_settings"),
                supportingContent = { Text(stringResource(R.string.inapp_notifications_help)) },
                content = { Text(stringResource(R.string.settings_in_app_notifications)) },
            )
        }
    }
}

@Composable
internal fun InAppNotificationSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val settings by remember(context) { InAppNotificationPrefs.state(context) }.collectAsState()
    PageContentList(innerPadding = innerPadding, scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        notificationToggle("In-app notifications", settings.defaults.enabled, true, "in_app_enabled") { enabled ->
            InAppNotificationPrefs.update(context) { it.copy(defaults = it.defaults.copy(enabled = enabled)) }
        }
        notificationOptions(settings.defaults, settings.defaults.enabled, InAppNotificationType.entries,
            onChange = { options -> InAppNotificationPrefs.update(context) { it.copy(defaults = options) } })
    }
}

@Composable
internal fun InAppNotificationSettingsMenu(onCustomizedNotifications: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("in_app_notification_options")) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.notification_options))
        }
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_customized_notifications)) },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    shape = MenuDefaults.standaloneItemShape,
                    modifier = Modifier.testTag("customized_notification_settings"),
                    onClick = { expanded = false; onCustomizedNotifications() },
                )
            }
        }
    }
}

@Composable
internal fun CustomizedNotificationsScreen(innerPadding: PaddingValues, scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val settings by remember(context) { InAppNotificationPrefs.state(context) }.collectAsState()
    val repository = runCatching { context.appContainer.messengerRepository }.getOrNull()
    val contacts = repository?.contacts?.collectAsState()?.value.orEmpty()
    val customizations = settings.conversations.values.sortedBy { it.displayName.lowercase() }
    if (customizations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.custom_notifications_empty), textAlign = TextAlign.Center,
                modifier = Modifier.testTag("no_notification_customizations"))
        }
        return
    }
    PageContentList(innerPadding = innerPadding, scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        items(customizations, key = { it.contactId }) { custom ->
            val name = contacts.firstOrNull { it.id.notificationKey() == custom.contactId }?.displayName
                ?.messengerPlusPlainText() ?: custom.displayName
            NotificationCustomizationRow(
                name = name,
                summary = (if (custom.isGroup) "Group" else "Contact") + " · " +
                    if (!custom.options.enabled && !custom.backgroundEnabled) "Muted" else "Custom alerts",
                contactId = custom.contactId,
                onClick = { context.startActivity(ConversationNotificationSettingsActivity.createIntent(
                    context, custom.contactId, name, custom.isGroup)) },
                onReset = { InAppNotificationPrefs.reset(context, custom.contactId) },
                modifier = Modifier.animateItem(
                    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                ),
            )
        }
    }
}

@Composable
internal fun ConversationNotificationSettingsScreen(
    contactId: String, displayName: String, isGroup: Boolean,
    innerPadding: PaddingValues, scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val settings by remember(context) { InAppNotificationPrefs.state(context) }.collectAsState()
    val custom = settings.conversations[contactId.notificationKey()]
    val options = custom?.options ?: settings.defaults
    fun update(transform: (InAppNotificationOptions) -> InAppNotificationOptions) {
        InAppNotificationPrefs.customize(context, contactId, displayName, isGroup, transform)
    }
    PageContentList(innerPadding = innerPadding, scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleLarge)
            }
        }
        notificationToggle("Use custom settings", custom != null, true, "use_custom_notifications") { enabled ->
            InAppNotificationPrefs.setCustomizationEnabled(context, contactId, displayName, isGroup, enabled)
        }
        item { Text(stringResource(R.string.settings_in_app_notifications), Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall) }
        notificationToggle("In-app alerts for this ${if (isGroup) "group" else "contact"}", options.enabled,
            custom != null, "conversation_notifications_enabled") { enabled ->
            update { it.copy(enabled = enabled) }
        }
        notificationOptions(options, custom != null && options.enabled,
            types = if (isGroup) listOf(InAppNotificationType.GroupMessage, InAppNotificationType.Nudge) else
                listOf(InAppNotificationType.Message, InAppNotificationType.Nudge),
            onChange = { changed -> update { changed } })
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.background_notifications), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.background_notifications_help))
            }
        }
        notificationToggle("Background alerts", custom?.backgroundEnabled ?: true, custom != null, "conversation_background_enabled") { enabled ->
            InAppNotificationPrefs.customizeBackground(context, contactId, displayName, isGroup) { it.copy(backgroundEnabled = enabled) }
        }
        notificationToggle("Silent background alerts", custom?.backgroundSilent ?: false,
            custom != null && custom.backgroundEnabled, "conversation_background_silent") { silent ->
            InAppNotificationPrefs.customizeBackground(context, contactId, displayName, isGroup) { it.copy(backgroundSilent = silent) }
        }
        if (custom != null) item {
            OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = { InAppNotificationPrefs.reset(context, contactId) },
                modifier = Modifier.testTag("reset_conversation_notifications")) { Text(stringResource(R.string.notifications_reset_general)) }
        }
    }
}

private fun LazyListScope.notificationOptions(
    options: InAppNotificationOptions, enabled: Boolean, types: List<InAppNotificationType>,
    onChange: (InAppNotificationOptions) -> Unit,
) {
    notificationToggle("Vibration", options.vibrate, enabled, "in_app_vibration") {
        onChange(options.copy(vibrate = it))
    }
    notificationToggle("Banners", options.banners, enabled, "in_app_banners") {
        onChange(options.copy(banners = it))
    }
    types.forEach { type ->
        item(key = "notification-section-${type.name}") {
            Text(type.label, Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .semantics { heading() },
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        val rule = options.rule(type)
        notificationToggle("Notifications", rule.enabled, enabled, "in_app_type_${type.name}", index = 0, count = 2) {
            onChange(options.copy(rules = options.rules + (type to rule.copy(enabled = it))))
        }
        notificationToggle("Vibration", rule.vibrate,
            enabled && options.vibrate && rule.enabled, "in_app_type_vibration_${type.name}",
            index = 1, count = 2) {
            onChange(options.copy(rules = options.rules + (type to rule.copy(vibrate = it))))
        }
    }
}

private fun LazyListScope.notificationToggle(label: String, checked: Boolean, enabled: Boolean, tag: String,
                                            index: Int = 0, count: Int = 1, onChange: (Boolean) -> Unit) {
    item(key = tag) {
        SegmentedListItem(checked = checked, onCheckedChange = onChange, enabled = enabled,
            shapes = ListItemDefaults.segmentedShapes(index, count),
            modifier = Modifier.fillMaxWidth().testTag(tag),
            trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
            content = { Text(label) })
    }
}
