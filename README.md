# TODO アプリ

Java 21 のバックエンド（外部ライブラリ不要）と Next.js のフロントエンドで作った TODO アプリです。
コマンドラインからも Web 画面からも操作でき、データは共通です。

## Web 版の起動

ターミナルを2つ使います。

```sh
# 1. Java の API サーバー（http://localhost:8080）
./run.sh serve

# 2. Next.js（http://localhost:3000）
cd frontend
npm install   # 初回のみ
npm run dev
```

ブラウザで http://localhost:3000 を開きます。
Next.js が `/api/*` を Java サーバーへ転送するので（`frontend/next.config.ts` の `rewrites`）、CORS 設定は不要です。
API の転送先は環境変数 `API_URL`、Java 側のポートは `PORT` で変更できます。

### API

| メソッド | パス | 本文 | 説明 |
|---|---|---|---|
| GET | `/api/todos?filter=all\|open\|done` | | 一覧 |
| POST | `/api/todos` | `{"title": "..."}` | 追加 |
| PATCH | `/api/todos/{id}` | `{"title"?: "...", "done"?: true}` | 更新 |
| DELETE | `/api/todos/{id}` | | 削除 |
| POST | `/api/todos/clear-done` | | 完了済みを一括削除 |

## コマンドライン版の実行

```sh
./run.sh            # 対話モード
./run.sh add 牛乳を買う   # コマンドを1回だけ実行
./run.sh list open
```

データはカレントディレクトリの `todos.tsv` に保存されます（環境変数 `TODO_FILE` で変更可）。

## コマンド

| コマンド | 説明 |
|---|---|
| `add <タイトル>` | 追加 |
| `list [all\|open\|done]` | 一覧 |
| `done <ID>` / `undo <ID>` | 完了 / 未完了に戻す |
| `edit <ID> <タイトル>` | タイトル変更 |
| `delete <ID>` | 削除 |
| `clear` | 完了済みを一括削除 |
| `help` / `exit` | ヘルプ / 終了 |

## 構成

### バックエンド（`src/main/java/todo/`）

| ファイル | 役割 |
|---|---|
| `Main.java` | 起動処理・コマンドライン版の入出力 |
| `ApiServer.java` | REST API（JDK 標準の `HttpServer`） |
| `Json.java` | 最小限の JSON 変換 |
| `Command.java` | 入力文字列 → コマンドへの変換（sealed interface + record） |
| `TodoList.java` | TODO の操作ロジック |
| `TodoRepository.java` | ファイルへの保存・読み込み |
| `Todo.java` / `Filter.java` / `TodoNotFoundException.java` | データ型・例外 |

### フロントエンド（`frontend/src/`）

| ファイル | 役割 |
|---|---|
| `app/page.tsx` | トップページ（Server Component） |
| `components/TodoApp.tsx` | 画面全体の状態管理（Client Component） |
| `components/TodoItem.tsx` | TODO 1件の表示・編集 |
| `lib/api.ts` | API 呼び出し |
