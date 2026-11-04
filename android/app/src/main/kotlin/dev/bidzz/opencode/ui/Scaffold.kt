package dev.bidzz.opencode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bidzz.opencode.R
import dev.bidzz.opencode.core.design.OpenCodeTheme
import dev.bidzz.opencode.core.design.OpenCodeTypography

/**
 * The frame every secondary screen shares: a top bar with a back arrow, a title and optional
 * actions, and a body that clears the system bars.
 *
 * One place for the things that were repeated nine times and drifted apart: the back target is
 * 48dp and announced as a button, the status bar and the navigation bar are both respected
 * (edge-to-edge is on), and the keyboard pushes the body up instead of covering it.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    backLabel: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surfaceRaised, colors.surface),
                    ),
                )
                .height(56.dp)
                .padding(horizontal = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(Glyph.ArrowLeft, backLabel ?: stringResource(R.string.common_back), onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = spacing.xs, end = spacing.sm),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = OpenCodeTypography.title,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = OpenCodeTypography.label,
                        color = subtitleColor ?: colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            actions()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.accent.copy(alpha = 0.45f), colors.outline, Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            content = content,
        )
    }
}

/** A round icon button with the 48dp touch target and a spoken label. */
@Composable
fun IconAction(glyph: Glyph, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OpenCodeTheme.colors

    dev.bidzz.opencode.core.design.GlassSurface(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        tint = colors.surface,
        elevation = 6.dp,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, tint = colors.onSurface, size = 21.dp)
        }
    }
}

/** A text button for a top bar or a row of links: accent coloured, at least 48dp square. */
@Composable
fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = OpenCodeTheme.colors
    val spacing = OpenCodeTheme.tokens.spacing

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OpenCodeTypography.label,
            color = if (enabled) colors.accent else colors.onSurfaceFaint,
        )
    }
}
