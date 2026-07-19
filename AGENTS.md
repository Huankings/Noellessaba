# AGENTS.md

本文件是 NoellesRoles / Wathe / HarpyModLoader 扩展职业开发的本地协作说明。后续接到“新增职业、修职业机制、联动 Wathe/Harpy/其他扩展”的任务时，先按这里的路径和流程读源码，再决定修改范围。

## 固定项目路径

| 项目 | 路径 | 用途 |
| --- | --- | --- |
| NoellesRoles 当前开发仓库 | `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1` | 当前主要修改目标，新增职业通常落在这里。 |
| 职业需求 txt | `D:\哈比快车最新源码\noellesroles\txt要求\新职业` | 历史职业需求、后续简化提示词模板。 |
| Wathe | `D:\哈比快车最新源码\wathe\Wathe - 副本1` | 类狼人杀玩法本体，提供角色、游戏流程、商店、任务、回放、托盘、经济等公开 API。 |
| HarpyModLoader | `D:\哈比快车最新源码\harpymodloader\HarpyModLoader1` | 扩展职业加载、角色分配、强制角色、权重、扩展词条。 |
| StupidExpress | `D:\哈比快车最新源码\stupidexpress\StupidExpress2.1` | 其他扩展职业参考；含小偷、召集者、商店、经济、动作/客户端参考。 |
| kinssaba | `D:\哈比快车最新源码\kinswathe\kinssaba` | 其他扩展职业参考；含 KinsWatheRoles、商店、经济、词条、Noelles 联动。 |
| StarryExpress | `D:\哈比快车最新源码\starryexpress\StarryExpress1.3.2`，源码在 `...\src` | 其他扩展职业参考；注意构建应在父目录执行，不是在 `src` 里执行。 |

旧 txt 里出现的 spark、桌面旧路径等只作为历史语境；除非用户重新要求，否则以后以本文件上面的路径为准。

## 先读哪些源码

### Wathe / Harpy 基础

- `wathe/src/main/java/dev/doctor4t/wathe/api/Role.java`
- `wathe/src/main/java/dev/doctor4t/wathe/api/WatheRoles.java`
- `wathe/src/main/java/dev/doctor4t/wathe/api/Faction.java`
- `harpymodloader/src/main/java/org/agmas/harpymodloader/Harpymodloader.java`
- `harpymodloader/src/main/java/org/agmas/harpymodloader/modded_murder/ModdedMurderGameMode.java`

当前 Wathe 已经支持显式阵营注册：

- `WatheRoles.registerCivilianRole(role)`
- `WatheRoles.registerVigilanteRole(role)`
- `WatheRoles.registerKillerRole(role)`
- `WatheRoles.registerNeutralRole(role)`

新增职业优先用显式阵营注册，不要只依赖 `isInnocent/canUseKiller` 推断。Harpy 的扩展分配按 `role.getFaction()` 划分平民、义警、杀手、中立池；`Harpymodloader.setRoleMaximum(role/id, max)` 控制最大生成数。

### NoellesRoles 入口

- `src/main/java/org/agmas/noellesroles/Noellesroles.java`
- `src/main/java/org/agmas/noellesroles/NoellesRolesComponents.java`
- `src/main/java/org/agmas/noellesroles/ModItems.java`
- `src/main/java/org/agmas/noellesroles/NoellesRolesShops.java`
- `src/main/java/org/agmas/noellesroles/shop/NoellesRolesShopBootstrap.java`
- `src/main/java/org/agmas/noellesroles/roleassign/NoellesRolesRoleAssignedBootstrap.java`
- `src/main/java/org/agmas/noellesroles/record/NoellesRolesReplayFormatters.java`
- `src/client/java/org/agmas/noellesroles/client/NoellesrolesClient.java`
- `src/main/resources/fabric.mod.json`
- `src/main/resources/noellesroles.mixins.json`
- `src/client/resources/noellesroles.client.mixins.json`
- `src/main/resources/assets/noellesroles/lang/zh_cn.json`
- `src/main/resources/assets/noellesroles/lang/en_us.json`

### 当前仓库里的优先参考职业

- 服务员：`roles/waiter`，参考任务交互、托盘效果、被动透视、静态商店、回放。
- 追忆者：`roles/rememberer`，参考右键交互、冷却组件、成书、狙击枪、HUD、准星、心情 HUD。
- 风灵师：`roles/winder`，参考背包选人界面、能力键、标记、HUD、商店。
- 接线员：`roles/operator`，参考双人选择、语音/聊天桥接、持续状态、回放。
- 魔术师：`roles/magician`，参考复杂状态机、播放实体、动作记录、强制清理、回放。

### Wathe 公共 API 优先于新 mixin

能用公开 API 时优先用 API，少写对内部类的 mixin：

- 商店：`ShopApi.registerRoleShop`、`ShopApi.registerShopModifier`
- 金币 HUD / 被动收入：`EconomyApi`
- 任务收入和任务完成后效果：`TaskCompletionApi`
- 回放：`GameRecordManager` + `ReplayRegistry`
- 本能透视：`InstinctApi`
- 心情 HUD：`MoodHudApi`
- 名字 HUD：`RoleNameHudApi`
- 手持物品隐藏：`HeldItemInvisibilityApi`
- 胜利规则：`VictoryApi`
- 尸体外观：`BodyAppearanceApi`
- 托盘/床效果：`TrayEffectRegistry`、`BedEffectRegistry`
- 死亡保护链：`AllowPlayerDeath`，Noelles 侧还要看 `NoellesRolesDeathBootstrap` 和 `CommonForcedDeathHandler`

## 新职业开发流程

1. 先把用户需求拆成字段：职业名、英文 id、阵营、职业色、欢迎公告、技能、交互方式、冷却、商店、物品、HUD/UI、回放、死亡/胜利、兼容要求、是否要求先出方案。
2. 用 `rg` 搜本仓库已有相似实现；跨项目参考时只复制思路，不直接复制映射名。`noellesroles/harpy/kinssaba` 多为 Yarn 命名，`stupidexpress/starryexpress` 有 Mojang 官方命名痕迹。
3. 判断是否需要改 Wathe 或 Harpy。只要能在 NoellesRoles 侧通过 API 或窄 mixin 解决，就优先不动 Wathe/Harpy。
4. 如果用户要求“先分析方案”，先给方案，不改文件。否则按需求直接实现。
5. 所有玩法数值除职业 RGB 以外，放到该职业 `*Constants` 类里；冷却统一用 `GameConstants.getInTicks(min, sec)` 或明确 tick 常量。
6. 关键代码写详细中文注释，尤其是：为什么要这么接入 API、为什么要在服务端/客户端判断、为什么要同步组件、为什么要这样处理回合结束/玩家死亡/掉线。
7. 每个新增职业优先拆成独立包：`roles/<role_id>/` 放服务端逻辑、组件、常量、商店、能力处理；客户端对应放到 `client/mixin/roles/<role_id>/`、`client/instinct/roles/<role_id>/`、`client/ui/roles/<role_id>/` 等。
8. 新增功能完成后按“注册点检查清单”逐项核对，再编译。

## 注册点检查清单

新增职业通常需要检查这些位置：

- `Noellesroles.java`：`Identifier`、`Role` 注册、事件 id、死亡原因 id、`onInitialize` 初始化、payload codec、`registerPackets`、`registerEvents`、`registerEconomyApi`、`registerReplayFormatters`。
- `NoellesRolesComponents.java`：需要持久/同步状态时注册 CCA 组件。
- `fabric.mod.json`：新增 CCA 组件 id。
- `NoellesRolesRoleAssignedBootstrap.java`：职业分配后发初始物品、重置状态、设置开局冷却。
- `NoellesRolesShopBootstrap.java`：注册静态/动态职业商店，或 ShopModifier。
- `NoellesRolesShops.java`：购买特殊图标、即时能力物品、随机物品时的交付逻辑。
- `ModItems.java`：新增物品、数据组件、默认冷却。
- `NoellesrolesClient.java`：客户端按键、tooltip/model predicate、实体渲染、客户端网络包。
- `NoellesInstinctHandlers.java` / `NoellesAppearanceHandlers.java` / `NoellesHeldItemVisibilityHandlers.java`：本能、外观、手持隐藏注册。
- `noellesroles.mixins.json` / `noellesroles.client.mixins.json`：服务端和客户端 mixin 分开注册，环境要正确。
- `zh_cn.json` / `en_us.json`：职业名、欢迎公告、物品名、tooltip、HUD、actionbar、回放、死亡原因。
- `assets/noellesroles/models/item/*.json`、`textures/item/*.png`、`textures/gui/sprites/hud/*`：物品模型和 HUD 图标。
- `NoellesRolesReplayFormatters.java`：格式化器使用 `Text.translatable`，回放数据里存稳定 id/uuid/必要显示名，避免玩家掉线后显示未知。

## 角色注册规则

推荐格式：

```java
public static Identifier SOME_ROLE_ID = Identifier.of(MOD_ID, "some_role");
public static Role SOME_ROLE = WatheRoles.registerCivilianRole(new Role(
        SOME_ROLE_ID,
        SomeRoleConstants.ROLE_COLOR,
        true,
        false,
        Role.MoodType.REAL,
        WatheRoles.CIVILIAN.getMaxSprintTime(),
        false
));
```

常见阵营语义：

- 平民好人：`registerCivilianRole`，通常 `isInnocent=true`、`canUseKiller=false`、`MoodType.REAL`。
- 义警：`registerVigilanteRole`，需要替换原版义警位，不要混进普通平民池。
- 杀手：`registerKillerRole` 或旧七参构造中 `canUseKiller=true`，通常 `MoodType.FAKE`、`canSeeTime=true`。
- 中立：`registerNeutralRole`，不要让中立靠 `false,false` 被误归类后缺少结算/欢迎文本。

Harpy 会在 `refreshRoles()` 中自动给非特殊职业生成 announcement；Noelles 侧仍要补：

- `announcement.role.noellesroles.<id>`
- `announcement.goals.noellesroles.<id>`

## 组件和同步

需要跨 tick、跨客户端显示、死亡/回合重置的状态，优先建 CCA 组件：

- 玩家状态：`ComponentRegistry.getOrCreate(Identifier.of(MOD_ID, "<id>"), XxxPlayerComponent.class)`
- 世界状态：在 `registerWorldComponentFactories`
- 只给本人看的状态：`shouldSyncWith` 限制为本人。
- 所有人可见的被动透视/标记：`shouldSyncWith` 可以同步给所有人，再由客户端 handler 判断观看者身份。
- 重置入口：`ResetPlayerEvent`、职业分配 handler、回合结束 `GameEvents.ON_FINISH_FINALIZE`。

## 商店和经济

优先走 Wathe `ShopApi`：

- 完全替换职业商店：`ShopApi.registerRoleShop(role, provider)`。
- 只改默认杀手商店少数条目：`ShopApi.registerShopModifier(id, priority, handler)`。
- 购买时不要重复扣钱、播放音效、写购买回放；provider 的 `purchase` 只负责“是否真的交付成功”，公共结算由 Wathe `PlayerShopComponent` 处理。
- 需要金币 HUD 的非杀手职业，注册 `EconomyApi.registerBalanceHudRole(role)`。
- 需要普通被动收入，注册 `EconomyApi.registerPassiveIncomeRole(role)`。
- 任务金币走 `TaskCompletionApi.registerTaskIncomeProvider`；需要“任务完成后的特殊效果”走 `TaskCompletionApi.AFTER_TASK_COMPLETE`。

## 客户端与 mixin

- 客户端 HUD / 准星 / 屏幕 / 相机 / 手臂动作放 `src/client/java`，并注册到 `noellesroles.client.mixins.json`。
- 服务端逻辑、死亡链、物品行为、任务处理放 `src/main/java`，并注册到 `noellesroles.mixins.json`。
- mixin 条件必须尽量窄：判断玩家存活、当前职业、手持物品、世界是否 client/server、是否对局中。
- 背包玩家选择界面优先复用 `client/ui/common` 的分页/头像组件，避免某个职业 mixin 影响其他职业界面。

## 回放和语言

新增回放一般分两步：

1. 记录：`GameRecordManager.recordGlobalEvent(...)` 或 `GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)...record()`，必要时把 item id、target uuid、离线显示名、任务 key 等放进 extra。
2. 格式化：`ReplayRegistry.registerGlobalEventFormatter(EVENT_ID, NoellesRolesReplayFormatters::formatXxx)`。

格式化器里优先用 `Text.translatable("replay.global.noellesroles.xxx", ...)`，物品名用 `ItemStack#getName()` 或 `Text.translatable(item.getTranslationKey())`，不要把中文/英文字符串硬编码进回放数据，除非是为了玩家掉线兜底缓存。

## 编译和 jar 传递顺序

只改 NoellesRoles 时：

```powershell
cd "D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1"
.\gradlew.bat build
```

如果改了 Wathe：

1. 编译 Wathe：

```powershell
cd "D:\哈比快车最新源码\wathe\Wathe - 副本1"
.\gradlew.bat build
```

2. 把 `build\libs\wathe-*.jar` 复制到这些项目的 `libs`：

- `D:\哈比快车最新源码\harpymodloader\HarpyModLoader1\libs`
- `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\libs`
- `D:\哈比快车最新源码\stupidexpress\StupidExpress2.1\libs`
- `D:\哈比快车最新源码\kinswathe\kinssaba\libs`
- `D:\哈比快车最新源码\starryexpress\StarryExpress1.3.2\libs`

3. 如果 Wathe 改动影响 Harpy API 或分配逻辑，编译 Harpy：

```powershell
cd "D:\哈比快车最新源码\harpymodloader\HarpyModLoader1"
.\gradlew.bat build
```

再把 `harpymodloader-*.jar` 复制到 NoellesRoles 和其他扩展的 `libs`。

4. 编译 NoellesRoles，并在其他扩展依赖 Noelles 新 API 时把 `noellesroles-*.jar` 复制到 StupidExpress、kinssaba、StarryExpress 的 `libs`。

5. 编译扩展联调：

```powershell
cd "D:\哈比快车最新源码\stupidexpress\StupidExpress2.1"
gradle build

cd "D:\哈比快车最新源码\kinswathe\kinssaba"
.\gradlew.bat build

cd "D:\哈比快车最新源码\starryexpress\StarryExpress1.3.2"
.\gradlew.bat build
```

StupidExpress 当前按用户说明使用本机 `gradle build`。StarryExpress 的源码路径虽然给到 `src`，但构建目录是父目录 `StarryExpress1.3.2`。

## 后续新职业提示词模板

后续用户不需要重复项目路径，可以直接写：

```text
请按 AGENTS.md 的流程，在 NoellesRoles 中实现/分析新职业。
职业：中文名 / english_id / 阵营
颜色：RGB(...)
欢迎公告：
技能机制：
交互方式：
数值常量：
商店/经济：
新增物品/实体/资源：
HUD/UI/准星/本能/心情图标：
回放/死亡/胜利：
参考职业：
是否先给方案：
编译要求：
```

如果是修 bug，写“请按 AGENTS.md 先定位相关源码，再修复：现象、复现步骤、期望结果、是否只改 NoellesRoles、编译要求”即可。
