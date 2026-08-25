# 配方发现：RecipeManager 与 JEI 边界

## 权威来源

服务端核心从当前世界的 `RecipeManager` 和机器公开 API 获取配方。0.1.0 的 JEI 插件只给绑定器和链接器增加使用说明；已发布虚拟样板由 AE2 自己的合成终端展示。这样不会为了 JEI 登录同步数千条配方，也不会向无关客户端泄露绑定目录。

公共服务端类的字段、方法签名和静态初始化都不能引用 JEI 类。仅仅把调用包在 `if (ModList...)` 中仍可能在类加载阶段崩溃。

## 索引流程

1. 为当前 RecipeManager 分配一代索引。
2. 每个 `MachineAdapter` 声明它关心的 RecipeType、机器类型和自动化约束。
3. 遍历相关类型一次，规范化输入、输出和条件。
4. 过滤随机、递归、环境依赖和不完整配方。
5. 生成 `RecipeFingerprint`，按稳定键排序并去重。
6. 保存成不可变共享目录；每个绑定只保存筛选结果或引用。
7. `/reload` 后生成新目录，与旧指纹集合做 diff。
8. 实际样板集合或 Grid 可用性变化时调用 `ICraftingProvider.requestUpdate`。

## KubeJS 与数据包重载

玩家登录不是配方重载，不能在每次登录重扫。真正的 `/reload`、数据包变化或 KubeJS 服务端脚本重载会让 RecipeManager 内容变化，必须清空旧代缓存并重建。旧 `IPatternDetails` 不得跨代继续发布。

## 去重与冲突

全局配方指纹与绑定调度键分离。全局 `RecipeFingerprint` 至少包含：

```text
adapter id + recipe id + normalized inputs + normalized outputs + schema
```

provider 侧的 `BindingPatternKey` 再组合 `binding id + recipe fingerprint`。这样共享索引与具体网络无关，同一配方可被多个链接器复用。

哈希编码使用结构化字段或长度前缀，不能简单用换行拼接；规范化内容中出现分隔符时会产生字段边界碰撞。

相同输出但输入不同的配方可以同时发布；完全相同输入/输出的多模组配方按配置去重或保留最高优先级。任何排序都必须稳定，不能依赖 HashMap 遍历顺序。

## Ingredient 具体化

AE2 处理样板最终需要具体 `GenericStack`。tag/Ingredient 配方要按当前 Registry 展开为具体物品，并有界处理：

- 每个 Ingredient 的代表项上限；
- 多 Ingredient 笛卡尔积上限；
- 组件/NBT 不同的同物品；
- 输出依赖具体输入的配方；
- 展开后完全相同的输入输出去重。

超出上限的配方记录过滤原因，不在后台无限组合。
