# XmlCloner：複製 XML 樣板並注入資料

在 LibGDX + XML UI 架構中，實現「以 XML 樣板 + 資料模型產出 UI 元件」的設計模式。

---

## ✨ 使用情境

假設你有一個樣板：

```xml
<Table>
    <Image id="${id}" src="${image}" />
    <Label text="${text}" />
</Table>
```

你希望根據資料清單動態產生多個 UI 元件：

```java
for (Note note : noteList) {
    Actor actor = uiBuilder.build(XmlCloner.deepCopy(template, note));
    parent.addActor(actor);
}
```

---

## 🔧 XmlCloner 基本功能

```java
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlCloner {

    /**
     * 深度複製 XML Element（保留所有屬性與子節點）
     */
    public static XmlReader.Element deepCopy(XmlReader.Element source) {
        return deepCopy(source, null);
    }

    /**
     * 深度複製並套用資料模型
     */
    public static XmlReader.Element deepCopy(XmlReader.Element source, Object model) {
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
                copy.addChild(deepCopy(child, model));
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
                // 忽略錯誤，保持空字串
                replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
```

### ✅ `deepCopy(XmlReader.Element source)`

複製一棵完整 XML 樹，包括：

* 標籤名稱
* 屬性（如 `id`, `text`）
* 子元素（遞迴）
* 文字內容（`<Label>Hello</Label>`）

### ✅ `deepCopy(XmlReader.Element source, Object model)`

複製同時**將資料注入**到屬性與文字中。

支援：

```xml
<Image id="${id}" src="${image}" />
<Label text="${text}" />
```

你只需要提供 model，例如：

```java
public class Note {
    public String id = "note123";
    public String text = "歡迎使用剪語";
    public String image = "img/icon.png";
}
```

---

## 🧠 interpolate()：注入參數

支援的佔位符語法： `${fieldName}`

會使用 Java 反射自動讀取 model 的 public 欄位。

未來可擴充支援：

* Map
* JsonNode
* 自定轉換器

---

## ✅ 範例代碼

```java
XmlReader.Element copied = XmlCloner.deepCopy(templateElement, noteModel);
Actor actor = uiBuilder.build(copied);
```

---

## 🔒 不可變的樣板

為了避免重複建構過程中污染樣板物件，`XmlCloner` 採用 deep copy，確保每個 UI 生成都是獨立的實體。

避免像這樣的問題：

```java
// 不可共用 templateElement 來直接改屬性！
template.setAttribute("id", "X"); // 將污染原始樣板
```

---

## 🧩 延伸功能建議

* 支援 clone="true" 時複製樣板（而非引用）
* 支援 model + 動態屬性配置
* 支援綁定資料回傳、點擊觸發參考該資料

---

## 🔚 總結

使用 `XmlCloner` 可以讓你的 UI 建構流程達到：

* 樣板重用
* 模型注入
* 結構不變
* 易於測試與維護

適合用於：列表、便貼、資訊卡片、任務、裝備欄、自訂表單欄位等結構性 UI。
