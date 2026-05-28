package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class ControllerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ControllerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "controller_player"),
            ControllerPlayerComponent.class
    );

    private final PlayerEntity player;

    // 附体状态。
    // 这里同时也是附体师伪装状态的唯一数据源：
    // 1. `controlledTarget` 表示当前伪装成谁；
    // 2. `possessTicks` 表示伪装还能持续多久。
    // 这样就不会再依赖变形怪的组件，也不会出现两个职业时间不同步的问题。
    public UUID controlledTarget = null;
    public int possessTicks = 0; // 正数：附体剩余时间，负数：冷却时间
    public boolean hasArmor = false;
    /**
     * 用来标记“上一帧之前是否真的处于附体中”。
     *
     * <p>这样自然结束事件就不依赖 tick 是否恰好大于 0，
     * 可以像修 Morphling 时那样，稳定地在状态真正收束时打点。</p>
     */
    public boolean possessActive = false;

    // 位置存储（附体师）
    public double originalX = 0;
    public double originalY = 0;
    public double originalZ = 0;
    public float originalYaw = 0;
    public float originalPitch = 0;

    // 位置存储（被附体者）
    public double targetOriginalX = 0;
    public double targetOriginalY = 0;
    public double targetOriginalZ = 0;
    public float targetOriginalYaw = 0;
    public float targetOriginalPitch = 0;

    // 常量
    public static final int POSSESS_DURATION_TICKS = GameConstants.getInTicks(0, 60); // 60秒
    public static final int COOLDOWN_SHORT_TICKS = GameConstants.getInTicks(0, 10);  // 10秒
    public static final int COOLDOWN_LONG_TICKS = GameConstants.getInTicks(0, 10);  // 20秒

    public ControllerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.controlledTarget = null;
        this.possessTicks = 0;
        this.hasArmor = false;
        this.possessActive = false;
        this.originalX = 0;
        this.originalY = 0;
        this.originalZ = 0;
        this.originalYaw = 0;
        this.originalPitch = 0;
        this.targetOriginalX = 0;
        this.targetOriginalY = 0;
        this.targetOriginalZ = 0;
        this.targetOriginalYaw = 0;
        this.targetOriginalPitch = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 判断附体师当前是否处于有效附体中。
     * 只要附体有效，就代表附体师也正在伪装成目标玩家。
     */
    public boolean isPossessing() {
        return this.possessTicks > 0 && this.controlledTarget != null;
    }

    /**
     * 获取附体师当前伪装的目标。
     * 伪装逻辑已经完全抽离到附体师自身，因此客户端渲染只需要读取这里即可。
     */
    public UUID getDisguiseTarget() {
        return this.isPossessing() ? this.controlledTarget : null;
    }

    /**
     * 获取附体师当前伪装剩余时间。
     * 这里直接复用附体剩余时间，保证附体结束时伪装也一定同步结束。
     */
    public int getDisguiseTicks() {
        return Math.max(this.possessTicks, 0);
    }

    @Override
    public void serverTick() {
        if (this.possessTicks > 0) {
            this.possessTicks--;
            if (this.possessTicks == 0) {
                // 时间结束，自动解除
                releasePossession(false);
            }
            this.sync();
        } else if (this.possessTicks < 0) {
            this.possessTicks++;
            this.sync();
        }
    }

    public void startPossession(UUID target, Vec3d targetPos, float targetYaw, float targetPitch) {
        // 附体开始时，附体目标本身就是附体师的伪装目标，
        // 因此不再额外调用变形怪组件来保存另一份变形状态。
        this.controlledTarget = target;
        this.possessTicks = POSSESS_DURATION_TICKS;
        this.hasArmor = true;
        this.possessActive = true;

        // 保存附体师位置
        this.originalX = player.getX();
        this.originalY = player.getY();
        this.originalZ = player.getZ();
        this.originalYaw = player.getYaw();
        this.originalPitch = player.getPitch();

        // 保存目标位置
        this.targetOriginalX = targetPos.getX();
        this.targetOriginalY = targetPos.getY();
        this.targetOriginalZ = targetPos.getZ();
        this.targetOriginalYaw = targetYaw;
        this.targetOriginalPitch = targetPitch;

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", target);
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.CONTROLLER_POSSESS_STARTED_EVENT, serverPlayer, extra);
        }

        this.sync();
    }

    public void releasePossession(boolean killed) {
        releasePossession(killed, false);
    }

    /**
     * 统一解除附体。
     *
     * <p>manual=true 表示玩家主动提前结束；
     * killed=true 表示因死亡/重置等异常收束；
     * 两者都为 false 则代表自然时间结束。</p>
     */
    public void releasePossession(boolean killed, boolean manual) {
        UUID targetUuid = this.controlledTarget;
        boolean wasActivelyPossessing = this.possessActive && targetUuid != null;

        this.controlledTarget = null;
        this.hasArmor = false;
        this.possessActive = false;

        // 由于附体师的伪装状态已经直接绑定在 `controlledTarget + possessTicks` 上，
        // 所以这里清空目标并进入冷却后，客户端渲染会自动停止伪装。

        // 设置冷却时间
        if (killed) {
            this.possessTicks = -COOLDOWN_LONG_TICKS;
        } else {
            this.possessTicks = -COOLDOWN_SHORT_TICKS;
        }

        // 清除被附体者的状态
        if (targetUuid != null) {
            PlayerEntity target = player.getWorld().getPlayerByUuid(targetUuid);
            if (target != null) {
                ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(target);
                if (controlledComp != null) {
                    controlledComp.clearControlled();
                }

                // 移除被附体者的状态效果
                target.removeStatusEffect(StatusEffects.INVISIBILITY);
                target.removeStatusEffect(StatusEffects.SLOW_FALLING);

                // 统一传送逻辑：被附体者传到附体师当前位置，附体师回到原始位置
                target.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(),
                        player.getYaw(), player.getPitch());
                player.refreshPositionAndAngles(originalX, originalY, originalZ, originalYaw, originalPitch);
            }
        }

        if (wasActivelyPossessing && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            if (manual) {
                NbtCompound extra = new NbtCompound();
                extra.putUuid("target_player", targetUuid);
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.CONTROLLER_POSSESS_STOPPED_EARLY_EVENT, serverPlayer, extra);
            } else if (!killed) {
                GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.CONTROLLER_POSSESS_ENDED_EVENT, serverPlayer, null);
            }
        }

        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // 保存附体状态
        if (controlledTarget != null) {
            tag.putUuid("controlledTarget", controlledTarget);
        }
        tag.putInt("possessTicks", possessTicks);
        tag.putBoolean("hasArmor", hasArmor);
        tag.putBoolean("possessActive", possessActive);

        // 保存位置
        tag.putDouble("originalX", originalX);
        tag.putDouble("originalY", originalY);
        tag.putDouble("originalZ", originalZ);
        tag.putFloat("originalYaw", originalYaw);
        tag.putFloat("originalPitch", originalPitch);

        tag.putDouble("targetOriginalX", targetOriginalX);
        tag.putDouble("targetOriginalY", targetOriginalY);
        tag.putDouble("targetOriginalZ", targetOriginalZ);
        tag.putFloat("targetOriginalYaw", targetOriginalYaw);
        tag.putFloat("targetOriginalPitch", targetOriginalPitch);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.containsUuid("controlledTarget")) {
            this.controlledTarget = tag.getUuid("controlledTarget");
        }
        this.possessTicks = tag.getInt("possessTicks");
        this.hasArmor = tag.getBoolean("hasArmor");
        this.possessActive = tag.getBoolean("possessActive");

        this.originalX = tag.getDouble("originalX");
        this.originalY = tag.getDouble("originalY");
        this.originalZ = tag.getDouble("originalZ");
        this.originalYaw = tag.getFloat("originalYaw");
        this.originalPitch = tag.getFloat("originalPitch");

        this.targetOriginalX = tag.getDouble("targetOriginalX");
        this.targetOriginalY = tag.getDouble("targetOriginalY");
        this.targetOriginalZ = tag.getDouble("targetOriginalZ");
        this.targetOriginalYaw = tag.getFloat("targetOriginalYaw");
        this.targetOriginalPitch = tag.getFloat("targetOriginalPitch");
    }
}
