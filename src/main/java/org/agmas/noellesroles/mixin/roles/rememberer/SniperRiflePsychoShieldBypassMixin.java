package org.agmas.noellesroles.mixin.roles.rememberer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 只对狙击枪死因绕过 Wathe 原版疯魔护盾。
 *
 * <p>这里不改 Wathe 主体 API，而是在 NoellesRoles 自己侧做一个极小特判：
 * 当死因是狙击枪时，直接把 psychoTicks 视作 0，
 * 从而整段“护盾格挡 / 停止疯魔”逻辑都会被跳过。</p>
 */
@Mixin(GameFunctions.class)
public abstract class SniperRiflePsychoShieldBypassMixin {

    @ModifyExpressionValue(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerPsychoComponent;getPsychoTicks()I"
            )
    )
    private static int noellesroles$bypassPsychoShieldForSniper(
            int original,
            PlayerEntity victim,
            boolean spawnBody,
            PlayerEntity killer,
            Identifier deathReason
    ) {
        if (Noellesroles.DEATH_REASON_SNIPER_RIFLE.equals(deathReason)) {
            return 0;
        }
        return original;
    }
}
