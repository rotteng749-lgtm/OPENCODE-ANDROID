package dev.bidzz.opencode.core.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Glass primitives.
 *
 * A real backdrop blur on every card is expensive on mid-range phones and would put
 * a RenderEffect on top of a scrolling chat. The kit's look is reproduced with what
 * actually reads as glass at 60 fps: a translucent tint, a bright hairline, a top
 * highlight, a soft shadow, and a colourful mesh behind it. The mesh is static, so
 * the background costs one draw pass per frame, not a blur per card.
 */

/** The mesh every screen floats over. One instance per app, behind the navigation. */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val dark = colors.background.luminanceIsDark()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (dark) Palette.graphite else Palette.ivory)
            .drawMesh(colors, dark),
        content = content,
    )
}

private fun Color.luminanceIsDark(): Boolean =
    (0.2126f * red + 0.7152f * green + 0.0722f * blue) < 0.45f

private fun Modifier.drawMesh(colors: OpenCodeColors, dark: Boolean): Modifier =
    this.then(
        Modifier.drawBehind {
            val width = size.width
            val height = size.height
            val alpha = if (dark) 1f else 0.55f

            fun glow(color: Color, center: Offset, radius: Float, strength: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = strength), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }

            glow(colors.glowA, Offset(width * 0.08f, -height * 0.02f), width * 0.95f, 0.20f * alpha)
            glow(colors.glowB, Offset(width * 0.98f, height * 0.10f), width * 0.85f, 0.16f * alpha)
            glow(colors.glowC, Offset(width * 0.25f, height * 1.02f), width * 1.05f, 0.15f * alpha)

            // A whisper of a grid: it gives the eye a plane to read the glass against,
            // without becoming a visible pattern.
            val step = 56f * density
            val line = if (dark) Color.White.copy(alpha = 0.018f) else Color.Black.copy(alpha = 0.018f)
            var x = 0f
            while (x < width) {
                drawLine(line, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < height) {
                drawLine(line, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                y += step
            }

            // Top and bottom scrims keep status/navigation bar icons readable.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (dark) Color.Black else Color.White).copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = 140f * density,
                ),
            )
        },
    )

/** Base translucent pane. Everything glass in the app is built on this. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(OpenCodeTheme.tokens.radius.lg),
    tint: Color = OpenCodeTheme.colors.surface,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val clickable = if (onClick != null) {
        Modifier.clickable(role = role, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = min(1f, tint.alpha * 1.35f)),
                        tint,
                    ),
                ),
            )
            .border(
                borderWidth,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (colors.background.luminanceIsDark()) 0.30f else 0.75f),
                        colors.outline,
                        Color.White.copy(alpha = if (colors.background.luminanceIsDark()) 0.05f else 0.25f),
                    ),
                ),
                shape,
            )
            .then(clickable)
            .padding(contentPadding),
        content = content,
    )
}

/** A titled panel: the app's standard grouping surface. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = OpenCodeTheme.colors.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        tint = tint,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = OpenCodeTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "glass-button-scale")

    val fill = if (primary) {
        Brush.horizontalGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.82f)))
    } else {
        Brush.verticalGradient(listOf(colors.surfaceRaised, colors.surface))
    }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .shadow(if (primary) 14.dp else 0.dp, shape, clip = false, ambientColor = colors.accent.copy(alpha = 0.25f), spotColor = colors.accent.copy(alpha = 0.25f))
            .clip(shape)
            .background(if (enabled) fill else Brush.verticalGradient(listOf(colors.surface, colors.surface)))
            .border(1.dp, if (primary) colors.accent.copy(alpha = 0.45f) else colors.outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun GlassIconButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = OpenCodeTheme.colors.onSurface,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        tint = OpenCodeTheme.colors.surface,
        elevation = 8.dp,
        onClick = if (enabled) onClick else null,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                content()
            }
        }
    }
}

/** A selectable pill. The only place a filled accent is used at rest. */
@Composable
fun GlassPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = OpenCodeTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent else colors.surfaceVariant)
            .border(1.dp, if (selected) colors.accent.copy(alpha = 0.6f) else colors.outline, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke(this)
        Text(
            text = label,
            style = OpenCodeTypography.label,
            color = if (selected) colors.onAccent else colors.onSurfaceMuted,
            maxLines = 1,
        )
    }
}

/** Glass text field. Used where the stock outlined field's opaque fill breaks the look. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    textStyle: TextStyle = OpenCodeTypography.input,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentDescription: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = OpenCodeTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = colors.onSurfaceFaint,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = textStyle.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.accent),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke(this)
    }
}

/** A hairline that reads as an edge of glass rather than a drawn line. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier
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

/** The rounded slab the terminal itself sits in. TerminalView paints its own background. */
@Composable
fun GlassTerminalFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val colors = OpenCodeTheme.colors
    Box(
        modifier = modifier
            .shadow(24.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(shape)
            .background(Color(0xFF050807))
            .border(1.dp, colors.outline, shape),
        content = content,
    )
}
