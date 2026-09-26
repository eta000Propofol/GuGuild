# GuGuild

Minecraft 26.1.2 Paper 公会插件。

## 功能
- 箱子 GUI + 底部功能键 + 翻页
- 公会创建/列表/加入/邀请/成员管理
- 每日签到与活跃值排行
- Vault 龙门币消费
- 手持物品公会图标
- PlayerTitle 公会称号
- 公会主城设置与传送参观
- 公会人数上限扩容

## 构建
需要 JDK 25 与 Maven 3.9+。

```powershell
mvnw.cmd package
```

产物：`target/GuGuild-1.1.1.jar`

## 安装
1. 将 `GuGuild-1.1.1.jar` 放入 Paper 服务端 `plugins/`。
2. 确保已安装 Vault 与经济插件，并可选安装 PlayerTitle。
3. 启动服务端，插件会在 `plugins/GuGuild/config.yml` 生成配置。
4. 玩家使用 `/guild` 或 `/gh` 打开公会界面。

## 命令
- `/guild create <名称>` 创建公会
- `/guild top` 公会列表
- `/guild sign` 每日签到
- `/guild invite <玩家>` / `/guild accept <公会>` / `/guild decline <公会>`
- `/guild seticon` 用手持物品设置图标
- `/guild sethome` 设置公会主城（会长，消耗 2000 龙门币）
- `/guild tp <公会>` / `/guild visit <公会>` 传送到公会主城参观
- `/guild setjointype <invite|free>`
- `/guild setvice <玩家>` / `/guild kick <玩家>` / `/guild transfer <玩家>`
- `/guild notice <文本>`
- `/guild title` / `/guild title buy <文字>`
- `/guild admin reload|delete|giveactive`


