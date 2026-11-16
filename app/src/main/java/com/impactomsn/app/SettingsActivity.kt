package com.impactomsn.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import com.impactomsn.app.pages.AppearanceSettingsScreen
import com.impactomsn.app.pages.ConnectionInformationScreen
import com.impactomsn.app.pages.ConnectionSettingsScreen
import com.impactomsn.app.pages.ContactInvitationSettingsScreen
import com.impactomsn.app.pages.CustomizedNotificationsScreen
import com.impactomsn.app.pages.EmoticonSettingsScreen
import com.impactomsn.app.pages.InAppNotificationSettingsMenu
import com.impactomsn.app.pages.InAppNotificationSettingsScreen
import com.impactomsn.app.pages.MessengerPlusSettingsScreen
import com.impactomsn.app.pages.NotificationSettingsScreen
import com.impactomsn.app.pages.SettingsDestination
import com.impactomsn.app.pages.SettingsScreen
import com.impactomsn.app.pages.UpdateSettingsScreen
import com.impactomsn.app.pages.components.SecondaryPageScaffold
import com.impactomsn.app.ui.theme.ImpactoMsnTheme

class SettingsActivity : ImpactoMsnActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectToLoginIfNeeded()) {
            return
        }
        setContent {
            ImpactoMsnTheme {
                SettingsRoute(
                    onBack = ::finish,
                    onDeleteAllChatHistories = {
                        lifecycleScope.launch {
                            appContainer.messengerRepository.clearConversationHistory()
                        }
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        redirectToLoginIfNeeded()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsRoute(
    onBack: () -> Unit,
    onDeleteAllChatHistories: () -> Unit,
) {
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<SettingsDestination>, SettingsDestination>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf(SettingsDestination.Root) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val navigateTo: (SettingsDestination) -> Unit = { destination ->
        if (backStack.last() != destination) backStack.add(destination)
    }
    val pop: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    val transition: AnimatedContentTransitionScope<*>.(
        AnimatedContentTransitionScope.SlideDirection,
    ) -> ContentTransform = { direction ->
        (
            slideIntoContainer(
                towards = direction,
                animationSpec = spatialSpec,
                initialOffset = { it / 4 },
            ) + fadeIn(animationSpec = effectsSpec)
        ).togetherWith(
            slideOutOfContainer(
                towards = direction,
                animationSpec = spatialSpec,
                targetOffset = { it / 4 },
            ) + fadeOut(animationSpec = effectsSpec),
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = pop,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            transition(AnimatedContentTransitionScope.SlideDirection.Start)
        },
        popTransitionSpec = {
            transition(AnimatedContentTransitionScope.SlideDirection.End)
        },
        entryProvider = { key ->
            NavEntry(key) { destination ->
                SecondaryPageScaffold(
                    title = androidx.compose.ui.res.stringResource(destination.titleRes),
                    onBack = {
                        if (destination == SettingsDestination.Root) {
                            onBack()
                        } else {
                            pop()
                        }
                    },
                    actions = {
                        if (
                            destination == SettingsDestination.Notifications ||
                            destination == SettingsDestination.InAppNotifications
                        ) {
                            InAppNotificationSettingsMenu(
                                onCustomizedNotifications = { navigateTo(SettingsDestination.CustomizedNotifications) },
                            )
                        }
                    },
                ) { innerPadding, scrollBehavior ->
                    when (destination) {
                        SettingsDestination.Root -> SettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                            onDestinationSelected = navigateTo,
                            onDeleteAllChatHistories = onDeleteAllChatHistories,
                        )
                        SettingsDestination.Appearance -> AppearanceSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.Notifications -> NotificationSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                            onInAppNotifications = { navigateTo(SettingsDestination.InAppNotifications) },
                        )
                        SettingsDestination.InAppNotifications -> InAppNotificationSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.CustomizedNotifications -> CustomizedNotificationsScreen(innerPadding, scrollBehavior)
                        SettingsDestination.MessengerPlus -> MessengerPlusSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.Emoticons -> EmoticonSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.ContactInvitations -> ContactInvitationSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.Transfers -> com.impactomsn.app.pages.TransferSettingsScreen(innerPadding, scrollBehavior)
                        SettingsDestination.Connection -> ConnectionSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                            onDestinationSelected = navigateTo,
                        )
                        SettingsDestination.ConnectionInformation -> ConnectionInformationScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.Licenses -> com.impactomsn.app.pages.OpenSourceLicensesScreen(innerPadding)
                        SettingsDestination.Language -> com.impactomsn.app.pages.LanguageSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                        SettingsDestination.Updates -> UpdateSettingsScreen(
                            innerPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        },
    )
}
