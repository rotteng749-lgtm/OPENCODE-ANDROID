package dev.bidzz.opencode.ui.terminal

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.view.TerminalView
import dev.bidzz.opencode.R
import dev.bidzz.opencode.core.design.GlassButton
import dev.bidzz.opencode.core.design.GlassCard
import dev.bidzz.opencode.core.design.GlassIconButton
import dev.bidzz.opencode.core.design.GlassSurface
import dev.bidzz.opencode.core.design.GlassTerminalFrame
import dev.bidzz.opencode.core.design.OpenCodeTheme
import dev.bidzz.opencode.core.design.OpenCodeTypography
import dev.bidzz.opencode.ui.Glyph
import dev.bidzz.opencode.ui.GlyphIcon
import dev.bidzz.opencode.ui.IconAction
import dev.bidzz.opencode.ui.ScreenScaffold

/**
 * A real embedded Termux-style terminal.
 *
 * - Multi-session tabs across the top.
 * - The terminal view renders the PTY output.
 * - A glass extra-keys bar below the terminal sends control characters.
 * - Copy/paste from the system clipboard.
 */
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: TerminalViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Notify the ViewModel when the screen is shown.
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
    }

    // Watch clipboard changes so the paste button reflects availability.
    val clipboardManager = LocalClipboardManager.current
    DisposableEffect(clipboardManager) {
        viewModel.onClipboardChanged()
        onDispose { }
    }

    BackHandler(enabled = true) { onBack() }

    ScreenScaffold(
        title = stringResource(R.string.terminal_title),
        onBack = onBack,
        actions = {
            IconAction(
                glyph = Glyph.Plus,
                description = stringResource(R.string.terminal_new_session),
                onClick = { viewModel.newSession() },
            )
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = OpenCodeTheme.tokens.spacing.md)
                .padding(bottom = OpenCodeTheme.tokens.spacing.md),
        ) {
            when (val install = state.installState) {
                is TerminalInstaller.State.NotInstalled,
                is TerminalInstaller.State.Downloading,
                is TerminalInstaller.State.Extracting -> {
                    InstallProgressPanel(install)
                }
                is TerminalInstaller.State.Failed -> {
                    InstallErrorPanel(
                        reason = install.reason,
                        onRetry = { viewModel.retryInstall() },
                    )
                }
                is TerminalInstaller.State.Installed -> {
                    TerminalContent(viewModel, state)
                }
            }
        }
    }
}

@Composable
private fun InstallProgressPanel(installState: TerminalInstaller.State) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.md),
        contentPadding = PaddingValues(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(
                text = stringResource(R.string.terminal_bootstrap_title),
                style = OpenCodeTypography.title,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.terminal_bootstrap_body),
                style = OpenCodeTypography.body,
                color = colors.onSurfaceMuted,
            )
            if (installState is TerminalInstaller.State.Downloading ||
                installState is TerminalInstaller.State.Extracting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.surfaceSunken,
                )
            }
        }
    }
}

@Composable
private fun InstallErrorPanel(reason: String, onRetry: () -> Unit) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.md),
        contentPadding = PaddingValues(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(
                text = stringResource(R.string.terminal_bootstrap_error),
                style = OpenCodeTypography.title,
                color = colors.danger,
            )
            Text(
                text = reason,
                style = OpenCodeTypography.body,
                color = colors.onSurfaceMuted,
            )
            GlassButton(onClick = onRetry) {
                Text(stringResource(R.string.common_refresh), style = OpenCodeTypography.label)
            }
        }
    }
}

@Composable
private fun TerminalContent(
    viewModel: TerminalViewModel,
    state: TerminalViewModel.UiState,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SessionTabs(
            sessions = state.sessions,
            selectedId = state.selectedId,
            onSelect = { viewModel.selectSession(it) },
            onClose = { viewModel.closeSession(it) },
            onRename = { id, name -> viewModel.renameSession(id, name) },
        )

        val session = state.selectedId?.let { id -> state.sessions.find { it.id == id } }
        if (session != null) {
            GlassTerminalFrame(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AndroidView(
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            setTextSize(14)
                        }
                    },
                    update = { view ->
                        viewModel.attach(view, session.id)
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { },
                )
            }

            if (session.isFallback) {
                FallbackBanner()
            }

            val clipboard = LocalClipboardManager.current
            if (state.showExtraKeys) {
            ExtraKeysBar(
                onKey = { viewModel.sendControlKey(it) },
                onPaste = {
                    val text = clipboard.getText()?.text.orEmpty()
                    if (text.isNotEmpty()) viewModel.paste(text)
                },
                onCopy = {
                    val selected = viewModel.copyFromTerminal()
                    if (selected.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(selected))
                    }
                },
                canPaste = state.canPaste,
            )
        }
        } else {
            EmptyTerminalPlaceholder()
        }
    }
}

@Composable
private fun SessionTabs(
    sessions: List<TerminalViewModel.Session>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onRename: (String, String) -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(horizontal = spacing.xs),
    ) {
        items(sessions, key = { it.id }) { session ->
            var expanded by remember { mutableStateOf(false) }

            Box {
                SessionTabPill(
                    label = session.name,
                    selected = session.id == selectedId,
                    onClick = { onSelect(session.id) },
                    onClose = { onClose(session.id) },
                    onLongClick = { expanded = true },
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.surfaceRaised),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal_rename_session)) },
                        onClick = {
                            expanded = false
                            // Simple rename to a counter-based label; a real dialog is overkill here.
                            onRename(session.id, "${session.shell} ${session.handle.take(4)}")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal_close_session)) },
                        onClick = {
                            expanded = false
                            onClose(session.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val shape = RoundedCornerShape(10.dp)

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.surface)
            .border(1.dp, if (selected) colors.accent.copy(alpha = 0.5f) else colors.outline, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = OpenCodeTypography.label,
            color = if (selected) colors.accent else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GlassIconButton(
            description = stringResource(R.string.terminal_close_session),
            onClick = onClose,
            modifier = Modifier.size(28.dp),
            tint = colors.onSurfaceMuted,
        ) {
            GlyphIcon(Glyph.Close, tint = colors.onSurfaceMuted, size = 16.dp)
        }
    }
}

@Composable
private fun FallbackBanner() {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tint = colors.warning.copy(alpha = 0.12f),
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        Column {
            Text(
                text = stringResource(R.string.terminal_fallback_title),
                style = OpenCodeTypography.label,
                color = colors.warning,
            )
            Text(
                text = stringResource(R.string.terminal_fallback_body),
                style = OpenCodeTypography.bodySmall,
                color = colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun EmptyTerminalPlaceholder() {
    val colors = OpenCodeTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(OpenCodeTheme.tokens.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.terminal_new_session),
            style = OpenCodeTypography.body,
            color = colors.onSurfaceFaint,
        )
    }
}

@Composable
private fun ExtraKeysBar(
    onKey: (String) -> Unit,
    onPaste: () -> Unit,
    onCopy: () -> Unit,
    canPaste: Boolean,
) {
    val colors = OpenCodeTheme.colors
    val keys = listOf(
        "ESC", "TAB", "CTRL", "ALT",
        "UP", "DOWN", "LEFT", "RIGHT",
        "PGUP", "PGDN", "HOME", "END",
        "BKSP", "DEL",
    )

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        tint = colors.surfaceSunken,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            keys.forEach { key ->
                ExtraKeyButton(key) { onKey(key) }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.outline),
            )
            ExtraKeyButton(stringResource(R.string.terminal_copy)) { onCopy() }
            ExtraKeyButton(
                label = stringResource(R.string.terminal_paste),
                enabled = canPaste,
            ) { onPaste() }
        }
    }
}

@Composable
private fun ExtraKeyButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = OpenCodeTheme.colors
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 36.dp)
            .clip(shape)
            .background(if (enabled) colors.surface else Color.Transparent)
            .border(1.dp, colors.outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OpenCodeTypography.bodySmall,
            color = if (enabled) colors.onSurface else colors.onSurfaceFaint,
        )
    }
}


