要做一个“DOS 式文字冒险＋WASD 即时移动”的小框架，你可以把它拆成以下几部分：

1. **世界模型**：房间（Room）或地图格（Cell）
2. **玩家状态**：当前位置、背包、属性
3. **输入处理**：捕获 WASD 键和回车后的文本命令
4. **命令解析**：把字符串拆成动词＋名词，分发给各个 CommandHandler
5. **输出界面**：把房间描述、反馈文本打印到屏幕（控制台或 LibGDX UI）

下面给你一个 Java＋LibGDX Scene2D 的最小可运行样板，供参考。

---

## 一、世界和玩家模型

```java
// Room.java
public class Room {
  private final String description;
  private final Map<Direction, String> exits; // N,S,E,W -> roomId
  private final List<String> items;

  public Room(String desc) {
    this.description = desc;
    this.exits = new EnumMap<>(Direction.class);
    this.items = new ArrayList<>();
  }
  public String getDescription() { return description; }
  public void setExit(Direction dir, String roomId) { exits.put(dir, roomId); }
  public String getExit(Direction dir) { return exits.get(dir); }
  public List<String> getItems() { return items; }
}
```

```java
// Direction.java
public enum Direction { N, S, E, W }
```

```java
// GameWorld.java
public class GameWorld {
  private final Map<String,Room> rooms = new HashMap<>();
  private String currentRoomId;

  public void addRoom(String id, Room room) {
    rooms.put(id, room);
    if (currentRoomId == null) currentRoomId = id;
  }

  public Room getCurrentRoom() { return rooms.get(currentRoomId); }
  public boolean move(Direction dir) {
    String next = getCurrentRoom().getExit(dir);
    if (next != null && rooms.containsKey(next)) {
      currentRoomId = next;
      return true;
    }
    return false;
  }
}
```

---

## 二、命令解析与执行

```java
// Command.java
public interface Command {
  /** 
   * 执行后的输出文本 
   * @param args 命令参数 (不含动词) 
   */
  String execute(String[] args, GameWorld world);
}
```

```java
// LookCommand.java
public class LookCommand implements Command {
  @Override
  public String execute(String[] args, GameWorld world) {
    Room room = world.getCurrentRoom();
    StringBuilder sb = new StringBuilder(room.getDescription()).append("\nExits: ");
    for (Direction d : room.getExits().keySet()) sb.append(d).append(" ");
    if (!room.getItems().isEmpty()) {
      sb.append("\nItems: ").append(String.join(", ", room.getItems()));
    }
    return sb.toString();
  }
}
```

```java
// CommandFactory.java
public class CommandFactory {
  private final Map<String,Command> cmds = new HashMap<>();
  public CommandFactory() {
    cmds.put("look", new LookCommand());
    // cmds.put("take", new TakeCommand()); …
  }
  public Optional<Command> get(String verb) {
    return Optional.ofNullable(cmds.get(verb));
  }
}
```

---

## 三、输入与界面（LibGDX + Scene2D）

```java
public class TextAdventureScreen extends ScreenAdapter {
  private final Stage stage;
  private final Label output;
  private final TextField input;
  private final GameWorld world;
  private final CommandFactory cmdFactory;

  public TextAdventureScreen() {
    stage = new Stage(new ScreenViewport());
    Skin skin = ApplicationContext.get().getSkin();
    // 输出区域
    output = new Label("", skin);
    output.setWrap(true);
    // 输入框
    input = new TextField("", skin);
    input.setMessageText("Type command...");
    input.setBlinkTime(0.4f);
    input.addListener(new InputListener() {
      @Override
      public boolean keyDown(InputEvent e, int keycode) {
        if (keycode == Input.Keys.ENTER) {
          processInput(input.getText());
          input.setText("");
        }
        // WASD 也可以在这里直接移动
        if (keycode == Input.Keys.W) move(Direction.N);
        if (keycode == Input.Keys.S) move(Direction.S);
        if (keycode == Input.Keys.A) move(Direction.W);
        if (keycode == Input.Keys.D) move(Direction.E);
        return false;
      }
    });

    // 布局
    Table table = new Table(skin);
    table.setFillParent(true);
    table.top().pad(10);
    table.add(output).expand().fill().row();
    table.add(input).growX();
    stage.addActor(table);

    // 世界、命令工厂
    world = new GameWorld();
    setupWorld(world);
    cmdFactory = new CommandFactory();
    // 初始 look
    output.setText(cmdFactory.get("look").get().execute(new String[]{}, world));
    Gdx.input.setInputProcessor(stage);
  }

  private void setupWorld(GameWorld w) {
    Room r1 = new Room("你在一片森林中…");
    Room r2 = new Room("你来到湖边…");
    r1.setExit(Direction.N, "lake");
    r2.setExit(Direction.S, "forest");
    w.addRoom("forest", r1);
    w.addRoom("lake",   r2);
  }

  private void move(Direction d) {
    if (world.move(d)) {
      String text = cmdFactory.get("look").get().execute(new String[]{}, world);
      output.setText(text);
    } else {
      output.setText("你不能往這個方向走。");
    }
  }

  private void processInput(String line) {
    String[] parts = line.trim().toLowerCase().split("\\s+");
    Optional<Command> cmd = cmdFactory.get(parts[0]);
    String result = cmd
      .map(c -> c.execute(Arrays.copyOfRange(parts,1,parts.length), world))
      .orElse("不知道要怎麼做: " + line);
    output.setText(result);
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    stage.act(delta);
    stage.draw();
  }
}
```

---

### 重点提示

1. **WASD 直接映射到 `move(Direction)`**，即时更新房间描述。
2. **文本命令**（`look`、`take`、`use`…）通过回车触发，拆词后交给对应的 `Command`。
3. **可扩展性**：新增命令只要实现 `Command` 并在 `CommandFactory` 中注册。
4. **界面简单**：一个可滚动的 `Label`＋一个 `TextField` 模拟 DOS 文本界面。

这样，你就能同时支持“WASD 四向走动”与“回车后输入文字命令”两种交互，还原经典 DOS 文字冒险的感觉！
