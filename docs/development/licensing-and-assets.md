# 许可、署名与素材政策

## 自有代码

仓库使用 MIT License。新贡献默认按仓库许可证发布，除非贡献说明明确且项目接受其他安排。

## 第三方

- 通过 Maven 依赖 AE2、JEI、Mekanism，不把其 JAR 纳入仓库。
- Thunderbolt Core 以独立源码构建保存在 `third_party/thunderbolt-core`，
  保留 LGPL-3.0 原许可证、来源提交和独立包名，不改标为本项目 MIT 代码。
- 阅读和调用公开 API 不等于获得其所有素材的再授权。
- 复制上游代码时即使许可证允许，也必须保留许可头和来源；本项目优先不复制。
- 派生纹理/模型/音效必须确认许可证允许，并在 `NOTICE.md` 列出作者、项目、版本、链接和许可证。

## 商标和命名

项目是独立附属。README 与模组元数据不得暗示官方合作或认证。AE2、JEI、Mekanism、Minecraft 名称仅用于说明兼容关系。

## 0.1.0 像素素材来源

- `pattern_binder.png`：基于 AE2 Lightning Tech 的 16×16
  `wireless_tianshu_pattern_encoding_terminal.png` 修改，屏幕使用
  `matter_warping_matrix_overload_main_core.png` 中央区域派生的 6×6 过载核心图案。
- `pattern_linker.png`：基于 AE2 Lightning Tech 的 16×16
  `matter_warping_matrix_overload_main_core.png` 修改，将粉色核心调整为天枢终端的紫色，并加入 2 个蓝色连接标记像素。
- 上述两项上游素材取自提交
  `379b99a3ef188218caab0071b08d1c707d7e9e27`，来源为
  <https://github.com/ae2lt/AE2-Lightning-Tech>，按 CC BY-NC-SA 3.0
  许可；本项目中的两个派生贴图沿用同一许可。
- `tianshu_pattern_selector*.png` 与对应方块模型：直接基于同一提交的
  天枢超算控制器 64×64 素材和模型改名到 `aeallpattern` 命名空间，继续按
  CC BY-NC-SA 3.0 发布。
- `tianshu` Java 单方块宿主：基于 AE2 Lightning Tech 提交
  `fe8590ea45becd0c5f4ab67f4e779612eff09a8a` 的已删除测试时间轮 CPU，
  改名并适配本项目注册系统；该派生部分按 LGPL-3.0 发布。
- `third_party/thunderbolt-core`：取自 Thunderbolt Core 提交
  `171a6cba1a5d8d62a0019dd8ff74158344928f54`，完整保留 LGPL-3.0。
- `icon.png`：本项目自有 128×128 像素素材。

修改日期为 2026-08-26。视觉派生文件只存放在 `aeallpattern` 自己的资源命名空间；
第三方源码放在明确隔离的 `third_party` 目录，并作为独立运行时模组构建。

## 发布检查

- 解压 JAR，确认没有意外包含上游 sources、dev JAR、Blockbench 工程和测试存档。
- `NOTICE.md` 与实际资产一致。
- 平台依赖关系把 AE2 标为 required，Mekanism/JEI 按实现状态标为 optional。
