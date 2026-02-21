# 技術概要 (Technical Overview)

このドキュメントは、ASMR Keyboard プロジェクトの技術スタック、これまでの主要な修正、および実装の全体像をまとめたものです。

## 1. 主要な技術スタック

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (InputMethodService 上で動作)
- **Audio Engine:** SoundPool (低遅延再生用)
- **Lifecycle Management:** カスタム ServiceLifecycleOwner

---

## 2. これまでの重要な修正記録

### 2.1. LifecycleOwner の解決
`InputMethodService` は標準の `Activity` ライフサイクルを持たないため、Compose が動作するために必要な `LifecycleOwner` を手動で提供する実装を行いました。
- **解決策:** `ServiceLifecycleOwner` クラスを導入し、サービスの `Window` の `DecorView` に紐付けることで、Compose View ツリーが正しくライフサイクルを解決できるようになりました。

### 2.2. テーマシステムのリファクタリング
初期の実装では `KeyButton` 内にテーマごとの分岐がありましたが、拡張性を高めるために `KeyboardTheme` シールクラスに `Colors` と `Animation` の両方をカプセル化しました。
- **成果:** `KeyButton` は現在のテーマが何であるかを意識せず、提供された設定に従って描画されるだけの疎結合な設計になりました。

---

## 3. 実装の全体像

### 3.1. IME サービス (`AsmrKeyboardService`)
キーボードの本体となる `InputMethodService`。`onCreateInputView` で `ComposeView` を返却し、ライフサイクルイベントを適切に中継します。

### 3.2. テーマ定義 (`Theme.kt`)
`CompositionLocal` を用いて、色 (`KeyboardColors`) とアニメーション定数 (`KeyAnimation`) を UI ツリー全体に伝播させます。
- **Animation:** `spring` を多用し、有機的な動きを実現。
- **Interactions:** `MutableInteractionSource` を活用したプレス状態の管理。
