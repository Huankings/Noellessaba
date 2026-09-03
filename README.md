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
- 停电机制引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesBlackoutBootstrap.java`
- 回放 formatter 注册引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesReplayBootstrap.java`
- Harpy 角色上限引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRoleLimitsBootstrap.java`
- 职业分配总引导：`src/main/java/org/agmas/noellesroles/roleassign/NoellesRolesRoleAssignedBootstrap.java`
- 枪械 / 左轮反火总引导：`src/main/java/org/agmas/noellesroles/combat/NoellesRolesCombatBootstrap.java`
- 死亡保护 / 击杀流程总引导：`src/main/java/org/agmas/noellesroles/death/NoellesRolesDeathBootstrap.java`
- 体力与移动速度修正总引导：`src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesMovementBootstrap.java`
- 商店总引导：`src/main/java/org/agmas/noellesroles/shop/NoellesRolesShopBootstrap.java`
- 玩家碰撞总引导：`src/main/java/org/agmas/noellesroles/collision/NoellesPlayerCollisionHandlers.java`
- 物品：`src/main/java/org/agmas/noellesroles/item/`
- Wathe 客户端武器目标 API：`dev.doctor4t.wathe.api.combat.WeaponTargetingApi`
- 职业逻辑：`src/main/java/org/agmas/noellesroles/roles/<role>/`
- 客户端：`src/client/java/org/agmas/noellesroles/client/`
- 杰森客户端雾效 provider：`src/client/java/org/agmas/noellesroles/client/roles/jason/JasonAbilityFogHandler.java`
- 通用 HUD 注册入口：`src/client/java/org/agmas/noellesroles/client/hud/NoellesHudHandlers.java`
- 通用 HUD 辅助：`src/client/java/org/agmas/noellesroles/client/hud/NoellesHudSupport.java`
- 低心情幻觉手持物预留注册：`src/client/java/org/agmas/noellesroles/client/mood/NoellesPsychosisHandlers.java`
- 客户端移动表现总引导：`src/client/java/org/agmas/noellesroles/client/movement/NoellesClientMovementBootstrap.java`
- 准心图标注册入口：`src/client/java/org/agmas/noellesroles/client/crosshair/NoellesCrosshairHandlers.java`
- 职业状态 HUD：`src/client/java/org/agmas/noellesroles/client/roles/<role>/*StatusHud.java`
- 职业准心图标：`src/client/java/org/agmas/noellesroles/client/roles/<role>/*Crosshair.java`
- 词条固定 HUD：`src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/*Hud.java`
- 背包按钮注册入口：`src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtons.java`
- 背包按钮共享工具：`src/client/java/org/agmas/noellesroles/client/inventory/NoellesInventoryButtonSupport.java`
- 职业背包按钮：`src/client/java/org/agmas/noellesroles/client/ui/roles/<role>/*InventoryButtons.java`
- 词条背包按钮：`src/client/java/org/agmas/noellesroles/client/ui/modifiers/<modifier>/*InventoryButtons.java`
- mixin：`src/main/java/org/agmas/noellesroles/mixin/` 和 `src/client/java/org/agmas/noellesroles/client/mixin/`

NoellesRoles 的职业或词条如果需要让本地观察者脑补目标手持指定/随机物品或手臂姿势，应使用 Wathe 的 `dev.doctor4t.wathe.api.client.mood.PsychosisItemApi`。当前 `NoellesPsychosisHandlers` 仅提供空注册入口，后续按职业/词条拆分 provider，并通过 priority 控制覆盖：高于 Wathe 默认 priority 0 的规则优先，低于或等于 0 的规则只在默认幻觉未处理时生效。该能力仅改变客户端视觉，不改变真实物品、服务端交互或攻击判定；特殊存活授权的 spectator/creative 仍会看到幻觉，普通死亡旁观则由 Wathe 自动清理。

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
- 杰森（`jason`）
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
- 刺客：`BayonetItem` + `AssassinPlayerComponent` + `AssassinBayonetAttackMixin` / `AssassinBodySpawnHandler` 实现无声刺杀、击退和隐藏尸体；隐藏尸体的渲染、准心不可选中和服务端交互拦截通过 `AssassinTargetVisibilityHandler` 接入 Wathe `TargetVisibilityApi`；`AssassinGunHandler` 通过 `GunShotApi` 接管无声左轮，`AssassinShopHandler` 负责刺刀、冷却刷新和商店改写。
- 变形怪：`MorphlingMorphAbility` 和 `MorphlingPlayerComponent` 负责变形成任意存活玩家、变形时长和冷却，选人包复用统一的 `MorphC2SPacket`。
- 魔术师：先录一段玩家操作，再用播放实体重放；`MagicianPlayerComponent` 存录像，`MagicianPlaybackManager`、`MagicianPlaybackEntity`、`MagicianServerHooks` 执行回放，`MagicianGunHandler` 通过 `GunShotApi` 记录枪击，`MagicianPlaybackDeathHandler` 通过 `DeathApi` 改写播放体击杀归属，其余动作记录 mixin 负责刀、手雷和交互时间线。
- 交换者：`SwapperC2SPacket` 加 `SwapperAbility` 先选两人，再按随机延迟交换位置，执行结果也会写回放。
- 幻灵：`PhantomAbility` 提供短时隐身，`PhantomPlayerComponent` 管倒计时，`PhantomConstants` 控制 35 秒隐身和 90 秒冷却。
- 洗脑师：`BrainwasherAbility` 能把目标平民洗成随机杀手角色，成功后清商店并广播；`BrainwasherRoleAssignedHandler` 只负责开局冷却初始化。
- 亡语杀手：默认被 `shitpostRoles` 关闭时自动禁用；源码上主要接语音聊天和疯狂观察视觉表现，核心入口是 `NoellesrolesVoiceChatPlugin`、`InsaneObserverAppearanceHandler` 和 `NoellesRolesConfig.insanePlayersSeeMorphs`。
- 杰森：核心状态由 `JasonWoundManager`、`JasonAbilityManager`、`JasonAbilityPlayerComponent` 和 `JasonPsychoHandler` 管理；投掷武器、倒地救治、油桶燃烧、杰森模式和“无恶不在”能力分别按职业包拆分。无恶不在的客户端雾效通过 `JasonAbilityFogHandler` 接入 Wathe `FogOverrideApi`，兼容 Sodium + Iris shaderpack。

### 杀手侧中立

- 狂信者：开局拿假匕首、假左轮和撬棍，`JesterRoleAssignedHandler` 只做发物品；`JesterDeathProtectionHandler` 保留 psycho 无敌窗口，`JesterGunTargetHandler`、`JesterJestMixin` 和 `JesterItemEntityMixin` 负责假武器和狂化表现。
- 仇杀客：`ExecutionerPlayerComponent` 负责目标，server tick 会在目标失效后重选；`ExecutionerDeathHandler` 通过 `DeathApi` 处理目标达成后的转职，`ExecutionerBackfireDeathHandler` 和 `ExecutionerGunPenaltyHandler` 处理误杀反噬和射击锁定。
- 秃鹫：`VultureRoleAssignedHandler` 会按当前人数算需要吞多少尸体，`VultureAbility` 吞尸后累计进度，达标后随机转成一个未禁用的杀手角色并发 200 金币，尸体状态记录在 `VulturePlayerComponent`。

### 平民阵营

- 典狱长：`ConductorRoleAssignedHandler` 开局发万能钥匙、开锁器和假左轮，`MasterKeyTrainDoorMixin`、`MasterKeySmallDoorMixin` 让钥匙能开火车上各种门，`ShouldDropOnDeath` 保证死亡掉落。
- 记者：`AwesomeBinglusRoleAssignedHandler` 直接发 12 张纸条和撬棍，这是一个默认关闭的搞笑职业。
- 工程师：`ToolboxItem` 负责修门，`CaptureDeviceItem` 负责定点拘束并生成报告，`PowerRestorationItem` 通过 Wathe `BlackoutApi.restorePower` 消除停电；`EngineerShopHandler` 把这三件东西接进商店。
- 酒保：`BartenderPlayerComponent` 追踪防御瓶充能和护甲，`BartenderDeathProtectionHandler` 把护甲当一次免死，`DefenseVialApplyMixin`、`PoisonToHealsMixin` 和 `CocktailItemMixin` 改酒和毒的处理。
- 风灵师：`WinderPlayerComponent` 记录已选目标和漂浮状态，`WinderAbility` 开关漂浮，`WinderTargetAbility` 负责选人，`WindMarkPlayerComponent` 负责风印记，`WinderShopHandler` 卖风弹和风印。
- 灵术师：`SpiritualistAbility` 一枚 G 键分出“出窍 / 附身 / 结束”几种行为，`SpiritualistPlayerComponent` 是主状态中心，`SpiritualistHostComponent` 保存被附身者状态，`SpiritualistManager` 负责控制输入、视角、回写背包、语音转发和结束冷却，`SpiritualistDeathProtectionHandler` 负责免死；脱体本体的玩家碰撞通过 Wathe `PlayerCollisionApi` 返回 `NO_COLLISION`，附身本体的不可见/不可选中/不可交互通过 `TargetVisibilityApi` 处理，只有原版投射物命中仍保留一个窄 mixin 兜底。
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
- 羽化者：只要任意一方带这个词条，就通过 Wathe `PlayerCollisionApi` 返回 `VANILLA_PUSH`，不会吃 Wathe 的实体墙阻挡，但保留原版玩家轻微推挤。

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
- `HackerRoleAssignmentRules.java` 通过 Harpy `RoleAssignmentApi` 注册 Hacker / Mimic 同局互斥，不再使用 Harpy 分配方法 mixin。
- `InitiateRoleAssignmentRules.java` 通过 Harpy 阶段结束回调补齐第二名初学者，保留“随机只抽一个名额、成对落地”的旧玩法。
- `LoversModifierAssignmentRules.java` 和 `DualPersonalityModifierAssignmentRules.java` 通过 Harpy 词条公告前生命周期回调消费强制配对，并在公告显示前清理随机残留。
- `NoellesPlayerCollisionHandlers.java` 负责玩家碰撞 API 总入口，只调用各职业 / 词条自己的碰撞 handler；当前包含灵术师脱体本体和 FEATHER。
- `NoellesRoleLimitsBootstrap.java` 负责开服时的静态 Harpy 上限，例如 `Conductor`、`Executioner`、`Jester`、`Dreamer`、`Starstruck` 等默认最大生成数。
- `NoellesRolesComponents.java` 负责把所有 CCA component 和 world component 一次性注册进去。
- `TimekeeperWorldComponent.java` 负责时停者世界级快照历史、回溯游标、保护名单和回溯播放。
- `TimekeeperRiftHandler.java` 负责时间狭缝入口、动态失效检测，以及“狭缝玩家不应继续阻塞胜利结算”时的提前收束。
- `TimekeeperSnapshots.java` 负责把 Wathe / Noelles 的可回溯运行态写成快照并恢复；新增 CCA 组件后要同步加入这里的 `PLAYER_COMPONENTS` 或 `WORLD_COMPONENTS`，否则回溯时该组件会停留在“回溯前未来状态”。
- `NoellesRolesRoleAssignedBootstrap.java` 负责统一监听 `ModdedRoleAssigned`，先写通用能力冷却，再按固定顺序分发到各职业。
- `NoellesRolesCombatBootstrap.java` 负责统一接入 Wathe `GunShotApi`，只调用各职业 / 词条自己的枪击、左轮反火和冷却 handler。
- `NoellesRolesDeathBootstrap.java` 负责统一监听 `AllowPlayerDeath` 并接入 Wathe `DeathApi`，保持“先保命，再强制放行，再反噬”和“死亡阶段按优先级执行”的顺序。
- `NoellesRolesMovementBootstrap.java` 负责统一接入 Wathe `PlayerStaminaApi` / `PlayerMovementApi`，把各职业 / 词条的体力、加速、减速和覆盖规则聚合到一起。
- `NoellesRolesShopBootstrap.java` 负责固定商店、动态商店和默认杀手商店改写。
- `NoellesRolesShops.java` 负责支付、物品交付、特殊道具瞬发结算和购买回填。
- `NoellesRolesEconomyBootstrap.java` 负责金币 HUD、任务收入、被动收入和经济类词条接入。
- `NoellesRolesBlackoutBootstrap.java` 负责接入 Wathe `BlackoutApi`，当前把杀手侧中立注册为停电夜视、独立中立注册为停电失明；后续具体职业特殊规则要拆到对应 `roles/<role>/*BlackoutHandler` 再由这里调用。
- `NoellesRolesTrayEffects.java` 和 `NoellesRolesBedEffects.java` 负责把炸弹、毒药、镇静等逻辑接进托盘和床。
- `NoellesRolesReplayBootstrap.java` 负责把 NoellesRoles 的事件 id 注册到 Wathe 回放系统。
- `NoellesRolesReplayFormatters.java` 负责把 NoellesRoles 的专属事件翻译成回放文本。
- `NoellesrolesVoiceChatPlugin.java` 负责语音聊天桥接，主要给接线员、附身和亡语杀手这类职业用。
- `JasonAbilityManager.java` 负责无恶不在服务端状态机、冷却、退出过渡、惊吓和回合清理；`JasonAbilityFogHandler.java` 负责客户端三类雾距和进入/退出过渡。
- `NoellesHudHandlers.java` 是客户端通用屏幕 HUD 总注册入口，只负责调用各职业 / 词条自己的 `register()` 和少量实体名牌 provider。
- `NoellesHudSupport.java` 负责复用 Wathe `HudOverlayApi` 的存活职业注册、右下角文字绘制和玩家名兜底。
- `NoellesClientMovementBootstrap.java` 负责客户端移动相关表现的聚合入口，当前主要给双重人格等需要本地提示的规则使用。
- `NoellesCrosshairHandlers.java` 是客户端准心图标总注册入口，只负责调用各职业自己的 `*Crosshair.register()`。
- `NoellesInventoryButtons.java` 是客户端背包按钮总注册入口，只负责调用各职业自己的 `*InventoryButtons.register()`。
- `NoellesInventoryButtonSupport.java` 负责复用 Wathe `InventoryButtonApi` 的注册、分页、在线玩家和头像列表辅助。
- `GameEvents.ON_FINISH_FINALIZE` 会在回合结束时清理交换者延迟交换、隐藏尸体、魔术师播放实体、飞斧、角色装置和捕捉装置，防止影响下一局。

## Wathe 心情任务 API 接入方式

NoellesRoles 的心情任务逻辑应优先使用 Wathe `MoodTaskApi`，不要直接 mixin `PlayerMoodComponent`。

- 新增任务定义用 `MoodTaskApi.registerTask(...)`，再由 `NoellesRolesMoodTaskBootstrap` 调用对应职业或词条自己的 `*MoodTaskHandler.init()`。
- 主动发放随机任务用 `assignRandomTask` / `assignRandomTasks`，指定任务用 `assignTask(player, taskId)`。
- 如果职业要阻止 Wathe 自动刷任务、低心情补槽、外部随机发放或指定发放，用 `MoodTaskApi.registerAssignmentRule(...)`，并按 `AssignmentSource` 判断来源。
- 不要在 server tick 里等任务出现在 HUD 后再 `removeTask`。这种做法会让客户端看到任务闪现。影子小丑的第一阶段任务节奏见 `roles/shadow_jester/ShadowJesterTaskHandler.java`：它只允许自己 guard 包住的随机发放，其余发放在进入任务栏前就被拒绝。
- 任务完成后的收益和副作用用 `TaskCompletionApi.AFTER_TASK_COMPLETE`、任务收入 provider、任务收入规则或 `MoodTaskApi.registerCompletionRule(...)`。

## Harpy 分配 API 接入方式

NoellesRoles 现在不再通过 mixin Harpy 私有分配方法来做同局互斥、绑定生成或强制词条配对。相关逻辑统一走 `org.agmas.harpymodloader.api.assignment`，并按职业 / 词条拆到自己的小类里，再由 `NoellesRolesBootstrap.init()` 调用。

当前已经迁移的入口：

- `roles/hacker/HackerRoleAssignmentRules.java`：`RoleAssignmentApi.registerMutualExclusion(...)`，在 `CIVILIAN_REPLACEMENT` 阶段阻止 Hacker 和 Mimic 同局随机生成。
- `roles/initiate/InitiateRoleAssignmentRules.java`：`RoleAssignmentApi.registerAfterPhaseHandler(...)`，平民 / 中立替换阶段结束后，如果只出现 1 名初学者，就从另一名中立玩家中补第 2 名，并清理旧中立职业物品。
- `modifiers/lovers/LoversModifierAssignmentRules.java`：`ModifierAssignmentApi.registerBeforeAnnouncementHandler(...)`，在 Harpy 词条公告前消费 `/noellesroles setlovers` 的强制配对，清掉随机恋人残留后写入最终配对。
- `modifiers/dual_personality/DualPersonalityModifierAssignmentRules.java`：分配开始前刷新双重人格动态上限，公告前消费强制主 / 副人格配对。

新增同类规则时按这个格式放文件：

```text
src/main/java/org/agmas/noellesroles/roles/<role>/<RoleName>RoleAssignmentRules.java
src/main/java/org/agmas/noellesroles/modifiers/<modifier>/<ModifierName>ModifierAssignmentRules.java
```

职业规则常用 `RoleAssignmentApi.registerMutualExclusion(...)`、`registerOneWayExclusion(...)`、`registerBeforePhaseHandler(...)`、`registerAfterPhaseHandler(...)`。阶段回调里需要补职业时用 `RoleAssignmentPhaseContext.assignRole(...)`，不要自己拼 `gameWorldComponent.addRole(...) + ModdedRoleAssigned`。

词条规则常用 `ModifierAssignmentApi.registerModifierExcludesRole(...)`、`registerModifierRequiresRole(...)`、`registerModifierMutualExclusion(...)`、`registerModifierOneWayExclusion(...)`。如果要替代旧 `assignModifiers` 的 HEAD / 公告前 / TAIL mixin，用 `registerBeforeAssignmentHandler(...)`、`registerBeforeAnnouncementHandler(...)`、`registerAfterAssignmentHandler(...)`。

已经删除并不应加回的 Harpy 分配类 mixin 包括 Hacker / Mimic 排斥、初学者配对、强制恋人、强制双重人格。后续只有在 Harpy API 无法表达需求时才考虑新增窄 mixin，并且要先评估是否应该把 Harpy API 继续补强。

## 玩家体力与移动接入

Wathe 已经把玩家体力和移动速度公开化了。NoellesRoles 侧不需要再 shadow `PlayerEntity` 的 `forwardSpeed` / `sidewaysSpeed`，也不要再自己重写 `travel()`、`jump()` 或 `getMovementSpeed()`。

- 体力读写统一走 `PlayerStaminaApi` / `PlayerStaminaComponent`：清空、回满、增减当前体力、调整体力上限、判断是否还能疾跑 / 自主移动 / 跳跃，都从这里拿。
- 速度叠加统一走 `PlayerMovementApi.registerSpeedModifier(...)`。它支持 `ADD`、`MULTIPLY`、`OVERRIDE` 和 `PASS`，扩展职业要做加速或减速就注册修正器，不要自己覆盖原版速度。
- 当前体力基础值、三档消耗、三档恢复、心情阈值和两个默认关闭的惩罚开关都在 Wathe `GameConstants` / `GameWorldComponent` 里，NoellesRoles 只读不复制。
- 中等心情惩罚开启后，`mood < MID_MOOD_THRESHOLD` 只在疾跑时扣体力；低落心情惩罚开启后，`mood <= DEPRESSIVE_MOOD_THRESHOLD` 会禁跑、走路耗体力，体力归零后无法自主水平移动和跳跃，但外力位移仍保留。
- 两个惩罚开关默认都关闭，测试时可用 `/wathe:setMood` 和 `/wathe:moodStaminaPenalty`。
- 如果某个职业或词条需要专属移动分支，按 `roles/<role>/<RoleName>MovementHandler.java` 或 `modifiers/<modifier>/*MovementHandler.java` 拆开，再由 `NoellesRolesMovementBootstrap` 聚合注册。

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
- `dev.doctor4t.wathe.api.combat.WeaponTargetingApi`
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

### 武器目标判定

NoellesRoles 的客户端武器目标必须区分“准心显示”和“真实攻击”两种语义，统一使用 Wathe `WeaponTargetingApi`：

```java
// 准心 / HUD / 玩家名：尸体伪装可以拒绝 TARGET，避免准心变化暴露身份。
EntityHitResult visibleTarget =
        WeaponTargetingApi.getVisibleAlivePlayerTarget(player, range);

// 普通近战或物品真实命中：使用 ATTACK 语义，不能因为准心隐藏而造成无敌。
EntityHitResult attackTarget =
        WeaponTargetingApi.getAttackableAlivePlayerTarget(player, range);

// 枪械需要继续接入 GunShotApi 的目标覆写链时，使用带 stack 的枪械入口。
HitResult gunTarget =
        WeaponTargetingApi.resolveAttackableGunTarget(player, stack, range);
```

枪械准心对应使用 `resolveVisibleGunTarget(player, stack, range)`，真实发包对应使用 `resolveAttackableGunTarget(player, stack, range)`。枪械 `stack` 应使用当前 `use()` / `onStoppedUsing()` 收到的手持物品，避免副手物品误读主手状态。客户端只负责准备目标实体 id，服务端仍要重新校验目标类型、职业、存活、冷却、距离和 `TargetVisibilityApi.canAttackPlayer(...)`。

扩展武器不要再各自维护 `NoellesItemTargeting` 或重复拼接 `ProjectileUtil` + `TargetVisibilityApi`。具体武器仍按职业 / 词条放在自己的 `item` 或 `roles/<role>/` 包中，公共注册入口只负责调用对应 handler，不要把所有职业目标判定塞进一个大类。

死亡 / 击杀相关逻辑同样按职业或词条拆分，例如：

- `roles/timekeeper/TimekeeperDeathHandler.java`：重复死亡吞噬和时间狭缝。
- `modifiers/dual_personality/DualPersonalityDeathHandler.java`：双重人格致死转化。
- `roles/bounty_hunter/BountyHunterDeathHandler.java`：赏金猎人击杀奖励。
- `roles/controller/ControllerDeathHandler.java`：附体死亡连锁。
- `roles/voodoo/VoodooDeathHandler.java`：巫毒死亡连锁。
- `roles/coroner/CoronerBodySpawnHandler.java`：尸体死因数据。
- `roles/assassin/AssassinBodySpawnHandler.java`：刺客隐藏尸体登记。
- `roles/assassin/AssassinTargetVisibilityHandler.java`：刺客隐藏尸体的渲染、准心选中和服务端交互拦截。

优先级按 Wathe `DeathApi` 的常量表达：重复死亡保护最高，其次是特殊存活保护、死亡流程状态、回放上下文、致死确认前拦截、普通逻辑、确认死亡后的奖励 / 二段机制、最终清理。priority 越大越先执行；同 priority 后注册的规则先执行。handler 返回 `PASS` 或不处理时继续往下走，只有明确 `CANCEL`、`ALLOW`、`DENY` 或 `HANDLED` 才终止对应链路。

## 停电 API 接入方式

Wathe 的停电黑幕、停电夜视/失明和恢复电力现在统一由 `dev.doctor4t.wathe.api.blackout.BlackoutApi` 管理。NoellesRoles 不再 mixin `WorldBlackoutComponent` 私有字段；时停者快照只恢复 `wathe:blackout` 世界组件 NBT，组件里已经包含完整倒计时和配置。

当前 Noelles 接入入口是：

- `src/main/java/org/agmas/noellesroles/bootstrap/NoellesRolesBlackoutBootstrap.java`

已有规则：

- `NoellesRoleGroups.KILLER_SIDED_NEUTRALS` 停电期间获得夜视。
- `NoellesRoleGroups.INDEPENDENT_NEUTRALS` 停电期间获得失明。

新增职业如果要改变停电效果或时长，不要把逻辑直接写进大 bootstrap。按职业新建：

```text
src/main/java/org/agmas/noellesroles/roles/my_role/MyRoleBlackoutHandler.java
```

然后在 `NoellesRolesBlackoutBootstrap.init()` 里调用 `MyRoleBlackoutHandler.init()`。工程师这类“恢复供电”技能直接调用 `BlackoutApi.restorePower(world)`。

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

旧的 `LimitedInventoryScreen` / `LimitedHandledScreen` screen mixin 已经迁出或删除。新增背包按钮不要再添加 `*ScreenMixin` 到 `noellesroles.client.mixins.json`；普通屏幕 HUD、准心图标、准心名字、玩家 / 尸体隐藏以及武器目标射线已经有 Wathe `HudOverlayApi` / `CrosshairHudApi` / `RoleNameHudApi` / `TargetVisibilityApi` / `WeaponTargetingApi`，只有相机、输入控制、物品渲染等 Wathe 尚未公开 API 的场景才考虑窄 mixin。

## HUD 接入方式

NoellesRoles 的普通屏幕 HUD 已经迁移到 Wathe 公开 API，不再通过 `InGameHud` 或 `RoleNameRenderer` mixin 注入。新增职业/词条时按现有结构拆文件：

- 普通右下角职业状态：新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>StatusHud.java`。
- 词条自己的固定屏幕 HUD：新建 `src/client/java/org/agmas/noellesroles/client/hud/modifiers/<modifier>/<ModifierName>Hud.java`。
- 通用注册入口：在 `NoellesHudHandlers.register()` 里调用该职业 `*StatusHud.register()`。
- 重复布局和存活职业过滤：使用 `NoellesHudSupport.registerAliveRole(...)`、`drawBottomRightLine(...)` 或 `drawBottomRightLines(...)`。
- 准心图标、武器锁定和准心下方小进度条：新建 `src/client/java/org/agmas/noellesroles/client/roles/<role>/<RoleName>Crosshair.java`，并在 `NoellesCrosshairHandlers.register()` 里调用；通过 Wathe `CrosshairHudApi.registerProvider(...)` 或 `registerOverlay(...)` 接入，目标选择使用 `WeaponTargetingApi` 的 visible 入口。
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

## 客户端雾效与 Iris 兼容

NoellesRoles 的职业雾效统一接入 Wathe：

- API：`dev.doctor4t.wathe.api.client.fog.FogOverrideApi`
- 杰森 provider：`src/client/java/org/agmas/noellesroles/client/roles/jason/JasonAbilityFogHandler.java`
- 注册入口：`src/client/java/org/agmas/noellesroles/client/NoellesrolesClient.java`

工作顺序是：原版、液体状态和 Wathe 地图雾先计算基础值，职业 provider 再通过 `FogOverrideApi.FogContext` 读取基础值并返回目标 start/end/shape，Wathe 最后把最终值写回 RenderSystem。Iris 的标准 `FogUniforms` 会从 RenderSystem getter 读取该最终值，因此 Sodium + Iris shaderpack 不应把 Wathe 地图雾或杰森雾恢复成普通视频设置视距。世界渲染结束后 Wathe 会调用原版 `BackgroundRenderer.clearFog()` 切换到 GUI 无雾状态，避免 1.21.1 文字 shader 继续使用世界的 `FogColor`，导致聊天栏、tab 和 HUD 在白天变白、夜晚变黑。

新增职业需要雾效时：

1. 在 `client/roles/<role>/` 新建 `<RoleName>FogHandler.java`。
2. 把视距、优先级和过渡时间放入对应 `*Constants`。
3. 在 `NoellesrolesClient.onInitializeClient()` 注册 provider。
4. provider 返回 `FogOverride.pass()` 表示当前状态不接管雾效。
5. 不要新增职业自己的 `WorldRenderer.render` 雾效 mixin，也不要直接依赖 Iris 私有类。

如果修改 Wathe 的 Fog API，联调顺序必须是：

```powershell
cd "D:\哈比快车最新源码\wathe\Wathe - 副本1"
.\gradlew.bat build

Copy-Item "build\libs\wathe-1.3.3-1.21.1.jar" `
  "D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1\libs\wathe-1.3.3-1.21.1.jar" `
  -Force

cd "D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1"
.\gradlew.bat build
```

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
枪击接管、左轮反火、默认击杀收益、确认死亡后清理和尸体生成回调也不是 mixin；它们应通过 `NoellesRolesCombatBootstrap` / `NoellesRolesDeathBootstrap` 调用各职业 handler 接入 Wathe `GunShotApi` / `WeaponTargetingApi` / `DeathApi`。
Harpy 开局职业 / 词条分配规则也不是 mixin；同局互斥、绑定生成、词条公告前强制配对应通过 `HackerRoleAssignmentRules`、`InitiateRoleAssignmentRules`、`LoversModifierAssignmentRules`、`DualPersonalityModifierAssignmentRules` 这类小类接入 Harpy assignment API。

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

## Wathe 托盘 API 接入（1.3.3）

托盘效果在 `NoellesRolesTrayEffects` 中实现 `TrayEffectHandler#replayDisplay` 即可复用 Wathe 的通用 `[效果名]` 回放。职业取物限制分别由 `WaiterTrayTakeRules`、`BartenderTrayTakeRules` 和 `CookTrayTakeRules` 注册到 `TrayTakeRegistry`：服务员同类最多 2 份，酒保鸡尾酒和厨师普通食物同类最多 3 份（均为背包+主手总数）。托盘粒子使用 `TrayParticleRegistry.registerProvider` 注册，禁止新增托盘方块实体 mixin。
客户端粒子由 `client/tray/NoellesRolesTrayParticles` 聚合，具体按职业拆分到 `client/roles/bartender/BartenderTrayParticleHandler`、`client/roles/coward/CowardTrayParticleHandler` 和 `client/roles/bomber/BomberTrayParticleHandler`；`NoellesrolesClient` 只调用聚合入口的 `init()`。
