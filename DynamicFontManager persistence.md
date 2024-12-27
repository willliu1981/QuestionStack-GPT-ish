為您的 `DynamicFontManager` 添加一個持久化存取方法，可以將字型的字符集和生成的字型紋理保存到本地存儲。以下是實現方式：

---

### **實現持久化存取**

1. **新增保存與加載方法**：
   - 將字符集存儲為一個文本文件。
   - 將生成的 BitmapFont 與其紋理保存到本地。

2. **文件結構建議**：
   - 每個 `fontId` 對應一個目錄。
   - 目錄內保存：
     - **字符集文件**（`characters.txt`）
     - **FNT 文件**（`font.fnt`）
     - **紋理文件**（`font.png` 或其他格式）

---

### **新增持久化相關方法**

#### **保存方法**
將字型的字符集和生成的 BitmapFont 保存到本地：
```java
public void saveFontToDisk(String fontId, String fontName, int size) {
    String directoryPath = Gdx.files.local("fonts/" + fontId).path();
    Gdx.files.local(directoryPath).mkdirs();

    // 保存字符集
    CharactersSet charactersSet = charactersSetMap.get(fontId);
    if (charactersSet != null) {
        FileHandle charFile = Gdx.files.local(directoryPath + "/characters.txt");
        charFile.writeString(charactersSet.getCharacters(), false);
    }

    // 保存 BitmapFont
    ReferenceCountedFont referencedFont = fontCache.get(fontId).get(new FontKey(fontName, size));
    if (referencedFont != null && referencedFont.getFont() != null) {
        referencedFont.getFont().getData().writeFont(Gdx.files.local(directoryPath + "/font.fnt"), 
                                                     Gdx.files.local(directoryPath + "/font.png"));
    }
}
```

#### **加載方法**
從本地存儲加載字符集與 BitmapFont：
```java
public void loadFontFromDisk(String fontId, String fontName, int size) {
    String directoryPath = Gdx.files.local("fonts/" + fontId).path();

    // 加載字符集
    FileHandle charFile = Gdx.files.local(directoryPath + "/characters.txt");
    if (charFile.exists()) {
        String characters = charFile.readString();
        addCharacters(fontId, characters);
    }

    // 加載 BitmapFont
    FileHandle fontFile = Gdx.files.local(directoryPath + "/font.fnt");
    if (fontFile.exists()) {
        BitmapFont font = new BitmapFont(fontFile, Gdx.files.local(directoryPath + "/font.png"), false);
        fontCache.computeIfAbsent(fontId, k -> new HashMap<>())
                 .put(new FontKey(fontName, size), new ReferenceCountedFont(font));
    }
}
```

---

### **改寫 `getFont` 方法**
在加載字型前，檢查是否有持久化數據：
```java
public BitmapFont getFont(String fontId, String fontName, int size) {
    boolean isUpdated;
    CharactersSet charactersSet = charactersSetMap.get(fontId);
    if (charactersSet == null) {
        charactersSet = new CharactersSet();
        charactersSetMap.put(fontId, charactersSet);
        isUpdated = charactersSet.isUpdated = true;
    } else {
        isUpdated = charactersSet.isUpdated;
    }

    FontKey key = new FontKey(fontName, size);
    if (!fontCache.containsKey(fontId) || !fontCache.get(fontId).containsKey(key)) {
        // 嘗試從本地加載
        loadFontFromDisk(fontId, fontName, size);
    }

    if (isUpdated || !fontCache.get(fontId).containsKey(key)) {
        regenerateFont(fontId, fontName, size);
        saveFontToDisk(fontId, fontName, size); // 保存新生成的字型
        charactersSet.isUpdated = false;
    }

    Map<FontKey, ReferenceCountedFont> fontMap = fontCache.get(fontId);

    ReferenceCountedFont referencedFont = fontMap.get(key);
    if (referencedFont == null) {
        throw new IllegalStateException("Font generation failed for size: " + size);
    }
    referencedFont.acquire(); // 增加引用
    Logger.debug(this.getClass().getSimpleName(), "Font acquired. Current references : " +
        "fontId= " + fontId + ", fontName= " + fontName + ", fontSize= " + size + ", references count=" + referencedFont.referenceCount);

    return referencedFont.getFont();
}
```

---

### **注意事項**
1. **存儲位置**：
   - 在 Android 上，存放於 `context.getFilesDir()`，即 LibGDX 的 `Gdx.files.local`。
   - 在 Desktop 上，存放於應用程式本地文件夾。

2. **文件同步**：
   - 每次更新字符集或字型時，記得調用 `saveFontToDisk` 方法。

3. **首次啟動的處理**：
   - 若本地沒有字型數據，則正常生成後進行保存。

---

### **測試範例**
生成並持久化字型：
```java
DynamicFontManager fontManager = new DynamicFontManager(fontPathMap);
fontManager.addCharacters("default", "Hello, 世界!");
BitmapFont font = fontManager.getFont("default", "SourceHanSans", 32);
fontManager.saveFontToDisk("default", "SourceHanSans", 32);
```

從本地加載字型：
```java
fontManager.loadFontFromDisk("default", "SourceHanSans", 32);
BitmapFont font = fontManager.getFont("default", "SourceHanSans", 32);
```

---

這樣，您的字型可以在應用程序中以持久化的方式存取，並在首次生成後提高後續啟動的效率。
