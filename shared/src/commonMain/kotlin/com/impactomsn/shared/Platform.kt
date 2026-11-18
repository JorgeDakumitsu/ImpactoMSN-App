package com.impactomsn.shared

/**
 * Runtime information exposed to shared code.
 *
 * Keep platform-specific APIs behind this boundary. The Android application can
 * migrate incrementally while Apple applications use the same protocol and
 * domain modules.
 */
public expect object Platform {
    public val name: String
    public val isApple: Boolean
    public val supportsBackgroundConnection: Boolean
}

public data class PlatformCapabilities(
    val platformName: String = Platform.name,
    val isApple: Boolean = Platform.isApple,
    val supportsBackgroundConnection: Boolean = Platform.supportsBackgroundConnection,
)
