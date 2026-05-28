package org.agmas.noellesroles.mixin.framing;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerPoisonComponent.class)
public abstract class DoNotApplyFakePoisonMixin {
    //属于旧 fake poison 兼容思路，继续挂回去反而会和现在的新托盘逻辑冲突，所以不需要再加回mixin.json里面

    @Shadow @Final private PlayerEntity player;

    @Shadow public int poisonTicks;


    @Shadow public abstract void reset();

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void defenseVialApply(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.canUseKillerFeatures(player)) {
            if (gameWorldComponent.getRole(player) == null) return;
            if (gameWorldComponent.getRole(player).identifier().getNamespace().equals(Noellesroles.MOD_ID)) { // Don't interfere with any custom non-killer poisoning roles from other mods
                if (poisonTicks <= 5) {
                    reset();
                    ci.cancel();
                }
            }
        }
    }
}
