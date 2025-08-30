
## ComponentFactory
```public class XmlTextButtonFactory extends ComponentFactory {

    public XmlTextButtonFactory(UIBuilder uiBuilder) {
        super(uiBuilder);
    }

    @Override
    protected Actor createActor(UIBuilder uiBuilder, XmlReader.Element element, Group containerGroup) {
        XmlTextButton actor = new XmlTextButton(
            element.getText(),
            ApplicationContext.get().getResourceManager().getSkin()
        );
        actor.getLabel().getStyle().font = FontHelper.generateFontFromAttributes(element);
        actor.getLabel().setStyle(actor.getLabel().getStyle());

        // 先加到父容器，吃掉 layout（width/height 等）
        addChildWithLayoutOptions(element, actor, containerGroup);

        // 通用屬性
        ActorAttributeHelper.applyCommonAttributes(actor, element);

        // 先給 label 一個內容寬度（按鈕寬 - padding）
        String wAttr = element.getAttribute("width", null);
        if (wAttr != null) {
            try {
                float w = Float.parseFloat(wAttr);
                float contentW = Math.max(0f, w - actor.getPadLeft() - actor.getPadRight());
                actor.getLabelCell().width(contentW).minWidth(contentW);
            } catch (Exception ignore) {
            }
        }

        // 再套 label 屬性（wrap/ellipsis/對齊）
        ActorAttributeHelper.applyLabelAttributes(actor.getLabel(), element);

        actor.invalidateHierarchy(); // 觸發重新排版，wrap 才會立即生效
        return actor;
    }
}
```
---
## XmlLabelFactory
```java
public class XmlLabelFactory extends ComponentFactory {


    public XmlLabelFactory(UIBuilder uiBuilder) {
        super(uiBuilder);
    }

    @Override
    protected Actor createActor(UIBuilder uiBuilder, XmlReader.Element element, Group containerGroup) {
        String indentStr = element.getAttribute("indent", "0em");
        int fontSizePx = Integer.valueOf(element.getAttribute("fontSize", "32"));
        int indentPx = FontHelper.parseEmToPixel(indentStr, fontSizePx);
        String indentSpaces = FontHelper.generateIndentSpaces(indentPx, fontSizePx);

        String rawText = element.getText();
        String labelText = rawText != null ? rawText : "";

        XmlLabel actor = new XmlLabel(
            indentSpaces + labelText,
            ApplicationContext.get().getResourceManager().getSkin());
        actor.getStyle().font = FontHelper.generateFontFromAttributes(element);
        actor.setStyle(actor.getStyle());

        addChildWithLayoutOptions(element, actor, containerGroup);
        ActorAttributeHelper.applyCommonAttributes(actor, element);
        ActorAttributeHelper.applyLabelAttributes(actor, element);

        return actor;
    }
}

```
---
## ActorAttributeHelper
```java
public class ActorAttributeHelper {

    public static void applyCommonAttributes(Actor actor, XmlReader.Element element) {
        setVisible(actor, element);
        setDebug(actor, element);
        setRotation(actor, element);
        setScale(actor, element);
        setZIndex(actor, element);
        setTouchable(actor, element);
        setOrigin(actor, element);
        setColor(actor, element);
    }

    /**
     * Label 常見屬性：textAlign、fontScale、wrap、ellipsis（wrap/ellipsis 亦支援 flags）
     */
    public static void applyLabelAttributes(Label label, XmlReader.Element element) {
        // textAlign
        String align = element.getAttribute("textAlign", null);
        if (align != null) {
            switch (align.toLowerCase()) {
                case "center" -> label.setAlignment(Align.center);
                case "left" -> label.setAlignment(Align.left);
                case "right" -> label.setAlignment(Align.right);
            }
        }

        // fontScale
        String scale = element.getAttribute("fontScale", null);
        if (scale != null) {
            try {
                label.setFontScale(Float.parseFloat(scale));
            } catch (Exception ignore) {
            }
        }

        // wrap（屬性優先；否則 flags="wrap" 視為 true）
        String wrapAttr = element.getAttribute("wrap", null);
        if (wrapAttr != null) {
            try {
                label.setWrap(Boolean.parseBoolean(wrapAttr));
            } catch (Exception ignore) {
            }
        } else if (XmlHelper.hasFlag(element, "wrap")) {
            label.setWrap(true);
        }

        // ellipsis：true/false/自訂字串；相容 flags="ellipsis"
        String ellAttr = element.getAttribute("ellipsis", null);
        if (ellAttr != null) {
            if ("true".equalsIgnoreCase(ellAttr)) label.setEllipsis(true);
            else if ("false".equalsIgnoreCase(ellAttr)) label.setEllipsis(false);
            else label.setEllipsis(ellAttr);
        } else if (XmlHelper.hasFlag(element, "ellipsis")) {
            label.setEllipsis(true);
        }
    }

    /**
     * TextField 專屬屬性：textAlign、fontScale
     */
    public static void applyTextFieldAttributes(TextField textField, XmlReader.Element element) {
        // textAlign
        String align = element.getAttribute("textAlign", null);
        if (align != null) {
            switch (align.toLowerCase()) {
                case "center" -> textField.setAlignment(Align.center);
                case "left" -> textField.setAlignment(Align.left);
                case "right" -> textField.setAlignment(Align.right);
            }
        }

        // fontScale
        String fontScale = element.getAttribute("fontScale", null);
        if (fontScale != null) {
            try {
                float scale = Float.parseFloat(fontScale);
                textField.getStyle().font.getData().setScale(scale);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * TextArea 共用 TextField 邏輯
     */
    public static void applyTextAreaAttributes(TextArea textArea, XmlReader.Element element) {
        // TextArea 強制換行，不需設 wrap
        // 保留 flags="wrap" 可作為樣式用途（例如 layout 設定）

        // 繼承 TextField 的其他屬性
        applyTextFieldAttributes(textArea, element);
    }


    public static void applyImageAttributes(Image image, XmlReader.Element element) {
        String align = element.getAttribute("align", null);
        if (align != null) {
            switch (align.toLowerCase()) {
                case "center" -> image.setAlign(Align.center);
                case "top" -> image.setAlign(Align.top);
                case "bottom" -> image.setAlign(Align.bottom);
                case "left" -> image.setAlign(Align.left);
                case "right" -> image.setAlign(Align.right);
            }
        }

        String scale = element.getAttribute("scale", null);
        if (scale != null) {
            try {
                image.setScale(Float.parseFloat(scale));
            } catch (Exception ignore) {
            }
        }
    }

    public static void applyGroupAttributes(Group group, XmlReader.Element element) {
        if (XmlHelper.hasFlag(element, "transform")) group.setTransform(true);
        String touch = element.getAttribute("touchChildrenOnly", null);
        if ("true".equalsIgnoreCase(touch)) group.setTouchable(Touchable.childrenOnly);
    }

    // --- 私有通用 ---

    private static void setVisible(Actor actor, XmlReader.Element element) {
        String v = element.getAttribute("visible", null);
        if (v != null) actor.setVisible(XmlHelper.parseBoolean(v));
    }

    private static void setDebug(Actor actor, XmlReader.Element element) {
        if (XmlHelper.hasFlag(element, "debug")) actor.debug();
    }

    private static void setRotation(Actor actor, XmlReader.Element element) {
        String val = element.getAttribute("rotation", null);
        if (val != null) try {
            actor.setRotation(Float.parseFloat(val));
        } catch (Exception ignore) {
        }
    }

    private static void setScale(Actor actor, XmlReader.Element element) {
        String sx = element.getAttribute("scaleX", null);
        String sy = element.getAttribute("scaleY", null);
        if (sx != null || sy != null) {
            float fx = sx != null ? parseFloatSafe(sx, 1f) : 1f;
            float fy = sy != null ? parseFloatSafe(sy, 1f) : 1f;
            actor.setScale(fx, fy);
        }
    }

    private static void setZIndex(Actor actor, XmlReader.Element element) {
        String z = element.getAttribute("zIndex", null);
        if (z != null) try {
            actor.setZIndex(Integer.parseInt(z));
        } catch (Exception ignore) {
        }
    }

    private static void setTouchable(Actor actor, XmlReader.Element element) {
        String t = element.getAttribute("touchable", null);
        if (t == null) return;
        switch (t.toLowerCase()) {
            case "enabled" -> actor.setTouchable(Touchable.enabled);
            case "disabled" -> actor.setTouchable(Touchable.disabled);
            case "childrenonly" -> actor.setTouchable(Touchable.childrenOnly);
        }
    }

    private static void setOrigin(Actor actor, XmlReader.Element element) {
        String origin = element.getAttribute("origin", null);
        if (origin == null) return;
        switch (origin.toLowerCase()) {
            case "center" -> actor.setOrigin(Align.center);
            case "top" -> actor.setOrigin(Align.top);
            case "bottomleft" -> actor.setOrigin(Align.bottomLeft);
            default -> {
                String[] parts = origin.split(",");
                if (parts.length == 2) {
                    try {
                        actor.setOrigin(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }

    private static void setColor(Actor actor, XmlReader.Element element) {
        String colorStr = element.getAttribute("color", null);
        if (colorStr == null) return;
        try {
            Color color = XmlHelper.parseColor(colorStr);

            // 對不同元件做不同處理
            if (actor instanceof Label l) {
                l.setColor(color); // 設 Label 字色
            } else if (actor instanceof TextField tf) {
                tf.getStyle().fontColor = color; // 設 TextField 字色
            } else if (actor instanceof TextArea ta) {
                ta.getStyle().fontColor = color;
            } else {
                actor.setColor(color); // 例如 Image、Button 就設 tint
            }
        } catch (Exception ignore) {
        }
    }


    private static float parseFloatSafe(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return def;
        }
    }
}


```
---
## XmlHelper
```java
public class XmlHelper {

    public static boolean hasFlag(XmlReader.Element element, String flagName) {
        String flags = element.getAttribute("flags", ""); // 預設空字串
        String[] parts = flags.split("\\s+"); // 用空白分隔
        for (String flag : parts) {
            if (flag.trim().equalsIgnoreCase(flagName.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 專業級布林值解析：支援多種表示方式
     */
    public static boolean parseBoolean(String s) {
        if (s == null) return false;
        String val = s.trim().toLowerCase();
        return val.equals("1") || val.equals("true") || val.equals("yes")
            || val.equals("y") || val.equals("on")
            || val.equals("是") || val.equals("真") || val.equals("開");
    }

    public static Color parseColor(String input) {
        if (input == null) return Color.WHITE;

        input = input.trim();

        // 1. 支援格式 "#RRGGBB" 或 "#RRGGBBAA"
        if (input.startsWith("#")) {
            if (input.length() == 7) {
                return Color.valueOf(input + "FF"); // 補上 alpha
            } else {
                return Color.valueOf(input);
            }
        }

        // 2. 支援格式 "r,g,b" 或 "r,g,b,a"（0~1 float）
        if (input.contains(",")) {
            String[] parts = input.split(",");
            try {
                float r = Float.parseFloat(parts[0].trim());
                float g = Float.parseFloat(parts[1].trim());
                float b = Float.parseFloat(parts[2].trim());
                float a = parts.length >= 4 ? Float.parseFloat(parts[3].trim()) : 1f;
                return new Color(r, g, b, a);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid color float value: " + input, e);
            }
        }

        // 3. 支援 Gdx Color 內建名稱
        switch (input.toLowerCase()) {
            case "red": return Color.RED.cpy();
            case "green": return Color.GREEN.cpy();
            case "blue": return Color.BLUE.cpy();
            case "yellow": return Color.YELLOW.cpy();
            case "white": return Color.WHITE.cpy();
            case "black": return Color.BLACK.cpy();
            case "gray", "grey": return Color.GRAY.cpy();
            case "cyan": return Color.CYAN.cpy();
            case "magenta": return Color.MAGENTA.cpy();
            case "orange": return Color.ORANGE.cpy();
            case "clear": return Color.CLEAR.cpy();
            default: throw new RuntimeException("Unrecognized color: " + input);
        }
    }
}
```
---
## TableCellHelper
```java
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
            boolean expandX = parts.length > 0 && XmlHelper.parseBoolean(parts[0]);
            boolean expandY = parts.length > 1 && XmlHelper.parseBoolean(parts[1]);
            cell.expand(expandX, expandY);
        }

        // fill
        String strFill = element.getAttribute("fill", null);
        if (strFill != null && !strFill.isEmpty()) {
            String[] parts = strFill.split(",");
            boolean fillX = parts.length > 0 && XmlHelper.parseBoolean(parts[0]);
            boolean fillY = parts.length > 1 && XmlHelper.parseBoolean(parts[1]);
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
            boolean uniformX = parts.length > 0 && XmlHelper.parseBoolean(parts[0]);
            boolean uniformY = parts.length > 1 && XmlHelper.parseBoolean(parts[1]);
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

}

```
