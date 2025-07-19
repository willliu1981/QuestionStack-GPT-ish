```xml
<UI>
    <script>
        void lang(String i18nPath){
            i18n.load(i18nPath);
            screen("lobby");
        }
    </script>


    <table id="tb_settings" flags="fillParent">
        <!-- Header Section -->
        <table fill="1,0" pad="20" background="picture/common/pane.png">
            <label fontKey="title" fontSize="80" align="center">${text["settings"]}</label>
            <row />
            <label fontSize="40" align="center" padTop="10">${text["subtitle"]}</label>
        </table>

        <!-- Content Section -->
        <row />
        <table expand="0,1">
            <!-- common settings... -->
            <table>
                <label>
                    Common Settings...
                </label>
            </table>

            <!-- language -->
            <row />
            <table padTop="50">
                <label>
                    ${text["language"]}
                </label>

                <row />
                <scrollPane padTop="20">
                    <table>
                        <textButton onClick='lang("i18n/v2/lobby_en.xml")' width="200" flags="next">English
                        </textButton>
                        <textButton onClick='lang("i18n/v2/lobby_ja.xml")' width="200" flags="next">にほんご
                        </textButton>
                        <textButton onClick='lang("i18n/v2/lobby_zhTW.xml")' width="200" flags="next">中文
                        </textButton>
                    </table>
                </scrollPane>
            </table>

            <row />
            <textButton onClick='screen("lobby")' width="400" padTop="100">${text["return"]}</textButton>
        </table>

        <!-- Footer Section -->
        <row />
        <table>
            <include template="footer" />
        </table>
    </table>
</UI>


```

---

```java
Exception in thread "main" java.lang.RuntimeException: Sourced file: inline evaluation of: `screen("settings");'' : Method Invocation setScreen : at Line: 1 : in file: inline evaluation of: void screen(String name) {    context.getGame().setScreen(context.getScreenRegis . . . '' : .setScreen ( context .getScreenRegistry ( ) .get ( name ) ) 

Called from method: screen : at Line: 1 : in file: inline evaluation of: `screen("settings");'' : screen ( "settings" ) 
Target exception: com.badlogic.gdx.utils.GdxRuntimeException: Element script doesn't have attribute: lang
```
---

這個錯誤訊息雖然是從 `screen("settings")` 拋出，但**實際原因是你定義的 `<script>` 被系統解析錯誤，導致 `screen()` 無法執行**。

---

## ❗ 錯誤解析重點：

```
GdxRuntimeException: Element script doesn't have attribute: lang
```

這代表：

### ❌ LibGDX 的 `XmlReader.Element` 在處理 `<script>` 時，**預期它是一個像 `<xxx lang="java" />` 的節點**，但你這邊的 `<script>` 寫法是：

```xml
<script>
    void lang(String xmlPath){
        i18n.load(xmlPath);
        screen("lobby");
    }
</script>
```

### ⚠ 因此它嘗試讀取 `<script>` 的屬性 `"lang"`，但你沒有給它，導致錯誤。

---

## ✅ 解法有兩種：

### ✅ 方法一：加入 `lang` 屬性（建議）

```xml
<script lang="java">
    void lang(String xmlPath){
        i18n.load(xmlPath);
        screen("lobby");
    }
</script>
```

這樣一來就能滿足你框架中 `XmlHelper.getAttribute(scriptElement, "lang")` 的需求，避免錯誤。

---

### ✅ 方法二：改寫程式碼，讓 `lang` 屬性變成非必要（不建議）

除非你控制整個 UIBuilder 的 script 解析邏輯，否則不建議這樣做。若你真的希望 `<script>` 不加屬性也能跑，得改寫如下：

```java
// 舊的
String lang = scriptElement.getAttribute("lang"); // ⚠ 沒有就會噴錯

// 建議改為
String lang = scriptElement.getAttribute("lang", "java"); // 設預設值
```

---

## ✅ 總結

你目前錯誤的主因是：

> `<script>` 少了 `lang="java"` 屬性，導致 `getAttribute("lang")` 時噴錯。

### 🔧 正確寫法：

```xml
<script lang="java">
    void lang(String xmlPath){
        i18n.load(xmlPath);
        screen("lobby");
    }
</script>
```

加上這段就能順利執行 `screen("lobby")`，不會噴 `GdxRuntimeException`。

