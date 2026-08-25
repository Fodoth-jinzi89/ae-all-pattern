# 性能、缓存与诊断

## 复杂度目标

- 每次 RecipeManager 代数：相关配方全量遍历最多一次/适配器。
- 每个绑定：从共享索引筛选，不重新解析全部 Recipe JSON。
- 每次 AE craftables 查询：返回已构建不可变列表，复杂度与公开样板数线性，不访问世界库存。
- 每 tick：不扫描配方，不反射遍历机器类层级。

## 缓存层

1. `RecipeManager -> SharedRecipeIndex`：世界生命周期弱引用或显式卸载清理。
2. `Adapter + machine type -> RecipeCatalog`：不可变、按代数替换。
3. `Binding -> immutable virtual pattern list`：由稳定指纹构建，在绑定或配方代数变化时替换。

目录有 4096 硬上限、重建计数和明确失效条件。进程级缓存用 `RecipeManager` 弱键，值中不保存 Level、BlockEntity 或 Grid。

## 重载

- 玩家加入不清缓存。
- 数据包/KubeJS 真正重载时产生新代数。
- 先在服务端建立新不可变目录，再原子替换，避免 AE 读取半成品。
- 使用指纹 diff，仅在可见样板集合变化时通知 AE。
- 0.1.0 同步重建目录；大型整合包可在后续版本增加分 tick 预热。

## 预算和保护

- 每个适配器目录最多公开 4096 张样板，单个 ingredient 最多展开 64 个变体。
- 适配器不做反射扫描。
- 空闲 tick 不访问 RecipeManager 或重建虚拟样板；只处理小型持久输入队列和节点可用性变化。

## 诊断

命令 `/aeallpattern perf` 输出：

```text
recipe generation, total/accepted/filtered
adapter scan milliseconds
catalog rebuild count and diff size
AE refresh count
pushPattern accepted/rejected
```

单元测试验证缓存淘汰和代数失效；spark 等采样工具只用于集成验证，不替代指标。
