package com.impactomsn.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.impactomsn.app.pages.ContactProfilePage
import com.impactomsn.app.pages.components.ActionSnackbarHost
import com.impactomsn.app.pages.components.AddContactDialog
import com.impactomsn.app.pages.components.SecondaryPageScaffold
import com.impactomsn.app.pages.components.showActionSnackbar
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import androidx.compose.ui.res.stringResource

class ContactProfileActivity : ImpactoMsnActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectToLoginIfNeeded()) return
        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
        if (contactId.isNullOrBlank()) {
            finish()
            return
        }
        val repository = appContainer.messengerRepository
        val contactFlow = repository.contact(contactId)
        setContent {
            LaunchedEffect(contactId) { repository.refreshContactPicture(contactId) }
            val contact by contactFlow.collectAsState(initial = repository.contactSnapshot(contactId))
            val contacts by repository.contacts.collectAsState()
            val profile by repository.profile.collectAsState()
            var addContactEmail by rememberSaveable { mutableStateOf<String?>(null) }
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            ImpactoMsnTheme {
                SecondaryPageScaffold(
                    title = stringResource(R.string.profile_contact),
                    onBack = ::finish,
                    snackbarHost = { ActionSnackbarHost(snackbarHostState) },
                ) { padding, scroll ->
                    ContactProfilePage(
                        contact = contact,
                        innerPadding = padding,
                        scrollBehavior = scroll,
                        onBlockContact = { repository.blockContact(it.id) },
                        onEraseHistory = { repository.clearConversationHistory(it.id) },
                        onDeleteContact = { target, block -> repository.deleteContact(target.id, block) },
                        onContactDeleted = ::finish,
                        onAddContact = { addContactEmail = it.email },
                    )
                }
                addContactEmail?.let { email ->
                    AddContactDialog(
                        ownEmail = profile.email,
                        contacts = contacts,
                        initialEmail = email,
                        isMock = SessionPrefs.isMockServerActive(),
                        onAddContact = repository::addContact,
                        onAdded = {
                            addContactEmail = null
                            scope.launch {
                                snackbarHostState.showActionSnackbar(getString(
                                    if (SessionPrefs.isMockServerActive()) R.string.contact_add_mock_success
                                    else R.string.contact_add_success,
                                ))
                            }
                        },
                        onDismiss = { addContactEmail = null },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        redirectToLoginIfNeeded()
    }

    companion object {
        private const val EXTRA_CONTACT_ID = "contact_id"

        fun createIntent(context: Context, contactId: String): Intent =
            Intent(context, ContactProfileActivity::class.java).putExtra(EXTRA_CONTACT_ID, contactId)
    }
}
