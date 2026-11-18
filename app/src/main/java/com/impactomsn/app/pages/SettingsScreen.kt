@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.pages

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.impactomsn.app.AppearanceColorOption
import com.impactomsn.app.AppearancePrefs
import com.impactomsn.app.BuildConfig
import com.impactomsn.app.ColorMode
import com.impactomsn.app.ContactInvitationPrefs
import com.impactomsn.app.DisplayPictureSyncBehavior
import com.impactomsn.app.DisplayPictureSyncPrefs
import com.impactomsn.app.EmoticonPrefs
import com.impactomsn.app.MESSENGER_PLUS_DISCLAIMER
import com.impactomsn.app.MessengerPlusPrefs
import com.impactomsn.app.R
import com.impactomsn.app.SessionPrefs
import com.impactomsn.app.UiStyle
import com.impactomsn.app.WinkCatalogActivity
import com.impactomsn.app.appContainer
import com.impactomsn.app.displayPictureSync
import com.impactomsn.app.notifications.BackgroundConnectionController
import com.impactomsn.app.notifications.BackgroundDeliveryPrefs
import com.impactomsn.app.pages.components.PageContentList
import com.impactomsn.app.pages.components.ExpandableSettingsGroup
import com.impactomsn.app.updates.updateManager

enum class SettingsDestination(
    @StringRes val titleRes: Int,
) {
    Root(R.string.settings_title),
    Appearance(R.string.settings_appearance),
    Emoticons(R.string.settings_emoticons_winks),
    MessengerPlus(R.string.settings_messenger_plus),
    ContactInvitations(R.string.settings_contact_invitations),
    Transfers(R.string.settings_transfers),
    Notifications(R.string.settings_notifications),
    InAppNotifications(R.string.settings_in_app_notifications),
    CustomizedNotifications(R.string.settings_customized_notifications),
    Connection(R.string.settings_connection),
    ConnectionInformation(R.string.settings_connection_info),
    Updates(R.string.settings_updates),
    Licenses(R.string.settings_licenses),
    Language(R.string.settings_language),
}

internal enum class SettingsCategory(
    @StringRes val titleRes: Int,
    val presentation: SettingsCategoryPresentation,
    val destination: SettingsDestination? = null,
    val testTag: String? = null,
    val supportingText: String? = null,
) {
    Appearance(
        titleRes = R.string.settings_appearance,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Appearance,
    ),
    Emoticons(
        titleRes = R.string.settings_emoticons_winks,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Emoticons,
    ),
    MessengerPlus(
        titleRes = R.string.settings_messenger_plus,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.MessengerPlus,
    ),
    ContactInvitations(
        titleRes = R.string.settings_contact_invitations,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.ContactInvitations,
        testTag = "contact_invitation_settings",
    ),
    Transfers(
        titleRes = R.string.settings_transfers,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Transfers,
        testTag = "transfer_settings",
    ),
    Connection(
        titleRes = R.string.settings_connection,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Connection,
    ),
    Notifications(
        titleRes = R.string.settings_notifications,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Notifications,
        testTag = "notification_settings",
    ),
    InAppNotifications(
        titleRes = R.string.settings_in_app_notifications,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.InAppNotifications,
        testTag = "in_app_notification_settings",
    ),
    ConnectionInformation(
        titleRes = R.string.settings_connection_info,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.ConnectionInformation,
    ),
    Updates(
        titleRes = R.string.settings_updates,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Updates,
        testTag = "update-settings",
    ),
    SignOutOtherSessions(
        titleRes = R.string.settings_sign_out_others,
        presentation = SettingsCategoryPresentation.Toggle,
        testTag = "sign_out_other_sessions_on_sign_in",
    ),
    BackgroundConnection(
        titleRes = R.string.settings_keep_connected,
        presentation = SettingsCategoryPresentation.Toggle,
        testTag = "keep_messenger_connected",
    ),
    Language(
        titleRes = R.string.settings_language,
        presentation = SettingsCategoryPresentation.Destination,
        destination = SettingsDestination.Language,
        testTag = "language_settings",
    ),
    DeleteAllChatHistories(
        titleRes = R.string.settings_delete_histories,
        presentation = SettingsCategoryPresentation.DestructiveAction,
        testTag = "delete_all_chat_histories",
    ),
}

internal enum class SettingsCategoryPresentation {
    Destination,
    Toggle,
    DestructiveAction,
}

private val primarySettingsCategories = listOf(
    SettingsCategory.Appearance,
    SettingsCategory.Language,
    SettingsCategory.Emoticons,
    SettingsCategory.MessengerPlus,
    SettingsCategory.ContactInvitations,
    SettingsCategory.Transfers,
    SettingsCategory.Notifications,
    SettingsCategory.Connection,
    SettingsCategory.Updates,
)

private val connectionSettingsCategories = listOf(
    SettingsCategory.SignOutOtherSessions,
    SettingsCategory.BackgroundConnection,
    SettingsCategory.ConnectionInformation,
)

private val destructiveSettingsCategories = SettingsCategory.entries.filter {
    it.presentation == SettingsCategoryPresentation.DestructiveAction
}

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    onDestinationSelected: (SettingsDestination) -> Unit,
    onDeleteAllChatHistories: () -> Unit,
) {
    var showDeleteHistoryConfirmation by rememberSaveable { mutableStateOf(false) }

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        primarySettingsCategories.forEachIndexed { index, category ->
            settingsCategory(
                category = category,
                index = index,
                count = primarySettingsCategories.size,
                onClick = { onDestinationSelected(checkNotNull(category.destination)) },
            )
        }
        settingsSectionSpacer()
        destructiveSettingsCategories.forEach { category ->
            settingsCategory(
                category = category,
                onClick = { showDeleteHistoryConfirmation = true },
            )
        }
    }

    if (showDeleteHistoryConfirmation) {
        DeleteHistoryDialog(
            onDismissRequest = { showDeleteHistoryConfirmation = false },
            onConfirm = {
                showDeleteHistoryConfirmation = false
                onDeleteAllChatHistories()
            },
        )
    }
}

@Composable
fun ConnectionSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    onDestinationSelected: (SettingsDestination) -> Unit,
) {
    val context = LocalContext.current
    val account = SessionPrefs.preferenceAccount(context)
    var pictureBehavior by remember(account) { mutableStateOf(DisplayPictureSyncPrefs.behavior(context)) }
    var showPictureBehavior by rememberSaveable { mutableStateOf(false) }
    var keepsBackgroundConnection by rememberSaveable {
        mutableStateOf(BackgroundDeliveryPrefs.keepsConnectionAlive(context))
    }
    var signsOutOtherSessions by rememberSaveable {
        mutableStateOf(SessionPrefs.signsOutOtherSessionsOnSignIn(context))
    }

    fun updateBackgroundConnection(enabled: Boolean) {
        keepsBackgroundConnection = enabled
        BackgroundDeliveryPrefs.setKeepsConnectionAlive(context, enabled)
        if (enabled) {
            BackgroundConnectionController.startIfEnabled(context)
        } else {
            BackgroundConnectionController.stop(context)
        }
    }

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        connectionSettingsCategories.forEachIndexed { index, category ->
            settingsCategory(
                category = category,
                index = index,
                count = connectionSettingsCategories.size,
                toggleChecked = when (category) {
                    SettingsCategory.BackgroundConnection -> keepsBackgroundConnection
                    SettingsCategory.SignOutOtherSessions -> signsOutOtherSessions
                    else -> false
                },
                onClick = { onDestinationSelected(checkNotNull(category.destination)) },
                onToggle = { enabled ->
                    when (category) {
                        SettingsCategory.BackgroundConnection -> {
                            updateBackgroundConnection(enabled)
                        }
                        SettingsCategory.SignOutOtherSessions -> {
                            SessionPrefs.setSignOutOtherSessionsOnSignIn(context, enabled)
                            signsOutOtherSessions = enabled
                        }
                        else -> Unit
                    }
                },
            )
        }
        settingsSectionSpacer()
        item(key = "display-picture-sync") {
            SegmentedListItem(
                onClick = { showPictureBehavior = true },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                modifier = Modifier.settingsRow().testTag("display-picture-sync-setting"),
                supportingContent = { Text(pictureBehavior.label) },
            ) { Text(stringResource(R.string.settings_dp_changes)) }
        }
    }
    if (showPictureBehavior) {
        AlertDialog(
            onDismissRequest = { showPictureBehavior = false },
            title = { Text(stringResource(R.string.settings_dp_changes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.settings_dp_differs))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        DisplayPictureSyncBehavior.entries.forEachIndexed { index, behavior ->
                            SegmentedListItem(
                                onClick = {
                                    DisplayPictureSyncPrefs.setBehavior(context, behavior)
                                    pictureBehavior = behavior
                                    showPictureBehavior = false
                                    runCatching { context.appContainer.messengerRepository }.getOrNull()?.let {
                                        context.displayPictureSync.requestRefresh(it)
                                    }
                                },
                                shapes = ListItemDefaults.segmentedShapes(index, DisplayPictureSyncBehavior.entries.size),
                                modifier = Modifier.fillMaxWidth().testTag("picture-sync-${behavior.name}")
                                    .semantics { role = Role.RadioButton; selected = pictureBehavior == behavior },
                                leadingContent = { RadioButton(selected = pictureBehavior == behavior, onClick = null) },
                                supportingContent = if (behavior == DisplayPictureSyncBehavior.AskEveryTime) ({ Text(stringResource(R.string.common_default)) }) else null,
                            ) { Text(behavior.label) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPictureBehavior = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

private fun LazyListScope.settingsCategory(
    category: SettingsCategory,
    onClick: () -> Unit,
    index: Int = 0,
    count: Int = 1,
    toggleChecked: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
) {
    item(key = category.name) {
        val modifier = category.testTag?.let { tag ->
            Modifier.settingsRow().testTag(tag)
        } ?: Modifier.settingsRow()

        when (category.presentation) {
            SettingsCategoryPresentation.Destination -> SegmentedListItem(
                onClick = onClick,
                shapes = ListItemDefaults.segmentedShapes(index, count),
                modifier = modifier,
                content = { Text(stringResource(category.titleRes)) },
                trailingContent = if (category == SettingsCategory.Updates) {
                    {
                        val updateState by LocalContext.current.updateManager.state.collectAsState()
                        if (updateState.hasUpdate) UpdateBadge(Modifier.testTag("updates-settings-badge"))
                    }
                } else null,
            )
            SettingsCategoryPresentation.Toggle -> SegmentedListItem(
                checked = toggleChecked,
                onCheckedChange = onToggle,
                shapes = ListItemDefaults.segmentedShapes(index, count),
                modifier = modifier,
                trailingContent = {
                    Switch(
                        checked = toggleChecked,
                        onCheckedChange = null,
                    )
                },
                supportingContent = category.supportingText?.let { supportingText ->
                    { Text(supportingText) }
                },
                content = { Text(stringResource(category.titleRes)) },
            )
            SettingsCategoryPresentation.DestructiveAction -> FilledTonalButton(
                onClick = onClick,
                shapes = ButtonDefaults.shapes(),
                modifier = modifier,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(stringResource(category.titleRes))
            }
        }
    }
}

@Composable
fun ContactInvitationSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val settings by remember(context) { ContactInvitationPrefs.state(context) }.collectAsState()

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "use-decline-defaults") {
            ExpandableSettingsGroup(expanded = settings.useDeclineDefaults, master = { shapes ->
                SegmentedListItem(
                    checked = settings.useDeclineDefaults,
                    onCheckedChange = { ContactInvitationPrefs.setUseDeclineDefaults(context, it) },
                    shapes = shapes,
                    modifier = Modifier.settingsRow().testTag("use_contact_invitation_decline_defaults"),
                    trailingContent = {
                        Switch(checked = settings.useDeclineDefaults, onCheckedChange = null)
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_decline_summary))
                    },
                    content = { Text(stringResource(R.string.settings_decline_default)) },
                )
            }) { shapes, colors ->
                SegmentedListItem(
                    checked = settings.blockDeclinedInvitations,
                    onCheckedChange = { ContactInvitationPrefs.setBlockDeclinedInvitations(context, it) },
                    shapes = shapes,
                    colors = colors,
                    modifier = Modifier.settingsRow().testTag("block_declined_contact_invitations"),
                    trailingContent = {
                        Checkbox(
                            checked = settings.blockDeclinedInvitations,
                            onCheckedChange = null,
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.settings_decline_block)) },
                    content = { Text(stringResource(R.string.contact_request_block)) },
                )
            }
        }
    }
}

@Composable
fun MessengerPlusSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val settings by remember(context) { MessengerPlusPrefs.state(context) }.collectAsState()

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "rendering") {
            ExpandableSettingsGroup(expanded = !settings.enabled, highlighted = false, master = { shapes ->
                SegmentedListItem(
                    checked = settings.enabled,
                    onCheckedChange = { MessengerPlusPrefs.setEnabled(context, it) },
                    shapes = shapes,
                    modifier = Modifier.settingsRow().testTag("messenger_plus_enabled"),
                    trailingContent = {
                        Switch(checked = settings.enabled, onCheckedChange = null)
                    },
                    supportingContent = { Text(MESSENGER_PLUS_DISCLAIMER) },
                    content = { Text(stringResource(R.string.settings_plus_enable)) },
                )
            }) { shapes, colors ->
                SegmentedListItem(
                    checked = settings.shouldHideControlCharacters,
                    onCheckedChange = { MessengerPlusPrefs.setHideControlCharacters(context, it) },
                    shapes = shapes,
                    colors = colors,
                    modifier = Modifier.settingsRow().testTag("messenger_plus_hide_control_characters"),
                    trailingContent = {
                        Checkbox(
                            checked = settings.shouldHideControlCharacters,
                            onCheckedChange = null,
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_plus_hide_summary))
                    },
                    content = { Text(stringResource(R.string.settings_plus_hide)) },
                )
            }
        }
        item(key = "emotion-sounds-autoplay") {
            SegmentedListItem(
                checked = settings.playEmotionSounds,
                onCheckedChange = { MessengerPlusPrefs.setPlayEmotionSounds(context, it) },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                modifier = Modifier.settingsRow().testTag("emotion_sound_autoplay"),
                trailingContent = { Switch(checked = settings.playEmotionSounds, onCheckedChange = null) },
                supportingContent = { Text(stringResource(R.string.settings_emotion_summary)) },
                content = { Text(stringResource(R.string.settings_emotion_auto)) },
            )
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val savedAppearance by remember(context) { AppearancePrefs.state(context) }.collectAsState()
    val appearance = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && savedAppearance.colorMode == ColorMode.Dynamic)
        savedAppearance.copy(colorMode = ColorMode.Default) else savedAppearance
    // Resolve localized labels in the composable scope, not inside LazyListScope.
    val appearanceStyleTitle = stringResource(R.string.appearance_style)
    val colorSchemeTitle = stringResource(R.string.settings_color_scheme)
    val colorModes = ColorMode.entries.filter { it != ColorMode.Dynamic || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        settingsSectionHeader(appearanceStyleTitle, first = true)
        UiStyle.entries.forEachIndexed { index, style ->
            item(key = "style-${style.name}") {
                SegmentedListItem(
                    selected = appearance.uiStyle == style,
                    onClick = { AppearancePrefs.setUiStyle(context, style) },
                    shapes = ListItemDefaults.segmentedShapes(index, UiStyle.entries.size),
                    modifier = Modifier.settingsRow().testTag("style-${style.name}"),
                    trailingContent = {
                        RadioButton(selected = appearance.uiStyle == style, onClick = null)
                    },
                    supportingContent = {
                        Text(stringResource(
                            when (style) {
                                UiStyle.Aero -> R.string.appearance_style_aero_summary
                                UiStyle.LiquidGlass -> R.string.appearance_style_liquid_glass_summary
                                UiStyle.Standard -> R.string.appearance_style_standard_summary
                            },
                        ))
                    },
                    content = {
                        Text(stringResource(
                            when (style) {
                                UiStyle.Aero -> R.string.appearance_style_aero
                                UiStyle.LiquidGlass -> R.string.appearance_style_liquid_glass
                                UiStyle.Standard -> R.string.appearance_style_standard
                            },
                        ))
                    },
                )
            }
        }
        run {
            settingsSectionSpacer()
            settingsSectionHeader(colorSchemeTitle)
            colorModes.forEachIndexed { index, mode ->
                item(key = "color-${mode.name}") {
                    AppearanceColorOption(mode, index, colorModes.size, appearance)
                }
            }
        }
    }
}

@Composable
fun EmoticonSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val settings by remember(context) { EmoticonPrefs.state(context) }.collectAsState()
    val winkSettings by remember(context) { com.impactomsn.app.WinkPrefs.state(context) }.collectAsState()
    val itemCount = 4

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "typed-shortcuts") {
            SegmentedListItem(
                checked = settings.convertTypedShortcuts,
                onCheckedChange = { enabled ->
                    EmoticonPrefs.setConvertTypedShortcuts(context, enabled)
                },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
                modifier = Modifier
                    .settingsRow()
                    .testTag("convert_typed_shortcuts"),
                trailingContent = {
                    Switch(
                        checked = settings.convertTypedShortcuts,
                        onCheckedChange = null,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_keyboard_shortcuts_help))
                },
                content = { Text(stringResource(R.string.settings_shortcuts)) },
            )
        }
        item(key = "original-emoticons") {
            SegmentedListItem(
                checked = settings.useOriginalEmoticons,
                onCheckedChange = { enabled ->
                    EmoticonPrefs.setUseOriginalEmoticons(context, enabled)
                },
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
                modifier = Modifier
                    .settingsRow()
                    .testTag("use_original_emoticons"),
                trailingContent = {
                    Switch(
                        checked = settings.useOriginalEmoticons,
                        onCheckedChange = null,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_classic_art_summary))
                },
                content = { Text(stringResource(R.string.settings_classic_art)) },
            )
        }
        item(key = "play-winks") {
            SegmentedListItem(checked = winkSettings.playWinks,
                onCheckedChange = { com.impactomsn.app.WinkPrefs.update(context, winkSettings.copy(playWinks = it)) },
                shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount), modifier = Modifier.settingsRow(),
                trailingContent = { Switch(winkSettings.playWinks, null) },
                supportingContent = { Text(stringResource(R.string.settings_winks_summary)) },
                content = { Text(stringResource(R.string.settings_winks_auto)) })
        }
        item(key = "wink-sounds") {
            SegmentedListItem(checked = winkSettings.sounds,
                onCheckedChange = { com.impactomsn.app.WinkPrefs.update(context, winkSettings.copy(sounds = it)) },
                shapes = ListItemDefaults.segmentedShapes(index = 3, count = itemCount), modifier = Modifier.settingsRow(),
                trailingContent = { Switch(winkSettings.sounds, null) },
                supportingContent = { Text(stringResource(R.string.settings_wink_sounds_summary)) },
                content = { Text(stringResource(R.string.settings_wink_sounds)) })
        }
    }
}

@Composable
fun ConnectionInformationScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val snapshot = remember(context) { SessionPrefs.readLoginSnapshot(context) }
    val accountDetails = listOf(
        "Provider" to snapshot.provider,
        "Remembered credentials" to if (snapshot.savePassword) {
            "Encrypted on this device"
        } else {
            "Not saved"
        },
        "Notification server" to
            "${snapshot.resolvedServers.mainServer}:${snapshot.resolvedServers.mainPort}",
        "Configuration" to snapshot.resolvedServers.configServer,
    )
    val serviceEndpoints = listOf(
        "Authentication" to snapshot.resolvedServers.authServer,
        "Address book" to snapshot.resolvedServers.addressBookServer,
        "Storage" to snapshot.resolvedServers.storageServer,
        "Games & activities" to snapshot.resolvedServers.activitiesServer,
        "Mail" to snapshot.resolvedServers.mailServer,
    )

    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        settingsDetailsSection("Account and service", accountDetails, first = true)
        settingsSectionSpacer()
        settingsDetailsSection("Connected services", serviceEndpoints)
        settingsSectionSpacer()
        settingsDetailsSection(
            "Messenger Plus!",
            messengerPlusConnectionDetails(
                winkCatalogUrl = WinkCatalogActivity.CATALOG_URL,
                emotionSoundUrl = snapshot.resolvedServers.emotionSoundServer,
            ),
        )
    }
}

internal fun messengerPlusConnectionDetails(
    winkCatalogUrl: String?,
    emotionSoundUrl: String?,
): List<Pair<String, String>> = listOf(
    "Winks" to winkCatalogUrl,
    "Emotion sounds" to emotionSoundUrl,
).map { (label, url) -> label to (url?.takeIf { it.isNotBlank() } ?: "Not configured") }

@Composable
fun AboutSettingsScreen(
    innerPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    onLicenses: () -> Unit = {},
    onUpdates: () -> Unit = {},
) {
    PageContentList(
        innerPadding = innerPadding,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "about-updates") {
            SegmentedListItem(
                onClick = onUpdates,
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                modifier = Modifier.settingsRow().testTag("update-settings"),
                content = { Text(stringResource(R.string.settings_updates)) },
                supportingContent = { Text(stringResource(R.string.about_updates_summary)) },
                trailingContent = {
                    val updateState by LocalContext.current.updateManager.state.collectAsState()
                    if (updateState.hasUpdate) UpdateBadge(Modifier.testTag("updates-settings-badge"))
                },
            )
        }
        item(key = "about") {
            SettingsInformationCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsValue(stringResource(R.string.about_developer), "ImpactoMSN")
                Text(stringResource(R.string.about_license_note))
                TextButton(onClick = onLicenses) { Text(stringResource(R.string.settings_licenses)) }
                Text(
                    stringResource(R.string.about_thanks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsInformationCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

private fun LazyListScope.settingsDetailsSection(
    title: String,
    details: List<Pair<String, String>>,
    first: Boolean = false,
) {
    settingsSectionHeader(title, first)
    details.forEachIndexed { index, (label, value) ->
        item(key = "$title-$label") {
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index, details.size),
                modifier = Modifier.fillMaxWidth(),
                content = { SettingsValue(label, value) },
            )
        }
    }
}

@Composable
private fun SettingsValue(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DeleteHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_delete_histories_title)) },
        text = {
            Text(stringResource(R.string.settings_delete_history_warning))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.settings_delete_all))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

internal fun LazyListScope.settingsSectionHeader(title: String, first: Boolean = false) {
    item(key = "header-$title") {
        Text(
            text = title,
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (first) 2.dp else 8.dp,
                    bottom = 8.dp,
                )
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun LazyListScope.settingsSectionSpacer() {
    item { Spacer(Modifier.height(10.dp)) }
}

internal fun Modifier.settingsRow(): Modifier =
    fillMaxWidth()
