# Interpolation - 巢狀取值語法擴展

本篇說明如何在 QuestionStack-GPT-ish 中，擴充原本的 `${}` 插值語法，使其支援巢狀 Map 結構存取，例如：`${lang["submit"]}`，特別適用於國際化語系、動態設定等情境。

---

## ✨ 功能亮點

* 支援 `${aaa["key"]}` 形式存取 Map 值
* 可連續巢狀存取：`${config["ui"]["theme"]}`
* 與原本的 `${key}` 插值語法兼容

---

## 📘 語法使用

### ✅ 推薦用法（單引號包屬性值，提升可讀性）

```xml
<Label text='${lang["submit"]}' />
```

### ❗ 雙引號也可用，但需跳脫

```xml
<Label text="${lang[\"submit\"]}" />
```

---

## 🔧 JavaShell 設定示範

```java
// 模擬多語言資料
Map<String, String> zhTW = Map.of(
    "submit", "送出",
    "cancel", "取消"
);
javaShell.set("lang", zhTW);
```

```xml
<!-- XML 使用語言包 -->
<Label text='${lang["submit"]}' />
```

顯示結果：

```
送出
```

---

## 🧠 多層巢狀存取

```java
Map<String, Object> config = Map.of(
    "ui", Map.of("theme", "dark", "fontSize", 16)
);
javaShell.set("config", config);
```

```xml
<Label text='${config["ui"]["theme"]}' />
```

結果：

```
dark
```

---

## ⚙️ 程式碼實作重點（interpolate）

核心在於 `interpolate(String)` 方法中對 `${}` 包裹內容進行解析：

```java
private String interpolate(String text) {
    if (text == null || !text.contains("${")) return text;

    Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
        String expr = matcher.group(1);
        String replacement = resolveNestedField(expr);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
}
```

### 巢狀解析方法：

```java
private String resolveNestedField(String expr) {
    try {
        Object value = javaShell.get(expr);
        if (value != null) return value.toString();
    } catch (EvalError ignored) {}

    // 巢狀格式處理：lang["submit"]
    String[] parts = expr.split("\\[");
    Object context = javaShell.get(parts[0]);

    for (int i = 1; i < parts.length && context != null; i++) {
        String key = parts[i].replace("]", "").replace("\"", "");
        if (context instanceof Map map) {
            context = map.get(key);
        } else {
            return "";
        }
    }

    return context != null ? context.toString() : "";
}
```

---

## 📌 注意事項

| 狀況   | 建議或限制                           |
| ---- | ------------------------------- |
| 語法格式 | 必須使用 `${...}` 包裹才能解析            |
| 鍵名引號 | 建議使用雙引號 `"key"` 包覆              |
| 資料結構 | 僅支援 `Map<String, Object>` 的巢狀存取 |
| 陣列   | 尚未支援 `arr[0]` 形式                |

---

## 📝 小結

這項語法擴展讓 XML UI 配置更加靈活，配合 JavaShell 的變數機制，可輕鬆達成國際化、主題設定、甚至動態內容顯示，是建構彈性 UI 的強力工具。
