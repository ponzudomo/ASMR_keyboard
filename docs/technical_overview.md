# 技術概要

このドキュメントは、ASMR Keyboardプロジェクトの現在までの技術的な実装内容と、リポジトリの構造をまとめたものです。

## 1. 主な修正と実装

### 1.1. 起動時クラッシュ問題の解決

- **問題**: `InputMethodService` 上でJetpack Composeを使用すると、`ViewTreeLifecycleOwner not found`という`IllegalStateException`が発生し、キーボードがクラッシュしていました。
- **原因**: `InputMethodService`は`Activity`とは異なる独自のライフサイクルを持つため、Composeが自身のライフサイクルを管理するための`LifecycleOwner`を自動的に見つけられませんでした。
- **解決策**:
    1. ライフサイクル関連のすべての責務（`LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner`）を担う`ServiceLifecycleOwner`というヘルパークラスを`AsmrKeyboardService.kt`内に作成しました。
    2. `AsmrKeyboardService`のライフサイクルコールバック（`onCreate`, `onWindowShown`など）が呼ばれるたびに、この`ServiceLifecycleOwner`のイベントを更新するようにしました。
    3. `onCreateInputView`内で、ComposeViewが作成される前に、サービスのウィンドウのルートである`decorView`に対して`ServiceLifecycleOwner`を明示的に設定しました。これにより、Compose Viewツリーが`LifecycleOwner`を正しく見つけられるようになり、クラッシュが解消されました。

### 1.2. UIとアニメーションの実装

`DESIGN.md`で定義された「Organic Motion（有機的な動き）」の原則に基づき、キーのインタラクションを段階的に改良しました。

- **初期実装**: `detectTapGestures`と`animateDpAsState`を用いて、キー押下時に影が変化する基本的なアニメーションを実装しました。
- **アニメーションの質の向上**: 
    - 状態管理をより堅牢にするため、`MutableInteractionSource`と`collectIsPressedAsState`を使用する方式にリファクタリングしました。
    - アニメーションに`spring`エフェクトを適用し、物理的なボタンのような弾力のある動きを実現しました。
- **テーマ別アニメーションの実装**: 
    - `SlimePop`テーマのために、`Modifier.graphicsLayer`を用いてキーが「ぐにゃっ」と変形するアニメーション（`scaleX`, `scaleY`）を追加しました。

### 1.3. テーマ設計のリファクタリング

テーマが増えるごとに`KeyButton`コンポーザブルが複雑化するのを防ぐため、テーマの設計をリファクタリングしました。

- **問題**: 当初、`KeyButton`内に`if (currentTheme is KeyboardTheme.SlimePop)`のような分岐ロジックが存在し、拡張性に乏しい状態でした。
- **解決策**:
    1. アニメーション設定を保持する`KeyAnimation`データクラスを`Theme.kt`に定義しました。
    2. `KeyboardTheme`シールクラスを拡張し、各テーマオブジェクトが`colors`（色設定）に加えて`animation`（アニメーション設定）も保持するようにしました。
    3. `ASMRKeyboardTheme`コンポーザブルを更新し、`CompositionLocalProvider`を通じて、現在のテーマの色とアニメーション設定をコンポジション全体に提供するようにしました。
    4. `KeyButton`からテーマ分岐ロジックを完全に削除し、代わりに`ASMRKeyboardTheme.animation`から現在の設定を読み込むように簡潔化しました。

この変更により、テーマの追加や変更は`Theme.kt`の修正だけで完結するようになり、保守性と拡張性が大幅に向上しました。

## 2. 現在のリポジトリ構造

プロジェクトの主要なファイルとディレクトリの構成は以下の通りです。

```
ASMRkeyboard
├── app
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com/example/asmrkeyboard
│   │       │       ├── AsmrKeyboardService.kt  # IMEのメインサービス。UIと主要ロジックを格納
│   │       │       └── ui/theme
│   │       │           └── Theme.kt            # キーボードのテーマ定義（色、アニメーション）
│   │       └── res
│   │           └── raw/                      # 音源ファイル（.ogg, .wav）の格納場所
│   └── build.gradle.kts                      # アプリケーションのビルド設定
├── docs
│   ├── design/                             # デザイン関連の詳細ドキュメント
│   ├── technical_overview.md               # このファイル。技術的な実装概要
│   └── ...
├── DESIGN.md                               # プロジェクトのコアとなるデザイン原則とテーマ仕様
└── GEMINI.md                               # AIアシスタント（あなた）への指示書
```
