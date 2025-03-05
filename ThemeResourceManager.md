以下提供一個完整的示例，說明如何在 XML 裡定義資源，並透過全局資源管理器在運行時根據主題切換來載入不同風格的資源。

---

### 1. XML 定義

假設你有一個 UI 定義文件（例如 ui.xml），裡面定義了兩個圖片資源，分別是背景和按鈕圖示。注意，這裡只指定檔名，不含主題資料夾的部分：

```xml
<UI>
    <!-- img 屬性僅保留檔名，實際路徑由主題管理器組合 -->
    <Image id="background" img="background.png" />
    <Image id="button" img="button.png" />
</UI>
```

---

### 2. 全局資源管理器

在前面的示例中，我們已經建立了一個基於 AssetManager 的主題管理器。這裡再重申重點：

```java
public class ThemeResourceManager {
    private static ThemeResourceManager instance;
    private AssetManager assetManager;
    private String currentTheme;

    private ThemeResourceManager() {
        assetManager = new AssetManager();
        // 預設主題設定，例如 "yellow"
        currentTheme = "yellow";
    }

    public static ThemeResourceManager getInstance() {
        if (instance == null) {
            instance = new ThemeResourceManager();
        }
        return instance;
    }

    // 切換主題時，可以選擇清除目前載入的資源
    public void setTheme(String theme) {
        if (!theme.equals(currentTheme)) {
            currentTheme = theme;
            assetManager.clear(); // 清除所有已載入的資源，可根據需求做更精細的卸載處理
        }
    }

    // 根據圖片檔名返回對應主題下的 Texture
    public Texture getTexture(String imageName) {
        String themedPath = currentTheme + "/" + imageName;
        if (!assetManager.isLoaded(themedPath, Texture.class)) {
            assetManager.load(themedPath, Texture.class);
            assetManager.finishLoadingAsset(themedPath);
        }
        return assetManager.get(themedPath, Texture.class);
    }
}
```

---

### 3. 整合 XML 與主題切換的示例

假設我們在程式中使用 XML 解析器（或自行解析 XML 文件）來讀取資源定義，並依據 `img` 屬性使用 ThemeResourceManager 載入圖片。以下是一個簡化版的示例，展示如何在運行過程中中途切換主題（例如從 "yellow" 切換到 "cartoon"）：

```java
public class GameScreen implements Screen {
    private SpriteBatch batch;
    private Texture background;
    private Texture buttonTexture;
    // 用來模擬主題切換的旗標，實際上可根據按鈕事件或其他條件來切換
    private boolean themeSwitched = false;
    private float timeElapsed = 0f;

    public GameScreen() {
        batch = new SpriteBatch();
        // 預設主題為 "yellow"
        ThemeResourceManager.getInstance().setTheme("yellow");

        // 假設透過 XML 解析後獲得的圖片檔名
        String bgImg = "background.png";
        String btnImg = "button.png";

        // 使用主題管理器載入資源
        background = ThemeResourceManager.getInstance().getTexture(bgImg);
        buttonTexture = ThemeResourceManager.getInstance().getTexture(btnImg);
    }

    @Override
    public void render(float delta) {
        timeElapsed += delta;
        // 模擬在 5 秒後切換主題
        if (!themeSwitched && timeElapsed > 5f) {
            // 切換主題到 "cartoon"
            ThemeResourceManager.getInstance().setTheme("cartoon");
            // 重新從管理器取得資源，此時 getTexture 組合出新路徑 "cartoon/xxx.png"
            background = ThemeResourceManager.getInstance().getTexture("background.png");
            buttonTexture = ThemeResourceManager.getInstance().getTexture("button.png");
            themeSwitched = true;
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        // 畫背景與按鈕，注意圖片會根據主題不同而不同
        batch.draw(background, 0, 0);
        batch.draw(buttonTexture, 100, 100);
        batch.end();
    }

    @Override
    public void resize(int width, int height) { }
    @Override
    public void show() { }
    @Override
    public void hide() { }
    @Override
    public void pause() { }
    @Override
    public void resume() { }
    @Override
    public void dispose() {
        batch.dispose();
        // 適時釋放資源
    }
}
```

#### 說明

1. **XML 部分**  
   - XML 僅定義圖片檔名，例如 `img="background.png"`。這樣你就可以根據不同主題由程式自動組合出完整路徑（如 `"yellow/background.png"` 或 `"cartoon/background.png"）`。

2. **初始載入**  
   - 在 GameScreen 的建構子中，預設主題設定為 `"yellow"`，因此初始載入的資源來自於 `"yellow"` 資料夾。

3. **主題切換**  
   - 在 render 方法中，模擬在 5 秒後切換主題。當條件滿足時，調用 `setTheme("cartoon")` 並清除已載入的資源（或根據需要重載），然後再次調用 `getTexture()` 載入新的資源。

這樣的設計可以讓你在全局層面上輕鬆切換主題，而 XML 定義則保持不變，實現了資料與風格的分離。這對於 UI 系統和遊戲內的主題動態切換非常有幫助。
