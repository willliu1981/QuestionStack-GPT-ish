```java
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class FontManager implements Disposable {
    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private HashSet<Character> characterSet;
    private Map<Integer, ReferenceCountedFont> fontCache;
    private boolean isUpdated;

    public FontManager(String fontPath) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        characterSet = new HashSet<>();
        fontCache = new HashMap<>();
        isUpdated = false;
    }

    public void addCharacters(String newCharacters) {
        for (char c : newCharacters.toCharArray()) {
            if (characterSet.add(c)) {
                isUpdated = true; // 標記需要更新字型
            }
        }
    }

    public BitmapFont getFont(int size) {
        if (!fontCache.containsKey(size) || isUpdated) {
            regenerateFont(size);
            isUpdated = false;
        }


        ReferenceCountedFont countedFont = fontCache.get(size);
        if (countedFont == null) {
            throw new IllegalStateException("Font generation failed for size: " + size);
        }
        countedFont.acquire(); // 增加引用
        Gdx.app.log(this.getClass().getSimpleName(), "Font acquired. Current references: " + countedFont.referenceCount);

        return countedFont.getFont();
    }

    public void releaseFont(int size) {
        if (fontCache.containsKey(size)) {
            ReferenceCountedFont countedFont = fontCache.get(size);
            countedFont.release(); // 減少引用
            Gdx.app.log(this.getClass().getSimpleName(), "Font released. Current references: " + countedFont.referenceCount);
            if (countedFont.isUnused()) {
                fontCache.remove(size);
                countedFont.dispose();
                Gdx.app.log(this.getClass().getSimpleName(), "Font disposed for size: " + size);
            }
        }
    }

    private void regenerateFont(int size) {
        // 只清理未被使用的字型
        if (fontCache.containsKey(size)) {
            ReferenceCountedFont countedFont = fontCache.get(size);
            if (countedFont.isUnused()) {
                fontCache.remove(size);
                countedFont.dispose();
                Gdx.app.log(this.getClass().getSimpleName(), "Unused font cleaned up for size: " + size);
            }
        }

        // 合併字元集
        StringBuilder combineCharacters = new StringBuilder();
        combineCharacters.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()_+-=[]{};':\\\",.<>?/|");
        for (Character c : characterSet) {
            combineCharacters.append(c);
        }

        // 生成新字型並存入快取
        parameter.size = size;
        parameter.characters = combineCharacters.toString();
        BitmapFont font = generator.generateFont(parameter);
        fontCache.put(size, new ReferenceCountedFont(font));
        Gdx.app.log(this.getClass().getSimpleName(), "New font generated for size: " + size);
    }


    @Override
    public void dispose() {
        for (ReferenceCountedFont countedFont : fontCache.values()) {
            countedFont.dispose();
        }
        generator.dispose();
    }


    public static class ReferenceCountedFont {
        private BitmapFont font;       // 引用的 BitmapFont
        private int referenceCount;    // 引用計數

        public ReferenceCountedFont(BitmapFont font) {
            this.font = font;
            this.referenceCount = 0;   // 初始引用計數為 0
        }

        // 獲取 BitmapFont
        public BitmapFont getFont() {
            return font;
        }

        // 增加引用計數
        public void acquire() {
            referenceCount++;
        }

        // 減少引用計數
        public void release() {
            if (referenceCount > 0) {
                referenceCount--;
            }
        }

        // 檢查是否沒有人在使用
        public boolean isUnused() {
            return referenceCount <= 0;
        }

        // 釋放資源
        public void dispose() {
            if (font != null) {
                font.dispose();
                font = null;
            }
        }
    }
}
```

這裡使用思源字體作為範例
```java
FontManger fontManager=new FontManager("assets/font/SourceHanSansHC-Regular.otf");
fontManager.addCharacters("思源字體範例");
BitmapFont font=fontManager.getFont((int) fontSize)

skin.add("shs-regular", font);

//從skin取得font
BitmapFont newFont = skin.getFont(fontSB.toString());

//然後
label.getStyle().font=newFont;

//如果是button的子類,例如TextButton:
Label.LabelStyle style = button.getLabel().getStyle();
style.font=newFont;
button.getLabel().setStyle(style);//這裡需要setStyle以觸發字型更新
```


