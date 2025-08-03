``` java
package idv.kuan.studio.libgdx.simpleui.tool;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;
import bsh.Interpreter;

public class StringInterpolator {
    private final Interpreter shell;

    public StringInterpolator(Interpreter shell) {
        this.shell = shell;
    }


    public String interpolate(String text) {
        if (text == null || !text.contains("${")) return text;

        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expr = matcher.group(1);
            String result = resolve(expr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(result));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolve(String expr) {
        if (shell == null) return "${" + expr + "}";

        try {
            // 先解析 fallback，例如 ${aaa.bbb?default}
            int questionIndex = expr.indexOf('?');
            String mainExpr = questionIndex >= 0 ? expr.substring(0, questionIndex) : expr;
            String fallback = questionIndex >= 0 ? expr.substring(questionIndex + 1) : null;

            // 若包含冒號，例如 aaa.bbb:xxx.yyy → 表示查 Map 中 key
            int colonIndex = mainExpr.indexOf(':');
            if (colonIndex != -1) {
                String objExpr = mainExpr.substring(0, colonIndex);   // aaa.bbb
                String key = mainExpr.substring(colonIndex + 1);      // xxx.yyy

                Object root = shell.get(getRootToken(objExpr));
                if (root != null) {
                    Object ctx = resolveTokens(objExpr, root);
                    if (ctx instanceof Map<?, ?> map) {
                        Object val = map.get(key);
                        if (val != null) return val.toString();
                    }
                }

                // 解析失敗使用 fallback
                return fallback != null ? fallback : "${" + expr + "}";
            }

            // 一般巢狀屬性解析
            Object root = shell.get(getRootToken(mainExpr));
            if (root != null) {
                Object value = resolveTokens(mainExpr, root);
                if (value != null) return value.toString();
            }

            return fallback != null ? fallback : "${" + expr + "}";

        } catch (EvalError e) {
            return "${" + expr + "}";
        }
    }

    private Object resolveTokens(String expr, Object ctx) {
        String[] tokens = expr.split("(?=\\[)|(?<=])|\\.");
        int i = 1;
        if (tokens.length > 0 && (tokens[0].startsWith("[") || tokens[0].equals("."))) i = 0;

        for (; i < tokens.length; i++) {
            String token = tokens[i].replaceAll("^\\[\"?|\"?]$", "");

            if (ctx instanceof Map<?, ?> map) {
                ctx = map.get(token);
            } else if (ctx instanceof SessionContext sc) {
                ctx = sc.get(token); // 呼叫你提供的 get(String)
            } else {
                try {
                    Interpreter temp = new Interpreter();
                    temp.set("ctx", ctx);
                    ctx = temp.eval("ctx." + token);
                } catch (EvalError e) {
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
        int colonIndex = expr.indexOf(':');
        int questionIndex = expr.indexOf('?');

        int endIndex = expr.length();
        if (dotIndex != -1) endIndex = Math.min(endIndex, dotIndex);
        if (bracketIndex != -1) endIndex = Math.min(endIndex, bracketIndex);
        if (colonIndex != -1) endIndex = Math.min(endIndex, colonIndex);
        if (questionIndex != -1) endIndex = Math.min(endIndex, questionIndex);

        return expr.substring(0, endIndex);
    }
}

```
