
## ✅ 引用其他地方尚未建立的 Actor

```xml
<table>
    <image actorRef="xxx" />
</table>
```

這表示：「這個 `<image>` 並不直接描述圖片來源，而是請 UIBuilder 找出 id 為 `xxx` 的 Actor 並套用進來」。

---

## 🔄 將它應用在任意 Actor 的通用邏輯如下：

### ✅ 做法 1：建一個通用的 `ActorRefFactory`

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
            placeholder.addActor(resolved); // 替換成真正 actor
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

或讓 `<Image>`、`<Label>` 這類 Actor factory 加入 `actorRef` 檢查：

---

## ✅ 做法 2：在每個 ComponentFactory 加入對 `actorRef` 的判斷

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

讓他不是共用，而是複製該 Actor，這就需要你在 `UIBuilder` 中支援 clone actor 的行為（進階功能）。

---

## ✅ 結論：actorRef 是通用的解法

你可以這樣用它：

* `<scrollPane actorRef="myTable"/>`
* `<image actorRef="logoImg"/>`
* `<group> <actorRef actorRef="sharedToggle"/> </group>`


