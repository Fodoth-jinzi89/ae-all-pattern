# 实施路线图

## Phase 0：工程骨架（当前）

- [x] NeoForge 1.21.1 可编译项目。
- [x] AE2/JEI/Mekanism 依赖分层。
- [x] 绑定器占位注册。
- [x] 配方指纹纯单元测试。
- [x] 架构、开发、测试、发布文档。

## Phase 1：绑定与链接器

- [ ] 链接器方块、BlockEntity、IManagedGridNode、频道与能耗。
- [ ] Binder Data Component 和两阶段服务端状态机。
- [ ] SavedData、schema 迁移、安全权限与解绑。
- [ ] 紫色包围框同步和渲染。
- [ ] 纯状态机测试、资源测试和基础 GameTest。

## Phase 2：原版熔炉 MVP

- [ ] MachineAdapterRegistry。
- [ ] RecipeManager 共享索引、规范化、指纹和 diff。
- [ ] VanillaFurnaceAdapter 与燃料策略。
- [ ] PatternDetailsHelper + ICraftingProvider。
- [ ] IncomingBuffer、单输入安全转运、PendingCraft 与 ReturnBuffer。
- [ ] `/reload`、重启、堵塞和断网测试。

## Phase 3：Mekanism

- [ ] 条件加载 Mek compat。
- [ ] 电力熔炉与工厂适配。
- [ ] 明确 RecipeType 映射和侧面配置。
- [ ] 粉碎/富集等确定性 ItemStackToItemStack 配方。
- [ ] 无 Mek 专服和完整环境双矩阵测试。

## Phase 4：展示、过滤与性能

- [ ] JEI 插件只展示已发布目录和过滤原因。
- [ ] 绑定过滤 UI、优先级和样板上限。
- [ ] 诊断命令、缓存统计和 1000/10000 配方基准。
- [ ] 测试存档/结构自动生成器。

## Phase 5：发布

- [ ] 自有 16×16 图标和完整模型。
- [ ] README 截图与演示视频。
- [ ] GitHub Release、Modrinth、CurseForge。
- [ ] 更新 JSON 和版本一致性门禁。
