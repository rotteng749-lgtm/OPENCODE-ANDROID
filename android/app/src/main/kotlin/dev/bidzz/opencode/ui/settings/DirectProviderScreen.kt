package dev.bidzz.opencode.ui.settings

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.bidzz.opencode.OpenCodeApplication
import dev.bidzz.opencode.core.design.OpenCodeTheme
import dev.bidzz.opencode.core.design.OpenCodeTypography
import dev.bidzz.opencode.engine.FreeProviders
import dev.bidzz.opencode.ui.CardGroup
import dev.bidzz.opencode.ui.Glyph
import dev.bidzz.opencode.ui.GlyphIcon
import dev.bidzz.opencode.ui.IconBadge
import dev.bidzz.opencode.ui.RowDivider
import dev.bidzz.opencode.ui.ScreenScaffold

/**
 * Free-provider configuration for direct-API mode.
 *
 * Lists every provider from [FreeProviders], lets the user paste an API key,
 * pick the active provider/model, and edit the system prompt. Values persist in
 * [dev.bidzz.opencode.engine.DirectProviderStore].
 */
@Composable
fun DirectProviderScreen(
    app: OpenCodeApplication,
    onBack: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val store = app.directProviderStore

    var editingKeyFor by remember { mutableStateOf<String?>(null) }
    var editingSystemPrompt by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    ScreenScaffold(title = "Free Providers", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            Text(
                text = "SYSTEM PROMPT",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                ProviderRow(
                    title = "System prompt",
                    subtitle = store.systemPrompt.take(80) + if (store.systemPrompt.length > 80) "…" else "",
                    onClick = { editingSystemPrompt = true },
                )
            }

            Text(
                text = "PROVIDERS",
                style = OpenCodeTypography.label,
                color = colors.onSurfaceFaint,
                modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
            )
            CardGroup {
                FreeProviders.all.forEachIndexed { index, provider ->
                    if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                    val hasKey = store.apiKey(provider.id).isNotBlank()
                    val active = store.activeProviderId == provider.id
                    val status = when {
                        active -> "Active"
                        !provider.requiresKey -> "No key needed"
                        hasKey -> "Key saved"
                        else -> "Tap to add key"
                    }
                    ProviderRow(
                        title = provider.name,
                        subtitle = "${provider.models.size} models • $status",
                        value = if (active) "●" else null,
                        onClick = {
                            if (provider.requiresKey && !hasKey) {
                                editingKeyFor = provider.id
                            } else {
                                store.activeProviderId = provider.id
                                refresh++
                            }
                        },
                    )
                }
            }

            // Model picker for the active provider
            val activeProvider = store.activeProvider()
            if (activeProvider != null) {
                Text(
                    text = "MODEL — ${activeProvider.name.uppercase()}",
                    style = OpenCodeTypography.label,
                    color = colors.onSurfaceFaint,
                    modifier = Modifier.padding(start = spacing.xs, top = spacing.xl, bottom = spacing.sm),
                )
                CardGroup {
                    activeProvider.models.forEachIndexed { index, model ->
                        if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                        val selected = store.activeModel(activeProvider).id == model.id
                        ProviderRow(
                            title = model.name,
                            subtitle = model.id,
                            value = if (selected) "●" else null,
                            onClick = {
                                store.activeModelId = model.id
                                refresh++
                            },
                        )
                    }
                }
            }
        }
    }

    // API-key dialog
    editingKeyFor?.let { providerId ->
        val provider = FreeProviders.byId(providerId) ?: return@let
        var key by remember { mutableStateOf(store.apiKey(providerId)) }
        AlertDialog(
            onDismissRequest = { editingKeyFor = null },
            title = { Text("${provider.name} API key", color = colors.onSurface) },
            text = {
                Column {
                    Text(
                        "Paste your API key. Get one free at the provider's site.",
                        style = OpenCodeTypography.bodySmall,
                        color = colors.onSurfaceMuted,
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
                        singleLine = true,
                        placeholder = { Text("sk-…") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.setApiKey(providerId, key.trim())
                    if (key.isNotBlank()) store.activeProviderId = providerId
                    editingKeyFor = null
                    refresh++
                }) { Text("Save", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { editingKeyFor = null }) {
                    Text("Cancel", color = colors.onSurfaceMuted)
                }
            },
        )
    }

    // System-prompt dialog
    if (editingSystemPrompt) {
        var prompt by remember { mutableStateOf(store.systemPrompt) }
        AlertDialog(
            onDismissRequest = { editingSystemPrompt = false },
            title = { Text("System prompt", color = colors.onSurface) },
            text = {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.systemPrompt = prompt
                    editingSystemPrompt = false
                    refresh++
                }) { Text("Save", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { editingSystemPrompt = false }) {
                    Text("Cancel", color = colors.onSurfaceMuted)
                }
            },
        )
    }
}

@Composable
private fun ProviderRow(
    title: String,
    subtitle: String?,
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
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.md),
        ) {
            Text(title, style = OpenCodeTypography.body, color = colors.onSurface)
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = OpenCodeTypography.bodySmall,
                    color = colors.onSurfaceMuted,
                    maxLines = 2,
                )
            }
        }
        if (value != null) {
            Text(value, style = OpenCodeTypography.bodySmall, color = colors.accent)
        }
        if (onClick != null) {
            GlyphIcon(
                Glyph.ChevronRight,
                tint = colors.onSurfaceFaint,
                size = 18.dp,
                modifier = Modifier.padding(start = spacing.sm),
            )
        }
    }
}
