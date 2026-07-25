package org.agmas.noellesroles.registry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import org.agmas.noellesroles.roles.amnesiac.AmnesiacConstants;
import org.agmas.noellesroles.roles.arsonist.ArsonistConstants;
import org.agmas.noellesroles.roles.avaricious.AvariciousConstants;
import org.agmas.noellesroles.roles.bellringer.BellringerConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.cleaner.CleanerConstants;
import org.agmas.noellesroles.roles.convener.ConvenerConstants;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.detective.DetectiveConstants;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.initiate.InitiateConstants;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainConstants;
import org.agmas.noellesroles.roles.magician.MagicianConstants;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.necromancer.NecromancerConstants;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;
import org.agmas.noellesroles.roles.robot.RobotConstants;
import org.agmas.noellesroles.roles.starstruck.StarstruckConstants;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;

import java.awt.Color;
import java.util.HashMap;

/**
 * NoellesRoles 的职业注册中心。
 *
 * <p>入口类以前直接持有这些字段，导致任何新增职业都会继续把代码塞进总类。
 * 现在职业实例集中在这里，入口类只通过兼容导出保留旧字段名，新的业务代码应直接引用本类。</p>
 */
public final class NoellesRoleRegistry {
    public static final HashMap<Role, RoleAnnouncementTexts.RoleAnnouncementText> ROLE_ANNOUNCEMENTS = new HashMap<>();
// 下面顺序刻意沿用旧入口类，避免 Wathe/Harpy 中按注册顺序遍历职业时出现行为漂移。
/**
 * 好人：存活到最后即可胜利
 */
    //天使(好人)：自身san值下降速度减缓一半，可安抚周围人san值不掉一段时间，可保护其中一个玩家，在该玩家被伤害的时候牺牲自己而保护别人
    public static final Role ANGEL = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.ANGEL_ID, new Color(236, 220, 239).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //记者(好人)：有便条和撬棍
    public static final Role AWESOME_BINGLUS = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //酒保(好人)：可以看到托盘是否有人下毒，可购买防御试剂，覆盖掉毒药的同时给饮用的玩家提供护盾保护
    public static final Role BARTENDER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.BARTENDER_ID, new Color(217, 241, 240).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //敲钟人(好人)：可以查看剩余时间，并花钱减少时间
    public static final Role BELLRINGER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.BELLRINGER_ID, BellringerConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), true));
    //列车长(好人)：开局拥有万能钥匙，开锁器，假枪。可以打开一般人无法打开的门
    public static final Role CONDUCTOR = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.CONDUCTOR_ID, new Color(255, 205, 84).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //厨师(好人)：可购买食物和锅，锅可以敲晕别人一定时间
    public static final Role COOK = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.COOK_ID, CookConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //验尸官(好人)：可以看到尸体，靠近尸体可获得金币。可以购买一些道具
    public static final Role CORONER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.CORONER_ID, new Color(122, 122, 122).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //胆小鬼(好人)：能感知自己周围的危险，越危险san下降越快，花钱购买镇定试剂，试剂食用后不再感知到，也不会san下降。同时左轮冷却减少
    public static final Role COWARD = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.COWARD_ID, new Color(208, 232, 140).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //侦探(好人)：可以对准玩家调查玩家的身份
    public static final Role DETECTIVE = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.DETECTIVE_ID, DetectiveConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //工程师(好人)：可以购买工具箱修复门的异常状态，购买捕捉装置捕捉其他人，购买电力恢复装置结束停电状态
    public static final Role ENGINEER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.ENGINEER_ID, new Color(100, 149, 237).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //圣母(好人)：把别人变成好人阵营
    public static final Role GODDESS = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.GODDESS_ID, Color.WHITE.getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //模仿者(好人)：在杀手看来是杀手同伙并且和杀手一样的透视颜色，并且拥有通用杀手中立商店
    public static final Role MIMIC = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.MIMIC_ID, new Color(255, 137, 155).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //大嗓门(好人)：死后发光，可以点亮别人
    public static final Role NOISEMAKER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.NOISEMAKER_ID, new Color(200, 255, 0).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //接线员(好人)：可以选择搭建两个人的联系或者广播另外一个人的话
    public static final Role OPERATOR = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.OPERATOR_ID, new Color(75, 221, 192).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //医师(好人)：可以看到谁中毒并且用医疗箱救治，救治后获得金币奖励，可购买药丸饮用来获得护盾抵挡伤害
    public static final Role PHYSICIAN = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.PHYSICIAN_ID, PhysicianConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, PhysicianConstants.getMaxSprintTimeTicks(), false));
    //先知(好人)：可以花钱揭露身份，用水晶球对准玩家使用可以指定揭露该玩家的身份
    public static final Role PROPHET = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.PROPHET_ID, new Color(207, 42, 177).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //回溯者(好人)：可以保持自己的回溯点，花钱传送回去；同时可以购买末影珍珠和紫颂果
    public static final Role RECALLER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.RECALLER_ID, new Color(158, 255, 255).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //追忆者(好人)：开局拥有狙击枪。能力是可以摸取玩家回忆。购买狙击枪子弹装填给狙击枪，狙击枪可穿墙，大部分伤害都无法阻挡
    public static final Role REMEMBERER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.REMEMBERER_ID, new Color(46, 46, 66).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //机器人(好人)：假心情，无限体力，可以夜视
    public static final Role ROBOT = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.ROBOT_ID, RobotConstants.ROLE_COLOR, true, false, Role.MoodType.FAKE, -1, false));
    //灵术师(好人)：可以灵魂出窍观察，也可以附身其他玩家
    public static final Role SPIRITUALIST = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.SPIRITUALIST_ID, org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //星界使者(好人)：可以发动技能透视到所有人一段时间，做任务可以减少冷却时间
    public static final Role STARSTRUCK = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.STARSTRUCK_ID, StarstruckConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime() + StarstruckConstants.SPRINT_TIME_BONUS_TICKS, false));
    //调查官(好人)：可以购买调查装置检测周围玩家的身份
    public static final Role TRAPPER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.TRAPPER_ID, new Color(132, 186, 167).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //巫毒师(好人)：可以绑定一个人，在自己受到伤害后让他和你同归于尽
    public static final Role VOODOO = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.VOODOO_ID, new Color(128, 114, 253).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //服务员(好人)：可以辅助别的玩家完成心情任务
    public static final Role WAITER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.WAITER_ID, WaiterConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.VIGILANTE.getMaxSprintTime(), false));
    //风灵师(好人)：可以漂浮玩家，购买风弹和风之印记，风之印记可标记人，当周围有人举刀会被紧急抬升
    public static final Role WINDER = WatheRoles.registerCivilianRole(new Role(NoellesRoleIds.WINDER_ID, new Color(66, 215, 215).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));

/**
 * 义警：帮助好人消灭凶手
 */
    //更好的义警(义警)：开局有个手雷
    public static final Role BETTER_VIGILANTE = WatheRoles.registerVigilanteRole(new Role(NoellesRoleIds.BETTER_VIGILANTE_ID, new Color(0, 255, 255).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));

/**
 * 杀手：杀死阻碍自己获胜的人
 */
    //刺客(杀手)：专属刺客商店，刺刀刀人无声音无前摇，并且可以在商店花钱重置刺刀cd，此外刺客杀的人大部分人都会无法看见
    public static final Role ASSASSIN = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.ASSASSIN_ID, new Color(34, 68, 36).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //扒手(杀手)：无被动收入，初始50金币。靠近玩家从而每隔一段时间获得金币
    public static final Role AVARICIOUS = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.AVARICIOUS_ID, AvariciousConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //炸弹客(杀手)：没有一般性武器，但是可购买特殊炸弹武器来杀人
    public static final Role BOMBER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.BOMBER_ID, new Color(50, 50, 50).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //赏金猎人(杀手)：锁定悬赏目标，击杀目标时获得额外金币，并拥有赏金枪械与赏金模式商店。
    public static final Role BOUNTY_HUNTER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.BOUNTY_HUNTER_ID, BountyHunterConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //洗脑师(杀手)：把别人变成杀手阵营
    public static final Role BRAINWASHER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.BRAINWASHER_ID, new Color(255, 105, 180).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //清道夫(杀手)：拥有专属武器硫酸桶，可以清理尸体并额外获得金币。还可以花钱来清理掉落物
    public static final Role CLEANER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.CLEANER_ID, CleanerConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //附体师(杀手)：可以附体别人，别人在此期间会动弹不得，屏幕黑屏
    public static final Role CONTROLLER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.CONTROLLER_ID, new Color(128, 0, 128).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //造尸怪(杀手)：可以伪造尸体，并且尸体的角色和死因都可以被编辑。特殊的伪装会有特殊的效果
    public static final Role CORPSEMAKER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.CORPSEMAKER_ID, new Color(12, 0, 228).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //制毒师(杀手)：特殊商店，购买毒液注射器和吹矢给玩家中毒，毒药蝎子更便宜
    public static final Role DRUGMAKER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.DRUGMAKER_ID, DrugmakerConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //追猎者(杀手)：拥有专属商店武器猎刀，疾跑时候举刀速度会更快，但是猎刀放下后会进入动态时间冷却。可以花钱重置猎刀和匕首的cd
    public static final Role HUNTER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.HUNTER_ID, HunterConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //绑匪(杀手)：特殊道具迷药，可迷晕别人和自己走
    public static final Role KIDNAPPER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.KIDNAPPER_ID, KidnapperConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //魔术师(杀手)：可以录制自己的行为回放，并且以自己选择的皮套播放，皮套造成的伤害结算给魔术师。皮套被伤害则也给魔术师提供奖励
    public static final Role MAGICIAN = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.MAGICIAN_ID, MagicianConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //变形怪(杀手)：可以变形成别人的样子
    public static final Role MORPHLING = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.MORPHLING_ID, new Color(170, 2, 61).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //静语者(杀手)：特殊武器胶带，对准玩家使用堵住玩家的嘴巴，撕胶带如果到san为0的时候则
    public static final Role MUZZLER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.MUZZLER_ID, MuzzlerConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, WatheRoles.KILLER.getMaxSprintTime(), true));
    //死灵法师(杀手)：可以复活其他人的尸体为杀手
    public static final Role NECROMANCER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.NECROMANCER_ID, NecromancerConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //幻灵(杀手)：可以隐身一段时间
    public static final Role PHANTOM = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.PHANTOM_ID, new Color(80, 5, 5, 192).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //强盗(杀手)：拥有自己的特色武器，可用这些特色武器来完成击杀
    public static final Role ROBBER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.ROBBER_ID, new Color(220, 82, 50).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //潜行者(杀手)：通过凝视别人获得能量，进到阶段后解锁属于自己的能力来大杀特杀
    public static final Role STALKER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.STALKER_ID, new Color(186, 85, 211).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //交换者(杀手)：交换任意两个玩家的位置
    public static final Role SWAPPER = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.SWAPPER_ID, new Color(57, 4, 170).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //亡语杀手(杀手)：能听到死者说话的声音
    public static final Role THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES = WatheRoles.registerKillerRole(new Role(NoellesRoleIds.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID, new Color(255, 0, 0, 192).getRGB(), false, true, Role.MoodType.FAKE, -1, true));

/**
 * 杀手中立：辅助杀手，帮杀手取得胜利
 */
    //梦者(杀手中立)：拥有透视，可以用梦之印记来烙印别人，别人受伤害后会传送回梦者身边并标记次数。当传送次数+饮用幻觉试剂人数次数满足条件时转变为随机杀手
    public static final Role DREAMER = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.DREAMER_ID, DreamerConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, -1, true));
    //仇杀客(杀手中立)：获得自己的仇杀目标，如果目标因为非杀手原因而死亡则自己变成杀手，反之寻找新的目标
    public static final Role EXECUTIONER = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.EXECUTIONER_ID, new Color(74, 27, 5).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime() * 3 / 2, true));
    //黑客(杀手中立)：可透视到杀手，可以破解未知玩家身份。破解后获得金币，可以在商店购买辅助道具辅助杀手取得胜利
    public static final Role HACKER = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.HACKER_ID, HackerConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime(), true));
    //狂信者(杀手中立)：拥有透视，和杀手一起合作。被好人枪击会进入疯魔模式
    public static final Role JESTER = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.JESTER_ID, new Color(255, 86, 243).getRGB(), false, false, Role.MoodType.FAKE, -1, true));
    //秃鹫(杀手中立)：吃掉一定的尸体后变成杀手
    public static final Role VULTURE = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.VULTURE_ID, new Color(181, 103, 0).getRGB(), false, false, Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime() + 100, true));

/**
 * 普通中立：可自行选择帮助的阵营，或者自行转变身份
 */
    //失忆患者(普通中立)：可以透视到尸体，对尸体交互即可获得该尸体的身份
    public static final Role AMNESIAC = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.AMNESIAC_ID, AmnesiacConstants.ROLE_COLOR, false, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //初学者(普通中立)：总是成对出现，正确击杀另一名初学者后晋升为随机杀手；失败或同伴死亡时按死因转职
    public static final Role INITIATE = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.INITIATE_ID, InitiateConstants.ROLE_COLOR, false, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), true));

/**
 * 独立中立：达成自己的胜利条件获胜
 */
    //纵火犯(独立中立)：给所有玩家浇上油后点燃，只剩下自己的时候获得胜利
    public static final Role ARSONIST = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.ARSONIST_ID, ArsonistConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, -1, true));
    //召集者(独立中立)：召集尸体到一定次数或者全场只剩下自己的时候获胜
    public static final Role CONVENER = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.CONVENER_ID, ConvenerConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, -1, true));
    //小偷(独立中立)：空手从玩家背包偷取武器/工具，场上仍有可用武器时拖住普通结算，最终独自存活获胜
    public static final Role THIEF = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.THIEF_ID, ThiefConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, -1, true));
    //执照恶棍(独立中立)：持证使用左轮追杀所有人，活着时会阻止普通阵营提前结算
    public static final Role LICENSED_VILLAIN = WatheRoles.registerNeutralRole(new Role(NoellesRoleIds.LICENSED_VILLAIN_ID, LicensedVillainConstants.ROLE_COLOR, false, false, Role.MoodType.FAKE, LicensedVillainConstants.getMaxSprintTimeTicks(), false));
///
    private NoellesRoleRegistry() {
    }
}
