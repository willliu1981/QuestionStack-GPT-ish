``` java
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
        this.model = model;
        this.shell = null;
    }

    public StringInterpolator(Interpreter shell, Object model) {
        this.shell = shell;
        this.model = model;
    }

    public String interpolate(String text) {
        if (text == null || !text.contains("${")) return text;

        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expr = matcher.group(1);
            String replacement = resolve(expr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolve(String expr) {
        Object value = null;

        // 優先從 shell 取值
        if (shell != null) {
            try {
                Object root = shell.get(getRootToken(expr));
                if (root != null) {
                    value = resolveTokens(expr, root);
                    if (value != null) return value.toString();
                }
            } catch (EvalError ignored) {
            }
        }

        // 再從 model 嘗試
        if (model != null) {
            try {
                Object root;
                if (model instanceof Map<?, ?> modelMap) {
                    root = modelMap.get(getRootToken(expr));
                } else {
                    Field field = getFieldByName(model.getClass(), getRootToken(expr));
                    if (field != null) {
                        field.setAccessible(true);
                        root = field.get(model);
                    } else {
                        root = null;
                    }
                }

                if (root != null) {
                    value = resolveTokens(expr, root);
                    if (value != null) return value.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private Object resolveTokens(String expr, Object ctx) {
        // 拆解 token: 支援 field["inner"]、field.inner 混合
        String[] tokens = expr.split("(?=\\[)|(?<=])|\\.");
        // 移除第一個 root
        int i = 1;
        if (tokens.length > 0 && (tokens[0].startsWith("[") || tokens[0].equals("."))) i = 0;

        for (; i < tokens.length; i++) {
            String token = tokens[i];
            token = token.replaceAll("^\\[\"?|\"?]$", ""); // 移除 [" 和 "] 或 [key]

            if (ctx instanceof Map<?, ?> map) {
                ctx = map.get(token);
            } else {
                try {
                    Field field = getFieldByName(ctx.getClass(), token);
                    if (field == null) return null;
                    field.setAccessible(true);
                    ctx = field.get(ctx);
                } catch (Exception e) {
                    return null;
                }
            }

            if (ctx == null) return null;
        }

        return ctx;
    }

    private String getRootToken(String expr) {
        int dotIndex = expr.indexOf('.');
        int bracketIndex = expr.indexOf('[');

        int endIndex;
        if (dotIndex == -1 && bracketIndex == -1) {
            endIndex = expr.length();
        } else if (dotIndex == -1) {
            endIndex = bracketIndex;
        } else if (bracketIndex == -1) {
            endIndex = dotIndex;
        } else {
            endIndex = Math.min(dotIndex, bracketIndex);
        }

        return expr.substring(0, endIndex);
    }

    private Field getFieldByName(Class<?> clazz, String name) {
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
