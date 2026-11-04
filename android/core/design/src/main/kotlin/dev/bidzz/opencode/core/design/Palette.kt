package dev.bidzz.opencode.core.design

import androidx.compose.ui.graphics.Color

/**
 * Glass palette.
 *
 * The identity is still the logo's three colours — graphite, ivory, electric lime —
 * but the surfaces are now translucent panes floating over a soft three-colour mesh.
 * No surface is opaque except the app backdrop, which is what makes the glass read
 * as glass instead of as flat grey cards.
 *
 * Rules that keep this from turning into neon soup:
 *  - Lime is the only action colour. Cyan and violet appear only in the backdrop
 *    mesh and in decorative hairline gradients, never on a control.
 *  - Text is always fully opaque. Translucency is for fills and borders only, so
 *    contrast stays above WCAG AA on every pane.
 *  - Status colours are muted; a developer tool should not look like a dashboard.
 */
object Palette {

    // ---- Brand ----

    val graphite = Color(0xFF070A0B)
    val ivory = Color(0xFFF6F2EE)
    val electricLime = Color(0xFFB8FF3D)
    val cyan = Color(0xFF5EEAD4)
    val violet = Color(0xFFA78BFA)

    // ---- Dark: the primary experience. ----

    /** Backdrop base. Screens themselves stay transparent so the mesh shows through. */
    val darkBackground = graphite

    /** Default glass pane: white at 6%, enough to lift off the mesh without fogging it. */
    val darkSurface = Color(0x14FFFFFF)

    /** One step raised: sheets, menus, the composer. */
    val darkSurfaceRaised = Color(0x1FFFFFFF)

    /** Inset panes: code blocks, terminal chrome, fields. Darkens instead of lifting. */
    val darkSurfaceSunken = Color(0x38000000)

    /** Chips and the user bubble. */
    val darkSurfaceVariant = Color(0x1AFFFFFF)

    /** Primary text: opaque, never tinted by the glass. */
    val darkOnSurface = Color(0xFFF2F7F5)

    val darkOnSurfaceMuted = Color(0xFFB4C4C1)

    /** Small labels; 4.6:1 on the darkest part of the mesh. */
    val darkOnSurfaceFaint = Color(0xFF84948F)

    /** Hairline on glass: white at 15%. */
    val darkOutline = Color(0x26FFFFFF)

    val darkAccent = electricLime
    val darkOnAccent = Color(0xFF07120A)

    /** User bubble: a lime wash, not a lime slab. */
    val darkUserBubble = Color(0x1FB8FF3D)
    val darkOnUserBubble = Color(0xFFECFFD6)

    // ---- Light ----

    val lightBackground = ivory

    val lightSurface = Color(0xB8FFFFFF)
    val lightSurfaceRaised = Color(0xD9FFFFFF)
    val lightSurfaceSunken = Color(0x140B1210)
    val lightSurfaceVariant = Color(0x99FFFFFF)

    val lightOnSurface = Color(0xFF0B1210)
    val lightOnSurfaceMuted = Color(0xFF44524F)

    /** 5.1:1 on the lightest pane. */
    val lightOnSurfaceFaint = Color(0xFF5F6D6A)

    val lightOutline = Color(0x240B1210)

    /** Darker lime: readable as text and icons on ivory (5:1). */
    val lightAccent = Color(0xFF447000)
    val lightOnAccent = Color(0xFFFFFFFF)

    val lightUserBubble = Color(0x1F447000)
    val lightOnUserBubble = Color(0xFF0B1210)

    // ---- Status. Muted, same hue logic in both themes. ----

    val success = Color(0xFF52E5A0)
    val successLight = Color(0xFF147A4B)

    val warning = Color(0xFFFFD166)
    val warningLight = Color(0xFF8A6100)

    val danger = Color(0xFFFF6B81)
    val dangerLight = Color(0xFFC11B36)

    val neutral = Color(0xFF8A9A98)

    // ---- Syntax roles for code blocks. ----

    val darkSyntaxKeyword = Color(0xFF7DD3FC)
    val darkSyntaxString = Color(0xFFFDA4AF)
    val darkSyntaxNumber = Color(0xFFBEF264)
    val darkSyntaxComment = Color(0xFF7C8C8A)

    val lightSyntaxKeyword = Color(0xFF1D4ED8)
    val lightSyntaxString = Color(0xFF9F1239)
    val lightSyntaxNumber = Color(0xFF3F6212)
    val lightSyntaxComment = Color(0xFF4B5A57)
}
