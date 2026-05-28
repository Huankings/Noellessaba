package org.agmas.noellesroles.client.mixin.roles.coward;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.coward.CowardConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeveragePlateBlockEntity.class)
public abstract class SedativeTrayViewMixin {
    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void noellesroles$renderSedativeParticles(
            World world,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            CallbackInfo ci
    ) {
        if (!(blockEntity instanceof BeveragePlateBlockEntity tray)) {
            return;
        }
        if (!Noellesroles.SEDATIVE_TRAY_EFFECT.toString().equals(tray.getTrayEffect())) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.COWARD)) {
            return;
        }
        if (Math.floorMod(pos.asLong() + world.getTime(), CowardConstants.SEDATIVE_TRAY_PARTICLE_INTERVAL_TICKS) != 0) {
            return;
        }

        double angle = world.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.08 + world.random.nextDouble() * 0.24;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double velocityScale = 0.012 + world.random.nextDouble() * 0.012;

        world.addParticle(
                ParticleTypes.WAX_ON,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.05 + world.random.nextDouble() * 0.08,
                pos.getZ() + 0.5 + offsetZ,
                offsetX * velocityScale,
                0.02 + world.random.nextDouble() * 0.03,
                offsetZ * velocityScale
        );
    }
}
