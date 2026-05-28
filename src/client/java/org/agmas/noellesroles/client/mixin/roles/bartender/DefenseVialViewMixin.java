package org.agmas.noellesroles.client.mixin.roles.bartender;

import dev.doctor4t.wathe.api.event.CanSeePoison;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeveragePlateBlockEntity.class)
public class DefenseVialViewMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), order = 1001, cancellable = true)
    private static void view(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof BeveragePlateBlockEntity tray)) {
            return;
        }
        if (!Noellesroles.DEFENSE_TRAY_EFFECT.toString().equals(tray.getTrayEffect())) {
            return;
        }
        if (!CanSeePoison.EVENT.invoker().visible(MinecraftClient.getInstance().player)) {
            return;
        }
        world.addParticle(
                ParticleTypes.HAPPY_VILLAGER,
                (double) ((float) pos.getX() + 0.5F),
                (double) pos.getY(),
                (double) ((float) pos.getZ() + 0.5F),
                0.0D,
                0.15D,
                0.0D
        );
        ci.cancel();
    }
}
