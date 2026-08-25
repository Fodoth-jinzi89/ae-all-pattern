# 版本移植

## 原则

主线先稳定 1.21.1 NeoForge。只有用户或维护计划明确要求，才建立其他 MC 版本。

## 可直接移植

- 纯 Java 配方指纹、排序、diff、缓存与策略测试。
- 产品语义和适配器接口意图。
- 不包含 MC API 的诊断格式。

## 必须重写/适配

- Forge/NeoForge 注册、网络与 capability API。
- Java 版本及标准库方法。
- RecipeHolder、Ingredient、ItemStack 组件/NBT API。
- AE2 大版本 API 和 pattern details。
- Mekanism recipe/input handler API。
- 数据包目录、loot/tag 名称和 JSON Codec。
- 存档字段、Data Component 与 BlockEntity 序列化。

## 验证

每个分支独立运行单测、编译、专服、客户端和存档。不要以“代码看起来一样”替代目标版本运行。Release notes 明确哪个功能因上游 API 差异暂不支持。
