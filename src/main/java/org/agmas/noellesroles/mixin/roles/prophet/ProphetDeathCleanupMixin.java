package org.agmas.noellesroles.mixin.roles.prophet;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 先知死亡后立即清理“当前标记”。
 *
 * <p>注意这里只清理先知自己的标记，不清理别人身上的巫毒免伤，
 * 因为需求要求该免伤持续到本局结束为止。</p>
 */
@Mixin(GameFunctions.class)
public abstract class ProphetDeathCleanupMixin {

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;reset()V")
    )
    private static void clearProphetMarkOnDeath(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier deathReason, CallbackInfo ci) {
        if (victim == null || victim.getWorld() == null) {
            return;
        }

        if (dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(victim.getWorld()).isRole(victim, Noellesroles.PROPHET)) {
            ProphetPlayerComponent.KEY.get(victim).clearMarkOnly();
        }
    }
}
