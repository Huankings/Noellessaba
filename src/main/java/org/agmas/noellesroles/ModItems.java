package org.agmas.noellesroles;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import com.mojang.serialization.Codec;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.item.*;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.arsonist.ArsonistConstants;
import org.agmas.noellesroles.item.LighterItem;
import org.agmas.noellesroles.roles.cleaner.CleanerConstants;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchState;

public class ModItems {
    public static void init() {
        GameConstants.ITEM_COOLDOWNS.put(FAKE_REVOLVER, GameConstants.getInTicks(0,8));
        GameConstants.ITEM_COOLDOWNS.put(TOOLBOX, GameConstants.getInTicks(0, 20));
        GameConstants.ITEM_COOLDOWNS.put(POWER_RESTORATION, GameConstants.getInTicks(1, 0));
        // 飞斧使用固定冷却，避免受 Wathe 初始化顺序影响。
        GameConstants.ITEM_COOLDOWNS.put(THROWING_AXE, GameConstants.getInTicks(0, 0));
        GameConstants.ITEM_COOLDOWNS.put(BLOOD_AXE, SpringTrapConstants.BLOOD_AXE_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(THROWING_SPEED_AXE, GameConstants.getInTicks(0, 0));
        GameConstants.ITEM_COOLDOWNS.put(THROWING_BOMB_AXE, GameConstants.getInTicks(0, 0));
        GameConstants.ITEM_COOLDOWNS.put(SPRING_TRAP, SpringTrapConstants.SPRING_TRAP_COOLDOWN_TICKS);
        // 强盗手枪使用固定冷却，与飞斧分开控制，后续改数值也更直观。
        GameConstants.ITEM_COOLDOWNS.put(ROBBER_PISTOL, GameConstants.getInTicks(0, 35));
        /*
         * 赏金手枪有 30 秒开局冷却、15 秒目标击杀冷却、45 秒失败/非目标冷却三种来源。
         * 这里登记最长的失败冷却作为默认值，客户端 tooltip 会按组件同步的来源动态修正。
         */
        GameConstants.ITEM_COOLDOWNS.put(BOUNTY_PISTOL, BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(BOUNTY_DERRINGER, BountyHunterConstants.BOUNTY_DERRINGER_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(BOUNTY_MODE, BountyHunterConstants.BOUNTY_MODE_COOLDOWN_TICKS);
        // 刺刀是刺客的主力近战武器，击杀后进入 35 秒冷却。
        GameConstants.ITEM_COOLDOWNS.put(BAYONET, GameConstants.getInTicks(0, 35));
        // 无声左轮沿用用户指定的 15 秒冷却。
        GameConstants.ITEM_COOLDOWNS.put(SILENCED_REVOLVER, GameConstants.getInTicks(0, 15));
        // 无声手雷是一次性大件，投出后 5 分钟内无法再次购买。
        GameConstants.ITEM_COOLDOWNS.put(SILENT_GRENADE, GameConstants.getInTicks(5, 0));
        // 定时炸弹存在“开局冷却”和“传递冷却”两种时长。
        // 这里先登记更长的开局冷却作为默认值，客户端 tooltip 再根据当前状态动态修正。
        GameConstants.ITEM_COOLDOWNS.put(TIMED_BOMB, BomberPlayerComponent.BOMBER_START_COOLDOWN_TICKS);
        // 狙击枪同样有多种冷却来源，这里登记开局 30 秒作为默认基线。
        GameConstants.ITEM_COOLDOWNS.put(SNIPER_RIFLE, org.agmas.noellesroles.roles.rememberer.RemembererConstants.SNIPER_START_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(ICON_WEAPON_COOLDOWN_REFRESH, HackerConstants.REFRESH_WEAPON_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(ICON_ABILITY_COOLDOWN_REFRESH, HackerConstants.REFRESH_ABILITY_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(ICON_POTION_EFFECT_REFRESH, HackerConstants.REFRESH_POTION_EFFECT_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(MEDICAL_KIT, PhysicianConstants.MEDICAL_KIT_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(PAN, CookConstants.PAN_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(PILL, PhysicianConstants.PILL_COOLDOWN_TICKS);
        // 静语者胶带冷却来自原 StarryExpress Muzzler 配置，现固定为 NoellesRoles 常量。
        GameConstants.ITEM_COOLDOWNS.put(TAPE, MuzzlerConstants.TAPE_COOLDOWN_TICKS);
        // 猎刀和硫酸桶来自 kinssaba，冷却值迁入各自职业常量，避免继续读取 kinssaba config。
        GameConstants.ITEM_COOLDOWNS.put(HUNTING_KNIFE, HunterConstants.HUNTING_KNIFE_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(SULFURIC_ACID_BARREL, CleanerConstants.SULFURIC_ACID_BARREL_COOLDOWN_TICKS);
        // 制毒师/绑匪三件迁移物品的数值全部落在各自职业常量中，不再依赖 kinssaba config。
        GameConstants.ITEM_COOLDOWNS.put(BLOWGUN, DrugmakerConstants.BLOWGUN_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(POISON_INJECTOR, DrugmakerConstants.POISON_INJECTOR_COOLDOWN_TICKS);
        GameConstants.ITEM_COOLDOWNS.put(KNOCKOUT_DRUG, KidnapperConstants.KNOCKOUT_DRUG_COOLDOWN_TICKS);
        // 纵火犯道具的真实冷却会根据存活人数动态写入，这里登记默认值只用于客户端物品说明。
        GameConstants.ITEM_COOLDOWNS.put(JERRY_CAN, ArsonistConstants.getDouseCooldownTicks(0));
        GameConstants.ITEM_COOLDOWNS.put(LIGHTER, ArsonistConstants.getDouseCooldownTicks(0));

        /*
         * 这里把 NoellesRoles 自己的“实物道具”挂到 Wathe 的装备创造栏里。
         *
         * 只放对局中会真正拿在手里使用的物品，不把商店图标、即时结算图标塞进来，
         * 这样创造物品栏里的展示会和实际可玩道具保持一致，也不会出现一堆纯按钮占位物。
         *
         * 参考 kinssaba / StupidExpress / StarryExpress 的做法，都是通过 Wathe 的
         * equipment 组来追加扩展物品；这里采用同样的方式，但保持 NoellesRoles 现有的
         * 原生注册方式不变，避免改动面扩大。
         */
        ItemGroupEvents.modifyEntriesEvent(WatheItems.EQUIPMENT_GROUP).register(entries -> {
            // 伪装与进攻类武器
            entries.add(FAKE_KNIFE);
            entries.add(FAKE_GRENADE);
            entries.add(FAKE_REVOLVER);
            entries.add(THROWING_AXE);
            entries.add(BLOOD_AXE);
            entries.add(COLORFUL_AXE);
            entries.add(THROWING_SPEED_AXE);
            entries.add(THROWING_BOMB_AXE);
            entries.add(CRYSTAL_BALL);
            entries.add(ROBBER_PISTOL);
            entries.add(BOUNTY_PISTOL);
            entries.add(BOUNTY_DERRINGER);
            entries.add(BAYONET);
            entries.add(SILENCED_REVOLVER);
            entries.add(SILENT_GRENADE);

            // 角色机制与功能道具
            entries.add(MASTER_KEY);
            entries.add(DELUSION_VIAL);
            entries.add(WIND_MARK);
            entries.add(DREAM_IMPRINT);
            entries.add(MEDICAL_KIT);
            entries.add(PAN);
            entries.add(PILL);
            entries.add(TAPE);
            entries.add(HUNTING_KNIFE);
            entries.add(SULFURIC_ACID_BARREL);
            entries.add(BLOWGUN);
            entries.add(POISON_INJECTOR);
            entries.add(KNOCKOUT_DRUG);
            entries.add(MORPH_REAGENT);
            entries.add(MORPH_DEVICE);
            entries.add(JERRY_CAN);
            entries.add(LIGHTER);
            entries.add(PHONE);
            entries.add(DEFENSE_VIAL);
            entries.add(SEDATIVE);
            entries.add(ROLE_MINE);
            entries.add(TOOLBOX);
            entries.add(CAPTURE_DEVICE);
            entries.add(TIMED_BOMB);
            entries.add(SNIPER_RIFLE);
            entries.add(SNIPER_RIFLE_BULLET);
            entries.add(SLEEPING_BAG);
            entries.add(BOOK);
            entries.add(DYING_WATCH);


        });
    }

    /**
     * 狙击枪当前装填弹药量。
     *
     * <p>追忆者的狙击枪需要把“装了几发子弹”稳定写在物品本体上，
     * 这样无论是装填、切格子、掉落还是回放读取，都能直接从同一份数据源拿到结果。</p>
     */
    public static final ComponentType<Integer> SNIPER_AMMO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "sniper_ammo"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build()
    );

    /**
     * 标记“赏金模式临时给予”的德林加。
     *
     * <p>结束赏金模式时只移除带这个标记的那一把，避免误删玩家通过其他来源拿到的普通赏金德林加。</p>
     */
    public static final ComponentType<Boolean> BOUNTY_MODE_GRANTED = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "bounty_mode_granted"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOL)
                    .build()
    );

    /**
     * 濒毁怀表当前状态。
     *
     * <p>0=普通、1=损坏、2=精致。这里刻意用 int 而不是字符串，
     * 是为了让物品数据组件的网络同步更轻，同时避免旧存档里写入未知字符串导致模型谓词崩掉。</p>
     */
    public static final ComponentType<Integer> TIMEKEEPER_WATCH_STATE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_state"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build()
    );

    /**
     * 濒毁怀表当前选择的技能模式。
     *
     * <p>0=物品加速、1=技能加速、2=时间回溯。服务端右键使用时只信任这份组件数据，
     * 客户端左键切换后也会发包给服务端写回，保证 HUD/tooltip/实际效果读同一份状态。</p>
     */
    public static final ComponentType<Integer> TIMEKEEPER_WATCH_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_mode"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build()
    );
    ///添加noellesroles的物品
    //假刀
    public static final Item FAKE_KNIFE = register(
            new FakeKnifeItem(new Item.Settings().maxCount(1)),
            "fake_knife"
    );
    //假手雷
    public static final Item FAKE_GRENADE = register(
            new FakeGrenadeItem(new Item.Settings().maxCount(1)),
            "fake_grenade"
    );
    //假枪
    public static final Item FAKE_REVOLVER = register(
            new RevolverItem(new Item.Settings().maxCount(1)),
            "fake_revolver"
    );
    //飞斧
    public static final Item THROWING_AXE = register(
            new ThrowingAxeItem(new Item.Settings().maxCount(1)),
            "throwing_axe"
    );
    // 血斧
    public static final Item BLOOD_AXE = register(
            new BloodAxeItem(new Item.Settings().maxCount(1)),
            "blood_axe"
    );
    // 彩虹斧
    public static final Item COLORFUL_AXE = register(
            new ColorfulAxeItem(new Item.Settings().maxCount(1)),
            "colorful_axe"
    );
    // 增速飞斧
    public static final Item THROWING_SPEED_AXE = register(
            new ThrowingAxeItem(new Item.Settings().maxCount(1)),
            "throwing_speed_axe"
    );
    // 爆炸飞斧
    public static final Item THROWING_BOMB_AXE = register(
            new ThrowingAxeItem(new Item.Settings().maxCount(1)),
            "throwing_bomb_axe"
    );
    //水晶球
    public static final Item CRYSTAL_BALL = register(
            new CrystalBallItem(new Item.Settings().maxCount(1)),
            "crystal_ball"
    );
    //强盗手枪
    public static final Item ROBBER_PISTOL = register(
            new RobberPistolItem(new Item.Settings().maxCount(1)),
            "robber_pistol"
    );
    //赏金手枪
    public static final Item BOUNTY_PISTOL = register(
            new BountyPistolItem(new Item.Settings().maxCount(1)),
            "bounty_pistol"
    );
    //赏金德林加
    public static final Item BOUNTY_DERRINGER = register(
            new BountyDerringerItem(new Item.Settings().maxCount(1)),
            "bounty_derringer"
    );
    // 刺刀
    public static final Item BAYONET = register(
            new BayonetItem(new Item.Settings().maxCount(1)),
            "bayonet"
    );
    // 无声左轮
    public static final Item SILENCED_REVOLVER = register(
            new SilencedRevolverItem(new Item.Settings().maxCount(1)),
            "silenced_revolver"
    );
    // 无声手雷
    public static final Item SILENT_GRENADE = register(
            new SilentGrenadeItem(new Item.Settings().maxCount(1)),
            "silent_grenade"
    );
    //万能钥匙
    public static final Item MASTER_KEY = register(
            new Item(new Item.Settings().maxCount(1)),
            "master_key"
    );
    //幻觉试剂
    public static final Item DELUSION_VIAL = register(
            new Item(new Item.Settings().maxCount(1)),
            "delusion_vial"
    );
    //风之印记
    public static final Item WIND_MARK = register(
            new WindMarkItem(new Item.Settings().maxCount(1)),
            "wind_mark"
    );
    //梦之印记
    public static final Item DREAM_IMPRINT = register(
            // 梦者最多只能动态拿到 3 个梦之印记；堆叠上限也跟随常量，避免创造栏/其它交付入口出现 4 个一组的旧上限。
            new DreamImprintItem(new Item.Settings().maxCount(DreamerConstants.MAX_DREAM_IMPRINT_COUNT)),
            "dream_imprint"
    );
    //医疗箱
    public static final Item MEDICAL_KIT = register(
            new MedicalKitItem(new Item.Settings().maxCount(1)),
            "medical_kit"
    );
    //平底锅
    public static final Item PAN = register(
            new PanItem(new Item.Settings().maxCount(1)),
            "pan"
    );
    //药丸
    public static final Item PILL = register(
            new PillItem(new Item.Settings().maxCount(1)),
            "pill"
    );
    //静语者胶带
    public static final Item TAPE = register(
            new TapeItem(new Item.Settings().maxCount(1)),
            "tape"
    );
    // 猎刀
    public static final Item HUNTING_KNIFE = register(
            new HuntingKnifeItem(new Item.Settings().maxCount(1)),
            "hunting_knife"
    );
    // 硫酸桶
    public static final Item SULFURIC_ACID_BARREL = register(
            new SulfuricAcidBarrelItem(new Item.Settings().maxCount(1)),
            "sulfuric_acid_barrel"
    );
    // 吹矢
    public static final Item BLOWGUN = register(
            new BlowgunItem(new Item.Settings().maxCount(1)),
            "blowgun"
    );
    // 毒液注射器
    public static final Item POISON_INJECTOR = register(
            new PoisonInjectorItem(new Item.Settings().maxCount(1)),
            "poison_injector"
    );
    // 迷药
    public static final Item KNOCKOUT_DRUG = register(
            new KnockoutDrugItem(new Item.Settings().maxCount(4)),
            "knockout_drug"
    );
    // 变形试剂
    public static final Item MORPH_REAGENT = register(
            new MorphReagentItem(new Item.Settings().maxCount(1)),
            "morph_reagent"
    );
    // 变形遥控器
    public static final Item MORPH_DEVICE = register(
            new MorphDeviceItem(new Item.Settings().maxCount(1)),
            "morph_device"
    );
    // 汽油桶
    public static final Item JERRY_CAN = register(
            new Item(new Item.Settings().maxCount(1)),
            "jerry_can"
    );
    // 打火机
    public static final Item LIGHTER = register(
            new LighterItem(new Item.Settings().maxCount(1)),
            "lighter"
    );
    //黑客手机
    public static final Item PHONE = register(
            new PhoneItem(new Item.Settings().maxCount(1)),
            "phone"
    );
    //防御试剂
    public static final Item DEFENSE_VIAL = register(
            new Item(new Item.Settings().maxCount(1)),
            "defense_vial"
    );
    //镇静试剂
    public static final Item SEDATIVE = register(
            new Item(new Item.Settings().maxCount(1)),
            "sedative"
    );
    //角色装置检测器
    public static final Item ROLE_MINE = register(
            new RoleMineItem(new Item.Settings().maxCount(1)),
            "role_mine"
    );
    //工具箱
    public static final Item TOOLBOX = register(
            new ToolboxItem(new Item.Settings().maxCount(1)),
            "toolbox"
    );
    //捕捉装置
    public static final Item CAPTURE_DEVICE = register(
            new CaptureDeviceItem(new Item.Settings().maxCount(1)),
            "capture_device"
    );
    //定时炸弹
    public static final Item TIMED_BOMB = register(
            new TimedBombItem(new Item.Settings().maxCount(1)),
            "timed_bomb"
    );
    //狙击枪
    public static final Item SNIPER_RIFLE = register(
            new SniperRifleItem(new Item.Settings().maxCount(1).component(SNIPER_AMMO, 0)),
            "sniper_rifle"
    );
    //狙击枪子弹
    public static final Item SNIPER_RIFLE_BULLET = register(
            new SniperRifleBulletItem(new Item.Settings()),
            "sniper_rifle_bullet"
    );
    //睡袋
    public static final Item SLEEPING_BAG = register(
            new Item(new Item.Settings().maxCount(1)),
            "sleeping_bag"
    );
    //图书
    public static final Item BOOK = register(
            new Item(new Item.Settings().maxCount(1)),
            "book"
    );
    // 濒毁怀表：时停者开局获得的核心物品，状态与模式由上方数据组件保存。
    public static final Item DYING_WATCH = register(
            new TimekeeperWatchItem(new Item.Settings()
                    .maxCount(1)
                    .component(TIMEKEEPER_WATCH_STATE, TimekeeperWatchState.NORMAL.ordinal())
                    .component(TIMEKEEPER_WATCH_MODE, TimekeeperWatchMode.ITEM_ACCELERATE.ordinal())),
            "dying_watch"
    );
    
    ///添加noellesroles的商店图标
    //服务员随机食物图标
    public static final Item RANDOM_FOOD = register(
            new Item(new Item.Settings().maxCount(1)),
            "random_food"
    );
    //服务员随机饮品图标
    public static final Item RANDOM_DRINK = register(
            new Item(new Item.Settings().maxCount(1)),
            "random_drink"
    );
    //服务员随机药水图标
    public static final Item RANDOM_POTION = register(
            new Item(new Item.Settings().maxCount(1)),
            "random_potion"
    );
    // 赏金模式图标：购买后立即进入赏金德林加模式，不会作为普通物品进入背包。
    public static final Item BOUNTY_MODE = register(
            new Item(new Item.Settings().maxCount(1)),
            "bounty_mode"
    );
    // 弹簧陷阱状态图标：购买后立即启动状态，不进入背包。
    public static final Item SPRING_TRAP = register(
            new SpringTrapShopIconItem(new Item.Settings().maxCount(1)),
            "spring_trap"
    );
    // 弹簧陷阱续时器图标：购买后直接延长当前状态，不进入背包。
    public static final Item SPRING_TRAP_ADDTIME = register(
            new SpringTrapShopIconItem(new Item.Settings().maxCount(1)),
            "spring_trap_addtime"
    );
    //电力恢复装置
    public static final Item POWER_RESTORATION = register(
            new PowerRestorationItem(new Item.Settings().maxCount(1)),
            "power_restoration"
    );
    // 刺刀冷却刷新图标
    public static final Item BAYONET_COLDOWN_REFRESH = register(
            new BayonetCooldownRefreshItem(new Item.Settings().maxCount(1)),
            "bayonet_coldown_refresh"
    );
    // 黑客刷新武器冷却图标
    public static final Item ICON_WEAPON_COOLDOWN_REFRESH = register(
            new Item(new Item.Settings().maxCount(1)),
            "icon_weapon_cooldown_refresh"
    );
    // 黑客刷新技能冷却图标
    public static final Item ICON_ABILITY_COOLDOWN_REFRESH = register(
            new Item(new Item.Settings().maxCount(1)),
            "icon_ability_cooldown_refresh"
    );
    // 黑客清除药水状态图标
    public static final Item ICON_POTION_EFFECT_REFRESH = register(
            new Item(new Item.Settings().maxCount(1)),
            "icon_potion_effect_refresh"
    );
    // 回溯保护商店图标：购买成功只写入玩家标记，不会发进背包。
    public static final Item DYING_WATCH_PROTECT = register(
            new Item(new Item.Settings().maxCount(1)),
            "dying_watch_protect"
    );



    public static Item register(Item item, String id) {
        // Create the identifier for the item.
        Identifier itemID = Identifier.of(NoellesRolesCore.MOD_ID, id);

        // Register the item.
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

        // Return the registered item!
        return registeredItem;
    }

}
