# CosmosDB Hour Setting Migration

CosmosDBに保存されているユーザーデータのhour_settingを2時間単位（1-12）から1時間単位（1-24）に変換するSpringBatchアプリケーションです。Azure Functionsで実行され、PATCH APIを使用してRUを抑制します。

## 概要

### 変換ルール
- 1 → [1,2]
- 2 → [3,4] 
- 3 → [5,6]
- 4 → [7,8]
- 5 → [9,10]
- 6 → [11,12]
- 7 → [13,14]
- 8 → [15,16]
- 9 → [17,18]
- 10 → [19,20]
- 11 → [21,22]
- 12 → [23,24]

### データ例
**変換前:**
```json
{
  "userId": "12345",
  "app_setting": {
    "hour_setting": [1,2,3,4,7,9,12]
  }
}
```

**変換後:**
```json
{
  "userId": "12345",
  "app_setting": {
    "hour_setting": [1,2,3,4,5,6,7,8,13,14,17,18,23,24],
    "hour_setting_version": 1
  }
}
```

## アーキテクチャ

- **SpringBatch**: バッチ処理フレームワーク
- **Azure Functions**: サーバーレス実行環境
- **CosmosDB**: データストレージ
- **PATCH API**: RU効率的な更新操作

## 主要コンポーネント

### 1. データモデル (`UserItem.kt`)
CosmosDBのドキュメント構造を表現

### 2. 変換サービス (`HourSettingConverter.kt`)
- `convertHourSetting()`: 2時間単位から1時間単位への変換
- `needsConversion()`: 変換済みかどうかの判定

### 3. CosmosDBサービス (`CosmosDbService.kt`)
- 変換対象アイテムの取得
- PATCH操作による効率的な更新

### 4. バッチ設定 (`BatchConfiguration.kt`)
- **Reader**: 変換が必要なアイテムを読み込み
- **Processor**: 変換済みデータのスキップ
- **Writer**: PATCH APIでの更新（100件ずつ）

### 5. Azure Functions (`MigrationFunction.kt`)
- **タイマートリガー**: 毎日午前2時に自動実行
- **HTTPトリガー**: 手動実行用エンドポイント

## 設定

### 環境変数
```properties
COSMOS_DB_URI=https://your-cosmosdb-account.documents.azure.com:443/
COSMOS_DB_KEY=your-cosmos-db-key
COSMOS_DB_DATABASE=your-database-name
COSMOS_DB_CONTAINER=your-container-name
```

### application.properties
```properties
spring.application.name=patchCosmosDbSample

# CosmosDB Configuration
azure.cosmos.uri=${COSMOS_DB_URI}
azure.cosmos.key=${COSMOS_DB_KEY}
azure.cosmos.database=${COSMOS_DB_DATABASE}
azure.cosmos.container=${COSMOS_DB_CONTAINER}

# Spring Batch Configuration
spring.batch.job.enabled=false
spring.batch.initialize-schema=embedded

# Logging Configuration
logging.level.org.ukky.patchcosmosdbsample=INFO
logging.level.com.azure.cosmos=WARN
logging.level.org.springframework.batch=INFO
```

## ビルドとデプロイ

### 1. プロジェクトビルド
```bash
./gradlew clean build
```

### 2. Azure Functionsデプロイ
```bash
./gradlew azureFunctionsDeploy
```

## 実行方法

### 1. 自動実行（タイマートリガー）
毎日午前2時に自動実行されます。

### 2. 手動実行（HTTPトリガー）
```bash
curl -X POST https://your-function-app.azurewebsites.net/api/hourSettingMigrationManual?code=your-function-key
```

## パフォーマンス特性

- **バッチサイズ**: 100件ずつ処理
- **PATCH操作**: RU消費を最小化
- **スキップ機能**: 変換済みデータは処理しない
- **進捗ログ**: 1000件ごとに進捗を出力

## エラーハンドリング

- アイテム単位でのエラー処理
- 詳細なログ出力
- バッチ処理の継続性確保

## 監視

- SpringBatchの実行ステータス
- 処理件数のログ出力
- エラー詳細の記録

## セキュリティ

- Azure Functions認証レベル: `FUNCTION`
- CosmosDB接続キーの環境変数管理
- HTTPS通信の強制

## 注意事項

1. **大量データ処理**: 100万件のデータに対応
2. **重複実行防止**: `hour_setting_version`による制御
3. **RU制限**: CosmosDBのRU制限に注意
4. **トランザクション**: アイテム単位での更新
