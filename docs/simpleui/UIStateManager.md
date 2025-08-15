```java
package idv.kuan.studio.libgdx.simpleui.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import idv.kuan.studio.libgdx.simpleui.builder.BuiltUI;
import idv.kuan.studio.libgdx.simpleui.context.ApplicationContext;

/**
 * 管理每個畫面的 UI 與其對應資料（清單或任意 payload）的狀態。
 *
 * 特點：
 * - 以「可變長度複合鍵」StateKey 區分不同狀態（鍵的順序有意義）
 * - 提供 needsRecreate 旗標以控制是否重建 UI
 * - 可批次標記/刪除（依鍵前綴 prefix）
 * - 提供 dataList（List<Map>）與 get/setData(itemId, dataKey, ...) 便捷介面
 * - 允許附加任意 payload（如排序條件、查詢參數）
 *
 * 基本用法：
 * <pre>{@code
 * UIStateManager sm = new UIStateManager(context);
 * UIStateManager.StateKey key = UIStateManager.StateKey.of("inspection_supplyitem", cateId);
 *
 * UIStateManager.UIState state = sm.stateFor(key);
 * if (state.getUI() == null || state.needsRecreate()) {
 *     // 1) 建 UI
 *     BuiltUI ui = builder.build(...);
 *     sm.registerUI(key, ui);
 *
 *     // 2) 放資料清單（會自動依 "id" 建索引）
 *     List<Map<String,Object>> list = service.getAllByCategory(cateId, Sort.INSPECTION);
 *     sm.setDataList(key, list);
 *
 *     // 3) 重建完成
 *     sm.setNeedsRecreate(key, false);
 * }
 *
 * // 取回 UI
 * BuiltUI ui = sm.getUI(key);
 * }</pre>
 *
 * 取得/設定單筆資料欄位：
 * <pre>{@code
 * // 讀取（若不存在給預設值，並可選擇 putIfAbsent）
 * Object qty = sm.getData(key, "room11", "quantity", 0, true); // 不存在則寫入 0 並回傳 0
 *
 * // 寫入
 * sm.setData(key, "room11", "quantity", 5);
 * }</pre>
 *
 * Payload（非清單型附加資料）：
 * <pre>{@code
 * sm.setPayload(key, new SortSpec("inspection"));
 * SortSpec spec = sm.getPayload(key, SortSpec.class);
 * }</pre>
 *
 * 重新標記與批次操作：
 * <pre>{@code
 * // 指定鍵：標記需要重建 / 移除
 * sm.setNeedsRecreate(key, true);
 * sm.reset(key);
 *
 * // 依前綴批次（例如整個 cateId 的畫面群）
 * sm.markRecreateByPrefix("inspection_supplyitem", cateId);
 * sm.resetByPrefix("inspection_supplyitem"); // 清掉該模組所有 cateId 狀態
 *
 * // 全清
 * sm.resetAll();
 * }</pre>
 *
 * 前綴比對（startsWith）說明：
 * <pre>{@code
 * // 下列兩者不是同一個 key（順序有意義）
 * StateKey.of("inspection_supplyitem", cateId)
 * StateKey.of(cateId, "inspection_supplyitem")
 *
 * // resetByPrefix("inspection_supplyitem", cateId) 只會匹配以此序列開頭的鍵
 * }</pre>
 *
 * 執行緒安全與注意事項：
 * - STATES 為 ConcurrentHashMap；keys() 回傳的是快照，不保證之後不變。
 * - setDataList 會淺拷貝清單／Map，避免外部修改影響內部索引。
 * - getData(...) 對不存在的 itemId / dataKey 會丟 RuntimeException；若要寬鬆，請改用帶 defaultValue 的版本。
 * - 請以「同一個 StateKey」貫穿 UI 建立、資料掛載與後續更新；鍵不一致會被視為不同狀態。
 */

public final class UIStateManager {

    public UIStateManager(ApplicationContext context) {
        Objects.requireNonNull(context, "Config must not be null");

        // 可用 context 作為後續擴充使用

    }

    /**
     * 全部狀態（thread-safe）
     */
    private final Map<StateKey, UIState> STATES = new ConcurrentHashMap<>();

    /* ----------------------------- 取得/建立狀態 ----------------------------- */

    /**
     * 取得既有狀態；不存在則建立空殼（needsRecreate 預設 true）
     */
    public UIState stateFor(StateKey key) {
        return STATES.computeIfAbsent(key, UIState::new);
    }

    /**
     * 取得既有狀態；無則回傳 null
     */
    public UIState getState(StateKey key) {
        return STATES.get(key);
    }

    /* ----------------------------- UI 與資料存取 ----------------------------- */

    public void registerUI(StateKey key, BuiltUI ui) {
        stateFor(key).setUi(ui);
    }

    public BuiltUI getUI(StateKey key) {
        UIState s = STATES.get(key);
        return s != null ? s.getUI() : null;
    }

    public void setDataList(StateKey key, List<Map<String, Object>> list) {
        // 淺複製，避免外部修改影響內部；必要時對 map 也 copy
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> m : list) copy.add(new HashMap<>(m));
        stateFor(key).setDataList(Collections.unmodifiableList(copy));
    }


    public List<Map<String, Object>> getDataList(StateKey key) {
        UIState state = STATES.get(key);
        return state != null ? state.getDataList() : Collections.emptyList();
    }

    public Object getData(StateKey stateKey, String itemKey, String dataKey) {
        if (!STATES.containsKey(stateKey)) {
            throw new RuntimeException(
                UIStateManager.class.getSimpleName() + " getData: 找不到 stateKey: " + stateKey);
        }

        Map<String, Object> map = STATES.get(stateKey).getItem(itemKey);
        if (map == null) {
            throw new RuntimeException(
                UIStateManager.class.getSimpleName() + " getData: 找不到 itemKey: " + itemKey + " (stateKey=" + stateKey + ")");
        }

        if (!map.containsKey(dataKey)) {
            throw new RuntimeException(
                UIStateManager.class.getSimpleName() + " getData: 找不到 dataKey: " + dataKey + " (itemKey=" + itemKey + ")");
        }
        return map.get(dataKey); // 可能為 null（如果鍵存在但值為 null），這是預期行為
    }


    public Object getData(StateKey stateKey, String itemKey, String dataKey, Object defaultValue, boolean putIfAbsent) {
        List<Map<String, Object>> dataList = getDataList(stateKey);
        Map<String, Object> map = dataList.stream()
            .filter(m -> itemKey.equals(m.get("id")))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                UIStateManager.class.getSimpleName() + " getData: 找不到 itemKey: " + itemKey));

        if (!map.containsKey(dataKey)) {
            if (putIfAbsent) {
                map.put(dataKey, defaultValue);
            }
            return defaultValue;
        }
        return map.get(dataKey);
    }

    public void setData(StateKey stateKey, String itemKey, String dataKey, Object value) {
        List<Map<String, Object>> dataList = getDataList(stateKey);
        Map<String, Object> map = dataList.stream()
            .filter(m -> itemKey.equals(m.get("id")))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                UIStateManager.class.getSimpleName() + " getData: 找不到 itemKey: " + itemKey));

        map.put(dataKey, value);
    }


    /**
     * 若不是清單資料，也可放任意 payload（例如排序條件、查詢參數等）
     */
    public void setPayload(StateKey key, Object payload) {
        stateFor(key).setPayload(payload);
    }

    public <T> T getPayload(StateKey key, Class<T> type) {
        UIState s = STATES.get(key);
        if (s == null || s.getPayload() == null) return null;
        return type.cast(s.getPayload());
    }

    /* ----------------------------- 重建旗標 ----------------------------- */

    public void setNeedsRecreate(StateKey key, boolean flag) {
        stateFor(key).markRecreate(flag);
    }

    public boolean needsRecreate(StateKey key) {
        UIState s = STATES.get(key);
        return s != null && s.needsRecreate();
    }

    /**
     * 批次標記需要重建
     */
    public void markRecreate(StateKey... keys) {
        for (StateKey k : keys) {
            setNeedsRecreate(k, true);
        }
    }

    /**
     * 依「鍵前綴」批次標記重建（例如傳入 screenName, cateId）
     */
    public void markRecreateByPrefix(Object... prefixParts) {
        forEachKeyMatchingPrefix(prefixParts, k -> {
            setNeedsRecreate(k, true);
            return true;
        });
    }

    /* ----------------------------- 重置/刪除 ----------------------------- */

    /**
     * 移除指定 keys 的狀態（下次會從 0 開始重建）
     */
    public void reset(StateKey... keys) {
        for (StateKey k : keys) {
            STATES.remove(k);
        }
    }

    /**
     * 依「鍵前綴」批次移除（例如整個畫面或某 cateId 全部清掉）
     */
    public void resetByPrefix(Object... prefixParts) {
        List<StateKey> toRemove = new ArrayList<>();
        forEachKeyMatchingPrefix(prefixParts, k -> {
            toRemove.add(k);
            return true;
        });
        toRemove.forEach(STATES::remove);
    }

    /**
     * 清空所有狀態（請小心使用）
     */
    public void resetAll() {
        STATES.clear();
    }

    /* ----------------------------- 查詢/工具 ----------------------------- */

    /**
     * 遍歷符合條件的 key（內部使用）
     */
    private void forEachKeyMatchingPrefix(Object[] prefix, Predicate<StateKey> action) {
        for (StateKey k : STATES.keySet()) {
            if (k.startsWith(prefix)) {
                if (!action.test(k)) break;
            }
        }
    }

    /**
     * 列出目前持有的所有 key（除錯用）
     */
    public List<StateKey> keys() {
        return new ArrayList<>(STATES.keySet());
    }

    /* ----------------------------- 型別：UIState ----------------------------- */

    public static final class UIState {
        private final StateKey key;
        private volatile BuiltUI ui;
        private volatile List<Map<String, Object>> dataList = Collections.emptyList();
        private volatile Map<String, Map<String, Object>> byId = Map.of();
        private volatile Object payload;
        private volatile boolean needsRecreate = true;
        private volatile long version = 0L;
        private volatile long lastUpdatedNanos = System.nanoTime();

        UIState(StateKey key) {
            this.key = key;
        }

        public StateKey getKey() {
            return key;
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

        public boolean needsRecreate() {
            return needsRecreate;
        }

        public void markRecreate(boolean b) {
            this.needsRecreate = b;
            touch();
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

    /* ----------------------------- 型別：StateKey（可變長度） ----------------------------- */

    /**
     * 可變長度複合鍵。順序有意義：
     * StateKey.of("inspection_supplyitem", cateId)
     * 和
     * StateKey.of(cateId, "inspection_supplyitem")
     * 不是同一個 key。
     */
    public static final class StateKey {
        private final List<Object> parts;
        private final int hash; // 預先計算，提升 Map 操作效率

        private StateKey(List<Object> parts) {
            // 轉不可變，避免外部修改
            this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
            this.hash = Objects.hash(this.parts.toArray());
        }

        /**
         * 以可變參數建立 key（順序有意義）
         */
        public static StateKey of(Object... parts) {
            if (parts == null || parts.length == 0) {
                throw new IllegalArgumentException("StateKey parts must not be empty");
            }
            return new StateKey(Arrays.asList(parts));
        }

        /**
         * 是否以 prefixParts 開頭（全部相等且順序相同）
         */
        public boolean startsWith(Object... prefixParts) {
            if (prefixParts == null || prefixParts.length == 0) return true;
            if (prefixParts.length > parts.size()) return false;
            for (int i = 0; i < prefixParts.length; i++) {
                if (!Objects.equals(parts.get(i), prefixParts[i])) return false;
            }
            return true;
        }

        public List<Object> parts() {
            return parts;
        }

        @Override
        public boolean equals(Object o) {
            return (this == o) ||
                (o instanceof StateKey && parts.equals(((StateKey) o).parts));
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
