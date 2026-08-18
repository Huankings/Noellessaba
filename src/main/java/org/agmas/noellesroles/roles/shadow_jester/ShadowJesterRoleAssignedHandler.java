package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import java.util.List;

/**
 * 影子小丑职业分配后的轻量清理。
 *
 * <p>实际初始任务由配对规则在双方都确定后统一发放。
 * 这里只保留“被其他逻辑转成影子小丑时也不会带着旧刀冷却”的兜底。</p>
 */
public final class ShadowJesterRoleAssignedHandler {
    private static final List<Item> ROLE_START_ITEMS_TO_CLEAN = List.of(
            WatheItems.LOCKPICK,
            WatheItems.KNIFE,
            WatheItems.CROWBAR,
            WatheItems.GRENADE,
            WatheItems.REVOLVER,
            WatheItems.DERRINGER,
            WatheItems.NOTE,
            ModItems.FAKE_KNIFE,
            ModItems.FAKE_REVOLVER,
            ModItems.MASTER_KEY,
            ModItems.DREAM_IMPRINT,
            ModItems.ROBBER_PISTOL,
            ModItems.BOUNTY_PISTOL,
            ModItems.BOUNTY_DERRINGER,
            ModItems.SILENCED_REVOLVER,
            ModItems.SNIPER_RIFLE,
            ModItems.MEDICAL_KIT,
            ModItems.SULFURIC_ACID_BARREL,
            ModItems.KNOCKOUT_DRUG,
            ModItems.JERRY_CAN,
            ModItems.LIGHTER,
            ModItems.PHONE,
            ModItems.DYING_WATCH
    );

    private ShadowJesterRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.SHADOW_JESTER) {
            return;
        }
        clearForeignRoleStartItems(player);
    }

    private static void clearForeignRoleStartItems(PlayerEntity player) {
        /*
         * 影子小丑有两种“覆盖成职业”的路径：
         * 1. 强制配对会在 Harpy 分配流程里指定两名玩家；
         * 2. 随机生成时，第二名会从平民阵营里补齐。
         *
         * 如果被覆盖的玩家此前已经触发过其它扩展职业的 RoleAssignedHandler，
         * 那些职业开局物品会留在背包里。这里集中删除各职业开局会直接发放的物品，
         * 但保留 Wathe 本局基础物品 KEY / LETTER，避免破坏地图钥匙、信件等公共流程。
         */
        for (Item item : ROLE_START_ITEMS_TO_CLEAN) {
            player.getInventory().remove(stack -> stack.isOf(item), Integer.MAX_VALUE, player.getInventory());
            player.getItemCooldownManager().remove(item);
        }
        player.getInventory().markDirty();
    }
}
