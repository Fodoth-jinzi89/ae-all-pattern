# 绑定协议、持久化与安全

## BinderState

绑定器使用 Data Component 保存临时选择，不保存 Java 对象：

```text
schemaVersion
owner UUID
anchor dimension + BlockPos
anchor block/entity fingerprint
selection timestamp
```

绑定完成后，长期记录由链接器或世界 `SavedData` 管理：

```text
binding UUID
owner UUID
anchor reference
target dimension + BlockPos + clicked side
target block/entity fingerprint
adapter id + adapter schema
recipe filter/config
created/last validated time
```

## 服务端验证顺序

1. 确认交互来自服务端玩家，客户端包只表示请求。
2. 检查绑定器组件可解码且 schema 可迁移。
3. 检查玩家与两个位置的距离、维度和区块加载状态。
4. 解析链接器自己的 `IManagedGridNode`，不能复用旧 Grid 引用。
5. 校验链接器所有者；Grid Node 使用放置者作为 AE2 owning player，并要求当前在线、供能且有频道。
6. 校验目标方块实体类型指纹与可用适配器。
7. 检查目标没有被另一个不兼容绑定占用。
8. 原子写入绑定记录，随后才同步客户端和刷新 AE 目录。

## 为什么不保存 Grid ID 或 Grid 对象

AE 网络会因线缆拆除、频道、区块加载和服务器重启而拆分、合并或重建。内部 Grid 身份不是稳定存档 API。持久化只保存自有链接器位置，使用时从链接器节点重新解析当前 Grid。

## 迁移与恢复

- 每种记录都有独立 schema 版本。
- 解码失败时保留原数据副本并把记录标为“需要迁移”，不能让区块加载崩溃。
- 旧字段只在至少一个稳定版本周期后删除。
- 机器类型变化时不自动把材料投向新机器，要求重新确认绑定。
- 解绑或拆除链接器会把尚未转运的缓冲输入掉落到链接器位置；已进入目标机器的物品不重复补偿。
