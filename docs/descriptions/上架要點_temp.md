結論：上架 Google Play 你需要三類東西：**商店素材**、**合規文件/聲明**、**可發佈的 AAB 與版本資訊**。以下給精簡清單與可直接套用的範本。

## 步驟

1. **準備商店素材（Store Listing）**

* App 名稱（最多 30）與簡短說明（最多 80）。
* 介紹文字（約 4,000）。
* 圖示 512×512 PNG（含透明）。
* Feature graphic 1024×500 PNG/JPG。
* 手機截圖 ≥2 張（建議 1080p；比例 16:9 或 9:16）。
* 分類：類別、聯絡資訊（email 必填、網站可選）。
* 多語在此新增（建議：zh-TW、en、ja）。

2. **合規與政策（App content）**

* **Privacy Policy 隱私權政策 URL**（必備，放你的網域；內容需與 App 實作一致）。
* **Data safety** 表單：宣告蒐集/分享/處理資料與用途。
* **內容分級（Content rating）** 問卷。
* **是否含廣告** 勾選。
* **目標對象** 與是否面向兒童（Family/兒少規範）。
* **權限聲明**：若用到敏感權限（背景定位、SMS/Call Log 等）需額外說明。
* **金融設定**：若有付費或 IAP，建立商家/付款設定與稅務資料。
* **內購商品**：建立 INAPP 項目（如 `remove_ads`）並啟用。

3. **可發佈套件與版本資訊**

* **AAB**（Android App Bundle，簽章、versionCode/Name 皆設定）。
* **目標/最低 API**：依 Play 當期要求（以 Console 指示為準）。
* **Release notes**（變更紀錄）。
* 測試管道：Internal/Closed/Open，加入測試帳號。
* **上傳符號檔**（如用到 NDK/崩潰分析）。

## 範例

### 隱私權政策（極簡可用骨架）

```
Privacy Policy – RoomPrep
We do not collect or share personal information beyond what is necessary for app functionality.

Data we process:
- Advertising: Display ads via Google AdMob. Ad providers may collect device identifiers to show ads.
- In-app purchases: Google Play Billing processes payments. We do not store payment information.
- Crash/Logs: Only non-personal technical logs for debugging (no contacts, photos, precise location).

Your Choices:
- You can opt out of personalized ads via device settings.
- Uninstall the app to stop all processing.

Contact:
support@your-domain.example
Last updated: 2025-09-03
```

> 放在你的網域，例如 `https://your-domain.example/roomprep-privacy.html`，在商店後台填入此 URL。

### Data safety（示意填答）

* **Data collection**：是（因廣告/IAP 需宣告），**Data shared**：視廣告 SDK 設定。
* **Collected categories**：Device or other IDs（廣告用途）；Purchase history（IAP）。
* **Purposes**：App functionality、Payments、Advertising。
* **Security**：傳輸加密；使用者可要求刪除（寫在隱私權政策）。

### 版本備註（Release notes）範例

```
v2.1.6
- 新增內購「移除廣告」
- 設定頁新增語言切換
- 修正偶發閃退與效能
```

## 注意事項 / 替代方案

* **API/目標版本要求**會變動；以 Play Console 當頁警示為準。
* **圖文一致**：商店截圖需真實反映 App。
* **IAP 審核**：商店描述提到「移除廣告」等承諾，App 內需有對應功能與回復購買入口。
* **隱私政策不可留白**；內容必須與實作與宣告一致，否則會被退件。
* **測試**：用 Internal testing 加入測試帳號驗證購買與退款/復原。
* 若暫無自有網域，可先用 GitHub Pages/自家站放隱私權政策，但長期建議自有網域。

需要我依你目前功能（AdMob + Billing，無定位/帳號/雲端）把 **Data safety 問卷逐題清單**整理成可直接照填的版本嗎？
