package todo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * エントリポイント。
 * 引数なしで起動すると対話モード、引数ありならそのコマンドを1回だけ実行する。
 *   例) java todo.Main add 牛乳を買う
 * 引数が serve なら Web 用の API サーバーとして起動する。
 */
public class Main {

    private static final String HELP = """
            コマンド一覧:
              add <タイトル>          TODO を追加            (a)
              list [all|open|done]    一覧を表示             (ls, l)
              done <ID>               完了にする             (d)
              undo <ID>               未完了に戻す           (u)
              edit <ID> <タイトル>    タイトルを変更         (e)
              delete <ID>             削除する               (del, rm)
              clear                   完了済みをまとめて削除
              help                    このヘルプを表示       (h, ?)
              exit                    終了する               (quit, q)""";

    private final TodoList todos;
    private final PrintStream out;

    Main(TodoList todos, PrintStream out) {
        this.todos = todos;
        this.out = out;
    }

    public static void main(String[] args) {
        // 保存先は環境変数 TODO_FILE で変更できる（未指定ならカレントディレクトリの todos.tsv）
        Path file = Path.of(System.getenv().getOrDefault("TODO_FILE", "todos.tsv"));
        try {
            TodoList todoList = new TodoList(new TodoRepository(file));
            if (args.length > 0 && args[0].equals("serve")) {
                // Web フロントエンド向けの API サーバーとして起動（ポートは環境変数 PORT で変更可）
                int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
                new ApiServer(todoList, port).start();
                return;
            }
            Main app = new Main(todoList, System.out);
            if (args.length > 0) {
                app.runOnce(String.join(" ", args));
            } else {
                app.repl(new Scanner(System.in));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("エラー: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ファイルの読み書きに失敗しました: " + e.getMessage());
            System.exit(1);
        }
    }

    private void runOnce(String line) throws IOException {
        execute(Command.parse(line));
    }

    private void repl(Scanner in) throws IOException {
        out.println("TODO アプリ（help でコマンド一覧、exit で終了）");
        while (true) {
            out.print("todo> ");
            if (!in.hasNextLine()) {
                break; // Ctrl+D
            }
            String line = in.nextLine();
            if (line.isBlank()) {
                continue;
            }
            try {
                if (!execute(Command.parse(line))) {
                    break;
                }
            } catch (IllegalArgumentException e) {
                // 入力ミスでアプリを終了させず、メッセージだけ出して続行する
                out.println("エラー: " + e.getMessage());
            }
        }
        out.println("終了します");
    }

    /** コマンドを実行する。続行するなら true、終了するなら false を返す。 */
    private boolean execute(Command command) throws IOException {
        // sealed interface + record パターンの switch（Java 21）。
        // 全ケースを網羅していないとコンパイルエラーになるので、default は不要。
        switch (command) {
            case Command.Add(String title) -> out.println("追加しました: " + format(todos.add(title)));
            case Command.ListTodos(Filter filter) -> printList(todos.list(filter));
            case Command.Done(int id) -> out.println("完了にしました: " + format(todos.setDone(id, true)));
            case Command.Undo(int id) -> out.println("未完了に戻しました: " + format(todos.setDone(id, false)));
            case Command.Edit(int id, String title) -> out.println("変更しました: " + format(todos.rename(id, title)));
            case Command.Delete(int id) -> out.println("削除しました: " + format(todos.delete(id)));
            case Command.Clear() -> out.println("完了済みを " + todos.clearDone() + " 件削除しました");
            case Command.Help() -> out.println(HELP);
            case Command.Exit() -> {
                return false;
            }
        }
        return true;
    }

    private void printList(List<Todo> list) {
        if (list.isEmpty()) {
            out.println("TODO はありません");
            return;
        }
        list.forEach(t -> out.println(format(t)));
        long open = list.stream().filter(t -> !t.done()).count();
        out.printf("-- %d 件（未完了 %d / 完了 %d）%n", list.size(), open, list.size() - open);
    }

    private static String format(Todo t) {
        return "%3d. [%s] %s  (%s)".formatted(t.id(), t.done() ? "x" : " ", t.title(), t.createdAt());
    }
}
