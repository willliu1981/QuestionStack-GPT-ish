```java
package idv.kuan.studio.libgdx.simpleui.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 通用資料工具：
 * - XmlMapStore：從 assets 讀預設、從 local 讀/寫目前資料，型別為 List<Map<String,Object>>。
 * - AttributeMapper：以 Map(attr->field) 對應，把 Map 轉 POJO 或反向。
 *
 * 用法（Inspection）：
 *   DataTool.XmlMapStore store = new DataTool.XmlMapStore(
 *       "data/defaultSupplyItem.xml",     // assets 路徑
 *       "data/localSupplyItem.xml",       // local 路徑
 *       "supplyitem-list",                // root tag
 *       "supplyitem"                      // item tag
 *   );
 *   List<Map<String,Object>> defaults = store.loadDefaultMaps();
 *   List<Map<String,Object>> localOrNull = store.loadLocalMapsOrNull();
 *   store.saveLocalMaps(defaults);
 *
 * 用法（POJO 對應）：
 *   Map<String,String> m = Map.of("id","id","name","name","quantity","quantity");
 *   DataTool.AttributeMapper mapper = DataTool.AttributeMapper.of(m);
 *   List<SupplyItem> items = mapper.toObjects(defaults, SupplyItem.class);
 *   List<Map<String,Object>> back = mapper.fromObjects(items);
 */
public final class DataTool {

    private DataTool() {
    }

    /* ----------------------------- XML Map Store ----------------------------- */

    public static final class XmlMapStore {

        private final String assetsPath;
        private final String localPath;
        private final String rootTag;
        private final String itemTag;

        public XmlMapStore(String assetsPath, String localPath, String rootTag, String itemTag) {
            Objects.requireNonNull(assetsPath, "assetsPath");
            Objects.requireNonNull(localPath, "localPath");
            Objects.requireNonNull(rootTag, "rootTag");
            Objects.requireNonNull(itemTag, "itemTag");
            this.assetsPath = assetsPath;
            this.localPath = localPath;
            this.rootTag = rootTag;
            this.itemTag = itemTag;
        }

        /** 從 assets 讀預設資料。 */
        public List<Map<String, Object>> loadDefaultMaps() {
            FileHandle fh = Gdx.files.internal(assetsPath);
            if (!fh.exists()) {
                throw new IllegalStateException("assets not found: " + assetsPath);
            }
            return readXmlToMaps(fh);
        }

        /** 從 local 讀目前資料；不存在回傳 null。 */
        public List<Map<String, Object>> loadLocalMapsOrNull() {
            FileHandle fh = Gdx.files.local(localPath);
            if (!fh.exists()) {
                return null;
            }
            return readXmlToMaps(fh);
        }

        /** 寫入 local。會覆蓋。 */
        public void saveLocalMaps(List<Map<String, Object>> data) {
            Objects.requireNonNull(data, "data");
            FileHandle fh = Gdx.files.local(localPath);
            writeMapsToXml(fh, data);
        }

        private List<Map<String, Object>> readXmlToMaps(FileHandle fh) {
            try {
                XmlReader.Element root = new XmlReader().parse(fh);
                Array<XmlReader.Element> nodes = root.getChildrenByName(itemTag);
                List<Map<String, Object>> out = new ArrayList<>(nodes.size);

                for (int i = 0; i < nodes.size; i++) {
                    XmlReader.Element e = nodes.get(i);
                    Map<String, Object> m = new LinkedHashMap<>();

                    ObjectMap<String, String> attrs = e.getAttributes();
                    for (ObjectMap.Entry<String, String> en : attrs.entries()) {
                        String name = en.key;
                        String val  = en.value;
                        m.put(name, coerce(val));
                    }
                    out.add(m);
                }
                return out;
            } catch (Exception ex) {
                throw new RuntimeException("readXmlToMaps error: " + fh.path(), ex);
            }
        }

        private void writeMapsToXml(FileHandle fh, List<Map<String, Object>> data) {
            Writer w = null;
            XmlWriter xw = null;
            try {
                w = fh.writer(false, "UTF-8");
                xw = new XmlWriter(w);
                xw.element(rootTag);
                for (Map<String, Object> m : data) {
                    xw.element(itemTag);
                    for (Map.Entry<String, Object> en : m.entrySet()) {
                        xw.attribute(en.getKey(), String.valueOf(en.getValue()));
                    }
                    xw.pop(); // item
                }
                xw.pop(); // root
            } catch (IOException e) {
                throw new RuntimeException("writeMapsToXml error: " + fh.path(), e);
            } finally {
                try {
                    if (xw != null) {
                        xw.close();
                    } else if (w != null) {
                        w.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }

        private static Object coerce(String s) {
            if (s == null) {
                return null;
            }
            String v = s.trim();
            if (v.equalsIgnoreCase("true")) {
                return true;
            }
            if (v.equalsIgnoreCase("false")) {
                return false;
            }
            try {
                if (v.matches("-?\\d+")) {
                    return Integer.parseInt(v);
                }
                if (v.matches("-?\\d+\\.\\d+")) {
                    return Float.parseFloat(v);
                }
            } catch (Exception ignore) {
            }
            return v;
        }
    }

    /* ----------------------------- Attribute Mapper ----------------------------- */

    public static final class AttributeMapper {

        private final Map<String, String> attrToField;

        private AttributeMapper(Map<String, String> attrToField) {
            this.attrToField = new LinkedHashMap<>(attrToField);
        }

        public static AttributeMapper of(Map<String, String> attrToField) {
            Objects.requireNonNull(attrToField, "attrToField");
            return new AttributeMapper(attrToField);
        }

        /** Map -> POJO 清單。僅對應 mapping 中列出的欄位。 */
        public <T> List<T> toObjects(List<Map<String, Object>> maps, Class<T> type) {
            try {
                List<T> out = new ArrayList<>(maps.size());
                for (Map<String, Object> m : maps) {
                    T obj = type.getDeclaredConstructor().newInstance();
                    for (Map.Entry<String, String> en : attrToField.entrySet()) {
                        String attr = en.getKey();
                        String field = en.getValue();
                        if (!m.containsKey(attr)) {
                            continue;
                        }
                        Object val = m.get(attr);
                        setField(obj, field, val);
                    }
                    out.add(obj);
                }
                return out;
            } catch (Exception e) {
                throw new RuntimeException("toObjects error", e);
            }
        }

        /** POJO -> Map 清單。僅輸出 mapping 中列出的欄位。 */
        public <T> List<Map<String, Object>> fromObjects(List<T> objs) {
            try {
                List<Map<String, Object>> out = new ArrayList<>(objs.size());
                for (T obj : objs) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    for (Map.Entry<String, String> en : attrToField.entrySet()) {
                        String attr = en.getKey();
                        String field = en.getValue();
                        Object val = getField(obj, field);
                        m.put(attr, val);
                    }
                    out.add(m);
                }
                return out;
            } catch (Exception e) {
                throw new RuntimeException("fromObjects error", e);
            }
        }

        private static void setField(Object obj, String fieldName, Object value) throws Exception {
            Field f = findField(obj.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Class<?> t = f.getType();
            f.set(obj, convert(value, t));
        }

        private static Object getField(Object obj, String fieldName) throws Exception {
            Field f = findField(obj.getClass(), fieldName);
            if (f == null) {
                return null;
            }
            f.setAccessible(true);
            return f.get(obj);
        }

        private static Field findField(Class<?> c, String name) {
            Class<?> cur = c;
            while (cur != null && cur != Object.class) {
                try {
                    return cur.getDeclaredField(name);
                } catch (NoSuchFieldException ignore) {
                }
                cur = cur.getSuperclass();
            }
            return null;
        }

        private static Object convert(Object v, Class<?> t) {
            if (v == null) {
                return null;
            }
            if (t.isInstance(v)) {
                return v;
            }
            if (t == String.class) {
                return String.valueOf(v);
            }
            if ((t == int.class || t == Integer.class) && v instanceof Number) {
                return ((Number) v).intValue();
            }
            if ((t == float.class || t == Float.class) && v instanceof Number) {
                return ((Number) v).floatValue();
            }
            if ((t == boolean.class || t == Boolean.class) && v instanceof Boolean) {
                return v;
            }
            // 其它基本型別可按需擴充
            return v;
        }
    }
}


```
