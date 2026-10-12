package todo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 用の最小限の JSON 変換。
 * 外部ライブラリ（Jackson など）を使わないため、このアプリで必要な範囲だけを自前で実装している。
 * 読み込みは「値が文字列・数値・真偽値・null のフラットなオブジェクト」のみ対応。
 */
public final class Json {

    private Json() {}

    // ---- 書き出し ----

    public static String of(Todo t) {
        return "{\"id\":%d,\"title\":%s,\"done\":%b,\"createdAt\":%s}"
                .formatted(t.id(), quote(t.title()), t.done(), quote(t.createdAt().toString()));
    }

    public static String of(List<Todo> todos) {
        return todos.stream().map(Json::of).collect(Collectors.joining(",", "[", "]"));
    }

    public static String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u%04x".formatted((int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    // ---- 読み込み ----

    public static Map<String, Object> parseObject(String json) {
        Parser p = new Parser(json);
        Map<String, Object> result = p.object();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw p.error("余分な文字があります");
        }
        return result;
    }

    /** 再帰下降パーサ。pos を進めながら 1文字ずつ読む。 */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, value());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("',' か '}' が必要です");
                }
            }
        }

        Object value() {
            char c = peek();
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", pos)) {
                pos += 4;
                return true;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            if (c == '-' || Character.isDigit(c)) {
                int start = pos;
                while (!atEnd() && "+-.eE0123456789".indexOf(peek()) >= 0) {
                    pos++;
                }
                return Double.parseDouble(s.substring(start, pos));
            }
            throw error("対応していない値です");
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = next();
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("\\u エスケープが不正です");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("不正なエスケープ \\" + e);
                }
            }
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(peek())) {
                pos++;
            }
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        private char peek() {
            if (atEnd()) {
                throw error("JSON が途中で終わっています");
            }
            return s.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) {
                throw error("'" + c + "' が必要です");
            }
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException("JSON の形式が不正です（" + pos + " 文字目）: " + message);
        }
    }
}
