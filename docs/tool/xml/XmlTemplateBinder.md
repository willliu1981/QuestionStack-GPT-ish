
````markdown
# 🧩 XmlTemplateBinder 工具介紹

`XmlTemplateBinder` 是一個實用的工具類，用於處理 XML 中的 `${欄位}` 語法，將其替換為 Java 資料模型中的對應值，並產生一個深度複製的新 XML 節點。

---

## ✨ 功能特點

- 支援 `${}` 插值語法
- 遞迴複製整棵 XML 節點
- 自動替換屬性與文字內容中的變數
- 可安全處理 null 或無對應欄位情況

---

## 📦 範例程式碼

### 1️⃣ Java 資料模型

```java
public class User {
    public String id = "u001";
    public String username = "Kuan";
    public String password = "abc123";
}
````

---

### 2️⃣ 範本 XML

```xml
<Table id="${id}">
    <Label>user:</Label>
    <Label>${username}</Label>

    <Label>pw:</Label>
    <Label>${password}</Label>
</Table>
```

---

### 3️⃣ 使用範例

```java
XmlReader.Element template = ...; // 從 XML 檔案中載入的節點
User user = new User();

XmlReader.Element filled = XmlTemplateBinder.bind(template, user);
```

---

### ✅ 結果 XML

```xml
<Table id="u001">
    <Label>user:</Label>
    <Label>Kuan</Label>

    <Label>pw:</Label>
    <Label>abc123</Label>
</Table>
```

---

## 🔧 完整原始碼

```java
package idv.kuan.studio.libgdx.simpleui.tool;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlTemplateBinder {

    /**
     * 深度複製 XML Element（保留所有屬性與子節點）
     */
    public static XmlReader.Element bind(XmlReader.Element source) {
        return bind(source, null);
    }

    /**
     * 深度複製並套用資料模型
     */
    public static XmlReader.Element bind(XmlReader.Element source, Object model) {
        XmlReader.Element copy = new XmlReader.Element(source.getName(), null);

        // 安全複製屬性
        ObjectMap<String, String> attrs = source.getAttributes();
        if (attrs != null) {
            for (ObjectMap.Entry<String, String> entry : attrs.entries()) {
                String name = entry.key;
                String value = interpolate(entry.value, model);
                copy.setAttribute(name, value);
            }
        }

        // 複製文字內容
        String text = source.getText();
        if (text != null && !text.isEmpty()) {
            copy.setText(interpolate(text, model));
        }

        // 複製子節點
        for (int i = 0; i < source.getChildCount(); i++) {
            XmlReader.Element child = source.getChild(i);
            if (child != null) {
                copy.addChild(bind(child, model));
            }
        }

        return copy;
    }

    /**
     * 使用模型替換 ${欄位名} 的語法
     */
    private static String interpolate(String text, Object model) {
        if (model == null || text == null) return text;

        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String replacement = "";
            try {
                Field field = model.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(model);
                replacement = (value != null) ? value.toString() : "";
            } catch (Exception e) {
                replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
```



