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
5. MVP 单输入配方优先尝试绑定面，并在不能完整接收时扫描适配器允许的其他输入面；每个候选都先模拟再提交。失败、机器忙碌或区块卸载时，输入仍归链接器所有并等待重试或退回 AE。
6. 多输入配方只有适配器能提供预留、独占或补偿回滚时才开放。普通多槽 capability 的多次 `execute` 不是数据库事务。
7. 转运完成后创建 `PendingCraft`，记录预期输出、数量、超时与目标位置。

这个模型保证材料不因“模拟成功、提交中途失败”而消失，但不声称任意外部机器拥有真正跨槽原子提交。输入缓冲与返回缓冲都必须随链接器持久化。

## 输出回收

链接器把“绑定机器的输出能力”视为该 AE 网络托管的回收边界。只要绑定与目标指纹仍有效，适配器就会持续扫描机器各面，把任何可抽取物品注回同一 Grid，不再与 `PendingCraft` 的期望产物匹配；旧库存、副产物和其他物流的结果同样会被回收。输入槽若未通过机器能力公开为可抽取槽则不会被触碰。

抽取前必须确认 ME 网络可完整接收该栈。模拟后若发生容量竞争，余量转为链接器持久拥有的 `RecoveredOutputs`，不能丢弃或再次从机器抽取。解绑或拆除链接器时，这些已拥有但尚未入网的物品必须返还给玩家。

第一版每个绑定只有一个在途批次，因此 `binding ID + pattern key` 足以消除同绑定内歧义；未来提高 lane 并发前必须增加 batch sequence。

## Busy 与并行

第一版每个绑定只允许一个在途批次，保证可预测。一个链接器若绑定多个目标，`isBusy()` 只在所有目标都无法接受任务时返回 true；`pushPattern` 仍按 `BindingPatternKey` 路由并可拒绝某个忙碌目标。以后并行数由适配器显式声明，且要独立跟踪每个 lane 的输入、输出和超时。不能仅因机器有多个槽就假设它支持独立并行。
