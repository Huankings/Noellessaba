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
- `wathe/src/main/java/dev/doctor4t/wathe/game/GameConstants.java`
- `wathe/src/main/java/dev/doctor4t/wathe/cca/GameWorldComponent.java`
- `wathe/src/main/java/dev/doctor4t/wathe/cca/PlayerStaminaComponent.java`
- `wathe/src/main/java/dev/doctor4t/wathe/api/stamina/PlayerStaminaApi.java`
- `wathe/src/main/java/dev/doctor4t/wathe/api/movement/PlayerMovementApi.java`
- `wathe/src/main/java/dev/doctor4t/wathe/command/SetMoodCommand.java`
- `wathe/src/main/java/dev/doctor4t/wathe/command/MoodStaminaPenaltyCommand.java`
- `wathe/src/main/java/dev/doctor4t/wathe/mixin/PlayerEntityMixin.java`
- `wathe/src/main/java/dev/doctor4t/wathe/mixin/LivingEntityMixin.java`
- `harpymodloader/src/main/java/org/agmas/harpymodloader/Harpymodloader.java`
- `harpymodloader/src/main/java/org/agmas/harpymodloader/modded_murder/ModdedMurderGameMode.java`

当前 Wathe 已经支持显式阵营注册：

- `WatheRoles.registerCivilianRole(role)`
- `WatheRoles.registerVigilanteRole(role)`
- `WatheRoles.registerKillerRole(role)`
- `WatheRoles.registerNeutralRole(role)`

新增职业优先用显式阵营注册，不要只依赖 `isInnocent/canUseKiller` 推断。Harpy 的扩展分配按 `role.getFaction()` 划分平民、义警、杀手、中立池；`Harpymodloader.setRoleMaximum(role/id, max)` 控制最大生成数。

### NoellesRoles 入口与注册拆分

- `src/main/java/org/agmas/noellesroles/Noellesroles.java`：Fabric 主入口，只调用 `NoellesRolesBootstrap.init()`；不要再把职业、事件、packet、经济、回放等注册塞回这里。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesBootstrap.java`：总启动编排器，负责维护初始化顺序。
- `src/main/java/org/agmas/noellesroles/registry/NoellesRolesCore.java`：`MOD_ID`、日志器和 `Identifier` 工具方法。
- `src/main/java/org/agmas/noellesroles/registry/NoellesRoleIds.java`：职业和词条的稳定 id。
- `src/main/java/org/agmas/noellesroles/registry/NoellesRoleRegistry.java`：Wathe `Role` 实例和显式阵营注册。
- `src/main/java/org/agmas/noellesroles/registry/NoellesModifierRegistry.java`：Harpy 词条注册。
- `src/main/java/org/agmas/noellesroles/registry/NoellesRoleGroups.java`：跨系统共享角色分组，例如 `KILLER_SIDED_NEUTRALS`；它不再从 `Noellesroles.X` 导出。
- `src/main/java/org/agmas/noellesroles/registry/NoellesDeathReasons.java`：专属死亡原因 id。
- `src/main/java/org/agmas/noellesroles/registry/NoellesEventIds.java`：回放事件、托盘/床效果和护盾来源 id。
- `src/main/java/org/agmas/noellesroles/registry/NoellesFramingShopEntries.java`：伪装商店共享条目。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesPayloadTypes.java`：payload codec 注册。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesPacketReceivers.java`：服务端 packet receiver 注册和能力分发。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesEventBootstrap.java`：事件监听、server tick、回合清理、Harpy 禁用职业配置同步。
- `src/main/java/org/agmas/noellesroles/combat/NoellesRolesCombatBootstrap.java`：Wathe `GunShotApi` 接入总引导，只调用各职业/词条自己的枪击、左轮惩罚和冷却 handler。
- `src/main/java/org/agmas/noellesroles/death/NoellesRolesDeathBootstrap.java`：`AllowPlayerDeath` 保护链和 Wathe `DeathApi` 分阶段击杀流程接入总引导。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesMovementBootstrap.java`：体力与移动速度修正总引导。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesEconomyBootstrap.java`：金币 HUD、任务收入、被动收入和经济词条接入。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesReplayBootstrap.java`：回放 formatter 注册。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesPsychoBootstrap.java`：疯魔 API 接入分发器，只调用各职业自己的 `*PsychoHandler.init()`。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesMoodTaskBootstrap.java`：Wathe 心情任务 API 接入分发器，只调用各职业 / 词条自己的 `*MoodTaskHandler.init()`。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesBlackoutBootstrap.java`：Wathe 停电 API 接入分发器，只放分组规则和各职业 `*BlackoutHandler.init()` 调用。
- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRoleLimitsBootstrap.java`：Harpy 静态角色上限初始化；人数相关动态上限在 `NoellesRolesEventBootstrap`。
- `src/main/java/org/agmas/noellesroles/roles/hacker/HackerRoleAssignmentRules.java`：Harpy 职业分配 API 接入，阻止 Hacker / Mimic 同局随机生成。
- `src/main/java/org/agmas/noellesroles/roles/initiate/InitiateRoleAssignmentRules.java`：Harpy 阶段回调接入，补齐第二名初学者。
- `src/main/java/org/agmas/noellesroles/modifiers/lovers/LoversModifierAssignmentRules.java`：Harpy 词条公告前回调接入，消费强制恋人配对。
- `src/main/java/org/agmas/noellesroles/modifiers/dual_personality/DualPersonalityModifierAssignmentRules.java`：Harpy 词条分配生命周期接入，刷新双重人格上限并消费强制配对。
- `src/main/java/org/agmas/noellesroles/NoellesRolesComponents.java`
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperWorldComponent.java`：时停者世界级快照历史、回溯播放游标、保护名单。
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperRiftHandler.java`：时间狭缝入口、动态失效检测、以及排除狭缝玩家后的胜利收束判断。
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperSnapshots.java`：时停者回溯的玩家 / 世界组件快照白名单和恢复逻辑；新增运行态组件后必须检查这里。
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperWorldStateSnapshot.java`：时停者回溯覆盖的门、火等可控世界状态。
- `src/main/java/org/agmas/noellesroles/ModItems.java`
- `src/main/java/org/agmas/noellesroles/NoellesRolesShops.java`
- `src/main/java/org/agmas/noellesroles/shop/NoellesRolesShopBootstrap.java`
- `src/main/java/org/agmas/noellesroles/roleassign/NoellesRolesRoleAssignedBootstrap.java`
- `src/main/java/org/agmas/noellesroles/record/NoellesRolesReplayFormatters.java`
- `src/client/java/org/agmas/noellesroles/client/NoellesrolesClient.java`
- `src/client/java/org/agmas/noellesroles/client/hud/NoellesHudHandlers.java`：通用屏幕 HUD 总注册入口，只调用各职业自己的 `register()`。
- `src/client/java/org/agmas/noellesroles/client/hud/NoellesHudSupport.java`：Wathe `HudOverlayApi` 的存活职业过滤、右下角文字和玩家名兜底辅助。
- `src/client/java/org/agmas/noellesroles/client/movement/NoellesClientMovementBootstrap.java`：客户端移动表现总注册入口。
- `src/client/java/org/agmas/noellesroles/client/roles/<role>/*StatusHud.java`：各职业自己的普通屏幕 HUD provider。
- `src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/*Hud.java`：各词条自己的固定屏幕 HUD provider。
- `src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtons.java`：背包按钮总注册入口，只调用各职业自己的 `register()`。
- `src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtonSupport.java`：背包按钮的 Wathe API 接入、分页、在线玩家和头像列表共享工具。
- `src/client/java/org/agmas/noellesroles/client/ui/roles/<role>/*InventoryButtons.java`：各职业自己的背包按钮 provider。
- `src/client/java/org/agmas/noellesroles/client/ui/modifiers/<modifier>/*InventoryButtons.java`：词条自己的背包按钮 provider。
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
- 时停者：`roles/timekeeper`，参考新货币、物品多状态、商店保护、全局快照、时间回溯、时间狭缝、语音/聊天隔离；新增 CCA 运行态组件时重点参考 `TimekeeperSnapshots`，新增独立胜利或阻拦普通结算的规则时重点检查 `TimekeeperRiftHandler`。

### Wathe 公共 API 优先于新 mixin

能用公开 API 时优先用 API，少写对内部类的 mixin：

- 商店：`ShopApi.registerRoleShop`、`ShopApi.registerShopModifier`
- 金币 HUD / 被动收入：`EconomyApi`
- 心情任务注册、指定发放、发放前拦截、完成拦截：`MoodTaskApi`
- 心情任务点透视：`MoodTaskPointApi`
- 任务收入和任务完成后效果：`TaskCompletionApi`
- 回放：`GameRecordManager` + `ReplayRegistry`
- 本能透视：`InstinctApi`
- 心情 HUD：`MoodHudApi`
- 通用屏幕 HUD：`HudOverlayApi`、`HudOverlayContext`、`HudOverlayLayer`、`HudOverlayLayout`
- 准心图标 / 准心下方小进度条：`CrosshairHudApi`
- 准心名字 / 实体名牌 / 准心额外 HUD：`RoleNameHudApi`
- 玩家 / 尸体隐藏、不可选中、不可交互和不可攻击：`TargetVisibilityApi`
- 手持物品隐藏：`HeldItemInvisibilityApi`
- 疯魔模式：`PsychoModeApi`、`PsychoModeProfile`、`PsychoShieldContext`、`PsychoShieldResult`，客户端皮肤/音乐用 `PsychoModeClientApi`
- 停电机制：`BlackoutApi`、`BlackoutDuration`、`BlackoutEffectResult`；恢复供电、改停电时长、分配停电夜视/失明都走这里，不要 mixin `WorldBlackoutComponent` ticks。
- 玩家物理碰撞：`PlayerCollisionApi`、`PlayerCollisionMode`；硬阻挡、原版推挤可穿过、完全无碰撞无推挤都走这里，不要再 mixin `Entity#collidesWith`、`EntityView#getEntityCollisions` 或推挤方法。
- 玩家体力：`PlayerStaminaApi`、`PlayerStaminaComponent`
- 玩家移动速度：`PlayerMovementApi`
- 枪击、目标覆写、左轮反火、冷却修正：`GunShotApi`、`GunShotContext`、`GunTargetContext`、`RevolverPenaltyContext`
- 客户端武器目标选择：`WeaponTargetingApi`
- 击杀/死亡分阶段流程、默认击杀奖励、尸体生成回调：`DeathApi`、`DeathContext`、`BodySpawnContext`
- 背包按钮：`InventoryButtonApi`、`InventoryScreenType`、`InventoryButtonContext`、`InventoryPageState`、`InventoryPageSwitchWidget`
- 胜利规则：`VictoryApi`
- 尸体外观：`BodyAppearanceApi`
- 托盘/床效果：`TrayEffectRegistry`、`BedEffectRegistry`
- 死亡保护链：`AllowPlayerDeath`，Noelles 侧还要看 `NoellesRolesDeathBootstrap` 和 `CommonForcedDeathHandler`

Harpy 分配规则也优先走公开 API，不要再 mixin `ModdedMurderGameMode#findAndAssignPlayers` 或 `assignModifiers`：

- 职业互斥 / 单向排斥：`RoleAssignmentApi.registerMutualExclusion(...)`、`registerOneWayExclusion(...)`。
- 职业绑定生成或阶段补位：`registerBeforePhaseHandler(...)`、`registerAfterPhaseHandler(...)`，补最终职业用 `RoleAssignmentPhaseContext.assignRole(...)`。
- 词条与职业绑定 / 排斥：`ModifierAssignmentApi.registerModifierRequiresRole(...)`、`registerModifierExcludesRole(...)`。
- 同玩家词条互斥：`registerModifierMutualExclusion(...)`、`registerModifierOneWayExclusion(...)`。
- 强制恋人、强制双重人格、动态词条上限：用 `registerBeforeAssignmentHandler(...)` / `registerBeforeAnnouncementHandler(...)` / `registerAfterAssignmentHandler(...)` 替代词条分配 mixin。

NoellesRoles 接入格式必须按职业或词条拆文件：`roles/<role>/<RoleName>RoleAssignmentRules`、`modifiers/<modifier>/<ModifierName>ModifierAssignmentRules`，再由 `NoellesRolesBootstrap.init()` 调用。不要把多个职业/词条的 Harpy 规则塞进一个公共大类。

### 玩家体力与移动接入

Wathe 已经公开了玩家体力和移动速度。NoellesRoles 侧如果要改这些数值，优先走公开 API，不要再 shadow `forwardSpeed` / `sidewaysSpeed`，也不要再把逻辑塞进 `travel()`、`jump()` 或 `getMovementSpeed()` 的大 mixin。

- 体力读写用 `PlayerStaminaApi` / `PlayerStaminaComponent`；如果要清空、回满、增减体力或调整上限，统一从这里走。
- 速度叠加用 `PlayerMovementApi.registerSpeedModifier(...)`；它支持 `ADD`、`MULTIPLY`、`OVERRIDE` 和 `PASS`，适合职业 / 词条按优先级叠加加速和减速。
- 中等和低落心情体力惩罚默认都关闭，开关在 `GameWorldComponent`，测试指令是 `/wathe:setMood` 和 `/wathe:moodStaminaPenalty`。
- 低落惩罚开启时，`mood <= DEPRESSIVE_MOOD_THRESHOLD` 会禁跑，体力归零后不能自主水平移动和跳跃，但外力位移仍保留。
- 中等惩罚开启时，`mood < MID_MOOD_THRESHOLD` 只在疾跑时扣体力；如果只开中等惩罚，低落心情也沿用中等规则。
- 如果某个职业 / 词条真的需要特殊移动分支，就按职业或词条拆到 `roles/<role>/<RoleName>MovementHandler` 或 `modifiers/<modifier>/*MovementHandler`，再由 `NoellesRolesMovementBootstrap` 聚合注册。

### 疯魔 API 接入

NoellesRoles 里的疯魔相关改动必须按职业拆分，不要把所有规则塞到一个公共大类：

- 聚合入口只放 `bootstrap/NoellesRolesPsychoBootstrap.java`，它只负责调用 `roles/<role>/<RoleName>PsychoHandler.init()`。
- 新职业需要自己的疯魔形态时，在对应 `roles/<role>/` 包里注册 `PsychoModeProfile`；持续时间、护盾层数、授予物品、锁栏物品、球棒/近战命中声音、护盾声音、背景音乐、皮肤、结束回放名称和护盾回放名称都写进 profile。
- 服务端启动疯魔用 `PsychoModeApi.start(player, profileId)`，提前结束或回合清理用 `PsychoModeApi.stop(player, recordReplay)`；不要直接写 `PlayerPsychoComponent#psychoTicks`、`armour` 或手动增减 `psychosActive`。
- 判断状态用 `PsychoModeApi.isActive(...)`、`getRemainingTicks(...)`、`getArmour(...)`；只有 `TimekeeperSnapshots` 这种组件快照白名单才应直接引用 Wathe 的 CCA key。
- 疯魔护盾的穿透/强制抵挡用 `PsychoModeApi.registerShieldRule(...)`，不要再 mixin `PlayerPsychoComponent#getArmour()`。
- 静音或自定义音乐用 profile 的 `hitSound(null)`、`backgroundSound(sound, false/true)`；客户端新增音乐时再调用 `PsychoModeClientApi.registerBackgroundAmbience(...)`。
- 疯魔皮肤采用“profile 默认皮肤 + 客户端 visual provider 按优先级覆盖”。职业需要特殊皮肤时，先在 profile 给默认值；临时状态覆盖再放客户端 provider。
- 疯魔 Mood 显示采用 Wathe `MoodHudApi.registerPsychoStyle(...)`。职业要自定义完整/破损图标、跑马文本、文本颜色或倒计时条颜色时返回自己的 `PsychoMoodHudStyle`；Wathe 会按当前护盾是否大于 0 切换 body / hitBody，0 护盾 profile 从启动开始就应显示破损态。颜色 provider 可返回 `0xRRGGBB` 或 `0xAARRGGBB`。
- 已被 Wathe API 覆盖的 mixin 不要重新加回：Jester 触发疯魔、BountyHunter 锁槽/防丢弃、Muzzler 静音、Rememberer 狙击穿盾都应走各职业 handler。

### 停电 API 接入

- 统一使用 Wathe `BlackoutApi`，不要再写 `WorldBlackoutComponentAccessor` 或客户端监听停电音效计时黑幕。
- `NoellesRolesBlackoutBootstrap` 当前注册两个分组规则：`KILLER_SIDED_NEUTRALS` 获得停电夜视，`INDEPENDENT_NEUTRALS` 获得停电失明。
- 工程师恢复供电调用 `BlackoutApi.restorePower(world)`；这个入口会恢复灯光、同步 Wathe 黑幕并清理 Wathe 自己发放的停电药水。
- 后续具体职业或词条需要特殊停电规则时，按 `roles/<role>/<RoleName>BlackoutHandler` 或 `modifiers/<modifier>/*BlackoutHandler` 拆文件，再由 bootstrap 调用 `init()`。
- 时停者快照恢复 `wathe:blackout` 世界组件即可；Wathe 组件 NBT 已包含 ticks、总时长、恢复事件标记、黑幕不透明度和药水开关，不要再额外 accessor 私有字段。

## 新职业开发流程

1. 先把用户需求拆成字段：职业名、英文 id、阵营、职业色、欢迎公告、技能、交互方式、冷却、商店、物品、HUD/UI、回放、死亡/胜利、兼容要求、是否要求先出方案。
2. 用 `rg` 搜本仓库已有相似实现；跨项目参考时只复制思路，不直接复制映射名。`noellesroles/harpy/kinssaba` 多为 Yarn 命名，`stupidexpress/starryexpress` 有 Mojang 官方命名痕迹。
3. 判断是否需要改 Wathe 或 Harpy。只要能在 NoellesRoles 侧通过 API 或窄 mixin 解决，就优先不动 Wathe/Harpy。玩家体力和移动速度修正优先走 `PlayerStaminaApi` / `PlayerMovementApi`，不要再自己 shadow `forwardSpeed` / `sidewaysSpeed`。
4. 如果用户要求“先分析方案”，先给方案，不改文件。否则按需求直接实现。
5. 所有玩法数值除职业 RGB 以外，放到该职业 `*Constants` 类里；冷却统一用 `GameConstants.getInTicks(min, sec)` 或明确 tick 常量。这里也包括体力、速度和心情惩罚值。
6. 关键代码写详细中文注释，尤其是：为什么要这么接入 API、为什么要在服务端/客户端判断、为什么要同步组件、为什么要这样处理回合结束/玩家死亡/掉线。
7. 每个新增职业优先拆成独立包：`roles/<role_id>/` 放服务端逻辑、组件、常量、商店、能力处理；客户端对应放到 `client/roles/<role_id>/`、`client/ui/roles/<role_id>/`、`client/instinct/roles/<role_id>/` 等。普通屏幕 HUD 放到 `client/roles/<role_id>/<RoleName>StatusHud.java`；词条固定 HUD 放到 `client/hud/modifiers/<modifier>/<ModifierName>Hud.java`；背包按钮放到 `client/ui/roles/<role_id>/<RoleName>InventoryButtons.java`，不要新增 HUD / screen mixin。
8. 只要新增或改动 CCA 组件、世界组件、实体运行态、全局 Map/管理器状态，就必须评估时停者回溯：应回滚的玩家组件加入 `TimekeeperSnapshots.PLAYER_COMPONENTS`，应回滚的世界组件加入 `TimekeeperSnapshots.WORLD_COMPONENTS`；不应回滚的配置/缓存/播放机械要在代码或方案里写明排除原因。
9. 只要新增或改动 `VictoryApi` 胜利规则，尤其是独立阵营胜利、共胜、或活着时返回 `KEEP_RUNNING` 阻拦普通杀手/乘客结算的职业/词条，就必须评估时间狭缝：把“排除狭缝玩家后仍真正存活且仍应阻拦结算”的条件补进 `TimekeeperRiftHandler`，避免死者处于特殊存活旁观时继续卡住胜利。
10. 只要新增或改动 Harpy 开局生成限制、同局互斥、绑定生成、词条与职业绑定/排斥，按 `*RoleAssignmentRules` / `*ModifierAssignmentRules` 拆小类并接 Harpy assignment API，不要新增分配 mixin。
11. 新增功能完成后按“注册点检查清单”逐项核对，再编译。

## 注册点检查清单

新增职业通常需要检查这些位置：

- `Noellesroles.java`：只作为 Fabric 主入口，通常不改；除非新增的是全局启动编排入口，否则不要把注册逻辑写回这里。
- `NoellesRoleIds.java`：新增职业 / 词条的稳定 `Identifier`。
- `NoellesRoleRegistry.java`：新增 `Role` 实例，并用 `WatheRoles.registerCivilianRole/registerVigilanteRole/registerKillerRole/registerNeutralRole` 显式登记阵营。
- `NoellesModifierRegistry.java`：新增或调整 Harpy 词条。
- `NoellesRoleGroups.java`：新增跨系统共享角色集合，例如杀手侧中立、本能/HUD 分组等。
- `NoellesDeathReasons.java`：新增死亡原因 id。
- `NoellesEventIds.java`：新增回放事件、托盘/床效果、护盾来源等稳定事件 id。
- `NoellesRolesBootstrap.java`：新增全局初始化器时，在这里按顺序调用。
- `NoellesRolesPayloadTypes.java`：新增自定义 payload codec。
- `NoellesRolesPacketReceivers.java`：新增服务端 packet receiver 或能力分发分支。
- `NoellesRolesEventBootstrap.java`：新增事件监听、server tick、回合结束清理、人数动态角色上限、Harpy 禁用列表同步。
- `NoellesRolesEconomyBootstrap.java`：新增金币 HUD、任务收入、被动收入或经济词条规则。
- `NoellesRolesReplayBootstrap.java`：新增回放 formatter 注册。
- `NoellesRolesPsychoBootstrap.java`：新增疯魔 profile、护盾规则、声音/皮肤规则时，在这里调用对应职业 `*PsychoHandler.init()`；具体逻辑仍留在 `roles/<role>/`。
- `NoellesPlayerCollisionHandlers.java`：新增玩家碰撞规则时，在这里调用对应职业 / 词条的 `*PlayerCollisionHandler.init()`；具体逻辑放在 `roles/<role>/` 或 `modifiers/<modifier>/`。
- `NoellesRolesCombatBootstrap.java`：新增枪械开火接管、左轮目标覆写、左轮误伤惩罚或冷却修正规则时，在这里调用对应职业/词条 `*GunHandler` 或 `*GunCooldownHandler.init()`。
- `NoellesRolesDeathBootstrap.java`：新增死亡保护、反噬、击杀奖励、确认死亡后清理或尸体生成回调时，在这里按阶段接入对应 handler。
- `NoellesRolesMovementBootstrap.java`：新增体力、速度加成 / 减速 / 覆盖规则时，在这里按职业 / 词条接入对应 handler。
- `NoellesRoleLimitsBootstrap.java`：新增 Harpy 静态最大生成数。
- `roles/<role>/<RoleName>RoleAssignmentRules.java`：新增 Harpy 职业分配互斥、单向排斥、绑定生成或阶段补位规则。
- `modifiers/<modifier>/<ModifierName>ModifierAssignmentRules.java`：新增 Harpy 词条分配规则、词条与职业绑定/排斥、词条公告前强制配对或动态上限。
- `NoellesRolesComponents.java`：需要持久/同步状态时注册 CCA 组件。
- `fabric.mod.json`：新增 CCA 组件 id。
- `TimekeeperSnapshots.java`：新增 CCA 运行态组件后同步加入 `PLAYER_COMPONENTS` / `WORLD_COMPONENTS`，或明确说明该组件不应被时间回溯。
- `TimekeeperRiftHandler.java`：新增或改动独立胜利、共胜、`KEEP_RUNNING` 阻拦普通结算的职业/词条后，检查时间狭缝提前收束逻辑是否需要加入该规则。
- `TimekeeperWorldStateSnapshot.java`：新增门、火、放置物、机关等可控世界状态时，评估是否需要纳入时停者回溯；不要做整张地图方块级回滚。
- `NoellesRolesRoleAssignedBootstrap.java`：职业分配后发初始物品、重置状态、设置开局冷却。
- `NoellesRolesShopBootstrap.java`：注册静态/动态职业商店，或 ShopModifier。
- `NoellesRolesShops.java`：购买特殊图标、即时能力物品、随机物品时的交付逻辑。
- `ModItems.java`：新增物品、数据组件、默认冷却。
- `NoellesrolesClient.java`：客户端按键、tooltip/model predicate、实体渲染、客户端网络包。
- `NoellesClientMovementBootstrap.java`：新增客户端移动提示、表现或本地状态时，在这里按职业 / 词条聚合调用。
- `NoellesHudHandlers.java`：新增普通屏幕 HUD provider 后，在这里调用该职业/词条 HUD 类的 `register()`。
- `NoellesInventoryButtons.java`：新增背包按钮 provider 后，在这里调用该职业 `*InventoryButtons.register()`。
- `NoellesInstinctHandlers.java` / `NoellesAppearanceHandlers.java` / `NoellesHeldItemVisibilityHandlers.java`：本能、外观、手持隐藏注册。
- `noellesroles.mixins.json` / `noellesroles.client.mixins.json`：服务端和客户端 mixin 分开注册，环境要正确。
- `zh_cn.json` / `en_us.json`：职业名、欢迎公告、物品名、tooltip、HUD、actionbar、回放、死亡原因。
- `assets/noellesroles/models/item/*.json`、`textures/item/*.png`、`textures/gui/sprites/hud/*`：物品模型和 HUD 图标。
- `NoellesRolesReplayFormatters.java`：格式化器使用 `Text.translatable`，回放数据里存稳定 id/uuid/必要显示名，避免玩家掉线后显示未知。

## 角色注册规则

推荐格式：

```java
// NoellesRoleIds.java
public static final Identifier SOME_ROLE_ID = NoellesRolesCore.id("some_role");

// NoellesRoleRegistry.java
public static final Role SOME_ROLE = WatheRoles.registerCivilianRole(new Role(
        NoellesRoleIds.SOME_ROLE_ID,
        SomeRoleConstants.ROLE_COLOR,
        true,
        false,
        Role.MoodType.REAL,
        WatheRoles.CIVILIAN.getMaxSprintTime(),
        false
));
```

不要再把 `Identifier`、`Role` 或兼容导出字段加回 `Noellesroles.java`。其他扩展需要反射 NoellesRoles 职业时，应读取 `org.agmas.noellesroles.registry.NoellesRoleRegistry`；需要读取 `KILLER_SIDED_NEUTRALS` 时，应读取 `org.agmas.noellesroles.registry.NoellesRoleGroups`。

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

时停者回溯会按 `TimekeeperSnapshots` 的快照白名单恢复组件 NBT。新增组件时按下面规则处理：

- 属于局内运行态的玩家组件，例如冷却、目标、标记、伪装、任务/进度、免死层数、临时控制、语音/聊天状态，加入 `TimekeeperSnapshots.PLAYER_COMPONENTS`。
- 属于局内运行态的世界组件，例如全局倒计时、全局标记、全局实体索引、阵营共享进度，加入 `TimekeeperSnapshots.WORLD_COMPONENTS`。
- 组件的 `writeToNbt` / `readFromNbt` 必须覆盖所有应回滚字段；恢复后需要客户端立刻刷新的组件必须能正确 `sync`。
- 配置、常量缓存、纯客户端显示缓存、以及 `TimekeeperWorldComponent` 这类“正在执行回溯”的播放机械通常不应进入快照；排除时要写清楚原因。
- 如果状态存在于 CCA 之外，例如静态 Map、延迟任务队列、播放实体、自定义非物品实体、语音连接或方块实体，不能只加组件白名单；要补对应的快照恢复、重建或回合清理。
- 新组件开发完成后至少测试一次：组件状态改变后发动时停者回溯，确认该状态回到 30 秒前；购买回溯保护的玩家则不应被回滚。

## 胜利规则与时间狭缝

时间狭缝会把刚死亡的玩家临时拉成 Wathe 的“特殊存活旁观”，所以他们在狭缝倒计时结束前仍可能被 `GameFunctions.isPlayerAliveAndSurvival(...)` 和 `VictoryApi` 当作存活玩家。这个机制是为了允许 30 秒内的时间回溯复活死者，但它也会影响阵营结算。

新增或修改以下规则时，必须同步检查 `TimekeeperRiftHandler`：

- 独立职业 / 独立词条的自定义胜利。
- 普通阵营胜利时追加赢家的共胜规则。
- 活着时返回 `VictoryApi.VictoryResult.keepRunning()`，用于阻拦普通杀手 / 乘客结算的规则。

处理原则：如果排除当前处于时间狭缝的玩家后，游戏已经只剩一个可获胜阵营，狭缝玩家应立即 `finishTimeRift()` 转回普通死亡旁观和死亡语音频道，让 Wathe 正常结算；如果排除狭缝后仍有真正存活的独立阻拦者，则继续保留狭缝。后续新增这类职业时，要把“非狭缝存活阻拦条件”补进 `TimekeeperRiftHandler`，并测试“阻拦者正常存活”和“阻拦者死亡进入狭缝”两种局面。

## 枪击与死亡 API

枪击接管、左轮反火、客户端目标覆写、左轮冷却修正优先接 Wathe `GunShotApi`，不要再新增 `GunShootPayload`、`RevolverItem` 或 `DerringerItem` mixin。NoellesRoles 侧按职业/词条拆 handler，例如 `roles/robber/RobberGunHandler`、`roles/assassin/AssassinGunHandler`、`roles/coward/CowardGunCooldownHandler`、`roles/jester/JesterGunTargetHandler`、`roles/executioner/ExecutionerGunPenaltyHandler`，然后只在 `NoellesRolesCombatBootstrap` 里调用 `init()`。

客户端武器在准备发送命中包时统一使用 Wathe `dev.doctor4t.wathe.api.combat.WeaponTargetingApi`，不要在 NoellesRoles 里重新组合 `ProjectileUtil`、`GameFunctions.isPlayerAliveAndSurvival(...)` 和 `TargetVisibilityApi`：

- 准心、HUD、玩家名和锁定提示使用 `WeaponTargetingApi.getVisibleAlivePlayerTarget(player, range)`。
- 普通近战或物品真实命中使用 `WeaponTargetingApi.getAttackableAlivePlayerTarget(player, range)`。
- 枪械准心使用 `WeaponTargetingApi.resolveVisibleGunTarget(player, stack, range)`。
- 枪械真实发包使用 `WeaponTargetingApi.resolveAttackableGunTarget(player, stack, range)`。

这四个入口已经分别处理 `TARGET` 和 `ATTACK` 语义，并且枪械入口会继续经过 Wathe `GunShotApi` 的目标覆写链。尸体伪装可以隐藏准心变化，但不会因为隐藏 `TARGET` 而获得攻击免疫。客户端目标只负责准备实体 id，服务端 packet / `GunShotContext` 仍必须重新校验职业、存活、冷却、距离和 `TargetVisibilityApi.canAttackPlayer(...)`。`NoellesItemTargeting` 这类扩展侧重复 helper 不应重新创建。

击杀奖励、重复死亡保护、致死确认前转化、确认死亡后清理、心情重置前处理、尸体生成回调优先接 Wathe `DeathApi`，不要再新增 `GameFunctions.killPlayer(...)` mixin。NoellesRoles 侧按职业或词条拆 handler，例如 `roles/timekeeper/TimekeeperDeathHandler`、`modifiers/dual_personality/DualPersonalityDeathHandler`、`roles/bounty_hunter/BountyHunterDeathHandler`、`roles/coroner/CoronerBodySpawnHandler`，然后只在 `NoellesRolesDeathBootstrap` 里注册。

死亡优先级以 Wathe `DeathApi` 常量为准：重复死亡吞噬最高，其次是特殊存活保护、死亡流程状态、回放上下文、致死确认前拦截、普通逻辑、确认死亡后的奖励/二段机制、最终清理。priority 越大越先执行；同 priority 后注册的规则先执行。新增 handler 时必须在中文注释里说明它选用该阶段和 priority 的原因，避免反火、免死、赏金、时间狭缝和双重人格转化互相抢顺序。

## 商店和经济

优先走 Wathe `ShopApi`：

- 完全替换职业商店：`ShopApi.registerRoleShop(role, provider)`。
- 只改默认杀手商店少数条目：`ShopApi.registerShopModifier(id, priority, handler)`。
- 购买时不要重复扣钱、播放音效、写购买回放；provider 的 `purchase` 只负责“是否真的交付成功”，公共结算由 Wathe `PlayerShopComponent` 处理。
- 需要金币 HUD 的非杀手职业，注册 `EconomyApi.registerBalanceHudRole(role)`。
- 需要普通被动收入，注册 `EconomyApi.registerPassiveIncomeRole(role)`。
- 任务金币走 `TaskCompletionApi.registerTaskIncomeProvider`；需要“任务完成后的特殊效果”走 `TaskCompletionApi.AFTER_TASK_COMPLETE`。
- 需要“任务完成但跳过 Wathe 默认收入”的窄场景，例如服务员帮别人完成任务，用 `TaskCompletionApi.registerTaskIncomeRule(...)`，不要再 mixin `TaskCompletionApi`。
- 需要阻止任务完成确认的职业状态，例如灵术师附身，用 `MoodTaskApi.registerCompletionRule(...)`，不要再 mixin `PlayerMoodComponent#completeTask(...)`。
- 需要阻止 Wathe 自动刷任务、低心情补槽、外部随机发放或指定发放时，用 `MoodTaskApi.registerAssignmentRule(...)` 并按 `AssignmentSource` 判断来源；不要在 server tick 里等任务出现后再 `removeTask`，否则客户端 HUD 会看到任务闪现。
- 新增 Noelles 自定义心情任务时，按职业或词条拆到 `roles/<role>/<RoleName>MoodTaskHandler` 或 `modifiers/<modifier>/*MoodTaskHandler`，再由 `NoellesRolesMoodTaskBootstrap` 聚合调用；不要塞进总入口或经济 bootstrap。

## 客户端与 mixin

- 普通屏幕 HUD 放 `src/client/java/org/agmas/noellesroles/client/roles/<role>/*StatusHud.java`，通过 `NoellesHudHandlers` 注册到 Wathe `HudOverlayApi`，不要注册到 `noellesroles.client.mixins.json`。
- 准心图标、武器锁定和准心下方小进度条放 `src/client/java/org/agmas/noellesroles/client/roles/<role>/*Crosshair.java`，通过 `NoellesCrosshairHandlers` 注册到 Wathe `CrosshairHudApi`，目标射线使用 `WeaponTargetingApi` 的 visible 入口，不要 mixin `CrosshairRenderer`。
- 准心名字、尸体提示、准心附近额外文字通过 Wathe `RoleNameHudApi` 注册。
- 背包玩家选择界面不再写 `LimitedInventoryScreen` / `LimitedHandledScreen` mixin，优先接 Wathe `InventoryButtonApi`。
- 枪击、左轮反火、击杀奖励和尸体生成回调不再写通用流程 mixin，优先接 Wathe `GunShotApi` / `WeaponTargetingApi` / `DeathApi`。
- 相机、输入控制、手臂动作、物品渲染等尚无公开 API 的客户端钩子才放 `src/client/java` 并注册到 `noellesroles.client.mixins.json`。
- 服务端逻辑、死亡链、物品行为、任务处理放 `src/main/java`，并注册到 `noellesroles.mixins.json`。
- mixin 条件必须尽量窄：判断玩家存活、当前职业、手持物品、世界是否 client/server、是否对局中。

## HUD 接入

Wathe API 定义在 `D:\哈比快车最新源码\wathe\Wathe - 副本1\src\main\java\dev\doctor4t\wathe\api\client\hud`、`D:\哈比快车最新源码\wathe\Wathe - 副本1\src\main\java\dev\doctor4t\wathe\api\client\gui\RoleNameHudApi.java` 和 `D:\哈比快车最新源码\wathe\Wathe - 副本1\src\main\java\dev\doctor4t\wathe\api\client\gui\CrosshairHudApi.java`。NoellesRoles 只是调用这些 API，当前工程自己的接入代码在 `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\src\client\java\org\agmas\noellesroles\client\hud`、`client/crosshair` 和各职业 `client/roles/<role>` 包里。

- 新职业普通右下角状态新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>StatusHud.java`。
- 新词条固定屏幕 HUD 新建 `src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/<ModifierName>Hud.java`。
- 在 `NoellesHudHandlers.register()` 里调用该类的 `register()`，不要把职业/词条状态判定塞回总类。
- 新职业准心图标新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>Crosshair.java`，在 `NoellesCrosshairHandlers.register()` 里调用该类的 `register()`。
- 职业独占 HUD 优先用 `NoellesHudSupport.registerAliveRole("roles/<role>/status", NoellesRoleRegistry.MY_ROLE, renderer)`。这个 helper 会同时检查职业和 Wathe 的 `GameFunctions.isPlayerAliveAndSurvival(...)`，死亡、旁观、创造和非局内状态不会继续显示 HUD。
- 不是职业独占、但仍只应给活人看的 HUD，例如被附体者、被绑架者、开局安全、停电提示、狙击镜遮罩，用 `HudOverlayApi.register(...)`，并在 provider 内显式判断 `context.aliveAndSurvival()`。
- 常规状态文字用 `HudOverlayLayer.MAIN_HUD`；需要尽早盖画面的提示用 `BEFORE_HUD`；狙击镜这类最终遮罩用 `AFTER_HUD`，需要保留快捷栏时调用 `context.renderHotbar()`。
- 准心目标旁边的信息、尸体死因/身份提示和魔术师播放体名字使用 `RoleNameHudApi`；非玩家实体名牌用 `registerEntityName(...)`，不要写 `RoleNameRenderer` mixin。
- 替换 3x3 准心、武器命中高亮或准心下方 10x7 ready/progress 图标使用 `CrosshairHudApi.registerProvider(...)`；只追加默认准心后的短进度条使用 `registerOverlay(...)`。准心只是客户端提示，服务端仍要重新校验职业、存活、冷却、距离和目标合法性。
- 隐藏玩家或玩家尸体、让其不可被准心选中 / 不可被职业道具交互时，优先在对应职业包注册 `TargetVisibilityApi` 规则，并在 `visibility/NoellesTargetVisibilityHandlers` 中调用。不要再新增 `PlayerBodyEntityRenderer`、`LivingEntity#canHit`、`RoleNameRenderer` 或 `CrosshairRenderer` 的隐藏类 mixin。
- 改变玩家之间的物理碰撞时，优先在对应职业或词条包注册 Wathe `PlayerCollisionApi` 规则，并在 `collision/NoellesPlayerCollisionHandlers` 中调用。`SOLID` 是 Wathe/spark 式实体墙，只在已经重叠时保留原版轻微推挤用于解卡；`VANILLA_PUSH` 是 FEATHER 这类原版推挤可穿过，`NO_COLLISION` 是灵术师脱体本体这类完全无碰撞无推挤。
- 已被 API 替代的 `*HudMixin`、`*ScreenMixin` 和旧 RoleNameRenderer mixin 不要重新加回 `noellesroles.client.mixins.json`。

## 背包按钮接入

Wathe API 定义在 `D:\哈比快车最新源码\wathe\Wathe - 副本1\src\main\java\dev\doctor4t\wathe\api\client\inventory`。NoellesRoles 只是调用这些 API，当前工程自己的接入代码在 `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\src\client\java\org\agmas\noellesroles\client\inventory` 和各职业 `client/ui/roles/<role>` 包里，路径不要混写。

- 新职业背包按钮新建 `src/client/java/org/agmas/noellesroles/client/ui/roles/<role>/<RoleName>InventoryButtons.java`。
- 新词条背包按钮新建 `src/client/java/org/agmas/noellesroles/client/ui/modifiers/<modifier>/<ModifierName>InventoryButtons.java`。
- 在 `NoellesInventoryButtons.register()` 里调用该类的 `register()`，不要把职业按钮逻辑塞回总类。
- 职业按钮通常用 `NoellesInventoryButtonSupport.registerLimited("role_id", Factory::create)`，只挂到 Wathe `InventoryScreenType.LIMITED`。
- 玩家头像分页用 `NoellesInventoryButtonSupport.PagedExtension` 或 `PagedButtons`，不要复制一套分页坐标。
- 动态增删列表用同一个 group 重建，参考 `client/ui/roles/convener/ConvenerInventoryButtons.java`。
- 变形怪这类点击后要隐藏头像和翻页按钮的职业，覆写 `selectionVisible(...)`，让翻页按钮和头像共用同一条件。
- 文本输入阶段要禁止 E 键关闭背包时，实现 `allowInventoryKeyClose(...)` 返回 `false`；关闭时在 `close(...)` 清理静态输入状态。
- 已被 API 替代的 `*ScreenMixin` / `*DoNotClose` 不要重新加回 `noellesroles.client.mixins.json`。

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
HUD/UI/准星/本能/心情图标：普通屏幕 HUD 优先走 `HudOverlayApi`，准心图标优先走 `CrosshairHudApi`，准心名字/目标文字优先走 `RoleNameHudApi`
时停者回溯快照：新增 CCA / 世界 / 实体运行态是否需要加入 `TimekeeperSnapshots`
时间狭缝胜利收束：独立胜利 / 共胜 / KEEP_RUNNING 阻拦规则是否需要加入 `TimekeeperRiftHandler`
回放/死亡/胜利：
参考职业：
是否先给方案：
编译要求：
```

如果是修 bug，写“请按 AGENTS.md 先定位相关源码，再修复：现象、复现步骤、期望结果、是否只改 NoellesRoles、编译要求”即可。
