package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 追忆者狙击枪的延迟弹道管理器。
 *
 * <p>这里不走 Wathe 左轮的“瞬时命中”，而是维护一条真正按 tick 前进的白色弹道：
 * 1. 开枪那一刻锁定方向；
 * 2. 之后连续 20 tick 沿该方向推进；
 * 3. 所有被线段穿过的玩家都会被击杀；
 * 4. 中途完全无视方块阻挡。</p>
 */
public final class RemembererSniperManager {

    private static final CopyOnWriteArrayList<ActiveSniperShot> ACTIVE_SHOTS = new CopyOnWriteArrayList<>();
    private static final DustParticleEffect SNIPER_TRACE_PARTICLE = new DustParticleEffect(new Vector3f(1.0F, 1.0F, 1.0F), 1.1F);
    private static boolean initialized = false;

    private RemembererSniperManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(RemembererSniperManager::tick);
    }

    public static void fireShot(@NotNull ServerPlayerEntity shooter, @NotNull Vec3d direction, @NotNull ItemStack replayStack) {
        Vec3d normalizedDirection = direction.normalize();
        Vec3d start = shooter.getEyePos().add(normalizedDirection.multiply(RemembererConstants.SNIPER_TRACE_START_OFFSET));
        ACTIVE_SHOTS.add(new ActiveSniperShot(
                shooter.getServerWorld(),
                shooter.getUuid(),
                start,
                normalizedDirection,
                replayStack.copy()
        ));
    }

    private static void tick(MinecraftServer server) {
        ACTIVE_SHOTS.removeIf(ActiveSniperShot::tick);
    }

    private static final class ActiveSniperShot {
        private final ServerWorld world;
        private final UUID shooterUuid;
        private final Vec3d start;
        private final Vec3d direction;
        private final ItemStack replayStack;
        private final Set<UUID> hitPlayers = new HashSet<>();
        private int age = 0;

        private ActiveSniperShot(ServerWorld world, UUID shooterUuid, Vec3d start, Vec3d direction, ItemStack replayStack) {
            this.world = world;
            this.shooterUuid = shooterUuid;
            this.start = start;
            this.direction = direction;
            this.replayStack = replayStack;
        }

        private boolean tick() {
            double segmentStartDistance = this.age * RemembererConstants.SNIPER_BLOCKS_PER_TICK;
            double segmentEndDistance = Math.min(
                    RemembererConstants.SNIPER_RANGE_BLOCKS,
                    (this.age + 1) * RemembererConstants.SNIPER_BLOCKS_PER_TICK
            );

            Vec3d segmentStart = this.start.add(this.direction.multiply(segmentStartDistance));
            Vec3d segmentEnd = this.start.add(this.direction.multiply(segmentEndDistance));

            spawnTraceParticles(segmentStart, segmentEnd);
            hitPlayersAlongSegment(segmentStart, segmentEnd);

            this.age++;
            return this.age >= RemembererConstants.SNIPER_TRAVEL_TICKS;
        }

        /**
         * 用连续的小白色粒子把整条弹道显出来。
         *
         * <p>这里不做方块遮挡裁剪，故意让它即便穿墙也继续被看见，
         * 从视觉上更符合“狙击枪射线贯穿一整段空间”的需求。</p>
         */
        private void spawnTraceParticles(Vec3d segmentStart, Vec3d segmentEnd) {
            Vec3d delta = segmentEnd.subtract(segmentStart);
            double length = delta.length();
            if (length <= 0.0D) {
                return;
            }

            Vec3d step = delta.normalize().multiply(RemembererConstants.SNIPER_PARTICLE_STEP);
            int count = Math.max(1, (int) Math.ceil(length / RemembererConstants.SNIPER_PARTICLE_STEP));
            Vec3d cursor = segmentStart;
            for (int index = 0; index <= count; index++) {
                this.world.spawnParticles(
                        SNIPER_TRACE_PARTICLE,
                        cursor.x,
                        cursor.y,
                        cursor.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                );
                cursor = cursor.add(step);
            }
        }

        private void hitPlayersAlongSegment(Vec3d segmentStart, Vec3d segmentEnd) {
            @Nullable ServerPlayerEntity shooter = this.world.getServer().getPlayerManager().getPlayer(this.shooterUuid);
            for (ServerPlayerEntity candidate : this.world.getPlayers()) {
                if (candidate.getUuid().equals(this.shooterUuid)) {
                    continue;
                }
                if (this.hitPlayers.contains(candidate.getUuid())) {
                    continue;
                }
                if (!GameFunctions.isPlayerAliveAndSurvival(candidate)) {
                    continue;
                }

                Box hitBox = candidate.getBoundingBox().expand(RemembererConstants.SNIPER_HITBOX_EXPANSION);
                if (!intersectsSegment(hitBox, segmentStart, segmentEnd)) {
                    continue;
                }

                this.hitPlayers.add(candidate.getUuid());
                if (shooter != null) {
                    GameRecordManager.recordItemHit(shooter, this.replayStack, candidate, null);
                }
                GameFunctions.killPlayer(
                        candidate,
                        true,
                        shooter,
                        Noellesroles.DEATH_REASON_SNIPER_RIFLE,
                        GameFunctions.createReplayItemData(this.world, this.replayStack)
                );
            }
        }

        private boolean intersectsSegment(Box hitBox, Vec3d segmentStart, Vec3d segmentEnd) {
            return hitBox.contains(segmentStart)
                    || hitBox.contains(segmentEnd)
                    || hitBox.raycast(segmentStart, segmentEnd).isPresent();
        }
    }
}
