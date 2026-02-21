# Lifecycle 管理 (Lifecycle Management)

`InputMethodService` (IME) 上で Jetpack Compose を安定して動作させるためのライフサイクル管理手法について記述します。

## 1. 直面した課題
ComposeView を `onCreateInputView` で返却する際、標準的な実装では以下のエラーが発生し、キーボードがクラッシュします。
`java.lang.IllegalStateException: ViewTreeLifecycleOwner not found`

これは、`InputMethodService` が `Activity` ではないため、Compose が自身の状態（Recompositionなど）を管理するために必要な `LifecycleOwner` を自動で見つけられないことが原因です。

## 2. 解決策: ServiceLifecycleOwner
サービス専用のライフサイクルプロバイダーを実装しました。

### 実装のポイント
- **インターフェースの統合:** `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner` を一つのクラスで実装。
- **手動イベント通知:** サービスのコールバックに合わせて、ライフサイクルイベントを明示的に送出。
  - `onCreate` -> `ON_CREATE`
  - `onWindowShown` -> `ON_RESUME`
  - `onWindowHidden` -> `ON_PAUSE`
  - `onDestroy` -> `ON_DESTROY`
- **DecorView への注入:** 
  ```kotlin
  val decorView = window.window!!.decorView
  decorView.setViewTreeLifecycleOwner(lifecycleOwner)
  decorView.setViewTreeViewModelStoreOwner(lifecycleOwner)
  decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
  ```
  `ComposeView` を作成する前に、ウィンドウのルートである `DecorView` にこれらを設定することで、View ツリー内のすべての Compose コンポーネントがライフサイクルを参照可能になります。

## 3. 注意事項
- **メモリリーク防止:** `onDestroy` で `ViewModelStore.clear()` を呼び出し、リソースを確実に解放してください。
- **SavedState:** 現在は `performRestore(null)` としていますが、将来的に IME の状態保存が必要な場合はここを拡張します。
