package org.agmas.noellesroles.mixin.roles.necromancer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public class NecromancerKillTrackerMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("TAIL")
    )
    private static void noellesroles$addNecromancerRevive(
            PlayerEntity victim,
            boolean spawnBody,
            PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.canUseKillerFeatures(victim)) {
            return;
        }

        /*
         * StupidExpress 统计的是“死亡者是否属于杀手能力阵营”，不是“谁杀了他”。
         * 因此自杀、误伤、好人击杀杀手都会给死灵法师增加一次可复活次数。
         */
        NecromancerWorldComponent.KEY.get(victim.getWorld()).increaseAvailableRevives();
    }

    @Inject(method = "finalizeGame", at = @At("TAIL"))
    private static void noellesroles$resetNecromancerRevives(ServerWorld world, CallbackInfo ci) {
        NecromancerWorldComponent.KEY.get(world).reset();
    }
}
