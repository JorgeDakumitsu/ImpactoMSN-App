package com.impactomsn.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.impactomsn.app.pages.UpdateSettingsScreen
import com.impactomsn.app.pages.components.SecondaryPageScaffold
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import com.impactomsn.app.updates.updateManager

/** Updates remain accessible from a notification even when the user is signed out. */
class UpdateActivity : ImpactoMsnActivity() {
    override val observesConnectionLoss = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateManager.dismissNotification()
        setContent {
            ImpactoMsnTheme {
                SecondaryPageScaffold(title = getString(R.string.updates_title), onBack = ::finish) { padding, behavior ->
                    UpdateSettingsScreen(padding, behavior)
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, UpdateActivity::class.java)
    }
}
