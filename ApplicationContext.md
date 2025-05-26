下面是一份极简版的示例，演示了如何用一个 `ApplicationContext` 来集中构造并获取各个全局服务（`ResourceManager`、`UIBuilderFactory`），同时防止它们被外部随意 new：

```java
// -------------------------
// ApplicationContext.java
// -------------------------
package com.example.app;

import java.util.Objects;

public final class ApplicationContext {
    private static ApplicationContext INSTANCE;

    private final ResourceManager resourceManager;
    private final UIBuilderFactory uiFactory;

    private ApplicationContext(Config cfg) {
        // 强制校验，cfg 不可为 null
        Objects.requireNonNull(cfg, "Config must not be null");

        // 由 Context 统一 new
        this.resourceManager = new ResourceManager(this, cfg.fontPath, cfg.chars);
        this.uiFactory       = new UIBuilderFactory(this);
    }

    /** 只能调用一次 */
    public static synchronized void initialize(Config cfg) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Context already initialized");
        }
        INSTANCE = new ApplicationContext(cfg);
    }

    /** 获取已初始化的单例 */
    public static ApplicationContext get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Context not yet initialized");
        }
        return INSTANCE;
    }

    public ResourceManager   getResourceManager() { return resourceManager; }
    public UIBuilderFactory  getUiFactory()       { return uiFactory; }

    /** 配置载体，构造 Context 时传入 */
    public static class Config {
        public final String fontPath;
        public final String chars;

        public Config(String fontPath, String chars) {
            this.fontPath = Objects.requireNonNull(fontPath);
            this.chars    = Objects.requireNonNull(chars);
        }
    }
}
```

```java
// -------------------------
// ResourceManager.java
// -------------------------
package com.example.app;

import java.util.Objects;

/** package-private，只有 Context 能 new */
class ResourceManager {
    private final String fontPath;
    private final String chars;

    ResourceManager(ApplicationContext ctx, String fontPath, String chars) {
        // ctx 校验，防止外部直接调用
        Objects.requireNonNull(ctx, "Context must not be null");
        this.fontPath = Objects.requireNonNull(fontPath);
        this.chars    = Objects.requireNonNull(chars);
        // ……初始化你的字体／纹理等资源……
    }

    public String getFontPath() { return fontPath; }
    public String getChars()    { return chars; }
}
```

```java
// -------------------------
// UIBuilderFactory.java
// -------------------------
package com.example.app;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** package-private，只有 Context 能 new */
class UIBuilderFactory {
    private final ApplicationContext ctx;
    private final Map<String, UIBuilder> cache = new HashMap<>();

    UIBuilderFactory(ApplicationContext ctx) {
        Objects.requireNonNull(ctx, "Context must not be null");
        this.ctx = ctx;
    }

    /** 按 key 缓存 Builder 实例 */
    public UIBuilder getBuilder(String key, Supplier<UIBuilder> creator) {
        return cache.computeIfAbsent(key, k -> creator.get());
    }
}
```

```java
// -------------------------
// UIBuilder.java
// （伪代码，省略具体实现）
package com.example.app;

public class UIBuilder {
    public UIBuilder(ResourceManager rm) {
        // 用 rm.getFontPath()/getChars() 等初始化
    }
    // … build 方法 …
}
```

```java
// -------------------------
// MainGame.java
// -------------------------
package com.example.app;

public class MainGame {
    public static void main(String[] args) {
        // 1) 先初始化全局 Context
        ApplicationContext.initialize(
          new ApplicationContext.Config(
            "fonts/SourceHan.otf",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          )
        );

        // 2) 在任意地方通过 Context 拿服务
        ResourceManager rm = ApplicationContext.get().getResourceManager();
        System.out.println("Font path is: " + rm.getFontPath());

        UIBuilder ui = ApplicationContext
          .get()
          .getUiFactory()
          .getBuilder("main", () -> new UIBuilder(rm));

        // ……后续启动游戏逻辑……
    }
}
```

**亮点：**

* **构造器权限可见性**：`ResourceManager`、`UIBuilderFactory` 构造器设为 package-private，只能被同包的 `ApplicationContext` new。
* **禁止重复初始化**：`ApplicationContext.initialize()` 只能调用一次，多次调用会抛错。
* **统一获取入口**：所有地方都通过 `ApplicationContext.get()` 拿到上下文，再获取具体服务，彻底杜绝随意 `new`。

