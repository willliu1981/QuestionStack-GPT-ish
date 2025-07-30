```java
package idv.kuan.studio.libgdx.simpleui.tool;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;
import bsh.Interpreter;

public class StringInterpolator {
    private final Interpreter shell;
    private final Object model;

    public StringInterpolator(Interpreter shell) {
        this.shell = shell;
        this.model = null;
    }

    public StringInterpolator(Object model) {
        this.shell = null;
        this.model = model;
    }

    public String interpolate(String text) {
        if (text == null || !text.contains("${")) return text;

        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expr = matcher.group(1);
            String result = resolveExpression(expr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(result));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveExpression(String expr) {
        Object result = null;

        // 1. 嘗試用shell取得物件或map
        if (shell != null) {
            try {
                result = shell.get(expr);
                if (result != null && !result.toString().isEmpty()) {
                    return result.toString();
                }
            } catch (EvalError ignored) {}

            // shell 若取得的是 map 或 object，再做進一步解析
            String[] parts = expr.split("(?=\\[)|(?<=])|\\.");
            if (parts.length > 1) {
                try {
                    Object ctx = shell.get(parts[0]);
                    result = resolveFromPath(parts, ctx, true);
                    if (result != null) return result.toString();
                } catch (EvalError ignored) {}
            }
        }

        // 2. model (僅支援物件欄位存取)
        if (model != null) {
            String[] parts = expr.split("(?=\\[)|(?<=])|\\.");
            result = resolveFromPath(parts, model, false);
            if (result != null) return result.toString();
        }

        return "";
    }

    private Object resolveFromPath(String[] parts, Object context, boolean allowMap) {
        Object current = context;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            if (allowMap && part.startsWith("[")) {
                String key = part.replace("[", "").replace("]", "").replace("\"", "");
                current = resolveMapValue(current, key);
            } else {
                current = resolveFieldValue(current, part);
            }

            if (current == null) return null;
        }

        return current;
    }

    private Object resolveMapValue(Object ctx, String key) {
        if (ctx instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private Object resolveFieldValue(Object ctx, String fieldName) {
        if (ctx == null) return null;

        try {
            Field field = findFieldIncludingSuper(ctx.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(ctx);
            }
        } catch (IllegalAccessException ignored) {}
        return null;
    }

    private Field findFieldIncludingSuper(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}

```
