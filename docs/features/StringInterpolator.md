``` java
package idv.kuan.studio.libgdx.simpleui.tool;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * StringInterpolator — 在字串中展開 ${...} 表示式。
 *
 * 支援的寫法：
 * 1) 變數：           ${name}
 * 2) 物件屬性：       ${user.name}                // 透過 BeanShell 取 ctx.name
 * 3) Map 取值（冒號）： ${text:supply.water}        // key 允許含點
 * 4) Map 取值（中括號）：${text["reset.confirm.message"]}
 *    也可多段：       ${dict["a"]["b.c"]}
 * 5) SessionContext：  ${session.someKey}         // 透過 resolveTokens 的 SessionContext 分支
 * 6) 預設值：         ${something?Default Text}   // 取不到時回傳預設字串
 *
 * 解析失敗時，會回傳原樣（例如 "${xxx}"）或 fallback（若有 ?fallback）。
 */
public class StringInterpolator {
    private final Interpreter shell;

    // 中括號存取：root["k1"]["k2"]...；root 名稱 group(1)，其餘一連串中括號 group(2+)
    private static final Pattern BRACKET_ACCESS =
        Pattern.compile("^([A-Za-z_][\\w]*)((\\[(\"([^\"]*)\"|'([^']*)')\\])+)\\s*$");

    // 擷取每一個 ["..."] 或 ['...'] 內的 key
    private static final Pattern EACH_BRACKET_KEY =
        Pattern.compile("\\[(\"([^\"]*)\"|'([^']*)')\\]");

    public StringInterpolator(Interpreter shell) {
        this.shell = shell;
    }

    public String interpolate(String text) {
        if (text == null || !text.contains("${")) return text;

        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
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
            String fallback  = questionIndex >= 0 ? expr.substring(questionIndex + 1) : null;

            // ==== 先處理中括號語法： obj["a"]["b.c"] ====
            // 這類寫法 key 可能含點，不可用 '.' 切割
            Matcher bx = BRACKET_ACCESS.matcher(mainExpr);
            if (bx.matches()) {
                String rootName = bx.group(1);
                Object ctx = shell.get(rootName);
                if (ctx == null) return (fallback != null) ? fallback : "${" + expr + "}";

                Matcher km = EACH_BRACKET_KEY.matcher(mainExpr.substring(rootName.length()));
                while (km.find() && ctx != null) {
                    // 取雙/單引號內字串
                    String k = km.group(2) != null ? km.group(2) : km.group(3);
                    if (ctx instanceof Map<?, ?> m) {
                        ctx = m.get(k);
                    } else if (ctx instanceof SessionContext sc) {
                        ctx = sc.get(k);
                    } else {
                        // 不支援在非 Map/SessionContext 上做 bracket 字串索引
                        return (fallback != null) ? fallback : "${" + expr + "}";
                    }
                }
                return (ctx != null) ? String.valueOf(ctx)
                    : (fallback != null ? fallback : "${" + expr + "}");
            }

            // ==== 冒號語法： objExpr:key （key 允許含點）====
            int colonIndex = mainExpr.indexOf(':');
            if (colonIndex != -1) {
                String objExpr = mainExpr.substring(0, colonIndex); // 例如 text
                String key     = mainExpr.substring(colonIndex + 1); // 例如 supply.water

                Object root = shell.get(getRootToken(objExpr));
                if (root != null) {
                    Object ctx = resolveTokens(objExpr, root);
                    if (ctx instanceof Map<?, ?> map) {
                        Object val = map.get(key);
                        if (val != null) return val.toString();
                    } else if (ctx instanceof SessionContext sc) {
                        Object val = sc.get(key);
                        if (val != null) return val.toString();
                    }
                }
                return (fallback != null) ? fallback : "${" + expr + "}";
            }

            // ==== 一般巢狀屬性 ====
            Object root = shell.get(getRootToken(mainExpr));
            if (root != null) {
                Object value = resolveTokens(mainExpr, root);
                if (value != null) return value.toString();
            }

            return (fallback != null) ? fallback : "${" + expr + "}";
        } catch (EvalError e) {
            return "${" + expr + "}";
        }
    }

    private Object resolveTokens(String expr, Object ctx) {
        // 以 . 和 [] 邊界切 token；注意：中括號字串寫法已在 BRACKET_ACCESS 快路徑處理
        String[] tokens = expr.split("(?=\\[)|(?<=])|\\.");
        int i = 1;
        if (tokens.length > 0 && (tokens[0].startsWith("[") || tokens[0].equals("."))) i = 0;

        for (; i < tokens.length; i++) {
            // 去除 ["..."] / ['...'] / [] 外層記號，只保留中間內容
            String token = tokens[i].replaceAll("^\\[\"?|\"?]$", "");

            if (ctx instanceof Map<?, ?> map) {
                ctx = map.get(token);
            } else if (ctx instanceof SessionContext sc) {
                ctx = sc.get(token);
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
