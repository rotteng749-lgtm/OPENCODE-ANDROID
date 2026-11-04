package dev.bidzz.opencode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bidzz.opencode.core.design.GlassSurface
import dev.bidzz.opencode.core.design.OpenCodeTheme

/** Rows grouped on one rounded surface with a hairline edge: the shared "card" of the app. */
@Composable
fun CardGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tint = OpenCodeTheme.colors.surface,
        elevation = 14.dp,
    ) {
        Column(content = content)
    }
}

/** A glyph on a softly tinted rounded square; the leading mark of a list row. */
@Composable
fun IconBadge(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = OpenCodeTheme.colors.accent,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f)),
                ),
            )
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint = tint, size = size * 0.55f)
    }
}

/** Hairline between two rows of a card; [inset] lines it up under the text, not the icon. */
@Composable
fun RowDivider(inset: Dp = 0.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, OpenCodeTheme.colors.outline, Color.Transparent),
                ),
            ),
    )
}
