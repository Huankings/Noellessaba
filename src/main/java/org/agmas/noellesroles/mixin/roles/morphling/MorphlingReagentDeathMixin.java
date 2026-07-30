package org.agmas.noellesroles.mixin.roles.morphling;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 变形试剂的死亡后置结算。
 *
 * <p>奖励金币和清理激活标记必须等 Wathe 真正完成死亡后再执行。
 * 如果放进 AllowPlayerDeath 保护链，护盾/免死/反噬这类“最后没有死亡”的场景也会误发奖励。</p>
 */
@Mixin(GameFunctions.class)
public class MorphlingReagentDeathMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("TAIL")
    )
    private static void noellesroles$handleMorphlingReagentAfterKill(
            PlayerEntity victim,
            boolean spawnBody,
            PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        if (!(victim instanceof ServerPlayerEntity serverVictim)) {
            return;
        }
        MorphlingReagentService.afterKill(
                serverVictim,
                killer instanceof ServerPlayerEntity serverKiller ? serverKiller : null,
                deathReason
        );
    }
}
