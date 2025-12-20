# Visual Design Guide: Healing ASMR Keyboard

このドキュメントでは、「癒やし」を視覚的に具現化するためのレイヤー構造、デザイン手法、およびアニメーション原則を定義します。

## 1. 視覚的レイヤー構造 (Visual Layering)

各テーマのUIは、以下の4つのレイヤーを重ねて構築します。これにより、奥行きと没入感を演出します。

| レイヤー | 名称 | 役割 | 実装技術 (Compose) |
| :--- | :--- | :--- | :--- |
| **L4** | **Effects** | ヒビ、火の粉、気泡などの動的演出 | `Canvas`, `drawBehind` |
| **L3** | **Key Surface** | キーの質感（氷、木、スライムの表面） | `Glassmorphism`, `Neumorphism` |
| **L2** | **Key Base** | キーの沈み込み、影、立体感 | `graphicsLayer`, `shadow` |
| **L1** | **Background** | テーマ全体の環境（液体、木目、炭など） | `Modifier.blur()`, `Brush` |

---

## 2. 主要デザインスタイル (Core Styles)

テーマに合わせて、以下の3つのスタイルを使い分けます。

### A. Glassmorphism（氷・ソーダ系）
- **特徴:** 背後の透過とボケによる「冷たさ」と「透明感」。
- **実装のコツ:** - `Modifier.blur()` を背景に適用。
    - キーの境界線（Border）に 0.5dp 程度の白いハイライトを入れ、エッジの鋭さを表現。
    - 背景の透過率は 20%〜40% に留め、視認性を確保する。

### B. Neumorphism（スライム・泡系）
- **特徴:** 背景と一体化した「盛り上がり」による「もちもち感」。
- **実装のコツ:**
    - 背景とキーの `Color` を同一にする。
    - 左上に明るい影（Light Shadow）、右下に暗い影（Dark Shadow）を配置。
    - `Shadow` の `blurRadius` を大きく取り、境界を曖昧にすることで柔らかさを出す。

### C. Texture & Organic (木・焚き火系)
- **特徴:** 実在する素材感による「安心感」。
- **実装のコツ:**
    - 写真素材を `Image` として敷き、上に薄くグラデーションを重ねて文字の可読性を上げる。
    - 静止画にせず、背景を 1分間かけて数ピクセルだけゆっくりループ移動（Pan）させると、生きている感覚が出る。

---

## 3. アニメーション原則 (Motion Principles)

機械的な動きを排除し、生命感のあるフィードバックを徹底します。

### Spring over Linear
すべてのプロパティ変化に `spring()` を適用します。
- **Slime/Wood:** `DampingRatioMediumBouncy` (弾力あり)
- **Ice/Fire:** `StiffnessMedium` (キビキビとした動き)

### Squash & Stretch（押し潰しと引き伸ばし）
キーを押した際、体積を維持するように変形させます。
- **実装:** `scaleX` を大きく（1.1）するなら、`scaleY` を小さく（0.9）する。これにより「触感」が生まれます。

### Organic Transitions
- **Color:** `animateColorAsState` を使い、色が「熱を帯びる」「冷める」ような滑らかな遷移を行う。
- **Easing:** 常に `FastOutSlowInEasing` または `LinearOutSlowInEasing` を使い、動きの出だしと終わりに表情をつける。

---

## 4. パーティクル・システム (Dynamic Particle Effects)

瞬間的な快感（氷が割れる、泡がはじける）を演出するためのロジック。

1. **Emitter (発生源):** キーが押された瞬間に、その中心座標 $(x, y)$ から複数のオブジェクトを生成。
2. **Behavior (振る舞い):** 各パーティクルにランダムな初期速度、重力、回転を与える。
3. **Lifespan (寿命):** 0.2秒〜0.5秒で `alpha` (不透明度) を 1.0 から 0.0 へ減少させ、リストから削除する。

---

## 5. カラーパレット（癒やしの選定）

| テーマ | ベースカラー | アクセントカラー |
| :--- | :--- | :--- |
| **Ice** | #E1F5FE (Light Blue 50) | #FFFFFF (White) |
| **Wood** | #3E2723 (Brown 900) | #FFB74D (Orange 300) |
| **Slime** | #F3E5F5 (Purple 50) | #CE93D8 (Purple 200) |
| **Fire** | #212121 (Grey 900) | #FF5722 (Deep Orange) |