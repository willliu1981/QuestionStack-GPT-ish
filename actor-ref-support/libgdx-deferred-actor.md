# libgdx-actorref-loader

這是一個擴充 LibGDX `UIBuilder` 系統的模組，實現了可延遲解析的 `actorRef` 機制，讓不同 XML 檔案之間能夠安全、穩定地相互引用未來才會建立的 Actor。

---

## ✅ 引用其他地方尚未建立的 Actor

```xml
<table>
    <image actorRef="xxx" />
</table>
```

這表示：「這個 `<image>` 並不直接描述圖片來源，而是請 UIBuilder 找出 id 為 `xxx` 的 Actor 並套用進來」。

---

## 🔄 UIBuilder 支援 `deferResolveActor(String id, Consumer<Actor>)`

你可以這樣設計 UIBuilder 的緩解機制：

```java
public void deferResolveActor(String actorId, Consumer<Actor> onResolved) {
    if (namedActors.containsKey(actorId)) {
        onResolved.accept(namedActors.get(actorId));
    } else {
        deferredBindings.add(new DeferredBinding(actorId, onResolved));
    }
}
```

等所有 XML 都解析完，呼叫：

```java
public void resolveAllDeferredBindings() {
    for (DeferredBinding binding : deferredBindings) {
        Actor actor = namedActors.get(binding.actorId);
        if (actor != null) {
            binding.callback.accept(actor);
        } else {
            throw new RuntimeException("Missing actor: " + binding.actorId);
        }
    }
    deferredBindings.clear();
}
```

---

## ✅ 將它應用在任意 Actor 的通用邏輯

### ✅ 做法 1：建一個通用的 `XmlActorRefFactory`

```java
public class XmlActorRefFactory extends ComponentFactory {
    public XmlActorRefFactory(UIBuilder uiBuilder) {
        super(uiBuilder);
    }

    @Override
    protected Actor createActor(UIBuilder uiBuilder, XmlReader.Element element, Group containerGroup) {
        final Group placeholder = new Group(); // 暫時占位
        String actorId = element.getAttribute("actorRef");

        uiBuilder.deferResolveActor(actorId, resolved -> {
            placeholder.clearChildren();
            placeholder.addActor(resolved);
        });

        addChildWithLayoutOptions(element, placeholder, containerGroup);
        return placeholder;
    }
}
```

這樣你就可以在任何地方寫：

```xml
<actorRef actorRef="sharedBtn"/>
```

---

### ✅ 做法 2：在每個 ComponentFactory 加入對 `actorRef` 的判斷

在 `XmlImageFactory` 中：

```java
@Override
protected Actor createActor(UIBuilder uiBuilder, XmlReader.Element element, Group containerGroup) {
    String actorRefId = element.getAttribute("actorRef", null);
    if (actorRefId != null) {
        Group placeholder = new Group();
        uiBuilder.deferResolveActor(actorRefId, resolved -> {
            placeholder.clearChildren();
            placeholder.addActor(resolved);
        });
        return placeholder;
    }

    // 正常流程建立 Image
    return new Image(...);
}
```

---

## 🔁 延遲解決時機（重點）

你必須在 UIBuilder 的流程中「結束所有 XML 解析後」加這一行：

```java
uiBuilder.resolveAllDeferredBindings();
```

最好放在：

* `UIBuilder.build()` 的最後
* 或你整個 `build(xml)` 流程收尾時

---

## 🔧 延伸應用：actorRef + clone

你未來甚至可以：

```xml
<image actorRef="sharedBtn" clone="true"/>
```

讓它不是共用，而是複製該 Actor，這就需要你在 `UIBuilder` 中支援 clone actor 的行為（進階功能）。

---

## ✅ 結論：actorRef 是通用的解法

你可以這樣用它：

* `<scrollPane actorRef="myTable"/>`
* `<image actorRef="logoImg"/>`
* `<group> <actorRef actorRef="sharedToggle"/> </group>`

這是一種強大又乾淨的 UI 模組化手段，讓 LibGDX 的 XML UI 定義支援跨檔引用與懶加載行為。
