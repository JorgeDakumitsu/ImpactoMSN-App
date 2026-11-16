@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.impactomsn.app.pages

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.stringResource
import com.impactomsn.app.R
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.impactomsn.app.DisplayPictureSelection

@Composable
internal fun DisplayPictureResizeDialog(picture: DisplayPictureSelection, onResize: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.dp_resize_title)) },
        text = {
            Text(stringResource(
                if (picture.animation != null) R.string.dp_resize_message_animated else R.string.dp_resize_message,
                picture.width, picture.height, (picture.bytes.size + 1023) / 1024))
        },
        confirmButton = { Button(shapes = ButtonDefaults.shapes(), onClick = onResize) { Text(stringResource(R.string.dp_resize_continue)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) } },
    )
}
