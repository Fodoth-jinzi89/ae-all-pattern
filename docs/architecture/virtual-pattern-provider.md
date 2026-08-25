# AE2 虚拟样板服务

## 官方 API 路径

链接器的 `IManagedGridNode` 提供 `ICraftingProvider` 服务：

- `getAvailablePatterns()` 返回当前不可变 `List<IPatternDetails>`。
- `pushPattern(...)` 接受 AE2 已为该合成分配的输入。
- `isBusy()` 反映链接器/目标机器是否暂时不能接单。
- 目录变化后调用 `ICraftingProvider.requestUpdate(managedNode)`。

处理样板可用 `PatternDetailsHelper.encodeProcessingPattern(inputs, outputs)` 在内存编码，再用 `PatternDetailsHelper.decodePattern(stack, level)` 得到 `IPatternDetails`。编码得到的 `ItemStack` 只是构造官方样板描述的中间值，不放进玩家或方块库存。

## 为什么不用真实样板物品

- 数千配方会制造巨大的 NBT/组件库存与同步负担。
- 配方重载后实体样板容易过期。
- 玩家会误以为这些样板能被取出、复制或跨网络使用。
- `ICraftingProvider` 已经允许节点直接发布样板能力。

## pushPattern 与输入所有权

1. 验证传入 `IPatternDetails` 属于当前目录代数和绑定。
2. 根据指纹查找不可变 `RecipeSnapshot`，不重新全量搜索。
3. 从 `KeyCounter[]` 构造本轮确切输入。
4. 把完整输入从 counters 接管到链接器持久 `IncomingBuffer`；只有全部接管成功才返回接受。
5. MVP 单输入配方对机器执行一次模拟/提交。失败、机器忙碌或区块卸载时，输入仍归链接器所有并等待重试或退回 AE。
6. 多输入配方只有适配器能提供预留、独占或补偿回滚时才开放。普通多槽 capability 的多次 `execute` 不是数据库事务。
7. 转运完成后创建 `PendingCraft`，记录预期输出、数量、超时与目标位置。

这个模型保证材料不因“模拟成功、提交中途失败”而消失，但不声称任意外部机器拥有真正跨槽原子提交。输入缓冲与返回缓冲都必须随链接器持久化。

## 输出回收

MVP 推荐遵循 AE2 原生处理样板语义：机器把结果通过管道、导入总线或链接器返回网络。若链接器实现主动回收，必须只抽取本轮可确认的输出，不能偷走玩家或其他任务物品。并行任务需要绑定 ID + pattern key + batch sequence 进行追踪。

## Busy 与并行

第一版每个绑定只允许一个在途批次，保证可预测。一个链接器若绑定多个目标，`isBusy()` 只在所有目标都无法接受任务时返回 true；`pushPattern` 仍按 `BindingPatternKey` 路由并可拒绝某个忙碌目标。以后并行数由适配器显式声明，且要独立跟踪每个 lane 的输入、输出和超时。不能仅因机器有多个槽就假设它支持独立并行。
