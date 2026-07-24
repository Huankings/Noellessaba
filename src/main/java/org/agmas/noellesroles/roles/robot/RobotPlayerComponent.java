package org.agmas.noellesroles.roles.robot;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 机器人玩家状态。
 *
 * <p>目前只托管夜视剩余时间。夜视结束回放必须由服务端 tick 落地，
 * 不能依赖客户端药水 HUD 是否显示完成，否则回放会在掉线或视角切换时不稳定。</p>
 */
public class RobotPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<RobotPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            net.minecraft.util.Identifier.of(NoellesRolesCore.MOD_ID, "robot"),
            RobotPlayerComponent.class
    );

    private final PlayerEntity player;
    private int nightVisionTicks = 0;

    public RobotPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.nightVisionTicks <= 0) {
            return;
        }

        --this.nightVisionTicks;
        if (this.nightVisionTicks == 0 && this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.ROBOT_NIGHT_VISION_END_EVENT, serverPlayer, null);
        }
        this.sync();
    }

    public void startNightVision(int ticks) {
        this.nightVisionTicks = Math.max(0, ticks);
        this.sync();
    }

    public void reset() {
        this.nightVisionTicks = 0;
        /*
         * 回合重置或中途转职时清掉残留夜视，避免上一身份的视觉效果带进下一局。
         */
        this.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("nightVisionTicks", this.nightVisionTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.nightVisionTicks = tag.contains("nightVisionTicks") ? tag.getInt("nightVisionTicks") : 0;
    }
}
