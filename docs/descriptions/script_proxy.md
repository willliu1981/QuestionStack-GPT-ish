ScriptApi
```java
package idv.kuan.studio.libgdx.simpleui.scripting;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import idv.kuan.studio.libgdx.simpleui.builder.BuiltUI;
import idv.kuan.studio.libgdx.simpleui.i18n.I18nManager;
import idv.kuan.studio.libgdx.simpleui.screen.ScreenIdentifier;
import idv.kuan.studio.libgdx.simpleui.tool.SessionContext;

public interface ScriptApi {

    void screen(String screenId);

    I18nManagerProxy i18n( );

    SessionProxy session();

    BuiltUIProxy ui();

    class I18nManagerProxy{
        private final I18nManager i18nManager;

        public I18nManagerProxy(I18nManager i18nManager) {
            this.i18nManager = i18nManager;
        }

        public void load(String path) {
            i18nManager.load(path);
        }

        public void setCurrentScreen(ScreenIdentifier screenIdentifier) {
            i18nManager.setCurrentScreen(screenIdentifier);
        }

        public Map<String, String> getValueMap(Set<String> templateIds) {
            return i18nManager.getValueMap(templateIds);
        }

        public Map<String, String> getDescriptionMap(Set<String> templateIds) {
            return i18nManager.getDescriptionMap(templateIds);
        }
    }

    class BuiltUIProxy {
        private final BuiltUI ui;

        public BuiltUIProxy(BuiltUI ui) {
            this.ui = ui;
        }

        public Actor getActor(String id) {
            return ui.getActor(id);
        }

        public void setTextForXmlLabel(String id, String content) {
            ui.setTextForXmlLabel(id, content);
        }

        public <T extends Actor> T getActor(String id, Class<T> type) {
            return ui.getActor(id, type);
        }

        public Map<String, Actor> getActors() {
            return ui.getActors();
        }

        public void refreshListItem(String parentContainerId, String templateId, int repeatCount, Object... data) {
            ui.refreshListItem(parentContainerId, templateId, repeatCount, data);
        }

        public String getTextFormXmlLabel(String id) {
            return ui.getTextFormXmlLabel(id);
        }

        public BuiltUI refreshListItem(String parentContainerId, String templateId, List<Map<String, Object>> dataList) {
            return ui.refreshListItem(parentContainerId, templateId, dataList);
        }

        public String getTextFormXmlTextButton(String id) {
            return ui.getTextFormXmlTextButton(id);
        }

        public void onClickToScreen(String id, Screen target) {
            ui.onClickToScreen(id, target);
        }

        public BuiltUI refreshListItem(String parentContainerId, String templateId, boolean useRow, Map<String, Object>... data) {
            return ui.refreshListItem(parentContainerId, templateId, useRow, data);
        }

        public void setTextForXmlTextButton(String id, String content) {
            ui.setTextForXmlTextButton(id, content);
        }

        public String getTextFormXmlTextField(String id) {
            return ui.getTextFormXmlTextField(id);
        }

        public void setTextForXmlTextField(String id, String content) {
            ui.setTextForXmlTextField(id, content);
        }

        public String getTextFormXmlTextArea(String id) {
            return ui.getTextFormXmlTextArea(id);
        }

        public void setTextForXmlTextArea(String id, String content) {
            ui.setTextForXmlTextArea(id, content);
        }

        public void setColor(String id, Color color) {
            ui.setColor(id, color);
        }

        public void onClick(String id, Runnable callback) {
            ui.onClick(id, callback);
        }
    }


    class SessionProxy {
        private final SessionContext sessionContext;

        public SessionProxy(SessionContext sessionContext) {
            this.sessionContext = sessionContext;
        }

        public void put(String key, Object value) {
            sessionContext.put(key, value);
        }

        public <T> T get(String key, Class<T> clazz) {
            return sessionContext.get(key, clazz);
        }

        public <T> T get(String key, Class<T> clazz, Object defaultValue) {
            return sessionContext.get(key, clazz, defaultValue);
        }

        public Object get(String key) {
            return sessionContext.get(key);
        }

        public Object get(String key, Object defaultValue) {
            return sessionContext.get(key, defaultValue);
        }

        public <T> T consume(String key, Class<T> clazz) {
            return sessionContext.consume(key, clazz);
        }

        public boolean contains(String key) {
            return sessionContext.contains(key);
        }

        public void clear() {
            sessionContext.clear();
        }

        public int adjustInt(String key, int delta, int min) {
            return sessionContext.adjustInt(key, delta, min);
        }

        public int adjustInt(String key, int delta) {
            return sessionContext.adjustInt(key, delta);
        }
    }
}

```
---
UIBuilder 部分
```java
private void setupShellVariables(BuiltUI builtUI) {
        try {
            // —— 原有注入 —— //
            Map<String, String> textMap = ApplicationContext.get().getI18nManager().getValueMap(templates.keySet());
            if (textMap != null && !textMap.isEmpty()) {
                javaShell.set("text", textMap);
            }
            Map<String, String> descMap = ApplicationContext.get().getI18nManager().getDescriptionMap(templates.keySet());
            if (descMap != null && !descMap.isEmpty()) {
                javaShell.set("desc", descMap);
            }
            // 注：先把 api 注入
            javaShell.set("api", new DefaultScriptApi(ApplicationContext.get(), builtUI));

            // —— 新增：把 prelude 映射也放这里 —— //
            String prelude =
                // 顶层方法映射
                "void screen(String id) { api.screen(id); }\n" +
                    // 代理对象赋值
                    "idv.kuan.studio.libgdx.simpleui.scripting.ScriptApi.I18nManagerProxy i18n = api.i18n();\n" +
                    "idv.kuan.studio.libgdx.simpleui.scripting.ScriptApi.SessionProxy session = api.session();\n" +
                    "idv.kuan.studio.libgdx.simpleui.scripting.ScriptApi.BuiltUIProxy ui = api.ui();\n";
            javaShell.eval(prelude);

            // —— 再注入 Actor 实例 —— //
            if (builtUI != null) {
                for (Map.Entry<String, Actor> entry : builtUI.getActors().entrySet()) {
                    javaShell.set(entry.getKey(), entry.getValue());
                }
            }
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
    }


    private void evalShellScripts() {
        try {
            // 导入常用包
            String importScript =
                "import com.badlogic.gdx.scenes.scene2d.*;\n" +
                    "import com.badlogic.gdx.scenes.scene2d.ui.*;\n" +
                    "import com.badlogic.gdx.graphics.*;\n" +
                    "import java.util.*;\n";
            javaShell.eval(importScript);

            // 执行从 XML 收集来的脚本片段
            for (String script : scriptJavaMap) {
                javaShell.eval(script);
            }
        } catch (EvalError e) {
            throw new RuntimeException("脚本初始化失败", e);
        }
    }
```
---
xml部分範例
```xml
  <UI>
    <script lang="java"><!-- @formatter:off -->
        boolean showAdjust=true;
        String cateName=session.get("cateName",String.class);

        void adjustItem(String itemId, int delta){
            int result= session.adjustInt(itemId+"Qty", delta);
            ui.setTextForXmlLabel(itemId+"_qty", "" + result);
            if(result>0){
                ui.getActor(itemId+"_minus").setColor(Color.valueOf("#00FF00FF"));
                ui.getActor(itemId+"_plus").setColor(Color.valueOf("#FF0000FF"));
            }else{
                ui.getActor(itemId+"_minus").setColor(Color.valueOf("#A9CFA3FF"));
                ui.getActor(itemId+"_plus").setColor(Color.valueOf("#D8A5A5FF"));
            }
        }
    </script><!-- @formatter:on -->

    <table id="tb_main_container" background="picture/common/lobby_black_winter.png" flags="fillParent">
        <!-- Header Section -->
        <include template="header" />


        <!-- Content Section -->
        <row />
        <table expand="0,1" pad="20" align="center" background="picture/common/panel.png" backgroundColor="#40404080">
            <label fontSize="75" padBottom="30" flags="next">${text["category"]} ${cateName}</label>

            <scrollPane>
                <table id="tb_list_supply" />
            </scrollPane>

            <row />
            <textButton onClick='screen("inspection")' width="400" padTop="100">${text["return"]}</textButton>
        </table>


        <!-- Footer Section -->
        <row />
        <include template="footer" />
    </table>
</UI>


<UI>
    <template id="item_category">
        <table onClick='session.put("cateId","${id}");session.put("cateName","${displayName}");screen(toScreenId);'>
            <label fontSize="55" flags="next">${displayName}</label>
            <label fontSize="25" padBottom="30" flags="next">${description}</label>
        </table>
    </template>

    <template id="item_supply">
    <table>
        <table pad="0,10,0,10">
            <table align="left">
                <label id="${id}_qty" fontSize="75" width="100" textAlign="center">0</label>
            </table>
            <table>
                <image size="100,100" src="${texturePath}" />
                <label fontSize="50" padLeft="50" textAlign="center" flags="next">${displayName}
                </label>
            </table>
        </table>
        <table if="showAdjust" pad="0,10,30,10" flags="newline" >
            <textButton id="${id}_minus" fontSize="50" width="200" color="#A9CFA3" onClick='adjustItem("${id}", -1)'>-
            </textButton>
            <textButton id="${id}_plus" fontSize="50" width="200" color="#D8A5A5" onClick='adjustItem("${id}", 1)'>+
            </textButton>
        </table>
    </table>
</template>
</UI>
```
---
補充的proguard rules
```
...前略

-keep class idv.kuan.studio.libgdx.simpleui.scripting.** {
    *;
}

-keep class bsh.** {
    *;
}


# 忽略所有 Android 上不存在的外部 API
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.script.**
-dontwarn javax.servlet.**
-dontwarn org.apache.bsf.**

# **重點**：忽略 BeanShell demo/Servlet/Engine 的 missing refs
-dontwarn bsh.util.**
-dontwarn bsh.servlet.**
-dontwarn bsh.engine.**

```

