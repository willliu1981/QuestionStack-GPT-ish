## 問題:
```
我有A,B,C key 均指向 x value

要如何不重複建入A,B,C
例如,我輸入A,或B,或C  都能找到x
但map 只存入一個key,並指向x
```


## GPT:
你這個需求很常見，
就是「多個名稱（key）共用一個 value」，**但 Map 只存一次 value**，
查詢時無論輸入哪個名稱都能找到對應的 value。

---

## 方案一：**用「主要 key」+「別名查找」的結構**

### 1. 用一個主 Map 存 value

### 2. 另一個 Map 存「所有 key 對應到主 key」

（也就是「別名表」）

---

### **範例程式碼**

```java
// 1. 存 value
Map<String, Object> valueMap = new HashMap<>();
valueMap.put("A", x);

// 2. 別名表
Map<String, String> aliasMap = new HashMap<>();
aliasMap.put("A", "A"); // 主 key 指向自己
aliasMap.put("B", "A"); // B 是 A 的別名
aliasMap.put("C", "A"); // C 也是

// 3. 查詢
String input = "B";
Object value = valueMap.get(aliasMap.get(input));
```

---

### **解說**

* 你**只在 valueMap 放一次 value**（只用一個 key 存入）
* 但可以設定多個「別名」指向主 key
* 查找時先從 aliasMap 取得主 key，再去 valueMap 找 value

---

### **延伸用法**

* 如果 x value 是 Object，可用同一個物件
* 如果 valueMap value 是資料結構，也可以

---

### **簡化工具方法**

你也可以寫一個小工具類來封裝：

```java
class AliasMap<V> {
    private final Map<String, V> valueMap = new HashMap<>();
    private final Map<String, String> aliasMap = new HashMap<>();

    public void put(String mainKey, V value, String... aliases) {
        valueMap.put(mainKey, value);
        aliasMap.put(mainKey, mainKey);
        for (String alias : aliases) {
            aliasMap.put(alias, mainKey);
        }
    }

    public V get(String key) {
        String mainKey = aliasMap.get(key);
        return mainKey != null ? valueMap.get(mainKey) : null;
    }
}
```

**用法：**

```java
AliasMap<String> aliasMap = new AliasMap<>();
aliasMap.put("A", "x", "B", "C"); // A, B, C 都能查到 "x"
String v = aliasMap.get("B"); // v = "x"
```

---

## 這是業界常用做「多名稱對單一實體」的最佳結構！

如果你有**很多不同群組**，也能用這方法靈活擴充。
