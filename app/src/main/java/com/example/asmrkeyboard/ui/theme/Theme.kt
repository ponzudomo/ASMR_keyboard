package com.example.asmrkeyboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * キーボードのテーマで使用する色のセットを定義するデータクラス。
 * @param keyBackgroundColor キーの背景色
 * @param keyTextColor キーの文字色
 * @param keyboardBackgroundColor キーボード全体の背景色
 */
data class KeyboardColors(
    val keyBackgroundColor: Color,
    val keyTextColor: Color,
    val keyboardBackgroundColor: Color,
)

// Woodテーマの色定義
private val WoodColors = KeyboardColors(
    keyBackgroundColor = Color(0xFF8D6E63), // 温かみのある木の色
    keyTextColor = Color.White,
    keyboardBackgroundColor = Color(0xFF5D4037) // 濃い木目調の背景色
)

/**
 * アプリ内で使用可能なキーボードテーマを定義する Sealed Class。
 * DESIGN.md に基づき、各テーマが必要とする色のセットを持つ。
 */
sealed class KeyboardTheme(val colors: KeyboardColors)
{
    object Wood : KeyboardTheme(WoodColors)
    // TODO: FrozenSerenity, RefreshingFizz, SlimePop, CozyEmber のテーマ定義を追加する
}

// 現在の KeyboardColors を提供するための CompositionLocal
private val LocalKeyboardColors = staticCompositionLocalOf<KeyboardColors> {
    // デフォルト値としてエラーを投げることで、テーマが設定されていない場合に気づけるようにする
    error("No KeyboardColors provided")
}

/**
 * キーボードUI全体にテーマ（色、形など）を適用するためのComposable。
 * この関数で囲むことで、内部のComposableは `KeyboardTheme.colors` で現在のテーマ色にアクセスできる。
 */
@Composable
fun ASMRKeyboardTheme(
    theme: KeyboardTheme = KeyboardTheme.Wood, // デフォルトテーマをWoodに設定
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalKeyboardColors provides theme.colors,
        content = content
    )
}

// グローバルにアクセス可能なテーマオブジェクト
// これにより、どのComposableからでも `KeyboardTheme.colors` のように簡単に色を呼び出せる
object ASMRKeyboardTheme {
    val colors: KeyboardColors
        @Composable
        get() = LocalKeyboardColors.current
}
