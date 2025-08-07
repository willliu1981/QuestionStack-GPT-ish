# 重用 i18n 條目於多個 UI 模板

**目錄**

* [背景](#背景)
* [問題描述](#問題描述)
* [解決方案](#解決方案)

  * [方案一：alias 屬性](#方案一alias-屬性)
  * [方案二：UI ↔ i18n 解耦](#方案二ui-↔-i18n-解耦)
* [結論](#結論)

---

## 背景

在開發 SimpleUI 時，我們希望使用同一組 `item_supply` 資料（翻譯條目）來生成多種**不同版面**的影像清單，例如「檢查模式」和「清單模式」。

同時，我們透過 XML 模板（`item_supply_inspection.xml`、`item_supply_checklist.xml`）來描述各自的布局，但兩者對應底層的翻譯 key（如 `supply.water`、`supply.tea`……）完全一樣。

## 問題描述

* 若在 i18n 檔案中為每個 UI 模板都寫一份重複的 `<entry>`，會導致維護成本大增，且條目冗餘。
* 需要一個方法，讓多個「外觀不同」的 UI 模板，共享同一套 i18n 翻譯資料。

## 解決方案

### 方案一：alias 屬性

1. **i18n XML** 中只定義一次 `item_supply` 條目，並新增 `alias` 屬性：

   ```xml
   <template id="item_supply"
             alias="item_supply_inspection,item_supply_checklist">
     <entry name="supply.water" value="Bottled Water" />
     <entry name="supply.tea"   value="Tea Bag" />
     <!-- 其他 supply 條目 -->
   </template>
   ```
2. 在 `I18nManager.load()` 中解析 alias：

   ```java
   String alias = elem.getAttribute("alias", null);
   // 儲存原始 id 的 map
   i18nMapByScreen.put(id, map);
   // 如果有 alias，分拆後也用同一份 map
   if (alias != null) {
     for (String a : alias.split(",")) {
       i18nMapByScreen.put(a.trim(), map);
     }
   }
   ```
3. 使用 `item_supply_inspection` 或 `item_supply_checklist` 作為 UI 模板 ID 時，都會拿到同一套翻譯。

### 方案二：UI ↔ i18n 解耦

1. 在 **UIBuilder** 收集模板時，給每個 UI 模板多一個自訂屬性 `i18n`：

   ```xml
   <template id="item_supply_inspection" i18n="item_supply">
     <!-- layout A -->
   </template>
   <template id="item_supply_checklist"  i18n="item_supply">
     <!-- layout B -->
   </template>
   ```
2. `collectTemplates(...)` 時，同時記錄 `uiToI18n.put(uiId, i18nKey)`。
3. `setupShellVariables(...)` 提供給 i18nManager 的 `templateIds`，改由 `uiToI18n.values()` 提取，避免重複。

## 結論

* **不要** 為每個 UI 模板都複寫相同的翻譯條目。
* 利用 `alias` 或在 UI 模板中加 `i18n` 屬性，**複用** 同一份翻譯，就能在不同布局間共用相同的多國語系資料。

---

*文件名稱：`i18n-template-reuse.md`*
