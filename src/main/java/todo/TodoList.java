package todo;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO の追加・更新・削除といった操作をまとめたクラス。
 * 変更のたびにリポジトリへ保存する。
 */
public class TodoList {

    private final TodoRepository repository;
    private final List<Todo> todos;

    public TodoList(TodoRepository repository) throws IOException {
        this.repository = repository;
        this.todos = new ArrayList<>(repository.load());
    }

    public Todo add(String title) throws IOException {
        int nextId = todos.stream().mapToInt(Todo::id).max().orElse(0) + 1;
        Todo todo = new Todo(nextId, title, false, LocalDate.now());
        todos.add(todo);
        repository.save(todos);
        return todo;
    }

    public List<Todo> list(Filter filter) {
        return todos.stream().filter(filter::matches).toList();
    }

    public Todo setDone(int id, boolean done) throws IOException {
        return replace(id, find(id).withDone(done));
    }

    public Todo rename(int id, String title) throws IOException {
        return replace(id, find(id).withTitle(title));
    }

    public Todo delete(int id) throws IOException {
        Todo todo = find(id);
        todos.remove(todo);
        repository.save(todos);
        return todo;
    }

    /** 完了済みの TODO をまとめて削除し、削除した件数を返す。 */
    public int clearDone() throws IOException {
        int before = todos.size();
        todos.removeIf(Todo::done);
        int removed = before - todos.size();
        if (removed > 0) {
            repository.save(todos);
        }
        return removed;
    }

    private Todo find(int id) {
        return todos.stream()
                .filter(t -> t.id() == id)
                .findFirst()
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    private Todo replace(int id, Todo updated) throws IOException {
        todos.set(todos.indexOf(find(id)), updated);
        repository.save(todos);
        return updated;
    }
}
