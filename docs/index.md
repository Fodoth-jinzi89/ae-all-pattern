# 文档索引

这套文档把 Mekanical Create 开发中积累的工程经验转换成 AE All Pattern 的可执行约定。正文使用中文，路径使用英文，便于代码引用与跨平台链接。

## 产品

- [愿景、范围与术语](product/vision-and-scope.md)
- [玩家操作流程](product/user-flow.md)
- [当前支持矩阵](product/support-matrix.md)
- [限制、风险与非目标](product/limitations.md)

## 架构

- [总体架构](architecture/overview.md)
- [绑定协议、持久化与安全](architecture/binding-and-persistence.md)
- [配方发现：RecipeManager 与 JEI 边界](architecture/recipe-discovery.md)
- [AE2 虚拟样板服务](architecture/virtual-pattern-provider.md)
- [机器适配器系统](architecture/machine-adapters.md)
- [客户端、服务端与紫色包围框](architecture/client-server-and-rendering.md)
- [性能、缓存与诊断](architecture/performance-and-cache.md)

## 开发

- [开发环境](development/environment-setup.md)
- [依赖、源码与资源下载](development/dependency-and-source-download.md)
- [启动、调试与常用任务](development/project-startup.md)
- [代码复用、继承、组合与反射](development/code-reuse-and-inheritance.md)
- [资源、数据生成与静态校验](development/resources-and-datagen.md)
- [Git、工作树与多版本移植](development/git-and-multiversion.md)
- [Minecraft 1.21.1 与 1.20.1 并行开发规范](development/dual-version-development.md)
- [许可、署名与素材政策](development/licensing-and-assets.md)

## 测试

- [测试策略](testing/strategy.md)
- [测试存档生成](testing/test-world-generation.md)
- [人工验收清单](testing/manual-checklist.md)

## 发布与运维

- [构建与平台发布](release/build-and-publish.md)
- [版本移植](release/version-porting.md)
- [热修复与回滚](release/hotfix-and-rollback.md)
- [故障排查](operations/troubleshooting.md)
- [玩家反馈模板](operations/support-template.md)

## 决策与计划

- [实施路线图](roadmap.md)
- [ADR-0001：服务端 RecipeManager 是配方真源](decisions/0001-recipe-manager-is-authority.md)
- [ADR-0002：使用自有 AE 网络锚点](decisions/0002-owned-ae-grid-anchor.md)
- [ADR-0003：机器能力通过适配器扩展](decisions/0003-machine-adapter-registry.md)
- [ADR-0004：发布虚拟样板而非生成实体样板](decisions/0004-virtual-not-physical-patterns.md)
- [参考资料](sources.md)
