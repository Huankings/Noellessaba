package org.agmas.noellesroles.mixin.roles.angel;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 处理“天使先死了，则守护关系解除并提示被守护者”的清理逻辑。
 */
@Mixin(GameFunctions.class)
public abstract class AngelDeathCleanupMixin {

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;reset()V")
    )
    private static void cleanupAngelOnDeath(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier deathReason, CallbackInfo ci) {
        if (!(victim instanceof ServerPlayerEntity angelPlayer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, Noellesroles.ANGEL)) {
            return;
        }

        AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(angelPlayer);
        ServerPlayerEntity guardedTarget = angelComponent.resolveGuardedTarget();
        boolean sacrificeDeath = angelComponent.isSacrificeDeathInProgress();
        angelComponent.clearGuardSilently();
        angelComponent.setSacrificeDeathInProgress(false);

        if (guardedTarget != null && !sacrificeDeath) {
            guardedTarget.sendMessage(
                    Text.translatable("message.noellesroles.angel.guardian_died", angelPlayer.getDisplayName())
                            .withColor(Noellesroles.ANGEL.color()),
                    true
            );
            guardedTarget.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }
}
