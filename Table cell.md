以下是**libGDX Table Cell 支援屬性的完整解析 Java 範例**，包含你常用與進階的（`size`、`pad`、`colspan`、`expand`、`fill`、`align`、`minSize`、`maxSize`、`uniform`、`width`、`height`…）
你可以直接複製 `applyCellAttributes(cell, element)` 這個工具函式到你的專案，然後在 `addChildWithLayoutOptions` 裡呼叫它！

---

## Table Cell 屬性自動解析工具

```java
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.XmlReader;

public class TableCellHelper {
    /** 解析 XML element 的所有常用 Table Cell 屬性並套用到 cell 上 */
    public static void applyCellAttributes(Cell<?> cell, XmlReader.Element element) {
        // size
        String strSize = element.getAttribute("size", null);
        if (strSize != null && !strSize.isEmpty()) {
            int[] sz = parseIntArray(strSize, 2);
            cell.size(sz[0], sz[1]);
        }

        // minSize
        String strMinSize = element.getAttribute("minSize", null);
        if (strMinSize != null && !strMinSize.isEmpty()) {
            int[] sz = parseIntArray(strMinSize, 2);
            cell.minSize(sz[0], sz[1]);
        }

        // maxSize
        String strMaxSize = element.getAttribute("maxSize", null);
        if (strMaxSize != null && !strMaxSize.isEmpty()) {
            int[] sz = parseIntArray(strMaxSize, 2);
            cell.maxSize(sz[0], sz[1]);
        }

        // width / height (單獨設)
        String strWidth = element.getAttribute("width", null);
        if (strWidth != null && !strWidth.isEmpty()) {
            cell.width(Integer.parseInt(strWidth.trim()));
        }
        String strHeight = element.getAttribute("height", null);
        if (strHeight != null && !strHeight.isEmpty()) {
            cell.height(Integer.parseInt(strHeight.trim()));
        }

        // pad (同時設上下左右)
        String strPad = element.getAttribute("pad", null);
        if (strPad != null && !strPad.isEmpty()) {
            float[] pad = parseFloatArray(strPad, 4);
            if (strPad.indexOf(",") < 0) { // 只填一個時
                cell.pad(pad[0]);
            } else if (pad.length == 4) {
                cell.pad(pad[0], pad[1], pad[2], pad[3]); // top, left, bottom, right
            }
        }
        // 個別 padTop/padLeft...
        String strPadTop = element.getAttribute("padTop", null);
        if (strPadTop != null && !strPadTop.isEmpty()) cell.padTop(Float.parseFloat(strPadTop.trim()));
        String strPadLeft = element.getAttribute("padLeft", null);
        if (strPadLeft != null && !strPadLeft.isEmpty()) cell.padLeft(Float.parseFloat(strPadLeft.trim()));
        String strPadRight = element.getAttribute("padRight", null);
        if (strPadRight != null && !strPadRight.isEmpty()) cell.padRight(Float.parseFloat(strPadRight.trim()));
        String strPadBottom = element.getAttribute("padBottom", null);
        if (strPadBottom != null && !strPadBottom.isEmpty()) cell.padBottom(Float.parseFloat(strPadBottom.trim()));

        // colspan
        String strColspan = element.getAttribute("colspan", null);
        if (strColspan != null && !strColspan.isEmpty()) {
            cell.colspan(Integer.parseInt(strColspan.trim()));
        }

        // expand
        String strExpand = element.getAttribute("expand", null);
        if (strExpand != null && !strExpand.isEmpty()) {
            String[] parts = strExpand.split(",");
            boolean expandX = parts.length > 0 && parseBoolean(parts[0]);
            boolean expandY = parts.length > 1 && parseBoolean(parts[1]);
            cell.expand(expandX, expandY);
        }

        // fill
        String strFill = element.getAttribute("fill", null);
        if (strFill != null && !strFill.isEmpty()) {
            String[] parts = strFill.split(",");
            boolean fillX = parts.length > 0 && parseBoolean(parts[0]);
            boolean fillY = parts.length > 1 && parseBoolean(parts[1]);
            cell.fill(fillX, fillY);
        }

        // align
        String strAlign = element.getAttribute("align", null);
        if (strAlign != null && !strAlign.isEmpty()) {
            int alignValue = 0;
            String[] aligns = strAlign.toLowerCase().split(",");
            for (String a : aligns) {
                switch (a.trim()) {
                    case "left": alignValue |= Align.left; break;
                    case "right": alignValue |= Align.right; break;
                    case "top": alignValue |= Align.top; break;
                    case "bottom": alignValue |= Align.bottom; break;
                    case "center": alignValue |= Align.center; break;
                }
            }
            cell.align(alignValue);
        }

        // uniform
        String strUniform = element.getAttribute("uniform", null);
        if (strUniform != null && !strUniform.isEmpty()) {
            String[] parts = strUniform.split(",");
            boolean uniformX = parts.length > 0 && parseBoolean(parts[0]);
            boolean uniformY = parts.length > 1 && parseBoolean(parts[1]);
            cell.uniform(uniformX, uniformY);
        }
    }

    /** 解析 "10,20" 這類字串成 int[] */
    public static int[] parseIntArray(String str, int len) {
        String[] parts = str.split(",");
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = (i < parts.length) ? Integer.parseInt(parts[i].trim()) : 0;
        }
        return arr;
    }

    /** 解析 "10.5,20" 這類字串成 float[] */
    public static float[] parseFloatArray(String str, int len) {
        String[] parts = str.split(",");
        float[] arr = new float[len];
        for (int i = 0; i < len; i++) {
            arr[i] = (i < parts.length) ? Float.parseFloat(parts[i].trim()) : 0f;
        }
        return arr;
    }

    /** 專業級布林值解析：支援多種表示方式 */
    public static boolean parseBoolean(String s) {
        if (s == null) return false;
        String val = s.trim().toLowerCase();
        return val.equals("1") || val.equals("true") || val.equals("yes")
            || val.equals("y") || val.equals("on")
            || val.equals("是") || val.equals("真") || val.equals("開");
    }
}
```

---

## 用法說明

只要在你加到 Table 時：

```java
Cell<?> cell = table.add(actor);
TableCellHelper.applyCellAttributes(cell, element);
```

---

## 支援的 XML 寫法（範例）

```xml
<XmlLabel text="大標題"
          size="180,50"
          pad="10"
          padTop="5"
          colspan="2"
          expand="1,0"
          fill="1,1"
          align="top,left"
          minSize="100,30"
          maxSize="200,80"
          uniform="1,0"/>
```

---

## 可再延伸

如果還有更多 cell 相關屬性，只要照這個 pattern 一直擴充即可。
有特殊屬性（如 grow/shrink/visible）也可加判斷。

---

## 中文總結

* 你只要呼叫 `TableCellHelper.applyCellAttributes(cell, element);`，Table 的 cell 就會自動根據 XML 設定所有常用屬性！
* 省下大量重複 if 判斷與鏈式調用。
* 所有解析與兼容都已包含在工具函式內。


