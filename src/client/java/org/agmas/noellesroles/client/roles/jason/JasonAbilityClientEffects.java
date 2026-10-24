package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 杰森无恶不在的纯客户端视觉效果。
 *
 * <p>红色提示粒子只在本地生成，不写 CCA、不发网络包，也不参与回放。
 * 无恶不在杰森用它观察其他玩家；正常或附身状态的灵术师则用它观察无恶不在杰森。</p>
 */
public final class JasonAbilityClientEffects {
    private static final DustParticleEffect REVEAL_PARTICLE =
            new DustParticleEffect(DustParticleEffect.RED, JasonConstants.ABILITY_REVEAL_PARTICLE_SCALE);
    private static final Map<UUID, TrackedMotion> TRACKED_PLAYERS = new HashMap<>();
    private static int particleTickCounter;

    private JasonAbilityClientEffects() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            reset();
            return;
        }

        ClientPlayerEntity viewer = client.player;
        boolean jasonObserver = JasonAbilityRules.isAbilityActiveLike(viewer);
        boolean spiritualistObserver = GameFunctions.isPlayerAliveAndSurvival(viewer)
                && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.SPIRITUALIST)
                /* NORMAL 与 POSSESSING 都允许显示；PROJECTING 明确排除。 */
                && !SpiritualistPlayerComponent.KEY.get(viewer).isProjecting();
        if (!jasonObserver && !spiritualistObserver) {
            reset();
            return;
        }

        if (jasonObserver && client.currentScreen instanceof LimitedInventoryScreen) {
            client.setScreen(null);
        }

        particleTickCounter++;

        for (PlayerEntity target : client.world.getPlayers()) {
            boolean validTarget = jasonObserver
                    ? !target.getUuid().equals(viewer.getUuid()) && GameFunctions.isPlayerAliveAndSurvival(target)
                    : JasonAbilityRules.isAbilityActiveLike(target);
            if (!validTarget) {
                continue;
            }

            UUID targetUuid = target.getUuid();
            TrackedMotion tracked = TRACKED_PLAYERS.computeIfAbsent(targetUuid, ignored -> new TrackedMotion(target.getPos()));
            boolean moving = tracked.update(target.getPos());
            if (isConcealedByPosture(target)) {
                continue;
            }

            boolean shouldReveal = moving || tracked.stationaryTicks >= JasonConstants.ABILITY_STATIONARY_REVEAL_TICKS;
            if (shouldReveal && particleTickCounter % JasonConstants.ABILITY_REVEAL_PARTICLE_INTERVAL_TICKS == 0) {
                spawnRevealParticles(client, target);
            }
        }

        TRACKED_PLAYERS.keySet().removeIf(uuid -> client.world.getPlayerByUuid(uuid) == null);
    }

    public static void reset() {
        TRACKED_PLAYERS.clear();
        particleTickCounter = 0;
    }

    private static boolean isConcealedByPosture(PlayerEntity target) {
        EntityPose pose = target.getPose();
        return target.isSneaking() || pose == EntityPose.CROUCHING || pose == EntityPose.SWIMMING;
    }

    private static void spawnRevealParticles(MinecraftClient client, PlayerEntity target) {
        if (client.world == null) {
            return;
        }
        Vec3d center = target.getBoundingBox().getCenter();
        for (int i = 0; i < JasonConstants.ABILITY_REVEAL_PARTICLE_COUNT; i++) {
            double x = center.x + (client.world.random.nextDouble() - 0.5D) * JasonConstants.ABILITY_REVEAL_PARTICLE_HORIZONTAL_SPREAD;
            double y = target.getY() + client.world.random.nextDouble() * JasonConstants.ABILITY_REVEAL_PARTICLE_VERTICAL_SPREAD;
            double z = center.z + (client.world.random.nextDouble() - 0.5D) * JasonConstants.ABILITY_REVEAL_PARTICLE_HORIZONTAL_SPREAD;
            client.world.addParticle(REVEAL_PARTICLE, x, y, z, 0.0D, 0.012D, 0.0D);
        }
    }

    private static final class TrackedMotion {
        private Vec3d lastPos;
        private int stationaryTicks;

        private TrackedMotion(Vec3d lastPos) {
            this.lastPos = lastPos;
        }

        private boolean update(Vec3d currentPos) {
            double dx = currentPos.x - this.lastPos.x;
            double dz = currentPos.z - this.lastPos.z;
            boolean moving = dx * dx + dz * dz > JasonConstants.ABILITY_REVEAL_MOVEMENT_EPSILON_SQUARED;
            this.lastPos = currentPos;
            if (moving) {
                this.stationaryTicks = 0;
            } else {
                this.stationaryTicks++;
            }
            return moving;
        }
    }
}
