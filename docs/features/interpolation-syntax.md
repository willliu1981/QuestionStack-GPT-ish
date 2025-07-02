

## 📘 完整範例：用 `${}` 插值語法構建 UI 元素文字

### 🎯 目標

從一段包含 `${變數}` 的 XML 標籤中，將變數取值填入，最後輸出成可顯示的 UI 文字。

---

### 🧪 模擬腳本（設定變數）

```java
import bsh.Interpreter;

Interpreter shell = new Interpreter();
shell.eval("playerName = \"Alice\";");
shell.eval("level = 42;");
```

---

### 🧾 模擬 XML 原始資料

```java
String xmlText = "Welcome, ${playerName}! Your level is ${level}.";
```

---

### 🔁 插值處理方法（簡化版本）

```java
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import bsh.EvalError;

public static String interpolate(String text, Interpreter shell) {
    if (text == null || !text.contains("${")) return text;

    Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
        String fieldName = matcher.group(1);
        String replacement = "";
        try {
            Object value = shell.get(fieldName);
            replacement = (value != null) ? value.toString() : "";
        } catch (EvalError e) {
            replacement = "";
        }
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);

    return sb.toString();
}
```

---

### 🖥️ 輸出結果

```java
String finalText = interpolate(xmlText, shell);
System.out.println(finalText);
```

📤 輸出：

```plaintext
Welcome, Alice! Your level is 42.
```

---

### ✅ 使用場景

* 結合 XML UI 語法，自動將變數插入 Label、Button、Text 等元件屬性。
* 使用腳本（如 JavaScript 或 BeanShell）設定值，不需改 XML。
* 適合遊戲資料顯示、設定面板、提示訊息等。

---

### 💡 提示

* 若變數不存在，預設會顯示空字串（可改成 `${變數}` 不變或顯示警告）。
* 可以進一步支援預設值語法，如 `${userName:Guest}`。


