package todo;

import java.time.LocalDate;

/**
 * TODO 1件分のデータ。record なので不変（immutable）で、変更時は新しいインスタンスを作る。
 */
public record Todo(int id, String title, boolean done, LocalDate createdAt) {

    /** コンパクトコンストラクタ: 生成時に必ず検証・正規化が走る。 */
    public Todo {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("タイトルが空です");
        }
        // 保存形式（TSV）を壊さないよう、タブや改行は空白に置き換える
        title = title.replaceAll("[\\t\\r\\n]+", " ").strip();
    }

    public Todo withDone(boolean done) {
        return new Todo(id, title, done, createdAt);
    }

    public Todo withTitle(String title) {
        return new Todo(id, title, done, createdAt);
    }
}
