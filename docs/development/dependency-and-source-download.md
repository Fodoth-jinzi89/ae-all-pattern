# 依赖、源码与资源下载

## 依赖原则

- AE2 是必需实现依赖。
- Mekanism 是可选适配目标：`compileOnly` API + `localRuntime` 完整模组。
- JEI 是可选客户端依赖：`compileOnly` API + `localRuntime` 实现。
- 不把第三方 JAR 放进 Git，也不把上游源码复制到 `src/`。

## 让 Gradle 下载依赖

```bash
./gradlew dependencies
./gradlew compileJava
```

需要清理损坏缓存时先确认问题，再使用：

```bash
./gradlew --refresh-dependencies compileJava
```

不要日常删除整个 `~/.gradle`；这会重新下载所有项目依赖并隐藏真正的版本问题。

## 阅读上游源码

推荐顺序：

1. 官方文档和 Javadoc。
2. Gradle 下载的 sources JAR。
3. 对应版本 tag/branch 的 GitHub 源码。
4. 运行时反编译仅用于确认实现，不把结果粘贴进仓库。

例：AE2 官方 Maven Central 坐标为 `org.appliedenergistics:appliedenergistics2:<version>`，API-only 可使用 `:api` classifier。当前项目需要运行 AE2，因此开发环境使用完整 artifact。

## 验证真实 API

搜索类名时必须锁定相同 MC/模组版本。网上最新分支与 1.21.1 API 可能不同。每个架构决策都应在当前 sources JAR 中确认方法签名，例如：

- `ICraftingProvider#getAvailablePatterns/pushPattern/isBusy`；
- `ICraftingProvider.requestUpdate`；
- `PatternDetailsHelper.encodeProcessingPattern/decodePattern`。

## 素材

- 优先自己绘制纹理、模型和音效。
- JSON 可以在运行时引用 AE2 已安装资产命名空间，但不复制其 PNG。
- 如果确需派生素材，先核对许可证、保留署名并写入 `NOTICE.md`。
- Blockbench 源文件可保留在资源源目录，但构建 JAR 时排除不需要的工程文件。

## 离线与镜像

依赖下载失败先检查 Maven 仓库、VPN、TLS 和 Gradle 错误，不随意从网盘下载不明 JAR。团队缓存只能缓存原始 artifact，不改包内容；发布构建应能从声明的官方 Maven 重现。
