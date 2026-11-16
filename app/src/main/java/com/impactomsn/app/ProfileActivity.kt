package com.impactomsn.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.impactomsn.app.pages.ProfilePage
import com.impactomsn.app.pages.components.SecondaryPageScaffold
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import androidx.compose.ui.res.stringResource

class ProfileActivity : ImpactoMsnActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectToLoginIfNeeded()) {
            return
        }
        setContent {
            ImpactoMsnTheme {
                ProfileRoute(
                    onBack = ::finish,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        redirectToLoginIfNeeded()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, ProfileActivity::class.java)
    }
}

@Composable
private fun ProfileRoute(
    onBack: () -> Unit,
) {
    SecondaryPageScaffold(
        title = stringResource(R.string.profile_edit),
        onBack = onBack,
    ) { innerPadding, scrollBehavior ->
        ProfilePage(
            innerPadding = innerPadding,
            scrollBehavior = scrollBehavior,
            onProfileSaved = onBack,
        )
    }
}
