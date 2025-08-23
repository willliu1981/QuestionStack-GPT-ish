


# UIStateManager（簡化版）：參數 & 觸發器

## TL;DR
- 用 `params`（bool/int/float/string）表示持續條件；用 `trigger` 表示一次性事件。
- 任何寫入都會 `touch()` → `version`++ 與 `lastUpdatedNanos` 更新，便於髒檢與去抖。
- 以 `StateKey` 區隔不同畫面/範圍；順序有意義。

## 核心概念
- **StateKey**：`StateKey.of("settings")`、`StateKey.of("inspection", cateId)`。
- **Params**：`setBool/setInt/setFloat/setString/get*`。
- **Triggers**：`trigger/hasTrigger/poppedTrigger`（消費後即不存在）。
- **DataList/Payload**：維持既有資料掛載與任意附加物件。
- **touch()**：任何變更都會更新 `version` 與 `lastUpdatedNanos`。

## 快速範例
```java
UIStateManager stateMgr = new UIStateManager(new Object());
var k = UIStateManager.StateKey.of("settings");

// 語系切換：只刷新視圖
stateMgr.setString(k, "language", "ja");
stateMgr.setBool(k, "needsRefreshView", true);

// 一次性事件：還原預設
stateMgr.trigger(k, "reset_to_default");

// 在更新迴圈：
if (stateMgr.poppedTrigger(k, "reset_to_default")) {
    // 套用預設值，只刷新 Actor，不動資料
}

```

---
UIStateManager
```java
package idv.kuan.studio.libgdx.simpleui.runtime;

import idv.kuan.studio.libgdx.simpleui.builder.BuiltUI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UIStateManager（簡化版）：以「參數（params）」與「觸發器（triggers）」管理每個畫面/範圍（StateKey）的 UI 狀態。
 *
 * <p><b>核心概念</b></p>
 * <ul>
 *   <li><b>StateKey</b>：可變長度複合鍵（順序有意義）。同一鍵代表同一「畫面/範圍」的狀態袋。</li>
 *   <li><b>Params</b>：一般狀態值（bool/int/float/string），適合「持續條件」，例如 language、needsRefreshView。</li>
 *   <li><b>Triggers</b>：一次性事件（邊緣），以 Set 儲存；<code>trigger()</code> 設置事件，<code>poppedTrigger()</code> 消費後即不存在。</li>
 *   <li><b>touch()</b>：任何寫入（UI/資料/參數/觸發器）都會遞增 <code>version</code> 並更新 <code>lastUpdatedNanos</code>，
 *       供外部做髒檢、快取失效與去抖（debounce）。</li>
 *   <li><b>DataList / Payload</b>：保留清單資料與任意附加物件（如查詢條件）。</li>
 * </ul>
 *
 * <p><b>典型流程</b></p>
 * <ol>
 *   <li>以 <code>StateKey.of(...)</code> 取得/建立狀態袋：<code>UIState s = stateFor(key)</code>。</li>
 *   <li>寫入參數：<code>setBool(key,"needsRefreshView",true)</code> 或 <code>setString(key,"language","ja")</code>。</li>
 *   <li>一次性事件：<code>trigger(key,"reset_to_default")</code>；在更新迴圈用 <code>poppedTrigger(key,"reset_to_default")</code> 處理一次。</li>
 *   <li>髒檢：比對 <code>version</code>；或以 <code>now - lastUpdatedNanos &gt;= quiet</code> 去抖後再刷新 UI。</li>
 * </ol>
 *
 * <p><b>遷移建議（自 needsRecreate → params/trigger）</b></p>
 * <ul>
 *   <li>原 <code>needsRecreate</code> → 以 <code>setBool(key,"needsRefreshView",true)</code> 代表「僅刷新視圖」。</li>
 *   <li>「還原預設」→ 以 <code>trigger(key,"reset_to_default")</code> 實作一次性動作（只刷新 Actor，不動資料）。</li>
 * </ul>
 *
 * <p><b>執行緒與效能</b></p>
 * <ul>
 *   <li>內部採用 ConcurrentHashMap/volatile；建議僅在 LibGDX 渲染執行緒寫入，跨緒請排隊投遞。</li>
 *   <li>查表均為攤銷 O(1)；大量 scope 請定期 <code>reset/resetByPrefix</code> 釋放。</li>
 * </ul>
 *
 * <p><b>命名建議</b>：避免把管理器變數命名為 <code>uiState</code> 以免與內部類 <code>UIState</code> 混淆。建議 <code>stateMgr</code> 或腳本別名 <code>uiStateMgr()</code>。</p>
 */
public final class UIStateManager {
    /**
     * 全部狀態（thread-safe）
     */
    private final Map<StateKey, UIState> STATES = new ConcurrentHashMap<>();

    public UIStateManager(Object context) {
        Objects.requireNonNull(context, "context must not be null");
    }

    /* ----------------------------- 狀態存取 ----------------------------- */
    public UIState stateFor(StateKey key) {
        return STATES.computeIfAbsent(key, UIState::new);
    }

    public UIState getState(StateKey key) {
        return STATES.get(key);
    }

    /* ----------------------------- UI 存取 ----------------------------- */
    public void registerUI(StateKey key, BuiltUI ui) {
        stateFor(key).setUi(ui);
    }

    public BuiltUI getUI(StateKey key) {
        UIState s = STATES.get(key);
        return s != null ? s.getUI() : null;
    }

    /* ----------------------------- 清單資料 ----------------------------- */
    public void setDataList(StateKey key, List<Map<String, Object>> list) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> m : list) copy.add(new HashMap<>(m));
        stateFor(key).setDataList(Collections.unmodifiableList(copy));
    }

    public List<Map<String, Object>> getDataList(StateKey key) {
        UIState s = STATES.get(key);
        return s != null ? s.getDataList() : Collections.emptyList();
    }

    public Object getData(StateKey key, String itemId, String dataKey) {
        UIState s = STATES.get(key);
        if (s == null) throw new RuntimeException("getData: no state for " + key);
        Map<String, Object> map = s.getItem(itemId);
        if (map == null) throw new RuntimeException("getData: no itemId=" + itemId);
        if (!map.containsKey(dataKey)) throw new RuntimeException("getData: no dataKey=" + dataKey);
        return map.get(dataKey);
    }

    public Object getData(StateKey key, String itemId, String dataKey, Object def, boolean putIfAbsent) {
        List<Map<String, Object>> list = getDataList(key);
        Map<String, Object> map = list.stream()
            .filter(m -> itemId.equals(String.valueOf(m.get("id")))).findFirst()
            .orElseThrow(() -> new RuntimeException("getData: no itemId=" + itemId));
        if (!map.containsKey(dataKey)) {
            if (putIfAbsent) map.put(dataKey, def);
            return def;
        }
        return map.get(dataKey);
    }

    public void setData(StateKey key, String itemId, String dataKey, Object value) {
        List<Map<String, Object>> list = getDataList(key);
        Map<String, Object> map = list.stream()
            .filter(m -> itemId.equals(String.valueOf(m.get("id")))).findFirst()
            .orElseThrow(() -> new RuntimeException("setData: no itemId=" + itemId));
        map.put(dataKey, value);
    }

    /* ----------------------------- Payload ----------------------------- */
    public void setPayload(StateKey key, Object payload) {
        stateFor(key).setPayload(payload);
    }

    public <T> T getPayload(StateKey key, Class<T> type) {
        UIState s = STATES.get(key);
        return (s == null || s.getPayload() == null) ? null : type.cast(s.getPayload());
    }

    /* ----------------------------- 參數（狀態） ----------------------------- */
    public void setParam(StateKey key, String name, Object value) {
        stateFor(key).setParam(name, value);
    }

    public <T> T getParam(StateKey key, String name, Class<T> type, T def) {
        return stateFor(key).getParam(name, type, def);
    }

    public void removeParam(StateKey key, String name) {
        stateFor(key).removeParam(name);
    }

    // 型別便捷
    public void setBool(StateKey k, String n, boolean v) {
        setParam(k, n, v);
    }

    public boolean getBool(StateKey k, String n) {
        Boolean b = getParam(k, n, Boolean.class, false);
        return b != null && b;
    }

    public void setInt(StateKey k, String n, int v) {
        setParam(k, n, v);
    }

    public int getInt(StateKey k, String n, int def) {
        Integer v = getParam(k, n, Integer.class, def);
        return v != null ? v : def;
    }

    public void setFloat(StateKey k, String n, float v) {
        setParam(k, n, v);
    }

    public float getFloat(StateKey k, String n, float def) {
        Float v = getParam(k, n, Float.class, def);
        return v != null ? v : def;
    }

    public void setString(StateKey k, String n, String v) {
        setParam(k, n, v);
    }

    public String getString(StateKey k, String n, String def) {
        String v = getParam(k, n, String.class, def);
        return v != null ? v : def;
    }

    /* ----------------------------- 觸發器（一次性） ----------------------------- */
    public void trigger(StateKey key, String name) {
        stateFor(key).setTrigger(name);
    }

    public boolean hasTrigger(StateKey key, String name) {
        return stateFor(key).hasTrigger(name);
    }

    public boolean poppedTrigger(StateKey key, String name) {
        return stateFor(key).consumeTrigger(name);
    }

    /* ----------------------------- 重置 ----------------------------- */
    public void reset(StateKey... keys) {
        for (StateKey k : keys) STATES.remove(k);
    }

    public void resetByPrefix(Object... prefixParts) {
        List<StateKey> del = new ArrayList<>();
        forEachKeyMatchingPrefix(prefixParts, k -> {
            del.add(k);
            return true;
        });
        del.forEach(STATES::remove);
    }

    public void resetAll() {
        STATES.clear();
    }

    /* ----------------------------- 工具 ----------------------------- */
    public List<StateKey> keys() {
        return new ArrayList<>(STATES.keySet());
    }

    private void forEachKeyMatchingPrefix(Object[] prefix, java.util.function.Predicate<StateKey> action) {
        for (StateKey k : STATES.keySet())
            if (k.startsWith(prefix)) {
                if (!action.test(k)) break;
            }
    }

    /* ----------------------------- 內部型別 ----------------------------- */
    public static final class UIState {
        private final StateKey key;
        private volatile BuiltUI ui;
        private volatile List<Map<String, Object>> dataList = Collections.emptyList();
        private volatile Map<String, Map<String, Object>> byId = Map.of();
        private volatile Object payload;

        private final Map<String, Object> params = new ConcurrentHashMap<>();
        private final Set<String> triggers = ConcurrentHashMap.newKeySet();

        private volatile long version = 0L;
        private volatile long lastUpdatedNanos = System.nanoTime();

        UIState(StateKey key) {
            this.key = key;
        }

        public BuiltUI getUI() {
            return ui;
        }

        public void setUi(BuiltUI ui) {
            this.ui = ui;
            touch();
        }

        public List<Map<String, Object>> getDataList() {
            return dataList;
        }

        public void setDataList(List<Map<String, Object>> list) {
            this.dataList = list;
            Map<String, Map<String, Object>> idx = new HashMap<>();
            for (Map<String, Object> m : list) {
                Object id = m.get("id");
                if (id != null) idx.put(String.valueOf(id), m);
            }
            this.byId = Collections.unmodifiableMap(idx);
            touch();
        }

        public Map<String, Object> getItem(String id) {
            return byId.get(id);
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
            touch();
        }

        // params
        public void setParam(String name, Object value) {
            if (value == null) params.remove(name);
            else params.put(name, value);
            touch();
        }

        @SuppressWarnings("unchecked")
        public <T> T getParam(String name, Class<T> type, T def) {
            Object v = params.get(name);
            return type.isInstance(v) ? (T) v : def;
        }

        public void removeParam(String name) {
            params.remove(name);
            touch();
        }

        // triggers
        public UIState setTrigger(String name) {
            triggers.add(name);
            touch();
            return this;
        }

        public boolean consumeTrigger(String name) {
            boolean r = triggers.remove(name);
            if (r) touch();
            return r;
        }

        public boolean hasTrigger(String name) {
            return triggers.contains(name);
        }

        public long version() {
            return version;
        }

        public long lastUpdatedNanos() {
            return lastUpdatedNanos;
        }

        private void touch() {
            version++;
            lastUpdatedNanos = System.nanoTime();
        }
    }

    /**
     * 可變長度複合鍵（順序有意義）
     */
    public static final class StateKey {
        private final List<Object> parts;
        private final int hash;

        private StateKey(List<Object> parts) {
            this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
            this.hash = Objects.hash(this.parts.toArray());
        }

        public static StateKey of(Object... parts) {
            if (parts == null || parts.length == 0)
                throw new IllegalArgumentException("parts must not be empty");
            return new StateKey(Arrays.asList(parts));
        }

        public boolean startsWith(Object... prefixParts) {
            if (prefixParts == null || prefixParts.length == 0) return true;
            if (prefixParts.length > parts.size()) return false;
            for (int i = 0; i < prefixParts.length; i++)
                if (!Objects.equals(parts.get(i), prefixParts[i])) return false;
            return true;
        }

        public List<Object> parts() {
            return parts;
        }

        @Override
        public boolean equals(Object o) {
            return (this == o) || (o instanceof StateKey && parts.equals(((StateKey) o).parts));
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "StateKey" + parts;
        }
    }
}



```

