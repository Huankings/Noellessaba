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
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;

public class ModItems {
    public static void init() {
        GameConstants.ITEM_COOLDOWNS.put(FAKE_REVOLVER, GameConstants.getInTicks(0,8));
        GameConstants.ITEM_COOLDOWNS.put(TOOLBOX, GameConstants.getInTicks(0, 20));
        GameConstants.ITEM_COOLDOWNS.put(POWER_RESTORATION, GameConstants.getInTicks(1, 0));
        // 飞斧使用固定冷却，避免受 Wathe 初始化顺序影响。
        GameConstants.ITEM_COOLDOWNS.put(THROWING_AXE, GameConstants.getInTicks(0, 0));
        // 强盗手枪使用固定冷却，与飞斧分开控制，后续改数值也更直观。
        GameConstants.ITEM_COOLDOWNS.put(ROBBER_PISTOL, GameConstants.getInTicks(0, 35));
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
            entries.add(CRYSTAL_BALL);
            entries.add(ROBBER_PISTOL);
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
            new DreamImprintItem(new Item.Settings().maxCount(4)),
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



    public static Item register(Item item, String id) {
        // Create the identifier for the item.
        Identifier itemID = Identifier.of(NoellesRolesCore.MOD_ID, id);

        // Register the item.
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

        // Return the registered item!
        return registeredItem;
    }

}
