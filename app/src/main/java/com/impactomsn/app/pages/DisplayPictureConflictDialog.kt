@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.pages

import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.impactomsn.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impactomsn.app.pages.components.ImpactoMsnCircularWavyProgressIndicator
import com.impactomsn.app.DisplayPictureConflict
import com.impactomsn.app.DisplayPictureSyncState
import com.impactomsn.app.SessionPrefs
import com.impactomsn.app.data.MessengerRepository
import com.impactomsn.app.displayPictureSync
import com.impactomsn.app.rememberAvatarImage
import com.impactomsn.app.ui.theme.ownDisplayPicture

@Composable
internal fun DisplayPictureSyncHost(repository: MessengerRepository) {
    val context = LocalContext.current
    val sync = remember(context) { context.displayPictureSync }
    val state by sync.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.notice) {
        state.notice?.let { notice ->
            Toast.makeText(context, notice, Toast.LENGTH_LONG).show()
            sync.consumeNotice(notice)
        }
    }
    val conflict = state.conflict ?: return
    if (!SessionPrefs.canOpenAuthenticatedScreens(context) || SessionPrefs.preferenceAccount(context) != conflict.account) return
    DisplayPictureConflictDialog(conflict, state,
        onKeepLocal = { sync.choose(repository, conflict.id, incoming = false) },
        onUseIncoming = { sync.choose(repository, conflict.id, incoming = true) },
        onDismiss = { sync.dismiss(conflict.id) })
}

@Composable
internal fun DisplayPictureConflictDialog(
    conflict: DisplayPictureConflict,
    state: DisplayPictureSyncState,
    onKeepLocal: () -> Unit,
    onUseIncoming: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text(stringResource(R.string.dp_conflict_title)) },
        modifier = Modifier.testTag("display-picture-conflict"),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.dp_conflict_message))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComparisonPicture(stringResource(R.string.dp_old_picture), stringResource(R.string.dp_on_device), conflict.local.uri,
                        Modifier.weight(1f).testTag("old-display-picture"))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.dp_changed_to), modifier = Modifier.size(24.dp))
                    ComparisonPicture(stringResource(R.string.dp_new_picture), stringResource(R.string.dp_from_messenger), conflict.incomingUri,
                        Modifier.weight(1f).testTag("incoming-display-picture"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onKeepLocal, enabled = !state.busy, shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f).testTag("keep-local-picture")) { Text(stringResource(R.string.dp_keep_local)) }
                    Button(onClick = onUseIncoming, enabled = !state.busy, shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f).testTag("use-incoming-picture")) { Text(stringResource(R.string.dp_use_incoming)) }
                }
                if (state.busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ImpactoMsnCircularWavyProgressIndicator(Modifier.size(20.dp))
                    Text(stringResource(R.string.dp_updating), style = MaterialTheme.typography.bodySmall)
                }
                state.error?.let { message -> Text(message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("picture-sync-error")) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !state.busy) { Text(stringResource(R.string.dp_not_now)) }
        },
    )
}

@Composable
private fun ComparisonPicture(title: String, subtitle: String, uri: String?, modifier: Modifier) {
    val image = rememberAvatarImage(uri, highResolution = true)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Surface(shape = MaterialTheme.shapes.ownDisplayPicture, color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(contentAlignment = Alignment.Center) {
                image.painter?.let { Image(it, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    ?: if (image.isLoading) ImpactoMsnCircularWavyProgressIndicator(Modifier.size(24.dp))
                    else Icon(Icons.Default.Person, contentDescription = stringResource(R.string.dp_no_display_picture), modifier = Modifier.size(48.dp))
            }
        }
        Text(if (uri == null) stringResource(R.string.dp_no_picture) else subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
