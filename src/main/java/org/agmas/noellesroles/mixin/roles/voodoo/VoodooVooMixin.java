package org.agmas.noellesroles.mixin.roles.voodoo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.roles.voodoo.VoodooPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class VoodooVooMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"))
    private static void voodoovoo(PlayerEntity victim, boolean spawnBody, PlayerEntity killer,
                                  Identifier identifier, CallbackInfo ci) {
        // 标记受害者正在处理
        DeathProcessComponent.KEY.get(victim).setProcessing(true);

        if (NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths || killer != null) {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
            if (gameWorldComponent.isRole(victim, Noellesroles.VOODOO)) {
                VoodooPlayerComponent voodooPlayerComponent = VoodooPlayerComponent.KEY.get(victim);
                if (voodooPlayerComponent.target != null) {
                    PlayerEntity voodooed = victim.getWorld().getPlayerByUuid(voodooPlayerComponent.target);
                    if (voodooed != null && GameFunctions.isPlayerAliveAndSurvival(voodooed) && voodooed != victim) {
                        // 检查目标是否已在处理中
                        if (!DeathProcessComponent.KEY.get(voodooed).isProcessing()) {
                            /*
                             * 这里把巫毒师本人写进 replay_actor，
                             * 让真正死亡成立后的 death 回放可以显示：
                             * “A 被 B 的巫毒魔法杀害”。
                             *
                             * 同时仍然不提前写 global_event，
                             * 这样若后续被护盾或免死逻辑拦下，就不会出现假死亡回放。
                             */
                            NbtCompound replayDeathData = new NbtCompound();
                            replayDeathData.putUuid("replay_actor", victim.getUuid());
                            GameFunctions.killPlayer(voodooed, true, null, Noellesroles.VOODOO_MAGIC_DEATH_REASON, replayDeathData);
                        }
                    }
                }
            }
        }
    }
}
