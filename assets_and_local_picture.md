為了實現可編輯的 `item` 圖片功能，同時支援新增和修改圖片名稱，以下是完整的解決方案：

---

### **1. 系統設計概述**
- **Assets 預設圖片**：`assets` 中存放預設圖片，這些圖片是只讀的。
- **User 資料夾**：使用者新增或修改的圖片會儲存在 `local` 目錄（`Gdx.files.local`），以便在遊戲中讀取與更新。
- **圖片名稱同步**：修改圖片名稱時，同步更新圖片路徑，讓顯示邏輯正確指向新的圖片。

---

### **2. 圖片路徑結構設計**
1. **預設圖片（只讀）**：`assets/picture/supplyitem/`
2. **使用者圖片（可編輯）**：`local/picture/supplyitem/`

例如：
```
assets/picture/supplyitem/water.png     // 預設圖片
local/picture/supplyitem/custom_water.png  // 使用者修改後的圖片
```

---

### **3. 修改 `SupplyItem` 的圖片邏輯**

#### **a. 修改 `SupplyItem` 結構**
為 `SupplyItem` 增加一個欄位來區分圖片來源（預設或自訂）。

```java
public class SupplyItem {
    private String imgName;        // 圖片名稱，不含路徑與副檔名
    private boolean isCustomImage; // 標記是否為自訂圖片

    // 取得圖片路徑
    public String getImgPath() {
        if (isCustomImage) {
            return Gdx.files.local("picture/supplyitem/" + imgName + ".png").path();
        } else {
            return Gdx.files.internal("picture/supplyitem/" + imgName + ".png").path();
        }
    }

    // 設定圖片名稱，並決定是否為自訂圖片
    public void setImgName(String imgName, boolean isCustomImage) {
        this.imgName = imgName;
        this.isCustomImage = isCustomImage;
    }
}
```

---

### **4. 實作圖片新增與修改功能**

#### **a. 新增圖片**
1. **選擇圖片檔案**：使用文件選擇器（需根據平台調整）。
2. **將圖片儲存到 `local` 目錄**：
   ```java
   public void saveCustomImage(FileHandle sourceFile, String newImageName) {
       FileHandle destination = Gdx.files.local("picture/supplyitem/" + newImageName + ".png");
       sourceFile.copyTo(destination);
   }
   ```

3. **更新 `SupplyItem` 的圖片名稱**：
   ```java
   supplyItem.setImgName("custom_water", true); // 設定為自訂圖片
   ```

---

#### **b. 修改圖片名稱**
1. **修改圖片名稱並重命名檔案**：
   ```java
   public void renameCustomImage(SupplyItem item, String newImageName) {
       if (item.isCustomImage()) {
           FileHandle oldFile = Gdx.files.local("picture/supplyitem/" + item.getImgName() + ".png");
           FileHandle newFile = Gdx.files.local("picture/supplyitem/" + newImageName + ".png");
           oldFile.moveTo(newFile);
           item.setImgName(newImageName, true); // 更新圖片名稱
       }
   }
   ```

---

### **5. 加載圖片邏輯**

當遊戲載入 `SupplyItem` 時，根據 `isCustomImage` 來決定圖片來源。

```java
Texture texture = new Texture(Gdx.files.internal(supplyItem.getImgPath()));
Image itemImage = new Image(texture);
```

---

### **6. 組合流程**
1. **預設圖片**：從 `assets` 載入，無法修改。
2. **新增圖片**：選擇圖片後儲存到 `local`，並更新 `SupplyItem` 的圖片名稱。
3. **修改圖片名稱**：直接在 `local` 資料夾中重命名檔案，並更新 `SupplyItem` 的 `imgName`。

---

### **7. 進階功能（可選）**
- **圖片格式支援**：支援 `.jpg`, `.png` 等不同格式，可在 `getImgPath()` 裡做條件判斷。
- **圖片驗證**：新增圖片時檢查檔案格式與大小，確保圖片可以被正確載入。

---

### **結論**
透過結合 `assets` 與 `local` 儲存路徑，你可以實現：
1. **預設圖片保留不變**。
2. **使用者可新增、修改圖片名稱與內容**。
3. **動態更新圖片路徑來顯示最新圖片**。

這樣設計可以確保預設資源完整，同時提供靈活的圖片編輯功能！ 🚀
