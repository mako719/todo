package todo;

/**
 * ユーザーが入力するコマンド。
 * sealed なので実装はここに列挙したものだけに限定され、switch で網羅性をコンパイラがチェックできる。
 */
public sealed interface Command {

    record Add(String title) implements Command {}
    record ListTodos(Filter filter) implements Command {}
    record Done(int id) implements Command {}
    record Undo(int id) implements Command {}
    record Edit(int id, String title) implements Command {}
    record Delete(int id) implements Command {}
    record Clear() implements Command {}
    record Help() implements Command {}
    record Exit() implements Command {}

    /** 1行の入力文字列をコマンドに変換する。不正な入力は IllegalArgumentException。 */
    static Command parse(String input) {
        String[] parts = input.strip().split("\\s+", 2);
        String name = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].strip() : "";

        return switch (name) {
            case "add", "a" -> new Add(requireText(arg, "add <タイトル>"));
            case "list", "ls", "l" -> new ListTodos(parseFilter(arg));
            case "done", "d" -> new Done(parseId(arg));
            case "undo", "u" -> new Undo(parseId(arg));
            case "edit", "e" -> {
                String[] idAndTitle = arg.split("\\s+", 2);
                if (idAndTitle.length < 2) {
                    throw new IllegalArgumentException("使い方: edit <ID> <新しいタイトル>");
                }
                yield new Edit(parseId(idAndTitle[0]), idAndTitle[1]);
            }
            case "delete", "del", "rm" -> new Delete(parseId(arg));
            case "clear" -> new Clear();
            case "help", "h", "?" -> new Help();
            case "exit", "quit", "q" -> new Exit();
            default -> throw new IllegalArgumentException("不明なコマンドです: " + name + "（help で一覧を表示）");
        };
    }

    private static String requireText(String arg, String usage) {
        if (arg.isEmpty()) {
            throw new IllegalArgumentException("使い方: " + usage);
        }
        return arg;
    }

    private static int parseId(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID は数字で指定してください: '" + arg + "'");
        }
    }

    private static Filter parseFilter(String arg) {
        return switch (arg.toLowerCase()) {
            case "" , "all" -> Filter.ALL;
            case "open", "todo" -> Filter.OPEN;
            case "done" -> Filter.DONE;
            default -> throw new IllegalArgumentException("list の引数は all / open / done のいずれかです");
        };
    }
}
