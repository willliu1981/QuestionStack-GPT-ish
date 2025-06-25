# ★ Actor 與 Model 結合的 List Item 更新 

**LibGDX** 中將 ScrollPane 內容與 Model 設計結合。當 model 變更時，將自動更新對應的圖形元件。

---

## 目標

* 為 ScrollPane 內的每個 item 設計 View
* 將觀測 View 與 Model 結合
* 支援 update single 與 update all

---

## 第 1 步: 設計 Item Model

```java
public class ItemModel {
    public String name;
    public String imagePath;
    public String buttonText;
    public boolean enabled;

    public ItemModel(String name, String imagePath, String buttonText, boolean enabled) {
        this.name = name;
        this.imagePath = imagePath;
        this.buttonText = buttonText;
        this.enabled = enabled;
    }
}
```

---

## 第 2 步: 組合 View Wrapper

```java
public class ItemViewWrapper {
    public Table container;
    public Label label;
    public Image image;
    public TextButton button;

    public void update(ItemModel model) {
        label.setText(model.name);
        image.setDrawable(new TextureRegionDrawable(new TextureRegion(new Texture(model.imagePath))));
        button.setText(model.buttonText);
        button.setDisabled(!model.enabled);
    }
}
```

---

## 第 3 步: 用 model 產生視圖

```java
List<ItemModel> models = ...;
List<ItemViewWrapper> views = new ArrayList<>();

for (ItemModel model : models) {
    ItemViewWrapper wrapper = new ItemViewWrapper();
    wrapper.label = new Label(model.name, skin);
    wrapper.image = new Image(new Texture(model.imagePath));
    wrapper.button = new TextButton(model.buttonText, skin);

    wrapper.container = new Table();
    wrapper.container.add(wrapper.image).row();
    wrapper.container.add(wrapper.label).row();
    wrapper.container.add(wrapper.button).row();

    scrollPaneTable.add(wrapper.container).pad(10).row();
    views.add(wrapper);
}
```

---

## 第 4 步: 更新 View

### 單一 Item

```java
views.get(index).update(models.get(index));
```

### 更新全部 Item

```java
for (int i = 0; i < models.size(); i++) {
    views.get(i).update(models.get(i));
}
```

---

## 延伸: 用 Map 管理 id 與 View 對應

```java
Map<String, ItemViewWrapper> itemViewMap = new HashMap<>();

itemViewMap.put("item1", wrapper);

// 使用 id 更新
itemViewMap.get("item1").update(newModel);
```

---

## 結論

* 將 Model 與 Actor 分離設計，保持細節控制與更新
* 可改為 UIBuilder + XML 產生
* 可為每個 item 設 id，實現 clone + update + remove

---

🚀 這是一個隨時可接入 LibGDX UI 或 MVC 系統的基礎模型，是設計 Scrollable List 時很有用的技巧。
