package org.agmas.noellesroles.client.mixin.roles.bomber;

import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
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

/**
 * 床里预埋炸弹的黑色粒子。
 * 只让能使用杀手特性的阵营看见，避免普通玩家被额外信息泄露。
 */
@Mixin(TrimmedBedBlockEntity.class)
public abstract class TimedBombBedViewMixin {

    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void noellesroles$renderTimedBomb(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof TrimmedBedBlockEntity bed)) {
            return;
        }

        /*
         * 床里的定时炸弹现在已经改走 wathe 的统一 bed effect 字段，
         * 客户端粒子也直接读取真实同步下来的 effect id，避免再次依赖旧 mixin 状态位。
         */
        if (!Noellesroles.TIMED_BOMB_BED_EMBEDDED_EVENT.toString().equals(bed.getBedEffect())) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (gameWorld == null || !gameWorld.canUseKillerFeatures(client.player)) {
            return;
        }

        if (world.getRandom().nextBetween(0, 20) < 17) {
            return;
        }

        world.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5F,
                pos.getY() + 0.5F,
                pos.getZ() + 0.5F,
                0.0F,
                0.04F,
                0.0F
        );
    }
}
