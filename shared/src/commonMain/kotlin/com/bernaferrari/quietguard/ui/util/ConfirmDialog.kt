package com.bernaferrari.quietguard.ui.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.menu_cancel
import com.bernaferrari.quietguard.generated.resources.msg_sure
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
import org.jetbrains.compose.resources.stringResource

/** Confirmation for actions that can't be undone; the confirm button uses error colors. */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    confirmLabel: String,
    icon: MaterialIcon,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String = stringResource(Res.string.msg_sure),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon = icon, contentDescription = null) },
        iconContentColor = MaterialTheme.colorScheme.error,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(text = confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.menu_cancel)) }
        },
    )
}
