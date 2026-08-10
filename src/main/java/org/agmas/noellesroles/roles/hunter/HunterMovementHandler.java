package org.agmas.noellesroles.roles.hunter;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 追猎者举刀疾跑移速处理。
 */
public final class HunterMovementHandler {
    private static final int PRIORITY = 2500;

    private HunterMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/hunter_sprint"), PRIORITY, context -> {
            PlayerEntity player = context.player();
            if (!context.gameWorld().isRole(player, NoellesRoleRegistry.HUNTER)
                    || !player.isUsingItem()
                    || !context.sprinting()) {
                return PlayerMovementApi.MovementSpeedResult.pass();
            }

            ItemStack stack = player.getActiveItem();
            /*
             * 仍然严格限定为 Wathe 匕首或 Noelles 猎刀，避免追猎者拿到其它 SPEAR 动作物品时误吃加速。
             * 旧 mixin 返回固定速度值，这里用 override 保留完全相同的手感。
             */
            if ((stack.isOf(WatheItems.KNIFE) || stack.isOf(ModItems.HUNTING_KNIFE))
                    && stack.getItem().getUseAction(stack) == UseAction.SPEAR) {
                return PlayerMovementApi.MovementSpeedResult.override(HunterConstants.HUNTER_SPRINT_MOVEMENT_SPEED);
            }
            return PlayerMovementApi.MovementSpeedResult.pass();
        });
    }
}
