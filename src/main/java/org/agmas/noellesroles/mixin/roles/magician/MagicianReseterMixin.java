package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackManager;
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 魔术师相关状态在 resetPlayer 时统一清理。
 *
 * <p>这样可以确保：
 * 1. 上一把残留的录制轨迹不会混进下一把；
 * 2. 如果对局重置时仍有皮套在播放，会立刻无声清理；</p>
 */
@Mixin(GameFunctions.class)
public abstract class MagicianReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetMagician(ServerPlayerEntity player, CallbackInfo ci) {
        MagicianPlaybackManager.stopPlaybackSilently(player);
        MagicianPlayerComponent.KEY.get(player).reset();
    }
}
