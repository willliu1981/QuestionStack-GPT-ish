package idv.kuan.game.libgdx.xmlactor.demo.tool;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.XmlReader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import idv.kuan.game.libgdx.xmlactor.demo.api.Logger;
import idv.kuan.game.libgdx.xmlactor.demo.tool.exception.ThemeAlreadySetException;

/**
 * ResourceManager 是一個統一的主題與多語系資源管理工具。
 * <p>
 * 支援以下功能：
 * <ul>
 *     <li>主題圖像與色彩路徑轉換（自動查找 theme 路徑）</li>
 *     <li>從 XML 或資源檔載入 color palette</li>
 *     <li>支援 @color/@string 的語法，並自動解析 fallback 資源</li>
 *     <li>支援主題化多語系文字檔（strings_xx.properties）</li>
 * </ul>
 * <p>
 * ✅ 使用建議順序：
 * <pre>
 * ResourceManager.loadI18nStrings("i18n/strings", Locale.getDefault());
 * ResourceManager.loadFallbackStrings("xml/strings.xml");
 * ResourceManager.applyTheme("cyberpunk", "theme");
 * </pre>
 * <p>
 * ✅ 支援格式化字串 {@code @string/greeting} → {@code Hello {0}}：
 * <pre>
 * String result = ResourceManager.resolveText("@string/greeting", "Kuan");
 * </pre>
 */
public class ResourceManager {

    // === 主題與資源管理 ===

    private static final String NO_THEME = "";
    private static final String NO_THEME_FOLDER = "";
    private static String currentTheme = NO_THEME;
    private static String currentThemeFolder = NO_THEME_FOLDER;
    private static boolean isThemeFolderSet = false;

    private static final AssetManager assetManager = new AssetManager();
    private static final Map<String, Map<String, Color>> themeColorMap = new HashMap<>();
    private static final Color DEFAULT_COLOR = Color.WHITE.cpy();

    /**
     * 設定主題名稱（影響圖像與色彩資源路徑）。
     *
     * @param theme 主題名稱，例如 "default"、"cyberpunk"
     */
    public static void setActiveColorTheme(String theme) {
        if (theme == null || theme.isEmpty()) {
            theme = NO_THEME;
        }
        if (!theme.equals(currentTheme)) {
            currentTheme = theme;
            assetManager.clear();
        }
    }

    /**
     * 設定主題根資料夾（只能設定一次）。
     *
     * @param folder 主題資料夾路徑，例如 "theme"
     * @throws ThemeAlreadySetException 若重複設定主題資料夾會拋出例外
     */
    public static void setThemeFolder(String folder) {
        if (isThemeFolderSet) {
            throw new ThemeAlreadySetException("Theme folder already set", 1001);
        }
        currentThemeFolder = (folder == null || folder.isEmpty()) ? NO_THEME_FOLDER : folder;
        isThemeFolderSet = true;
    }

    /**
     * 根據原始資源路徑取得最終圖像資源（主題優先、找不到回退）。
     *
     * @param originalPath 原始圖像路徑，例如 picture/icon.png
     * @return 對應的 Texture 實例
     */
    public static Texture getTexture(String originalPath) {
        String pathToLoad = resolveThemePath(originalPath);
        if (!assetManager.isLoaded(pathToLoad, Texture.class)) {
            assetManager.load(pathToLoad, Texture.class);
            assetManager.finishLoadingAsset(pathToLoad);
        }
        return assetManager.get(pathToLoad, Texture.class);
    }

    private static String resolveThemePath(String originalPath) {
        if (NO_THEME.equals(currentTheme) || NO_THEME_FOLDER.equals(currentThemeFolder)) {
            return originalPath;
        }
        String themedPath = currentThemeFolder + "/" + currentTheme + "/" + originalPath;
        return Gdx.files.internal(themedPath).exists() ? themedPath : originalPath;
    }

    /**
     * 載入指定主題的 color palette（來自 XML 檔）。
     *
     * @param path      XML 路徑
     * @param themeName 主題名稱
     */
    public static void loadColorsFromXml(String path, String themeName) {
        loadColorsFromXml(Gdx.files.internal(path), themeName);
    }

    public static void loadColorsFromXml(FileHandle file, String themeName) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            XmlReader reader = new XmlReader();
            XmlReader.Element root = reader.parse(file);
            Map<String, Color> colorMap = new HashMap<>();
            for (XmlReader.Element colorElem : root.getChildrenByName("color")) {
                String id = colorElem.getAttribute("name", null);
                String value = colorElem.getAttribute("value", null);
                if (value == null) {
                    value = colorElem.getText();
                }
                if (id != null && value != null) {
                    try {
                        colorMap.put(id, Color.valueOf(value));
                    } catch (Exception e) {
                        Logger.error("ColorParse", "Invalid color: " + value);
                    }
                }
            }
            themeColorMap.put(themeName, colorMap);
        } catch (Exception e) {
            Logger.error("ColorXML", "Error loading color xml");
        }
    }

    /**
     * 自動依主題路徑載入 colors.xml。
     */
    public static void loadThemeColorsFromValuesFolder() {
        if (currentThemeFolder.isEmpty() || currentTheme.isEmpty()) {
            return;
        }
        String path = currentThemeFolder + "/" + currentTheme + "/values/color.xml";
        FileHandle file = Gdx.files.internal(path);
        if (file.exists()) {
            loadColorsFromXml(file, currentTheme);
        } else {
            Logger.error(ResourceManager.class.getSimpleName(), "[Theme] Color XML not found for theme: " + currentTheme);
        }
    }

    /**
     * 套用完整主題設定，會設定主題名稱與資料夾並載入主題顏色與語系。
     *
     * @param themeName       主題名稱
     * @param themeRootFolder 主題根路徑
     */
    public static void applyTheme(String themeName, String themeRootFolder) {
        setActiveColorTheme(themeName);
        setThemeFolder(themeRootFolder);
        loadThemeColorsFromValuesFolder();
        loadLocalizedStringsFromTheme();
    }

    /**
     * 根據指定顏色 ID 取得 Color 實例（主題優先）。
     *
     * @param id 顏色 ID
     * @return 對應的 Color 物件，找不到則回傳白色
     */
    public static Color getColor(String id) {
        Map<String, Color> map = themeColorMap.get(currentTheme);
        return (map != null && map.containsKey(id)) ? map.get(id) : DEFAULT_COLOR;
    }

    /**
     * 解析顏色字串，可支援 @color/ID、#hex、r,g,b,a 格式。
     *
     * @param rawColor 顏色描述字串
     * @return 對應的 Color 物件，無效則回傳白色
     */
    public static Color resolveColor(String rawColor) {
        if (rawColor == null) {
            return DEFAULT_COLOR;
        }

        // 若為 @color/id 形式
        if (rawColor.startsWith("@color/")) {
            return getColor(rawColor.substring(7));
        }

        // 嘗試解析為十六進位顏色
        try {
            return Color.valueOf(rawColor);
        } catch (Exception ignored) {
        }

        // 嘗試解析為 r,g,b,a 形式
        try {
            String[] parts = rawColor.split(",");
            if (parts.length >= 3) {
                float r = Float.parseFloat(parts[0]);
                float g = Float.parseFloat(parts[1]);
                float b = Float.parseFloat(parts[2]);
                float a = parts.length >= 4 ? Float.parseFloat(parts[3]) : 1f;
                return new Color(r, g, b, a);
            }
        } catch (Exception ignored) {
        }

        return DEFAULT_COLOR;
    }


    // === 多語系與 fallback 處理 ===
    private static I18NBundle bundle;
    private static final Map<String, String> fallbackStrings = new HashMap<>();

    /**
     * 載入共用語系文字（使用 LibGDX I18NBundle）
     *
     * @param basePath 資源基底路徑
     * @param locale   語系區域
     */
    public static void loadI18nStrings(String basePath, Locale locale) {
        bundle = I18NBundle.createBundle(Gdx.files.internal(basePath), locale);
    }

    /**
     * 載入當前主題對應的語系檔案（會覆蓋前面載入的語系）
     */
    public static void loadLocalizedStringsFromTheme() {
        if (currentThemeFolder.isEmpty() || currentTheme.isEmpty()) {
            return;
        }
        String lang = Locale.getDefault().toString(); // e.g., zh_TW
        String path = currentThemeFolder + "/" + currentTheme + "/lang/strings_" + lang + ".properties";
        FileHandle file = Gdx.files.internal(path);
        if (file.exists()) {
            bundle = I18NBundle.createBundle(file);
        }
    }

    /**
     * 載入 fallback 字串，用於找不到語系 key 時兜底。
     *
     * @param path XML 檔案路徑
     */
    public static void loadFallbackStrings(String path) {
        loadFallbackStrings(Gdx.files.internal(path));
    }

    /**
     * 載入 fallback 字串（XML）
     *
     * @param file XML 檔案 FileHandle
     */
    public static void loadFallbackStrings(FileHandle file) {
        try {
            XmlReader.Element root = new XmlReader().parse(file);
            for (XmlReader.Element elem : root.getChildrenByName("string")) {
                String id = elem.getAttribute("name", null);
                String value = elem.getAttribute("value", null);
                if (value == null) {
                    value = elem.getText();
                }
                if (id != null && value != null) {
                    fallbackStrings.put(id, value);
                }
            }
        } catch (Exception e) {
            Logger.error("StringXML", "Error loading fallback strings xml");
        }
    }

    /**
     * 同時載入共用語系與 fallback XML（常用初始化方法）
     *
     * @param basePath        共用語系路徑
     * @param fallbackXmlPath fallback XML 路徑
     * @param locale          語系區域
     */
    public static void loadAllLocalizedResources(String basePath, String fallbackXmlPath, Locale locale) {
        loadI18nStrings(basePath, locale);
        loadFallbackStrings(fallbackXmlPath);
    }

    /**
     * 取得指定 key 對應的語系字串。
     *
     * @param key 語系 key
     * @return 對應的字串，若不存在則回傳 key 本身
     */
    public static String getString(String key) {
        if (bundle != null) {
            try {
                return bundle.get(key);
            } catch (Exception ignored) {
            }
        }
        return fallbackStrings.getOrDefault(key, key);
    }

    /**
     * 解析 @string/ 語法，並支援參數格式化（{0}, {1}...）
     *
     * @param rawText 原始文字（可能為 @string/xxx）
     * @param args    格式化參數
     * @return 處理後的文字
     */
    public static String resolveText(String rawText, Object... args) {
        if (rawText == null) {
            return "";
        }
        String base = rawText.startsWith("@string/") ? getString(rawText.substring(8)) : rawText;
        try {
            if (bundle != null) {
                return bundle.format(base, args);
            } else {
                return base;
            }
        } catch (Exception e) {
            Logger.error("Format", "Error formatting string: " + rawText);
            return base;
        }
    }
}
