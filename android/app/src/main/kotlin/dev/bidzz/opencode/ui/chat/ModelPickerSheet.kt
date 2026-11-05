package dev.bidzz.opencode.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * ChatGPT-style model picker.
 *
 * Two levels, both in one sheet:
 * 1. Routing combos (Auto/Fast/Smart) — the router picks a provider per request.
 * 2. A fixed provider + model — bypasses the router.
 *
 * The selection is persisted by [dev.bidzz.opencode.engine.DirectProviderStore]
 * through the callbacks, so it survives restarts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    app: OpenCodeApplication,
    activeCombo: String,
    onPickCombo: (String) -> Unit,
    onPickModel: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing
    val store = app.directProviderStore
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var openProviderId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xl),
        ) {
            val openProvider = openProviderId?.let { FreeProviders.byId(it) }

            if (openProvider == null) {
                Text(
                    text = "Pilih model",
                    style = OpenCodeTypography.title,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                )

                Text(
                    text = "ROUTING OTOMATIS",
                    style = OpenCodeTypography.label,
                    color = colors.onSurfaceFaint,
                    modifier = Modifier.padding(start = spacing.lg, top = spacing.md, bottom = spacing.sm),
                )
                CardGroup {
                    app.aiRouter.combos.forEachIndexed { index, combo ->
                        if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                        PickerRow(
                            title = combo.name,
                            subtitle = "${combo.routes.size} model • ${combo.strategy.name}",
                            selected = store.routerEnabled && combo.name == activeCombo,
                            onClick = { onPickCombo(combo.name) },
                        )
                    }
                }

                Text(
                    text = "MODEL LANGSUNG",
                    style = OpenCodeTypography.label,
                    color = colors.onSurfaceFaint,
                    modifier = Modifier.padding(start = spacing.lg, top = spacing.xl, bottom = spacing.sm),
                )
                CardGroup {
                    FreeProviders.all.forEachIndexed { index, provider ->
                        if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                        val ready = store.isReady(provider.id)
                        val selected = !store.routerEnabled && store.activeProviderId == provider.id
                        PickerRow(
                            title = provider.name,
                            subtitle = buildString {
                                append("${provider.models.size} model")
                                if (!provider.requiresKey) append(" • tanpa API key")
                                else if (!ready) append(" • butuh API key")
                            },
                            selected = selected,
                            onClick = { openProviderId = provider.id },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { openProviderId = null }
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(Glyph.ChevronRight, tint = colors.onSurfaceMuted, size = 18.dp)
                    Text(
                        text = openProvider.name,
                        style = OpenCodeTypography.title,
                        color = colors.onSurface,
                        modifier = Modifier.padding(start = spacing.sm),
                    )
                }
                Text(
                    text = "MODEL",
                    style = OpenCodeTypography.label,
                    color = colors.onSurfaceFaint,
                    modifier = Modifier.padding(start = spacing.lg, bottom = spacing.sm),
                )
                CardGroup {
                    openProvider.models.forEachIndexed { index, model ->
                        if (index > 0) RowDivider(inset = spacing.md + 36.dp + spacing.md)
                        val selected = !store.routerEnabled &&
                            store.activeProviderId == openProvider.id &&
                            store.activeModelId == model.id
                        PickerRow(
                            title = model.name,
                            subtitle = model.id,
                            selected = selected,
                            onClick = { onPickModel(openProvider.id, model.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        IconBadge(Glyph.Sliders)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OpenCodeTypography.body,
                color = if (selected) colors.accent else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OpenCodeTypography.bodySmall,
                color = colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Text("●", style = OpenCodeTypography.bodySmall, color = colors.accent)
        }
    }
}
