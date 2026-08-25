# 测试策略

## 测试金字塔

### 纯单元测试

不启动 Minecraft，覆盖：

- `RecipeFingerprint` 确定性和 schema 失效。
- 配方规范化、排序、去重与 tag 展开上限。
- 目录 diff、缓存代数和 LRU 淘汰。
- 绑定状态机与权限决策。
- 输入所有权转入持久 IncomingBuffer 的全有或全无，以及转运失败不丢物。
- ReturnBuffer 网络满时不丢物。

复杂规则应拆到不依赖 BlockEntity 的小类中，避免每次验证都启动游戏。

### 静态资源测试

覆盖 mods.toml、语言键、模型、loot、挖掘 tag、Data Component 持久化和版本一致性。

### GameTest

- 建立 AE 网络和链接器，绑定熔炉。
- 目录出现预期样板。
- 请求合成后一次性投入材料。
- 拆锚点、断频道、替换机器、输出堵塞和重载。
- Mekanism 已安装时测试电炉与工厂。

### 启动与人工测试

- 无 JEI 专用服务器启动。
- 有 JEI 客户端启动。
- 无 Mekanism 启动，compat 类不被加载。
- 带 Mekanism 启动并完成真实自动合成。
- 重启世界、区块卸载、网络拆分/合并。

## CI 门禁

每个 PR 至少运行：

```bash
./gradlew clean test check build
```

GameTest 稳定后加入独立 job。性能基准不必阻断每个提交，但发布前必须运行并保存结果。

## 回归测试原则

每个玩家问题先增加最小复现测试，再修复。无法轻量模拟的世界行为写 GameTest 或固定测试存档步骤，不能只依赖“我进游戏感觉没问题”。
