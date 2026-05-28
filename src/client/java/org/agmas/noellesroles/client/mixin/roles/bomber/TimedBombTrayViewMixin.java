package org.agmas.noellesroles.client.mixin.roles.bomber;

import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheParticles;
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
 * 托盘里的预埋炸弹专属黑色粒子。
 *
 * <p>最初这层显示依赖于旧版“托盘额外炸弹状态位”。
 * 现在定时炸弹托盘已经改走 wathe 的统一 tray effect 接口，
 * 因此这里直接读取托盘方块实体里真实同步的 trayEffect 标记，
 * 避免粒子显示和实际托盘逻辑再次脱节。</p>
 *
 * <p>粒子只对当前能使用杀手特性的玩家可见，
 * 这样既保留原来的暗示功能，也不会向普通阵营泄露额外信息。</p>
 */
@Mixin(BeveragePlateBlockEntity.class)
public abstract class TimedBombTrayViewMixin {

    @Inject(method = "clientTick", at = @At("HEAD"), order = 1000)
    private static void noellesroles$renderTimedBomb(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof BeveragePlateBlockEntity tray)) {
            return;
        }

        /*
         * 托盘定时炸弹如今是通过 trayEffect 来同步和持久化的，
         * 所以这里直接读取托盘当前真实记录下来的效果 id。
         */
        if (!Noellesroles.TIMED_BOMB_TRAY_EMBEDDED_EVENT.toString().equals(tray.getTrayEffect())) {
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
