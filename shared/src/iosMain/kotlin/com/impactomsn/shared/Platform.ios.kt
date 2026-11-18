package com.impactomsn.shared

public actual object Platform {
    public actual val name: String = "iOS"
    public actual val isApple: Boolean = true
    // iOS background execution is entitlement- and OS-policy-dependent.
    public actual val supportsBackgroundConnection: Boolean = false
}
