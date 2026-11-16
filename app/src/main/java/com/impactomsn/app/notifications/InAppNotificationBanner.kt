@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.notifications

import com.impactomsn.app.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.impactomsn.app.ChatActivity
import com.impactomsn.app.ImpactoMsnApplication
import com.impactomsn.app.RequestsActivity
import com.impactomsn.app.SessionPrefs
import com.impactomsn.app.pages.components.Avatar
import androidx.compose.ui.res.stringResource

@Composable
internal fun InAppNotificationHost() {
    val context = LocalContext.current
    val application = context.applicationContext as? ImpactoMsnApplication ?: return
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val controller = application.inAppNotifications
    val banner by controller.banner.collectAsState()
    val canShow = lifecycle == Lifecycle.State.RESUMED && SessionPrefs.canOpenAuthenticatedScreens(context)
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = banner.takeIf { canShow },
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
        contentKey = { it?.id },
        transitionSpec = {
            val enter = if (initialState == null) {
                slideInVertically(spatialSpec) { -it } + fadeIn(effectsSpec)
            } else fadeIn(effectsSpec)
            val exit = if (targetState == null) {
                slideOutVertically(spatialSpec) { -it } + fadeOut(effectsSpec)
            } else fadeOut(effectsSpec)
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "In-app notification banner",
    ) { current ->
        if (current != null) InAppNotificationBanner(
            notification = current,
            enabled = canShow && banner?.id == current.id,
            onDismiss = { controller.dismiss(current.id) },
            onOpen = {
                controller.dismiss(current.id)
                val intent = when (current.type) {
                    InAppNotificationType.ContactRequest, InAppNotificationType.GroupInvitation -> RequestsActivity.createIntent(context)
                    else -> current.contactId?.let { ChatActivity.createIntent(context, it) }
                }
                intent?.let { context.startActivity(it) }
            },
        )
    }
}

@Composable
internal fun InAppNotificationBanner(
    notification: InAppNotification,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onOpen,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("in_app_notification_banner").semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        // MSN Messenger banners use the pale blue container with deep blue ink.
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 8.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (notification.contactId != null) {
                Avatar(name = notification.title, avatarUri = notification.avatarUri,
                    presence = com.impactomsn.app.enums.Presence.Offline,
                    modifier = Modifier.size(40.dp), showPresenceIndicator = false)
            } else Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(notification.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(notification.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.testTag("dismiss_in_app_notification")) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.notification_dismiss))
            }
        }
    }
}
