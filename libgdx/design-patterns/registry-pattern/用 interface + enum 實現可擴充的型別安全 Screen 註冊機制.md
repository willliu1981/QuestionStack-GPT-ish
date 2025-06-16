

# 用 interface + enum 實現可擴充的 Screen 註冊表設計

> 如何讓 Screen 註冊機制既型別安全、可維護，又支援主專案自行擴充 key 名？

---

## 1. 需求背景

在遊戲或 UI 框架中，常需要一個「註冊所有 Screen」的機制，
以便可以用名稱（key）在程式/腳本/XML 中直接切換畫面。
但如果 key 名被設計成 enum，又想讓主專案能擴充，
你會發現：**Java 的 enum 不能繼承！**

---

## 2. 解決方案：interface + enum pattern

利用 interface 當 key 標準，API 提供 enum，
主專案再加自己的 enum，兩邊都能安全使用！

---

### **Step 1. 定義共用 interface**

```java
public interface SimpleUIScreenName {
    String name();
}
```

---

### **Step 2. API/共用層註冊表**

```java
import java.util.Map;
import java.util.HashMap;
import com.badlogic.gdx.Screen;

public class ScreenRegistry {
    private static final Map<SimpleUIScreenName, Screen> screenMap = new HashMap<>();

    public static void register(SimpleUIScreenName name, Screen screen) {
        screenMap.put(name, screen);
    }

    public static Screen get(SimpleUIScreenName name) {
        return screenMap.get(name);
    }
}
```

---

### **Step 3. 主專案自訂 enum key**

```java
public enum ScreenName implements SimpleUIScreenName {
    FIRST, TEST
}
```

---

### **Step 4. 使用方式**

**註冊 Screen：**

```java
ScreenRegistry.register(ScreenName.FIRST, new FirstScreen());
ScreenRegistry.register(ScreenName.TEST, new TestScreen());
```

**切換畫面：**

```java
setScreen(ScreenRegistry.get(ScreenName.FIRST));
```

---

## 3. 優點

* **型別安全**：只允許實作 `SimpleUIScreenName` 的 enum/class 作為 key
* **可擴充**：主專案可自行定義新 key，完全不受 API enum 限制
* **語意清楚**：每個 screen 對應一個獨立、唯一的 key
* **便於維護**：enum 享有 IDE 自動補全、避免魔法字串 typo

---

## 4. 小提醒

* 如果有跨模組 key 名重複，不會衝突，因為 enum 不同實例彼此不等同
* 若想讓 key 支援完全比對，可自行加強 equals/hashCode 設計

---

## 5. 總結

這個設計模式，讓你的**Screen 註冊與切換既彈性又安全**，
也很適合用於任何「可擴充 key 集合」的管理場景。

---

**歡迎參考本範例，自由擴充你的註冊表與 enum key！**

---
