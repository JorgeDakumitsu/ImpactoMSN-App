package com.impactomsn.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class PlatformTest {
    @Test
    fun platformCapabilitiesAreAvailable() {
        val capabilities = PlatformCapabilities()
        assertNotNull(capabilities.platformName)
        if (!capabilities.isApple) assertFalse(capabilities.supportsBackgroundConnection.not())
    }
}
