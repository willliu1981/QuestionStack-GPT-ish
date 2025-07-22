以下是三個完整的 Java 類別，分別對應 DAO、Repository、Service 結構。

---

### ✅ `SupplyCategoryDao.java`

```java
package idv.kuan.roomprep.data.dao;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import idv.kuan.roomprep.model.v2.DefaultSupplyCategory;
import idv.kuan.roomprep.model.v2.SupplyCategory;

import java.util.ArrayList;
import java.util.List;

public class SupplyCategoryDao {

    public List<SupplyCategory> load(String xmlPath) {
        List<SupplyCategory> list = new ArrayList<>();
        try {
            FileHandle file = Gdx.files.internal(xmlPath);
            XmlReader reader = new XmlReader();
            Element root = reader.parse(file);

            for (Element elem : root.getChildrenByName("category")) {
                String id = elem.getAttribute("id");
                String label = elem.getAttribute("displayName");
                String i18nKey = elem.getAttribute("i18nKey");
                String texturePath = elem.getAttribute("texturePath", "");
                String description = elem.getAttribute("description", "");
                int sortOrder = elem.getIntAttribute("sortOrder", 0);

                SupplyCategory category = new DefaultSupplyCategory(
                        id, label, i18nKey, texturePath, description, sortOrder);
                list.add(category);
            }
        } catch (Exception e) {
            Gdx.app.error("SupplyCategoryDao", "Failed to load from: " + xmlPath, e);
        }
        return list;
    }
}
```

---

### ✅ `SupplyCategoryRepository.java`

```java
package idv.kuan.roomprep.data.repository;

import idv.kuan.roomprep.model.v2.SupplyCategory;

import java.util.*;

public class SupplyCategoryRepository {
    private final Map<String, SupplyCategory> categoryMap = new LinkedHashMap<>();

    public SupplyCategoryRepository(List<SupplyCategory> initialData) {
        for (SupplyCategory cat : initialData) {
            categoryMap.put(cat.getId(), cat);
        }
    }

    public void add(SupplyCategory category) {
        categoryMap.put(category.getId(), category);
    }

    public void remove(String id) {
        categoryMap.remove(id);
    }

    public SupplyCategory findById(String id) {
        return categoryMap.get(id);
    }

    public List<SupplyCategory> getAll() {
        return new ArrayList<>(categoryMap.values());
    }

    public void update(SupplyCategory category) {
        categoryMap.put(category.getId(), category);
    }
}
```

---

### ✅ `SupplyCategoryService.java`

```java
package idv.kuan.roomprep.service;

import idv.kuan.roomprep.data.repository.SupplyCategoryRepository;
import idv.kuan.roomprep.model.v2.SupplyCategory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SupplyCategoryService {
    private final SupplyCategoryRepository repository;

    public SupplyCategoryService(SupplyCategoryRepository repository) {
        this.repository = repository;
    }

    public List<SupplyCategory> getSortedByOrder() {
        return repository.getAll().stream()
                .sorted(Comparator.comparingInt(SupplyCategory::getSortOrder))
                .collect(Collectors.toList());
    }

    public List<SupplyCategory> searchByKeyword(String keyword) {
        return repository.getAll().stream()
                .filter(cat -> cat.getDisplayName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}
```


