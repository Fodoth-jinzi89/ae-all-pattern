# 资源、数据生成与静态校验

## 目录

Minecraft 1.21.1 使用新版单数数据目录，例如：

```text
assets/aeallpattern/lang/
assets/aeallpattern/models/item/
assets/aeallpattern/textures/item/
data/aeallpattern/recipe/
data/aeallpattern/loot_table/
data/aeallpattern/tags/
```

跨到 1.20.1 时目录与 JSON 字段会变化，不能机械复制。

## Datagen

```bash
./gradlew runData
```

生成结果放 `src/generated/resources`，构建会自动包含。生成器必须确定性：相同源码和依赖产生相同排序与内容。

## 静态校验

随着链接器方块实现，给 `check` 增加：

- 每个方块有 blockstate、模型、loot table 和挖掘 tag。
- 绑定器有 item model、语言键和配方。
- mods.toml 版本范围与 gradle.properties 一致。
- 拆除时应该保留的 Data Component 有 `copy_components` 或对应掉落策略。
- 资源 JSON 可解析，不包含旧版本目录名。

旧项目出现过“有 loot table 但缺 mineable tag，实际挖掘不掉落”问题；校验必须检查完整链路，而不是只检查一个文件存在。

## 纹理与模型

- 源工程与最终资源分离，JAR 排除 `.bbmodel` 等开发文件。
- 绑定紫色框是渲染逻辑，不需要为每种机器复制模型。
- 占位模型可引用 AE2 已安装资源，正式发布前应换成自己的绑定器图标。
- 所有第三方引用写入素材政策和 NOTICE。
