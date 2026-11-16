package com.impactomsn.app.notifications

import com.impactomsn.app.LoginSnapshot

internal fun canRestoreBackgroundConnection(
    enabled: Boolean,
    snapshot: LoginSnapshot,
): Boolean = enabled &&
    snapshot.savePassword &&
    snapshot.automaticSignIn &&
    snapshot.email.isNotBlank() &&
    snapshot.password.isNotEmpty()
