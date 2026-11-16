package com.impactomsn.app.notifications

import com.impactomsn.app.LoginSnapshot
import com.impactomsn.app.ProviderOptions
import com.impactomsn.app.ServerOverrides
import com.impactomsn.app.ServerPresets
import com.impactomsn.app.enums.Presence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundConnectionPolicyTest {
    @Test
    fun restoreRequiresExplicitAutomaticSignInAndRememberedCredentials() {
        val eligible = snapshot()

        assertTrue(canRestoreBackgroundConnection(enabled = true, snapshot = eligible))
        assertFalse(
            canRestoreBackgroundConnection(
                enabled = true,
                snapshot = eligible.copy(automaticSignIn = false),
            ),
        )
        assertFalse(
            canRestoreBackgroundConnection(
                enabled = true,
                snapshot = eligible.copy(savePassword = false, password = ""),
            ),
        )
        assertFalse(canRestoreBackgroundConnection(enabled = false, snapshot = eligible))
    }

    private fun snapshot(): LoginSnapshot {
        val servers = ServerPresets.resolveForProvider(
            ProviderOptions.ESCARGOT,
            ServerOverrides(),
        )
        return LoginSnapshot(
            email = "user@example.com",
            password = "secret",
            savePassword = true,
            provider = ProviderOptions.ESCARGOT,
            customServers = ServerOverrides(),
            resolvedServers = servers,
            useMockServer = false,
            automaticSignIn = true,
            loginPresence = Presence.Online,
        )
    }
}
