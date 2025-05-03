### UIBuilder
```java
package idv.kuan.studio.libgdx.simpleui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.XmlReader;

import java.util.HashMap;
import java.util.Map;

public class UIBuilder {
    private Skin skin;
    private Map<String, Actor> namedActors = new HashMap<>();

    public UIBuilder(Skin skin) {
        this.skin = skin;
    }

    public UIBuilder build(FileHandle xmlFile) {

        XmlReader reader = new XmlReader();
        XmlReader.Element root = reader.parse(xmlFile);

        buildElement(root);
        return this;
    }

    private Actor buildElement(XmlReader.Element elem) {
        int childCount = elem.getChildCount();
        String elemName = elem.getName();
        Group group;
        if ("Table".equals(elemName)) {
            group = new Table();
            ((Table) group).setFillParent(true);
        } else {
            group = new Group();
        }

        for (int idx = 0; idx < childCount; idx++) {
            XmlReader.Element childElem = elem.getChild(idx);
            String id = childElem.getAttribute("id", null);
            String childName = childElem.getName();

            Actor childActor = null;
            switch (childName) {
                case "Table":
                    childActor = buildElement(childElem);
                    break;
                case "Label":
                    childActor = new Label(childElem.getText(), skin);
                    break;
                case "TextButton":
                    childActor = new TextButton(childElem.getText(), skin);
                default:
                    //Do nothing
                    break;
            }

            if (id != null) {
                namedActors.put(id, childActor);
            }

            if (childActor != null) {
                if ("Table".equals(elemName)) {
                    ((Table) group).add(childActor);
                } else {
                    group.addActor(childActor);
                }
            }
        }

        return group;
    }

    public Actor getActor(String actorName) {
        return namedActors.get(actorName);
    }

}
```

<br>

### XML
```xml
<SimpleUI>
    <Table id="tb1">
        <Label>
            123
        </Label>
        <TextButton>
            456
        </TextButton>
    </Table>
</SimpleUI>
```

<br>

### Main
```java
package idv.kuan.studio.libgdx.simpleui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * First screen of the application. Displayed after the application is created.
 */
public class FirstScreen implements Screen {
    private Skin skin;
    private Stage stage;


    @Override
    public void show() {
        // Prepare your screen here.
        ScreenViewport screenViewport = new ScreenViewport();
        stage = new Stage(screenViewport);
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        String xmlPath = "xml/test.xml";

        FileHandle fileHandle = Gdx.files.internal(xmlPath);
        UIBuilder uiBuilder = new UIBuilder(skin);
        uiBuilder.build(fileHandle);
        Actor actor = uiBuilder.getActor("tb1");


        stage.addActor(actor);
    }

    @Override
    public void render(float delta) {
        // Draw your screen here. "delta" is the time since last render in seconds.
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        stage.dispose();
        skin.dispose();
    }
}
```
