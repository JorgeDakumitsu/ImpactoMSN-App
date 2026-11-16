package com.impactomsn.app.pages.components

import com.impactomsn.app.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource

@Composable
internal fun ContactReadAction(
    contactId: String,
    unread: Boolean,
    onReadStateChange: ((Boolean) -> Unit)?,
    onClick: () -> Unit,
    content: @Composable (Modifier, () -> Unit) -> Unit,
) {
    if (onReadStateChange == null) {
        content(Modifier, onClick)
        return
    }
    val readActionLabel = stringResource(if (unread) R.string.common_mark_read else R.string.common_mark_unread)
    val readStateLabel = stringResource(if (unread) R.string.common_unread else R.string.common_read)
    SwipeRevealAction(
        itemKey = contactId,
        label = readActionLabel,
        icon = if (unread) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
        onAction = { onReadStateChange(!unread) },
        onClick = onClick,
        closeAfterAction = true,
    ) { modifier, onRowClick ->
        content(modifier.semantics { stateDescription = readStateLabel }, onRowClick)
    }
}
