以下提供一個示範做法，展示如何在「不動到原有的 internal 路徑邏輯」前提下，動態切換至另一個（或多個）主題路徑。核心概念是：**若當前主題為預設（例如 `"default"`），就直接使用原路徑；若當前主題不是預設，就在載入時自動為原路徑加上「主題」的前綴**，並嘗試載入該檔案。  

---

## 路徑結構範例

假設原本的預設資源路徑結構如下（預設主題）：

```
assets/
 ┣ font/
 ┣ i18n/
 ┣ picture/
 │   ┣ buttonicon/
 │   ┣ common/
 │   ┗ supplyitem/
 │       ┣ bag_coffee.png
 │       ┣ bag_tea.png
 │       ┣ ...
 ┗ ...
```

- 預設情況下，你的程式直接用 `Gdx.files.internal("picture/supplyitem/bag_coffee.png")` 之類的方式載入。  

現在想要增加一個「gold」主題，並且在不動到現有程式邏輯的基礎上，把 gold 主題的圖片放在 `theme/gold/` 裡面，整體路徑可能像這樣：

```
assets/
 ┣ theme/
 │   ┗ gold/
 │       ┗ picture/
 │           ┗ supplyitem/
 │               ┣ bag_coffee.png  (gold 風格的替換圖)
 │               ┣ bag_tea.png
 │               ┣ ...
 ┣ picture/
 │   ┗ supplyitem/
 │       ┣ bag_coffee.png  (default 風格)
 │       ┣ ...
 ┗ ...
```

如此一來：
- **預設**：載入 `picture/supplyitem/bag_coffee.png`  
- **gold**：載入 `theme/gold/picture/supplyitem/bag_coffee.png`  

> 若找不到 `theme/gold/picture/supplyitem/bag_coffee.png`，你也可以選擇自動回退到預設資源，避免檔案缺失造成錯誤。

---

## ThemeResourceManager 示範程式

以下程式示範如何以一個簡單的「主題資源管理器」來實現上述邏輯。這裡仍然使用 libGDX 的 `AssetManager` 來管理資源，但你也可以自行用 `Texture` 載入或其他方式，關鍵在「載入時動態組合路徑」。

```java
public class ThemeResourceManager {
    // 預設主題常數，可自行命名
    private static final String DEFAULT_THEME = "default";
    
    // 這裡示範存放主題檔案的資料夾為 "theme/"，可自行調整
    private static final String THEME_FOLDER  = "theme/";
    
    // 單例實例
    private static ThemeResourceManager instance;
    
    // libgdx 的資源管理器
    private AssetManager assetManager;
    
    // 當前主題
    private String currentTheme;

    // 私有建構子
    private ThemeResourceManager() {
        assetManager = new AssetManager();
        // 預設主題：default
        currentTheme = DEFAULT_THEME;
    }

    // 取得單例
    public static ThemeResourceManager getInstance() {
        if (instance == null) {
            instance = new ThemeResourceManager();
        }
        return instance;
    }

    /**
     * 設定主題。
     * @param theme 例如 "gold" 或 "default"。
     */
    public void setTheme(String theme) {
        if (!theme.equals(currentTheme)) {
            currentTheme = theme;
            // 這裡簡單做法：直接清空所有已載入的資源，以免舊圖留在記憶體裡
            // 若要更細緻管理，可自行設計不清空全部，或做差異載入
            assetManager.clear();
        }
    }

    /**
     * 根據「原路徑」取得對應主題下的 Texture。
     * @param originalPath 例如 "picture/supplyitem/bag_coffee.png"
     */
    public Texture getTexture(String originalPath) {
        // 先解析最終路徑
        String pathToLoad = resolvePath(originalPath);
        
        // 若尚未載入，就先行載入
        if (!assetManager.isLoaded(pathToLoad, Texture.class)) {
            assetManager.load(pathToLoad, Texture.class);
            // 同步載入（若要非同步，可用 finishLoading() 或 checkLoaded()）
            assetManager.finishLoadingAsset(pathToLoad);
        }
        
        return assetManager.get(pathToLoad, Texture.class);
    }

    /**
     * 組合最終路徑的核心方法：
     * - 若當前主題是 "default"，直接回傳原路徑
     * - 若非 "default"，嘗試 "theme/主題名/ + 原路徑"
     *   - 亦可加上檔案存在判斷，若不存在再回退到原路徑
     */
    private String resolvePath(String originalPath) {
        // 如果是預設主題，直接使用原路徑
        if (currentTheme.equals(DEFAULT_THEME)) {
            return originalPath;
        } else {
            // 組合出 "theme/gold/picture/supplyitem/xxx.png" 這樣的路徑
            String themedPath = THEME_FOLDER + currentTheme + "/" + originalPath;
            
            // 如果想做「檔案不存在就回退」，可以這樣：
            if (!Gdx.files.internal(themedPath).exists()) {
                // 若檔案不存在，就回退到原路徑 (可視需求而定)
                return originalPath;
            }
            
            return themedPath;
        }
    }
}
```

### 用法示例

1. **初始化 / 預設主題**
   ```java
   // 預設使用 default 主題
   ThemeResourceManager.getInstance().setTheme("default");
   ```

2. **載入圖片**  
   假設在你的程式中，原先都是用 `picture/supplyitem/bag_coffee.png` 作為路徑：
   ```java
   // 不用修改原本邏輯，只要呼叫 getTexture("picture/supplyitem/bag_coffee.png")
   Texture coffeeTex = ThemeResourceManager.getInstance().getTexture("picture/supplyitem/bag_coffee.png");
   ```

3. **切換主題**  
   例如，你想在某個時機切換到「gold」主題：
   ```java
   ThemeResourceManager.getInstance().setTheme("gold");
   // 之後再取同樣路徑，就會自動載入 "theme/gold/picture/supplyitem/bag_coffee.png" (若存在)
   Texture coffeeTexGold = ThemeResourceManager.getInstance().getTexture("picture/supplyitem/bag_coffee.png");
   ```

> 若 `theme/gold/picture/supplyitem/bag_coffee.png` 不存在，則會 fallback 到原始路徑（依照 `resolvePath()` 中的判斷來決定），這樣可以避免主題檔案不齊全導致程式出錯。

---

## 優化要點

1. **檔案回退機制**  
   - 在 `resolvePath()` 裡可以檢查 `themedPath` 是否存在，不存在就自動回退到 `originalPath`。這對於部分圖片未提供特別「gold 版本」時很實用。

2. **更細緻的載入卸載**  
   - 目前示範在 `setTheme()` 時直接 `assetManager.clear()`，會把所有載入的資源都卸載。若想要更精細，可以只卸載與舊主題相關的資源，或做差異化載入。

3. **異步載入**  
   - 若資源檔案較大，建議使用 `assetManager.load(...)` + `assetManager.update()` 或 `assetManager.finishLoading()` 的非同步方式，並在載入完成後再通知畫面更新，避免卡頓。

4. **支援多種主題**  
   - 只要維持相同結構（`theme/<主題名>/picture/...`），你可以很輕鬆地擴充更多主題（`silver`, `cartoon` 等）。

---

這樣一來，你就能**在不大改原有路徑的情況下**，同時保留對「預設」與「自定主題」的載入支援。程式裡大部分只要呼叫 `getTexture("picture/xxx.png")` 就好；要切換主題時，只需 `setTheme("gold")`，後續同樣的路徑就會自動帶到 `theme/gold/picture/xxx.png` 去載入。如此便能輕鬆實現多套風格並行。
