package org.agmas.noellesroles;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.item.*;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;

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
    }
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
    
    ///添加noellesroles的商店图标
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



    public static Item register(Item item, String id) {
        // Create the identifier for the item.
        Identifier itemID = Identifier.of(Noellesroles.MOD_ID, id);

        // Register the item.
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

        // Return the registered item!
        return registeredItem;
    }

}
