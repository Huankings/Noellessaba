package org.agmas.noellesroles.mixin.roles.jester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class JesterJestMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"), cancellable = true)
    private static void jesterJest(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier identifier, CallbackInfo ci) {
        if (killer != null) {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
            if (gameWorldComponent.isRole(victim, Noellesroles.JESTER) && !gameWorldComponent.isRole(killer, Noellesroles.JESTER) && gameWorldComponent.isInnocent(killer)) {
                PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(victim);
                if (component.getPsychoTicks() <= 0) {
                    component.startPsycho();
                    component.psychoTicks = GameConstants.getInTicks(0, 48);
                    component.armour = 1;
                    if (victim instanceof net.minecraft.server.network.ServerPlayerEntity serverVictim) {
                        NbtCompound extra = new NbtCompound();
                        extra.putUuid("victim", victim.getUuid());
                        GameRecordManager.recordGlobalEvent(serverVictim.getServerWorld(), Noellesroles.JESTER_PSYCHO_STARTED_EVENT, null, extra);
                    }
                    ci.cancel();
                }
            }
        }
    }

}
