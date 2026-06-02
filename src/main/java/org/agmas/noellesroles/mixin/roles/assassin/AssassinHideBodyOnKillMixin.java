package org.agmas.noellesroles.mixin.roles.assassin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当刺客击杀后，把新生成出来的尸体实体标记为“隐藏尸体”。
 */
@Mixin(GameFunctions.class)
public abstract class AssassinHideBodyOnKillMixin {

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/entity/PlayerBodyEntity;setHeadYaw(F)V")
    )
    private static void noellesroles$markAssassinBodyHidden(
            PlayerEntity victim,
            boolean spawnBody,
            PlayerEntity killer,
            Identifier identifier,
            CallbackInfo ci,
            @Local PlayerBodyEntity playerBodyEntity
    ) {
        if (!spawnBody || killer == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(killer.getWorld());
        if (!gameWorld.isRole(killer, Noellesroles.ASSASSIN)) {
            return;
        }

        HiddenBodiesWorldComponent.KEY.get(killer.getWorld()).addHiddenBody(playerBodyEntity.getUuid());
    }
}
