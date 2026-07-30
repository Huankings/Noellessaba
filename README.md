# NoellesRoles 中文说明与开发文档

这是一个基于 Minecraft 1.21.1 Fabric 的 Wathe 扩展职业模组。
它不是独立玩法，而是依赖 Wathe + HarpyModLoader 的职业扩展层，给类狼人杀对局补充更多职业、词条、商店改写、回放文本和客户端表现。

本文档分两部分：
1. 给玩家和服主看的职业总览。
2. 给其他开发者看的注册流程和源码接入方式。

## 依赖与启动

- Minecraft 1.21.1
- Fabric Loader / Fabric API
- Wathe
- HarpyModLoader
- Cardinal Components API
- 语音桥接为可选项，当前源码里已经接了 `voicechat` 入口。

仓库里 `libs` 目录已经放了 Wathe 和 HarpyModLoader 的本地依赖；如果你本地的 Wathe 构建还要求额外训练/语音相关 jar，也要一起放进 `libs`。

## 源码地图

- Fabric 主入口：`src/main/java/org/agmas/noellesroles/Noellesroles.java`，现在只调用 `NoellesRolesBootstrap.init()`，不再承载职业、事件、packet、经济、回放等注册细节。
- 总启动编排：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesBootstrap.java`
- 模组基础标识：`src/main/java/org/agmas/noellesroles/registry/NoellesRolesCore.java`
- 职业 / 词条 id：`src/main/java/org/agmas/noellesroles/registry/NoellesRoleIds.java`
- 职业注册：`src/main/java/org/agmas/noellesroles/registry/NoellesRoleRegistry.java`
- 词条注册：`src/main/java/org/agmas/noellesroles/registry/NoellesModifierRegistry.java`
- 跨系统职业分组：`src/main/java/org/agmas/noellesroles/registry/NoellesRoleGroups.java`
- 死亡原因 id：`src/main/java/org/agmas/noellesroles/registry/NoellesDeathReasons.java`
- 回放 / 托盘 / 床 / 护盾来源事件 id：`src/main/java/org/agmas/noellesroles/registry/NoellesEventIds.java`
- 伪装商店条目：`src/main/java/org/agmas/noellesroles/registry/NoellesFramingShopEntries.java`
- 组件注册：`src/main/java/org/agmas/noellesroles/NoellesRolesComponents.java`
- payload codec 注册：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesPayloadTypes.java`
- 服务端 packet 接收：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesPacketReceivers.java`
- 事件 / tick / 回合清理引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesEventBootstrap.java`
- 经济 / 任务收入引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesEconomyBootstrap.java`
- 回放 formatter 注册引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesReplayBootstrap.java`
- Harpy 角色上限引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRoleLimitsBootstrap.java`
- 职业分配总引导：`src/main/java/org/agmas/noellesroles/roleassign/NoellesRolesRoleAssignedBootstrap.java`
- 枪械 / 左轮反火总引导：`src/main/java/org/agmas/noellesroles/combat/NoellesRolesCombatBootstrap.java`
- 死亡保护 / 击杀流程总引导：`src/main/java/org/agmas/noellesroles/death/NoellesRolesDeathBootstrap.java`
- 商店总引导：`src/main/java/org/agmas/noellesroles/shop/NoellesRolesShopBootstrap.java`
- 物品：`src/main/java/org/agmas/noellesroles/item/`
- 职业逻辑：`src/main/java/org/agmas/noellesroles/roles/<role>/`
- 客户端：`src/client/java/org/agmas/noellesroles/client/`
- 通用 HUD 注册入口：`src/client/java/org/agmas/noellesroles/client/hud/NoellesHudHandlers.java`
- 通用 HUD 辅助：`src/client/java/org/agmas/noellesroles/client/hud/NoellesHudSupport.java`
- 准心图标注册入口：`src/client/java/org/agmas/noellesroles/client/crosshair/NoellesCrosshairHandlers.java`
- 职业状态 HUD：`src/client/java/org/agmas/noellesroles/client/roles/<role>/*StatusHud.java`
- 职业准心图标：`src/client/java/org/agmas/noellesroles/client/roles/<role>/*Crosshair.java`
- 词条固定 HUD：`src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/*Hud.java`
- 背包按钮注册入口：`src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtons.java`
- 背包按钮共享工具：`src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtonSupport.java`
- 职业背包按钮：`src/client/java/org/agmas/noellesroles/client/ui/roles/<role>/*InventoryButtons.java`
- 词条背包按钮：`src/client/java/org/agmas/noellesroles/client/ui/modifiers/<modifier>/*InventoryButtons.java`
- mixin：`src/main/java/org/agmas/noellesroles/mixin/` 和 `src/client/java/org/agmas/noellesroles/client/mixin/`

## 阵营规则

当前源码里角色注册仍然沿用 Wathe 的 `Role` 结构，但阵营已经可以显式声明。

`Role` 的核心构造参数是：
- `Identifier`
- `color`
- `isInnocent`
- `canUseKiller`
- `MoodType`
- `maxSprintTime`
- `canSeeTime`

推荐新职业直接使用：
- `WatheRoles.registerCivilianRole(...)`
- `WatheRoles.registerKillerRole(...)`
- `WatheRoles.registerNeutralRole(...)`
- `WatheRoles.registerVigilanteRole(...)`

不要再只靠 `isInnocent/canUseKiller` 去猜阵营。当前 HarpyModLoader 会优先按 `role.getFaction()` 分池和结算。

注意：
- `Faction.NEUTRAL` 是真正的中立阵营。
- `KILLER_SIDED_NEUTRALS` 只是“杀手侧中立”显示和本能识别集合，不等于阵营本身；它现在位于 `NoellesRoleGroups`，不再通过 `Noellesroles.X` 导出。

## 职业总览

### 杀手阵营

- 造尸怪（`corpsemaker`）
- 潜行者（`stalker`）
- 附体师（`controller`）
- 炸弹客（`bomber`）
- 强盗（`robber`）
- 刺客（`assassin`）
- 变形怪（`morphling`）
- 魔术师（`magician`）
- 交换者（`swapper`）
- 幻灵（`phantom`）
- 洗脑师（`brainwasher`）
- 亡语杀手（`the_insane_damned_paranoid_killer`）

### 杀手侧中立

- 狂信者（`jester`）
- 仇杀客（`executioner`）
- 秃鹫（`vulture`）

### 平民阵营

- 典狱长（`conductor`，源码注释里也叫列车长）
- 记者（`awesome_binglus`）
- 工程师（`engineer`）
- 酒保（`bartender`）
- 风灵师（`winder`）
- 灵术师（`spiritualist`）
- 接线员（`operator`）
- 大嗓门（`noisemaker`）
- 巫毒师（`voodoo`）
- 调查官（`trapper`）
- 演尸官（`coroner`）
- 回溯者（`recaller`）
- 先知（`prophet`）
- 圣母（`goddess`）
- 天使（`angel`）
- 胆小鬼（`coward`）
- 追忆者（`rememberer`）
- 时停者（`timekeeper`）
- 服务员（`waiter`）
- 模仿者（`mimic`）

### 义警阵营

- 更好的义警（`better_vigilante`）

### 附加词条

- 小孩子（`tiny`）
- 变色龙（`chameleon`）
- 猜测者（`guesser`）
- 盗墓者（`graverobber`）
- 羽化者（`feather`）

## 职业机制与源码实现

### 杀手阵营

- 造尸怪：能伪造尸体、假身份和假死因；源码入口是 `CorpsemakerC2SPacket`、`CorpsemakerAbility` 和 `CorpsemakerRoleAssignedHandler`，尸体信息通过 `BodyDeathReasonComponent` 和 `NoellesEventIds.CORPSEMAKER_FORGED_BODY_EVENT` 记录。
- 潜行者：三阶段成长，靠凝视攒能量，二阶段拿刀，三阶段进入处刑突进；核心状态都在 `StalkerPlayerComponent`，技能包在 `StalkerGazeC2SPacket` / `StalkerDashC2SPacket`，免死在 `StalkerDeathProtectionHandler`，商店动态切换在 `StalkerShopHandler`。
- 附体师：通过 `ControllerPossessC2SPacket` 选人，`ControllerPossessAbility` 交换位置并给目标隐身和缓落，`ControllerPlayerComponent` 负责附体计时、伪装目标和一次性护甲，`ControllerDeathProtectionHandler` 负责挡一次死，`ControllerDeathHandler` 在 DeathApi 流程里解除附体并结算目标连锁，客户端 mixin 负责输入与显示。
- 炸弹客：`TimedBombItem` 会把活动炸弹放到目标身上，`BomberPlayerComponent` 维护静默期、滴滴期、传递和爆炸，`BomberDeathHandler` 在真实死亡后补结算和清理，`NoellesRolesTrayEffects` 与 `NoellesRolesBedEffects` 支持把炸弹埋进托盘和床。
- 强盗：`RobberGunHandler` 通过 Wathe `GunShotApi` 完整接管强盗手枪开火，命中后按目标阵营决定保留、掉左轮或消失；`RobberShopHandler` 改写默认杀手商店，`RobberRoleAssignedHandler` 开局发强盗手枪和撬棍。
- 刺客：`BayonetItem` + `AssassinPlayerComponent` + `AssassinBayonetAttackMixin` / `AssassinBodySpawnHandler` 实现无声刺杀、击退和隐藏尸体；`AssassinGunHandler` 通过 `GunShotApi` 接管无声左轮，`AssassinShopHandler` 负责刺刀、冷却刷新和商店改写。
- 变形怪：`MorphlingMorphAbility` 和 `MorphlingPlayerComponent` 负责变形成任意存活玩家、变形时长和冷却，选人包复用统一的 `MorphC2SPacket`。
- 魔术师：先录一段玩家操作，再用播放实体重放；`MagicianPlayerComponent` 存录像，`MagicianPlaybackManager`、`MagicianPlaybackEntity`、`MagicianServerHooks` 执行回放，`MagicianGunHandler` 通过 `GunShotApi` 记录枪击，`MagicianPlaybackDeathHandler` 通过 `DeathApi` 改写播放体击杀归属，其余动作记录 mixin 负责刀、手雷和交互时间线。
- 交换者：`SwapperC2SPacket` 加 `SwapperAbility` 先选两人，再按随机延迟交换位置，执行结果也会写回放。
- 幻灵：`PhantomAbility` 提供短时隐身，`PhantomPlayerComponent` 管倒计时，`PhantomConstants` 控制 35 秒隐身和 90 秒冷却。
- 洗脑师：`BrainwasherAbility` 能把目标平民洗成随机杀手角色，成功后清商店并广播；`BrainwasherRoleAssignedHandler` 只负责开局冷却初始化。
- 亡语杀手：默认被 `shitpostRoles` 关闭时自动禁用；源码上主要接语音聊天和疯狂观察视觉表现，核心入口是 `NoellesrolesVoiceChatPlugin`、`InsaneObserverAppearanceHandler` 和 `NoellesRolesConfig.insanePlayersSeeMorphs`。

### 杀手侧中立

- 狂信者：开局拿假匕首、假左轮和撬棍，`JesterRoleAssignedHandler` 只做发物品；`JesterDeathProtectionHandler` 保留 psycho 无敌窗口，`JesterGunTargetHandler`、`JesterJestMixin` 和 `JesterItemEntityMixin` 负责假武器和狂化表现。
- 仇杀客：`ExecutionerPlayerComponent` 负责目标，server tick 会在目标失效后重选；`ExecutionerDeathHandler` 通过 `DeathApi` 处理目标达成后的转职，`ExecutionerBackfireDeathHandler` 和 `ExecutionerGunPenaltyHandler` 处理误杀反噬和射击锁定。
- 秃鹫：`VultureRoleAssignedHandler` 会按当前人数算需要吞多少尸体，`VultureAbility` 吞尸后累计进度，达标后随机转成一个未禁用的杀手角色并发 200 金币，尸体状态记录在 `VulturePlayerComponent`。

### 平民阵营

- 典狱长：`ConductorRoleAssignedHandler` 开局发万能钥匙、开锁器和假左轮，`MasterKeyTrainDoorMixin`、`MasterKeySmallDoorMixin` 让钥匙能开火车上各种门，`ShouldDropOnDeath` 保证死亡掉落。
- 记者：`AwesomeBinglusRoleAssignedHandler` 直接发 12 张纸条和撬棍，这是一个默认关闭的搞笑职业。
- 工程师：`ToolboxItem` 负责修门，`CaptureDeviceItem` 负责定点拘束并生成报告，`PowerRestorationItem` 负责消除停电；`EngineerShopHandler` 把这三件东西接进商店。
- 酒保：`BartenderPlayerComponent` 追踪防御瓶充能和护甲，`BartenderDeathProtectionHandler` 把护甲当一次免死，`DefenseVialApplyMixin`、`PoisonToHealsMixin` 和 `CocktailItemMixin` 改酒和毒的处理。
- 风灵师：`WinderPlayerComponent` 记录已选目标和漂浮状态，`WinderAbility` 开关漂浮，`WinderTargetAbility` 负责选人，`WindMarkPlayerComponent` 负责风印记，`WinderShopHandler` 卖风弹和风印。
- 灵术师：`SpiritualistAbility` 一枚 G 键分出“出窍 / 附身 / 结束”几种行为，`SpiritualistPlayerComponent` 是主状态中心，`SpiritualistHostComponent` 保存被附身者状态，`SpiritualistManager` 负责控制输入、视角、回写背包、语音转发和结束冷却，`SpiritualistDeathProtectionHandler` 负责免死。
- 接线员：`OperatorAbility`、`OperatorPlayerComponent` 和 `OperatorCommunicationManager` 负责把两个人接通或把某个人广播出去，持续时间、成功冷却和失败冷却都在 `OperatorConstants`。
- 大嗓门：`NoisemakerGlowC2SPacket` 点亮目标，`NoisemakerPlayerComponent` 管主动技能冷却，`NoisemakerGlowTargetComponent` 负责追踪发光结束并补回放事件，`NoisemakerBodySpawnHandler` 让自己死亡后的尸体也会发光。
- 巫毒师：`VoodooTargetAbility` 负责选人，`VoodooDeathHandler` 在 `DeathApi` 死亡流程里把目标一起杀掉；默认配置下只有“有击杀者的死亡”会触发，是否允许自然死亡触发由 `voodooNonKillerDeaths` 控制。
- 调查官：`RoleMineItem` 放角色检测器，`TrapperShopHandler` 只卖这个核心道具，客户端 HUD 和目标提示优先走 Wathe `HudOverlayApi` / `RoleNameHudApi`。
- 演尸官：`CoronerPlayerComponent` 保存伪装、尸体检查记录和金币，`CoronerMorphAbility` 负责变形目标，`CoronerExamineRewardMixin` 在靠近尸体时自动结算奖励，尸体死因和身份提示走 `RoleNameHudApi.registerExtraHud(...)`。
- 回溯者：`RecallerAbility` 先存点位，再花钱传送，`RecallerPlayerComponent` 和 `RecallerShopHandler` 负责位置、冷却和商店，末影珍珠投掷也会被记录进回放。
- 先知：`CrystalBallItem` 负责 0.1 秒标记目标，`ProphetAbility` 花 125 金揭露角色，`ProphetDeathProtectionHandler` 给被揭露者一层巫毒免疫，`ProphetShopHandler` 只卖水晶球。
- 圣母：`GoddessAbility` 把目标洗成随机平民角色，清空商店并发左轮，`GoddessRoleAssignedHandler` 只做分配时的冷却初始化。
- 天使：`AngelAbility` 在“安抚”和“守护”间切换，`AngelPlayerComponent` 保存守护目标和安抚粒子状态，`AngelDeathProtectionHandler` 会让守护目标死时天使代死，`AngelConstants` 控制 30 秒守护、90 秒安抚和 2 格贴身判定。
- 胆小鬼：`CowardPlayerComponent` 按周围危险调心情和感官反馈，`SedativePlayerComponent` 管镇静状态和过量死亡，`CowardShopHandler` 卖镇静试剂，`CowardGunCooldownHandler`、`CowardFovMixin`、`CowardCameraMixin` 和 `SedativeTrayViewMixin` 调整左轮冷却、视野和托盘行为。
- 追忆者：`RemembererInteractionHandler` 负责摸取回忆书，`RemembererPlayerComponent` 管回忆和狙击冷却，`RemembererSniperManager`、`SniperRifleItem`、`SniperRifleBulletItem` 实现狙击枪，`RemembererReplayBookBuilder` 负责把三分钟内的事件写成书。
- 时停者：`TimekeeperPlayerComponent` 保存光阴收入计时、怀表三模式冷却、回溯保护和时间狭缝状态；`TimekeeperWorldComponent` 每 4 tick 采样一次局内快照，保留 120 秒历史并按 30 秒深度倒放；`TimekeeperSnapshots` 负责恢复玩家运行态、背包、物品冷却、尸体、掉落物、门/火等可控状态，后续新增组件时必须同步检查它的快照清单。
- 服务员：`WaiterInteractionHandler` 判断玩家当前缺什么并递送，`WaiterShopHandler` 提供随机饮品、食物、药水、吧凳、钓鱼竿、唱片、篝火、烟熏炉、睡袋和书，`WaiterConstants` 里集中定义互动距离、价格和奖励。
- 模仿者：`MimicRoleAssignedHandler` 只发假匕首，但杀手侧会把它当同伙看；`MimicBackfireDeathHandler` 在无辜者被推下列车时反噬自己，`KillerNeutralInstinctHandler` 和 `MimicInstinctHandler` 负责杀手视角提示。

### 义警阵营

- 更好的义警：`BetterVigilanteRoleAssignedHandler` 只发一颗手雷，能否进入池由 `Harpymodloader.setRoleMaximum(BETTER_VIGILANTE, vigilanteSlots >= 4 ? 1 : 0)` 动态控制，默认还会被 `shitpostRoles` 关闭。

### 附加词条

- 小孩子：只允许给变形怪，靠 `EntityAttributes.GENERIC_SCALE` 缩小模型。
- 变色龙：走 `ChameleonPlayerComponent` 和客户端 mixin，重点是外观伪装。
- 猜测者：`GuessC2SPacket` + `GuesserAbility`，可以猜平民职业，猜错以后按配置走 `none / death / explode`，是否允许平民拿到它由 `allowCivillianGuessers` 决定。
- 盗墓者：当前主要是给尸体相关 HUD 和验尸视图开权限，尸体提示由演尸官/盗墓者共享的 `RoleNameHudApi` provider 判断权限。
- 羽化者：只要任意一方带这个词条，就不会和另一名玩家发生实体碰撞，`DoNotCollideWithFeatherMixin` 负责这条规则。

## 通用系统

- `Noellesroles.java` 现在只是 Fabric entrypoint，唯一职责是调用 `NoellesRolesBootstrap.init()`。
- `NoellesRolesBootstrap.java` 负责维护初始化顺序，把配置、物品、实体、托盘/床效果、回放、商店、经济、事件和 packet 引导串起来。
- `NoellesRolesCore.java` 保存 `MOD_ID`、日志器和 `Identifier` 工具方法，避免再把入口类当公共常量仓库。
- `NoellesRoleIds.java` 保存所有职业 / 词条稳定 id；新增 id 先放这里。
- `NoellesRoleRegistry.java` 保存所有 `Role` 实例和 Wathe 显式阵营注册；新增职业不要再写进 `Noellesroles.java`。
- `NoellesModifierRegistry.java` 保存 Harpy 词条注册。
- `NoellesRoleGroups.java` 保存跨系统共享分组，例如原版角色列表和 `KILLER_SIDED_NEUTRALS`。
- `NoellesDeathReasons.java` 保存 NoellesRoles 专属死亡原因 id。
- `NoellesEventIds.java` 保存回放事件、托盘/床效果和护盾来源 id。
- `NoellesRolesPayloadTypes.java` 负责注册自定义 payload codec。
- `NoellesRolesPacketReceivers.java` 负责注册服务端 packet receiver，并把旧的按职业能力分发逻辑集中在 packet 层。
- `NoellesRolesEventBootstrap.java` 负责事件监听、server tick、回合清理和 Harpy 禁用职业配置同步；其中人数相关动态上限包括 `Mimic`、`Vulture`、`Hacker`、`Drugmaker` 和 `Better Vigilante`。
- `NoellesRoleLimitsBootstrap.java` 负责开服时的静态 Harpy 上限，例如 `Conductor`、`Executioner`、`Jester`、`Dreamer`、`Starstruck` 等默认最大生成数。
- `NoellesRolesComponents.java` 负责把所有 CCA component 和 world component 一次性注册进去。
- `TimekeeperWorldComponent.java` 负责时停者世界级快照历史、回溯游标、保护名单和回溯播放。
- `TimekeeperRiftHandler.java` 负责时间狭缝入口、动态失效检测，以及“狭缝玩家不应继续阻塞胜利结算”时的提前收束。
- `TimekeeperSnapshots.java` 负责把 Wathe / Noelles 的可回溯运行态写成快照并恢复；新增 CCA 组件后要同步加入这里的 `PLAYER_COMPONENTS` 或 `WORLD_COMPONENTS`，否则回溯时该组件会停留在“回溯前未来状态”。
- `NoellesRolesRoleAssignedBootstrap.java` 负责统一监听 `ModdedRoleAssigned`，先写通用能力冷却，再按固定顺序分发到各职业。
- `NoellesRolesCombatBootstrap.java` 负责统一接入 Wathe `GunShotApi`，只调用各职业 / 词条自己的枪击、左轮反火和冷却 handler。
- `NoellesRolesDeathBootstrap.java` 负责统一监听 `AllowPlayerDeath` 并接入 Wathe `DeathApi`，保持“先保命，再强制放行，再反噬”和“死亡阶段按优先级执行”的顺序。
- `NoellesRolesShopBootstrap.java` 负责固定商店、动态商店和默认杀手商店改写。
- `NoellesRolesShops.java` 负责支付、物品交付、特殊道具瞬发结算和购买回填。
- `NoellesRolesEconomyBootstrap.java` 负责金币 HUD、任务收入、被动收入和经济类词条接入。
- `NoellesRolesTrayEffects.java` 和 `NoellesRolesBedEffects.java` 负责把炸弹、毒药、镇静等逻辑接进托盘和床。
- `NoellesRolesReplayBootstrap.java` 负责把 NoellesRoles 的事件 id 注册到 Wathe 回放系统。
- `NoellesRolesReplayFormatters.java` 负责把 NoellesRoles 的专属事件翻译成回放文本。
- `NoellesrolesVoiceChatPlugin.java` 负责语音聊天桥接，主要给接线员、附身和亡语杀手这类职业用。
- `NoellesHudHandlers.java` 是客户端通用屏幕 HUD 总注册入口，只负责调用各职业 / 词条自己的 `register()` 和少量实体名牌 provider。
- `NoellesHudSupport.java` 负责复用 Wathe `HudOverlayApi` 的存活职业注册、右下角文字绘制和玩家名兜底。
- `NoellesCrosshairHandlers.java` 是客户端准心图标总注册入口，只负责调用各职业自己的 `*Crosshair.register()`。
- `NoellesInventoryButtons.java` 是客户端背包按钮总注册入口，只负责调用各职业自己的 `*InventoryButtons.register()`。
- `NoellesInventoryButtonSupport.java` 负责复用 Wathe `InventoryButtonApi` 的注册、分页、在线玩家和头像列表辅助。
- `GameEvents.ON_FINISH_FINALIZE` 会在回合结束时清理交换者延迟交换、隐藏尸体、魔术师播放实体、飞斧、角色装置和捕捉装置，防止影响下一局。

## 时停者回溯快照接入

时停者的时间回溯不是整张地图方块级回滚，而是“可控运行态快照”。它会恢复玩家位置、生命、药水、背包、物品冷却、部分 Wathe/Noelles 组件、尸体、掉落物、门和火等状态。核心入口是：

- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperWorldComponent.java`
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperRiftHandler.java`
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperSnapshots.java`
- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperWorldStateSnapshot.java`

新增职业或词条只要新增了 CCA 组件，就必须判断这个组件是否属于局内运行态。如果它会影响冷却、目标、标记、伪装、任务进度、免死层数、语音/聊天状态、商店状态、转职状态、实体控制状态等玩法结果，就要加入 `TimekeeperSnapshots`：

- 玩家组件加到 `PLAYER_COMPONENTS`
- 世界组件加到 `WORLD_COMPONENTS`
- 组件的 `writeToNbt` / `readFromNbt` 必须完整覆盖可回溯字段
- 恢复后需要客户端立刻看到的状态，组件必须能被 `ComponentKey.sync(...)` 正确同步

如果组件只是配置、常量缓存、客户端临时显示缓存，或者像 `TimekeeperWorldComponent` 自己一样代表“正在回溯的播放机械”，不要盲目加入快照；这种例外要在代码注释里说明原因。若组件指向自定义实体、延迟任务、语音连接、全局 Map 或非 CCA 静态状态，单纯保存组件 NBT 可能不够，还要给对应实体/管理器补快照恢复或回合清理逻辑。

## 时间狭缝与胜利规则接入

时间狭缝会把刚死亡的玩家临时拉成 Wathe 的“特殊存活旁观”。这让 30 秒内的时间回溯可以把死者从历史快照里复活，但也意味着这些玩家在倒计时结束前会被 `GameFunctions.isPlayerAliveAndSurvival(...)` 和 `VictoryApi` 视为仍然存活。

因此，只要新增或修改了独立胜利、共胜、或“活着时阻拦普通杀手 / 乘客结算”的职业/词条，就必须同步检查：

- `src/main/java/org/agmas/noellesroles/roles/timekeeper/TimekeeperRiftHandler.java`
- 对应的 `*VictoryRule.java`

如果新规则会在 `VictoryApi` 中返回 `KEEP_RUNNING`，或者依赖“场上只剩自己/同阵营成员”触发独立胜利，就要把它的“非狭缝存活阻拦条件”补进 `TimekeeperRiftHandler`。否则最后一个杀手或独立阻拦者死亡后可能因为仍处于时间狭缝，被继续算作存活，导致普通阵营胜利延迟到 30 秒狭缝结束后才结算。

新增这类规则时至少测试两种情况：

- 目标职业/词条玩家正常存活时，仍能按自己的规则阻拦或触发独立胜利。
- 目标职业/词条玩家死亡并进入时间狭缝时，如果排除狭缝玩家后已经只剩一个可获胜阵营，狭缝应立即收束为真死亡，普通结算不应被拖住。

## 枪械与死亡 API 接入方式

NoellesRoles 的枪击接管、左轮反火和击杀流程已经迁移到 Wathe 公开 API。新增或维护同类机制时，默认不要再 mixin `GunShootPayload`、`RevolverItem`、`DerringerItem` 或 `GameFunctions.killPlayer(...)`。

Wathe 侧公开入口：

- `dev.doctor4t.wathe.api.combat.GunShotApi`
- `dev.doctor4t.wathe.api.combat.GunShotContext`
- `dev.doctor4t.wathe.api.combat.RevolverPenaltyContext`
- `dev.doctor4t.wathe.api.death.DeathApi`
- `dev.doctor4t.wathe.api.death.DeathContext`
- `dev.doctor4t.wathe.api.death.BodySpawnContext`

NoellesRoles 侧接入入口：

- `src/main/java/org/agmas/noellesroles/combat/NoellesRolesCombatBootstrap.java`
- `src/main/java/org/agmas/noellesroles/death/NoellesRolesDeathBootstrap.java`

枪械相关逻辑按职业或词条拆到自己的包里，例如：

- `roles/robber/RobberGunHandler.java`：强盗手枪开火和击杀后掉枪。
- `roles/assassin/AssassinGunHandler.java`：无声左轮开火。
- `roles/bounty_hunter/BountyHunterGunHandler.java`：赏金枪械开火。
- `roles/coward/CowardGunCooldownHandler.java`：胆小鬼左轮冷却修正。
- `roles/jester/JesterGunTargetHandler.java`：假左轮客户端目标覆写。
- `roles/executioner/ExecutionerGunPenaltyHandler.java`：仇杀客目标和配置类左轮惩罚豁免。
- `roles/licensed_villain/LicensedVillainGunPenaltyHandler.java`：执照恶棍左轮惩罚豁免。
- `roles/morphling/MorphlingGunPenaltyHandler.java`：变形试剂伪装下的左轮惩罚豁免。

死亡 / 击杀相关逻辑同样按职业或词条拆分，例如：

- `roles/timekeeper/TimekeeperDeathHandler.java`：重复死亡吞噬和时间狭缝。
- `modifiers/dual_personality/DualPersonalityDeathHandler.java`：双重人格致死转化。
- `roles/bounty_hunter/BountyHunterDeathHandler.java`：赏金猎人击杀奖励。
- `roles/controller/ControllerDeathHandler.java`：附体死亡连锁。
- `roles/voodoo/VoodooDeathHandler.java`：巫毒死亡连锁。
- `roles/coroner/CoronerBodySpawnHandler.java`：尸体死因数据。
- `roles/assassin/AssassinBodySpawnHandler.java`：刺客隐藏尸体。

优先级按 Wathe `DeathApi` 的常量表达：重复死亡保护最高，其次是特殊存活保护、死亡流程状态、回放上下文、致死确认前拦截、普通逻辑、确认死亡后的奖励 / 二段机制、最终清理。priority 越大越先执行；同 priority 后注册的规则先执行。handler 返回 `PASS` 或不处理时继续往下走，只有明确 `CANCEL`、`ALLOW`、`DENY` 或 `HANDLED` 才终止对应链路。

## 背包按钮接入方式

NoellesRoles 只消费 Wathe 的公开 API，不在本工程复制 API 类。Wathe API 定义在：

- `D:\哈比快车最新源码\wathe\Wathe - 副本1\src\main\java\dev\doctor4t\wathe\api\client\inventory`

NoellesRoles 的接入代码放在当前工程：

- `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\src\client\java\org\agmas\noellesroles\client\inventory`
- `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\src\client\java\org\agmas\noellesroles\client\ui\roles\<role>`
- `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\src\client\java\org\agmas\noellesroles\client\ui\modifiers\<modifier>`

新增职业如果要在 Wathe 限制背包里显示玩家选择按钮，优先新建：

```text
src/client/java/org/agmas/noellesroles/client/ui/roles/my_role/MyRoleInventoryButtons.java
```

然后在 `NoellesInventoryButtons.register()` 里加一行：

```java
MyRoleInventoryButtons.register();
```

常规写法：

```java
public static void register() {
    NoellesInventoryButtonSupport.registerLimited("my_role", MyRoleInventoryButtons::create);
}

private static InventoryButtonExtension create(InventoryButtonContext context) {
    return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.MY_ROLE)
            ? new Extension()
            : null;
}
```

需要玩家头像分页时，让内部 `Extension` 继承：

```java
NoellesInventoryButtonSupport.PagedExtension<MyRolePlayerWidget>
```

并在 `populate(...)` 里添加按钮。`PagedExtension` 会负责上一页/下一页按钮、居中坐标、每页 10 人和页码缓存。像变形怪这种点击后按钮必须全部消失的职业，应覆写 `selectionVisible(...)`，让头像和翻页按钮共用同一个可见性条件。

需要动态增删列表时参考 `ConvenerInventoryButtons`：每 tick 比较目标 UUID 列表，变化后重建同一个 group。需要文本输入阶段禁止按 E 关背包时，在自己的 `InventoryButtonExtension.allowInventoryKeyClose(...)` 返回 `false`，并在 `close(...)` 里清掉静态输入状态。

旧的 `LimitedInventoryScreen` / `LimitedHandledScreen` screen mixin 已经迁出或删除。新增背包按钮不要再添加 `*ScreenMixin` 到 `noellesroles.client.mixins.json`；普通屏幕 HUD、准心图标和准心名字也已经有 Wathe `HudOverlayApi` / `CrosshairHudApi` / `RoleNameHudApi`，只有相机、输入控制、物品渲染等 Wathe 尚未公开 API 的场景才考虑窄 mixin。

## HUD 接入方式

NoellesRoles 的普通屏幕 HUD 已经迁移到 Wathe 公开 API，不再通过 `InGameHud` 或 `RoleNameRenderer` mixin 注入。新增职业/词条时按现有结构拆文件：

- 普通右下角职业状态：新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>StatusHud.java`。
- 词条自己的固定屏幕 HUD：新建 `src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/<ModifierName>Hud.java`。
- 通用注册入口：在 `NoellesHudHandlers.register()` 里调用该职业 `*StatusHud.register()`。
- 重复布局和存活职业过滤：使用 `NoellesHudSupport.registerAliveRole(...)`、`drawBottomRightLine(...)` 或 `drawBottomRightLines(...)`。
- 准心图标、武器锁定和准心下方小进度条：新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>Crosshair.java`，并在 `NoellesCrosshairHandlers.register()` 里调用；通过 Wathe `CrosshairHudApi.registerProvider(...)` 或 `registerOverlay(...)` 接入。
- 准心名字、尸体信息、目标旁边的小提示：使用 Wathe `RoleNameHudApi.registerExtraHud(...)`、`registerName(...)` 或 `registerEntityName(...)`。
- 狙击镜、黑屏控制、绑架提示这类全屏叠加：使用 `HudOverlayApi.register(...)` 并选择合适的 `HudOverlayLayer`。

普通职业状态推荐写法：

```java
public static void register() {
    NoellesHudSupport.registerAliveRole("roles/my_role/status", NoellesRoleRegistry.MY_ROLE, context -> {
        MyRolePlayerComponent component = MyRolePlayerComponent.KEY.get(context.player());
        if (!component.shouldShowStatus()) {
            return;
        }
        NoellesHudSupport.drawBottomRightLine(
                context,
                Text.translatable("hud.noellesroles.my_role.status"),
                NoellesRoleRegistry.MY_ROLE.color()
        );
    });
}
```

`registerAliveRole(...)` 会统一检查“本地玩家是该职业”以及 Wathe 的 `GameFunctions.isPlayerAliveAndSurvival(...)` 存活定义，所以死亡、旁观、创造和非局内状态不会继续显示职业 HUD。不是职业独占的 provider，例如被附体者、被绑架者或狙击镜遮罩，必须在自己的 lambda 里显式判断 `context.aliveAndSurvival()`。

不要把多个职业/词条 HUD 合并到一个巨大的 renderer。`NoellesHudHandlers` / `NoellesCrosshairHandlers` 只负责注册顺序，具体状态、文本、颜色和准心判定仍然放在各职业或词条自己的客户端包里；旧的 `*HudMixin` / `*CrosshairMixin` 被 API 替代后，也不要重新加回 `noellesroles.client.mixins.json`。

## 新职业注册流程

下面是给其他开发者看的标准接入方式。

### 1. 先注册角色

建议按当前拆分结构注册，不要再把字段加回 `Noellesroles.java`：

- 在 `NoellesRoleIds.java` 里新增稳定 `Identifier`。
- 在 `NoellesRoleRegistry.java` 里新增 `Role` 实例，并用 Wathe 的显式阵营注册方法。
- 如果这是词条，改 `NoellesModifierRegistry.java`；如果它需要共享分组，再改 `NoellesRoleGroups.java`。

推荐写法：

```java
// NoellesRoleIds.java
public static final Identifier MY_ROLE_ID = NoellesRolesCore.id("my_role");

// NoellesRoleRegistry.java
public static final Role MY_ROLE = WatheRoles.registerCivilianRole(
    new Role(
        NoellesRoleIds.MY_ROLE_ID,
        0x66CCFF,
        true,
        false,
        Role.MoodType.REAL,
        WatheRoles.CIVILIAN.getMaxSprintTime(),
        false
    )
);
```

如果是杀手、中立或义警，就换成对应的 `registerKillerRole`、`registerNeutralRole` 或 `registerVigilanteRole`。

### 2. 补语言文件

至少补这些键：

- `announcement.role.noellesroles.my_role`
- `announcement.goals.noellesroles.my_role`

如果有 HUD、提示、消息、商店说明，也一起补：

- `hud.noellesroles.my_role.*`
- `message.noellesroles.my_role.*`
- `tip.noellesroles.my_role.*`

`Harpymodloader.refreshRoles()` 会在找不到手写翻译时生成兜底文本，但正式发布最好还是自己写中文。

### 3. 注册开局分配逻辑

如果这个职业需要开局物品、初始冷却、初始状态，就新建：

- `src/main/java/org/agmas/noellesroles/roles/my_role/MyRoleAssignedHandler.java`

然后在 `NoellesRolesRoleAssignedBootstrap` 里按旧顺序加入一行：

```java
MyRoleAssignedHandler.onRoleAssigned(player, role);
```

如果你要在游戏中动态转职，也要记得：

```java
gameWorld.addRole(player, newRole);
ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, newRole);
```

### 4. 需要持久状态时再上组件

如果职业要存冷却、目标、计数、伪装状态、标记等，就建 CCA 组件：

- `src/main/java/org/agmas/noellesroles/roles/my_role/MyRolePlayerComponent.java`

注册时照着 `NoellesRolesComponents` 里的写法加进去，并把 component id 同步写进 `fabric.mod.json` 的 `custom.cardinal-components`。

建议默认用：

- `RespawnCopyStrategy.NEVER_COPY`
- `AutoSyncedComponent`
- `ServerTickingComponent`，必要时再加 `ClientTickingComponent`

注册新组件后还要同步检查时停者回溯：如果该组件属于局内运行态，把玩家组件加入 `TimekeeperSnapshots.PLAYER_COMPONENTS`，把世界组件加入 `TimekeeperSnapshots.WORLD_COMPONENTS`。这一步和 `NoellesRolesComponents`、`fabric.mod.json` 一样属于组件注册的必做项，否则时间回溯只会回滚玩家背包/位置等基础状态，而新组件仍停在回溯前的未来状态。

如果新职业或词条还新增了 `VictoryApi` 规则，尤其是独立阵营胜利、共胜，或活着时会返回 `KEEP_RUNNING` 阻拦普通杀手/乘客结算，也要同步检查 `TimekeeperRiftHandler`。时间狭缝玩家会被 Wathe 当作特殊存活旁观；没有把新阻拦条件加入狭缝收束判断时，最后一个阻拦者死亡后可能仍在狭缝里卡住胜利结算。

### 5. 需要按键或目标选择时，加 packet

如果是普通能力键，可以复用当前的 `ABILITY_PACKET` 分支；如果要独立目标选择，建议单独建 packet。

标准路径：

- `src/main/java/org/agmas/noellesroles/packet/role/my_role/`

注册时要做两步：

1. 在 `NoellesRolesPayloadTypes.register()` 里注册 codec：`PayloadTypeRegistry.playC2S().register(...)`
2. 在 `NoellesRolesPacketReceivers.register()` 里注册服务端接收器：`ServerPlayNetworking.registerGlobalReceiver(...)`

如果有客户端选择界面，就在 `src/client/java/org/agmas/noellesroles/client/...` 里接按钮、屏幕或 HUD。背包内按钮必须优先走 Wathe `InventoryButtonApi`，按上面的“背包按钮接入方式”放到职业自己的 `*InventoryButtons.java`。

### 6. 需要物品、实体或模型时

- 物品放 `src/main/java/org/agmas/noellesroles/item/`
- 实体放 `src/main/java/org/agmas/noellesroles/entities/`
- 物品贴图放 `src/main/resources/assets/noellesroles/textures/item/`
- 物品模型放 `src/main/resources/assets/noellesroles/models/item/`
- 文本放 `src/main/resources/assets/noellesroles/lang/`

如果你要让物品有默认冷却，记得在 `ModItems.init()` 里补 `GameConstants.ITEM_COOLDOWNS.put(...)`。

### 7. 需要商店时

两种做法：

- 固定商店：写 `MyRoleShopHandler.getShopEntries()`，然后在 `NoellesRolesShopBootstrap` 里 `registerStatic(...)`
- 默认杀手商店改写：写 `ShopApi.registerShopModifier(...)`

如果商品是瞬发效果、不是单纯放进背包，还要在 `NoellesRolesShops.deliverPurchasedStack()` 里补交付分支。

### 8. 需要死亡保护、反噬或特殊清理时

把逻辑拆成独立处理器，再接进 `NoellesRolesDeathBootstrap`。保命类机制继续走 `AllowPlayerDeath` 保护链；真正依赖“死亡流程阶段、击杀收益、尸体生成、确认死亡后清理”的机制优先走 Wathe `DeathApi`，不要再 mixin `GameFunctions.killPlayer(...)`。

常见结构：

- `YourRoleDeathProtectionHandler`
- `YourRoleBackfireDeathHandler`
- `YourRoleDeathHandler`
- `YourRoleBodySpawnHandler`

如果这个职业会在回合结束残留实体，也要在 `GameEvents.ON_FINISH_FINALIZE` 里清掉。

### 9. 需要客户端表现时

客户端相关内容统一放在：

- `src/client/java/org/agmas/noellesroles/client/...`

常见内容包括：

- 普通屏幕 HUD：走 `NoellesHudHandlers` + 各职业 `*StatusHud.java`，并接入 Wathe `HudOverlayApi`
- 准心图标 / 武器锁定 / 准心下方进度条：走 Wathe `CrosshairHudApi`
- 准心名字 / 准心附近提示：走 Wathe `RoleNameHudApi`
- 角色头像
- 光标高亮
- 视角/相机
- 皮肤和外观伪装

如果确实需要 mixin，记得加到：

- `src/main/resources/noellesroles.mixins.json`
- `src/client/resources/noellesroles.client.mixins.json`

背包内玩家选择按钮不是 mixin。它们应通过 `NoellesInventoryButtons` 注册，再由各职业包里的 `*InventoryButtons.java` 接入 Wathe `InventoryButtonApi`。
普通屏幕 HUD 也不是 mixin。它们应通过 `NoellesHudHandlers` 注册，再由各职业包里的 `*StatusHud.java` 接入 Wathe `HudOverlayApi`；只有相机、输入控制、物品渲染等缺少公开 API 的场景才保留窄 mixin。
枪击接管、左轮反火、默认击杀收益、确认死亡后清理和尸体生成回调也不是 mixin；它们应通过 `NoellesRolesCombatBootstrap` / `NoellesRolesDeathBootstrap` 调用各职业 handler 接入 Wathe `GunShotApi` / `DeathApi`。

### 10. 需要回放和文本时

如果你的职业会改变死亡、物品使用、身份揭露、转职、传送等关键事件，最好同步做两件事：

1. `GameRecordManager.recordGlobalEvent(...)`
2. 在 `NoellesRolesReplayFormatters` 里加对应的格式化器

这样回放文本不会只剩一个“发生了某事”，而是能讲清楚是谁、做了什么、结果是什么。

## 开发建议

- 新职业先定阵营，再定是否需要组件、packet、商店和 client。
- 先写最小闭环，再补 HUD 和回放。
- 动态转职时一定要同步 `ModdedRoleAssigned`。
- 尽量把常量收进 `Constants` 类里，别散落 magic number。
- 如果你想让职业能被 `forceRole`、`listRoles` 和开局池稳定识别，务必走 `WatheRoles.register...` 和 `Harpymodloader.setRoleMaximum(...)`。
- 服主日常开关角色可以直接用 HarpyModLoader 的 `/listRoles` 和 `/setEnabledRole <modid:path> true|false`，配置会写进 `HarpyModLoaderConfig` 的 `disabled` 列表。

## 参考扩展

这套仓库的结构和下面几个扩展思路很像：

- `StupidExpress2.1`：集中式角色注册、动态最大值、商店改写、`ModdedRoleAssigned`。
- `kinssaba`：大体量职业组件化、转职链、商店和 CCA 的分层方式。
- `StarryExpress1.3.2`：更精简的中央注册 + 商店改写模板。

如果你打算继续加职业，建议先顺着这几个模组的写法看一遍，再决定是新建一个大包，还是直接复用现有 `roles/<role>/` 结构。
