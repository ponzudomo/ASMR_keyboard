# ビジュアルガイド (Visual Guide)

このドキュメントでは、「癒やし」を視覚的に具現化するためのレイヤー構造、デザイン手法、およびアニメーション原則を定義します。

## 1. 視覚的レイヤー構造 (Visual Layering)

没入感のある「立体的な」体験を作るため、UIを以下の5つのレイヤーで構成します。これらは `parallax.md` で定義される視差効果の制御対象となります。

| レイヤー | 名称 | 役割 | 実装技術 (Compose) |
| :--- | :--- | :--- | :--- |
| **L5** | **Overlay** | 画面全体のフィルター、周辺減光、火の粉（前面） | `Canvas`, `Brush` |
| **L4** | **Key Effects** | 押下時のヒビ、気泡、パーティクル | `drawBehind`, `ParticleSystem` |
| **L3** | **Key Surface** | キーの質感（氷、木、スライムの表面）、文字 | `Glassmorphism`, `Neumorphism` |
| **L2** | **Key Base** | キーの沈み込み、動的な影 | `graphicsLayer`, `shadow` |
| **L1** | **Background** | テーマ環境（液体、木目、炭、火影） | `Modifier.blur()`, `Image` |

---

## 2. デザインスタイル

### A. Glassmorphism (氷・ソーダ)
- **特徴:** 背後の透過とボケ。
- **実装:** `Modifier.blur()` + 境界線の 0.5dp ハイライト。

### B. Neumorphism (スライム・泡)
- **特徴:** 背景と一体化した柔らかい盛り上がり。
- **実装:** 背景と同色のキー + 左上のハイライト影と右下のディープ影。

### C. Texture & Organic (木・焚き火)
- **特徴:** 素材感と微細な動き。
- **実装:** 高解像度テクスチャ + 非常にゆっくりとした背景のパンニング（Pan）。

---

## 3. アニメーション原則 (Motion Principles)

### Spring (バネ)
機械的な Linear を排除し、すべてに `spring()` を適用。
- **Slime/Wood:** `DampingRatioMediumBouncy`
- **Ice/Fire:** `StiffnessMedium`

### Squash & Stretch (押し潰しと引き伸ばし)
- **実装:** 押下時に `scaleX: 1.1`, `scaleY: 0.9` のように体積を維持した変形。

### Organic Color Transitions
- `animateColorAsState` を用い、色が「熱を帯びる」「凍りつく」ような質感を伴う遷移。
