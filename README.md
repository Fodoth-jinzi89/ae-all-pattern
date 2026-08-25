# AE All Pattern

> 用一次绑定，让机器支持的配方自动成为 AE2 的虚拟处理样板。

AE All Pattern 是面向 Applied Energistics 2 的附属模组原型。玩家先用“全样板绑定器”绑定一个 AE 网络锚点，再潜行右击目标机器。绑定成功后，机器会显示紫色 AE 风格包围框，AE 网络会把该机器可自动化的配方作为虚拟处理样板公开，无需逐张制作并存放实体样板。

## 当前状态

仓库目前是**文档先行、可编译的开发骨架**：已固定技术路线、依赖、目录、测试与发布流程，并注册了绑定器占位物品；绑定、虚拟样板发布和机器适配器还未实现。这样可以先把服务端权威、AE2 生命周期、安全与性能边界定清楚，再开始功能代码，避免做出只在单机 JEI 环境可用的版本。

## 目标交互

1. 手持绑定器右击“全样板链接器”，绑定 AE 网络锚点。
2. 潜行右击目标机器，例如 Mekanism 电力熔炉。
3. 服务端验证权限、距离、维度、区块与机器适配器。
4. 客户端为目标机器显示紫色 AE 风格包围框。
5. 服务端从 `RecipeManager` 和机器公开 API 建立配方目录。
6. 链接器通过 AE2 `ICraftingProvider` 发布虚拟处理样板。
7. AE 发起合成时，链接器先持久接管完整输入，再由适配器安全转运并追踪预期输出。

## 核心原则

- **JEI 不是服务端配方真源。** JEI 是可选客户端展示层；专用服务器从 `RecipeManager` 与机器 API 建索引。
- **拥有自己的 AE 锚点。** 不把持久状态或服务强行注入任意 AE 方块，避免网络拆分、重启和升级时失效。
- **适配器明确声明能力。** 不凭 JEI 页面猜测输入面、催化剂、概率、流体和输出回收规则。
- **虚拟样板不是伪造物品库存。** 由 AE2 `ICraftingProvider` 发布内存中的 `IPatternDetails`。
- **扫描发生在绑定或配方重载时。** AE 查询可合成物时只读取不可变快照，不在每 tick 全量遍历配方。
- **先接管、后转运。** `pushPattern` 成功前完整输入进入链接器持久暂存；机器变化或堵塞时材料留在缓冲，不部分丢失。

## 技术基线

| 组件 | 基线 |
| --- | --- |
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.219+ |
| Java | 21 |
| Applied Energistics 2 | 19.2.17 |
| JEI | 可选，仅客户端展示与调试 |
| Mekanism | 可选，第一批机器适配目标 |

## 开发

```bash
./gradlew test
./gradlew runClient
./gradlew runServer
./gradlew clean check build
```

完整说明从 [文档索引](docs/index.md) 开始；[当前支持矩阵](docs/product/support-matrix.md) 区分已实现、计划中与暂不支持，第一阶段任务见 [实施路线图](docs/roadmap.md)。

## 许可证

本项目自有代码使用 MIT License。第三方项目与素材归各自作者所有，详见 [NOTICE.md](NOTICE.md) 与 [许可和素材政策](docs/development/licensing-and-assets.md)。
