package todo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JDK 標準の HttpServer を使った REST API。
 *
 * <pre>
 * GET    /api/todos?filter=all|open|done  一覧
 * POST   /api/todos             {"title"}  追加
 * PATCH  /api/todos/{id}  {"title"?, "done"?} 更新
 * DELETE /api/todos/{id}                    削除
 * POST   /api/todos/clear-done              完了済みを一括削除
 * </pre>
 *
 * Executor を指定していないため、リクエストは1スレッドで順番に処理される。
 * そのおかげでスレッドセーフでない TodoList をそのまま使える。
 */
public class ApiServer {

    /** ハンドラの戻り値。HTTP ステータスと JSON 本文の組。 */
    private record Response(int status, String body) {}

    private final TodoList todos;
    private final HttpServer server;

    public ApiServer(TodoList todos, int port) throws IOException {
        this.todos = todos;
        // localhost のみで待ち受け、外部のマシンからはアクセスさせない
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/api/todos", this::handle);
    }

    public void start() {
        server.start();
        System.out.println("API サーバーを起動しました: http://localhost:" + server.getAddress().getPort() + "/api/todos");
        System.out.println("停止するには Ctrl+C");
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Response response;
            try {
                response = route(exchange);
            } catch (TodoNotFoundException e) {
                response = new Response(404, Json.error(e.getMessage()));
            } catch (IllegalArgumentException e) {
                response = new Response(400, Json.error(e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                response = new Response(500, Json.error("サーバー内部でエラーが発生しました"));
            }
            send(exchange, response);
        }
    }

    private Response route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        // "/api/todos/3" → "3"、"/api/todos" → ""
        String rest = exchange.getRequestURI().getPath().replaceFirst("^/api/todos/?", "");

        if (rest.isEmpty()) {
            return switch (method) {
                case "GET" -> new Response(200, Json.of(todos.list(parseFilter(exchange.getRequestURI().getQuery()))));
                case "POST" -> {
                    Map<String, Object> body = readBody(exchange);
                    yield new Response(201, Json.of(todos.add(requireString(body, "title"))));
                }
                default -> methodNotAllowed();
            };
        }
        if (rest.equals("clear-done")) {
            if (!method.equals("POST")) {
                return methodNotAllowed();
            }
            return new Response(200, "{\"removed\":" + todos.clearDone() + "}");
        }
        if (rest.matches("\\d+")) {
            int id = Integer.parseInt(rest);
            return switch (method) {
                case "PATCH" -> new Response(200, Json.of(update(id, readBody(exchange))));
                case "DELETE" -> new Response(200, Json.of(todos.delete(id)));
                default -> methodNotAllowed();
            };
        }
        return new Response(404, Json.error("見つかりません: " + exchange.getRequestURI().getPath()));
    }

    private Todo update(int id, Map<String, Object> body) throws IOException {
        Object title = body.get("title");
        Object done = body.get("done");
        if (title == null && done == null) {
            throw new IllegalArgumentException("title か done のどちらかを指定してください");
        }
        // 型チェックを先に済ませ、途中まで更新されて失敗する状態を避ける
        if (title != null && !(title instanceof String)) {
            throw new IllegalArgumentException("title は文字列で指定してください");
        }
        if (done != null && !(done instanceof Boolean)) {
            throw new IllegalArgumentException("done は true / false で指定してください");
        }

        Todo result = null;
        if (title instanceof String s) {
            result = todos.rename(id, s);
        }
        if (done instanceof Boolean b) {
            result = todos.setDone(id, b);
        }
        return result;
    }

    private static Filter parseFilter(String query) {
        if (query == null) {
            return Filter.ALL;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("filter=")) {
                String value = param.substring("filter=".length());
                try {
                    return Filter.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("filter は all / open / done のいずれかです");
                }
            }
        }
        return Filter.ALL;
    }

    private static Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return Json.parseObject(body);
    }

    private static String requireString(Map<String, Object> body, String key) {
        if (body.get(key) instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(key + " を文字列で指定してください");
    }

    private static Response methodNotAllowed() {
        return new Response(405, Json.error("このメソッドは使えません"));
    }

    private static void send(HttpExchange exchange, Response response) throws IOException {
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
