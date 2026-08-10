package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 狙击枪手持减速处理。
 */
public final class RemembererMovementHandler {
    private static final int PRIORITY = 1600;

    private RemembererMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/sniper_rifle_slow"), PRIORITY, context -> {
            PlayerEntity player = context.player();
            if (!player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE)) {
                return PlayerMovementApi.MovementSpeedResult.pass();
            }

            /*
             * 狙击枪减速按物品生效，不额外限制追忆者身份。
             * 这样其它职业通过掉落、商店或调试拿到狙击枪时，也会沿用同一把武器的沉重手感。
             */
            return PlayerMovementApi.MovementSpeedResult.multiply(RemembererConstants.SNIPER_SPEED_MULTIPLIER);
        });
    }
}
