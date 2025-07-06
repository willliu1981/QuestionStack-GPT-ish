

## 📦 專案功能簡述：SimpleUI 單表單 CRUD 機制

這是一個 LibGDX + SimpleUI 結合的範例，展示如何使用 **同一個畫面與表單**，根據資料狀態執行：

* Create（新增）
* Read（讀取）
* Update（更新）
* Delete（刪除）

設計理念：

* 若 `SessionContext` 沒有傳入 ID，進入「新增模式」
* 若 `SessionContext` 有 ID，則自動切換至「編輯模式」
* 所有操作統一透過相同 UI 畫面完成，不需切換至其他表單

---

## ✅ XML：`test_update.xml`

```xml
<UI>
    <Table id="tb_update" flags="fillParent">
        <Label fontKey="title">Note Editor</Label>
        <Row />
        <Table>
            <Label>ID:</Label>
            <Label id="lbl_id" padLeft="20" flags="next" />

            <Label>Title:</Label>
            <TextField id="txt_title" padLeft="20" flags="next" />

            <Label>Content:</Label>
            <TextField id="txt_content" padLeft="20" flags="next" />

            <Table padTop="20">
                <TextButton id="btn_edit">Edit</TextButton>
                <TextButton id="btn_save">Save</TextButton>
                <TextButton id="btn_delete">Delete</TextButton>
                <TextButton onClick='screen("first")'>Back</TextButton>
            </Table>
        </Table>
    </Table>
</UI>
```

---

## ✅ Java 程式邏輯（擷取自 `TestUpdateListItemScreen`）

```java
String id = context.getSessionContext().get("id", String.class);
NoteItem item = noteItemMap.get(id); // item 可為 null 表示是「新增」

// 綁定 UI 元件
txtTitle.setText(item != null ? item.getTitle() : "");
txtContent.setText(item != null ? item.getContent() : "");
lblId.setText(item != null ? item.getId() : "(new)");

// 預設唯讀模式
txtTitle.setDisabled(true);
txtContent.setDisabled(true);
btnSave.setVisible(false);
btnDelete.setVisible(item != null); // 只有編輯模式才顯示刪除鍵

// 編輯按鈕
ui.onClick("btn_edit", () -> {
    txtTitle.setDisabled(false);
    txtContent.setDisabled(false);
    btnSave.setVisible(true);
    btnEdit.setVisible(false);
});

// 儲存按鈕（Create or Update）
ui.onClick("btn_save", () -> {
    String newTitle = txtTitle.getText().toString();
    String newContent = txtContent.getText().toString();
    
    if (item == null) { // ➤ 新增
        String newId = UUID.randomUUID().toString();
        item = new NoteItem(newId, newTitle, "common");
        noteItemMap.put(newId, item);
    }
    
    item.setTitle(newTitle); // ➤ 更新內容
    item.setContent(newContent);

    context.getSessionContext().put("noteItemMap", noteItemMap);
    context.getGame().setScreen(new FirstScreen()); // 回列表
});

// 刪除按鈕
ui.onClick("btn_delete", () -> {
    if (item != null) {
        noteItemMap.remove(item.getId());
    }
    context.getGame().setScreen(new FirstScreen()); // 回列表
});
```

---

## 📌 小結與使用說明

| 狀態判斷      | 模式     | 功能行為說明           |
| --------- | ------ | ---------------- |
| 無 ID      | Create | 空白表單，填完 Save 則新增 |
| 有 ID      | Read   | 顯示現有資料，預設唯讀      |
| Edit 點擊   | Update | 解鎖欄位，Save 後更新原資料 |
| Delete 點擊 | Delete | 移除資料並返回列表        |


