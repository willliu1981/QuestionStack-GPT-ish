以下是針對 Android 與 Desktop 讀取 `assets` 資源時，使用 `Gdx.files.internal` 和 `Gdx.files.local` 的差異與實踐方式整理：

---

## 1. Android 與 Desktop 讀取方式

- **Android**  
  - 通常使用 `Gdx.files.internal("xxx")` 來讀取 APK 內的 `assets` 檔案。  
  - 不需要在路徑前面加上 `"assets/"`，因為 APK 內已自動對應到 `assets` 資料夾。  
  - `internal` 檔案在 Android 上是唯讀（無法直接 move 或編輯）。  

- **Desktop**  
  - 如果在 **Android Studio** 的 Desktop 模式下直接執行，通常需要在程式碼中加上 `"assets/"`，例如 `Gdx.files.internal("assets/picture/xxx.png")`，才能找到對應檔案。  
  - 或者可以把資源輸出到不含 `assets` 資料夾的路徑（詳見下方第 3 點），就能直接使用 `Gdx.files.internal("picture/xxx.png")`。  
  - `internal` 在桌面上多半是唯讀（設計概念），但實際上若檔案系統允許，還是能夠做某些操作；`local` 則預設用來做讀寫操作。

---

## 2. `internal` vs. `local` 差異

- **`internal`**  
  - 設計上用於讀取「唯讀」的資源檔案。  
  - 在 Android 上指向 APK 內的 `assets`，在桌面上預設指向與 jar 同層的 `assets` 資料夾（或你自訂的輸出位置）。  
  - 可以做 `copyTo`，但無法 `moveTo` 或直接修改檔案（Android 上絕對不行，桌面上有時行得通但不建議）。

- **`local`**  
  - 設計上用於「讀寫」檔案，如存檔、快取等。  
  - 在 Android 上對應到 `/data/data/<你的包名>/files/` 目錄。  
  - 在桌面上則相對於執行時的工作目錄，可讀寫。

---

## 3. 輸出 Desktop jar 並複製 `assets` 的作法

### 預設指令
- 透過 Gradle 執行：  
  ```bash
  ./gradlew lwjgl3:dist
  ```
  會產生 `jar` 檔，並放在 `lwjgl3/build/lib` 或 `lwjgl3/build/dist` 之類的目錄下（視專案設定而定）。

### 自訂 Copy 任務

- **若想保持 `assets` 資料夾結構**  
  ```groovy
  task copyAssets(type: Copy) {
      from rootProject.file('assets')
      into "${project.layout.buildDirectory.get().asFile.absolutePath}/lib/assets"
  }
  jar.finalizedBy(copyAssets)
  ```
  - 最終結果：  
    ```
    build/lib/
    ├─ <your-jar>.jar
    └─ assets/
       └─ picture/
       └─ skin/
       └─ ...
    ```
  - 此時桌面端讀取檔案路徑可寫成 `Gdx.files.internal("assets/picture/xxx.png")`（如果程式是這樣撰寫）。

- **若想去掉 `assets/` 這層**  
  ```groovy
  task copyAssets(type: Copy) {
      from rootProject.file('assets')
      // 不要再多包一層 "assets"
      into "${project.layout.buildDirectory.get().asFile.absolutePath}/lib"
  }
  jar.finalizedBy(copyAssets)
  ```
  - 最終結果：  
    ```
    build/lib/
    ├─ <your-jar>.jar
    ├─ picture/
    ├─ skin/
    ├─ xml/
    └─ ...
    ```
  - 此時若程式碼直接使用 `Gdx.files.internal("picture/xxx.png")`，就可以在桌面端正確讀取，**同時** Android 端也可以用相同路徑（因為 Android 的 `assets` 也不需再手動加 `"assets/"`）。

---

## 4. 統一程式碼路徑的好處

- 若你不想在程式碼中判斷 `ApplicationType`，可以在**桌面輸出時**將檔案結構調整成與 Android 相同的邏輯，省去加 `assets/` 這層。  
- 這樣在 Android 與 Desktop 都可以直接使用：
  ```java
  Gdx.files.internal("picture/xxx.png");
  ```
  而不必在 Desktop 特地加上 `"assets/"`。

---

### 總結

- **Android**：`internal` 不用加 `"assets/"`，對應 APK 內資源。  
- **Desktop**：可透過自訂複製任務，將輸出檔案結構調整成與 Android 一致（不含 `assets/` 目錄），便能直接用相同的 `internal("picture/xxx.png")`。  
- **`local`**：主要用於可讀寫的檔案操作，Android 與 Desktop 實際路徑不同，但使用方式相同。  

這樣就能讓兩個平台的程式碼保持一致。
