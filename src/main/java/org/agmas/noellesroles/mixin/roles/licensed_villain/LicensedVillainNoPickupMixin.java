package org.agmas.noellesroles.mixin.roles.licensed_villain;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止执照恶棍拾取地上的物品。
 */
@Mixin(ItemEntity.class)
public class LicensedVillainNoPickupMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockLicensedVillainPickup(PlayerEntity player, CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, NoellesRoleRegistry.LICENSED_VILLAIN)
                && GameFunctions.isPlayerAliveAndSurvival(player)) {
            /*
             * Wathe 当前没有“是否允许玩家拾取 ItemEntity”的公开 API。
             * 为完整保留 kinssaba 原行为，只能在实体碰撞入口做一个窄条件拦截。
             */
            ci.cancel();
        }
    }
}
