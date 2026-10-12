package todo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO をファイルに保存・読み込みする。
 * 形式は 1行1件の TSV: {@code id<TAB>完了(0/1)<TAB>作成日<TAB>タイトル}
 */
public class TodoRepository {

    private final Path file;

    public TodoRepository(Path file) {
        this.file = file;
    }

    public List<Todo> load() throws IOException {
        List<Todo> todos = new ArrayList<>();
        if (!Files.exists(file)) {
            return todos;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split("\t", 4);
            if (cols.length != 4) {
                throw new IOException(file + " の " + (i + 1) + " 行目の形式が不正です");
            }
            todos.add(new Todo(
                    Integer.parseInt(cols[0]),
                    cols[3],
                    cols[1].equals("1"),
                    LocalDate.parse(cols[2])));
        }
        return todos;
    }

    public void save(List<Todo> todos) throws IOException {
        List<String> lines = todos.stream()
                .map(t -> t.id() + "\t" + (t.done() ? "1" : "0") + "\t" + t.createdAt() + "\t" + t.title())
                .toList();

        // 一時ファイルに書いてから置き換えることで、書き込み途中で落ちてもデータが壊れない
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "todos", ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
