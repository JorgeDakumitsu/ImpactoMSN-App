package com.impactomsn.app

import android.content.Context
import com.impactomsn.app.data.MessengerRepository
import com.impactomsn.app.enums.Presence

internal suspend fun Context.restoreSignedInProfile(
    repository: MessengerRepository,
    serverProfile: UserProfileSnapshot,
    presence: Presence,
) {
    val mergedProfile = SessionPrefs.mergeServerProfile(this, serverProfile)
        .copy(presence = presence)
    repository.setLocalProfile(
        profile = mergedProfile,
        publishStoredSettings = true,
        // Fetch and compare before publishing a local picture over the server's one.
        publishDisplayPicture = false,
    )
    displayPictureSync.restore(repository, serverProfile,
        skipNextRefresh = (applicationContext as ImpactoMsnApplication).visibilityTracker.isForeground)
    val localProfile = repository.profile.value
    SessionPrefs.saveUserProfile(
        context = this,
        username = localProfile.username,
        presence = presence,
        avatarUri = localProfile.avatarUri,
        statusMessage = localProfile.statusMessage,
        saveDisplayPicture = false,
    )
}
