package org.agmas.noellesroles.mixin.roles.rememberer;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.rememberer.RemembererReplayBookBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合结束或玩家重置时，同步清掉追忆者残留状态。
 */
@Mixin(GameFunctions.class)
public abstract class RemembererReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void noellesroles$resetRememberer(ServerPlayerEntity player, CallbackInfo ci) {
        RemembererPlayerComponent.KEY.get(player).reset();
        RemembererReplayBookBuilder.removeOldMemoryBooks(player);
    }
}
