package todo;

/** 指定した ID の TODO が存在しないときの例外。API ではこれを 404 として返す。 */
public class TodoNotFoundException extends IllegalArgumentException {

    public TodoNotFoundException(int id) {
        super("ID " + id + " の TODO は見つかりません");
    }
}
