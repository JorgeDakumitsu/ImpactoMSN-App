package com.impactomsn.app

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.impactomsn.app.data.Contact
import com.impactomsn.app.data.MessengerRepository
import com.impactomsn.app.data.MockMsnp18Client
import com.impactomsn.app.enums.Presence
import com.impactomsn.app.pages.BlockedContactsPage
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BlockedContactsRefreshInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarRefreshDisablesWhileLoadingAndOffersRetry() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val repository = MessengerRepository(MockMsnp18Client(signInDelayMillis = 0), scope)
        val refreshing = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        var refreshes = 0
        try {
            compose.setContent {
                ImpactoMsnTheme {
                    BlockedContactsRoute(repository, {}, refreshing.value, error.value, { refreshes++ })
                }
            }
            compose.onNodeWithTag("blocked-contacts-refresh").assertIsEnabled().performClick()
            assertEquals(1, refreshes)
            compose.runOnIdle { refreshing.value = true }
            compose.onNodeWithTag("blocked-contacts-refresh").assertIsNotEnabled()
            compose.onNodeWithTag("blocked-contacts-refresh-indicator").assertIsDisplayed()
            compose.runOnIdle { refreshing.value = false; error.value = "Refresh failed" }
            compose.onNodeWithTag("blocked-contacts-refresh-error").assertIsDisplayed()
            compose.onNodeWithTag("blocked-contacts-retry").performClick()
            assertEquals(2, refreshes)
        } finally { scope.cancel() }
    }

    @Test fun failedRefreshKeepsBlockedRowsAndASuccessfulEmptyListClearsThem() {
        val peer = Contact("blocked@example.test", "Blocked friend", "blocked@example.test", Presence.Offline,
            isBlocked = true, isForwardListContact = false)
        val contacts = mutableStateOf(listOf(peer))
        val error = mutableStateOf<String?>("Refresh failed")
        compose.setContent {
            ImpactoMsnTheme {
                BlockedContactsPage(PaddingValues(), TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                    contacts.value, {}, refreshError = error.value)
            }
        }
        compose.onNodeWithText("Blocked friend").assertIsDisplayed()
        compose.onNodeWithTag("blocked-contacts-refresh-error").assertIsDisplayed()
        compose.runOnIdle { contacts.value = emptyList(); error.value = null }
        compose.onNodeWithText("Blocked friend").assertDoesNotExist()
        compose.onNodeWithText("No blocked contacts").assertIsDisplayed()
    }
}
