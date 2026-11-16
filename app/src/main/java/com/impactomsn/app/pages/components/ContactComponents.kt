@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.pages.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.impactomsn.app.EmoticonPrefs
import com.impactomsn.app.LocalMessengerScene
import com.impactomsn.app.MessengerPlusText
import com.impactomsn.app.MsnEmoticonText
import com.impactomsn.app.R
import com.impactomsn.app.SceneBackdrop
import com.impactomsn.app.UserProfileSnapshot
import com.impactomsn.app.avatarInitial
import com.impactomsn.app.data.Contact
import com.impactomsn.app.enums.Presence
import com.impactomsn.app.messengerPlusPlainText
import com.impactomsn.app.rememberAvatarImage
import com.impactomsn.app.sceneContentColor
import com.impactomsn.app.ui.theme.avatar
import com.impactomsn.app.ui.theme.ownDisplayPicture
import com.impactomsn.app.ui.theme.pill

@Composable
fun LoggedInUserProfileCard(
    profile: UserProfileSnapshot,
    onPresenceSelected: (Presence) -> Unit,
    onEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    val emoticonSettings by remember(context) { EmoticonPrefs.state(context) }.collectAsState()
    var presenceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val groupInteractionSource = remember { MutableInteractionSource() }
    val scene = LocalMessengerScene.current
    val sceneInk = sceneContentColor()

    ElevatedCard(
        onClick = onEditProfile,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SceneBackdrop(scene, Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    name = profile.username.messengerPlusPlainText(),
                    avatarUri = profile.avatarUri,
                    presence = profile.presence,
                    showPresenceIndicator = false,
                    shape = MaterialTheme.shapes.ownDisplayPicture,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MessengerPlusText(
                            text = profile.username,
                            sceneGlow = scene != null,
                            color = if (scene != null) sceneInk else androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_edit_accessibility),
                            tint = if (scene != null) sceneInk else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MsnEmoticonText(
                        text = profile.statusMessage.ifBlank { "No personal message" },
                        useOriginalEmoticons = emoticonSettings.useOriginalEmoticons,
                        sceneGlow = scene != null,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (scene != null) sceneInk else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box {
                        FilledTonalButton(
                            onClick = { presenceMenuExpanded = true },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PresenceDot(profile.presence, modifier = Modifier.size(9.dp))
                                Text(profile.presence.ownStatusLabel(), style = MaterialTheme.typography.labelLarge)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.shortcut_change_status_short),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        DropdownMenuPopup(
                            expanded = presenceMenuExpanded,
                            onDismissRequest = { presenceMenuExpanded = false },
                        ) {
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(0, 1),
                                interactionSource = groupInteractionSource,
                            ) {
                                Presence.entries.forEachIndexed { index, presence ->
                                    DropdownMenuItem(
                                        text = { Text(presence.ownStatusLabel()) },
                                        onClick = {
                                            presenceMenuExpanded = false
                                            onPresenceSelected(presence)
                                        },
                                        shape = MenuDefaults.itemShape(index, Presence.entries.size).shape
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    index: Int,
    count: Int,
    contact: Contact,
    onClick: (() -> Unit)?,
    contextActionLabel: String? = null,
    onContextAction: () -> Unit = {},
    onReadStateChange: ((Boolean) -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    hasDraft: Boolean = false,
    supportingText: String? = null,
    onDeleteContact: (() -> Unit)? = null,
    accessLevel: String? = null,
    profileActionLabel: String? = null,
    isOwnContact: Boolean = false,
    isPendingMember: Boolean = false,
) {
    val context = LocalContext.current
    val emoticonSettings by remember(context) { EmoticonPrefs.state(context) }.collectAsState()
    var contextMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val groupInteractionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        ContactReadAction(
            contactId = contact.id,
            unread = contact.isUnread,
            onReadStateChange = onReadStateChange,
            onClick = onClick ?: {},
        ) { rowModifier, onRowClick ->
            val rowContent: @Composable () -> Unit = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Avatar(
                        name = contact.displayName.messengerPlusPlainText(),
                        avatarUri = contact.avatarUri,
                        presence = contact.presence,
                        showPresenceIndicator = !isPendingMember,
                        modifier = if (onOpenProfile != null) {
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 28.dp),
                                    role = Role.Button,
                                    onClickLabel = profileActionLabel ?: if (contact.isGroup) "View group members" else "View profile",
                                    onClick = onOpenProfile,
                                )
                                .testTag("contact-profile-${contact.id}")
                                .semantics {
                                    contentDescription = profileActionLabel ?: if (contact.isGroup) {
                                        "View " + contact.displayName.messengerPlusPlainText() + " group members"
                                    } else {
                                        "View " + contact.displayName.messengerPlusPlainText() + "'s profile"
                                    }
                                }
                        } else Modifier,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MsnEmoticonText(
                                text = contact.displayName,
                                modifier = Modifier.weight(1f, fill = false),
                                useOriginalEmoticons = emoticonSettings.useOriginalEmoticons,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isOwnContact) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) { Text(stringResource(R.string.notification_self_name)) }
                            }
                            if (isPendingMember) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ) { Text(stringResource(R.string.common_pending)) }
                            }
                        }
                        if (hasDraft) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = stringResource(R.string.contact_draft_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            MsnEmoticonText(
                                text = supportingText ?: if (contact.isGroup) "${contact.joinedMembers.size} ${if (contact.joinedMembers.size == 1) "member" else "members"}" else contact.statusMessage.ifBlank { contact.email },
                                useOriginalEmoticons = emoticonSettings.useOriginalEmoticons,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            accessLevel?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium)
                            }
                            if (!isPendingMember) Text(
                                text = if (contact.isGroup) stringResource(R.string.common_group) else contact.presence.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (contact.unreadCount > 0) {
                            Badge { Text(contact.unreadCount.coerceAtMost(99).toString()) }
                        } else if (contact.isMarkedUnread) {
                            val unreadDescription = stringResource(R.string.common_unread)
                            Badge(modifier = Modifier.semantics { contentDescription = unreadDescription }) {
                                Text("")
                            }
                        }
                    }
                }
            }
            val shapes = ListItemDefaults.segmentedShapes(index, count)
            val modifier = rowModifier.fillMaxWidth().testTag("contact-row-${contact.id}")
            if (onClick == null) {
                SegmentedListItem(shapes = shapes, modifier = modifier, content = rowContent)
            } else {
                SegmentedListItem(
                    onClick = onRowClick,
                    onLongClick = if (contextActionLabel != null || onReadStateChange != null || onDeleteContact != null) {
                        { contextMenuExpanded = true }
                    } else null,
                    onLongClickLabel = "Contact actions",
                    shapes = shapes,
                    modifier = modifier,
                    content = rowContent,
                )
            }
        }
        DropdownMenuPopup(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, if (onDeleteContact != null) 2 else 1),
                interactionSource = groupInteractionSource,
            ) {
                onReadStateChange?.let { changeReadState ->
                    DropdownMenuItem(
                        text = { Text(stringResource(if (contact.isUnread) R.string.common_mark_read else R.string.common_mark_unread)) },
                        onClick = {
                            contextMenuExpanded = false
                            changeReadState(!contact.isUnread)
                        },
                        shape = MenuDefaults.leadingItemShape
                    )
                }
                contextActionLabel?.let { label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            contextMenuExpanded = false
                            onContextAction()
                        },
                        shape = if (onDeleteContact != null) MenuDefaults.middleItemShape else MenuDefaults.trailingItemShape
                    )
                }
            }
            onDeleteContact?.let { delete ->
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(1, 2),
                    interactionSource = groupInteractionSource,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contact_delete_action), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            contextMenuExpanded = false
                            delete()
                        },
                        shape = MenuDefaults.trailingItemShape
                    )
                }
            }

        }
    }
}

@Composable
fun Avatar(
    name: String,
    avatarUri: String?,
    presence: Presence,
    modifier: Modifier = Modifier,
    showPresenceIndicator: Boolean = true,
    imageVersion: Int = 0,
    highResolution: Boolean = false,
    shape: Shape = MaterialTheme.shapes.avatar,
) {
    val painter = rememberAvatarImage(avatarUri, highResolution, imageVersion).painter

    Box(modifier = modifier.size(56.dp)) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = stringResource(R.string.picture_named_display, name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = avatarInitial(name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        if (showPresenceIndicator) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp),
                shape = MaterialTheme.shapes.pill,
                color = MaterialTheme.colorScheme.surface,
            ) {
                PresenceDot(
                    presence = presence,
                    modifier = Modifier.padding(3.dp).fillMaxSize(),
                )
            }
        }
    }
}
