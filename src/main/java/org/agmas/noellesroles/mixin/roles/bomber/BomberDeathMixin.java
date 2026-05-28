package org.agmas.noellesroles.mixin.roles.bomber;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把炸弹相关的结算挂在 Wathe 的死亡流程里执行，这样可以保证：
 * 1. 死亡已经真实成立。
 * 2. 定时炸弹不会以掉落物的形式残留在场上。
 * 3. 额外金币奖励和炸弹状态清理会一起完成。
 */
@Mixin(GameFunctions.class)
public abstract class BomberDeathMixin {

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;changeGameMode(Lnet/minecraft/world/GameMode;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private static void noellesroles$handleBombCarrierDeath(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier deathReason, CallbackInfo ci) {
        BomberPlayerComponent.handleBombCarrierDeath(victim, deathReason);
    }
}
