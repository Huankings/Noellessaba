package org.agmas.noellesroles.mixin.roles.robber;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合结束重置玩家时清理强盗物品的实际冷却条目。
 */
@Mixin(GameFunctions.class)
public class RobberReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetRobber(ServerPlayerEntity player, CallbackInfo ci) {
        player.getItemCooldownManager().remove(ModItems.THROWING_AXE);
        player.getItemCooldownManager().remove(ModItems.ROBBER_PISTOL);
    }
}
