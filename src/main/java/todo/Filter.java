package todo;

/** 一覧表示の絞り込み条件。 */
public enum Filter {
    ALL, OPEN, DONE;

    public boolean matches(Todo todo) {
        return switch (this) {
            case ALL -> true;
            case OPEN -> !todo.done();
            case DONE -> todo.done();
        };
    }
}
