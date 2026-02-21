package com.example.asmrkeyboard.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines the set of colors used in the keyboard theme.
 * @param keyBackgroundColor The background color of the key.
 * @param keyBackgroundColorPressed The background color of the key when pressed.
 * @param keyTextColor The color of the text on the key.
 * @param keyboardBackgroundColor The background color of the entire keyboard.
 */
data class KeyboardColors(
    val keyBackgroundColor: Color,
    val keyBackgroundColorPressed: Color,
    val keyTextColor: Color,
    val keyboardBackgroundColor: Color,
)

/**
 * Defines the animation properties for a key.
 * @param pressScaleX The horizontal scale of the key when pressed.
 * @param pressScaleY The vertical scale of the key when pressed.
 * @param scaleAnimationSpec The animation spec for scaling.
 */
data class KeyAnimation(
    val pressScaleX: Float = 1f,
    val pressScaleY: Float = 1f,
    val scaleAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
)

// Color definitions for the Wood theme
private val WoodColors = KeyboardColors(
    keyBackgroundColor = Color(0xFF8D6E63), // Warm wood color
    keyBackgroundColorPressed = Color(0xFF795548), // Slightly darker wood color
    keyTextColor = Color.White,
    keyboardBackgroundColor = Color(0xFF5D4037) // Dark wood grain background
)

// Animation definition for the Wood theme
private val WoodAnimation = KeyAnimation()

// Color definitions for the FrozenSerenity theme
private val FrozenSerenityColors = KeyboardColors(
    keyBackgroundColor = Color(0x99FFFFFF), // Translucent ice
    keyBackgroundColorPressed = Color(0xCCFFFFFF), // Cracked, whiter ice
    keyTextColor = Color(0xFF0D47A1), // Deep blue
    keyboardBackgroundColor = Color(0xFFE3F2FD) // Pale blue ice wall
)

// Animation definition for the FrozenSerenity theme
private val FrozenSerenityAnimation = KeyAnimation()

// Color definitions for the SlimePop theme
private val SlimePopColors = KeyboardColors(
    keyBackgroundColor = Color(0xFFE0F7FA), // Light pastel cyan
    keyBackgroundColorPressed = Color(0xFFB2EBF2), // Slightly darker cyan
    keyTextColor = Color(0xFF006064),
    keyboardBackgroundColor = Color(0xFFF1F8E9) // Mint green background
)

// Animation definition for the SlimePop theme
private val SlimePopAnimation = KeyAnimation(
    pressScaleX = 1.1f,
    pressScaleY = 0.9f,
    scaleAnimationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
)

// Color definitions for the CozyEmber theme
private val CozyEmberColors = KeyboardColors(
    keyBackgroundColor = Color(0xFF424242), // Charcoal color
    keyBackgroundColorPressed = Color(0xFF616161), // Glowing charcoal color
    keyTextColor = Color(0xFFFF9800), // Burning text orange
    keyboardBackgroundColor = Color(0xFF212121) // Deep charcoal
)

// Animation definition for the CozyEmber theme
private val CozyEmberAnimation = KeyAnimation()

/**
 * Sealed class defining the available keyboard themes in the app.
 * Based on DESIGN.md, each theme has its own set of colors and animations.
 */
sealed class KeyboardTheme(
    val colors: KeyboardColors,
    val animation: KeyAnimation
) {
    object Wood : KeyboardTheme(WoodColors, WoodAnimation)
    object FrozenSerenity : KeyboardTheme(FrozenSerenityColors, FrozenSerenityAnimation)
    object SlimePop : KeyboardTheme(SlimePopColors, SlimePopAnimation)
    object CozyEmber : KeyboardTheme(CozyEmberColors, CozyEmberAnimation)
    // TODO: Add definition for RefreshingFizz theme
}

// CompositionLocal for providing the current KeyboardColors
private val LocalKeyboardColors = staticCompositionLocalOf<KeyboardColors> {
    error("No KeyboardColors provided")
}

// CompositionLocal for providing the current KeyAnimation
private val LocalKeyAnimation = staticCompositionLocalOf<KeyAnimation> {
    error("No KeyAnimation provided")
}

/**
 * Composable that applies a theme (colors, shapes, etc.) to the entire keyboard UI.
 * By wrapping content with this function, inner composables can access the current theme properties
 * via `ASMRKeyboardTheme.colors` and `ASMRKeyboardTheme.animation`.
 */
@Composable
fun ASMRKeyboardTheme(
    theme: KeyboardTheme = KeyboardTheme.Wood, // Set Wood as the default theme
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalKeyboardColors provides theme.colors,
        LocalKeyAnimation provides theme.animation,
        content = content
    )
}

// Globally accessible theme object
// This allows any composable to easily access colors and animations like `ASMRKeyboardTheme.colors`
object ASMRKeyboardTheme {
    val colors: KeyboardColors
        @Composable
        get() = LocalKeyboardColors.current

    val animation: KeyAnimation
        @Composable
        get() = LocalKeyAnimation.current
}
