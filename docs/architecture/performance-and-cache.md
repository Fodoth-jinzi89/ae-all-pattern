# 性能、缓存与诊断

## 复杂度目标

- 每次 RecipeManager 代数：相关配方全量遍历最多一次/适配器。
- 每个绑定：从共享索引筛选，不重新解析全部 Recipe JSON。
- 每次 AE craftables 查询：返回已构建不可变列表，复杂度与公开样板数线性，不访问世界库存。
- 每 tick：不扫描配方，不反射遍历机器类层级。

## 缓存层

1. `RecipeManager -> SharedRecipeIndex`：世界生命周期弱引用或显式卸载清理。
2. `Adapter + machine type -> RecipeCatalog`：不可变、按代数替换。
3. `Binding -> filtered fingerprints`：只保存稳定键和过滤配置。
4. `input signature -> candidate patterns`：有界 LRU，用于投料快速定位。

缓存必须有大小上限、命中/未命中统计和明确失效条件。不能把 Level、BlockEntity 或 Grid 放进进程级强引用 Map。

## 重载

- 玩家加入不清缓存。
- 数据包/KubeJS 真正重载时产生新代数。
- 先在服务端建立新不可变目录，再原子替换，避免 AE 读取半成品。
- 使用指纹 diff，只刷新受影响链接器。
- 大型整合包可分 tick 预热，但在目录完成前链接器报告“索引中”，不发布不完整结果。

## 预算和保护

- 每绑定默认最多公开 4096 张样板，可配置但有硬安全上限。
- 适配器反射深度不超过 64，候选类型数和扫描时间均有限制。
- 单 tick 刷新节点数量受预算控制。
- 连续失败的绑定指数退避，避免每 tick 重试不存在机器。

## 诊断

建议命令 `/aeallpattern perf` 输出：

```text
recipe generation, total/accepted/filtered
adapter scan milliseconds
catalog rebuild count and diff size
AE refresh count
input cache hit/miss/eviction
active/offline/invalid bindings
pushPattern accepted/rejected by reason
```

单元测试验证缓存淘汰和代数失效；spark 等采样工具只用于集成验证，不替代指标。
