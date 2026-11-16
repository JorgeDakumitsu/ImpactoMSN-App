package com.impactomsn.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.impactomsn.app.ProviderOptions
import com.impactomsn.app.ServerOverrides
import com.impactomsn.app.ServerPresets
import com.impactomsn.app.data.Contact
import com.impactomsn.app.data.ContactRequest
import com.impactomsn.app.data.MessengerEvent
import com.impactomsn.app.data.MessengerRepository
import com.impactomsn.app.data.MockMsnp18Client
import com.impactomsn.app.data.MsnpClient
import com.impactomsn.app.data.SignInRequest
import com.impactomsn.app.enums.Presence
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactRequestNotificationInstrumentedTest {
    @Test fun pendingAndDeclinedPeopleDoNotTriggerContactSignedInNotifications() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        val peer = "peer@example.test"
        val contact = Contact(peer, "peer", peer, Presence.Online, isForwardListContact = false)
        val contacts = MutableStateFlow(listOf(contact))
        val requests = MutableStateFlow(listOf(ContactRequest(peer, "Pending/mock-listener")))
        val events = MutableSharedFlow<MessengerEvent>()
        val mock = MockMsnp18Client(signInDelayMillis = 0)
        val client = object : MsnpClient by mock {
            override val contacts = contacts
            override val contactRequests = requests
            override val events = events
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = MessengerRepository(client, scope, useMockServer = true)
        val bridge = RealtimeNotificationBridge(AppVisibilityTracker(), ImpactoMsnNotificationManager(context), scope)
        val signedInId = ImpactoMsnNotificationManager.notificationId(NotificationEventType.ContactSignedIn, peer)
        try {
            bridge.attach(repository)
            repository.signIn(SignInRequest("owner@example.test", "mock-only",
                ServerPresets.resolveForProvider(ProviderOptions.ESCARGOT, ServerOverrides()))).getOrThrow()
            withTimeout(3000) { events.subscriptionCount.first { it >= 2 } }
            withTimeout(3000) {
                while (manager.activeNotifications.none { it.id == ImpactoMsnNotificationManager.CONTACT_REQUESTS_NOTIFICATION_ID }) delay(50)
            }
            events.emit(MessengerEvent.PresenceChanged(peer, Presence.Online, Presence.Offline))
            delay(200)
            assertTrue(manager.activeNotifications.any { it.id == ImpactoMsnNotificationManager.CONTACT_REQUESTS_NOTIFICATION_ID })
            assertTrue(manager.activeNotifications.none { it.id == signedInId })
            requests.value = emptyList() // Declining leaves the person outside the forward list.
            events.emit(MessengerEvent.PresenceChanged(peer, Presence.Online, Presence.Offline))
            delay(200)
            assertTrue(manager.activeNotifications.none { it.id == signedInId })
            contacts.value = listOf(contact.copy(isForwardListContact = true))
            events.emit(MessengerEvent.PresenceChanged(peer, Presence.Online, Presence.Offline))
            withTimeout(3000) { while (manager.activeNotifications.none { it.id == signedInId }) delay(50) }
        } finally { repository.logout(); scope.cancel(); manager.cancelAll() }
    }
}
