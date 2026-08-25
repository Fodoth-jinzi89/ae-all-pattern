# 机器适配器系统

## 目的

适配器把“某台机器能展示什么配方”与“如何可靠自动化它”绑定在一起。通用反射只能用于受限的辅助探测，不能代替明确适配。

## 建议接口

```java
interface MachineAdapter {
    ResourceLocation id();
    int schemaVersion();
    boolean supports(ServerLevel level, BlockPos pos, BlockEntity target);
    RecipeCatalog discoverRecipes(RecipeManager recipes, RegistryAccess registries);
    InsertSimulation simulateInsert(MachineContext machine, RecipeSnapshot recipe, GenericStack[] input);
    CommitResult commitInsert(InsertSimulation simulation);
    BusyState busyState(MachineContext machine);
    OutputPolicy outputPolicy(RecipeSnapshot recipe);
}
```

模拟结果应捕获用于提交的槽位版本或能力快照，提交前再次验证。MVP 只开放单输入的一次插入；多输入必须有适配器级预留/回滚/独占策略。AE 输入已在链接器 `IncomingBuffer` 中持久化，因此转运失败不能吞物。

## 第一批适配器

### VanillaFurnaceAdapter

- 读取服务端 smelting RecipeType。
- 只把待烧物品作为 AE 输入；燃料策略需明确：机器自备燃料，或把燃料作为附加输入，不能隐式免费。
- 输出为确定性主产物。

### MekanismSmeltingAdapter

- 条件加载，公共核心签名不出现 Mekanism 类。
- 机器识别优先使用注册 ID/公开能力，配方转换使用 Mekanism API artifact；若确实需要具体机器类，只在隔离 compat 包中 `compileOnly` 完整 Mekanism artifact，不让类型泄漏到公共核心。
- 使用公开 recipe/input handler API，而不是直接改私有库存。
- 同时兼容基础电炉和工厂时，工厂 lane 数与侧面配置由适配器读取。
- 不继承 Mekanism 内部实现类来“借用”行为；优先组合公开能力。

## 第三方扩展

- 注册发生在统一 `MachineAdapterRegistry`。
- ID 唯一、排序稳定、优先级显式。
- 同一机器命中多个适配器时，默认拒绝并打印冲突；配置可选择指定适配器。
- 反射扫描必须有访问集、递归深度上限、候选数量上限，并捕获 `LinkageError` 与运行时异常。
- 适配器更新导致 schema 变化时，旧配方指纹失效并触发目录重建。
