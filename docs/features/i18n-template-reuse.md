你会发现，真正“重复”的其实只是你的 i18n 条目，而 UI 模板只是两种不同的布局——它们背后用的还是同一批翻译 key（比如 `supply.water`、`supply.tea`……）。最理想的做法，是让这两套 UI 模板都“指向”同一个翻译模板，而不是为每个 UI 模板都写一份翻译。下面给你两种简单可行的思路：

---

## 方案一：在 i18n XML 里加一个 alias 属性

把所有 `item_supply` 相关的 `<entry>` 只写一次，然后用一个逗号分隔的 `alias` 属性把它“复制”给其他模板 ID：

```xml
<!-- i18n.xml -->
<template id="item_supply"
          alias="item_supply_inspection,item_supply_checklist">
  <entry name="supply.water"  value="Bottled Water"/>
  <entry name="supply.tea"    value="Tea Bag"/>
  ……
</template>
```

然后在你的 `I18nManager.load()` 里：

```java
for (XmlReader.Element tmpl : root.getChildrenByName("template")) {
  String id    = tmpl.getAttribute("id");
  String alias = tmpl.getAttribute("alias", null);

  Map<String,I18nEntry> map = parseEntries(tmpl);
  // 把自己 put 一次
  i18nMapByScreen.put(id, map);
  // 如果有 alias，把 alias 分出来也 put 一次
  if (alias != null) for (String a: alias.split(",")) {
    i18nMapByScreen.put(a.trim(), map);
  }
}
```

这样：

* `item_supply_inspection` 和 `item_supply_checklist` 都会“拿到”同一份 `map`，根本不用在 XML 里写两份重复的 `<entry>`。
* 当你切换到不同 screen 时，只要 include 对应的 UI 模板，i18nManager 就会自动去拿那个模板 ID（或它的 alias）的翻译。

---

## 方案二：在 UIBuilder 收集模板时，把 UI 模板 ID ↔ i18n 模板 ID 解耦

给你的 UI XML 里的 `<template>` 加个自定义属性，比如 `i18n="item_supply"`，标明它要用哪套翻译：

```xml
<template id="item_supply_inspection" i18n="item_supply">
   … 布局代码 …
</template>
<template id="item_supply_checklist"  i18n="item_supply">
   … 略有不同的布局 …
</template>
```

然后在 `collectTemplates(...)` 的时候，不只把 `templates.put(id, elem)`，还把一张 map 记录这个 `id` 对应的 `i18n` key：

```java
Map<String,String> uiToI18n = new HashMap<>();

String id    = elem.getAttribute("id");
String i18nKey = elem.getAttribute("i18n", id); // 默认就是自己
templates.put(id, elem);
uiToI18n.put(id, i18nKey);
```

最后在 `setupShellVariables(...)` 时，传给 i18nManager 的 templateIds 用 `uiToI18n.values()` 而不是 `templates.keySet()`，这样不管你有多少个 UI 模板，实际上翻译只会拉那几个真正的 i18nKey，不会重复。

---

### 总结

* **不要** 在 i18n XML 里为每个 UI 模板都写一份完全相同的 `<entry>`。
* **复用** 同一套翻译：要么通过 `alias`，要么通过给 UI 模板打一个指向同一翻译组的 `i18n` 属性。
* 这样无论将来你有 2 个、5 个还是 10 个长得略不一样的 `item_supply` 模板，都只写一组翻译，维护成本极低。
