# ASMR Keyboard Documentation

このディレクトリには、プロジェクトのデザイン設計、技術仕様、および開発記録が格納されています。

## 📂 リポジトリ構造 (docs/)

```text
docs/
├── README.md               # [Index] 全ドキュメントのポータル
├── roadmap.md              # [Project] 開発ロードマップ・進捗管理
│
├── design/                 # [Design] 設計思想・ビジュアル・体験設計
│   ├── concept.md          # 全体コンセプト・ビジョン
│   ├── general.md          # 共通デザインガイド（レイヤー、スタイル、アニメ）
│   ├── parallax.md         # 視差効果（Parallax）の詳細設計
│   ├── assets.md           # 素材（音源・画像）の仕様・管理
│   └── themes/             # 各テーマ別の個別詳細設計
│       ├── frozen_serenity.md
│       ├── wood.md
│       ├── slime_pop.md
│       └── cozy_ember.md
│
├── technical/              # [Technical] 技術仕様・実装解説
│   ├── overview.md         # 技術スタックと実装の全体像
│   ├── architecture.md     # アーキテクチャ詳細 (IME, Compose, ThemeSystem)
│   └── lifecycle.md        # ServiceでのLifecycle管理・トラブルシューティング
│
└── research/               # [Research] 技術調査・プロトタイプメモ
    └── sensor_parallax.md  # センサー利用に関する調査メモ
```

---

## 📖 クイックリンク

### 🎨 デザインを理解する
- [全体コンセプト](design/concept.md)
- [デザインガイドライン (general.md)](design/general.md)
- [視差効果の設計](design/parallax.md)

### 🛠 技術仕様を確認する
- [技術概要とこれまでの修正](technical/overview.md)
- [ロードマップ](roadmap.md)

---

## 🚀 開発の優先順位
1. **Core:** 入力機能の完備（Shift, Backspace, Spaceなど）。
2. **ASMR:** SoundPoolによる低遅延な打鍵音の実装。
3. **Visual:** テーマごとのユニークなアニメーション。
4. **Experience:** 視差効果（Parallax）の実装。
