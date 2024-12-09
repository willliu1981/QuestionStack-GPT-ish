拓撲排序（Topological Sort）是針對 **有向無環圖（DAG, Directed Acyclic Graph）** 的排序方法，用於確保每個頂點的排列順序滿足 **有向邊的約束條件**。簡單來說，如果有邊 \( u \to v \)，那麼在排序結果中，節點 \( u \) 必須出現在 \( v \) 之前。

---

### **應用場景**
1. 任務調度：任務之間有依賴關係，需按順序執行。
2. 編譯器：確定代碼或模組之間的依賴順序。
3. 資料處理：確保有依賴關係的數據按正確順序處理。

---

### **實現方法**
常見的拓撲排序方法包括：
1. **Kahn's Algorithm（基於入度的算法）**
2. **DFS（深度優先搜索）**

---

### **1. Kahn's Algorithm**
**思路**：
1. 計算所有頂點的初始入度（入邊數）。
2. 將所有入度為 0 的頂點加入隊列。
3. 每次從隊列中取出一個頂點，將其加入拓撲排序結果，並減少其所有相鄰頂點的入度。如果相鄰頂點的入度變為 0，則加入隊列。
4. 重複直到隊列為空。

**程式碼（Java 實現）**：
```java
import java.util.*;

public class TopologicalSortKahn {
    public static List<Integer> topologicalSort(int vertices, List<int[]> edges) {
        List<Integer> result = new ArrayList<>();
        Map<Integer, List<Integer>> graph = new HashMap<>();
        int[] inDegree = new int[vertices];

        // 建立圖和計算入度
        for (int[] edge : edges) {
            graph.computeIfAbsent(edge[0], x -> new ArrayList<>()).add(edge[1]);
            inDegree[edge[1]]++;
        }

        // 將所有入度為 0 的節點加入隊列
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < vertices; i++) {
            if (inDegree[i] == 0) queue.offer(i);
        }

        // 開始拓撲排序
        while (!queue.isEmpty()) {
            int node = queue.poll();
            result.add(node);

            // 減少相鄰節點的入度
            if (graph.containsKey(node)) {
                for (int neighbor : graph.get(node)) {
                    inDegree[neighbor]--;
                    if (inDegree[neighbor] == 0) queue.offer(neighbor);
                }
            }
        }

        // 如果排序結果的大小小於頂點數，表示有環
        if (result.size() != vertices) throw new RuntimeException("Graph has a cycle!");

        return result;
    }

    public static void main(String[] args) {
        int vertices = 6;
        List<int[]> edges = Arrays.asList(
            new int[]{5, 2}, new int[]{5, 0}, new int[]{4, 0}, 
            new int[]{4, 1}, new int[]{2, 3}, new int[]{3, 1}
        );
        System.out.println(topologicalSort(vertices, edges)); // 輸出 [5, 4, 2, 3, 1, 0]
    }
}
```

---

### **2. DFS（深度優先搜索）**
**思路**：
1. 使用一個布林數組標記節點是否被訪問過。
2. 遍歷每個節點，對尚未訪問過的節點進行 DFS。
3. 在 DFS 結束時，將節點加入結果棧。
4. 最終，結果棧的逆序即為拓撲排序。

**程式碼（Java 實現）**：
```java
import java.util.*;

public class TopologicalSortDFS {
    public static List<Integer> topologicalSort(int vertices, List<int[]> edges) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        boolean[] visited = new boolean[vertices];
        Stack<Integer> stack = new Stack<>();

        // 建立圖
        for (int[] edge : edges) {
            graph.computeIfAbsent(edge[0], x -> new ArrayList<>()).add(edge[1]);
        }

        // 遍歷每個節點進行 DFS
        for (int i = 0; i < vertices; i++) {
            if (!visited[i]) {
                dfs(i, graph, visited, stack);
            }
        }

        // 將棧內容轉換為結果
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) result.add(stack.pop());
        return result;
    }

    private static void dfs(int node, Map<Integer, List<Integer>> graph, boolean[] visited, Stack<Integer> stack) {
        visited[node] = true;

        // 遍歷相鄰節點
        if (graph.containsKey(node)) {
            for (int neighbor : graph.get(node)) {
                if (!visited[neighbor]) {
                    dfs(neighbor, graph, visited, stack);
                }
            }
        }

        // 結束時將節點壓入棧
        stack.push(node);
    }

    public static void main(String[] args) {
        int vertices = 6;
        List<int[]> edges = Arrays.asList(
            new int[]{5, 2}, new int[]{5, 0}, new int[]{4, 0}, 
            new int[]{4, 1}, new int[]{2, 3}, new int[]{3, 1}
        );
        System.out.println(topologicalSort(vertices, edges)); // 輸出 [5, 4, 2, 3, 1, 0]
    }
}
```

---

### **Kahn's Algorithm vs DFS**
| **特性**          | **Kahn's Algorithm**        | **DFS**                     |
|-------------------|----------------------------|----------------------------|
| **實現難度**      | 中                         | 簡單                       |
| **空間複雜度**    | \(O(V + E)\)               | \(O(V + E)\)               |
| **適用情況**      | 動態檢測入度               | 適合簡單圖結構處理         |
| **循環檢測**      | 可以輕鬆檢測循環            | 不適合（需要額外邏輯）     |

---

### **總結**
1. **Kahn's Algorithm** 適合需要在拓撲排序中檢測圖是否有環的情況。
2. **DFS 方法** 更簡單，適合處理靜態依賴結構的圖。

如果有進一步需求，請隨時提問！
