package com.impactomsn.app

import android.app.Application
import android.content.Context
import com.impactomsn.app.data.ConversationDraftStore
import com.impactomsn.app.data.ConversationHistoryStore
import com.impactomsn.app.data.ConversationStyleStore
import com.impactomsn.app.data.FileContactDisplayPictureCache
import com.impactomsn.app.data.FileConversationHistoryStore
import com.impactomsn.app.data.InMemoryConversationHistoryStore
import com.impactomsn.app.data.LiveMsnp18Client
import com.impactomsn.app.data.MessengerRepository
import com.impactomsn.app.data.MockMsnp18Client
import com.impactomsn.app.data.MsnpClient
import com.impactomsn.app.data.persistentConversationDraftStore
import com.impactomsn.app.data.persistentConversationStyleStore
import com.impactomsn.app.notifications.AppVisibilityTracker
import com.impactomsn.app.notifications.BackgroundConnectionController
import com.impactomsn.app.notifications.InAppNotificationController
import com.impactomsn.app.notifications.ImpactoMsnNotificationManager
import com.impactomsn.app.notifications.NotificationChannels
import com.impactomsn.app.notifications.RealtimeNotificationBridge
import java.io.File

class ImpactoMsnApplication : Application() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePrefs.wrap(newBase))
    }
    internal val receivedTransferStorage by lazy { ReceivedTransferStorage(this) }
    private val transferAutomation by lazy {
        TransferAutomation(
            settings = { account -> TransferPrefs.read(this, account) },
            saveFile = { account, transfer -> receivedTransferStorage.saveFile(account, transfer) },
            savePhoto = { account, share, photo -> receivedTransferStorage.savePhoto(account, share, photo) },
        )
    }
    internal val emotionSoundStore by lazy { com.impactomsn.app.data.EmotionSoundStore(File(noBackupFilesDir, "emotion_sounds")) }
    internal val winkStore by lazy {
        com.impactomsn.app.data.WinkStore(File(noBackupFilesDir, "winks")) {
            val catalog = org.json.JSONArray(assets.open("winks/catalog.json").bufferedReader().use { it.readText() })
            (0 until catalog.length()).map { index ->
                val item = catalog.getJSONObject(index)
                val bytes = assets.open("winks/" + item.getString("file")).use { it.readBytes() }
                val metadata = com.impactomsn.app.protocol.MsnDisplayPicture.create("impactomsn", bytes, 8)
                    .copy(extraAttributes = mapOf("stamp" to item.getString("stamp")))
                com.impactomsn.app.protocol.WinkReference(metadata, item.getString("name")) to bytes
            }
        }
    }
    internal val displayPictureSync by lazy { DisplayPictureSync(this) }
    val updateManager by lazy { com.impactomsn.app.updates.UpdateManager(this) }
    internal val visibilityTracker: AppVisibilityTracker = AppVisibilityTracker { isForeground ->
        if (isForeground) notificationManager.dismissContactSignedInNotifications()
        else inAppNotifications.dismiss()
        runCatching { container.messengerRepository }.getOrNull()?.let { repository ->
            repository.setAppInForeground(isForeground)
            displayPictureSync.setForeground(isForeground)
            if (isForeground && SessionPrefs.canOpenAuthenticatedScreens(this)) displayPictureSync.requestRefresh(repository)
        }
    }
    internal val notificationManager by lazy { ImpactoMsnNotificationManager(this) { visibilityTracker.isForeground } }
    internal val inAppNotifications by lazy { InAppNotificationController(this, visibilityTracker) }
    private val realtimeNotificationBridge by lazy {
        RealtimeNotificationBridge(visibilityTracker, notificationManager, inAppNotifications = inAppNotifications)
    }
    private val networkMonitor by lazy { NetworkMonitor(this) }

    val container: AppContainer by lazy {
        AppContainer(
            clientFactory = { useMockServer ->
                if (useMockServer) {
                    MockMsnp18Client()
                } else {
                    LiveMsnp18Client(
                        winkStore = winkStore,
                        onCustomEmoticon = { metadata, bytes ->
                            CustomEmoticonCatalog.add(this, metadata, bytes)
                        },
                        localCustomEmoticon = { metadata ->
                            CustomEmoticonCatalog.localObject(this, metadata)
                        },
                        customEmoticonAnnouncements = {
                            CustomEmoticonCatalog.announcements(this, it)
                        },
                        fileTransferDirectory = File(noBackupFilesDir, "file_transfers"),
                        endpointName = { deviceEndpointName(this) },
                        endpointId = { deviceEndpointId(this) },
                        contactDisplayPictureCache = FileContactDisplayPictureCache(
                            File(filesDir, "contact_display_pictures"),
                        ),
                    )
                }
            },
            onRepositoryCreated = { repository ->
                transferAutomation.attach(repository)
                realtimeNotificationBridge.attach(repository)
                displayPictureSync.attach(repository)
                displayPictureSync.setForeground(visibilityTracker.isForeground)
                repository.setAppInForeground(visibilityTracker.isForeground)
            },
            awaitNetworkAccess = networkMonitor::awaitInternetAccess,
            conversationHistoryStore = FileConversationHistoryStore(
                directory = File(noBackupFilesDir, "conversation_history"),
                legacyFile = File(noBackupFilesDir, "conversation_history.bin"),
            ),
            messengerPlusEnabled = { MessengerPlusPrefs.isEnabled(this) },
            conversationDraftStore = persistentConversationDraftStore(this),
            conversationStyleStore = persistentConversationStyleStore(this),
            onConversationRead = { contactId ->
                notificationManager.dismissConversation(contactId)
                inAppNotifications.dismissConversation(contactId)
            },
            onRemoteSignOut = {
                // A remote sign-out must survive process restart and background service restore.
                SessionPrefs.recordRemoteSignOut(this)
                SessionPrefs.logout()
                BackgroundConnectionController.stop(this)
                AppShortcuts.clearRecentChats(this)
            },
            signOutOtherSessionsOnSignIn = { account ->
                SessionPrefs.signsOutOtherSessionsOnSignIn(this, account)
            },
        )
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(visibilityTracker)
        NotificationChannels.createAll(this)
        // App updates disabled: MSN Messenger 8.5 no longer checks for or offers updates.
        AppShortcuts.refresh(this)
    }
}

class AppContainer(
    private val clientFactory: (useMockServer: Boolean) -> MsnpClient = { useMockServer ->
        if (useMockServer) MockMsnp18Client() else LiveMsnp18Client()
    },
    private val onRepositoryCreated: (MessengerRepository) -> Unit = {},
    private val awaitNetworkAccess: suspend () -> Unit = {},
    private val conversationHistoryStore: ConversationHistoryStore =
        InMemoryConversationHistoryStore(),
    private val messengerPlusEnabled: () -> Boolean = { true },
    private val conversationDraftStore: ConversationDraftStore = ConversationDraftStore(),
    private val onConversationRead: (String) -> Unit = {},
    private val onRemoteSignOut: () -> Unit = {},
    private val signOutOtherSessionsOnSignIn: (AccountIdentity) -> Boolean = { false },
    private val conversationStyleStore: ConversationStyleStore = ConversationStyleStore(),
) {
    private var repository: MessengerRepository? = null
    private var repositoryUsesMockServer: Boolean? = null

    @Synchronized
    fun repositoryFor(useMockServer: Boolean): MessengerRepository {
        val current = repository
        if (current != null && repositoryUsesMockServer == useMockServer) return current

        current?.logout()
        return MessengerRepository(
            client = clientFactory(useMockServer),
            awaitNetworkAccess = awaitNetworkAccess,
            historyStore = conversationHistoryStore,
            draftStore = conversationDraftStore,
            styleStore = conversationStyleStore,
            messengerPlusEnabled = messengerPlusEnabled,
            onConversationRead = onConversationRead,
            useMockServer = useMockServer,
            onRemoteSignOut = onRemoteSignOut,
            signOutOtherSessionsOnSignIn = signOutOtherSessionsOnSignIn,
        ).also {
            repository = it
            repositoryUsesMockServer = useMockServer
            onRepositoryCreated(it)
        }
    }

    internal val currentRepository: MessengerRepository?
        @Synchronized get() = repository

    val messengerRepository: MessengerRepository
        @Synchronized get() = checkNotNull(repository) {
            "The MSNP backend must be selected by a successful login first."
        }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ImpactoMsnApplication).container
