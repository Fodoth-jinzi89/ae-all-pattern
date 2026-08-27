# Minecraft 1.21.1 与 1.20.1 并行开发规范

本文记录 Mekanical Create 双版本维护中验证过的做法，并把它转换为 AE All Pattern 的开发约定。

这是一份维护流程，不是支持承诺。AE All Pattern 当前是否已经提供 1.20.1 版本，仍以 [当前支持矩阵](../product/support-matrix.md)为准。

## 结论先行

| 角色 | 分支 | 技术基线 | 责任 |
| --- | --- | --- | --- |
| 主版本 | `main` | Minecraft 1.21.1、NeoForge、Java 21 | 新功能、共享规则、架构决策、文档和测试语义的事实来源 |
| 辅助版本（建立后） | `mc/1.20.1` | Minecraft 1.20.1、Forge、Java 17 | 在旧版 API 上保持与主版本尽可能一致的玩家行为 |

默认只修改 `main`。只有需求明确包含“同步、适配、测试或发布 1.20.1”时，才操作 `mc/1.20.1`。

这里的“主”和“辅”只描述开发顺序，不代表 1.20.1 的质量可以降低。两个发布 JAR 都必须独立通过目标版本的构建和运行测试。

## 谁是事实来源

### `main` 决定的内容

- 玩家能看到的功能和交互语义。
- 绑定、配方发现、缓存、排序、冲突处理和安全转运规则。
- 诊断指标、错误分类和配置项含义。
- 纯 Java 算法及其回归测试。
- 通用文档、发布说明和版本行为定义。

### 各分支自己决定的内容

- Gradle 插件、Java 工具链和依赖版本。
- Forge 或 NeoForge 的注册、事件、网络和 capability 实现。
- AE2、Mekanism、JEI 在该 Minecraft 版本中的真实 API 用法。
- NBT、Data Component、SavedData 和方块实体的序列化签名。
- 数据包目录、JSON Codec、标签命名空间和生成资源。
- 目标版本专用的测试存档、运行目录和兼容模组 JAR。

因此，同步目标是“行为相同”，不是“源码逐行相同”。为了追求零差异而把 1.21.1 API 硬塞进 1.20.1，通常会制造更难排查的问题。

## 为什么以 1.21.1 为主

AE All Pattern 当前代码和依赖以 1.21.1 为基线，注册、网络、Data Component、AE2 与可选兼容层都已经围绕 NeoForge 和 Java 21 建立。新设计先在这个基线上完成，反馈回路最短，也不会为了旧 API 过早扭曲公共架构。

1.20.1 仍然重要，但它更适合作为长期兼容线：接受已经明确的行为，再用 Forge、Java 17 和相应上游版本实现同样的结果。

## 使用独立工作树

两个版本必须同时存在于不同目录，不能在同一个含有运行目录和未提交改动的目录里来回切分支。

首次创建辅助版本：

```bash
cd <repo-parent>/ae-all-pattern
git fetch origin
git branch --list mc/1.20.1
git ls-remote --heads origin mc/1.20.1
git worktree add -b mc/1.20.1 ../ae-all-pattern-1.20.1 origin/main
```

最后一个参数必须是经过确认的起点。新建兼容线时通常使用 `origin/main`；如果项目已经选定了更早的移植基线，则显式换成该 commit SHA。不要省略起点，否则 Git 会从调用命令时碰巧检出的 `HEAD` 创建分支。

远端分支已经存在时：

```bash
cd <repo-parent>/ae-all-pattern
git fetch origin
git worktree add -b mc/1.20.1 ../ae-all-pattern-1.20.1 origin/mc/1.20.1
```

本地已经有 `mc/1.20.1` 分支、只是尚未挂载工作树时：

```bash
git worktree add ../ae-all-pattern-1.20.1 mc/1.20.1
```

约定目录：

```text
<repo-parent>/ae-all-pattern         main / MC 1.21.1
<repo-parent>/ae-all-pattern-1.20.1  mc/1.20.1 / MC 1.20.1
```

每个工作树拥有独立的 `run/`、存档、配置、日志和崩溃报告。运行中的游戏也必须先确认对应的是哪个目录，不能用 1.21.1 客户端验证 1.20.1 JAR，反之亦然。

## 两个版本的关键差异

下表是移植时必须主动检查的边界。具体版本号仍以各分支的 `gradle.properties` 为准。

| 边界 | 1.21.1 主版本 | 1.20.1 辅助版本 | 同步方式 |
| --- | --- | --- | --- |
| 加载器 | NeoForge | Forge | 平台入口和事件注册分别实现 |
| Java | 21 | 17 | 禁止把 `List#getFirst`、`removeLast` 等 Java 21 写法直接带过去 |
| 构建插件 | ModDevGradle 的 NeoForge 模式 | ModDevGradle LegacyForge 或目标分支选定的 Forge 工具链 | `build.gradle` 保持分支本地化 |
| 注册 API | NeoForge `DeferredRegister`/holder 类型 | Forge 注册类型 | 保留注册意图，按目标 API 重写 |
| 网络 | payload、`StreamCodec`、`RegisterPayloadHandlersEvent` | Forge 对应网络通道和编码器 | 包内容一致，传输层分开 |
| capability | NeoForge block capability 查询 | `ForgeCapabilities`/Forge capability 查询 | 适配器对外语义一致，查询代码分开 |
| 持久化 | Data Component、带 registry provider 的保存/加载签名 | 旧 ItemStack NBT、旧 SavedData/BlockEntity 签名 | 定义共同 schema 含义，分别编码 |
| 配方 API | 新版 `RecipeHolder`、RecipeManager 与 Codec | 目标版本的配方容器和查询签名 | 配方指纹规则共享，枚举代码适配 |
| Mekanism 数值 | 新版能量 API 通常以 `long` 表示 | 10.4 系列仍大量使用 `FloatingLong` | 公式共享，容器、同步器和比较方法适配 |
| 标识与物品工具 | `ResourceLocation.fromNamespaceAndPath`、`copyWithCount`、组件比较 | 旧构造方式、copy 后 `setCount`、NBT/tag 比较 | 逐个替换，不引入 Java 21/新版组件方法 |
| 资源目录 | `recipe/`、`loot_table/`、`tags/block/` | `recipes/`、`loot_tables/`、`tags/blocks/` | 不能机械复制目录 |
| 通用标签 | 通常使用 `c:` | 通常使用 `forge:` | 每个分支加静态资源校验 |
| JSON 字段 | 新版 recipe/loot/component Codec；机器物品持久化可用 `copy_components` | 旧版 `item`、NBT 和 loot function；机器物品通常用 `copy_nbt` | 以目标游戏实际加载为准，并做资源回归测试 |
| 上游模组 | 1.21.1 对应 AE2/Mekanism/JEI | 1.20.1 对应 AE2/Mekanism/JEI | 先读目标版本 sources，禁止按记忆猜签名 |
| 测试世界 | 1.21.1 DataVersion 与 NBT | 1.20.1 DataVersion 与 NBT | 用两个版本各自的脚本和原生模板生成 |

尤其不能直接跨版本复制 region 文件或方块实体 NBT。即使方块 ID 相同，字段命名、ItemStack 表示和 DataVersion 也可能不同。

Mekanical Create 的并行维护暴露过三类典型移植风险：把 Java 21 的集合方法带进 Java 17；把 1.21.1 的 `c:` 标签和单数数据目录复制到 Forge 1.20.1；以及用同一份 loot JSON 保存两代 Mekanism 机器数据。它们有的能在编译期报错，有的只会在玩家挖掉机器或加载配方时静默丢数据，因此静态资源测试和实际世界重启测试都不能省略。

## 代码如何分层，才能减少重复劳动

### 优先共享的纯逻辑

以下代码应尽量不依赖 Minecraft、加载器或上游模组类：

- 配方指纹、稳定排序和去重。
- 缓存键、LRU、代数和增量 diff。
- 绑定状态机的纯规则。
- 路由策略和冲突选择。
- 输入预留、输出预算和失败分类的抽象模型。
- 性能统计的数学部分。

这些文件和 JUnit 测试适合用小提交直接移植，必要时可以 cherry-pick。

### 必须分版本实现的平台层

- 模组入口、注册和事件总线。
- 客户端初始化、屏幕和渲染事件。
- 网络包注册、编码和发送。
- capability 查询和物品/流体/化学品转运。
- SavedData、Data Component、ItemStack 与方块实体持久化。
- AE2 `ICraftingProvider`、grid node 和 pattern 编解码接入。
- Mekanism、JEI 与其他可选兼容层。

公共接口可以保持概念一致，但实现不应通过大段反射强行合并。优先用小型 platform/compat adapter 隔离差异。

### 资源与测试也属于代码

语言文本和贴图通常可以复用，recipe、loot、tag、模型状态和数据生成器必须按目标版本验证。纯 JUnit 可一起移植；GameTest、客户端截图、专服启动和测试存档必须分别维护。

## 从 1.21.1 同步到 1.20.1

### 1. 开工前保护现场

在两个目录分别执行：

```bash
git status --short --branch
git diff --check
git diff --cached --check
```

如果目标文件已有用户未提交改动，先停下并划清范围。不要 reset、checkout 或覆盖这些改动。确认没有游戏进程占用待修改的测试存档；修改存档前先做完整备份。

### 2. 先在 `main` 完成真实功能

1. 在 1.21.1 实现行为。
2. 把复杂规则拆成纯 Java 类并补回归测试。
3. 验证可选依赖隔离，避免客户端类进入专服公共路径。
4. 运行目标测试和客户端/专服验证。
5. 形成小而完整的提交并推送 `main`。

推荐门禁：

```bash
./gradlew test
./gradlew runGameTestServer
./gradlew clean check build
```

需要人工体验的改动再运行：

```bash
./gradlew runClient
./gradlew runServer
```

### 3. 给改动分类

移植前先看主版本提交，而不是直接合并分支：

```bash
git show --stat <main-commit>
git diff <main-commit>^ <main-commit> -- src docs
```

把文件分成三组：

1. **可直接移植**：纯逻辑、纯测试、普通文档。
2. **保留意图后改写**：加载器、Minecraft、AE2、Mekanism、JEI、网络和持久化代码。
3. **不得覆盖**：目标分支的 Gradle 基线、元数据模板、运行配置和版本专用资源。

### 4. 选择移植方法

只有提交足够原子、主要由纯逻辑组成时，才使用：

```bash
cd <repo-parent>/ae-all-pattern-1.20.1
git fetch origin
git cherry-pick -x <main-commit>
```

出现冲突后，先按目标版本 sources 和现有代码解冲突，运行测试，再执行：

```bash
git add <resolved-files>
git cherry-pick --continue
```

如果冲突落在 Gradle 基线、加载器 API、持久化 schema 或版本专用资源上，通常说明这个提交并不适合直接 cherry-pick。此时应执行 `git cherry-pick --abort` 恢复现场，再按照下面的手工移植方式重做；不要为了保留 cherry-pick 形式而勉强合并平台代码。

`-x` 会自动把来源 commit 写入提交正文，适合真正的 cherry-pick。如果一个提交混合了大量 NeoForge 集成代码，优先手工移植行为，不要整分支 merge，也不要对冲突文件一律选择 `ours` 或 `theirs`。手工移植形成的新提交应记录来源：

```text
port: sync virtual pattern refresh to Forge 1.20.1

Ported-from: main@<main-commit>
```

直接 cherry-pick 的 `(cherry picked from commit ...)` 与手工移植的 `Ported-from` trailer 都是有效的来源记录。不同版本的提交哈希本来就不会相同。

### 5. 维护同步台账

手工 API port 不会产生 patch-equivalent commit，因此 `git cherry`、相同提交标题或“两个分支版本号一样”都不能证明功能已经同步。每批同步都应在 PR、发布准备记录或对应 issue 中维护如下表格：

| 功能/修复 | main SHA | 1.20.1 SHA | 状态 | 允许的版本差异 |
| --- | --- | --- | --- | --- |
| 示例：配方目录增量刷新 | `abc1234` | `def5678` | 已验证 | 仅网络注册与 capability 查询不同 |

需要复查某个来源提交时，可以使用 commit trailer 搜索：

```bash
git log mc/1.20.1 --grep='Ported-from: main@abc1234' --oneline
```

一个 main 提交被拆成多个 1.20.1 提交时，台账要列出全部目标 SHA；一个功能明确不支持旧版时，也要写“不会移植”和原因，不能留成无法判断的空白。

台账状态至少区分“待移植”“已移植”“旧版早已具备（附验证）”和“不适用”。Mekanical Create 就出现过主线新增 `copy_components` 提交、而 1.20.1 早已用 `copy_nbt` 实现同一拆机保留行为的情况：目标分支没有对应新 SHA，并不等于功能缺失。台账必须记录行为验证，不能只比较 Git 历史。

### 6. 逐层适配

建议顺序如下：

1. 纯 Java model/policy/cache 与对应测试。
2. Minecraft 数据类型、配方枚举和序列化。
3. AE2 grid、provider 和 pattern 编解码。
4. Forge 注册、网络、capability 与客户端事件。
5. Mekanism、JEI 及其他可选兼容层。
6. 资源目录、标签、recipe、loot 和元数据。
7. GameTest、测试存档和人工验收场景。

每层能编译后再进入下一层，避免一次积累数百个互相遮蔽的错误。

主版本也应按相同边界拆提交：纯逻辑与单测、平台集成、资源、版本号与发布元数据尽量分开。这样一条主线提交通常可以对应一条维护线提交；若必须合并或拆分来源，必须在同步台账中列出全部 SHA，不能把多个无关功能压成一个难以追踪的 port。

### 7. 独立验证辅助版本

1.20.1 必须在自己的工作树执行完整门禁：

```bash
cd <repo-parent>/ae-all-pattern-1.20.1
./gradlew test
./gradlew runGameTestServer
./gradlew clean check build
```

然后至少验证：

- 无 JEI、无 Mekanism 的最小专服能够启动。
- 完整依赖客户端能够进入测试世界。
- 绑定、重启、拆除、配方 reload 和网络拆分不会丢状态。
- AE 能看到相同的虚拟样板，并能安全投料和回收输出。
- 版本专用资源全部加载，日志中没有 recipe、loot、tag 或 mixin 错误。
- JAR 内没有误打包 AE2、Mekanism、JEI 或加载器类。
- Java 17 toolchain 生成的 1.20.1 字节码能被 Java 17 运行时实际加载；Gradle launcher 使用更高版本 JDK 不能替代这项验证。
- 目标版本上一发布版的存档升级后，绑定、缓冲区和虚拟样板仍能恢复。

### 8. 提交、推送和发布

默认在各自的 topic branch 上提交并发起 PR，不直接把整个本地版本分支推到受保护分支。推送前记录精确 HEAD、检查提交范围，并确认这次任务已经获得推送授权：

```bash
git status --short --branch
git rev-parse HEAD
git log --oneline origin/main..HEAD
git push -u origin feat/<feature-name>

cd <repo-parent>/ae-all-pattern-1.20.1
git status --short --branch
git rev-parse HEAD
git log --oneline origin/mc/1.20.1..HEAD
git push -u origin port/<feature-name>-mc1.20.1
```

PR 分别合入 `main` 与 `mc/1.20.1`。只有仓库策略明确允许、用户明确授权且待推送提交已经逐个核对时，才直接更新长期分支。

同一功能版本可以放在一个 GitHub Release 中，但两个 JAR 必须标明 MC 版本和加载器：

```text
aeallpattern-1.21.1-neoforge-<version>.jar
aeallpattern-1.20.1-forge-<version>.jar
```

发布说明分别列出依赖、最低加载器版本、已知差异和测试结果。不要因为两者使用相同产品版本号，就假设二进制可以互换。

发布记录还应保存两个候选 commit SHA、两个最终文件名和各自的 SHA-256。平台上传完成后重新下载并核对哈希，避免本地产物、GitHub Release 与分发平台漂移。

## 反向发现问题时怎么处理

玩家可能先在 1.20.1 发现问题。先判断问题属于哪一类：

### 共享语义缺陷

例如配方指纹碰撞、缓存失效、物品预留不原子或绑定状态机错误：

1. 在纯逻辑层补一个能描述问题的回归测试。
2. 让 `main` 成为最终行为定义并完成修复。
3. 再把同一语义移植回 1.20.1。

紧急情况下可以先发 1.20.1 hotfix，但不能让共享语义只存在于辅助分支；随后必须把对应修复落回 `main`。

### 版本专用缺陷

例如 Forge capability、1.20.1 AE2 签名、旧 Codec 或旧资源目录问题，只修改 `mc/1.20.1`，并在提交信息中明确它不是共享行为变更。不要为了“代码一样”把旧 API workaround 搬到 `main`。

## 同步完成的判定

同步不是“冲突标记消失”或“能够编译”。只有同时满足以下条件才算完成：

- 玩家语义与主版本一致，或差异已在支持矩阵和发布说明中记录。
- 共享算法的回归测试在两个分支均通过。
- 目标版本 API、资源和持久化使用目标版本的真实格式。
- 目标版本客户端、专服和 GameTest 独立通过。
- 兼容依赖缺失时不会 classload 崩溃。
- 测试存档由目标版本生成或转换脚本验证，且写入前有备份。
- 辅助分支提交带 `-x` 生成的来源记录或 `Ported-from` trailer，可以追溯到主线提交。
- 两个 JAR 的名称、元数据和依赖范围正确。

## 常见错误

- 在同一个目录频繁 checkout 两个版本，导致 `run/`、IDE 和未提交资源串线。
- 未经明确要求顺手修改或发布 1.20.1。
- 把 `main` 整分支 merge 到 `mc/1.20.1`，再一次性解决所有冲突。
- 只改包名让 NeoForge 代码“看起来像 Forge”。
- 把 Java 21 集合方法带进 Java 17。
- 直接复制 1.21.1 的 NBT、region、recipe、loot 或 tag 到 1.20.1。
- 在公共服务端类签名中引用可选客户端或兼容模组类。
- 只运行 `compileJava`，没有运行单测、资源校验、专服和客户端。
- 辅助版本修改没有记录对应主线提交，下一次同步只能靠猜。
- 发布时用含糊的 JAR 文件名，导致玩家把 Forge 与 NeoForge 文件装反。

## 每次同步的简短清单

```text
[ ] 用户明确要求操作 1.20.1
[ ] 两个工作树状态已检查，未覆盖现有改动
[ ] main 已实现、测试、提交并推送
[ ] 已记录待移植的 main commit
[ ] 纯逻辑与平台代码已分类
[ ] Forge/Java 17/API/资源/NBT 已逐项适配
[ ] 1.20.1 单测、GameTest、check、build 已通过
[ ] 1.20.1 客户端与专服已验证
[ ] port commit 含 -x 来源记录或 Ported-from trailer
[ ] 两个 JAR 元数据和文件名已核对
[ ] 支持矩阵与发布说明已同步
```
