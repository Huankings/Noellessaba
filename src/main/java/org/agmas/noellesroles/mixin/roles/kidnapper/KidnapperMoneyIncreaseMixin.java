package org.agmas.noellesroles.mixin.roles.kidnapper;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class KidnapperMoneyIncreaseMixin {
    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    private static void noellesroles$increaseKidnapperMoney(@NotNull PlayerEntity victim, boolean spawnBody, @Nullable PlayerEntity killer, Identifier identifier, CallbackInfo ci) {
        if (killer == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        KidnapperComponent controlled = KidnapperComponent.KEY.get(victim);
        if (gameWorld.isRole(killer, NoellesRoleRegistry.KIDNAPPER) && controlled.controlTicks > 0) {
            /*
             * 额外金币在 killPlayer 入口处发放，和 kinssaba 一致：
             * 只要杀死的是仍处于迷药控制中的目标，就算绑匪的“绑架击杀”奖励。
             */
            PlayerShopComponent.KEY.get(killer).addToBalance(KidnapperConstants.ADDITIONAL_KILL_REWARD_COINS);
        }
    }
}
