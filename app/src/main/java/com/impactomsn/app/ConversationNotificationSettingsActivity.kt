package com.impactomsn.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.impactomsn.app.pages.ConversationNotificationSettingsScreen
import com.impactomsn.app.pages.components.SecondaryPageScaffold
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import androidx.compose.ui.res.stringResource

class ConversationNotificationSettingsActivity : ImpactoMsnActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectToLoginIfNeeded()) return
        val id = intent.getStringExtra("contact_id")?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        val name = intent.getStringExtra("display_name") ?: id
        val group = intent.getBooleanExtra("is_group", false)
        setContent {
            ImpactoMsnTheme {
                SecondaryPageScaffold(title = stringResource(R.string.group_custom_notifications), onBack = ::finish) { padding, scroll ->
                    ConversationNotificationSettingsScreen(id, name, group, padding, scroll)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        redirectToLoginIfNeeded()
    }

    companion object {
        fun createIntent(context: Context, contactId: String, displayName: String, isGroup: Boolean): Intent =
            Intent(context, ConversationNotificationSettingsActivity::class.java).putExtra("contact_id", contactId)
                .putExtra("display_name", displayName).putExtra("is_group", isGroup)
    }
}
