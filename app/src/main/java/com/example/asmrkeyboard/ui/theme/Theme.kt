package com.example.asmrkeyboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * キーボードのテーマで使用する色のセットを定義するデータクラス。
 * @param keyBackgroundColor キーの背景色
 * @param keyBackgroundColorPressed キーが押された時の背景色
 * @param keyTextColor キーの文字色
 * @param keyboardBackgroundColor キーボード全体の背景色
 */
data class KeyboardColors(
    val keyBackgroundColor: Color,
    val keyBackgroundColorPressed: Color,
    val keyTextColor: Color,
    val keyboardBackgroundColor: Color,
)

// Woodテーマの色定義
private val WoodColors = KeyboardColors(
    keyBackgroundColor = Color(0xFF8D6E63), // 温かみのある木の色
    keyBackgroundColorPressed = Color(0xFF795548), // 少し濃い木の色
    keyTextColor = Color.White,
    keyboardBackgroundColor = Color(0xFF5D4037) // 濃い木目調の背景色
)

// FrozenSerenityテーマの色定義
private val FrozenSerenityColors = KeyboardColors(
    keyBackgroundColor = Color(0x99FFFFFF), // 半透明の氷
    keyBackgroundColorPressed = Color(0xCCFFFFFF), // ヒビが入って白く見える氷
    keyTextColor = Color(0xFF0D47A1), // 深い青
    keyboardBackgroundColor = Color(0xFFE3F2FD) // 青白い氷の壁
)

// SlimePopテーマの色定義
private val SlimePopColors = KeyboardColors(
    keyBackgroundColor = Color(0xFFE0F7FA), // 明るいパステルシアン
    keyBackgroundColorPressed = Color(0xFFB2EBF2), // 少し濃いシアン
    keyTextColor = Color(0xFF006064),
    keyboardBackgroundColor = Color(0xFFF1F8E9) // 背景はミントグリーン
)

// CozyEmberテーマの色定義
private val CozyEmberColors = KeyboardColors(
    keyBackgroundColor = Color(0xFF424242), // 炭の色
    keyBackgroundColorPressed = Color(0xFF616161), // 赤熱した炭の色
    keyTextColor = Color(0xFFFF9800), // 燃える文字のオレンジ
    keyboardBackgroundColor = Color(0xFF212121) // 深いチャコール
)


/**
 * アプリ内で使用可能なキーボードテーマを定義する Sealed Class。
 * DESIGN.md に基づき、各テーマが必要とする色のセットを持つ。
 */
sealed class KeyboardTheme(val colors: KeyboardColors)
{
    object Wood : KeyboardTheme(WoodColors)
    object FrozenSerenity : KeyboardTheme(FrozenSerenityColors)
    object SlimePop : KeyboardTheme(SlimePopColors)
    object CozyEmber : KeyboardTheme(CozyEmberColors)
    // TODO: RefreshingFizzテーマ定義を追加する
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
