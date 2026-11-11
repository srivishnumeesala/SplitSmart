package splitsmart.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, zero-dependency JSON parser and serializer for the web REST API.
 * Uses only standard Java utility collections.
 */
public class SimpleJson {

    private SimpleJson() {
        // Utility class
    }

    /**
     * Serializes any Java object (Map, List, String, Number, Boolean, null) to a valid JSON string.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 32) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Parses a JSON string into a Map of key-value pairs.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object res = parse(json);
        if (res instanceof Map) {
            return (Map<String, Object>) res;
        }
        return new HashMap<>();
    }

    /**
     * Parses any valid JSON value from text.
     */
    public static Object parse(String json) {
        if (json == null) {
            return null;
        }
        String s = json.trim();
        if (s.isEmpty()) {
            return null;
        }
        int[] pos = new int[]{0};
        return parseValue(s, pos);
    }

    private static Object parseValue(String s, int[] pos) {
        skipWhitespace(s, pos);
        if (pos[0] >= s.length()) {
            return null;
        }
        char c = s.charAt(pos[0]);
        if (c == '{') {
            return parseObjectInternal(s, pos);
        }
        if (c == '[') {
            return parseListInternal(s, pos);
        }
        if (c == '"') {
            return parseStringInternal(s, pos);
        }
        if (c == 't' || c == 'f') {
            return parseBooleanInternal(s, pos);
        }
        if (c == 'n') {
            return parseNullInternal(s, pos);
        }
        return parseNumberInternal(s, pos);
    }

    private static Map<String, Object> parseObjectInternal(String s, int[] pos) {
        Map<String, Object> map = new HashMap<>();
        pos[0]++; // skip '{'
        skipWhitespace(s, pos);
        if (pos[0] < s.length() && s.charAt(pos[0]) == '}') {
            pos[0]++;
            return map;
        }
        while (pos[0] < s.length()) {
            skipWhitespace(s, pos);
            String key = parseStringInternal(s, pos);
            skipWhitespace(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ':') {
                pos[0]++;
            }
            Object val = parseValue(s, pos);
            map.put(key, val);
            skipWhitespace(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ',') {
                pos[0]++;
            } else if (pos[0] < s.length() && s.charAt(pos[0]) == '}') {
                pos[0]++;
                break;
            }
        }
        return map;
    }

    private static List<Object> parseListInternal(String s, int[] pos) {
        List<Object> list = new ArrayList<>();
        pos[0]++; // skip '['
        skipWhitespace(s, pos);
        if (pos[0] < s.length() && s.charAt(pos[0]) == ']') {
            pos[0]++;
            return list;
        }
        while (pos[0] < s.length()) {
            Object val = parseValue(s, pos);
            list.add(val);
            skipWhitespace(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ',') {
                pos[0]++;
            } else if (pos[0] < s.length() && s.charAt(pos[0]) == ']') {
                pos[0]++;
                break;
            }
        }
        return list;
    }

    private static String parseStringInternal(String s, int[] pos) {
        StringBuilder sb = new StringBuilder();
        if (pos[0] < s.length() && s.charAt(pos[0]) == '"') {
            pos[0]++;
        }
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]++);
            if (c == '"') {
                break;
            }
            if (c == '\\' && pos[0] < s.length()) {
                char next = s.charAt(pos[0]++);
                if (next == '"') sb.append('"');
                else if (next == '\\') sb.append('\\');
                else if (next == '/') sb.append('/');
                else if (next == 'b') sb.append('\b');
                else if (next == 'f') sb.append('\f');
                else if (next == 'n') sb.append('\n');
                else if (next == 'r') sb.append('\r');
                else if (next == 't') sb.append('\t');
                else if (next == 'u' && pos[0] + 4 <= s.length()) {
                    String hex = s.substring(pos[0], pos[0] + 4);
                    pos[0] += 4;
                    sb.append((char) Integer.parseInt(hex, 16));
                } else {
                    sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Object parseNumberInternal(String s, int[] pos) {
        int start = pos[0];
        boolean isFloating = false;
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+') {
                pos[0]++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                isFloating = true;
                pos[0]++;
            } else {
                break;
            }
        }
        String numStr = s.substring(start, pos[0]).trim();
        if (numStr.isEmpty()) {
            return 0L;
        }
        if (isFloating) {
            return Double.parseDouble(numStr);
        }
        return Long.parseLong(numStr);
    }

    private static Boolean parseBooleanInternal(String s, int[] pos) {
        if (s.startsWith("true", pos[0])) {
            pos[0] += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", pos[0])) {
            pos[0] += 5;
            return Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    private static Object parseNullInternal(String s, int[] pos) {
        if (s.startsWith("null", pos[0])) {
            pos[0] += 4;
        }
        return null;
    }

    private static void skipWhitespace(String s, int[] pos) {
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos[0]++;
            } else {
                break;
            }
        }
    }
}
