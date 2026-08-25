# 测试存档生成

## 目标

测试场应可重复生成，包含：

- AE 控制器/能源元件、终端、合成 CPU、链接器和返回路径。
- 原版熔炉、Mekanism 电炉和未来适配机器。
- 可烧物品、同输出冲突配方、KubeJS 动态配方与大量配方压力探针。
- 清晰标牌：预期操作、预期结果、版本和生成时间。

## 首选顺序

1. GameTest 模板：最可重复、最适合 CI。
2. 游戏内命令/结构方块：由游戏自己写合法 NBT。
3. 数据包函数或开发命令生成测试场。
4. 离线脚本直接编辑存档：仅在前面都不够时使用。

## 离线脚本安全流程

1. 正常停止客户端/服务器，确认没有进程占用 `session.lock`。
2. 明确目标存档绝对路径，禁止默认猜最新存档后直接写。
3. 对整个存档创建带时间戳 ZIP 备份。
4. 只读扫描目标坐标盒，确认没有玩家建筑、实体或方块实体。
5. Dry-run 输出将修改的 region、chunk、section、方块和 NBT 数量。
6. 用户明确确认后写入临时副本。
7. 重新解析全部改动 region，逐坐标断言 palette、方块实体 ID 与字段。
8. 先启动副本，再写真实存档。
9. 把玩家设为安全位置/创造飞行，避免悬空平台加载时坠落。

## 从 Mekanical Create 得到的具体教训

- 1.21.1 section palette 的 bit storage 不允许值跨 64-bit long；使用 `valuesPerLong = 64 // bits`，不能按连续位流打包。
- 1.20.1 与 1.21.1 的 Mekanism NBT 字段、ItemStack 计数类型、数据包目录和配方 JSON 不同，不能复制 region。
- 新多方块不能复制旧结构的 inventory UUID，否则缓存可能串联。
- 修改方块后需要让高度图与光照重算，避免旧光照数据错位。
- region 多文件写入不是天然事务；备份、临时副本和写后校验缺一不可。
- 测试配方必须先通过目标版本 Recipe Codec；字段看似合理也可能被 Create/NeoForge 拒绝加载。

## 本仓库工具

`build.gradle` 的 `generateGameTestTemplate` 会生成合法的空结构 NBT，四个核心 GameTest 在游戏内放置链接器、熔炉和可选 Mekanism 机器。`tools/testworld/generate.py` 生成更大人工测试场的确定性清单。`tools/testworld/build_lab_staging.py` 只为一个全新目录生成数据包、说明和压力配方；随后必须由 Minecraft 加载世界并执行 `function aeallpattern_test:build`，region、level NBT 和方块实体始终由游戏写入。脚本发现目标目录已存在时会拒绝运行。

完整存档的验收流程是：启动服务端、确认 5658 个配方加载、执行建场函数、按坐标断言关键方块、`save-all flush`、正常停服、重启后再次断言，最后停服并打包。若未来确需离线写世界，仍必须遵守以上安全流程。

## 不提交存档

`run/` 和生成存档始终 gitignore。仓库只提交：脚本、结构模板、数据包、清单和复现说明。玩家真实 UUID、聊天、坐标历史和其他个人数据不能进入版本库。
