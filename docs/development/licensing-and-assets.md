# 许可、署名与素材政策

## 自有代码

仓库使用 MIT License。新贡献默认按仓库许可证发布，除非贡献说明明确且项目接受其他安排。

## 第三方

- 通过 Maven 依赖 AE2、JEI、Mekanism，不把其 JAR 纳入仓库。
- 阅读和调用公开 API 不等于获得其所有素材的再授权。
- 复制上游代码时即使许可证允许，也必须保留许可头和来源；本项目优先不复制。
- 派生纹理/模型/音效必须确认许可证允许，并在 `NOTICE.md` 列出作者、项目、版本、链接和许可证。

## 商标和命名

项目是独立附属。README 与模组元数据不得暗示官方合作或认证。AE2、JEI、Mekanism、Minecraft 名称仅用于说明兼容关系。

## 发布检查

- 解压 JAR，确认没有意外包含上游 sources、dev JAR、Blockbench 工程和测试存档。
- `NOTICE.md` 与实际资产一致。
- 平台依赖关系把 AE2 标为 required，Mekanism/JEI 按实现状态标为 optional。
