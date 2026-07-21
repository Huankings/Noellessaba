package org.agmas.noellesroles.mixin.roles.physician;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.physician.PhysicianStatusAlertHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Wathe 真毒生效时通知 NoellesRoles 医师。
 */
@Mixin(PlayerPoisonComponent.class)
public abstract class PhysicianTipPoisonMixin {
    @Shadow @Final @NotNull private PlayerEntity player;

    @Inject(method = "setDetailedPoisonTicks", at = @At("HEAD"))
    private void noellesroles$tipPhysicianPoison(
            int ticks,
            @Nullable UUID poisoner,
            @NotNull Identifier source,
            @Nullable NbtCompound extra,
            CallbackInfo ci
    ) {
        if (ticks <= 0 || !(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        PhysicianStatusAlertHandler.notifyPoisoned(serverPlayer, poisoner);
    }
}
