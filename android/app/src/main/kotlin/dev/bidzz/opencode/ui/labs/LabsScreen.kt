package dev.bidzz.opencode.ui.labs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.bidzz.opencode.OpenCodeApplication
import dev.bidzz.opencode.core.design.OpenCodeTheme
import dev.bidzz.opencode.core.design.OpenCodeTypography
import dev.bidzz.opencode.ui.CardGroup
import dev.bidzz.opencode.ui.Glyph
import dev.bidzz.opencode.ui.GlyphIcon
import dev.bidzz.opencode.ui.IconBadge
import dev.bidzz.opencode.ui.RowDivider
import dev.bidzz.opencode.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Experimental/labs screen for mega-features:
 * - AI Router combos
 * - MCP servers
 * - Claude plugins
 * - Skills (GitHub / manual)
 * - Root shell tools
 */
@Composable
fun LabsScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    var section by remember { mutableStateOf<LabsSection?>(null) }

    when (section) {
        LabsSection.Router -> RouterScreen(app = app, onBack = { section = null })
        LabsSection.Mcp -> McpScreen(app = app, onBack = { section = null })
        LabsSection.Plugins -> PluginsScreen(app = app, onBack = { section = null })
        LabsSection.Skills -> SkillsScreen(app = app, onBack = { section = null })
        LabsSection.Root -> RootScreen(app = app, onBack = { section = null })
        null -> ScreenScaffold(title = "Labs", onBack = onBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
            ) {
                LabRow(
                    glyph = Glyph.Globe,
                    title = "AI Router",
                    subtitle = "9Router-style combos: Auto, Fast, Smart",
                    onClick = { section = LabsSection.Router },
                )
                LabRow(
                    glyph = Glyph.Terminal,
                    title = "MCP Servers",
                    subtitle = "Filesystem, memory, fetch, git, puppeteer…",
                    onClick = { section = LabsSection.Mcp },
                )
                LabRow(
                    glyph = Glyph.Sliders,
                    title = "Claude Plugins",
                    subtitle = "Install official & community plugins",
                    onClick = { section = LabsSection.Plugins },
                )
                LabRow(
                    glyph = Glyph.File,
                    title = "Skills",
                    subtitle = "Add from GitHub or paste SKILL.md",
                    onClick = { section = LabsSection.Skills },
                )
                LabRow(
                    glyph = Glyph.Terminal,
                    title = "Root Tools",
                    subtitle = "Shell, apps, browser automation",
                    onClick = { section = LabsSection.Root },
                )
            }
        }
    }
}

private enum class LabsSection { Router, Mcp, Plugins, Skills, Root }

@Composable
private fun RouterScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val router = app.aiRouter
    var selected by remember { mutableStateOf(router.combos.firstOrNull()?.name ?: "Auto") }
    var refresh by remember { mutableStateOf(0) }

    ScreenScaffold(title = "AI Router", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "COMBO",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                router.combos.forEachIndexed { index, combo ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    ProviderRow(
                        title = combo.name,
                        subtitle = "${combo.routes.size} routes • ${combo.strategy.name}",
                        value = if (combo.name == selected) "●" else null,
                        onClick = {
                            selected = combo.name
                            refresh++
                        },
                    )
                }
            }

            Text(
                text = "ACTIVE ROUTES",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                val active = router.combos.find { it.name == selected }
                active?.routes?.forEachIndexed { index, route ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    ProviderRow(
                        title = route.modelId,
                        subtitle = "${route.providerId} • ${route.tier.name}",
                        onClick = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun McpScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val manager = app.mcpServerManager
    var refresh by remember { mutableStateOf(0) }

    ScreenScaffold(title = "MCP Servers", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "SERVERS",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                manager.registry.forEachIndexed { index, config ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    val running = manager.isRunning(config.id)
                    ProviderRow(
                        title = config.name,
                        subtitle = config.description +
                            (config.setupHint?.let { "\nRequires: $it" } ?: ""),
                        value = if (running) "ON" else "OFF",
                        onClick = {
                            if (running) manager.stop(config.id) else manager.start(config)
                            refresh++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginsScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val manager = app.pluginManager
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }

    ScreenScaffold(title = "Claude Plugins", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "REGISTRY",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                val installed = manager.installedPlugins().map { it.id }
                manager.registry.forEachIndexed { index, plugin ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    ProviderRow(
                        title = plugin.name,
                        subtitle = plugin.description,
                        value = if (plugin.id in installed) "✓" else null,
                        onClick = {
                            scope.launch {
                                manager.installFromRegistry(plugin.id)
                                refresh++
                            }
                        },
                    )
                }
            }

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.padding(top = spacing.xl).fillMaxWidth(),
            ) { Text("Install from GitHub URL", color = colors.onAccent) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Install plugin from GitHub", color = colors.onSurface) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Repo URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        manager.installFromGitHub(url)
                        refresh++
                    }
                    showDialog = false
                }) { Text("Install", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = colors.onSurfaceMuted)
                }
            },
        )
    }
}

@Composable
private fun SkillsScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val manager = app.skillManager
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    var showGitHub by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var markdown by remember { mutableStateOf("") }

    ScreenScaffold(title = "Skills", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "BUILT-IN",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                manager.registry.forEachIndexed { index, skill ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    ProviderRow(
                        title = skill.name,
                        subtitle = skill.description,
                        onClick = {
                            manager.installFromMarkdown(skill.id, skill.instructions)
                            refresh++
                        },
                    )
                }
            }

            Button(
                onClick = { showGitHub = true },
                modifier = Modifier.padding(top = spacing.xl).fillMaxWidth(),
            ) { Text("Add from GitHub", color = colors.onAccent) }
            Button(
                onClick = { showManual = true },
                modifier = Modifier.padding(top = spacing.sm).fillMaxWidth(),
            ) { Text("Add SKILL.md manually", color = colors.onAccent) }
        }
    }

    if (showGitHub) {
        AlertDialog(
            onDismissRequest = { showGitHub = false },
            title = { Text("Add skill from GitHub", color = colors.onSurface) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("GitHub repo or raw SKILL.md URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        manager.installFromGitHub(url)
                        refresh++
                    }
                    showGitHub = false
                }) { Text("Add", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showGitHub = false }) {
                    Text("Cancel", color = colors.onSurfaceMuted)
                }
            },
        )
    }

    if (showManual) {
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Add skill manually", color = colors.onSurface) },
            text = {
                OutlinedTextField(
                    value = markdown,
                    onValueChange = { markdown = it },
                    label = { Text("Paste SKILL.md") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    manager.installFromMarkdown("manual-${System.currentTimeMillis()}", markdown)
                    refresh++
                    showManual = false
                }) { Text("Add", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showManual = false }) {
                    Text("Cancel", color = colors.onSurfaceMuted)
                }
            },
        )
    }
}

@Composable
private fun RootScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }

    ScreenScaffold(title = "Root Tools", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "Run commands as root. Requires rooted device.",
                style = OpenCodeTypography.bodySmall,
                color = colors.onSurfaceMuted,
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("su -c ...") },
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
            )
            Button(
                onClick = {
                    scope.launch {
                        val result = app.rootShell.execute(command)
                        output = "exit=${result.exitCode}\n\nSTDOUT:\n${result.stdout}\n\nSTDERR:\n${result.stderr}"
                    }
                },
                modifier = Modifier.padding(top = spacing.md).fillMaxWidth(),
            ) { Text("Run", color = colors.onAccent) }

            Text(
                text = output,
                style = OpenCodeTypography.bodySmall,
                color = colors.onSurface,
                modifier = Modifier.padding(top = spacing.lg),
            )
        }
    }
}

@Composable
private fun LabRow(
    glyph: Glyph,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(glyph)
        Column(
            modifier = Modifier.weight(1f).padding(start = spacing.md),
        ) {
            Text(title, style = OpenCodeTypography.body, color = colors.onSurface)
            Text(subtitle, style = OpenCodeTypography.bodySmall, color = colors.onSurfaceMuted)
        }
        GlyphIcon(Glyph.ChevronRight, tint = colors.onSurfaceFaint, size = 18.dp)
    }
}

@Composable
private fun ProviderRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    value: String? = null,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Glyph.Sliders)
        Column(
            modifier = Modifier.weight(1f).padding(start = spacing.md),
        ) {
            Text(title, style = OpenCodeTypography.body, color = colors.onSurface)
            Text(
                text = subtitle,
                style = OpenCodeTypography.bodySmall,
                color = colors.onSurfaceMuted,
                maxLines = 3,
            )
        }
        if (value != null) {
            Text(value, style = OpenCodeTypography.bodySmall, color = colors.accent)
        }
        if (onClick != null) {
            GlyphIcon(Glyph.ChevronRight, tint = colors.onSurfaceFaint, size = 18.dp)
        }
    }
}
