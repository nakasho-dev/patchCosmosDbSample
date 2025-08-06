# CosmosDB Hour Setting Migration - Local Development

このプロジェクトは、CosmosDBに保存されているユーザーデータのhour_settingを2時間単位（1-12）から1時間単位（1-24）に変換するSpring Batchアプリケーションです。

## データ変換仕様

### 変換前（2時間単位: 1-12）
```json
{
  "userId": "12345",
  "app_setting": {
    "hour_setting": [1, 2, 3, 4, 7, 9, 12]
  }
}
```

### 変換後（1時間単位: 1-24）
```json
{
  "userId": "12345",
  "app_setting": {
    "hour_setting": [1, 2, 3, 4, 5, 6, 7, 8, 13, 14, 17, 18, 23, 24],
    "hour_setting_version": 1
  }
}
```

### 変換ルール
- 1 → [1, 2]
- 2 → [3, 4]
- 3 → [5, 6]
- 4 → [7, 8]
- 5 → [9, 10]
- 6 → [11, 12]
- 7 → [13, 14]
- 8 → [15, 16]
- 9 → [17, 18]
- 10 → [19, 20]
- 11 → [21, 22]
- 12 → [23, 24]

## ローカル実行方法

### 1. CosmosDB Emulatorを使用する場合

1. Azure CosmosDB Emulatorをインストール・起動
2. プロジェクトを起動
```bash
./start-local.sh
```

### 2. 実際のCosmosDBを使用する場合

1. 環境変数を設定
```bash
export COSMOS_DB_URI=your-cosmos-uri
export COSMOS_DB_KEY=your-cosmos-key
export COSMOS_DB_DATABASE=your-database
export COSMOS_DB_CONTAINER=your-container
```

2. プロジェクトを起動
```bash
./start-local.sh
```

### 3. Gradleコマンドで直接実行
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## API エンドポイント

アプリケーション起動後、以下のエンドポイントが利用可能です：

### バッチジョブの開始
```bash
curl -X POST http://localhost:8080/api/batch/migrate-hour-settings
```

レスポンス例：
```json
{
  "jobId": 1,
  "status": "STARTED",
  "startTime": "2025-08-07T10:30:00",
  "message": "Migration job started successfully"
}
```

### ジョブステータスの確認
```bash
curl http://localhost:8080/api/batch/status/1
```

レスポンス例：
```json
{
  "jobId": 1,
  "jobName": "hourSettingMigrationJob",
  "status": "COMPLETED",
  "exitStatus": "COMPLETED",
  "startTime": "2025-08-07T10:30:00",
  "endTime": "2025-08-07T10:35:00",
  "stepExecutions": [
    {
      "stepName": "migrationStep",
      "status": "COMPLETED",
      "readCount": 1000000,
      "writeCount": 800000,
      "commitCount": 10000,
      "rollbackCount": 0,
      "skipCount": 200000
    }
  ]
}
```

### 全ジョブの一覧
```bash
curl http://localhost:8080/api/batch/jobs
```

## プロジェクト構成

```
src/main/kotlin/org/ukky/patchcosmosdbsample/
├── PatchCosmosDbSampleApplication.kt    # メインアプリケーション
├── batch/
│   └── BatchConfiguration.kt            # Spring Batch設定
├── config/
│   └── CosmosConfig.kt                  # CosmosDB設定
├── controller/
│   └── BatchController.kt               # REST API
├── model/
│   └── UserItem.kt                      # データモデル
└── service/
    ├── CosmosDbService.kt               # CosmosDB操作
    └── HourSettingConverter.kt          # 変換ロジック
```

## ログ確認

アプリケーションログでバッチ処理の進行状況を確認できます：

```
2025-08-07 10:30:00 [main] INFO  o.u.p.controller.BatchController - Starting hour setting migration job...
2025-08-07 10:30:01 [main] INFO  o.u.p.batch.CosmosItemReader - Found 1000000 items needing conversion
2025-08-07 10:31:00 [main] INFO  o.u.p.batch.CosmosItemWriter - Processed 100000 items
2025-08-07 10:32:00 [main] INFO  o.u.p.batch.CosmosItemWriter - Processed 200000 items
...
```

## トラブルシューティング

### CosmosDB接続エラー
- CosmosDB Emulatorが起動していることを確認
- 実際のCosmosDBを使用する場合、接続情報が正しいことを確認

### バッチジョブが開始しない
- Spring Batchのジョブリポジトリが正しく初期化されていることを確認
- ログでエラーメッセージを確認

### パフォーマンスの調整
- `BatchConfiguration.kt`のchunkサイズ（現在100）を調整
- CosmosDBのRU設定を確認
