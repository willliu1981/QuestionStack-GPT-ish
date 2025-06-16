# Fluent Step Builder Pattern 

這份文件將帶你理解並實作一個「**有順序的鏈式設定流程（Fluent Step Builder Pattern）**」，這種設計模式特別適合用於需要依序填寫資訊的流程，例如建立角色、報名表單、建構 UI 元件等。

---

## 🎯 範例主題：角色建立器 CharacterBuilder

我們以建立遊戲角色為例，使用者需依照順序填寫：

1. 姓名
2. 種族
3. 職業
4. 能力值（力量、敏捷、智力）
5. 完成建立並取得角色物件

這是一個非常適合教學 Fluent Step Builder 的實例。

---

## 🧩 核心理念

將設定過程拆成多個步驟介面（StepA → StepB → StepC → ...），
使用者每完成一個步驟後，僅能呼叫下一個步驟，避免錯誤順序與漏設欄位。

---

## 📐 結構設計

### ✅ 每個 Step 是一個 Interface

```java
interface StepA {
    StepB setName(String name);
}

interface StepB {
    StepC setRace(String race);
}

interface StepC {
    StepD setJob(String job);
}

interface StepD {
    FinalStep setStats(int strength, int agility, int intelligence);
}

interface FinalStep {
    Character build();
}
```

---

### ✅ Builder 類別：實作所有步驟介面

```java
public class CharacterBuilder implements StepA, StepB, StepC, StepD, FinalStep {
    private final Character character = new Character();

    public static StepA begin() {
        return new CharacterBuilder();
    }

    @Override
    public StepB setName(String name) {
        character.name = name;
        return this;
    }

    @Override
    public StepC setRace(String race) {
        character.race = race;
        return this;
    }

    @Override
    public StepD setJob(String job) {
        character.job = job;
        return this;
    }

    @Override
    public FinalStep setStats(int str, int agi, int intel) {
        character.strength = str;
        character.agility = agi;
        character.intelligence = intel;
        return this;
    }

    @Override
    public Character build() {
        return character;
    }
}
```

---

### ✅ 最終結果類：Character

```java
public class Character {
    String name;
    String race;
    String job;
    int strength, agility, intelligence;

    @Override
    public String toString() {
        return name + " the " + race + " " + job +
            " [STR=" + strength + ", AGI=" + agility + ", INT=" + intelligence + "]";
    }
}
```

---

## ✅ 使用範例

```java
Character character = CharacterBuilder.begin()
    .setName("Kuan")
    .setRace("Elf")
    .setJob("Mage")
    .setStats(5, 7, 15)
    .build();

System.out.println(character);
```

輸出結果：

```
Kuan the Elf Mage [STR=5, AGI=7, INT=15]
```

---

## 💡 優點

* 強制順序，不易出錯
* 無需複雜建構子，參數可清楚標示
* IDE 自動提示下一步驟，學習成本低

---

## 🔚 延伸應用

* 可擴充選填步驟，例如加上裝備、技能（使用 OptionalStep）
* 可應用於表單資料收集、UI 組態、遊戲流程控制

---

這份教學範例可直接作為 Java 專案、LibGDX 或遊戲開發流程中的設定導引基礎。
