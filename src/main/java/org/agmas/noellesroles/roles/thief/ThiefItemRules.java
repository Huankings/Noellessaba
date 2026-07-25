package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ThiefItemRules {
    private static final Set<Identifier> KEEP_GAME_GOING = initKeepGameGoing();
    private static final Set<Identifier> CAN_TAKE = initCanTake();

    private ThiefItemRules() {
    }

    // 获取物品的资源位置
    public static Identifier getId(Item item) {
        return Registries.ITEM.getId(item);
    }

    // 检查小偷是否被允许拿这个物品
    public static boolean canTake(Item item) {
        return CAN_TAKE.contains(getId(item));
    }

    // 如果为是，当游戏中物品可用且小偷还活着时，游戏不会结束
    public static boolean isKeepGameGoing(Item item) {
        return KEEP_GAME_GOING.contains(getId(item));
    }

    private static Set<Identifier> initKeepGameGoing() {
        Set<Identifier> list = new LinkedHashSet<>();
        list.add(getId(WatheItems.REVOLVER));
        list.add(getId(WatheItems.KNIFE));
        list.add(getId(ModItems.HUNTING_KNIFE)); // Hunting Knife from Hunter role in noellesroles
        list.add(getId(ModItems.THROWING_AXE)); // noellesroles throwing_axe
        list.add(getId(ModItems.ROBBER_PISTOL));
        return list;
    }

    private static Set<Identifier> initCanTake() {
        Set<Identifier> list = new LinkedHashSet<>();
        list.add(getId(WatheItems.REVOLVER));
        list.add(getId(WatheItems.KNIFE));
        list.add(getId(WatheItems.GRENADE));
        list.add(getId(WatheItems.SCORPION));
        list.add(getId(WatheItems.POISON_VIAL));
        list.add(getId(WatheItems.CROWBAR));
        list.add(getId(WatheItems.LOCKPICK));
        list.add(getId(WatheItems.FIRECRACKER));
        list.add(getId(WatheItems.BODY_BAG));
        list.add(getId(WatheItems.NOTE));
        list.add(getId(WatheItems.BAT));
        list.add(getId(ModItems.THROWING_AXE)); // noellesroles throwing_axe
        list.add(getId(ModItems.ROBBER_PISTOL));
        list.add(getId(ModItems.MASTER_KEY)); // Master Key from Conductor role in noellesroles
        list.add(getId(ModItems.CAPTURE_DEVICE));
        list.add(getId(ModItems.DEFENSE_VIAL));
        list.add(getId(ModItems.DELUSION_VIAL));
        list.add(getId(ModItems.ROLE_MINE));
        list.add(getId(ModItems.HUNTING_KNIFE));
        list.add(getId(ModItems.DREAM_IMPRINT));
        list.add(getId(ModItems.KNOCKOUT_DRUG));
        list.add(getId(ModItems.POISON_INJECTOR));
        list.add(getId(ModItems.BLOWGUN));
        list.add(getId(ModItems.PILL));
        list.add(getId(ModItems.TAPE));
        return list;
    }
}
