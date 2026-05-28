package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 天使玩家组件。
 *
 * <p>这里统一管理：
 * 1. 当前守护目标；
 * 2. 自己是否正在被安抚；
 * 3. 最近一次安抚粒子的中心点与剩余时间；
 * 4. 是否正在执行“代替守护目标赴死”的特殊死亡流程。
 *
 * <p>之所以把“安抚是否生效”也挂到每个玩家自己的组件上，
 * 是因为你要求离开半径后效果仍持续到结束，
 * 这类效果最稳的做法就是直接绑在被影响玩家自己身上持续倒计时。</p>
 */
public class AngelPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<AngelPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "angel"),
            AngelPlayerComponent.class
    );

    private final PlayerEntity player;

    private @Nullable UUID guardedTarget;
    private int soothedTicks = 0;
    private int sootheParticleTicks = 0;
    private double sootheCenterX = 0;
    private double sootheCenterY = 0;
    private double sootheCenterZ = 0;
    private boolean sacrificeDeathInProgress = false;

    public AngelPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.guardedTarget = null;
        this.soothedTicks = 0;
        this.sootheParticleTicks = 0;
        this.sootheCenterX = 0;
        this.sootheCenterY = 0;
        this.sootheCenterZ = 0;
        this.sacrificeDeathInProgress = false;
        this.sync();
    }

    public @Nullable UUID getGuardedTarget() {
        return guardedTarget;
    }

    public boolean hasGuardedTarget() {
        return this.guardedTarget != null;
    }

    public void setGuardedTarget(@Nullable UUID guardedTarget) {
        this.guardedTarget = guardedTarget;
        this.sync();
    }

    public int getSoothedTicks() {
        return soothedTicks;
    }

    public boolean isSoothed() {
        return this.soothedTicks > 0;
    }

    public void applySoothe(int ticks) {
        int sanitizedTicks = Math.max(0, ticks);
        if (sanitizedTicks > this.soothedTicks) {
            this.soothedTicks = sanitizedTicks;
            this.sync();
        }
    }

    public int getSootheParticleTicks() {
        return sootheParticleTicks;
    }

    public Vec3d getSootheCenter() {
        return new Vec3d(this.sootheCenterX, this.sootheCenterY, this.sootheCenterZ);
    }

    /**
     * 开始一轮安抚粒子演出。
     *
     * <p>这个状态只会挂在天使本人身上，用于服务器每 tick 生成一次“渐渐消散”的粒子。</p>
     */
    public void startSootheParticles(Vec3d center) {
        this.sootheParticleTicks = AngelConstants.SOOTHE_PARTICLE_TICKS;
        this.sootheCenterX = center.x;
        this.sootheCenterY = center.y;
        this.sootheCenterZ = center.z;
        this.sync();
    }

    public boolean isSacrificeDeathInProgress() {
        return sacrificeDeathInProgress;
    }

    public void setSacrificeDeathInProgress(boolean sacrificeDeathInProgress) {
        this.sacrificeDeathInProgress = sacrificeDeathInProgress;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.soothedTicks > 0) {
            if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
                this.soothedTicks = 0;
                this.sync();
            } else {
                this.soothedTicks--;
                if (this.soothedTicks == 0 || this.soothedTicks % 20 == 0) {
                    this.sync();
                }
            }
        }

        if (this.sootheParticleTicks > 0) {
            this.tickSootheParticles();
        }
    }

    private void tickSootheParticles() {
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        this.sootheParticleTicks--;

        double progress = (double) this.sootheParticleTicks / (double) AngelConstants.SOOTHE_PARTICLE_TICKS;
        int count = Math.max(2, (int) Math.ceil(AngelConstants.SOOTHE_PARTICLE_MAX_COUNT * progress));
        for (int i = 0; i < count; i++) {
            double angle = this.player.getRandom().nextDouble() * Math.PI * 2;
            double radius = this.player.getRandom().nextDouble() * AngelConstants.SOOTHE_RADIUS;
            double x = this.sootheCenterX + Math.cos(angle) * radius;
            double z = this.sootheCenterZ + Math.sin(angle) * radius;
            double y = this.sootheCenterY + this.player.getRandom().nextDouble() * AngelConstants.SOOTHE_PARTICLE_VERTICAL_SPREAD;

            serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.SNOWFLAKE,
                    x,
                    y,
                    z,
                    1,
                    0.04,
                    0.02,
                    0.04,
                    AngelConstants.SOOTHE_PARTICLE_SPEED
            );
        }

        if (this.sootheParticleTicks == 0 || this.sootheParticleTicks % 20 == 0) {
            this.sync();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.guardedTarget != null) {
            tag.putUuid("guardedTarget", this.guardedTarget);
        }
        tag.putInt("soothedTicks", this.soothedTicks);
        tag.putInt("sootheParticleTicks", this.sootheParticleTicks);
        tag.putDouble("sootheCenterX", this.sootheCenterX);
        tag.putDouble("sootheCenterY", this.sootheCenterY);
        tag.putDouble("sootheCenterZ", this.sootheCenterZ);
        tag.putBoolean("sacrificeDeathInProgress", this.sacrificeDeathInProgress);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.guardedTarget = tag.containsUuid("guardedTarget") ? tag.getUuid("guardedTarget") : null;
        this.soothedTicks = Math.max(0, tag.getInt("soothedTicks"));
        this.sootheParticleTicks = Math.max(0, tag.getInt("sootheParticleTicks"));
        this.sootheCenterX = tag.getDouble("sootheCenterX");
        this.sootheCenterY = tag.getDouble("sootheCenterY");
        this.sootheCenterZ = tag.getDouble("sootheCenterZ");
        this.sacrificeDeathInProgress = tag.getBoolean("sacrificeDeathInProgress");
    }

    /**
     * 仅在被守护玩家死亡 / 天使死亡时做静默解绑，不触发额外提示。
     */
    public void clearGuardSilently() {
        if (this.guardedTarget != null) {
            this.guardedTarget = null;
            this.sync();
        }
    }

    public @Nullable ServerPlayerEntity resolveGuardedTarget() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer) || this.guardedTarget == null) {
            return null;
        }
        return serverPlayer.getServer().getPlayerManager().getPlayer(this.guardedTarget);
    }
}
