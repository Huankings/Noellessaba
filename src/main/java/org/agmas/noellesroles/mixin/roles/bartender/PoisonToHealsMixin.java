package org.agmas.noellesroles.mixin.roles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerPoisonComponent.class)
public abstract class PoisonToHealsMixin {
    //属于旧 fake poison 兼容思路，继续挂回去反而会和现在的新托盘逻辑冲突，所以不需要再加回mixin.json里面

    @Shadow @Final private PlayerEntity player;

    @Inject(method = "setPoisonTicks", at = @At("HEAD"), cancellable = true)
    private void defenseVialApply(int ticks, UUID poisoner, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(poisoner, Noellesroles.BARTENDER)) {
            if (player.getWorld().getPlayerByUuid(poisoner) == null) return;
            BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get(player);
            bartenderPlayerComponent.giveArmor();
            ci.cancel();
        }
    }
}
