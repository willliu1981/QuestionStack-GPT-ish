## 創建desktop jar 指令: ./gradlew lwjgl3:dist  
其結果路徑位於:my_app\lwjgl3\build\lib


build.gradle: lwjgl3 加上以下可使Android Studio的assets資源一同打包到build\lib下
```
task copyAssets(type: Copy) {
  from rootProject.file('assets')
  into "${project.layout.buildDirectory.get().asFile.absolutePath}/lib/assets"
}
jar.finalizedBy(copyAssets)
```
