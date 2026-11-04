package dev.bidzz.opencode.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bidzz.opencode.core.design.GlassSurface
import dev.bidzz.opencode.core.design.OpenCodeTheme

/**
 * The context that shows the app's chosen language. Set once in `OpenCodeApp`.
 *
 * Kept as its own local because a dialog is a separate window with its own compose root, and
 * that root resets [LocalContext] and [LocalConfiguration] to the activity's, which follows the
 * phone's language. Custom locals pass through into the dialog; those two do not.
 */
val LocalAppContext = staticCompositionLocalOf<Context?> { null }

@Composable
private fun Localized(content: @Composable () -> Unit) {
    val app = LocalAppContext.current
    if (app == null) {
        content()
    } else {
        CompositionLocalProvider(
            LocalContext provides app,
            LocalConfiguration provides app.resources.configuration,
            content = content,
        )
    }
}

/**
 * A glass dialog whose content is drawn in the app's chosen language.
 *
 * Material's [androidx.compose.material3.AlertDialog] paints an opaque container, which
 * would punch a hole in the glass system. This draws the same anatomy — title, body,
 * actions — on a translucent pane over the dimmed mesh.
 */
@Composable
fun LocalizedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(26.dp),
                tint = OpenCodeTheme.colors.surfaceRaised,
                elevation = 34.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            ) {
                Column {
                    title?.let {
                        Localized(it)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
                    }
                    text?.let {
                        Localized(it)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dismissButton?.let { Localized(it) }
                        Localized(confirmButton)
                    }
                }
            }
        }
    }
}
